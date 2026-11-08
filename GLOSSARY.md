# Glossary

Domain terms used across the codebase and docs. Keep implementation details out.

- **Account** â€” a configured mail identity (email + provider + IMAP/SMTP endpoints + stored credential). One user may have several; exactly one is *active* at a time.
- **Active account** â€” the account selected in `SettingsRepository.activeAccountId`; all inbox/thread/compose operations target it. Falls back to the first account if unset.
- **Folder** â€” an IMAP mailbox (INBOX, Sent, Drafts, Archive, custom). Synced into Room; message lists are per-folder.
- **Message** â€” a single email. Locally keyed by `{accountId}:{folder}:{uid}`; `uid=0` marks local-only rows (drafts, sent copies).
- **Draft** â€” a `MessageEntity` with `isDraft=true` in the `Drafts` folder. Body stored inline in `body` column; resumable via Compose.
- **Envelope** â€” IMAP metadata (headers, flags, snippet) fetched without the body. Sync uses envelopes only; bodies are lazy-fetched on open.
- **Sync** â€” the WorkManager-driven IMAP poll (`MailSyncWorker`, 15 min) + manual pull-to-refresh. Writes envelopes/folders into Room.
- **Flag** â€” IMAP message state (`\Seen` = read, `\Flagged` = starred). UI toggles write Room first, then best-effort `engine.setFlag` to the server.
- **Exo Mail** â€” the app name and package (`com.example.exoemail`).
- **Demo account** â€” debug-only seeded account (`demo@liquidmail.app`, `syncEnabled=false`) inserted by `DemoSeeder` so all screens are reachable without credentials.
