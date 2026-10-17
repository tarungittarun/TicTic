# Publishing TicTic to GitHub — owner-run steps

This guide imports the current v1.11.2 project into the public repository while preserving the existing `main` branch content.

## Current remote state and session limits

Remote: `https://github.com/tarungittarun/TicTic.git`  
Branch: `main`  
Visible remote history at review: one commit titled **Create 1**, adding a root-level file named `1`.

The GitHub connector is unsupported in this session. The workspace project is not a Git checkout and has no authorized write credential. **No commit or push was performed.** Do not paste tokens/passwords into chat. The repository owner must authenticate locally with GitHub CLI, Git Credential Manager, SSH, or GitHub Desktop.

## Import bundle

The file `TicTic-v1.11.2-GitHub-Import.zip` is the project-root import bundle. It includes Android source, Gradle wrapper/configuration, resources, tests and licenses, existing project documentation/QA records, `.gitignore`, and the v1.11.2 debug APK/checksum. It excludes machine-specific `local.properties`, `.gradle/`, generated build outputs and caches. The debug APK is test-only and not production signed. The ZIP's SHA-256 sidecar is supplied next to the bundle.

## Recommended: clone, unpack, review, commit, push

Install Git and unzip, then run from a terminal on a machine with GitHub access. Replace `/path/to/TicTic-v1.11.2-GitHub-Import.zip` with the actual downloaded bundle path.

```sh
git clone https://github.com/tarungittarun/TicTic.git
cd TicTic

# Inspect the pre-existing main branch content before changing it.
git status --short
git log --oneline --decorate -5
ls -la

# Unpack at repository root. This updates/adds project files but does not delete
# files that are absent from the archive; the existing file named "1" is retained.
unzip -o /path/to/TicTic-v1.11.2-GitHub-Import.zip -d .

# Review before staging. Check that no local SDK path, cache, credential, or
# unintended user data is included.
git status --short
git diff --check
git diff --stat
# For new/untracked files, inspect the file list and important docs before add.

# Authenticate in your own environment, then commit and push the intended branch.
gh auth login
gh auth status
git add -A
git status --short
git commit -m "Import TicTic Android project v1.11.2"
git push origin main

# Confirm the pushed commit.
git log -1 --oneline --decorate
gh repo view tarungittarun/TicTic
```

If the owner intentionally wants to remove the starter file `1`, inspect its contents first and explicitly remove it in the clone; do not let an import script delete it silently. If `main` changed after cloning, fetch and integrate those changes before pushing rather than force-pushing.

## GitHub Desktop option

1. Sign in to GitHub Desktop as the repository owner.
2. Clone `tarungittarun/TicTic` and select branch `main`.
3. Download/extract the import ZIP into the cloned repository root; keep the existing `1` file unless deliberately reviewed/removed.
4. Review the changed-file list and confirm `local.properties`, generated `build/` output, credentials and caches are absent.
5. Commit with a descriptive message, then choose **Push origin** and verify the commit appears on GitHub.

## Reusable publication checklist for future projects

- Confirm the correct remote, owner, default branch, and existing files before importing.
- Inspect `.gitignore`; exclude machine paths, build outputs, secrets, credentials, private data, and signing keys.
- Include the source, reproducible build instructions, project/license docs, automated tests, and a clear QA status.
- For binaries, record version, size, checksum and signing type; never describe a debug-signed APK as store-ready.
- Run project tests and `git diff --check`; review every staged path before commit.
- Authenticate through the owner's local GitHub tooling. Never share access tokens in chat.
- Push only the intended branch, then verify the remote commit and links.
- Preserve an honest boundary between build success, automated tests, runtime/device QA, and release readiness.