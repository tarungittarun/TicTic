package app.space.notes.runtime;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import app.space.notes.domain.FolderNames;
import app.space.notes.domain.Note;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** Real SAF child directories and portable note documents. Use only on the serial storage worker.
 * The canonical private journal remains recovery authority; failed/partial moves never delete it.
 * Foreign files are never overwritten. No recursive deletion or folder deletion is offered.
 */
public final class LocalNotebookFolders {
 private final ContentResolver resolver;private final Uri tree,root;
 private static final class Entry {final String name,mime;final Uri uri;Entry(String name,String mime,Uri uri){this.name=name;this.mime=mime;this.uri=uri;}}
 public LocalNotebookFolders(ContentResolver resolver,Uri tree)throws IOException{
  new LocalTreeBackend(resolver,tree);this.resolver=resolver;this.tree=tree;root=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
 }
 private List<Entry> children(Uri parent)throws IOException{
  List<Entry> result=new ArrayList<>();Uri uri=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getDocumentId(parent));
  try(Cursor cursor=resolver.query(uri,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_DOCUMENT_ID},null,null,null)){
   if(cursor==null)throw new IOException("Storage folder is unavailable");while(cursor.moveToNext())result.add(new Entry(cursor.getString(0),cursor.getString(1),DocumentsContract.buildDocumentUriUsingTree(tree,cursor.getString(2))));
  }catch(SecurityException denied){throw new IOException("Folder access was revoked. Reconnect the main storage folder.",denied);}return result;
 }
 public List<String> list()throws IOException{
  TreeSet<String> names=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);for(Entry entry:children(root))if(DocumentsContract.Document.MIME_TYPE_DIR.equals(entry.mime)){try{names.add(FolderNames.require(entry.name));}catch(IllegalArgumentException unsupported){/* Do not reinterpret unsafe directory components. */}}return new ArrayList<>(names);
 }
 private Entry find(Uri parent,String name)throws IOException{Entry found=null;for(Entry entry:children(parent))if(name.equalsIgnoreCase(entry.name)){if(found!=null)throw new IOException("Ambiguous duplicate storage names; files left untouched");found=entry;}return found;}
 private Uri directory(String name)throws IOException{
  if(name.isEmpty())return root;String safe=FolderNames.require(name);Entry entry=find(root,safe);if(entry==null || !DocumentsContract.Document.MIME_TYPE_DIR.equals(entry.mime))throw new IOException("Folder no longer exists: "+safe);return entry.uri;
 }
 public void create(String name)throws IOException{
  String safe=FolderNames.require(name);if(find(root,safe)!=null)throw new IOException("A file or folder with that name already exists");
  Uri created=DocumentsContract.createDocument(resolver,root,DocumentsContract.Document.MIME_TYPE_DIR,safe);if(created==null)throw new IOException("Could not create the folder");Entry actual=find(root,safe);if(actual==null || !DocumentsContract.Document.MIME_TYPE_DIR.equals(actual.mime))throw new IOException("Provider changed the folder name. Check Android Files before retrying.");
 }
 private static String fileName(String id)throws Exception{
  byte[] digest=MessageDigest.getInstance("SHA-256").digest(id.getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder value=new StringBuilder("note-");for(byte b:digest)value.append(String.format(Locale.ROOT,"%02x",b&255));return value+".mynote.json";
 }
 private byte[] read(Uri uri)throws IOException{try(InputStream in=resolver.openInputStream(uri)){if(in==null)throw new IOException("Cannot read note file");return LocalTreeBackend.readBytes(in);}}
 private Note owned(Entry entry,String id)throws Exception{if(DocumentsContract.Document.MIME_TYPE_DIR.equals(entry.mime))throw new IOException("A directory occupies the note file name");List<Note> decoded=DocumentCodec.decode(read(entry.uri));if(decoded.size()!=1 || !decoded.get(0).id.equals(id))throw new IOException("A foreign file occupies the note file name; it was not changed");return decoded.get(0);}
 /** Call only after journalling the current library privately. Readback must match before removal. */
 public void write(String folder,Note note)throws Exception{
  Uri parent=directory(folder);String name=fileName(note.id);Entry existing=find(parent,name);byte[] payload=DocumentCodec.encode(Collections.singletonList(note));Uri target;
  if(existing!=null){owned(existing,note.id);if(Arrays.equals(payload,read(existing.uri)))return;target=existing.uri;}else{target=DocumentsContract.createDocument(resolver,parent,"application/json",name);if(target==null)throw new IOException("Cannot create the note file");Entry actual=find(parent,name);if(actual==null || !actual.uri.equals(target))throw new IOException("Provider changed the file name; original note remains in recovery");}
  try(OutputStream out=resolver.openOutputStream(target,"rwt")){if(out==null)throw new IOException("Cannot write the note file");out.write(payload);out.flush();}
  if(!Arrays.equals(payload,read(target)))throw new IOException("Note file readback mismatch; original note remains in recovery");
 }
 /** Materialize current placements, then remove verified same-ID copies from previous folders.
  * Root generation snapshots remain authoritative and are deliberately never removed here. */
 public void reconcile(List<Note> notes)throws Exception{
  List<String> folders=list();for(Note note:notes)if(!note.folder.isEmpty() && !folders.contains(note.folder)){Entry existing=find(root,note.folder);if(existing==null)create(note.folder);directory(note.folder);folders=list();}
  List<String> locations=new ArrayList<>(folders);locations.add("");Map<String,List<Entry>> catalog=new LinkedHashMap<>();for(String folder:locations)catalog.put(folder,children(directory(folder)));
  for(Note note:notes){String name=fileName(note.id);boolean placed=!note.folder.isEmpty();for(List<Entry> entries:catalog.values())for(Entry entry:entries)if(entry.name.equals(name))placed=true;if(!placed)continue;
   write(note.folder,note);for(Map.Entry<String,List<Entry>> location:catalog.entrySet())if(!location.getKey().equalsIgnoreCase(note.folder))for(Entry entry:location.getValue())if(entry.name.equals(name)){owned(entry,note.id);if(!DocumentsContract.deleteDocument(resolver,entry.uri))throw new IOException("Destination verified, but an old note copy remains. Retry save to finish moving.");}
  }
 }
 /** Destination copy is verified first. Any failure leaves source/recovery available for retry. */
 public void move(Note note,String from,String to)throws Exception{
  if(from.equals(to)){write(to,note);return;}Uri source=directory(from);Entry old=find(source,fileName(note.id));if(old!=null)owned(old,note.id);
  write(to,note);if(old!=null && !DocumentsContract.deleteDocument(resolver,old.uri))throw new IOException("New copy verified, but the old copy could not be removed. Both copies were retained.");
 }
}
