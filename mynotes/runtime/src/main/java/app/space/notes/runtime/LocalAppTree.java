package app.space.notes.runtime;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import java.util.*;

/** Small strict SAF filesystem. Worker-only; no provider-supplied path is used as a disk path. */
final class LocalAppTree {

  final ContentResolver resolver;
  final Uri tree, root;

  static final class Entry {

    final String name, mime;
    final Uri uri;

    Entry(String n, String m, Uri u) {
      name = n;
      mime = m;
      uri = u;
    }
  }

  LocalAppTree(ContentResolver resolver, Uri tree) throws IOException {
    new LocalTreeBackend(resolver, tree);
    this.resolver = resolver;
    this.tree = tree;
    root = DocumentsContract.buildDocumentUriUsingTree(
      tree,
      DocumentsContract.getTreeDocumentId(tree)
    );
  }

  List<Entry> list(Uri parent) throws IOException {
    List<Entry> out = new ArrayList<>();
    Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(
      tree,
      DocumentsContract.getDocumentId(parent)
    );
    try (
      Cursor c = resolver.query(
        children,
        new String[] {
          DocumentsContract.Document.COLUMN_DISPLAY_NAME,
          DocumentsContract.Document.COLUMN_MIME_TYPE,
          DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        },
        null,
        null,
        null
      )
    ) {
      if (c == null) throw new IOException("App folder is unavailable");
      while (c.moveToNext()) {
        String name = c.getString(0);
        if (name == null || !names.add(name)) throw new IOException(
          "Ambiguous App folder names; nothing overwritten"
        );
        if (out.size() >= 20000) throw new IOException(
          "Too many App folder entries to inspect safely"
        );
        out.add(
          new Entry(
            name,
            c.getString(1),
            DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(2))
          )
        );
      }
    } catch (SecurityException e) {
      throw new IOException(
        "App folder access was revoked. Private data is retained; reconnect the folder.",
        e
      );
    }
    return out;
  }

  Uri child(Uri parent, String name, boolean directory, boolean create)
    throws IOException {
    if (
      name.isEmpty() ||
      name.equals(".") ||
      name.equals("..") ||
      name.contains("/") ||
      name.contains("\\")
    ) throw new IOException("Invalid App folder component");
    for (Entry e : list(parent))
      if (e.name.equalsIgnoreCase(name)) {
        if (
          !e.name.equals(name) ||
          directory != DocumentsContract.Document.MIME_TYPE_DIR.equals(e.mime)
        ) throw new IOException("App folder name/type collision");
        return e.uri;
      }
    if (!create) return null;
    Uri made = DocumentsContract.createDocument(
      resolver,
      parent,
      directory
        ? DocumentsContract.Document.MIME_TYPE_DIR
        : "application/octet-stream",
      name
    );
    if (made == null) throw new IOException("Cannot create App folder data");
    Uri actual = child(parent, name, directory, false);
    if (!made.equals(actual)) throw new IOException(
      "Provider changed App folder name"
    );
    return made;
  }

  Uri path(String path, boolean create) throws IOException {
    Uri current = root;
    for (String part : path.split("/")) {
      if (current == null) throw new IOException(
        "Incomplete App folder structure"
      );
      current = child(current, part, true, create);
    }
    if (current == null) throw new IOException(
      "Incomplete App folder structure"
    );
    return current;
  }

  byte[] read(Uri file, int maximum) throws IOException {
    try (InputStream in = resolver.openInputStream(file)) {
      return LocalOpenFile.bounded(in, maximum);
    }
  }

  void immutable(Uri parent, String name, byte[] bytes) throws Exception {
    Uri old = child(parent, name, false, false);
    if (old != null) {
      if (!Arrays.equals(bytes, read(old, bytes.length))) throw new IOException(
        "Existing App folder data differs; refusing overwrite"
      );
      return;
    }
    Uri part = child(
      parent,
      "pending-" + UUID.randomUUID() + ".part",
      false,
      true
    );
    try (OutputStream out = resolver.openOutputStream(part, "w")) {
      if (out == null) throw new IOException("Cannot write App folder data");
      out.write(bytes);
      out.flush();
    }
    if (!Arrays.equals(bytes, read(part, bytes.length))) throw new IOException(
      "App folder readback failed"
    );
    if (
      DocumentsContract.renameDocument(resolver, part, name) == null ||
      !Arrays.equals(
        bytes,
        read(child(parent, name, false, false), bytes.length)
      )
    ) throw new IOException("App folder commit verification failed");
  }
}
