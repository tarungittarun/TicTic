"""Synthetic fresh-emulator check of the public v0.12.0 Note after a signed v0.72 update.

This checks one ASCII text Note in the actual public release, not personal data,
all historical schemas, a physical device, or public-release readiness. It never
uninstalls the app or clears its data.
"""
import argparse
import json
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

PACKAGE = 'app.space.notes'
TITLE = 'LegacyNoteFixture72'
BODY = 'RetainLegacyBody72'


def nodes(xml):
    return [node for node in ET.fromstring(xml).iter('node')
            if node.get('package') == PACKAGE and node.get('enabled') == 'true']


def named(items, label):
    matches = [node for node in items
               if label in (node.get('text'), node.get('content-desc'))]
    return next((node for node in matches if node.get('clickable') == 'true'),
                matches[0] if matches else None)


def center(bounds):
    match = re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', bounds)
    if not match:
        raise ValueError('Invalid UI bounds')
    left, top, right, bottom = map(int, match.groups())
    if right <= left or bottom <= top:
        raise ValueError('Empty UI target')
    return (left + right) // 2, (top + bottom) // 2


def identity(text):
    version = re.search(r'^\s*versionCode=(\d+)\b', text, re.M)
    first = re.search(r'^\s*firstInstallTime=(.+)$', text, re.M)
    if not version or not first:
        raise ValueError('Missing package installation identity')
    return {'version_code': int(version.group(1)),
            'first_install_time': first.group(1).strip()}


def saved_revisions(log):
    return [int(value) for value in
            re.findall(r'\bsave outcome=ok revision=(\d+)\b', log)]


def phase(message):
    print('::notice title=Legacy install-over smoke phase::' +
          message.replace('%', '%25').replace('\n', '%0A'))


class Device:
    def adb(self, *args):
        command = ['adb', *map(str, args)]
        try:
            return subprocess.check_output(command, text=True,
                                           stderr=subprocess.STDOUT, timeout=45)
        except (subprocess.CalledProcessError, subprocess.TimeoutExpired) as error:
            output = getattr(error, 'output', '')
            detail = ('command=' + ' '.join(map(str, args)) +
                      '; error=' + str(error) + '; output=' + str(output))
            print('::error title=Legacy install-over ADB command failed::' +
                  detail.replace('%', '%25').replace('\n', '%0A'))
            raise

    def ui(self):
        self.adb('shell', 'uiautomator', 'dump', '/sdcard/notes-update-ui.xml')
        return nodes(self.adb('exec-out', 'cat', '/sdcard/notes-update-ui.xml'))

    def wait(self, condition, description):
        deadline = time.monotonic() + 40
        current = []
        while time.monotonic() < deadline:
            current = self.ui()
            if condition(current):
                return current
            time.sleep(.25)
        visible = json.dumps([{key: node.get(key, '') for key in
                               ('text', 'content-desc', 'class', 'clickable')}
                              for node in current], ensure_ascii=True)[:2400]
        print('::error title=Legacy release update state missing::' +
              (description + ': ' + visible).replace('%', '%25').replace('\n', '%0A'))
        raise RuntimeError('Timed out waiting for ' + description)

    def click(self, label):
        current = self.wait(lambda items: named(items, label) is not None,
                            'visible control ' + label)
        self.adb('shell', 'input', 'tap',
                 *center(named(current, label).get('bounds', '')))

    def fill(self, hint, value):
        if not re.fullmatch('[A-Za-z0-9]+', value):
            raise ValueError('Only fixed synthetic ASCII fixture text is supported')
        self.click(hint)
        self.adb('shell', 'input', 'text', value)

    def save_count(self):
        log = self.adb('logcat', '-d', '-s', 'MyNotes:I')
        return len(saved_revisions(log))

    def wait_for_save_stable(self, description, after_count):
        """Wait for a new successful local-save log after the given baseline."""
        deadline = time.monotonic() + 40
        observed_count = after_count
        stable_since = None
        latest = ()
        while time.monotonic() < deadline:
            log = self.adb('logcat', '-d', '-s', 'MyNotes:I')
            revisions = tuple(saved_revisions(log))
            if len(revisions) > after_count and len(revisions) != observed_count:
                observed_count = len(revisions)
                latest = revisions
                stable_since = time.monotonic()
            elif len(revisions) > after_count and stable_since is not None and time.monotonic() - stable_since >= 1.25:
                return revisions[-1]
            time.sleep(.3)
        print('::error title=Legacy release save not confirmed::' +
              (description + '; baseline save count: ' + str(after_count) +
               '; completed save revisions: ' + repr(latest))
              .replace('%', '%25').replace('\n', '%0A'))
        raise RuntimeError('Timed out waiting for a stable local save: ' + description)

    def home(self):
        for _ in range(5):
            current = self.ui()
            if named(current, 'New text note') is not None:
                return
            self.adb('shell', 'input', 'keyevent', 4)
        self.wait(lambda items: named(items, 'New text note') is not None,
                  'Notes home')

    def launch(self):
        self.adb('shell', 'am', 'force-stop', PACKAGE)
        self.adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.NotesActivity')
        self.wait(lambda items: named(items, 'New text note') is not None,
                  'Notes home after cold start')

    def state(self):
        return identity(self.adb('shell', 'dumpsys', 'package', PACKAGE))

    def verify_note(self):
        self.wait(lambda items: named(items, TITLE) is not None,
                  'saved legacy Note card ' + TITLE)
        self.click(TITLE)
        self.wait(lambda items: all(
            any(node.get('class') == 'android.widget.EditText' and
                node.get('text') == value for node in items)
            for value in (TITLE, BODY)),
            'saved legacy Note title and body in the editor')
        self.home()


def verify(old_apk, candidate_apk, output, old_code=13, candidate_code=72):
    if old_code < 1 or candidate_code <= old_code:
        raise ValueError('Require an increasing installed version code')
    device = Device()
    device.adb('install', old_apk)
    before = device.state()
    if before['version_code'] != old_code:
        raise RuntimeError('Expected installed old version code ' + str(old_code))
    phase('Installed the exact public package at versionCode ' + str(old_code))

    device.launch()
    empty_note_save_baseline = device.save_count()
    device.click('New text note')
    device.wait(lambda items: any(node.get('class') == 'android.widget.EditText'
                                  for node in items),
                'new Note editor')
    # Use log-count baselines rather than logcat -c: Android 8/API 26 denies
    # clearing its main log buffer. These are diagnostics only, never app data.
    device.wait_for_save_stable('new empty Note', empty_note_save_baseline)
    phase('Created and confirmed the empty legacy Note save')
    fixture_save_baseline = device.save_count()
    device.fill('Title', TITLE)
    device.fill('Write your note…', BODY)
    device.home()
    device.wait_for_save_stable('fixture title and body', fixture_save_baseline)
    phase('Confirmed the synthetic legacy title and body were saved')

    # Verify disk persistence before the update, not just the old process's memory.
    device.launch()
    device.verify_note()
    phase('The Note survived a cold restart of public versionCode ' + str(old_code))
    device.adb('shell', 'am', 'force-stop', PACKAGE)
    device.adb('install', '-r', candidate_apk)
    after = device.state()
    if (after['version_code'] != candidate_code or
            after['first_install_time'] != before['first_install_time']):
        raise RuntimeError('Update changed install identity or installed the wrong version')
    phase('Install-over retained the package first-install time and reached versionCode ' + str(candidate_code))
    device.launch()
    device.verify_note()
    phase('The legacy Note survived the candidate update and a cold restart')
    report = {
        'ok': True,
        'package': PACKAGE,
        'from': before,
        'to': after,
        'api': int(device.adb('shell', 'getprop', 'ro.build.version.sdk').strip()),
        'legacy_note_retained_after_cold_restart_and_update': True,
        'uninstall_or_clear_data': False,
        'same_package_install_over': True,
        'release_approval': False,
        'fixture': {'title': TITLE, 'body': BODY},
        'scope': ('One synthetic ASCII Note created in the public 0.12.0 (13) APK, '
                  'confirmed after an old-app cold start, then read after the signed '
                  '0.72.0 (72) install-over. Emulator-only; not personal-data, '
                  'all-schema, or physical-device acceptance.')
    }
    Path(output).write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--old', required=True)
    parser.add_argument('--candidate', required=True)
    parser.add_argument('--output', required=True)
    parser.add_argument('--old-code', type=int, default=13)
    parser.add_argument('--candidate-code', type=int, default=72)
    args = parser.parse_args()
    try:
        verify(args.old, args.candidate, args.output,
               args.old_code, args.candidate_code)
    except Exception as error:
        print('::error title=Legacy release install-over smoke failed::' +
              str(error).replace('%', '%25').replace('\n', '%0A'))
        raise
