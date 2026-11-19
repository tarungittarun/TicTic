"""Structural guardrails. Runtime behaviour is covered separately in Android journeys."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'runtime/src/main/java/app/space/notes/runtime'

class CoreNotesContracts(unittest.TestCase):
    def test_schema_keeps_legacy_v6_and_explicitly_versions_canonical_content(self):
        s=(JAVA/'DocumentCodec.java').read_text()
        self.assertIn("int schema=6",s)
        self.assertIn("schema=Math.max(schema,7)",s)
        self.assertIn("schema=Math.max(schema,11)",s)
        self.assertIn("schema=Math.max(schema,12)",s)
        self.assertIn("schema=Math.max(schema,13)",s)
        self.assertIn('put("version",schema)',s)
        self.assertIn("version>13",s)
        self.assertIn('"original"',s)
        self.assertIn("version>=7 && n.has(\"content\")",s)
        for field in ['items','marks','labels','hideChecked']:
            self.assertIn('"'+field+'"',s)

    def test_board_is_virtualised_masonry(self):
        s=(JAVA/'NotesBoard.java').read_text()
        self.assertIn('extends RecyclerView',s)
        self.assertIn('new StaggeredGridLayoutManager(columns',s)
        self.assertIn('setFullSpan',s)
        self.assertIn('setMaxLines(7)',s)

    def test_editor_uses_native_spans_and_caret(self):
        s=(JAVA/'TextStyles.java').read_text()
        for token in ['StyleSpan','UnderlineSpan','StrikethroughSpan','getSpanStart','SPAN_EXCLUSIVE_EXCLUSIVE']:
            self.assertIn(token,s)
        c=(JAVA/'NotesController.java').read_text()
        self.assertIn('body.getSelectionStart()',c)
        self.assertIn('body.setSelection(anchor,focus)',c)

    def test_rotation_snapshot_is_bounded(self):
        self.assertIn('draft.length<=32768',(JAVA/'NotesController.java').read_text())

    def test_ocr_is_local_reviewed_and_saved_text_is_searchable(self):
        ocr=(JAVA/'OfflineOcr.java').read_text()
        for token in ['requireLocal(source)','getContentResolver().openInputStream(source)','context.getAssets().open("ocr/"+name)','new TessBaseAPI()']:
            self.assertIn(token,ocr)
        controller=(JAVA/'NotesController.java').read_text()
        save=controller.split('private void saveOcrText',1)[1].split('public void startVoiceCapture',1)[0]
        self.assertIn('if(value.trim().isEmpty())',save)
        self.assertIn('current.content.insert(',save)
        self.assertIn('current=current.rich(',save)
        android=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
        journey=android.split('public void ocrReviewRequiresNonblankTextAndCancelNeverChangesTheOriginal()',1)[1].split('\n    }',1)[0]
        self.assertIn('appended.matches("Reviewed line")',journey)
        self.assertIn('created.matches("reviewed OCR phrase")',journey)
        acceptance=(ROOT/'docs/CHECKLIST-ACCEPTANCE.md').read_text()
        self.assertIn('AT-K042.2 — Save and search',acceptance)
        self.assertIn('not evidence of ML Kit parity',acceptance)

    def test_edited_timestamp_is_a_live_bottom_row_in_both_editors(self):
        controller=(JAVA/'NotesController.java').read_text()
        legacy=controller.split('private void renderEditor()',1)[1].split('private void reflowEditorDock',1)[0]
        self.assertIn('editedTimestampView.setTag("edited-timestamp")',legacy)
        self.assertIn('editor.addView(editedTimestampView,new LinearLayout.LayoutParams(-1,-2))',legacy)
        self.assertIn('notes.set(i,n);refreshEditedTimestamp(n);return;',controller)
        self.assertIn('if(canonicalEditor!=null)canonicalEditor.refreshEditedTimestamp()',controller)
        document=(JAVA/'DocumentEditorView.java').read_text()
        self.assertIn('editedTimestamp=design.label(edited,12,design.muted)',document)
        self.assertIn('content.addView(editedTimestamp,new LayoutParams(-1,-2))',document)
        self.assertIn('host.current().updatedAt',document)
        android=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
        self.assertIn('editedTimestampFooterTracksSavedEditsAcrossNotesDocsAndTrash',android)
        self.assertIn('AT-K043.3 — Trash and accessibility',(ROOT/'docs/CHECKLIST-ACCEPTANCE.md').read_text())

    def test_app_font_size_is_persisted_and_applied_without_mutating_base_configuration(self):
        size=(JAVA/'NotesTextSize.java').read_text()
        for token in ['SMALL="small"','MEDIUM="medium"','LARGE="large"','return .90f','return 1.15f','edit().putString(KEY,value).commit()','new Configuration(base.getResources().getConfiguration())','configuration.fontScale*=multiplier','base.createConfigurationContext(configuration)']:
            self.assertIn(token,size)
        activity=(ROOT/'app/src/main/java/app/space/notes/NotesActivity.java').read_text()
        self.assertIn('super.attachBaseContext(app.space.notes.runtime.NotesTextSize.wrap(base))',activity)
        self.assertIn('public void textSizeChanged(){recreate();}',activity)
        controller=(JAVA/'NotesController.java').read_text()
        self.assertIn('settingRow(content,"Font size",NotesTextSize.label(context)',controller)
        self.assertIn('choiceSheet("Font size",new String[]{"Small","Medium","Large"}',controller)
        self.assertIn('host.textSizeChanged()',controller)
        self.assertIn('design.entry(body,18)',controller)
        design=(JAVA/'NotesDesign.java').read_text()
        self.assertIn('v.setTextSize(sp)',design)
        board=(JAVA/'NotesBoard.java').read_text()
        self.assertIn('private TextView label(int size){TextView v=new TextView(getContext());v.setTextSize(size);return v;}',board)
        editor=(JAVA/'DocumentEditorView.java').read_text()
        self.assertIn('title.setTextSize(26)',editor)
        journey=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
        self.assertIn('appFontSizeChooserPersistsAndCombinesWithAndroidScaleWithoutChangingNotes',journey)
        self.assertIn('new ContextWrapper(context)',journey)
        self.assertIn('Board-card preview text follows app scale',journey)
        self.assertIn('Editor text follows the same app scale',journey)
        self.assertIn('tap("Font size");tap("Cancel")',journey)
        self.assertIn('AT-K044.3 — Cancel and responsive layouts',(ROOT/'docs/CHECKLIST-ACCEPTANCE.md').read_text())

    def test_all_twelve_colours_and_no_extra_app_imports(self):
        s=(JAVA/'NotesBoard.java').read_text()
        for name in ['Default','Red','Orange','Yellow','Green','Teal','Blue','Dark blue','Purple','Pink','Brown','Gray']:
            self.assertIn('"'+name+'"',s)
        self.assertNotIn('app.space.feature.',s)

    def test_checklist_picker_offers_default_plus_eleven_named_colours(self):
        model=(ROOT/'domain/src/main/java/app/space/notes/domain/ChecklistAppearance.java').read_text()
        picker=(JAVA/'ChecklistAppearancePicker.java').read_text()
        compact_model=''.join(model.split())
        compact_picker=''.join(picker.split())
        self.assertIn('NAMED_THEME_COLOR_COUNT=11',compact_model)
        self.assertIn('THEME_COLOR_COUNT=NAMED_THEME_COLOR_COUNT+1',compact_model)
        self.assertIn('DEFAULT_THEME_COLOR=THEME_COLOR_COUNT',compact_model)
        self.assertIn('themeColor<1||themeColor>THEME_COLOR_COUNT',compact_model)
        self.assertIn('Arrays.copyOfRange(NotesBoard.NAMES,1,NotesBoard.NAMES.length)',compact_picker)
        self.assertIn('String[]colors=newString[palette.length+1]',compact_picker)
        self.assertIn('index==0?ChecklistAppearance.DEFAULT_THEME_COLOR:index',compact_picker)
        self.assertIn('i==0?ChecklistAppearance.DEFAULT_THEME_COLOR:i',compact_picker)
