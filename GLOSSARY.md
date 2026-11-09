# Glossary

Domain terms used across the codebase and docs. Keep implementation details out.

- **Account** — a configured mail identity (email + provider + IMAP/SMTP endpoints + stored credential). One user may have several; exactly one is *active* at a time.
- **Active account** — the account selected in `SettingsRepository.activeAccountId`; all inbox/thread/compose operations target it. Falls back to the first account if unset.
- **Folder** — an IMAP mailbox (INBOX, Sent, Drafts, Archive, custom). Synced into Room; message lists are per-folder.
- **Message** — a single email. Locally keyed by `{accountId}:{folder}:{uid}`; `uid ≤ 0` marks local-only rows (drafts use negative uids, sent copies use 0).
- **Draft** — a `MessageEntity` with `isDraft=true` in the `Drafts` folder. Body stored inline in `body` column; resumable via Compose.
- **Envelope** — IMAP metadata (headers, flags, snippet) fetched without the body. Sync uses envelopes only; bodies are lazy-fetched on open.
- **Sync** — the WorkManager-driven IMAP poll (`MailSyncWorker`, 15 min) + manual pull-to-refresh. Writes envelopes/folders into Room.
- **Flag** — IMAP message state (`\Seen` = read, `\Flagged` = starred). UI toggles write Room first, then best-effort `engine.setFlag` to the server.
- **Exo Mail** — the app name and package (`com.example.exoemail`).
- **Demo account** — debug-only seeded account (`demo@liquidmail.app`, `syncEnabled=false`) inserted by `DemoSeeder` so all screens are reachable without credentials.
- **Conversation** — the set of messages sharing a `threadId` derived from the IMAP `References` header chain (first Message-ID = thread root). `refreshInbox` extracts the root during sync so replies group immediately; `fetchBody` also populates `MailBody.references` for on-open refinement.
- **Attachment (file)** — a non-inline MIME part with a filename. Distinct from *inline image* (cid-referenced, rendered in body). Files can be downloaded, opened, saved, and attached on send.
- **Organize action** — a server-side message move: Archive (→ Archive folder), Delete (→ Trash then expunge), Move (→ chosen folder), Spam. Local Room write first, best-effort IMAP COPY+STORE+EXPUNGE/MOVE.
- **Snooze** — hide a message until a chosen time. Local-only (`snoozedUntil` timestamp); no server move. Reappears in inbox after the time passes.
- **Reply / Forward** — Reply sets `In-Reply-To`+`References` headers, `Re:` subject, quoted body; Forward sets `Fwd:` subject, original body + attachments.
