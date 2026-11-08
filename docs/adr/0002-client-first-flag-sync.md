# ADR-0002: Client-first flag sync, server sync deferred

## Status
Accepted — 2026-11-08

## Context
Star/read toggles previously wrote only to Room; stars vanished on next IMAP sync and the
server never learned the state. `MailEngine.setFlag`/`markRead` existed but were unused.

## Decision
UI actions now do **optimistic local write → best-effort server call**:
- `InboxViewModel.markRead`/`toggleStar` update Room immediately, then call
  `engine.markRead`/`engine.setStarred` inside `runCatching` (failures are silent — the
  next sync reconciles).
- `uid=0` rows (drafts, sent copies) skip the server call entirely.
- Archive/Delete remain unimplemented (no IMAP MOVE/EXPUNGE yet).

## Consequences
- Star/read now persist across syncs *and* propagate to other clients (best-effort).
- No UI blocking on network; sync worker remains the authoritative reconciler.
- Server writes are fire-and-forget — a failed `setFlag` won't retry until next manual sync.

## Alternatives considered
- Full server-sync with retry queue (rejected: triples IMAP write-path complexity for v1).
- Server-only writes (rejected: too slow, breaks offline UX).
