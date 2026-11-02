package app.space.notes.runtime;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

/** Versioned complete-app checkpoints. Serial worker only. Does not replace private storage.
 * Immutable objects + parented commit manifests: interrupted uploads cannot replace the last
 * verified checkpoint. No unverified object is ever promoted or restored.
 */
public final class AppFolder {

  static final String MARKER = "MyNotes-App.json";
  private static final Object CHECKPOINT_LOCK = new Object();
  static final int JSON_LIMIT = 8 * 1024 * 1024,
    FILE_LIMIT = 10000;
  static final String[] PREFS = {
    "mynotes-ui",
    "mynotes-style-presets",
    "notes-drawing",
    "mynotes-delivered-v1",
  };
  private static final byte[] ID =
    "{\"format\":\"MyNotesAppFolder\",\"version\":1}".getBytes(
      StandardCharsets.UTF_8
    );
  private final Context context;
  private Map<String, Boolean> copyChoices;
  private final LocalAppTree fs;
  private static final Map<String, Verified> AUDIO_CACHE = new LinkedHashMap<
    String,
    Verified
  >(64, .75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, Verified> e) {
      return size() > 4096;
    }
  };

  private static final class Verified {

    final long length, modified;
    final String hash, stamp;
    final Uri uri;

    Verified(File source, String hash, Uri uri, String stamp) {
      length = source.length();
      modified = source.lastModified();
      this.hash = hash;
      this.uri = uri;
      this.stamp = stamp;
    }
  }

  private String stamp(Uri uri) throws Exception {
    try (
      android.database.Cursor c = fs.resolver.query(
        uri,
        new String[] {
          DocumentsContract.Document.COLUMN_SIZE,
          DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        },
        null,
        null,
        null
      )
    ) {
      if (
        c == null ||
        !c.moveToFirst() ||
        c.isNull(0) ||
        c.isNull(1) ||
        c.getLong(1) <= 0
      ) return "";
      return c.getLong(0) + ":" + c.getLong(1);
    }
  }

  private String cacheKey(File source, String kind) {
    return fs.tree.toString() + "|" + source.getAbsolutePath() + "|" + kind;
  }

  private Verified cached(File source, String kind) throws Exception {
    if (!source.getName().endsWith(".wav")) return null;
    Verified v;
    synchronized (AUDIO_CACHE) {
      v = AUDIO_CACHE.get(cacheKey(source, kind));
    }
    return v != null &&
      source.length() == v.length &&
      source.lastModified() == v.modified &&
      !v.stamp.isEmpty() &&
      v.stamp.equals(stamp(v.uri))
      ? v
      : null;
  }

  private void remember(File source, String digest, Uri uri, String kind)
    throws Exception {
    if (source.getName().endsWith(".wav")) {
      Verified value = new Verified(source, digest, uri, stamp(uri));
      synchronized (AUDIO_CACHE) {
        AUDIO_CACHE.put(cacheKey(source, kind), value);
      }
    }
  }

  public AppFolder(Context context, Uri tree) throws IOException {
    this.context = context.getApplicationContext();
    fs = new LocalAppTree(this.context.getContentResolver(), tree);
  }

  public boolean recognized() throws Exception {
    Uri marker = fs.child(fs.root, MARKER, false, false);
    if (marker == null) return false;
    JSONObject id = json(fs.read(marker, 4096));
    if (
      !"MyNotesAppFolder".equals(id.getString("format")) ||
      id.getInt("version") != 1
    ) throw new IOException("Unsupported App folder version; nothing changed");
    return true;
  }

  public void initialize() throws Exception {
    String location = DocumentsContract.getTreeDocumentId(fs.tree).toLowerCase(
      Locale.ROOT
    );
    int colon = location.indexOf(':');
    String relative = colon < 0 ? location : location.substring(colon + 1);
    String normalized = "/" + relative + "/";
    for (String reserved : new String[] {
      "/android/data/",
      "/android/obb/",
      "/android/media/",
    })
      if (normalized.contains(reserved)) throw new IOException(
        "Choose a public Documents or other ordinary device folder, not Android app-specific storage that uninstall may remove"
      );

    if (!fs.list(fs.root).isEmpty()) throw new IOException(
      "New App folder must be completely empty. For an existing MyNotes App folder choose Recover App folder. No files were overwritten."
    );
    // Marker is deliberately last. An interrupted initialization is not mistaken for a recovery source.
    for (String path : new String[] {
      "Content/Notes",
      "Content/Voice",
      "Settings",
      "Profile",
      "History",
      "Diagnostics",
      "Recovery/Library",
      "Recovery/Objects",
      "Recovery/Checkpoints",
    })
      fs.path(path, true);
    fs.immutable(
      fs.root,
      "README.txt",
      "MyNotes on-device App folder, layout 1\n\nContent/Notes contains editable portable Notes and Docs. Use MyNotes Folders to create and arrange note subfolders. Content/Voice contains completed recording copies. Do not move or edit system directories or recovery objects.\n\nInternal app storage is always kept too. Recovery/Checkpoints and Recovery/Objects hold verified additional app data, according to your copy choices. Settings/Profile/History/Diagnostics are reserved namespaces; checkpoint manifests describe the exact included categories. There is no cloud sync or encryption: anyone with access to this folder can read its contents.\n\nAfter reinstall, open Storage & recovery and select this App folder to recover. Re-grant Android folder access. Full recovery is allowed only into an empty installation; otherwise merge notes with Open library. App data excluded by your choices cannot be recovered from this folder. Active recordings, pending saves, unavailable/removable storage and interrupted uploads are not guaranteed copied. Confirm the last complete checkpoint before uninstalling; keep a separate independent backup. Old checkpoints retain previous copies, including deleted or later-excluded data. No automatic permanent purge of recovery objects is performed. Notification permissions and pinned widgets need fresh Android consent.\n".getBytes(
        StandardCharsets.UTF_8
      )
    );
    fs.immutable(
      fs.path("Profile", false),
      "README.txt",
      "MyNotes has no account or online profile. Current personal preferences and saved styles are in Settings; a future layout version may introduce additional local profile fields. Android grants and diagnostic-consent settings are deliberately not restored.\n".getBytes(
        StandardCharsets.UTF_8
      )
    );
    fs.immutable(fs.root, MARKER, ID);
  }

  Uri library() throws Exception {
    if (!recognized()) return fs.root;
    return fs.path("Recovery/Library", false);
  }

  Uri content() throws Exception {
    if (!recognized()) return fs.root;
    return fs.path("Content/Notes", false);
  }

  private SharedPreferences options() {
    return context.getSharedPreferences("mynotes-app-folder", 0);
  }

  public boolean included(String category) {
    return copyChoices != null
      ? copyChoices.get(category)
      : options().getBoolean(category, !"diagnostics".equals(category));
  }

  public static final class Checkpoint {

    public final String id;
    public String notice =
      "Restores the dated complete checkpoint. Pending newer data is not included.";
    final JSONObject manifest;
    final JSONArray files;
    final JSONObject preferences;

    Checkpoint(String id, JSONObject manifest) throws Exception {
      this.id = id;
      this.manifest = manifest;
      files = manifest.getJSONArray("files");
      preferences = manifest.getJSONObject("preferences");
    }

    public int fileCount() {
      return files.length();
    }

    public long createdAt() {
      return manifest.optLong("createdAt", 0);
    }

    public String categories() {
      return manifest.optJSONArray("categories").toString();
    }
  }

  private static JSONObject json(byte[] data) throws Exception {
    return new JSONObject(app.space.notes.domain.JsonLimits.text(data));
  }

  private static byte[] readFile(File source, int maximum) throws IOException {
    try (InputStream in = new FileInputStream(source)) {
      return LocalOpenFile.bounded(in, maximum);
    }
  }

  private Uri objects() throws IOException {
    return fs.path("Recovery/Objects", false);
  }

  private static String hash(File file) throws Exception {
    try (InputStream in = new FileInputStream(file)) {
      return hash(in);
    }
  }

  private static String hash(InputStream in) throws Exception {
    if (in == null) throw new IOException("Missing checkpoint object");
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] buffer = new byte[65536];
    for (int n; (n = in.read(buffer)) != -1; ) digest.update(buffer, 0, n);
    StringBuilder out = new StringBuilder();
    for (byte b : digest.digest())
      out.append(String.format(Locale.ROOT, "%02x", b & 255));
    return out.toString();
  }

  private String objectHash(Uri file) throws Exception {
    try (InputStream in = fs.resolver.openInputStream(file)) {
      return hash(in);
    }
  }

  private String upload(File file) throws Exception {
    Verified fast = cached(file, "object");
    if (fast != null) return fast.hash;
    long size = file.length(),
      modified = file.lastModified();
    String digest = hash(file);
    Uri parent = objects(),
      existing = fs.child(parent, digest + ".bin", false, false);
    if (existing == null) {
      Uri part = fs.child(
        parent,
        "pending-" + UUID.randomUUID() + ".part",
        false,
        true
      );
      try (
        InputStream in = new FileInputStream(file);
        OutputStream out = fs.resolver.openOutputStream(part, "w")
      ) {
        if (out == null) throw new IOException(
          "Cannot write checkpoint object"
        );
        copy(in, out);
        out.flush();
      }
      if (!digest.equals(objectHash(part))) throw new IOException(
        "Checkpoint object readback failed; private data retained"
      );
      if (
        DocumentsContract.renameDocument(fs.resolver, part, digest + ".bin") ==
        null
      ) throw new IOException("Cannot finalize checkpoint object");
    } else if (!digest.equals(objectHash(existing))) throw new IOException(
      "Checkpoint object is damaged; refusing overwrite"
    );
    if (
      size != file.length() ||
      modified != file.lastModified() ||
      !digest.equals(hash(file))
    ) throw new IOException("App data changed during copying; retry pending");
    remember(
      file,
      digest,
      fs.child(parent, digest + ".bin", false, false),
      "object"
    );
    return digest;
  }

  private void voiceCopy(File source, String digest) throws Exception {
    if (cached(source, "visible") != null) return;
    Uri parent = fs.path("Content/Voice", false),
      existing = fs.child(parent, source.getName(), false, false);
    if (existing != null) {
      if (!digest.equals(objectHash(existing))) throw new IOException(
        "A changed recording copy is present; refusing overwrite"
      );
      remember(source, digest, existing, "visible");
      return;
    }
    Uri part = fs.child(
      parent,
      "pending-" + UUID.randomUUID() + ".part",
      false,
      true
    );
    try (
      InputStream in = new FileInputStream(source);
      OutputStream out = fs.resolver.openOutputStream(part, "w")
    ) {
      if (out == null) throw new IOException("Cannot copy original recording");
      copy(in, out);
      out.flush();
    }
    if (!digest.equals(objectHash(part))) throw new IOException(
      "Original recording copy failed readback"
    );
    if (
      DocumentsContract.renameDocument(fs.resolver, part, source.getName()) ==
      null
    ) throw new IOException("Recording copy pending");
    remember(
      source,
      digest,
      fs.child(parent, source.getName(), false, false),
      "visible"
    );
  }

  private static void copy(InputStream in, OutputStream out)
    throws IOException {
    byte[] buffer = new byte[65536];
    for (int n; (n = in.read(buffer)) != -1; ) out.write(buffer, 0, n);
  }

  /** Refuse traversal, unknown future paths and partial writes. Restore only registered user state. */
  static boolean owned(String path, boolean directory) {
    if (
      path == null ||
      path.length() > 512 ||
      path.startsWith("/") ||
      path.contains("\\") ||
      path.contains("\u0000")
    ) return false;
    String[] parts = path.split("/", -1);
    for (String part : parts)
      if (part.isEmpty() || part.equals(".") || part.equals("..")) return false;
    if (path.startsWith("note-folders/")) {
      if (!directory || parts.length != 2) return false;
      try {
        return parts[1].equals(
          app.space.notes.domain.FolderNames.require(parts[1])
        );
      } catch (IllegalArgumentException invalid) {
        return false;
      }
    }
    if (directory) return false;
    if (
      parts.length == 2 && parts[0].equals("notes-library")
    ) return Arrays.asList(
      "library-v1.json",
      "edit-history-v1.json",
      "before-document-editor-v7.json",
      "before-restore-v1.json"
    ).contains(parts[1]);
    if (
      parts.length == 2 && parts[0].equals("voice-inbox-v1")
    ) return parts[1].matches(
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(?:wav|transcript\\.json|note-created)"
    );
    return (
      path.equals("diagnostics.jsonl") ||
      path.equals("diagnostics.previous.jsonl")
    );
  }

  private File local(String path) throws IOException {
    File root = context.getFilesDir().getCanonicalFile(),
      file = new File(root, path);
    if (
      !file.getCanonicalPath().equals(file.getAbsolutePath()) ||
      !file.getCanonicalPath().startsWith(root.getPath() + File.separator)
    ) throw new IOException("Unsafe checkpoint path");
    return file;
  }

  private void collect(File directory, String prefix, JSONArray files)
    throws Exception {
    File[] children = directory.listFiles();
    if (children == null) {
      if (directory.exists()) throw new IOException(
        "Cannot inspect private app data"
      );
      return;
    }
    Arrays.sort(children, Comparator.comparing(File::getName));
    for (File child : children) {
      String path = prefix + child.getName();
      if (child.isDirectory()) {
        if (owned(path, true)) {
          local(path);
          files.put(new JSONObject().put("path", path).put("directory", true));
        }
        continue;
      }
      if (
        path.endsWith(".wav.part") ||
        ((path.endsWith(".bak") || path.endsWith(".new")) &&
          owned(path.substring(0, path.lastIndexOf('.')), false))
      ) throw new IOException(
        "A recording or atomic save is in progress; checkpoint pending"
      );
      if (!owned(path, false)) continue;
      if (path.contains("edit-history") || path.contains("before-")) {
        if (!included("history")) continue;
      }
      if (path.startsWith("voice-inbox") && !included("voice")) continue;
      if (path.startsWith("diagnostics") && !included("diagnostics")) continue;
      local(path);
      String digest = upload(child);
      if (
        path.startsWith("voice-inbox-v1/") && path.endsWith(".wav")
      ) voiceCopy(child, digest);
      files.put(
        new JSONObject()
          .put("path", path)
          .put("directory", false)
          .put("bytes", child.length())
          .put("sha256", digest)
      );
      if (files.length() > FILE_LIMIT) throw new IOException(
        "Too many app data files to checkpoint safely"
      );
    }
  }

  private JSONObject preferences() throws Exception {
    JSONObject result = new JSONObject();
    if (!included("settings")) return result;
    for (String name : PREFS) {
      JSONObject values = new JSONObject();
      for (Map.Entry<String, ?> e : context
        .getSharedPreferences(name, 0)
        .getAll()
        .entrySet()) {
        Object value = e.getValue();
        String type =
          value instanceof String
            ? "string"
            : value instanceof Boolean
              ? "boolean"
              : value instanceof Integer
                ? "int"
                : value instanceof Long
                  ? "long"
                  : value instanceof Float
                    ? "float"
                    : value instanceof Set
                      ? "set"
                      : "";
        if (type.isEmpty()) throw new IOException(
          "Unsupported portable preference"
        );
        values.put(
          e.getKey(),
          new JSONObject()
            .put("type", type)
            .put(
              "value",
              value instanceof Set
                ? new JSONArray(new TreeSet<>((Set<String>) value))
                : value
            )
        );
      }
      result.put(name, values);
    }
    return result;
  }

  /** Metadata chain validation is bounded. Content objects are fully checked during restore. */
  public Checkpoint inspect() throws Exception {
    if (!recognized()) throw new IOException(
      "Not a complete MyNotes App folder. Use Open library for older note libraries."
    );
    Map<String, JSONObject> records = new LinkedHashMap<>();
    for (LocalAppTree.Entry e : fs.list(fs.path("Recovery/Checkpoints", false)))
      if (e.name.matches("checkpoint-[0-9a-f-]{36}\\.json")) {
        JSONObject envelope;
        try {
          envelope = json(fs.read(e.uri, JSON_LIMIT));
        } catch (Exception incomplete) {
          continue;
        }
        if (!envelope.has("payload") || !envelope.has("sha256")) continue;
        byte[] payload = envelope
          .getString("payload")
          .getBytes(StandardCharsets.UTF_8);
        if (
          !GenerationProtocol.digest(payload).equals(
            envelope.getString("sha256")
          )
        ) continue;
        JSONObject record = json(payload);
        if (record.getInt("version") != 1) throw new IOException(
          "Unsupported App checkpoint version"
        );
        String id = record.getString("id");
        if (!e.name.equals("checkpoint-" + id + ".json")) throw new IOException(
          "Checkpoint identity mismatch"
        );
        records.put(id, record);
      }
    if (records.isEmpty()) return null;
    Set<String> heads = new HashSet<>(records.keySet());
    for (JSONObject record : records.values()) {
      String parent = record.getString("parent");
      if (!parent.isEmpty()) {
        if (!records.containsKey(parent)) throw new IOException(
          "Checkpoint parent is missing"
        );
        heads.remove(parent);
      }
      Set<String> seen = new HashSet<>();
      JSONObject cursor = record;
      while (cursor != null) {
        if (!seen.add(cursor.getString("id"))) throw new IOException(
          "Checkpoint cycle"
        );
        cursor = records.get(cursor.getString("parent"));
      }
    }
    if (heads.size() != 1) throw new IOException(
      "App folder has competing writers; no data overwritten"
    );
    String head = heads.iterator().next();
    return new Checkpoint(head, records.get(head));
  }

  private void requireSelected() throws IOException {
    if (
      !fs.tree
        .toString()
        .equals(
          context
            .getSharedPreferences("mynotes-storage-v1", 0)
            .getString("tree", null)
        )
    ) throw new IOException(
      "App folder selection changed; checkpoint cancelled"
    );
  }

  private String sourceHash(File source) throws Exception {
    Verified v = cached(source, "object");
    return v == null ? hash(source) : v.hash;
  }

  public void save() throws Exception {
    synchronized (CHECKPOINT_LOCK) {
      copyChoices = new LinkedHashMap<>();
      try {
        for (String category : new String[] {
          "voice",
          "settings",
          "history",
          "diagnostics",
        })
          copyChoices.put(
            category,
            options().getBoolean(category, !category.equals("diagnostics"))
          );
        saveVerified();
      } finally {
        copyChoices = null;
      }
    }
  }

  private void saveVerified() throws Exception {
    if (!recognized()) return;
    requireSelected();
    if (
      new File(context.getFilesDir(), "app-folder-recovery-v1").exists()
    ) throw new IOException("App recovery is finishing; checkpoint pending");
    NotesRepository owner = new NotesRepository(context);
    List<app.space.notes.domain.Note> privateNotes = owner.localSnapshot();
    if (included("history")) {
      PersistentHistoryStore history = new PersistentHistoryStore(context);
      history.load(privateNotes);
      if (history.notice() != null) throw new IOException(
        "Private edit history is not aligned or verified; retry after notes finish saving, or review Reset edit history"
      );
      for (String name : new String[] {
        "before-restore-v1.json",
        "before-document-editor-v7.json",
      }) {
        File saved = new File(context.getFilesDir(), "notes-library/" + name);
        if (saved.exists() || new File(saved + ".bak").exists()) {
          if (name.equals("before-restore-v1.json")) owner.previousSnapshot();
          else owner.documentUpgradeSnapshot();
        }
      }
    }
    if (included("voice")) {
      VoiceFiles voices = new VoiceFiles(context);
      for (String id : voices.list()) {
        voices.noteWasCreated(id);
        byte[] metadata = voices.archiveMetadata(id);
        if (metadata != null) ArchiveJson.metadata(metadata, id);
      }
    }
    Checkpoint previous = inspect();
    JSONArray files = new JSONArray();
    // Reconcile empty user folders too; notes are not the only folder membership source.
    LocalNotebookFolders visibleFolders = new LocalNotebookFolders(
      fs.resolver,
      fs.tree,
      content()
    );
    Set<String> folderNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    folderNames.addAll(visibleFolders.list());
    File privateFolders = new File(context.getFilesDir(), "note-folders");
    File[] localFolders = privateFolders.listFiles();
    if (localFolders != null) for (File directory : localFolders)
      if (
        directory.isDirectory() &&
        owned("note-folders/" + directory.getName(), true)
      ) folderNames.add(directory.getName());
    List<String> visibleNames = visibleFolders.list();
    for (String name : folderNames) {
      File directory = local("note-folders/" + name);
      if (
        !directory.mkdirs() && !directory.isDirectory()
      ) throw new IOException("Cannot retain private folder organisation");
      if (!visibleNames.contains(name)) visibleFolders.create(name);
    }
    collect(
      new File(context.getFilesDir(), "notes-library"),
      "notes-library/",
      files
    );
    collect(
      new File(context.getFilesDir(), "note-folders"),
      "note-folders/",
      files
    );
    collect(
      new File(context.getFilesDir(), "voice-inbox-v1"),
      "voice-inbox-v1/",
      files
    );
    if (included("diagnostics")) collect(context.getFilesDir(), "", files);
    File library = local("notes-library/library-v1.json");
    if (!library.isFile()) throw new IOException(
      "Private library is not ready for a checkpoint"
    );
    byte[] libraryBefore = readFile(library, 32 * 1024 * 1024);
    DocumentCodec.decode(libraryBefore);
    if (json(libraryBefore).optBoolean("pending", false)) throw new IOException(
      "Portable note files are pending; Retry save before relying on a complete checkpoint"
    );
    GenerationProtocol.Snapshot nativeLibrary = new GenerationProtocol(
      new LocalTreeBackend(fs.resolver, fs.tree, library())
    ).read();
    if (
      !Arrays.equals(
        DocumentCodec.encode(DocumentCodec.decode(libraryBefore)),
        nativeLibrary.payload
      )
    ) throw new IOException(
      "Notes are newer than folder copy; checkpoint pending"
    );
    JSONObject preferences = preferences();
    if (included("settings")) {
      byte[] settings = preferences.toString().getBytes(StandardCharsets.UTF_8);
      fs.immutable(
        fs.path("Settings", false),
        "settings-" + GenerationProtocol.digest(settings) + ".json",
        settings
      );
    }
    for (int i = 0; i < files.length(); i++) {
      JSONObject entry = files.getJSONObject(i);
      if (entry.getBoolean("directory")) continue;
      String path = entry.getString("path"),
        digest = entry.getString("sha256");
      if (
        path.startsWith("voice-inbox-v1/") && path.endsWith(".transcript.json")
      ) {
        try (InputStream in = new FileInputStream(local(path))) {
          fs.immutable(
            fs.path("Content/Voice", false),
            local(path)
              .getName()
              .replace(".transcript.json", "-" + digest + ".transcript.json"),
            LocalOpenFile.bounded(in, JSON_LIMIT)
          );
        }
      }
      if (path.contains("edit-history") || path.contains("before-")) {
        byte[] descriptor = new JSONObject()
          .put("format", "MyNotesHistoryReference")
          .put("source", path)
          .put("sha256", digest)
          .put("object", "Recovery/Objects/" + digest + ".bin")
          .toString()
          .getBytes(StandardCharsets.UTF_8);
        fs.immutable(
          fs.path("History", false),
          "history-" + digest + ".json",
          descriptor
        );
      }
      if (path.startsWith("diagnostics")) {
        try (InputStream in = new FileInputStream(local(path))) {
          fs.immutable(
            fs.path("Diagnostics", false),
            digest + ".jsonl",
            LocalOpenFile.bounded(in, JSON_LIMIT)
          );
        }
      }
    }
    JSONArray categories = new JSONArray().put("notes");
    for (String category : new String[] {
      "voice",
      "history",
      "settings",
      "diagnostics",
    })
      if (included(category)) categories.put(category);
    if (
      previous != null &&
      Objects.equals(
        previous.manifest.getString("libraryHead"),
        nativeLibrary.head
      ) &&
      previous.files.toString().equals(files.toString()) &&
      previous.preferences.toString().equals(preferences.toString()) &&
      previous.manifest
        .getJSONArray("categories")
        .toString()
        .equals(categories.toString())
    ) return;
    // Check each source again before acknowledging a complete checkpoint.
    for (int i = 0; i < files.length(); i++) {
      JSONObject entry = files.getJSONObject(i);
      if (
        !entry.getBoolean("directory") &&
        !entry
          .getString("sha256")
          .equals(sourceHash(local(entry.getString("path"))))
      ) throw new IOException(
        "App state changed during checkpoint; retry pending"
      );
    }
    String id = UUID.randomUUID().toString();
    JSONObject record = new JSONObject()
      .put("version", 1)
      .put("id", id)
      .put("parent", previous == null ? "" : previous.id)
      .put("createdAt", System.currentTimeMillis())
      .put("libraryHead", nativeLibrary.head)
      .put("categories", categories)
      .put("files", files)
      .put("preferences", preferences);
    validate(record);
    byte[] payload = record.toString().getBytes(StandardCharsets.UTF_8);
    byte[] envelope = new JSONObject()
      .put("payload", new String(payload, StandardCharsets.UTF_8))
      .put("sha256", GenerationProtocol.digest(payload))
      .toString()
      .getBytes(StandardCharsets.UTF_8);
    if (envelope.length > JSON_LIMIT) throw new IOException(
      "Checkpoint manifest exceeds safety size"
    );
    Checkpoint current = inspect();
    if (
      !Objects.equals(
        previous == null ? null : previous.id,
        current == null ? null : current.id
      )
    ) throw new IOException(
      "App folder changed externally; refusing competing checkpoint"
    );
    requireSelected();
    fs.immutable(
      fs.path("Recovery/Checkpoints", false),
      "checkpoint-" + id + ".json",
      envelope
    );
    Checkpoint verified = inspect();
    if (verified == null || !id.equals(verified.id)) throw new IOException(
      "Checkpoint readback failed"
    );
  }

  public void requireEmptyInstallation() throws Exception {
    if (NotesRepository.pendingSnapshots(context)) throw new IOException(
      "Private notes are still saving; finish the current operation before recovery"
    );
    if (
      !new NotesRepository(context).localSnapshot().isEmpty() ||
      !new VoiceFiles(context).list().isEmpty() ||
      !new StylePresetStore(context).load().isEmpty() ||
      new File(context.getFilesDir(), "voice-inbox-v1").listFiles().length > 0
    ) throw new IOException(
      "Complete recovery requires an empty installation. Your current content is unchanged. Use Open library for a note-only merge, or keep this installation and choose a new empty App folder."
    );
    for (String name : new String[] {
      "before-restore-v1.json",
      "before-document-editor-v7.json",
    }) {
      File prior = new File(context.getFilesDir(), "notes-library/" + name);
      if (
        prior.exists() &&
        !DocumentCodec.decode(readFile(prior, 32 * 1024 * 1024)).isEmpty()
      ) throw new IOException(
        "Private recovery contains existing content; use note-only merge instead of complete recovery"
      );
    }
    File history = new File(
      context.getFilesDir(),
      "notes-library/edit-history-v1.json"
    );
    if (history.exists()) {
      JSONObject prior = json(
        readFile(history, PersistentHistoryStore.MAX_BYTES)
      );
      byte[] data = prior.getString("payload").getBytes(StandardCharsets.UTF_8);
      if (
        !GenerationProtocol.digest(data).equals(prior.getString("sha256"))
      ) throw new IOException(
        "Private history is unreadable; it was not overwritten"
      );
      JSONObject state = json(data);
      if (
        state.getJSONArray("histories").length() > 0 ||
        state.getJSONObject("objects").length() > 0
      ) throw new IOException(
        "Private edit history contains existing content; use note-only merge instead"
      );
    }
    File[] folders = new File(
      context.getFilesDir(),
      "note-folders"
    ).listFiles();
    if (folders != null && folders.length > 0) throw new IOException(
      "Private folders already exist; complete recovery would overwrite their organisation. Use Open library to merge notes instead."
    );
  }

  public Checkpoint prepareRecovery() throws Exception {
    requireEmptyInstallation();
    Checkpoint checkpoint = inspect();
    if (checkpoint == null) throw new IOException(
      "No complete app checkpoint yet. Use Open library to recover verified notes."
    );
    validate(checkpoint.manifest);
    try {
      GenerationProtocol.Snapshot library = new GenerationProtocol(
        new LocalTreeBackend(fs.resolver, fs.tree, library())
      ).read();
      if (
        !Objects.equals(
          library.head,
          checkpoint.manifest.getString("libraryHead")
        )
      ) checkpoint.notice =
        "The note-only generation differs from this complete checkpoint. Recovery restores the dated complete checkpoint into INTERNAL storage without changing the source. Newer notes can be reviewed separately using Open library; choose a new empty App folder for continued copies.";
    } catch (Exception unavailable) {
      checkpoint.notice =
        "Latest note generations cannot be verified. The complete checkpoint can still be recovered independently into INTERNAL storage. The source stays untouched; use a new empty App folder for continued copies.";
    }
    return checkpoint;
  }

  /** Validate all schema/owned destinations before accepting provider data into private staging. */
  private static void validate(JSONObject manifest) throws Exception {
    if (manifest.getInt("version") != 1) throw new IOException(
      "Unsupported app checkpoint version"
    );
    JSONArray categoryArray = manifest.getJSONArray("categories");
    Set<String> categories = new HashSet<>();
    for (int i = 0; i < categoryArray.length(); i++) {
      String category = categoryArray.getString(i);
      if (
        !Arrays.asList(
          "notes",
          "voice",
          "history",
          "settings",
          "diagnostics"
        ).contains(category) ||
        !categories.add(category)
      ) throw new IOException("Unsupported checkpoint category");
    }
    if (!categories.contains("notes")) throw new IOException(
      "Checkpoint must include native content"
    );
    JSONArray files = manifest.getJSONArray("files");
    if (files.length() > FILE_LIMIT) throw new IOException(
      "Too many checkpoint files"
    );
    Set<String> paths = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    boolean hasLibrary = false;
    for (int i = 0; i < files.length(); i++) {
      JSONObject entry = files.getJSONObject(i);
      String path = entry.getString("path");
      boolean directory = entry.getBoolean("directory");
      if (!owned(path, directory) || !paths.add(path)) throw new IOException(
        "Unsafe or duplicate checkpoint destination"
      );
      if (
        (path.startsWith("voice-inbox-v1/") && !categories.contains("voice")) ||
        (path.startsWith("diagnostics") &&
          !categories.contains("diagnostics")) ||
        ((path.contains("edit-history") || path.contains("before-")) &&
          !categories.contains("history"))
      ) throw new IOException(
        "Checkpoint file is outside its declared categories"
      );
      if (!directory) {
        if (
          (path.endsWith(".wav") && entry.getLong("bytes") > 0xffffffffL + 8) ||
          (path.endsWith(".transcript.json") &&
            entry.getLong("bytes") >
              app.space.notes.domain.ArchiveManifest.MAX_METADATA_BYTES) ||
          (path.endsWith(".note-created") && entry.getLong("bytes") != 1) ||
          (path.endsWith(".jsonl") && entry.getLong("bytes") > JSON_LIMIT)
        ) throw new IOException("Checkpoint file exceeds its format bounds");
        if (
          !entry.getString("sha256").matches("[0-9a-f]{64}") ||
          entry.getLong("bytes") < 0
        ) throw new IOException("Invalid checkpoint object identity");
        if (
          path.endsWith(".json") && entry.getLong("bytes") > 32L * 1024 * 1024
        ) throw new IOException("Checkpoint JSON exceeds safe size");
      }
      if (path.equals("notes-library/library-v1.json")) hasLibrary = true;
    }
    if (!hasLibrary) throw new IOException("Checkpoint has no native library");
    JSONObject prefs = manifest.getJSONObject("preferences");
    if (
      prefs.length() > 0 && !categories.contains("settings")
    ) throw new IOException("Undeclared settings in checkpoint");
    Iterator<String> names = prefs.keys();
    while (names.hasNext()) {
      String name = names.next();
      if (!Arrays.asList(PREFS).contains(name)) throw new IOException(
        "Unknown portable preferences"
      );
      JSONObject values = prefs.getJSONObject(name);
      if (values.length() > 4096) throw new IOException("Too many preferences");
      Iterator<String> keys = values.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        if (key.length() > 512) throw new IOException("Invalid preference key");
        JSONObject item = values.getJSONObject(key);
        String type = item.getString("type");
        Object value = item.get("value");
        if (
          name.equals("mynotes-delivered-v1") && !type.equals("long")
        ) throw new IOException("Invalid delivered-reminder state");
        if (name.equals("mynotes-ui")) {
          String expected = Arrays.asList("grid", "sortCreated").contains(key)
            ? "boolean"
            : Arrays.asList("sort", "appearance", "manual-order").contains(key)
              ? "string"
              : null;
          if (expected == null || !expected.equals(type)) throw new IOException(
            "Unsupported UI setting"
          );
          if (
            key.equals("appearance") &&
            !Arrays.asList("system", "light", "dark").contains(value)
          ) throw new IOException("Invalid appearance choice");
          if (
            key.equals("sort") &&
            !Arrays.asList("edited", "created", "title", "manual").contains(value)
          ) throw new IOException("Invalid sort choice");
        }
        if(name.equals("mynotes-ui") && key.equals("manual-order")) LibraryOrderStore.decode((String)value);
        if (
          name.equals("notes-drawing") &&
          (!key.equals("grid") || !type.equals("boolean"))
        ) throw new IOException("Unsupported drawing preference");
        if (name.equals("mynotes-style-presets")) {
          if (
            !key.equals("styles") || !type.equals("string")
          ) throw new IOException("Unsupported saved styles");
          StylePresetStore.decode(item.getString("value"));
        }
        boolean good =
          (type.equals("string") && value instanceof String) ||
          (type.equals("boolean") && value instanceof Boolean) ||
          ((type.equals("int") || type.equals("long")) &&
            (value instanceof Integer || value instanceof Long)) ||
          (type.equals("float") && value instanceof Number) ||
          (type.equals("set") && value instanceof JSONArray);
        if (!good) throw new IOException("Invalid preference type");
        if (
          type.equals("int") &&
          (((Number) value).longValue() < Integer.MIN_VALUE ||
            ((Number) value).longValue() > Integer.MAX_VALUE)
        ) throw new IOException("Preference integer overflow");
        if (type.equals("set")) {
          JSONArray array = (JSONArray) value;
          for (int i = 0; i < array.length(); i++) if (
            !(array.get(i) instanceof String)
          ) throw new IOException("Invalid preference set");
        }
      }
    }
  }

  public void recover(String expected) throws Exception {
    synchronized (CHECKPOINT_LOCK) {
      recoverVerified(expected);
    }
  }

  private void recoverVerified(String expected) throws Exception {
    Checkpoint checkpoint = prepareRecovery();
    if (!checkpoint.id.equals(expected)) throw new IOException(
      "App folder changed; review recovery again"
    );
    File stage = new File(context.getFilesDir(), "app-folder-recovery-v1");
    if (stage.exists()) throw new IOException(
      "An earlier recovery stage exists. Reopen MyNotes to finish it; do not clear app data."
    );
    long available = context.getFilesDir().getUsableSpace() - 64L * 1024 * 1024;
    for (int i = 0; i < checkpoint.files.length(); i++) {
      JSONObject entry = checkpoint.files.getJSONObject(i);
      if (!entry.getBoolean("directory")) {
        long size = entry.getLong("bytes");
        if (size > available) throw new IOException(
          "Not enough device space to stage and verify complete recovery; installation unchanged"
        );
        available -= size;
      }
    }
    if (!stage.mkdir()) throw new IOException("Cannot stage recovery");
    // Before READY, failure only leaves removable private staging; no current app state changed.
    boolean ready = false;
    try {
      for (int i = 0; i < checkpoint.files.length(); i++) {
        JSONObject entry = checkpoint.files.getJSONObject(i);
        String path = entry.getString("path");
        File target = new File(stage, "files/" + path);
        if (entry.getBoolean("directory")) {
          if (!target.mkdirs() && !target.isDirectory()) throw new IOException(
            "Cannot stage folder"
          );
          continue;
        }
        if (
          !target.getParentFile().mkdirs() &&
          !target.getParentFile().isDirectory()
        ) throw new IOException("Cannot stage file");
        Uri object = fs.child(
          objects(),
          entry.getString("sha256") + ".bin",
          false,
          false
        );
        if (object == null) throw new IOException(
          "Referenced recovery object is missing"
        );
        long expectedBytes = entry.getLong("bytes"),
          written = 0,
          nextSpaceCheck = 0;
        try (
          InputStream in = fs.resolver.openInputStream(object);
          FileOutputStream out = new FileOutputStream(target)
        ) {
          if (in == null) throw new IOException("Cannot read recovery object");
          byte[] buffer = new byte[65536];
          for (int n; (n = in.read(buffer)) != -1; ) {
            if (
              written >= nextSpaceCheck &&
              stage.getUsableSpace() < 64L * 1024 * 1024
            ) throw new IOException(
              "Device space is too low for safe recovery; installation unchanged"
            );
            if (written >= nextSpaceCheck) nextSpaceCheck =
              written + 1024 * 1024;
            written += n;
            if (written > expectedBytes) throw new IOException(
              "Recovery object length mismatch"
            );
            out.write(buffer, 0, n);
          }
          out.getFD().sync();
        }
        if (
          written != expectedBytes ||
          !hash(target).equals(entry.getString("sha256"))
        ) throw new IOException(
          "Recovery object checksum mismatch; installation unchanged"
        );
        if (path.endsWith(".transcript.json")) {
          try (InputStream in = new FileInputStream(target)) {
            ArchiveJson.metadata(
              LocalOpenFile.bounded(
                in,
                app.space.notes.domain.ArchiveManifest.MAX_METADATA_BYTES
              ),
              target.getName().substring(0, 36)
            );
          }
        }
        if (path.endsWith(".note-created")) {
          try (InputStream in = new FileInputStream(target)) {
            if (in.read() != 1 || in.read() != -1) throw new IOException(
              "Invalid voice creation marker"
            );
          }
        }
        if (path.endsWith(".wav")) try (
          RandomAccessFile wave = new RandomAccessFile(target, "r")
        ) {
          app.space.notes.domain.PcmWave.inspect(wave);
        }
      }
      List<app.space.notes.domain.Note> restoredNotes = DocumentCodec.decode(
        new AtomicFile(
          new File(stage, "files/notes-library/library-v1.json")
        ).readFully()
      );
      File stagedHistory = new File(
        stage,
        "files/notes-library/edit-history-v1.json"
      );
      if (stagedHistory.exists()) {
        final File stagedFiles = new File(stage, "files");
        Context stagedContext = new android.content.ContextWrapper(context) {
          @Override
          public File getFilesDir() {
            return stagedFiles;
          }
        };
        PersistentHistoryStore history = new PersistentHistoryStore(
          stagedContext
        );
        history.load(restoredNotes);
        if (history.notice() != null) throw new IOException(
          "Edit history in checkpoint cannot be verified; nothing installed"
        );
      }
      for (String name : new String[] {
        "before-restore-v1.json",
        "before-document-editor-v7.json",
      }) {
        File saved = new File(stage, "files/notes-library/" + name);
        if (saved.exists()) DocumentCodec.decode(
          new AtomicFile(saved).readFully()
        );
      }

      Checkpoint current = inspect();
      if (
        current == null || !current.id.equals(checkpoint.id)
      ) throw new IOException("App folder changed during recovery");
      boolean connect = false;
      try {
        GenerationProtocol.Snapshot libraryNow = new GenerationProtocol(
          new LocalTreeBackend(fs.resolver, fs.tree, library())
        ).read();
        connect =
          Objects.equals(
            libraryNow.head,
            checkpoint.manifest.getString("libraryHead")
          ) &&
          NotesRepository.sameLibrary(
            restoredNotes,
            DocumentCodec.decode(libraryNow.payload)
          );
      } catch (Exception unavailable) {
        /* Complete capsule is independent of the note-generation projection. */
      }
      requireEmptyInstallation();
      JSONObject transaction = new JSONObject()
        .put("tree", fs.tree.toString())
        .put("connect", connect)
        .put("manifest", checkpoint.manifest);
      AtomicFile marker = new AtomicFile(new File(stage, "ready.json"));
      FileOutputStream out = marker.startWrite();
      try {
        out.write(transaction.toString().getBytes(StandardCharsets.UTF_8));
        marker.finishWrite(out);
      } catch (Exception e) {
        marker.failWrite(out);
        throw e;
      }
      ready = true;
      finishRecovery(context);
    } finally {
      if (!ready) removeStage(stage);
    }
  }

  /** Resume only fully validated private staging, before the controller loads. Renames are cheap;
   * large provider reads/hashes occurred on the storage worker before READY was written.
   */
  public static void finishRecovery(Context context) throws Exception {
    File stage = new File(context.getFilesDir(), "app-folder-recovery-v1");
    if (!stage.exists()) return;
    File marker = new File(stage, "ready.json");
    if (!marker.exists() && !new File(marker + ".bak").exists()) {
      removeStage(stage);
      return;
    }
    JSONObject transaction = json(new AtomicFile(marker).readFully()),
      manifest = transaction.getJSONObject("manifest");
    validate(manifest);
    NotesRepository.invalidateWriters(context);
    JSONArray files = manifest.getJSONArray("files");
    for (int i = 0; i < files.length(); i++) {
      JSONObject entry = files.getJSONObject(i);
      String path = entry.getString("path");
      File target = new File(context.getFilesDir().getCanonicalFile(), path);
      if (
        !target.getCanonicalPath().equals(target.getAbsolutePath())
      ) throw new IOException("Unsafe private recovery destination");
      if (entry.getBoolean("directory")) {
        if (!target.mkdirs() && !target.isDirectory()) throw new IOException(
          "Cannot recover folder"
        );
        continue;
      }
      File source = new File(stage, "files/" + path);
      if (
        !target.getParentFile().mkdirs() &&
        !target.getParentFile().isDirectory()
      ) throw new IOException("Cannot recover file directory");
      // Replaying a rename after a crash is idempotent. Never reconstruct bytes from UI.
      if (source.exists()) {
        if (!source.renameTo(target)) throw new IOException(
          "Recovery file install pending; reopen MyNotes"
        );
      } else if (
        !target.isFile() ||
        target.length() != entry.getLong("bytes") ||
        !entry.getString("sha256").equals(hash(target))
      ) throw new IOException(
        "Recovery staging is incomplete; source App folder retained"
      );
    }
    JSONObject preferences = manifest.getJSONObject("preferences");
    Iterator<String> names = preferences.keys();
    while (names.hasNext()) {
      String name = names.next();
      JSONObject values = preferences.getJSONObject(name);
      SharedPreferences.Editor editor = context
        .getSharedPreferences(name, 0)
        .edit()
        .clear();
      Iterator<String> keys = values.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        JSONObject value = values.getJSONObject(key);
        switch (value.getString("type")) {
          case "string":
            editor.putString(key, value.getString("value"));
            break;
          case "boolean":
            editor.putBoolean(key, value.getBoolean("value"));
            break;
          case "int":
            editor.putInt(key, value.getInt("value"));
            break;
          case "long":
            editor.putLong(key, value.getLong("value"));
            break;
          case "float":
            editor.putFloat(key, (float) value.getDouble("value"));
            break;
          case "set":
            JSONArray array = value.getJSONArray("value");
            Set<String> set = new HashSet<>();
            for (int i = 0; i < array.length(); i++) set.add(
              array.getString(i)
            );
            editor.putStringSet(key, set);
            break;
          default:
            throw new IOException("Unknown preference type");
        }
      }
      if (!editor.commit()) throw new IOException(
        "Preference recovery pending"
      );
    }
    JSONArray categories = manifest.getJSONArray("categories");
    Set<String> selected = new HashSet<>();
    for (int i = 0; i < categories.length(); i++) selected.add(
      categories.getString(i)
    );
    SharedPreferences.Editor choices = context
      .getSharedPreferences("mynotes-app-folder", 0)
      .edit();
    for (String category : new String[] {
      "voice",
      "history",
      "settings",
      "diagnostics",
    })
      choices.putBoolean(category, selected.contains(category));
    if (!choices.commit()) throw new IOException("Recovery choices pending");
    SharedPreferences.Editor destination = context
      .getSharedPreferences("mynotes-storage-v1", 0)
      .edit();
    if (transaction.optBoolean("connect", true)) destination.putString(
      "tree",
      transaction.getString("tree")
    );
    else destination.remove("tree");
    if (!destination.commit()) throw new IOException(
      "Recovery folder selection pending"
    );
    removeStage(stage);
  }

  private static void removeStage(File file) throws IOException {
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children == null) throw new IOException(
        "Cannot inspect recovery staging"
      );
      for (File child : children) removeStage(child);
    }
    if (file.exists() && !file.delete()) throw new IOException(
      "Cannot finish recovery staging cleanup"
    );
  }
}
