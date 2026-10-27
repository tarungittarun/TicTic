# ADR-001 — additive Notes runtime and recoverable folder library

Status: accepted for M0 implementation; Android adapter qualification pending.
Date: 2026-10-27

## Context
The existing :feature-notes uses shared MySpace Room/prefs, synchronous bulk replacement
and a 500-note cap. Replacing it in place would change MySpace before standalone acceptance.

## Decision
1. Add :notes-domain (pure Java), :notes-runtime (Android library), and :notes
   (standalone host) in the next slice. Only :notes consumes the new runtime initially.
   The existing :feature-notes remains untouched. The new runtime imports only its domain
   and :core-* contracts; it never imports :app, :shell or another feature. Modules are
   not registered until their buildable skeleton is ready.
2. Domain owns structured documents, seven-day trash rules, operations and serialization
   versioning. Runtime owns repository, SAF provider adapter, jobs and reusable UI.
   Host supplies permissions, picker callbacks, system surfaces and theme. The future
   MySpace adapter consumes this same runtime, rather than copying it.
3. Library authority is immutable, checksummed folder generations; an app-private index
   is rebuildable. Each commit contains the complete current document map and previous
   generation identity. Large attachments become immutable referenced blobs in the Android
   implementation; this prototype deliberately tests only document commits.
4. Write a unique candidate file, close, then reopen and validate before acknowledging.
   Never overwrite a prior generation or require provider rename/atomic replace.
   Recovery ignores incomplete/corrupt candidates and validates ancestry, not just filename
   ordering. UUID identities prevent overwrite; divergent branches are conflicts, not a
   reason to silently choose one. Missing/corrupt parent is an integrity error.
5. This is single-writer storage. A stale writer is rejected by head comparison. This
   preflight alone is NOT a cross-process lock: two simultaneous writers can fork.
   Fork detection must block writes and surface reconciliation. Until a tested host/provider
   locking strategy exists, two apps must not actively write one tree simultaneously.
6. The Python M0 probe is an executable protocol reference, NOT production persistence.
   Its filesystem flush is stronger than many SAF providers. Passing these tests does not
   prove Android provider durability, durable media saves, or power-loss safety.

## Consequences / gates
Android adapter must test closed-file readback, process death, SD removal, revoked grant,
provider partial writes, full disk, folder move and index rebuild. Never display Saved on
failure. App-private recovery journal is disclosed and bounded. Cross-app import requires
receiving-host folder grants and duplicate-ID reconciliation. No production storage is
released until these outcomes pass on supported local providers.

Next ADRs: structured schema/migrations; offline model licensing/pinning; reminder scheduling.
