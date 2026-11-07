"""Guard scope and live execution contracts, not claims that probes equal full UX acceptance."""
import ast
from pathlib import Path
import unittest
import yaml

SCRIPT=Path('mynotes/tools/comparisons/paired_modules37.py')
WORKFLOW=Path('.github/workflows/notes-comparison37.yml')

class PairedModuleComparisonTest(unittest.TestCase):
    def test_all_37_areas_have_explicit_per_app_dispatch(self):
        tree=ast.parse(SCRIPT.read_text())
        for name in ['ours_task','obs_task']:
            function=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name==name)
            numbers=set()
            for node in ast.walk(function):
                if isinstance(node,ast.Compare) and isinstance(node.left,ast.Name) and node.left.id=='number':
                    for right in node.comparators:
                        if isinstance(right,ast.Constant) and isinstance(right.value,int):numbers.add(right.value)
                        if isinstance(right,ast.Set):numbers.update(n.value for n in right.elts if isinstance(n,ast.Constant))
                if name=='obs_task' and isinstance(node,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='patterns' for t in node.targets):
                    numbers.update(n.value for n in node.value.keys)
            self.assertEqual(numbers,set(range(1,38)),name)

    def test_profiles_and_exact_current_signed_app_pins(self):
        text=WORKFLOW.read_text();workflow=yaml.safe_load(text)
        rows=workflow['jobs']['compare']['strategy']['matrix']['include']
        self.assertEqual(len(rows),4)
        self.assertEqual({(r['night'],r['orientation']) for r in rows},{(a,b) for a in ['yes','no'] for b in ['portrait','landscape']})
        self.assertIn('a7d6f685992c68fe4ffc7f3a5705d0514986c9d9fd82ca67c6f68d392ca41de3',text)
        self.assertIn('2d92966fba5441a8976ae86b39cf138c0a845d3dcd318e6f0d29d304950bca5d',text)
        self.assertLess(text.index('python -m mynotes.tools.ci.offline_guest'),text.index('adb install'))
        self.assertNotIn('gradlew',text)
        self.assertNotIn('pm clear',text)
        self.assertNotIn('adb uninstall',text)

    def test_pages_are_not_functional_passes_or_whole_task_acceptance(self):
        text=SCRIPT.read_text()
        self.assertIn("'whole_module_acceptance': False",text)
        self.assertIn("row['requested_task_acceptance']=False",text)
        self.assertIn('not proof of feature absence',text)
        self.assertIn("h.resolve('md.obsidian')",text)
        self.assertIn('document.querySelectorAll(".search-result-file-match")',text)
        self.assertNotIn("row['status']='passed'",text)

    def test_landscape_input_cannot_fall_back_to_the_ime_or_another_field(self):
        text=SCRIPT.read_text()
        self.assertIn("n.get('package')=='app.space.notes'",text)
        self.assertIn('no positional/IME fallback',text)
        self.assertNotIn('candidates[:1]',text)
        self.assertIn('Typed value not read back in the intended visible editor',text)
        self.assertIn('from office_fixtures import write_office',text)
        self.assertIn("OUT/'public-fixtures'",text)

    def test_anchored_command_id_matches_without_appending_the_human_name(self):
        import re
        from types import SimpleNamespace
        tree=ast.parse(SCRIPT.read_text());function=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name=='command')
        commands=[{'id':'global-search:open','name':'Search: Search in all files'}]
        device=SimpleNamespace(js=lambda expression:commands if expression.startswith('Object.values') else True)
        namespace={'D':device,'re':re,'json':__import__('json'),'time':SimpleNamespace(sleep=lambda _:None)}
        exec(compile(ast.Module(body=[function],type_ignores=[]),'command-unit','exec'),namespace)
        self.assertEqual(namespace['command']('^global-search:open$'),commands[0])

    def test_cleanup_cancels_only_known_external_choosers_not_app_errors(self):
        from types import SimpleNamespace
        tree=ast.parse(SCRIPT.read_text());function=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name=='cancel_public_session_picker')
        back=[];nodes=[{'package':'android','text':'Obsidian is not responding'},{'package':'android','text':'Close app'}]
        namespace={'h':SimpleNamespace(tree=lambda:(b'',nodes),back=lambda:back.append(True))}
        exec(compile(ast.Module(body=[function],type_ignores=[]),'cleanup-unit','exec'),namespace)
        self.assertFalse(namespace['cancel_public_session_picker']()['cancelled']);self.assertFalse(back)
        nodes[:]=[{'package':'com.google.android.documentsui','text':'Recent images'}]
        self.assertTrue(namespace['cancel_public_session_picker']()['cancelled']);self.assertEqual(len(back),1)
        nodes[:]=[{'package':'android','text':text} for text in ['Share','Just once','Always']]
        self.assertTrue(namespace['cancel_public_session_picker']()['cancelled']);self.assertEqual(len(back),2)

    def test_hidden_ime_view_does_not_generate_a_back_press(self):
        from types import SimpleNamespace
        import re,tempfile
        tree=ast.parse(SCRIPT.read_text());function=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name=='hide_ime')
        back=[];commands=[]
        def adb(*args):
            commands.append(args)
            return b'mInputShown=false\nmIsInputViewShown=true\n'
        with tempfile.TemporaryDirectory() as directory:
            namespace={'h':SimpleNamespace(adb=adb,back=lambda:back.append(True),COMMANDS=commands),'OUT':Path(directory),'re':re}
            exec(compile(ast.Module(body=[function],type_ignores=[]),'ime-unit','exec'),namespace)
            namespace['hide_ime']()
            self.assertEqual(back,[])
            self.assertEqual(len(list(Path(directory).glob('input-method-*.txt'))),1)

    def test_short_sessions_cover_exactly37_families_without_overlap(self):
        workflow=yaml.safe_load(Path('.github/workflows/notes-comparison37-shards.yml').read_text())
        rows=workflow['jobs']['compare']['strategy']['matrix']['include']
        self.assertEqual(len(rows),20)
        for profile in {r['id'] for r in rows}:
            numbers=[]
            for row in rows:
                if row['id']==profile:
                    first,last=map(int,row['range'].split('-'));numbers.extend(range(first,last+1))
            self.assertEqual(sorted(numbers),list(range(1,38)))
            self.assertEqual(len(numbers),len(set(numbers)))
        self.assertIn('fresh_guest_chunk_not_long_session_reliability',SCRIPT.read_text())


class NotificationIsolationTest(unittest.TestCase):
    def test_only_exact_notification_request_is_denied_after_original_capture(self):
        from types import SimpleNamespace
        function=next(n for n in ast.parse(SCRIPT.read_text()).body if isinstance(n,ast.FunctionDef) and n.name=='cancel_public_session_picker')
        package='com.google.android.permissioncontroller'
        nodes=[];events=[]
        def capture(*args):events.append(('capture',args))
        def tap(node):events.append(('deny',node));nodes.clear()
        helper=SimpleNamespace(tree=lambda:(b'',nodes),SNAPSHOTS=[],capture=capture,tap_node=tap,back=lambda:events.append(('back',)))
        namespace={'h':helper,'time':SimpleNamespace(sleep=lambda _:None)}
        exec(compile(ast.Module(body=[function],type_ignores=[]),'notification-isolation','exec'),namespace)
        cleanup=namespace['cancel_public_session_picker']
        button={'package':package,'resource-id':'com.android.permissioncontroller:id/permission_deny_button','enabled':'true','clickable':'true'}
        for message in ['Allow MyNotes to access your camera?','Allow Obsidian to send you notifications?','MyNotes is not responding']:
            nodes[:]=[{'package':package,'text':message},button]
            self.assertFalse(cleanup()['cancelled']);self.assertEqual(events,[])
        nodes[:]=[{'package':package,'text':'Allow MyNotes to send you notifications?'},button]
        result=cleanup()
        self.assertEqual(result['decision'],'denied')
        self.assertEqual([e[0] for e in events],['capture','deny'])
        self.assertIn('not reminder delivery',result['purpose'])

    def test_failed_original_capture_must_not_consume_the_prompt(self):
        from types import SimpleNamespace
        function=next(n for n in ast.parse(SCRIPT.read_text()).body if isinstance(n,ast.FunctionDef) and n.name=='cancel_public_session_picker')
        package='com.google.android.permissioncontroller';actions=[]
        nodes=[{'package':package,'text':'Allow MyNotes to send you notifications?'},{'package':package,'resource-id':'com.android.permissioncontroller:id/permission_deny_button','enabled':'true','clickable':'true'}]
        def capture(*args):raise RuntimeError('capture failed')
        helper=SimpleNamespace(tree=lambda:(b'',nodes),SNAPSHOTS=[],capture=capture,tap_node=lambda _:actions.append('deny'),back=lambda:actions.append('back'))
        namespace={'h':helper,'time':SimpleNamespace(sleep=lambda _:None)}
        exec(compile(ast.Module(body=[function],type_ignores=[]),'notification-isolation','exec'),namespace)
        with self.assertRaisesRegex(RuntimeError,'capture failed'):namespace['cancel_public_session_picker']()
        self.assertEqual(actions,[])


class PickerNavigationTest(unittest.TestCase):
    def helper(self):
        fn=next(n for n in ast.parse(SCRIPT.read_text()).body if isinstance(n,ast.FunctionDef) and n.name=='picker_target')
        namespace={};exec(compile(ast.Module(body=[fn],type_ignores=[]),'picker-navigation','exec'),namespace)
        return namespace['picker_target']

    def test_breadcrumb_is_not_a_folder_or_file_target(self):
        target=self.helper();package='com.google.android.documentsui'
        breadcrumb={'package':package,'text':'Comparison37','resource-id':package+':id/breadcrumb_text'}
        self.assertEqual(target([breadcrumb],'Comparison.pdf'),('wait',None))
        folder={'package':package,'text':'Comparison37','resource-id':'android:id/title'}
        self.assertEqual(target([breadcrumb,folder],'Comparison.pdf'),('folder',folder))
        file={'package':package,'text':'Comparison.pdf','resource-id':'android:id/title'}
        self.assertEqual(target([breadcrumb,folder,file],'Comparison.pdf'),('file',file))
        file['enabled']='false'
        self.assertEqual(target([breadcrumb,file],'Comparison.pdf'),('wait',None))

    def test_nonprovider_and_toolbar_titles_are_not_download_navigation(self):
        target=self.helper()
        self.assertEqual(target([{'package':'app.space.notes','text':'Comparison.pdf'}],'Comparison.pdf'),('wait',None))
        self.assertEqual(target([{'package':'com.google.android.documentsui','text':'Downloads','resource-id':''}],'Comparison.pdf'),('wait',None))

    def test_rotation_uses_natural_display_not_override_size(self):
        import re
        from types import SimpleNamespace
        fn=next(n for n in ast.parse(SCRIPT.read_text()).body if isinstance(n,ast.FunctionDef) and n.name=='restore_requested_orientation')
        for size,orientation,expected in [('1080x1920','landscape',1),('1080x1920','portrait',0),('1920x1080','portrait',1)]:
            commands=[]
            def adb(*args):
                commands.append(args);return ('Physical size: '+size+'\nOverride size: 200x100').encode()
            namespace={'h':SimpleNamespace(adb=adb),'os':SimpleNamespace(environ={'COMPARISON_ORIENTATION':orientation}),'re':re,'time':SimpleNamespace(sleep=lambda _:None)}
            exec(compile(ast.Module(body=[fn],type_ignores=[]),'rotation-reapplication','exec'),namespace)
            self.assertEqual(namespace['restore_requested_orientation']()['rotation'],expected)
            self.assertIn(('shell','wm','user-rotation','lock',str(expected)),commands)


class PublicRouteRecoveryTest(unittest.TestCase):
    def function(self,name,namespace):
        fn=next(n for n in ast.parse(SCRIPT.read_text()).body if isinstance(n,ast.FunctionDef) and n.name==name)
        exec(compile(ast.Module(body=[fn],type_ignores=[]),'public-route','exec'),namespace)
        return namespace[name]

    def test_picker_uses_smallest_actual_clickable_containing_item(self):
        import re
        target=self.function('picker_click_target',{'re':re})
        label={'package':'com.google.android.documentsui','bounds':'[213,739][489,796]'}
        parent={'package':label['package'],'bounds':'[72,696][528,840]','resource-id':label['package']+':id/item_root','clickable':'true','enabled':'true'}
        wrong=dict(parent,bounds='[600,696][900,840]')
        self.assertIs(target([wrong,parent],label),parent)
        self.assertIs(target([wrong],label),label)

    def test_scroll_is_bounded_to_one_native_container(self):
        import re
        from types import SimpleNamespace
        from unittest.mock import Mock
        control={'text':'Settings'}
        tap=Mock(side_effect=[RuntimeError('No visible enabled exact-label control: Settings'),control])
        commands=[]
        node={'package':'app.space.notes','class':'android.widget.ScrollView','scrollable':'true','bounds':'[168,511][1752,960]'}
        h=SimpleNamespace(tree=lambda:(b'',[node]),adb=lambda *args:commands.append(args))
        fn=self.function('tap_with_public_scroll',{'ORIGINAL_TAP':tap,'h':h,'re':re,'time':SimpleNamespace(sleep=lambda _:None)})
        self.assertIs(fn('Settings'),control)
        self.assertEqual(commands,[('shell','input','swipe','960','847','960','623','350')])

    def test_errors_and_permission_surfaces_are_never_swiped_away(self):
        import re
        from types import SimpleNamespace
        from unittest.mock import Mock
        for message in ['device offline','No visible enabled exact-label control: Settings']:
            tap=Mock(side_effect=RuntimeError(message));commands=[]
            h=SimpleNamespace(tree=lambda:(b'',[{'package':'com.google.android.permissioncontroller','class':'android.widget.ScrollView','scrollable':'true','bounds':'[0,0][1080,1920]'}]),adb=lambda *a:commands.append(a))
            fn=self.function('tap_with_public_scroll',{'ORIGINAL_TAP':tap,'h':h,'re':re,'time':SimpleNamespace(sleep=lambda _:None)})
            with self.assertRaises(RuntimeError):fn('Settings')
            self.assertEqual(commands,[])
