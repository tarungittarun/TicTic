"""Source boundaries complement—not replace—native interaction and visual review."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'runtime/src/main/java/app/space/notes/runtime'
class EditorCoherence(unittest.TestCase):
 def test_find_stays_inside_editor_and_keeps_cancellable_work(self):
  s=(JAVA/'NoteFindDialog.java').read_text()
  for token in ['owner.addView(bar','Previous match','Next match','Close find','task.cancel(true)','bar.addOnAttachStateChangeListener(this)','snapshot.set(i,targets.get(i).getText().toString())']:
   self.assertIn(token,s)
  self.assertNotIn('AlertDialog',s)
  self.assertNotIn('setSpan(',s)
 def test_tools_have_visible_labels_and_shared_outline_symbols(self):
  s=(JAVA/'NotesDesign.java').read_text()
  for token in ['editorTool(String text,String name,String symbol','b.setText(text)','b.setTextSize(11)','b.setMinHeight(dp(60))','actionRow(String name','EditorActions.symbol(name)']:
   self.assertIn(token,s)
 def test_document_tools_use_anchored_trays_not_centered_list_screens(self):
  s=(JAVA/'DocumentEditorView.java').read_text()
  self.assertIn('design.showEditorSheet(menu)',s)
  self.assertIn('design.actionRow(labels[i]',s)
  self.assertNotIn('.setItems(labels',s)
  self.assertNotIn('button("⋮⋮"',s)
 def test_system_date_and_time_pickers_are_not_replaced(self):
  s=(JAVA/'NotesController.java').read_text()
  self.assertIn('new android.app.DatePickerDialog',s)
  self.assertIn('new android.app.TimePickerDialog',s)
  self.assertIn('design.show(date)',s)
 def test_back_closes_find_before_leaving_document(self):
  s=(JAVA/'NotesController.java').read_text().split('public boolean back()',1)[1]
  self.assertLess(s.index('NoteFindDialog.close'),s.index('persistDraft(); renderHome()'))

 def test_conversion_is_in_insert_not_note_management(self):
  s=(JAVA/'NotesController.java').read_text();editor=s.split('private void renderEditor()',1)[1].split('private void buildFormatting',1)[0]
  self.assertIn('captureGrid(capture,new String[]{"Convert to checklist","Hyperlink","Reminder"}',editor)
  management=editor.split('LinearLayout actions=menu("Note actions"',1)[1]
  self.assertNotIn('new String[]{"Convert to checklist"}',management)
 def test_checked_rows_expand_per_list_and_remain_visible_in_read_only_trash(self):
  canonical=(JAVA/'DocumentEditorView.java').read_text().split('private void renderChecklist',1)[1].split('private void accessibleRowMove',1)[0]
  self.assertIn('boolean collapseChecked=block.hideChecked && !readOnly()',canonical)
  self.assertIn('if(collapseChecked && checkedCount>0)',canonical)
  self.assertIn('footer.setContentDescription("Show "+label)',canonical)
  legacy=(JAVA/'NotesController.java').read_text().split('private void renderEditor()',1)[1].split('private void reflowEditorDock',1)[0]
  self.assertIn('if(!trashed && n.hideChecked && item.checked)',legacy)
  self.assertIn('!trashed && n.hideChecked && checked>0?View.VISIBLE:View.GONE',legacy)
  android=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
  self.assertIn('canonicalHideCheckedFooterExpandsAndPersistsPerList',android)
  self.assertIn('hiddenCheckedFooterUsesAccuratePluralCountInBothEditors',android)
  self.assertIn('trashedChecklistsShowCheckedRowsWithoutAnExpansionControl',android)
 def test_checked_rows_dim_without_automatic_strikethrough(self):
  canonical=(JAVA/'DocumentEditorView.java').read_text().split('private void renderChecklist',1)[1].split('private void accessibleRowMove',1)[0]
  self.assertIn('rowText.setTextColor(item.checked?design.muted:design.ink)',canonical)
  self.assertIn('rowText.setTextColor(on?design.muted:design.ink)',canonical)
  self.assertNotIn('STRIKE_THRU_TEXT_FLAG',canonical)
  legacy=(JAVA/'NotesController.java').read_text().split('private void renderEditor()',1)[1].split('private void reflowEditorDock',1)[0]
  self.assertIn('input.setTextColor(item.checked?muted:ink)',legacy)
  self.assertIn('input.setTextColor(value?muted:ink)',legacy)
  self.assertNotIn('input.setPaintFlags(item.checked?',legacy)
  self.assertNotIn('input.setPaintFlags(value?',legacy)
  android=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
  self.assertIn('checklistToggleKeepsItemTextPlainAndPersistsForNotesAndDocs',android)
  self.assertIn('Read-only canonical checklist does not offer expansion',android)
 def test_checked_and_unchecked_rows_can_reorder_with_drag_and_accessibility(self):
  legacy=(JAVA/'NotesController.java').read_text().split('private Button checklistHandle',1)[1].split('private ChecklistDrag checklistDrag',1)[0]
  self.assertIn('handle.startDragAndDrop',legacy)
  self.assertIn('ChecklistOrder.move(note.items,itemId,insertion)',(JAVA/'NotesController.java').read_text())
  canonical=(JAVA/'DocumentEditorView.java').read_text().split('private void renderChecklist',1)[1].split('private void accessibleRowMove',1)[0]
  self.assertIn('new RowDrag(block.id,item.id)',canonical)
  self.assertIn('ChecklistOrder.move(list.items,drag.item',canonical)
  self.assertIn('if(!block.id.equals(drag.block))return false',canonical)
  self.assertIn('if(!readOnly())',canonical)
  self.assertIn('if(!trashed)line.addView(checklistHandle(n.id,item.id', (JAVA/'NotesController.java').read_text())
  self.assertIn('"Move item earlier"',canonical)
  self.assertIn('"Move item later"',canonical)
  android=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
  self.assertIn('nativeChecklistHandleDragMovesTheItemWithoutEditingItsText',android)
  self.assertIn('nativeLegacyChecklistDrag("second","first",false)',android)
  self.assertIn('nativeDocumentDrag("row-handle:second","row-handle:third",false)',android)
  self.assertIn('Unchecked row can be dragged too',android)
  self.assertIn('Read-only legacy Trash has no reorder handle',android)
  self.assertIn('Read-only Trash has no row reorder handle',android)
  self.assertIn('Move item earlier',android)
  domain=(ROOT/'domain/src/test/java/app/space/notes/domain/ChecklistOrderTest.java').read_text()
  self.assertIn('movePreservesUncheckedTextDepthAndIdentity',domain)
  self.assertIn('AT-K046.2 — Persistence and Undo',(ROOT/'docs/CHECKLIST-ACCEPTANCE.md').read_text())
 def test_checklist_nesting_depth_is_bounded_and_rendered_as_inset(self):
  legacySource=(JAVA/'NotesController.java').read_text()
  legacy=legacySource.split('private void renderEditor()',1)[1].split('private void reflowEditorDock',1)[0]
  self.assertIn('line.setPadding(dp(item.depth*16),0,0,0)',legacy)
  self.assertIn('boolean[] allowed={item.depth<3,item.depth>0,true}',legacySource)
  self.assertIn('rows.get(index).depth>=3',legacySource)
  self.assertIn('rows.get(index).depth==0',legacySource)
  canonicalSource=(JAVA/'DocumentEditorView.java').read_text()
  canonical=canonicalSource.split('private void renderChecklist',1)[1].split('private void renderTable',1)[0]
  self.assertIn('line.setPadding(dp(item.depth*16),0,0,0)',canonical)
  self.assertIn('row.change(row.text,row.checked,Math.max(0,Math.min(3,row.depth+(choice==0?1:-1))))',canonical)
  menu=canonicalSource.split('private void rowMenu',1)[1].split('private void renderTable',1)[0]
  self.assertIn('boolean[] enabled={depth<3,depth>0,true}',menu)
  self.assertIn('choose("List item",actions,enabled',menu)
  choices=canonicalSource.split('private void choose(String title,String[] labels,boolean[] enabled',1)[1]
  self.assertIn('if(enabled!=null && !enabled[i])option.setEnabled(false)',choices)
  domain=(ROOT/'domain/src/main/java/app/space/notes/domain/Note.java').read_text()
  self.assertIn('depth<0 || depth>3',domain)
  android=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
  self.assertIn('legacyChecklistIndentOutdentPersistsNestingAndEnforcesDepthLimits',android)
  self.assertIn('canonicalChecklistIndentOutdentPersistsNestingAndDisablesDepthLimits',android)
  self.assertIn('Read-only legacy Trash has no item actions',android)
  self.assertIn('Read-only Docs Trash has no item actions',android)
  self.assertIn('AT-K047.2 — Visible inset, persistence, and Undo',(ROOT/'docs/CHECKLIST-ACCEPTANCE.md').read_text())
 def test_checklist_enter_splits_both_editors_and_prunes_only_when_leaving(self):
  controller=(JAVA/'NotesController.java').read_text()
  document=(JAVA/'DocumentEditorView.java').read_text()
  enter=(JAVA/'ChecklistEnter.java').read_text()
  domain=(ROOT/'domain/src/main/java/app/space/notes/domain/ChecklistEditing.java').read_text()
  self.assertIn('ChecklistEnter.install(input,(original,start,end)->splitLegacyChecklistRow',controller)
  self.assertIn('ChecklistEditing.afterEnter(current.items,rowId,start,end)',controller)
  self.assertIn('persistDraft();discardTrailingBlankChecklistRows();hideEditorKeyboard()',controller)
  self.assertIn('note.deletedAt>0 || page.equals("Trash")',controller)
  self.assertIn('persistDraft();discardTrailingBlankChecklistRows();voicePage=true',controller)
  self.assertIn('if(item!=null && block!=null && !readOnly()){input.emptyBackspace=',document)
  self.assertIn('ChecklistEnter.install(input,(original,start,end)->splitChecklistRow',document)
  self.assertIn('ChecklistEditing.afterEnter(list.items,itemId,start,end)',document)
  self.assertIn('EditorInfo.IME_ACTION_NEXT',enter)
  self.assertIn('KeyEvent.KEYCODE_ENTER',enter)
  self.assertIn('withoutTrailingBlankRows(DocumentContent content)',domain)
  self.assertIn('withoutTrailingBlankRows(List<Note.Item> rows)',domain)
  journeys=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
  for scenario in ['legacyChecklistEnterSplitsAndUndoRestoresTheOriginalRowAfterReopen','canonicalChecklistEnterSplitsRichRowAndUndoRestoresItAfterReopen','trailingBlankChecklistRowsAreRemovedOnExitInNotesAndDocs','readOnlyTrashChecklistEnterDoesNotSplitOrPruneRows']:
   self.assertIn(scenario,journeys)
  self.assertIn('AT-K048.1 — Split at the caret',(ROOT/'docs/CHECKLIST-ACCEPTANCE.md').read_text())
 def test_find_compacts_without_hiding_a_live_result(self):
  s=(JAVA/'NoteFindDialog.java').read_text()
  self.assertIn('owner.addOnLayoutChangeListener(resizeListener)',s)
  self.assertIn('statusRow.setVisibility(matches.isEmpty()?View.GONE:View.VISIBLE)',s)
  self.assertIn('owner.removeOnLayoutChangeListener(resizeListener)',s)
 def test_basic_document_formatting_is_nonmodal_and_docked(self):
  s=(JAVA/'DocumentEditorView.java').read_text();part=s.split('private void formatMenu()',1)[1].split('private String fontLabel',1)[0]
  self.assertIn('InlineFormatTray dialog',part)
  self.assertNotIn('AlertDialog',part)
  self.assertIn('tray.setTag("document-formatting")',s)

 def test_content_removal_is_last_and_rename_edit_are_direct(self):
  s=(JAVA/'DocumentEditorView.java').read_text().split('private void blockMenu',1)[1].split('private void moveMenu',1)[0]
  self.assertLess(s.index('options.add("Edit")'),s.index('options.add("Rename")'))
  self.assertLess(s.index('options.add("Rename")'),s.index('options.add("Remove section")'))
  self.assertIn('choose(title,options.toArray',s)

 def test_properties_keep_editor_and_have_fixed_actions_outside_scroll(self):
  s=(JAVA/'NotesController.java').read_text()
  for name,end in [('renderLabels','manageLabels'),('renderColours','drawingDraft'),('renderBackgrounds','renderFolders')]:
   part=s.split('private void '+name+'()',1)[1].split('private ',1)[0]
   self.assertNotIn('clearPage()',part)
   self.assertIn('sheets.panel(',part)
  sheets=(JAVA/'NotesSheets.java').read_text()
  self.assertIn('footers.get(panel)',sheets)
  self.assertIn('surface.addView(currentFooter',sheets)
  self.assertIn('footers.clear()',sheets)
 def test_basic_styles_can_be_combined_in_one_open_ribbon(self):
  s=(JAVA/'DocumentEditorView.java').read_text()
  part=(JAVA/'FormatControls.java').read_text().split('String[] types={"bold"',1)[1].split('panel.addView(design.label("Paragraph"',1)[0]
  self.assertNotIn('dialog.dismiss()',part)
  self.assertIn('toggles.put(type,control)',part)
  self.assertIn('refreshFormatRibbon()',s)

 def test_footer_reflows_by_measured_scaled_words_not_smaller_font(self):
  s=(JAVA/'NotesController.java').read_text()
  self.assertIn('button.getPaint().measureText(button.getText().toString())',s)
  self.assertIn('reflowEditorDock(dock)',s)
  design=(JAVA/'NotesDesign.java').read_text().split('public Button editorTool',1)[1].split('public Button actionRow',1)[0]
  self.assertIn('b.setMaxLines(1)',design)
  self.assertNotIn('setTextScaleX',design)

 def test_folder_move_is_anchored_and_late_disk_results_cannot_reopen_it(self):
  s=(JAVA/'NotesController.java').read_text()
  part=s.split('private void renderMoveFolderSheet()',1)[1].split('private void renderFolders(boolean moving,Runnable parent)',1)[0]
  self.assertNotIn('clearPage()',part)
  self.assertNotIn('title=null',part)
  self.assertIn('sheets.footer(panel,button("Cancel",()->sheets.close()))',part)
  self.assertIn('token!=folderPageSequence || !panel.isShown()',part)
  self.assertIn('loadMoveFolders(panel,content,chosen)',part)
  self.assertIn('design.showEditorSheet(dialog)',part)

 def test_colour_targets_fit_the_short_landscape_scroll_view_without_smaller_text(self):
  s=(JAVA/'NotesController.java').read_text().split('private void renderColours()',1)[1].split('private java.io.File drawingDraft',1)[0]
  self.assertIn('pick.setMinHeight(dp(72))',s)
  self.assertIn('pick.setPadding(dp(4),dp(4),dp(4),dp(4))',s)
  self.assertIn('pick.setTextSize(13)',s)
  native=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
  self.assertIn('visible.height()>=text("Reminder").getHeight()',native)
  self.assertIn('visible.width()>=text("Reminder").getWidth()',native)

 def test_one_shot_panels_release_hidden_inputs_and_fixed_footers(self):
  s=(JAVA/'NotesSheets.java').read_text()
  self.assertIn('if(current!=null && current!=panel)close()',s)
  self.assertIn('current.getTag()==null',s)
  self.assertIn('panels.remove(current);footers.remove(current)',s)

 def test_real_provider_journey_cannot_click_archive_behind_the_folder_tray(self):
  s=(ROOT/'app/src/androidTest/java/app/space/notes/RealFolderStorageTest.java').read_text()
  self.assertIn('View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)return null',s)
  self.assertIn('Confirmed move must empty the old real provider folder',s)
  self.assertIn('tapFolderDestination(controller,"Archive")',s)
  self.assertIn('Destination belongs to the active tray',s)
  native=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
  self.assertIn('controller.view().isAttachedToWindow() && controller.view().hasWindowFocus()',native)
  self.assertIn('storeScreenshot("control-failure",instrumentation.getUiAutomation().takeScreenshot())',native)

 def test_editable_find_refreshes_live_text_and_releases_content_observers(self):
  s=(JAVA/'NoteFindDialog.java').read_text()
  for token in ['field.addTextChangedListener(contentWatcher)','field.removeTextChangedListener(contentWatcher)','owner.removeCallbacks(refreshContent)','search(query.getText().toString(),false)','if(!matches.isEmpty() && jump(matches.get(index)))close()','if(scrollToMatch)reveal(match)']:
   self.assertIn(token,s)

 def test_checked_row_swipe_is_guarded_and_undoable_in_both_editors(self):
  controller=(JAVA/'NotesController.java').read_text()
  document=(JAVA/'DocumentEditorView.java').read_text()
  gesture=(JAVA/'ChecklistSwipeRow.java').read_text()
  snackbar=(JAVA/'NotesSnackbar.java').read_text()
  self.assertIn('item.checked&&!trashed',controller)
  self.assertIn('item.checked&&!readOnly()',document)
  self.assertIn('touchStartedOnReorderHandle',gesture)
  self.assertIn('disallowAncestors(true)',gesture)
  self.assertIn('Math.abs(dx)>=threshold()',gesture)
  self.assertIn('if(index<0 || !rows.get(index).checked)return false',controller)
  self.assertIn('captureUndo(Arrays.asList(before),true)',controller)
  self.assertIn('showUndoSnackbar("Checked item deleted",()->{log("checklist-swipe-delete"',controller)
  self.assertIn('getRecommendedTimeoutMillis',snackbar)
  self.assertIn('setContentDescription(actionDescription)',snackbar)
  journeys=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
  self.assertIn('checkedChecklistRowsSwipeAwayWithUndoInBothEditors',journeys)
  self.assertIn('setSwipeEnabled(value&&!trashed)',controller)
  self.assertIn('line.setSwipeEnabled(on&&!readOnly())',document)
  self.assertIn('Delete checked item',gesture)
  acceptance=(ROOT/'docs/CHECKLIST-ACCEPTANCE.md').read_text()
  self.assertIn('AT-K050.1 — Checked rows only and gesture boundaries',acceptance)

 def test_checklist_urls_and_selected_text_formatting_preserve_rows(self):
  links=(JAVA/'ChecklistLinks.java').read_text()
  self.assertIn('Patterns.WEB_URL.matcher(value)',links)
  self.assertIn('LinkMovementMethod.getInstance()',links)
  self.assertIn('extends ClickableSpan',links)
  self.assertIn('SafeLinks.requireWeb(address)',links)
  self.assertIn('embeddedNonWebScheme(value,start)',links)
  self.assertIn('plainInsertion(source,start,end,field.getResources().getDisplayMetrics())',links)
  self.assertIn('if(span instanceof CharacterStyle || span instanceof ParagraphStyle)',links)
  self.assertIn('private static final class ChecklistUrlSpan extends URLSpan',links)
  self.assertNotIn('new URLSpan(',links)
  controller=(JAVA/'NotesController.java').read_text()
  self.assertIn('ChecklistLinks.install(input,this::openChecklistUrl)',controller)
  self.assertIn('SafeLinks.requireWeb(value)',controller)
  self.assertIn('upgradeLegacyChecklistForFormatting()',controller)
  self.assertIn('DocumentContent.fromLegacy(source).inNotesWorkspace(true).ensureWritableTail()',controller)
  self.assertIn('putString("field","row:"+rowId)',controller)
  document=(JAVA/'DocumentEditorView.java').read_text()
  self.assertIn('TextStyles.write(input.getText(),value.marks',document)
  self.assertIn('if(item!=null)ChecklistLinks.install(input,this::openWeb)',document)
  self.assertIn('if(item!=null)ChecklistLinks.install(input,this::openWeb)',document)
  self.assertIn('private void formatMenu()',document)
  self.assertIn('private void applyFormat(String type,String value)',document)
  self.assertNotIn('Checklist items use plain text',document)
  self.assertNotIn('selectedRow!=null)throw new IllegalStateException',document)
  journeys=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
  self.assertIn('checklistFormattingAndSafeLinksPreserveRowsInBothEditors',journeys)
  self.assertIn('awaitDialogText("The website is external to MyNotes.',journeys)
  self.assertIn('noteAndDocumentMenusDoNotOfferQuickSummary',journeys)
  acceptance=(ROOT/'docs/CHECKLIST-ACCEPTANCE.md').read_text()
  for scenario in ['AT-K051.1 — Safe URL taps','AT-K051.2 — Format a selected checklist range','AT-K051.3 — Writing text remains formattable']:
   self.assertIn(scenario,acceptance)
  self.assertNotIn('recorded **before K051 implementation**',acceptance)

 def test_empty_checklist_deletion_uncheck_all_and_writable_tails_are_regressed(self):
  controller=(JAVA/'NotesController.java').read_text()
  document=(JAVA/'DocumentEditorView.java').read_text()
  self.assertIn('emptyBackspace=()->removeEmptyLegacyChecklistRow',controller)
  self.assertIn('getText().toString().trim().isEmpty()',controller)
  self.assertIn('String[] names={"Indent item","Outdent item","Remove item"}',controller)
  self.assertIn('if(action.equals("remove")){rows.remove(index)',controller)
  self.assertIn('ChecklistEditing.uncheckAll(rows)',controller)
  self.assertIn('ChecklistEditing.uncheckAll(list.items)',document)
  self.assertIn('emptyOnly && !list.items.get(index).text.trim().isEmpty()',document)
  content=(ROOT/'domain/src/main/java/app/space/notes/domain/DocumentContent.java').read_text()
  self.assertIn('ensureWritableTail()',content)
  self.assertIn('public DocumentContent ensureWritableTail()',content)
  workspace=(ROOT/'domain/src/test/java/app/space/notes/domain/WorkspaceTest.java').read_text()
  self.assertIn('copiedChecklistDocHasAFollowingWritableTextSection',workspace)
  journeys=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
  for case in ['emptyChecklistBackspaceAndMenuRemovalAreImmediateInBothEditors','checklistOptionsUncheckAllAndOfferDraggingInsteadOfMoveMenu','insertedChecklistAndTableKeepWritableTailAndCaretOnNewRows']:
   self.assertIn(case,journeys)

 def test_inline_search_is_docked_and_search_controls_are_not_modal(self):
  search=(JAVA/'NoteFindDialog.java').read_text()
  for token in ['owner.addView(bar','bar.setTag("inline-find")','query.requestFocus()','showSoftInput(query','Previous match','Next match','Close find']:
   self.assertIn(token,search)
  self.assertNotIn('AlertDialog',search)
  journeys=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
  self.assertIn('findQueryAndNavigationControlsStayAboveKeyboardInNotesAndDocs',journeys)
  self.assertIn('docked just above the IME',journeys)

 def test_rapid_notices_replace_or_coalesce_instead_of_stacking(self):
  notice=(JAVA/'NotesNotice.java').read_text()
  self.assertIn('REPEAT_WINDOW_MS=3200',notice)
  self.assertIn('message.equals(state.lastMessage) && now-state.lastShownAt<REPEAT_WINDOW_MS',notice)
  self.assertIn('if(state.toast!=null)state.toast.cancel()',notice)
  self.assertIn('state.toast=Toast.makeText(context,message,Toast.LENGTH_SHORT)',notice)
  activities=list((ROOT/'app/src/main/java').rglob('*.java'))
  self.assertTrue(any('NotesNotice.show(this,message)' in source.read_text() for source in activities))

 def test_note_and_doc_menus_drop_quick_summary_and_checklist_move_menu(self):
  controller=(JAVA/'NotesController.java').read_text()
  note_menu=controller.split('LinearLayout actions=menu("Note actions"',1)[1].split('TextView dates=',1)[0]
  self.assertNotIn('Quick summary',note_menu)
  document=(JAVA/'DocumentEditorView.java').read_text()
  doc_menu=document.split('private void documentMenu()',1)[1].split('private void restoreAfterDialog',1)[0]
  self.assertNotIn('Quick summary',doc_menu)
  checklist_menu=document.split('private void contentOptions(String id)',1)[1].split('private void pasteTable()',1)[0]
  self.assertNotIn('Move checklist',checklist_menu)
  self.assertIn('block-handle:"+block.id',document)
  journeys=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
  self.assertIn('noteAndDocumentMenusDoNotOfferQuickSummary',journeys)
  self.assertIn('namedChecklistOptionsOmitMoveAndKeepTheDragHandle',journeys)
  self.assertIn('nativeChecklistBoxDragShowsLineAndDropsInsideRichWriting',journeys)

 def test_image_annotation_save_and_history_are_covered_for_notes_and_docs(self):
  editor=(JAVA/'ImageEditView.java').read_text()
  self.assertIn('Tool { NONE, HIGHLIGHT, PEN, RECTANGLE, OVAL, ARROW }',editor)
  self.assertIn('annotations.add(drawing);remember(drawingBefore)',editor)
  self.assertIn('render(decoded,plan,annotations)',(JAVA/'NotesController.java').read_text())
  journeys=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
  self.assertIn('imageAnnotationsUndoRedoRestoreAndOnlyChangeTheSavedAttachmentInNotesAndDocs',journeys)
  self.assertIn('Vector highlight is rasterized into the saved attachment',journeys)
