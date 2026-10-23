package app.space.core.data;

import java.util.Locale;

/**
 * The pure half of file sharing: authority naming, content-URI shape, mime lookup and the
 * path-containment check. {@link ShareProvider} is the framework half; this class is what the
 * JVM tests can reach, and it is where the security rule lives -- a share URI may only ever
 * resolve to a file inside our own data directory.
 */
public final class ShareRules {

    private ShareRules() { }

    /** Suffix on the package name; matches the provider declared in core-data's manifest. */
    public static final String AUTHORITY_SUFFIX = ".myspace_share";

    public static String authority(String packageName) {
        return (packageName == null ? "" : packageName) + AUTHORITY_SUFFIX;
    }

    /**
     * content://&lt;pkg&gt;.myspace_share/data/user/0/&lt;pkg&gt;/files/... -- the absolute path
     * travels as the URI path, which is exactly how the platform FileProvider encodes it, minus
     * the xml path tree (we enforce containment in code instead).
     */
    public static String uriString(String packageName, String absolutePath) {
        StringBuilder out = new StringBuilder("content://").append(authority(packageName));
        if (absolutePath != null && !absolutePath.isEmpty()) {
            for (String segment : absolutePath.split("/")) {
                if (segment.isEmpty()) continue;
                out.append('/').append(segment);
            }
        }
        return out.toString();
    }

    /**
     * True only when the requested path is the data root itself or lives inside it. Canonical
     * paths are compared, so ".." and symlink games lose.
     */
    public static boolean isInsideRoot(String dataRootCanonical, String requestedCanonical) {
        if (dataRootCanonical == null || requestedCanonical == null) return false;
        if (requestedCanonical.isEmpty()) return false;
        return requestedCanonical.equals(dataRootCanonical)
                || requestedCanonical.startsWith(dataRootCanonical + "/");
    }

    /** Mime by extension; the fallback is octet-stream so no receiver ever sees null. */
    public static String mimeFor(String fileName) {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (name.endsWith(".m4a") || name.endsWith(".mp4")) return "audio/mp4";
        if (name.endsWith(".mp3")) return "audio/mpeg";
        if (name.endsWith(".wav")) return "audio/wav";
        if (name.endsWith(".3gpp") || name.endsWith(".3gp")) return "audio/3gpp";
        if (name.endsWith(".ogg") || name.endsWith(".oga")) return "audio/ogg";
        if (name.endsWith(".flac")) return "audio/flac";
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".log")) return "text/plain";
        if (name.endsWith(".csv")) return "text/csv";
        if (name.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }

    /**
     * The renamed file name for a recording: sanitised, extension kept, null when the owner
     * typed nothing usable. The caller compares against existing names.
     */
    public static String renamedFile(String wanted, String currentName) {
        if (wanted == null) return null;
        String base = wanted.trim();
        if (base.isEmpty()) return null;
        String ext = "";
        int dot = currentName == null ? -1 : currentName.lastIndexOf('.');
        if (dot >= 0 && currentName != null) ext = currentName.substring(dot);
        if (base.toLowerCase(Locale.ROOT).endsWith(ext.toLowerCase(Locale.ROOT)) && !ext.isEmpty()) {
            base = base.substring(0, base.length() - ext.length());
        }
        base = sanitise(base);
        if (base.isEmpty()) return null;
        return base + ext;
    }

    /** Conservative name hygiene: letters, digits, dash, underscore, space, dot. */
    public static String sanitise(String label) {
        if (label == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == ' ' || c == '.';
            if (ok) out.append(c);
            else if (c == '/' || c == '\\') out.append('-');
        }
        while (out.length() > 0 && out.charAt(0) == '.') out.deleteCharAt(0);
        return out.toString().trim();
    }
}
