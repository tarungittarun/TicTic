"""Extract bounded, non-sensitive QEMU crash metadata from Ubuntu Apport reports.

Apport may still be writing a large CoreDump attachment after the emulator has
exited. This reader stops *before* that attachment, retains no raw report or
core, and exposes only a whitelisted signal and top stack frames. It never reads
ProcCmdline, ProcEnviron, registers, locals, or memory.
"""
from pathlib import Path
import re
import time


HEADER_BYTES_LIMIT = 1024 * 1024
LINE_BYTES_LIMIT = 4096
REPORTS_LIMIT = 16
STACK_FRAMES_LIMIT = 24
STACK_LINE_LIMIT = 320

_FIELD_RE = re.compile(rb"^([A-Za-z][A-Za-z0-9-]*):(?:[ \t]*(.*))?$")
_SAFE_NAME_RE = re.compile(r"[A-Za-z0-9_.+\-]{1,128}\Z")
_SENSITIVE_VALUE_RE = re.compile(
    r"\b(?:token|secret|password|credential|authorization)\b\s*[:=]\s*\S+",
    re.IGNORECASE,
)


def _basename(value):
    text = str(value or "").strip().replace("\\", "/")
    name = text.rsplit("/", 1)[-1]
    return name if _SAFE_NAME_RE.fullmatch(name) else "[name suppressed]"


def _safe_stack_line(value):
    """Keep a compact symbol/module frame; suppress paths, addresses and values."""
    text = value.decode("utf-8", errors="replace") if isinstance(value, bytes) else str(value)
    text = text.strip()
    if not text:
        return None
    # Keep module basenames but never CI paths or home-directory names.
    text = re.sub(r"(?<![A-Za-z0-9_.+\-])/(?:[^\s(),;]+/)*([^/\s(),;]+)", r"/\1", text)
    text = re.sub(r"\b0x[0-9a-fA-F]{4,}\b", "<address>", text)
    text = _SENSITIVE_VALUE_RE.sub("<redacted>", text)
    text = re.sub(r"[\x00-\x1f\x7f]", " ", text)
    text = re.sub(r"[^A-Za-z0-9_.$<>#():+~\-/\[\] ]", " ", text)
    text = " ".join(text.split())
    if not text:
        return None
    return text[:STACK_LINE_LIMIT]


def parse_report(path, root=Path("/var/crash")):
    """Read only safe text fields before CoreDump; return None for non-QEMU reports."""
    root = Path(root).resolve()
    path = Path(path)
    try:
        if path.is_symlink() or not path.is_file():
            return None
        resolved = path.resolve(strict=True)
        if not resolved.is_relative_to(root):
            return None
        stat = resolved.stat()
    except OSError:
        return None

    fields = {}
    stack = []
    current_field = None
    bytes_read = 0
    try:
        with resolved.open("rb") as stream:
            while bytes_read < HEADER_BYTES_LIMIT:
                line = stream.readline(min(LINE_BYTES_LIMIT, HEADER_BYTES_LIMIT - bytes_read))
                if not line:
                    break
                bytes_read += len(line)
                if b"\x00" in line:
                    break
                match = _FIELD_RE.match(line.rstrip(b"\r\n"))
                if match:
                    key = match.group(1).decode("ascii", errors="ignore")
                    value = (match.group(2) or b"").decode("utf-8", errors="replace").strip()
                    if key == "CoreDump":
                        # Never read even one byte of the raw-memory attachment.
                        break
                    current_field = key
                    if key in {"ProblemType", "ExecutablePath", "Signal"}:
                        fields[key] = value[:512]
                    elif key == "StacktraceTop":
                        if value:
                            frame = _safe_stack_line(value)
                            if frame:
                                stack.append(frame)
                    continue
                if current_field == "StacktraceTop" and line[:1] in (b" ", b"\t"):
                    frame = _safe_stack_line(line)
                    if frame and len(stack) < STACK_FRAMES_LIMIT:
                        stack.append(frame)
                elif line.strip():
                    current_field = None
    except OSError:
        return None

    executable = _basename(fields.get("ExecutablePath"))
    if not executable.startswith("qemu-system-"):
        return None
    signal_value = fields.get("Signal", "")
    signal_value = signal_value if re.fullmatch(r"\d{1,3}", signal_value) else None
    problem_type = fields.get("ProblemType", "")
    problem_type = problem_type if re.fullmatch(r"[A-Za-z ]{1,48}", problem_type) else None
    return {
        "executable_basename": executable,
        "signal_number": signal_value,
        "problem_type": problem_type,
        "report_bytes": stat.st_size,
        "header_bytes_examined": bytes_read,
        "stacktrace_top": stack[:STACK_FRAMES_LIMIT],
        "raw_report_retained": False,
        "raw_core_read": False,
        "process_arguments_read": False,
        "process_environment_read": False,
        "registers_or_locals_read": False,
    }


def _report_paths(root):
    root = Path(root)
    try:
        if root.is_symlink() or not root.is_dir():
            return []
        resolved_root = root.resolve(strict=True)
        paths = []
        for path in sorted(root.glob("*.crash"))[:REPORTS_LIMIT]:
            try:
                if path.is_symlink() or not path.is_file():
                    continue
                if not path.resolve(strict=True).is_relative_to(resolved_root):
                    continue
                paths.append(path)
            except OSError:
                continue
        return paths
    except OSError:
        return []


def collect(root=Path("/var/crash")):
    """Return safe summaries from already-written qemu-system Apport reports."""
    summaries = []
    for path in _report_paths(root):
        result = parse_report(path, root)
        if result is not None:
            # Do not expose the report filename; it may encode a local path.
            summaries.append(result)
    return {
        "scope": "Sanitized QEMU Apport metadata only; no crash core or raw report retained",
        "candidate_report_count": len(_report_paths(root)),
        "qemu_report_count": len(summaries),
        "reports": summaries,
        "raw_reports_retained": False,
        "raw_cores_read": False,
    }


def wait_for_reports(root=Path("/var/crash"), timeout_seconds=120, poll_seconds=2):
    """Wait boundedly for Apport to finish writing a matching report after exit."""
    timeout_seconds = max(0, min(float(timeout_seconds), 180.0))
    poll_seconds = max(0.1, min(float(poll_seconds), 10.0))
    started = time.monotonic()
    previous = {}
    stable = {}
    last = {"scope": "Sanitized QEMU Apport metadata only; no crash core or raw report retained",
            "candidate_report_count": 0, "qemu_report_count": 0, "reports": [],
            "raw_reports_retained": False, "raw_cores_read": False}
    while True:
        paths = _report_paths(root)
        last["candidate_report_count"] = len(paths)
        for path in paths:
            try:
                info = path.stat()
                signature = (info.st_size, info.st_mtime_ns)
                stable[path] = stable.get(path, 0) + 1 if previous.get(path) == signature else 0
                previous[path] = signature
            except OSError:
                continue
        # Read each bounded text header only after its file has stopped growing;
        # avoid rescanning every potentially large report on every poll.
        stable_qemu_reports = []
        for path in paths:
            if stable.get(path, 0) < 2:
                continue
            result = parse_report(path, root)
            if result is not None:
                stable_qemu_reports.append(result)
        if stable_qemu_reports:
            last["reports"] = stable_qemu_reports
            last["qemu_report_count"] = len(stable_qemu_reports)
            break
        elapsed = time.monotonic() - started
        if elapsed >= timeout_seconds:
            break
        time.sleep(min(poll_seconds, max(0.0, timeout_seconds - elapsed)))
    last["wait_attempted"] = timeout_seconds > 0
    last["waited_seconds"] = round(min(timeout_seconds, max(0.0, time.monotonic() - started)), 2)
    last["wait_limit_seconds"] = timeout_seconds
    return last
