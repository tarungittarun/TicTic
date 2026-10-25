"""Asserts a sideloadable APK does not declare permissions Play Protect refuses.

Run against a built APK, not against source. Google Play Protect refuses -- not warns about --
any APK installed outside the Play Store that declares READ_SMS or SEND_SMS, and that refusal
happens before any code in the APK runs, so no source-level assertion can observe it. This is
the one check in the project that looks at installability.

The manifest inside an APK is binary XML whose string pool is UTF-16LE, so permission names are
greppable as bytes without aapt. Controls are asserted first: if the scan cannot see strings
that are definitely in the manifest, the scan is broken and must fail loudly rather than pass
quietly.

The controls are a parameter because what is "definitely in the manifest" differs per app.
MySpace declares INTERNET and WAKE_LOCK, so those two prove the byte-scan works. MyCalc declares
zero permissions -- that is the whole point of it -- so its scan is controlled on the launcher
intent strings instead, which are android:name values stored exactly like permission names and
are guaranteed present in any launcher APK. Running the MySpace controls over MyCalc would fail
a healthy scan, and dropping controls entirely would let a broken scan pass quietly.
"""
import sys
import zipfile

DEFAULT_CONTROLS = ("INTERNET", "WAKE_LOCK")

def main(argv):
    path = argv[0]
    controls = []
    i = 1
    while i < len(argv):
        if argv[i] == "--control":
            i += 1
            if i >= len(argv):
                print("SCAN BROKEN: --control needs a name")
                return 2
            controls.append(argv[i])
        else:
            print("unknown argument: %s" % argv[i])
            return 2
        i += 1
    if not controls:
        controls = DEFAULT_CONTROLS

    blob = zipfile.ZipFile(path).read("AndroidManifest.xml")

    def present(name):
        return name.encode("utf-16-le") in blob

    for control in controls:
        if not present(control):
            print("SCAN BROKEN: %s should be visible in this manifest but was not found"
                  % control)
            return 2
    # AGP omits android:debuggable from the merged manifest when it is false, so the attribute
    # name appearing in the binary string pool at all means the build is debuggable -- which is
    # what Play Protect's harmful-app verdict was correctly objecting to.
    if "debuggable".encode("utf-16-le") in blob:
        print("DEBUGGABLE: the APK can be attached to over adb; do not hand this to a tester")
        return 1
    for forbidden in ("READ_SMS", "SEND_SMS"):
        if present(forbidden):
            print("REFUSED AT INSTALL: %s is declared; Play Protect blocks sideloaded APKs "
                  "that carry it" % forbidden)
            return 1
    print("OK: no SMS permissions declared; scan validated by controls")
    return 0

if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
