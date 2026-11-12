"""Fetch pinned completed-run metadata without mistaking transient API errors for evidence."""
import argparse
import json
from pathlib import Path

from github_api_retry import gh_json


RUN_PINS = {
    "42617793054": "e88f4be2343147c8f19f9f4dfc3b769bf76ecf05",
    "43083262273": "e38403dd03e29799cfd8022adb9e5e460f755264",
    "43086479017": "ef14395d8e75e9f482b0bfb71a061b75ac5eb8f2",
    "43099110149": "8d86e50aff85bad57ccae9da04516fb7dc87ce2e",
    "43101240889": "496fce9d6416782b4332924e23c91bd2cd3edc85",
    "43130164650": "b475dfc6b3764cacf1f00dc2f4b616405950c29a",
    "43132245933": "5e7cdf6fcddeb9d9ed9d4056eba0fb6f5c862618",
}
REPOSITORY = "tarungittarun/TicTic"


def fetch_run(run_id):
    run_id = str(run_id)
    if run_id not in RUN_PINS:
        raise ValueError(f"Run {run_id} is not an explicitly pinned comparison source")
    metadata = gh_json([
        "run", "view", run_id, "--json",
        "databaseId,headSha,status,conclusion,url,jobs",
    ])
    if metadata.get("databaseId") != int(run_id):
        raise ValueError(f"GitHub returned an unexpected database ID for run {run_id}")
    if metadata.get("status") != "completed":
        raise ValueError(f"Run {run_id} is not completed; pending evidence cannot be finalized")
    if metadata.get("headSha") != RUN_PINS[run_id]:
        raise ValueError(f"Run {run_id} does not match its pinned source commit")
    if not isinstance(metadata.get("jobs"), list) or not metadata["jobs"]:
        raise ValueError(f"Run {run_id} has no retrievable completed job records")

    artifacts = gh_json([
        "api", f"repos/{REPOSITORY}/actions/runs/{run_id}/artifacts",
    ])
    if not isinstance(artifacts.get("artifacts"), list):
        raise ValueError(f"Run {run_id} returned an invalid artifact inventory")
    metadata["artifacts"] = artifacts
    return metadata


def retain_run_metadata(run_ids, output_dir):
    output = Path(output_dir)
    output.mkdir(parents=True, exist_ok=True)
    results = []
    for run_id in run_ids:
        metadata = fetch_run(run_id)
        (output / f"{run_id}.json").write_text(json.dumps(metadata, indent=2) + "\n")
        results.append({"run": str(run_id), "status": metadata["status"],
                        "conclusion": metadata["conclusion"],
                        "jobs": len(metadata["jobs"]),
                        "artifacts": metadata["artifacts"].get("total_count")})
    return results


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("runs", nargs="+", choices=sorted(RUN_PINS))
    args = parser.parse_args()
    print(json.dumps(retain_run_metadata(args.runs, args.output_dir), indent=2))


if __name__ == "__main__":
    main()
