package app.space.core.suite;

import android.view.ViewGroup;

/**
 * A Settings category contributed by a {@link SuiteApp}.
 *
 * <p>Settings is a full-page screen with a category rail and independently bounded
 * scrolling; on compact screens the categories scroll horizontally, on wider screens
 * they form a left rail. Today those categories are hardcoded inside
 * {@code MainActivity}. Letting each app contribute its own is what allows thirteen
 * apps to share one Settings page without it becoming a wall of unrelated toggles —
 * and it means an app that is hidden, excluded from a {@code lite} flavour or not yet
 * downloaded simply contributes nothing, with no leftover empty category.
 *
 * <p>Content is built into a supplied container rather than returned as a {@link View},
 * because the existing settings groups add several sibling panels into a shared
 * scrolling body and need to preserve scroll position when a single option changes.
 */
public interface SettingsGroup {

    /**
     * Stable identifier, e.g. {@code "alarms"}.
     *
     * <p>Used as the target of {@link ScreenHost#openSettings(String)} and persisted
     * when the user's last-visited category is remembered, so it must not change once
     * shipped. Same comma-free constraint as {@link SuiteApp#id()}.
     */
    String id();

    /** Category name shown in the rail. */
    String title();

    /**
     * Populates this group's controls.
     *
     * <p>Colours must come from {@link ScreenHost#palette()}. Implementations should
     * read and write their own preferences directly through {@code :core-data}; the
     * shell does not mediate settings storage, and must not, or every app's preferences
     * would funnel through one class again.
     */
    void build(ScreenHost host, ViewGroup container);
}
