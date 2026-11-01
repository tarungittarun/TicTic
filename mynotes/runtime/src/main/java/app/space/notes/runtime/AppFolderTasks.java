package app.space.notes.runtime;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.FileObserver;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/** Debounced background mirror. Never queues one large recording copy per keystroke.
 * Pending survives process death via the authoritative private files and preferences;
 * startup/resume/retry always compares and checkpoints current state again.
 */
public final class AppFolderTasks {

  private static final ScheduledExecutorService WORKER =
    Executors.newSingleThreadScheduledExecutor();
  private static final Map<String, State> STATES = new HashMap<>();

  private AppFolderTasks() {}

  private static final class State {

    final Context context;
    final List<FileObserver> observers = new ArrayList<>();
    final Set<String> observed = new HashSet<>();
    final List<SharedPreferences> preferences = new ArrayList<>();
    SharedPreferences.OnSharedPreferenceChangeListener listener;
    ScheduledFuture<?> pending;
    volatile String status = "App-folder checkpoint pending";
    long revision;
    boolean running;

    State(Context context) {
      this.context = context.getApplicationContext();
    }

    void watch() {
      for (String path : new String[] {
        "",
        "notes-library",
        "voice-inbox-v1",
        "note-folders",
      }) {
        File dir = path.isEmpty()
          ? context.getFilesDir()
          : new File(context.getFilesDir(), path);
        if (!dir.isDirectory() || !observed.add(dir.getPath())) continue;
        FileObserver observer = new FileObserver(
          dir.getPath(),
          FileObserver.CLOSE_WRITE |
            FileObserver.MOVED_TO |
            FileObserver.CREATE |
            FileObserver.DELETE
        ) {
          @Override
          public void onEvent(int event, String name) {
            if (name != null && name.endsWith(".wav.part")) {
              if ((event & FileObserver.CREATE) != 0) request(context);
              return;
            }
            if (
              name != null &&
              (name.startsWith("app-folder-recovery") ||
                name.endsWith(".part") ||
                name.endsWith(".bak") ||
                name.endsWith(".new"))
            ) return;
            request(context);
          }
        };
        observers.add(observer);
        observer.startWatching();
      }
    }
  }

  private static synchronized State state(Context context) {
    String key = context.getFilesDir().getAbsolutePath();
    State found = STATES.get(key);
    if (found != null) return found;
    State state = new State(context);
    STATES.put(key, state);
    state.listener = (prefs, changedKey) -> {
      if (
        !"lastCheckpoint".equals(changedKey) && !"lastRoot".equals(changedKey)
      ) request(state.context);
    };
    List<String> names = new ArrayList<>(Arrays.asList(AppFolder.PREFS));
    names.add("mynotes-app-folder");
    names.add("mynotes-storage-v1");
    for (String name : names) {
      SharedPreferences prefs = state.context.getSharedPreferences(name, 0);
      prefs.registerOnSharedPreferenceChangeListener(state.listener);
      state.preferences.add(prefs);
    }
    state.watch();
    return state;
  }

  public static void initialize(Context context) {
    request(context);
  }

  public static void request(Context context) {
    if (
      context
        .getSharedPreferences("mynotes-storage-v1", 0)
        .getString("tree", null) == null
    ) return;
    State state = state(context);
    synchronized (state) {
      state.revision++;
      state.status = "App-folder checkpoint pending";
      if (state.pending != null) state.pending.cancel(false);
      state.pending = WORKER.schedule(
        () -> run(state),
        1500,
        TimeUnit.MILLISECONDS
      );
    }
  }

  private static void run(State state) {
    long revision;
    String root = state.context
      .getSharedPreferences("mynotes-storage-v1", 0)
      .getString("tree", null);
    synchronized (state) {
      revision = state.revision;
      state.running = true;
      state.pending = null;
      state.watch();
    }
    try {
      if (root == null) {
        state.status = "Internal storage only";
        return;
      }
      AppFolder folder = new AppFolder(state.context, Uri.parse(root));
      if (!folder.recognized()) {
        state.status =
          "Legacy note-only folder · choose a new App folder for complete checkpoints";
        return;
      }
      folder.save();
      AppFolder.Checkpoint checkpoint = folder.inspect();
      String current = state.context
        .getSharedPreferences("mynotes-storage-v1", 0)
        .getString("tree", null);
      if (!root.equals(current)) return;
      if (checkpoint == null) throw new java.io.IOException(
        "No complete checkpoint yet"
      );
      state.context
        .getSharedPreferences("mynotes-app-folder", 0)
        .edit()
        .putLong("lastCheckpoint", checkpoint.manifest.getLong("createdAt"))
        .putString("lastRoot", root)
        .commit();
      synchronized (state) {
        state.status =
          state.revision == revision
            ? "App-folder checkpoint verified · " +
              android.text.format.DateFormat.format(
                "yyyy-MM-dd HH:mm:ss",
                checkpoint.manifest.getLong("createdAt")
              )
            : "Newer app changes remain pending for the App folder";
      }
    } catch (Exception | OutOfMemoryError failure) {
      state.status =
        "Internal data retained · App-folder checkpoint pending: " +
        (failure instanceof OutOfMemoryError
          ? "not enough memory to verify safely"
          : failure.getMessage() == null
            ? failure.getClass().getSimpleName()
            : failure.getMessage()) +
        ". Review storage and Retry save; do not uninstall while pending.";
      android.util.Log.w(
        "MyNotes",
        "app-folder checkpoint pending: " + failure.getClass().getSimpleName()
      );
    } finally {
      synchronized (state) {
        state.running = false;
        if (state.revision != revision && state.pending == null) state.pending =
          WORKER.schedule(() -> run(state), 1500, TimeUnit.MILLISECONDS);
      }
    }
  }

  public static String status(Context context) {
    return context
      .getSharedPreferences("mynotes-storage-v1", 0)
      .getString("tree", null) == null
      ? "Internal storage only"
      : state(context).status;
  }

  public static boolean pending(Context context) {
    if (
      context
        .getSharedPreferences("mynotes-storage-v1", 0)
        .getString("tree", null) == null
    ) return false;
    State state = state(context);
    synchronized (state) {
      return (
        state.running ||
        state.pending != null ||
        state.status.contains("pending")
      );
    }
  }
}
