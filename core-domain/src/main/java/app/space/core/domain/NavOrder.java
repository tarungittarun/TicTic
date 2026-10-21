package app.space.core.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The rules for resolving which order apps appear in, and for encoding id lists.
 *
 * <p>Kept here rather than in {@code :shell} on purpose. This is the one piece of
 * navigation logic that is genuinely tricky, and it is tricky in a way that only shows
 * up over time: the stored order was written when the user had some set of apps, and by
 * the next launch that set may have changed in either direction. An app may be gone
 * (removed, or excluded by a {@code lite} flavour), and — far more often as MySpace grows
 * from one app toward thirteen — a newly added app has no stored position at all.
 *
 * <p>Getting that wrong is not a crash, it is navigation quietly rearranging itself or a
 * new app never appearing. Both are hard to notice in review, so the rules live in the
 * pure-JVM module where they can be unit-tested directly. {@code :shell} only adapts
 * preferences to and from these functions; it holds no ordering logic of its own.
 *
 * <p>No Android imports, consistent with the rest of {@code :core-domain}.
 */
public final class NavOrder {

    /**
     * The minimum an entry needs to be ordered.
     *
     * <p>{@code SuiteApp} in {@code :core-suite} extends this, so the shell can pass its
     * apps straight in without an adapter — the three methods are already declared there
     * with identical signatures.
     */
    public interface Entry {
        /** Stable identifier. Unique within a registration. */
        String id();
        /** Position before the user rearranges; lower sorts first. */
        int defaultOrder();
        /** Used only to break ties deterministically. */
        String label();
    }

    private NavOrder() {}

    /**
     * Resolves display order from a stored id list and the apps actually registered.
     *
     * <p>Rules, in order:
     * <ol>
     *   <li>Stored ids still registered keep the user's arrangement, in the stored order.</li>
     *   <li>Stored ids no longer registered are dropped. They are <em>not</em> written
     *       back anywhere, so an app that returns in a later build reappears where the
     *       user originally put it rather than being appended as if new.</li>
     *   <li>Registered apps with no stored position are appended, sorted by
     *       {@link Entry#defaultOrder()} then by label.</li>
     * </ol>
     *
     * <p>Label is the tie-break so two apps sharing a {@code defaultOrder} still produce
     * a stable result instead of one that depends on list iteration order — otherwise
     * navigation could flicker between renders.
     *
     * <p>Duplicate ids in either input are collapsed, and a {@code null} entry or id is
     * skipped rather than throwing: this runs on every navigation render, and a corrupt
     * preference should degrade to a slightly odd order, not take the app down.
     */
    public static <E extends Entry> List<E> resolve(Collection<String> storedIds, List<E> registered) {
        if (registered == null || registered.isEmpty()) return Collections.emptyList();

        List<E> out = new ArrayList<>(registered.size());
        Set<String> placed = new LinkedHashSet<>();

        if (storedIds != null) {
            for (String id : storedIds) {
                if (id == null || !placed.add(id)) continue;
                E match = findById(registered, id);
                if (match == null) {
                    // Not registered any more: forget it was placed, so a later
                    // re-registration is treated as new rather than silently dropped.
                    placed.remove(id);
                    continue;
                }
                out.add(match);
            }
        }

        List<E> remaining = new ArrayList<>();
        Set<String> seenRemaining = new LinkedHashSet<>();
        for (E entry : registered) {
            if (entry == null || entry.id() == null) continue;
            if (placed.contains(entry.id())) continue;
            if (!seenRemaining.add(entry.id())) continue;
            remaining.add(entry);
        }
        Collections.sort(remaining, (a, b) -> {
            int byOrder = Integer.compare(a.defaultOrder(), b.defaultOrder());
            if (byOrder != 0) return byOrder;
            String left = a.label() == null ? "" : a.label();
            String right = b.label() == null ? "" : b.label();
            return left.compareTo(right);
        });
        out.addAll(remaining);
        return out;
    }

    private static <E extends Entry> E findById(List<E> entries, String id) {
        for (E entry : entries) {
            if (entry != null && id.equals(entry.id())) return entry;
        }
        return null;
    }

    /**
     * Parses a comma-joined id list.
     *
     * <p>Returns a {@link LinkedHashSet} because order is meaningful here — it feeds
     * {@link #resolve} as the user's arrangement — and because de-duplicating stops a
     * repeated id from being counted twice. Blank entries are dropped, so a trailing or
     * doubled comma is harmless instead of producing a phantom empty id.
     */
    public static Set<String> parse(String joined) {
        Set<String> out = new LinkedHashSet<>();
        if (joined == null || joined.isEmpty()) return out;
        for (String part : Arrays.asList(joined.split(","))) {
            String id = part.trim();
            if (!id.isEmpty()) out.add(id);
        }
        return out;
    }

    /** Encodes ids as a comma-joined string. Nulls and blanks are skipped. */
    public static String join(Iterable<String> ids) {
        if (ids == null) return "";
        StringBuilder b = new StringBuilder();
        for (String id : ids) {
            if (id == null) continue;
            String trimmed = id.trim();
            if (trimmed.isEmpty()) continue;
            if (b.length() > 0) b.append(',');
            b.append(trimmed);
        }
        return b.toString();
    }
}
