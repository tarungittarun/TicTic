"""Small bounded retries for transient GitHub CLI API transport failures.

Never retry authorization, not-found, validation, or application failures; callers
must still validate every run's ID, SHA, and completion state after retrieval.
"""
import json
import subprocess
import time


_TRANSIENT_MARKERS = (
    "http 408", "http 429", "http 500", "http 502", "http 503", "http 504",
    "bad gateway", "service unavailable", "gateway timeout", "unexpected eof",
    "connection reset", "connection refused", "connection closed", "tls handshake timeout",
    "i/o timeout", "timed out", "temporary failure", " eof",
)


def is_transient_failure(stderr):
    if isinstance(stderr, bytes):
        text = stderr.decode(errors="replace")
    else:
        text = str(stderr)
    text = text.lower()
    return any(marker in text for marker in _TRANSIENT_MARKERS)


def run_gh(args, *, attempts=5, timeout=60, initial_delay=2, runner=None, sleeper=None):
    """Run ``gh`` with bounded exponential backoff; return (response, attempts)."""
    if isinstance(attempts, bool) or not isinstance(attempts, int) or attempts < 1:
        raise ValueError("attempts must be a positive integer")
    if runner is None:
        runner = subprocess.run
    if sleeper is None:
        sleeper = time.sleep

    last_response = None
    command = ["gh", *(str(argument) for argument in args)]
    for attempt in range(1, attempts + 1):
        try:
            response = runner(command, capture_output=True, timeout=timeout)
        except subprocess.TimeoutExpired as error:
            if attempt == attempts:
                raise RuntimeError(f"GitHub CLI timed out after {attempt} attempts") from error
            sleeper(min(30, initial_delay * (2 ** (attempt - 1))))
            continue

        last_response = response
        if response.returncode == 0 or not is_transient_failure(response.stderr):
            return response, attempt
        if attempt < attempts:
            sleeper(min(30, initial_delay * (2 ** (attempt - 1))))

    return last_response, attempts


def gh_json(args, **kwargs):
    """Run ``gh`` and parse a successful JSON response; reject all final errors."""
    response, attempts = run_gh(args, **kwargs)
    if response.returncode:
        detail = response.stderr.decode(errors="replace")[:1200]
        raise RuntimeError(f"GitHub command failed after {attempts} attempts: {detail}")
    try:
        return json.loads(response.stdout)
    except (TypeError, ValueError) as error:
        raise RuntimeError("GitHub command returned invalid JSON") from error
