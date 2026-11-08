# Exo Mail â€” Project Index for AI Agents

> Android email client, Material You 3 UI. Single-module Kotlin/Jetpack Compose app.
> Last indexed: 2026-11-08 (branch `ux-overhaul`, post-rebrand). ~40 Kotlin files.

## 1. What this is

**Exo Mail** (`com.example.exoemail`) â€” a fast, battery-first email client for
Android, **minSdk = targetSdk = compileSdk = 37** (API 37 only).

- Protocols: **plain IMAP + SMTP** via Jakarta Mail (Eclipse Angus fork). **No OAuth** â€” users
  paste app passwords / æŽˆæƒç . Provider auto-detect fills server settings.
- Design: **Material You 3** â€” dynamic color from wallpaper (API 31+), stock M3 components,
  dark+light. No custom blur/animation stack.
- Sync: no IMAP IDLE. Room is source of truth; WorkManager polls IMAP every 15 min;
  bodies lazy-fetched on open.

## 2. Stack & key versions

| Area | Choice |
|---|---|
| Language | Kotlin, Compose compiler plugin |
| Build | AGP 9.4.1, KSP 2.2.20, JDK 17 toolchain |
| UI | Compose BOM 2025.10.00, **Material3** 1.4.0, **Navigation3** (`NavDisplay` + sealed `Route`) |
| Backend | Room 2.8.0, Paging 3.3.6, WorkManager 2.10.2, DataStore, security-crypto |
| Mail | `org.eclipse.angus:angus-mail` 2.0.4 + angus-activation 2.0.3 |
| ~~Glass~~ | ~~Haze 1.6.3~~ **removed** â€” no blur/backdrop |
| Tests | JUnit4 + Robolectric + MockK + Turbine + coroutines-test + room-testing |

## 3. Module / package map

```
LiquidMailApp.kt          Application: StrictMode(debug), Room, Settings, DemoSeeder(debug),
                          schedules MailSyncWorker
MainActivity.kt           enableEdgeToEdge + LiquidMailTheme + AppNavHost

data/DemoSeeder.kt        DEBUG-ONLY: seeds demo account + sample mail if DB empty.
                          Account has syncEnabled=false â†’ never hits network.

core/mail/                ProviderCatalog, AccountConfig, MailEngine interface + DTOs
core/mail/engine/         ImapSmtpMailEngine (Angus/Jakarta), MailEngineFactory
core/security/            CredentialStore (EncryptedSharedPreferences + Keystore)

data/db/                  Entities, Daos (incl. pagingUnread, searchAll, delete, setBody),
                          AppDatabase v3 (migrations 1â†’2â†’3; body column added in v3)
data/                     MailRepository (pagers, refreshInbox, saveDraft/updateDraft/deleteMessage)
data/settings/            SettingsRepository (DataStore: onboarded, active_account)

sync/MailSyncWorker.kt    CoroutineWorker; loops syncEnabled accounts, refreshInbox,
                          posts "new_mail" notification; retry() only if ALL failed

ui/theme/                 Color.kt (brand seeds + M3 fallback schemes),
                          Type.kt (expanded type scale),
                          Theme.kt (dynamicLight/DarkColorScheme on API31+, fallback static)
ui/components/Glass.kt    GlassShape radii, GlassCard (M3 Card), GlassTopBar,
                          GlassBackButton, GlassFab, GlassButton, GlassSearchBar â€”
                          thin wrappers over stock M3, names kept for call-site compat
ui/components/LiquidBackdrop.kt  ScreenBackground â€” plain Box, no animation
ui/shell/LiquidShell.kt   Scaffold: TopAppBar(avatar+search), NavigationBar(5 tabs),
                          FloatingActionButton(compose); no blur
ui/navigation/            Routes.kt (sealed Route; ComposeRoute has replyTo+draftId),
                          AppNavHost.kt (backStack, switchTab, accountInitial from active account)

ui/screens/
  OnboardingScreen.kt     provider grid, email auto-detect, password, manual hosts, Connect
  InboxScreen.kt          greeting, FilterChips(All/Unread/Starredâ†’VM state),
                          PullToRefreshBox, InboxRow list, empty/error states
  MailListScreen.kt       generic paged list â†’ Starred/Sent/Drafts screens;
                          drafts call onOpenDraft(draftId) not onOpenMessage
  FolderScreen.kt         folder message list w/ pull-to-refresh
  FoldersScreen.kt        folder list w/ unread badges
  ThreadScreen.kt         sender header, body card, attachment list, Reply/Star;
                          menu has ONLY Star (Archive/Delete removed â€” were no-ops)
  ComposeScreen.kt        To/Cc/Bcc/Subject/body; draftId â†’ "Edit draft" + hydrate;
                          replyTo â†’ prefill To; Send/Save draft
  AccountsScreen.kt       account list, tapâ†’setActiveAccount, Add account

ui/inbox/InboxViewModel.kt  resolvedAccount, reactive filter+query StateFlows,
                          messages() flow (searchAll/unread/starred/inbox pagers),
                          markRead/toggleStar â†’ Room + best-effort engine.setFlag,
                          sync() skips credential-less accounts silently
ui/thread/ThreadViewModel.kt load msg, markRead(Room), lazy fetchBody, star()
ui/compose/ComposeViewModel.kt send(â†’SMTP, real Bcc), saveDraft/updateDraft,
                          draft StateFlow for hydration
ui/onboarding/OnboardingViewModel.kt testConnection â†’ save creds+account â†’ Done
```

## 4. Data flow

```
IMAP/SMTP â”€â”€(MailSyncWorker 15min / pull-refresh / first-launch auto-sync)â”€â”€â–¶
ImapSmtpMailEngine.fetchMessages (envelopes, UID>sinceUid) â”€â”€â–¶ MailRepository.refreshInbox
â”€â”€â–¶ Room â”€â”€â–¶ PagingSource â”€â”€â–¶ Compose LazyColumn

Open thread: ThreadViewModel â†’ engine.fetchBody â†’ MIME walk â†’ text/html + attachment meta
Send: ComposeViewModel â†’ engine.send (SMTP, real Bcc) â†’ local Sent row + delete draft
Star/Read: VM â†’ Room (instant) â†’ engine.setFlag (best-effort, runCatching)
Search: query StateFlow â†’ searchAll pager (ALL folders, not just INBOX)
Credentials: CredentialStore â€” read at sync/send/flag time only
```

Key IDs: `MessageEntity.id = "{accountId}:{folder}:{uid}"`; drafts/sent use
`"{accountId}:Drafts|Sent:{timestamp}"` with `uid=0`.

## 5. Design system cheat-sheet

- Theme: `MaterialTheme.colorScheme.*` everywhere â€” no custom color locals.
- Surfaces: `Surface(shape = MaterialTheme.shapes.large, color = surfaceContainerHigh)`
  or `GlassCard` wrapper. Shapes: `GlassShape.{Surface 20dp, Field 14dp, Chip 12dp, Pill 50%}`.
- Components: `GlassButton`â†’M3 Button, `GlassSearchBar`â†’M3 TextField,
  `GlassTopBar`â†’M3 TopAppBar, `GlassFab`â†’M3 FAB, `GlassBackButton`â†’M3 IconButton.
- Nav: `NavigationBar` + `NavigationBarItem` in `LiquidShell`; tabs = Inbox/Starred/Sent/
  Drafts/Folders; `FolderRoute` highlights Folders tab.
- Filter chips: `FilterChip` bound to `vm.filter`/`vm.setFilter` (ALL/UNREAD/STARRED).

## 6. Navigation model (Navigation3)

- `backStack: SnapshotStateList<Route>` in `AppNavHost`; `backStack[0]` = tab.
- Tab routes render inside `LiquidShell`; `ThreadRoute`, `ComposeRoute`, `OnboardingRoute`,
  `AccountsRoute` render own Scaffold.
- `ComposeRoute(replyTo, draftId)` â€” `draftId` loads draft from Room for "Edit draft".
- `SettingsRoute` still dead (no entry).

## 7. Build & run

```powershell
.\gradlew.bat :app:assembleDebug        # APK â†’ app/build/outputs/apk/debug/
.\gradlew.bat :app:testDebugUnitTest    # 15 pass; AppDatabaseTest needs ~/.m2+~/.robolectric (sandbox-blocked)
.\gradlew.bat :app:assembleRelease      # minified release
```

- `local.properties` â†’ `sdk.dir` (Android SDK 37).
- Permissions: INTERNET, ACCESS_NETWORK_STATE, POST_NOTIFICATIONS, FOREGROUND_SERVICE(+DATA_SYNC).
- `mailto:` intent-filter declared, not handled.
- **Demo mode**: debug builds auto-seed `demo@liquidmail.app` + sample mail via `DemoSeeder`
  when DB is empty. Account has `syncEnabled=false` â†’ all screens reachable offline.
  Remove `DemoSeeder.seedIfEmpty` call in `LiquidMailApp` for real usage.

## 8. Tests

- `AccountConfigTest` (4), `ProviderCatalogTest` (9) â€” all pass.
- `AppDatabaseTest` (Robolectric) â€” fails in sandbox (`~/.robolectric-download-lock` denied);
  passes in normal env. Pre-existing, not a regression.
- Room schema: `app/schemas/.../AppDatabase/{1,2,3}.json` exported.

## 9. Known gaps / TODOs

1. **Archive/Delete still unimplemented** â€” Thread menu only has Star now; no IMAP MOVE/EXPUNGE.
2. **Attachments can't be downloaded/opened** â€” metadata only.
3. **Server-side drafts** â€” drafts are Room-only; no IMAP APPEND to Drafts folder.
4. **Sent folder is local-only** â€” real Sent sync needs IMAP fetch of Sent folder.
5. **Multi-account half-built** â€” only `resolvedAccount` (first/active) read; pagers take one accountId.
6. **Bodies not cached to disk** â€” `bodyPath` column exists, `body` used for drafts only.
7. **`CredentialStore.delete` has no caller** â€” can't remove accounts in UI.
8. **`SettingsRoute` dead**, `ExampleUnitTest`s still present.
9. **Proguard/R8**: `EncryptedSharedPreferences` deprecated upstream (deliberate per README).
10. **DemoSeeder runs on every debug launch** â€” seeds only if accounts table empty; safe but
    should be removed or gated for production debug builds.
11. **Search is Room LIKE** â€” case-sensitive on some SQLite builds; consider `LOWER()` wrap.
12. **No swipe actions** (swipe-to-star/delete) â€” common email UX gap.
13. **No message multi-select/bulk actions**.
14. **Thread shows single message, not thread** â€” `threadId` exists but no conversation grouping.

## 10. Conventions for editing

- New screen â†’ `Route` in `Routes.kt` + `entry<>` in `AppNavHost`; `LiquidShell` for tabs,
  bare `Scaffold`+`GlassTopBar` for pushed detail.
- New ViewModel â†’ `XViewModel(app, ...) + XViewModelFactory(app)`; use `app.database`/`app.settings`.
- Filter/query state â†’ `MutableStateFlow` in VM + `combine`/`flatMapLatest` in `messages()`;
  never pass filter/query as plain params to a cached flow.
- Flag mutations â†’ `repo.setX` (Room) then `runCatching { engine.setX }` best-effort.
- DB change â†’ bump `AppDatabase.version`, add `Migration`, keep `exportSchema=true`.
- Strings hardcoded in composables (only `app_name` is a resource).
- **Verify**: build â†’ install â†’ `uiautomator dump` + `screencap` via `adb pull` (NOT `exec-out` â€”
  that corrupts PNGs to UTF-16). Check `logcat` for FATAL.
