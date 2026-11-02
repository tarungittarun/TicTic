from pathlib import Path
import sqlite3,unicodedata,unittest
ROOT=Path(__file__).resolve().parents[1]
def source(name):return (ROOT/'runtime/src/main/java/app/space/notes/runtime'/name).read_text()
class ViewerSearchDiagnosticsContracts(unittest.TestCase):
 def test_spreadsheet_view_precedes_limited_editable_import(self):
  s=source('NotesController.java');self.assertLess(s.index('SpreadsheetFile.open('),s.index('LocalOpenFile.Text result='));self.assertIn('new SpreadsheetPreview(',s)
  grid=source('SpreadsheetPreview.java');self.assertIn('RecyclerView',grid);self.assertIn('horizontal.getScrollX()',grid);self.assertIn('Copy cached value',grid)
  model=source('SpreadsheetFile.java');self.assertIn('MAX_CELLS=250000,MAX_TEXT=8000000',model);self.assertIn('relationships.get(id)',model);self.assertIn('sheet.rows.computeIfAbsent',model)
 def test_original_copy_checks_physical_alias_before_truncation(self):
  s=source('ViewedFileCopy.java');self.assertLess(s.index('from.st_ino==to.st_ino'),s.index('openOutputStream(destination,"wt")'));self.assertIn('OsConstants.S_ISREG',s);self.assertNotIn('delete(',s)
 def test_pdf_owns_both_workers_and_bounded_scratch(self):
  s=source('LocalPdfPreview.java');self.assertIn('LinearLayoutManager(context)',s);self.assertIn('setupTempFileOnly(64L*1024*1024)',s);self.assertIn('rendererClosed && (!textClaimed || textClosed)',s);self.assertIn('volatile int token',s);self.assertIn('indexText(index)',s)
 def test_user_data_precedes_viewed_files_and_settings(self):
  s=source('NotesController.java');a=s.index('private void renderUniversalResults(');b=s.index('private void rememberViewed(',a);s=s[a:b]
  self.assertLess(s.index('Saved notes & Docs'),s.index('Viewed files'));self.assertLess(s.index('Viewed files'),s.index('Settings'));self.assertIn('partial text',source('NotesController.java'))
 def test_word_original_layout_is_not_falsely_called_internal_fidelity(self):
  s=source('NotesController.java');self.assertIn('Word file · choose a view',s);self.assertIn('does not preserve full Word layout/media',s);self.assertLess(s.index('Word file · choose a view'),s.index('LocalOpenFile.Text result='))
 def test_log_rotation_and_parsed_filters_have_explicit_limits(self):
  s=source('Diagnostics.java');self.assertIn('LIMIT=1024*1024',s);self.assertIn('"level",level(event)',s);self.assertIn('at>=from && at<=to && levels.contains(severity)',s);self.assertIn('Math.min(5000,total)',s)
  view=source('DiagnosticLogView.java');self.assertIn('Custom local date/time range',view);self.assertIn('Save filtered logs',view);self.assertIn('new LogAdapter()',view)
 def test_viewed_index_is_bounded_and_never_an_authority(self):
  s=source('ViewedFiles.java');self.assertIn('MAX_FILES=32,MAX_TEXT=2*1024*1024',s);self.assertIn('tokenchars=',s);self.assertIn("append('*').append",s);self.assertIn('partial || body.length()>MAX_TEXT',s)
 def test_real_fts4_prefixes_preserve_indic_combining_marks(self):
  marks=''.join(chr(i) for i in range(65535) if unicodedata.category(chr(i)).startswith('M'))
  with sqlite3.connect(':memory:') as db:
   db.execute("create virtual table words using fts4(name,body,tokenize=unicode61 'tokenchars="+marks+"')");db.execute('insert into words values(?,?)',('Local fixture','notes ಭಾರತ हिंदी 😊 %'))
   for term in ('not','ಭಾ','हिं'):self.assertEqual(1,len(db.execute('select * from words where words match ?',('"'+term+'*"',)).fetchall()))
 def test_literal_emoji_and_percent_do_not_become_sql_wildcards(self):
  with sqlite3.connect(':memory:') as db:
   db.execute('create virtual table words using fts4(name,body)');db.executemany('insert into words values(?,?)',[('one','😊 25%'),('two','ordinary')])
   for term in ('😊','%'):
    like='%'+term.replace('^','^^').replace('%','^%').replace('_','^_')+'%';self.assertEqual(1,len(db.execute("select * from words where body like ? escape '^'",(like,)).fetchall()))
