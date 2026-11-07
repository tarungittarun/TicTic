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
