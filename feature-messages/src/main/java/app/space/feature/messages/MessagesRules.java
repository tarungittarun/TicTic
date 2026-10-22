package app.space.feature.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Phone-number normalisation, conversation threading and ordering.
 *
 * <p>Pure so it is tested. The normalisation is the part that actually breaks in use: the
 * same person arrives as {@code +91 98765 43210}, {@code 098765 43210} and
 * {@code 9876543210} from different senders, and without folding those together one
 * conversation renders as three.
 */
public final class MessagesRules {

    private MessagesRules() {}

    /** Digits kept when comparing numbers. A local number is matched on its last nine. */
    public static final int COMPARE_DIGITS = 9;

    public static final class Message {
        public final String id;
        public final String address;
        public final String body;
        public final long timestamp;
        public final boolean incoming;
        public final boolean read;

        public Message(String id, String address, String body, long timestamp,
                       boolean incoming, boolean read) {
            this.id = id == null ? "" : id;
            this.address = address == null ? "" : address.trim();
            this.body = body == null ? "" : body;
            this.timestamp = timestamp;
            this.incoming = incoming;
            this.read = read;
        }

        public Message withRead(boolean value) {
            return new Message(id, address, body, timestamp, incoming, value);
        }
    }

    /**
     * Reduces an address to the digits that identify it, so formatting differences and a
     * country code do not split a thread.
     *
     * <p>Returns "" for a sender with no address, which happens for carrier and service
     * short codes. Those are grouped under one "Unknown sender" thread rather than each
     * becoming its own conversation.
     */
    public static String normalise(String address) {
        if (address == null) return "";
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < address.length(); i++) {
            char c = address.charAt(i);
            if (c >= '0' && c <= '9') digits.append(c);
        }
        String out = digits.toString();
        if (out.length() > COMPARE_DIGITS) out = out.substring(out.length() - COMPARE_DIGITS);
        return out;
    }

    /** Whether an address is a short code rather than a phone number. */
    public static boolean isShortCode(String address) {
        String digits = digitsOnly(address);
        return !digits.isEmpty() && digits.length() <= 6;
    }

    static String digitsOnly(String value) {
        StringBuilder out = new StringBuilder();
        if (value != null) for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') out.append(c);
        }
        return out.toString();
    }

    /** The label shown for a thread. Falls back to a readable placeholder, never blank. */
    public static String threadLabel(String address) {
        if (address == null || address.trim().isEmpty()) return "Unknown sender";
        return address.trim();
    }

    /**
     * Groups messages into conversations keyed by the normalised address, newest thread
     * first.
     *
     * <p>A {@link LinkedHashMap} keeps insertion order so two threads with the same last
     * message time cannot swap places between renders.
     */
    public static List<Thread> threads(List<Message> messages) {
        Map<String, List<Message>> grouped = new LinkedHashMap<>();
        if (messages != null) {
            for (Message m : messages) {
                if (m == null) continue;
                String key = normalise(m.address);
                List<Message> bucket = grouped.get(key);
                if (bucket == null) { bucket = new ArrayList<>(); grouped.put(key, bucket); }
                bucket.add(m);
            }
        }
        List<Thread> out = new ArrayList<>();
        for (Map.Entry<String, List<Message>> e : grouped.entrySet()) {
            List<Message> ordered = chronological(e.getValue());
            Message newest = ordered.get(ordered.size() - 1);
            int unread = 0;
            for (Message m : ordered) if (!m.read && m.incoming) unread++;
            out.add(new Thread(e.getKey(), newest.address, ordered, unread, newest.timestamp));
        }
        Collections.sort(out, new Comparator<Thread>() {
            @Override public int compare(Thread a, Thread b) {
                if (a.lastTimestamp != b.lastTimestamp) return a.lastTimestamp < b.lastTimestamp ? 1 : -1;
                // Ties on the key, not the raw address, so the order cannot depend on which
                // formatting of the number happened to arrive first.
                return a.key.compareTo(b.key);
            }
        });
        return out;
    }

    /** Oldest first, which is how a conversation reads. */
    public static List<Message> chronological(List<Message> messages) {
        List<Message> out = new ArrayList<>(messages == null ? Collections.<Message>emptyList() : messages);
        Collections.sort(out, new Comparator<Message>() {
            @Override public int compare(Message a, Message b) {
                if (a.timestamp != b.timestamp) return a.timestamp < b.timestamp ? -1 : 1;
                return a.id.compareTo(b.id);
            }
        });
        return out;
    }

    /** One conversation. */
    public static final class Thread {
        public final String key;
        public final String address;
        public final List<Message> messages;
        public final int unread;
        public final long lastTimestamp;

        Thread(String key, String address, List<Message> messages, int unread, long lastTimestamp) {
            this.key = key;
            this.address = address;
            this.messages = messages;
            this.unread = unread;
            this.lastTimestamp = lastTimestamp;
        }

        public String label() { return threadLabel(address); }

        public String preview() {
            if (messages.isEmpty()) return "";
            return singleLine(messages.get(messages.size() - 1).body);
        }
    }

    /** Collapses whitespace and truncates, for the one-line preview under each thread. */
    public static String singleLine(String body) {
        if (body == null) return "";
        String flat = body.replaceAll("\\s+", " ").trim();
        return flat.length() <= 80 ? flat : flat.substring(0, 79) + "\u2026";
    }

    /** Total unread across every thread, for the badge. */
    public static int totalUnread(List<Message> messages) {
        int sum = 0;
        for (Thread t : threads(messages)) sum += t.unread;
        return sum;
    }

    /**
     * Whether a body is safe to hand to the SMS manager.
     *
     * <p>Empty and whitespace-only bodies are refused: sending one costs the user a charge
     * and delivers nothing, and it is easy to do by accident from a dialog.
     */
    public static boolean isSendable(String body) {
        return body != null && !body.trim().isEmpty() && body.trim().length() <= 1000;
    }

    /** Segments a body the way the SMS manager will, so the count shown matches the charge. */
    public static int segmentCount(String body) {
        if (body == null || body.isEmpty()) return 0;
        int limit = isSevenBit(body) ? 160 : 70;
        return (body.length() + limit - 1) / limit;
    }

    /** A body with any non-GSM character is sent as UCS-2, which halves the segment size. */
    public static boolean isSevenBit(String body) {
        if (body == null) return true;
        for (int i = 0; i < body.length(); i++) if (body.charAt(i) > 0x7F) return false;
        return true;
    }

    /** Case-insensitive search over bodies and addresses. */
    public static List<Message> filter(List<Message> messages, String query) {
        List<Message> all = chronological(messages);
        if (query == null || query.trim().isEmpty()) return all;
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<Message> out = new ArrayList<>();
        for (Message m : all) {
            if (m.body.toLowerCase(Locale.ROOT).contains(q)
                    || m.address.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
        }
        return out;
    }
}
