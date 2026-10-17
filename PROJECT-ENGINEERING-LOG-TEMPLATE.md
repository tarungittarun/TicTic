# Project engineering log — template

Copy this file into each project as `docs/ENGINEERING-LOG.md`. Keep operational details and decisions here; do not store secrets, tokens, private keys, user data, or credential files.

## 1. Project snapshot

- Project / product:
- Repository URL:
- Visibility / owner:
- Default branch / working branch:
- Current release / commit:
- Status (prototype, active, QA, released, blocked):
- Maintainer / approver:
- Last updated:

## 2. User goals and acceptance criteria

- User request, in plain language:
- Observable acceptance criteria:
- Non-goals / constraints:
- Accessibility, privacy, security, offline, licensing, or platform requirements:

## 3. Stack and environment

| Layer | Technology / version | Notes |
|---|---|---|
| Language/runtime | | |
| Framework/UI | | |
| Build system/toolchain | | |
| Storage/data model | | |
| Services/APIs | | |
| Tests/CI | | |
| Deployment/hosting | | |

- Supported operating systems / API levels:
- Required environment variables (names only; never values):
- Local setup commands:
- Build commands:
- Test commands:
- Known machine/environment assumptions:

## 4. Architecture and data flow

- Major components and responsibilities:
- User-visible flows:
- Persistence and migration approach:
- Network boundaries / offline behavior:
- Permissions and why they are needed:
- External dependencies and licenses:

## 5. Decisions and alternatives

| Date | Decision | Alternatives considered | Reason / trade-off | Follow-up |
|---|---|---|---|---|
| | | | | |

## 6. Change ledger — append one entry per change/commit

### Change: `<actual commit hash>` — `<commit subject>`

- Date / author / branch:
- User request / issue:
- Intended behavior:
- Files and components changed:
- Approach and implementation detail:
- Data/schema/API compatibility:
- Risks and regression areas:
- Tests run (exact commands):
- Results (pass/fail/blocked; counts):
- Mistakes or failed approaches:
- Root cause and correction:
- Manual QA performed / not performed:
- Known gaps / follow-up:
- Commit status (local only / pushed):
- PR/review URL:

## 7. Test and QA matrix

| ID | Scenario | Level (unit/integration/UI/device) | Environment | Expected | Result | Evidence / artifact | Owner |
|---|---|---|---|---|---|---|---|
| | | | | | NOT RUN | | |

Always distinguish tests that were run from proposed manual tests. Record device/API/OEM, permissions, setup, and flakiness where applicable.

## 8. Bugs, incidents, mistakes, and fixes

| Date | Symptom | Reproduction | Root cause | Fix | Regression test | Status |
|---|---|---|---|---|---|---|
| | | | | | | |

## 9. Security, privacy, and safety notes

- Sensitive data handled:
- Storage, encryption, retention, deletion:
- Permission rationale and user controls:
- Threats / abuse cases:
- Secrets handling: record secret names/locations, never values:
- Dependency/license risks:
- Security checks and results:

## 10. Release and artifact record

| Version | Artifact | Size | SHA-256 | Signing/provenance | Build/test result | Published location |
|---|---|---:|---|---|---|---|
| | | | | | | |

- Changelog location:
- Rollback procedure:
- Release approver / date:
- Store/deployment status:

## 11. Repository, review, and push workflow

- Branch strategy:
- Commit subject convention:
- Required reviews/status checks:
- Push/PR status (do not claim unless confirmed):
- External access constraints:
- Repo URL and current remote refs verified on:
- If push cannot be performed, exact blocker and owner action:

## 12. Open issues and next actions

| Priority | Item | Owner | Dependency/blocker | Due/target | Status |
|---|---|---|---|---|---|
| | | | | | |

## 13. Handoff checklist

- [ ] Current source and docs are in the repository.
- [ ] Commit ledger matches actual commit history.
- [ ] Build/test commands are reproducible.
- [ ] Artifacts and checksums verified.
- [ ] Manual QA gaps are explicit.
- [ ] Secrets are not in source, logs, zips, or chat.
- [ ] Deployment/rollback steps are documented.
- [ ] Next maintainer knows current blockers and decisions.
