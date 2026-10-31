package app.space.notes.runtime;

/** One outline-icon vocabulary for editor tools and their contextual actions. */
final class EditorActions {
 private EditorActions() { }
 static String symbol(String label){
  if(label.startsWith("Find"))return "search";
  if(label.startsWith("Rename") || label.equals("Edit"))return "edit";
  if(label.startsWith("Remove") || label.startsWith("Delete"))return "trash";
  if(label.startsWith("Insert") || label.equals("Text before"))return "add";
  if(label.startsWith("Move"))return label.contains("folder")?"folder":"drag";
  if(label.startsWith("Copy"))return "copy";
  if(label.startsWith("Heading") || label.contains("format") || label.equals("Font") || label.startsWith("Size"))return "format";
  if(label.contains("checklist") || label.contains("checked") || label.equals("Checklist"))return "checklist";
  switch(label){
   case "Sans serif":case "Serif":case "Monospace":case "Cursive":return "format";
   case "Text":case "Convert to text":case "Writing lines":return "text";
   case "Image":return "image";case "Drawing":return "pencil";case "Diagram":return "diagram";
   case "Table":case "Cell actions":return "grid";
   case "Reminder":case "Rename reminder":return "bell";
   case "Undo":case "Restore":return "undo";case "Redo":return "redo";
   case "Share":return "share";case "Export PDF":return "export";
   case "File location":return "folder";
   case "Appearance":case "Background":case "Colour":return "palette";case "Labels":return "label";
   case "Pin":case "Unpin":return "pin";case "Archive":case "Unarchive":return "archive";
   case "Indent":return "indent";case "Outdent":return "outdent";
   case "Hyperlink":case "Open link":return "link";
   default:return "next";
  }
 }
 static String scope(String title){
  if(title.equals("Document actions") || title.equals("Note actions"))return "Manage this "+(title.startsWith("Document")?"document":"note")+". Share, organise or review its history.";
  if(title.startsWith("Insert"))return "Add content where you are writing.";
  if(title.contains("Checklist") || title.contains("Content") || title.contains("Table") || title.contains("List item") || title.endsWith(" options"))return "Only this content. The rest of your document stays unchanged.";
  return "";
 }
}
