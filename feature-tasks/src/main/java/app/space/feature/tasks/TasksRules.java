package app.space.feature.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Task ordering and overdue arithmetic. Pure Java, so it is really tested.
 *
 * <p>The ordering is the whole product here: a list that buries an overdue task under
 * completed ones, or that reshuffles while the user is reading it, is worse than no list.
 */
public final class TasksRules {

    private TasksRules() {}

    /** No due date. Zero is not used, because that is a valid epoch timestamp. */
    public static final long NO_DUE = -1L;

    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_NORMAL = 1;
    public static final int PRIORITY_HIGH = 2;

    public static final class Task {
        public final String id;
        public final String title;
        public final boolean done;
        public final long dueAt;
        public final int priority;

        public Task(String id, String title, boolean done, long dueAt, int priority) {
            this.id = id == null ? "" : id;
            this.title = title == null || title.trim().isEmpty() ? "Untitled task" : title.trim();
            this.done = done;
            this.dueAt = dueAt;
            this.priority = clampPriority(priority);
        }

        public Task withDone(boolean value) {
            return new Task(id, title, value, dueAt, priority);
        }

        public boolean hasDue() { return dueAt != NO_DUE; }
    }

    public static int clampPriority(int priority) {
        return priority < PRIORITY_LOW ? PRIORITY_LOW : (priority > PRIORITY_HIGH ? PRIORITY_HIGH : priority);
    }

    /**
     * Incomplete first, then earliest due, then highest priority, then title.
     *
     * <p>Tasks with no due date sort after every dated task, which is the opposite of what
     * comparing {@code NO_DUE} as a timestamp would give -- -1 would float them to the top
     * of the list and bury anything actually due today.
     */
    public static List<Task> sorted(List<Task> tasks) {
        List<Task> out = new ArrayList<>(tasks == null ? Collections.<Task>emptyList() : tasks);
        Collections.sort(out, new Comparator<Task>() {
            @Override public int compare(Task a, Task b) {
                if (a.done != b.done) return a.done ? 1 : -1;
                if (a.hasDue() != b.hasDue()) return a.hasDue() ? -1 : 1;
                if (a.hasDue() && a.dueAt != b.dueAt) return a.dueAt < b.dueAt ? -1 : 1;
                if (a.priority != b.priority) return b.priority - a.priority;
                int byTitle = a.title.compareToIgnoreCase(b.title);
                // Last resort is the id, so two tasks titled identically cannot swap places
                // between renders.
                return byTitle != 0 ? byTitle : a.id.compareTo(b.id);
            }
        });
        return out;
    }

    /** A task is overdue once its due time has passed and it is still incomplete. */
    public static boolean isOverdue(Task task, long now) {
        return task != null && !task.done && task.hasDue() && task.dueAt < now;
    }

    /** Due today, in the half-open range so a task due at midnight is not also due yesterday. */
    public static boolean isDueToday(Task task, long todayStart, long tomorrowStart) {
        return task != null && task.hasDue()
                && task.dueAt >= todayStart && task.dueAt < tomorrowStart;
    }

    /** {@code {total, done, overdue}} for the header counts. */
    public static int[] counts(List<Task> tasks, long now) {
        int total = 0;
        int done = 0;
        int overdue = 0;
        if (tasks != null) {
            for (Task t : tasks) {
                if (t == null) continue;
                total++;
                if (t.done) done++;
                else if (isOverdue(t, now)) overdue++;
            }
        }
        return new int[]{total, done, overdue};
    }

    /** Case-insensitive title search; a blank query matches everything. */
    public static List<Task> filter(List<Task> tasks, String query) {
        List<Task> all = sorted(tasks);
        if (query == null || query.trim().isEmpty()) return all;
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<Task> out = new ArrayList<>();
        for (Task t : all) if (t.title.toLowerCase(Locale.ROOT).contains(q)) out.add(t);
        return out;
    }

    /** "Overdue", "Due today", "Due tomorrow", a short date, or "" when there is no due. */
    public static String dueLabel(Task task, long now, long todayStart) {
        if (task == null || !task.hasDue()) return "";
        if (isOverdue(task, now)) return "Overdue";
        long days = (task.dueAt - todayStart) / 86400000L;
        if (task.dueAt >= todayStart && days == 0) return "Due today";
        if (days == 1) return "Due tomorrow";
        return "Due " + new java.text.SimpleDateFormat("d MMM", Locale.getDefault())
                .format(new java.util.Date(task.dueAt));
    }

    public static List<Task> capped(List<Task> tasks, int max) {
        List<Task> all = sorted(tasks);
        if (max <= 0 || all.size() <= max) return all;
        return new ArrayList<>(all.subList(0, max));
    }
}
