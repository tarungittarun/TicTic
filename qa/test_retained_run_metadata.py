"""Pinned source evidence fails closed and retries only transient GitHub transport errors."""
import importlib.util
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import Mock, patch


COMPARISONS_DIR = Path("mynotes/tools/comparisons").resolve()


def load_module(filename, name):
    path = COMPARISONS_DIR / filename
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    with patch.object(sys, "path", [str(COMPARISONS_DIR), *sys.path]):
        spec.loader.exec_module(module)
    return module


class GitHubApiRetryTests(unittest.TestCase):
    def test_retries_http_502_then_returns_exact_success_response(self):
        retry = load_module("github_api_retry.py", "github_api_retry_test")
        failed = subprocess.CompletedProcess(["gh"], 1, b"", b"HTTP 502: Server Error")
        succeeded = subprocess.CompletedProcess(["gh"], 0, b'{"status":"ok"}', b"")
        runner = Mock(side_effect=[failed, succeeded])
        sleeper = Mock()

        response, attempts = retry.run_gh(
            ["api", "repos/example"], attempts=3, runner=runner, sleeper=sleeper,
        )

        self.assertIs(response, succeeded)
        self.assertEqual(attempts, 2)
        self.assertEqual(runner.call_count, 2)
        sleeper.assert_called_once_with(2)
        self.assertEqual(retry.gh_json(["api", "ignored"], runner=Mock(
            return_value=succeeded), sleeper=Mock()), {"status": "ok"})

    def test_does_not_retry_authorization_or_validation_errors(self):
        retry = load_module("github_api_retry.py", "github_api_retry_forbidden_test")
        forbidden = subprocess.CompletedProcess(["gh"], 1, b"", b"HTTP 403: Resource not accessible")
        runner = Mock(return_value=forbidden)
        sleeper = Mock()

        response, attempts = retry.run_gh(["api", "private"], attempts=5,
                                          runner=runner, sleeper=sleeper)

        self.assertIs(response, forbidden)
        self.assertEqual(attempts, 1)
        runner.assert_called_once()
        sleeper.assert_not_called()

    def test_retries_a_timeout_with_a_fixed_upper_bound(self):
        retry = load_module("github_api_retry.py", "github_api_retry_timeout_test")
        succeeded = subprocess.CompletedProcess(["gh"], 0, b"{}", b"")
        runner = Mock(side_effect=[subprocess.TimeoutExpired(["gh"], 1), succeeded])
        sleeper = Mock()

        response, attempts = retry.run_gh(["api", "x"], attempts=2,
                                          runner=runner, sleeper=sleeper)

        self.assertIs(response, succeeded)
        self.assertEqual(attempts, 2)
        sleeper.assert_called_once_with(2)

    def test_completed_failed_source_is_valid_evidence_but_wrong_sha_is_not(self):
        metadata_tool = load_module("retained_run_metadata.py", "retained_run_metadata_test")
        run_id = "43101240889"
        expected = {
            "databaseId": int(run_id),
            "headSha": metadata_tool.RUN_PINS[run_id],
            "status": "completed",
            "conclusion": "failure",
            "url": "https://github.com/tarungittarun/TicTic/actions/runs/43101240889",
            "jobs": [{"databaseId": 123, "name": "diagnose"}],
        }
        artifacts = {"total_count": 1, "artifacts": [{"name": "diagnostic"}]}
        with patch.object(metadata_tool, "gh_json", side_effect=[expected, artifacts]) as fetch:
            result = metadata_tool.fetch_run(run_id)
        self.assertEqual(result["conclusion"], "failure")
        self.assertEqual(result["artifacts"], artifacts)
        self.assertEqual(fetch.call_count, 2)

        wrong_sha = dict(expected, headSha="0" * 40)
        with patch.object(metadata_tool, "gh_json", return_value=wrong_sha) as fetch:
            with self.assertRaisesRegex(ValueError, "pinned source commit"):
                metadata_tool.fetch_run(run_id)
        fetch.assert_called_once()

    def test_pending_or_unknown_runs_cannot_be_published(self):
        metadata_tool = load_module("retained_run_metadata.py", "retained_run_metadata_pending_test")
        pending = {"databaseId": 43101240889, "status": "queued"}
        with patch.object(metadata_tool, "gh_json", return_value=pending) as fetch:
            with self.assertRaisesRegex(ValueError, "not completed"):
                metadata_tool.fetch_run("43101240889")
            fetch.assert_called_once()
            fetch.reset_mock()
            with self.assertRaisesRegex(ValueError, "not an explicitly pinned"):
                metadata_tool.fetch_run("99999999999")
            fetch.assert_not_called()

    def test_retain_writes_exact_run_metadata_under_the_pinned_id(self):
        metadata_tool = load_module("retained_run_metadata.py", "retained_run_metadata_write_test")
        run_id = "43101240889"
        metadata = {
            "databaseId": int(run_id),
            "headSha": metadata_tool.RUN_PINS[run_id],
            "status": "completed",
            "conclusion": "failure",
            "url": "https://github.com/tarungittarun/TicTic/actions/runs/43101240889",
            "jobs": [{"databaseId": 123, "name": "diagnose"}],
        }
        artifacts = {"total_count": 0, "artifacts": []}
        with tempfile.TemporaryDirectory() as directory, \
                patch.object(metadata_tool, "gh_json", side_effect=[metadata, artifacts]):
            summary = metadata_tool.retain_run_metadata([run_id], directory)
            saved = (Path(directory) / f"{run_id}.json").read_text()
        self.assertEqual(summary[0]["conclusion"], "failure")
        self.assertIn(metadata["headSha"], saved)
        self.assertIn('"total_count": 0', saved)


if __name__ == "__main__":
    unittest.main()
