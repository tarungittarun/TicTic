package app.space.feature.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TasksRulesTest {

    private static final long NOW = 1_700_000_000_000L;
    private static final long DAY = 86400000L;
    private static final long TODAY = NOW - (NOW % DAY);

    private static TasksRules.Task t(String id, boolean done, long due, int priority) {
        return new TasksRules.Task(id, "task " + id, done, due, priority);
    }

    @Test public void incompleteTasksSortBeforeCompletedOnes() {
        List<TasksRules.Task> out = TasksRules.sorted(Arrays.asList(
                t("done", true, TasksRules.NO_DUE, 0), t("open", false, TasksRules.NO_DUE, 0)));
        assertEquals("open", out.get(0).id);
        assertEquals("done", out.get(1).id);
    }

    @Test public void tasksWithNoDueDateSortAfterEveryDatedTask() {
        // NO_DUE is -1. Compared as a timestamp it would float undated tasks to the top of
        // the list and bury anything actually due today, which is the one thing a task list
        // must not do.
        List<TasksRules.Task> out = TasksRules.sorted(Arrays.asList(
                t("undated", false, TasksRules.NO_DUE, TasksRules.PRIORITY_HIGH),
                t("later", false, TODAY + 10 * DAY, TasksRules.PRIORITY_LOW),
                t("today", false, TODAY, TasksRules.PRIORITY_LOW)));
        assertEquals("today", out.get(0).id);
        assertEquals("later", out.get(1).id);
        assertEquals("undated", out.get(2).id);
    }

    @Test public void dueDateBeatsPriorityAmongIncompleteTasks() {
        List<TasksRules.Task> out = TasksRules.sorted(Arrays.asList(
                t("highLater", false, TODAY + 5 * DAY, TasksRules.PRIORITY_HIGH),
                t("lowToday", false, TODAY, TasksRules.PRIORITY_LOW)));
        assertEquals("lowToday", out.get(0).id);
    }

    @Test public void priorityThenTitleThenIdBreakRemainingTies() {
        List<TasksRules.Task> same = Arrays.asList(
                new TasksRules.Task("b", "same", false, TODAY, TasksRules.PRIORITY_LOW),
                new TasksRules.Task("a", "same", false, TODAY, TasksRules.PRIORITY_HIGH),
                new TasksRules.Task("d", "same", false, TODAY, TasksRules.PRIORITY_LOW));
        List<TasksRules.Task> out = TasksRules.sorted(same);
        assertEquals("a", out.get(0).id);
        // b and d tie on everything but id, so the order is stable across renders and the
        // list cannot reshuffle under the user's finger.
        assertEquals("b", out.get(1).id);
        assertEquals("d", out.get(2).id);
    }

    @Test public void overdueRequiresAnActualDueTimeInThePast() {
        assertTrue(TasksRules.isOverdue(t("a", false, NOW - 1, 0), NOW));
        assertFalse(TasksRules.isOverdue(t("b", false, NOW + 1, 0), NOW));
        assertFalse(TasksRules.isOverdue(t("c", false, TasksRules.NO_DUE, 0), NOW));
        // A completed task is never overdue, however old its due date.
        assertFalse(TasksRules.isOverdue(t("d", true, NOW - DAY, 0), NOW));
        assertFalse(TasksRules.isOverdue(null, NOW));
    }

    @Test public void dueTodayIsHalfOpen() {
        assertTrue(TasksRules.isDueToday(t("a", false, TODAY, 0), TODAY, TODAY + DAY));
        assertTrue(TasksRules.isDueToday(t("b", false, TODAY + DAY - 1, 0), TODAY, TODAY + DAY));
        assertFalse(TasksRules.isDueToday(t("c", false, TODAY + DAY, 0), TODAY, TODAY + DAY));
        assertFalse(TasksRules.isDueToday(t("d", false, TasksRules.NO_DUE, 0), TODAY, TODAY + DAY));
    }

    @Test public void countsSeparateTotalDoneAndOverdue() {
        List<TasksRules.Task> list = Arrays.asList(
                t("a", false, NOW - DAY, 0), t("b", false, NOW + DAY, 0),
                t("c", true, NOW - DAY, 0), t("d", false, TasksRules.NO_DUE, 0));
        int[] counts = TasksRules.counts(list, NOW);
        assertEquals(4, counts[0]);
        assertEquals(1, counts[1]);
        assertEquals(1, counts[2]);
        assertEquals(0, TasksRules.counts(null, NOW)[0]);
    }

    @Test public void searchIsCaseInsensitiveAndABlankQueryMatchesEverything() {
        List<TasksRules.Task> list = Arrays.asList(
                new TasksRules.Task("1", "Buy Milk", false, TasksRules.NO_DUE, 0),
                new TasksRules.Task("2", "write report", false, TasksRules.NO_DUE, 0));
        assertEquals(1, TasksRules.filter(list, "MILK").size());
        assertEquals(1, TasksRules.filter(list, "report").size());
        assertEquals(2, TasksRules.filter(list, "").size());
        assertEquals(2, TasksRules.filter(list, null).size());
        assertEquals(0, TasksRules.filter(list, "nothing").size());
    }

    @Test public void dueLabelNamesTheUrgentCasesInWords() {
        assertEquals("Overdue", TasksRules.dueLabel(t("a", false, NOW - 1, 0), NOW, TODAY));
        // NOW is 22 hours into TODAY, so "an hour after midnight" is already in the past
        // and would correctly read as Overdue. Anchoring to NOW keeps this case actually in
        // the future while still falling inside today.
        assertEquals("Due today", TasksRules.dueLabel(t("b", false, NOW + 3600000L, 0), NOW, TODAY));
        assertEquals("Due tomorrow", TasksRules.dueLabel(t("c", false, TODAY + DAY + 3600000L, 0), NOW, TODAY));
        assertEquals("", TasksRules.dueLabel(t("d", false, TasksRules.NO_DUE, 0), NOW, TODAY));
        // A completed task still shows its date; hiding it would make the row look undated.
        assertTrue(TasksRules.dueLabel(t("e", true, NOW - DAY, 0), NOW, TODAY).length() > 0);
    }

    @Test public void priorityIsClampedSoABadStoredValueCannotBreakOrdering() {
        assertEquals(TasksRules.PRIORITY_HIGH, new TasksRules.Task("1", "a", false, TasksRules.NO_DUE, 99).priority);
        assertEquals(TasksRules.PRIORITY_LOW, new TasksRules.Task("1", "a", false, TasksRules.NO_DUE, -5).priority);
    }

    @Test public void withDonePreservesIdentityAndDueDate() {
        TasksRules.Task original = t("a", false, TODAY, TasksRules.PRIORITY_HIGH);
        TasksRules.Task done = original.withDone(true);
        assertEquals("a", done.id);
        assertEquals(TODAY, done.dueAt);
        assertEquals(TasksRules.PRIORITY_HIGH, done.priority);
        assertTrue(done.done);
        assertFalse(original.done);
    }

    @Test public void anUntitledTaskGetsAPlaceholder() {
        assertEquals("Untitled task", new TasksRules.Task("1", "  ", false, TasksRules.NO_DUE, 0).title);
        assertEquals("Untitled task",
                new TasksRules.Task("1", null, false, TasksRules.NO_DUE, 0).title);
    }

    @Test public void theCapKeepsTheMostRelevantNotTheMostRecent() {
        List<TasksRules.Task> in = new ArrayList<>();
        in.add(t("old", false, TODAY - 10 * DAY, 0));
        in.add(t("done", true, TasksRules.NO_DUE, 0));
        in.add(t("soon", false, TODAY, 0));
        List<TasksRules.Task> out = TasksRules.capped(in, 2);
        assertEquals(2, out.size());
        // capped() keeps the head of the SORTED list, so it keeps the most urgent and drops
        // the completed task -- not the most recent, which is what a naive cap would do.
        assertEquals("old", out.get(0).id);
        assertEquals("soon", out.get(1).id);
    }
}
