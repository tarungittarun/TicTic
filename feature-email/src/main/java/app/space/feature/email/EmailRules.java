package app.space.feature.email;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Address parsing, subject threading, reply quoting and mailbox ordering.
 *
 * <p>Pure so it is tested. Address parsing is the part that fails visibly and immediately:
 * a pasted {@code "Name <a@b.com>, other@c.com>} that is not split correctly produces one
 * malformed recipient, and the send fails with a message that does not explain why.
 */
public final class EmailRules {

    private EmailRules() {}

    public static final String FOLDER_INBOX = "inbox";
    public static final String FOLDER_DRAFTS = "drafts";
    public static final String FOLDER_SENT = "sent";

    public static final class Mail {
        public final String id;
        public final String from;
        public final List<String> to;
        public final String subject;
        public final String body;
        public final long timestamp;
        public final String folder;
        public final boolean read;

        public Mail(String id, String from, List<String> to, String subject, String body,
                    long timestamp, String folder, boolean read) {
            this.id = id == null ? "" : id;
            this.from = from == null ? "" : from.trim();
            this.to = to == null ? Collections.<String>emptyList() : to;
            this.subject = subject == null || subject.trim().isEmpty() ? "(no subject)" : subject.trim();
            this.body = body == null ? "" : body;
            this.timestamp = timestamp;
            this.folder = folder == null || folder.isEmpty() ? FOLDER_INBOX : folder;
            this.read = read;
        }

        public Mail withRead(boolean value) {
            return new Mail(id, from, to, subject, body, timestamp, folder, value);
        }
    }

    /**
     * Splits a recipient field into individual addresses, honouring quoted display names.
     *
     * <p>A naive split on commas breaks {@code "Doe, Jane <jane@example.com>"} into two
     * invalid halves. This tracks quotes and angle brackets so the comma inside a display
     * name is not treated as a separator.
     */
    public static List<String> splitRecipients(String field) {
        List<String> out = new ArrayList<>();
        if (field == null) return out;
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean inAngle = false;
        for (int i = 0; i < field.length(); i++) {
            char c = field.charAt(i);
            if (c == '"') inQuotes = !inQuotes;
            else if (c == '<' && !inQuotes) inAngle = true;
            else if (c == '>' && !inQuotes) inAngle = false;
            if (c == ',' && !inQuotes && !inAngle) {
                addIfPresent(out, current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        addIfPresent(out, current.toString());
        return out;
    }

    private static void addIfPresent(List<String> out, String value) {
        String trimmed = value.trim();
        if (!trimmed.isEmpty()) out.add(trimmed);
    }

    /** Extracts the bare address from {@code Name <a@b.com>} or returns the input trimmed. */
    public static String addressOf(String recipient) {
        if (recipient == null) return "";
        String trimmed = recipient.trim();
        int open = trimmed.indexOf('<');
        int close = trimmed.lastIndexOf('>');
        if (open >= 0 && close > open) return trimmed.substring(open + 1, close).trim();
        return trimmed;
    }

    /** Extracts the display name, falling back to the address so a row is never blank. */
    public static String nameOf(String recipient) {
        if (recipient == null) return "";
        String trimmed = recipient.trim();
        int open = trimmed.indexOf('<');
        if (open > 0 && trimmed.lastIndexOf('>') > open) {
            String name = trimmed.substring(0, open).trim().replaceAll("^\"|\"$", "");
            if (!name.isEmpty()) return name;
        }
        return addressOf(trimmed);
    }

    /**
     * Whether an address is plausibly valid.
     *
     * <p>Deliberately permissive but not naive: it rejects the shapes that make a send fail
     * silently -- no at-sign, an empty local or domain part, a space, or a domain with no
     * dot. A strict regex would reject legitimate addresses with plus-tagging or unusual
     * TLDs and the user would be told their own address is wrong.
     */
    public static boolean isValidAddress(String address) {
        String bare = addressOf(address);
        if (bare.isEmpty() || bare.indexOf(' ') >= 0) return false;
        int at = bare.indexOf('@');
        if (at <= 0 || at != bare.lastIndexOf('@')) return false;
        String local = bare.substring(0, at);
        String domain = bare.substring(at + 1);
        if (local.isEmpty() || local.length() > 64) return false;
        if (domain.isEmpty() || domain.length() > 253) return false;
        if (domain.indexOf('.') < 0) return false;
        if (domain.startsWith(".") || domain.endsWith(".") || domain.contains("..")) return false;
        return true;
    }

    /** The first invalid recipient, or null when every one is usable. */
    public static String firstInvalid(List<String> recipients) {
        if (recipients == null) return null;
        for (String r : recipients) if (!isValidAddress(r)) return r;
        return null;
    }

    /**
     * Strips Re: and Fwd: prefixes so a reply threads with the message it answers.
     *
     * <p>Applied repeatedly, because {@code Re: Fwd: Re:} accumulates over a long thread,
     * and case-insensitively because clients differ.
     */
    public static String threadKey(String subject) {
        if (subject == null) return "";
        String out = subject.trim();
        while (true) {
            String lower = out.toLowerCase(Locale.ROOT);
            if (lower.startsWith("re:")) out = out.substring(3).trim();
            else if (lower.startsWith("fwd:")) out = out.substring(4).trim();
            else if (lower.startsWith("fw:")) out = out.substring(3).trim();
            else break;
        }
        return out.toLowerCase(Locale.ROOT);
    }

    /** Groups mail into conversations newest-first. */
    public static List<Conversation> conversations(List<Mail> mail) {
        Map<String, List<Mail>> grouped = new LinkedHashMap<>();
        if (mail != null) {
            for (Mail m : mail) {
                if (m == null) continue;
                String key = threadKey(m.subject);
                List<Mail> bucket = grouped.get(key);
                if (bucket == null) { bucket = new ArrayList<>(); grouped.put(key, bucket); }
                bucket.add(m);
            }
        }
        List<Conversation> out = new ArrayList<>();
        for (Map.Entry<String, List<Mail>> e : grouped.entrySet()) {
            List<Mail> ordered = newestFirst(e.getValue());
            Mail latest = ordered.get(0);
            int unread = 0;
            for (Mail m : ordered) if (!m.read) unread++;
            out.add(new Conversation(e.getKey(), ordered, unread, latest.timestamp));
        }
        Collections.sort(out, new Comparator<Conversation>() {
            @Override public int compare(Conversation a, Conversation b) {
                if (a.lastTimestamp != b.lastTimestamp) return a.lastTimestamp < b.lastTimestamp ? 1 : -1;
                return a.key.compareTo(b.key);
            }
        });
        return out;
    }

    /** One thread of mail. */
    public static final class Conversation {
        public final String key;
        public final List<Mail> messages;
        public final int unread;
        public final long lastTimestamp;

        Conversation(String key, List<Mail> messages, int unread, long lastTimestamp) {
            this.key = key;
            this.messages = messages;
            this.unread = unread;
            this.lastTimestamp = lastTimestamp;
        }

        public String label() {
            return messages.isEmpty() || key.isEmpty() ? "(no subject)" : messages.get(0).subject;
        }

        public int size() { return messages.size(); }
    }

    public static List<Mail> newestFirst(List<Mail> mail) {
        List<Mail> out = new ArrayList<>(mail == null ? Collections.<Mail>emptyList() : mail);
        Collections.sort(out, new Comparator<Mail>() {
            @Override public int compare(Mail a, Mail b) {
                if (a.timestamp != b.timestamp) return a.timestamp < b.timestamp ? 1 : -1;
                return a.id.compareTo(b.id);
            }
        });
        return out;
    }

    /** The subject for a reply, adding Re: only when it is not already there. */
    public static String replySubject(String subject) {
        String trimmed = subject == null ? "" : subject.trim();
        if (trimmed.isEmpty()) return "Re: ";
        return trimmed.toLowerCase(Locale.ROOT).startsWith("re:") ? trimmed : "Re: " + trimmed;
    }

    /**
     * The quoted original for a reply body.
     *
     * <p>Every line is prefixed, including blanks, so a pasted reply cannot be mistaken for
     * the original text -- which matters when the quoted message is longer than the reply.
     */
    public static String quoteBody(String from, long timestamp, String body) {
        StringBuilder out = new StringBuilder("\n\nOn ");
        out.append(new java.text.SimpleDateFormat("d MMM yyyy 'at' HH:mm", Locale.getDefault())
                .format(new java.util.Date(timestamp)));
        out.append(", ").append(nameOf(from)).append(" wrote:\n");
        String text = body == null ? "" : body;
        for (String line : text.split("\n", -1)) out.append("> ").append(line).append('\n');
        return out.toString();
    }

    /**
     * A mailto URI for handing the message to the device's own mail app.
     *
     * <p>Subject and body are percent-encoded. An unencoded ampersand or newline in a
     * subject would truncate the URI and silently drop the rest of the message.
     */
    public static String mailtoUri(List<String> recipients, String subject, String body) {
        StringBuilder out = new StringBuilder("mailto:");
        if (recipients != null) {
            for (int i = 0; i < recipients.size(); i++) {
                if (i > 0) out.append(',');
                out.append(encodeAddress(addressOf(recipients.get(i))));
            }
        }
        out.append("?subject=").append(encode(subject == null ? "" : subject));
        out.append("&body=").append(encode(body == null ? "" : body));
        return out.toString();
    }

    /**
     * Percent-encodes an address while leaving addr-spec characters intact.
     *
     * <p>{@link #encode} would turn the at-sign into {@code %40}, producing
     * {@code mailto:jane%40example.com}. RFC 6068 permits that, but plenty of mail apps do
     * not resolve it, and the failure is a "no app can open this" dialog with no hint that
     * the address was the problem. Only characters that cannot appear in an address are
     * escaped; the subject and body still go through the full encoder, where an ampersand or
     * a newline really would truncate the URI.
     */
    static String encodeAddress(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean safe = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '@' || c == '.' || c == '_' || c == '-' || c == '+';
            if (safe) out.append(c);
            else out.append(encode(String.valueOf(c)));
        }
        return out.toString();
    }

    static String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }

    /** Only mail in the given folder. */
    public static List<Mail> inFolder(List<Mail> mail, String folder) {
        List<Mail> out = new ArrayList<>();
        if (mail == null) return out;
        for (Mail m : mail) if (m != null && m.folder.equals(folder)) out.add(m);
        return newestFirst(out);
    }

    public static int unreadCount(List<Mail> mail, String folder) {
        int count = 0;
        for (Mail m : inFolder(mail, folder)) if (!m.read) count++;
        return count;
    }

    /** Whether there is anything worth sending. */
    public static boolean isSendable(List<String> recipients, String body) {
        if (recipients == null || recipients.isEmpty()) return false;
        if (firstInvalid(recipients) != null) return false;
        return body != null && !body.trim().isEmpty();
    }

    /** Collapses whitespace and truncates, for the preview line under each conversation. */
    public static String preview(String body) {
        if (body == null) return "";
        String flat = body.replaceAll("\\s+", " ").trim();
        return flat.length() <= 120 ? flat : flat.substring(0, 119) + "\u2026";
    }

    /** Case-insensitive search over subject, body and sender. */
    public static List<Mail> filter(List<Mail> mail, String query) {
        List<Mail> all = newestFirst(mail);
        if (query == null || query.trim().isEmpty()) return all;
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<Mail> out = new ArrayList<>();
        for (Mail m : all) {
            if (m.subject.toLowerCase(Locale.ROOT).contains(q)
                    || m.body.toLowerCase(Locale.ROOT).contains(q)
                    || m.from.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
        }
        return out;
    }
}
