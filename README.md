# Exo Mail

A fast, Material You 3 email client for Android (API 37), built performance- and battery-first. Real HTML email rendering, plain IMAP + SMTP â€” no OAuth in v1.

## Features

- **Material You 3** dynamic color theming (Android 12+ wallpaper-based schemes, static fallbacks below)
- **Liquid-glass dock** navigation with real-time Haze blur rendering
- **Full HTML email rendering** via WebView â€” remote images, `cid:` inline images, dark-mode-aware styling
- **Attachment extraction** â€” MIME parts surfaced as download chips
- **Real IMAP + SMTP** via Eclipse Angus (Jakarta Mail 2.x); envelope-only sync keeps inbox loads fast
- **Gmail / Outlook / iCloud / Yahoo / QQ / 163 / generic IMAP** provider presets
- Paged inbox (Room `PagingSource` â†’ `LazyColumn`), pull-to-refresh, search, star/read state synced to server flags

## Providers

Plain **IMAP + SMTP** â€” no OAuth in v1. Onboarding auto-detects the provider from your email domain and prefills servers:

| Provider | IMAP | SMTP | Password type |
|---|---|---|---|
| Gmail | imap.gmail.com:993 SSL | smtp.gmail.com:465 SSL | Google **app password** (needs 2-Step Verification) |
| QQ Mail / Foxmail | imap.qq.com:993 SSL | smtp.qq.com:465 SSL | **æŽˆæƒç ** (authorization code) |
| Outlook / Office365 | outlook.office365.com:993 SSL | smtp.office365.com:587 STARTTLS | password / app password |
| iCloud | imap.mail.me.com:993 SSL | smtp.mail.me.com:587 STARTTLS | Apple app-specific password |
| Yahoo | imap.mail.yahoo.com:993 SSL | smtp.mail.yahoo.com:465 SSL | app password |
| 163 / 126 / yeah.net | imap.163.com:993 SSL | smtp.163.com:465 SSL | æŽˆæƒç  |
| Other | manual | manual | password |

## Architecture

```
ui/           Compose + Navigation3 screens, Material You theme
  theme/        dynamic color schemes, shapes, typography
  shell/        LiquidShell â€” floating glass dock + Haze blur
  screens/      Onboarding, Inbox, Thread, Compose, Folders, Settings
  */ViewModel   per-screen VMs (thread VM cancels stale fetches on message switch)
core/
  mail/         ProviderCatalog, AccountConfig, MailEngine interface, MailBody/InlineImage models
  mail/engine/  ImapSmtpMailEngine (Eclipse Angus) â€” envelope FetchProfile sync, cid: rewriting
  security/     CredentialStore (EncryptedSharedPreferences + Keystore)
data/
  db/           Room v3: accounts / folders / messages / attachments (+ PagingSource)
  settings/     DataStore: onboarded flag, active account
sync/           MailSyncWorker (WorkManager, 15-min periodic, network-constrained)
```

## Performance & battery

- Paged inbox: Room `PagingSource` â†’ `LazyColumn` (pageSize 40, prefetch 15)
- `MailSyncWorker`: 15-min periodic WorkManager job, `NetworkType.CONNECTED`, exponential backoff â€” **no IMAP IDLE push** (wakelock-heavy)
- IMAP **envelope-only** `FETCH` for list sync (`FetchProfile` prefetch); body fetched lazily on open and cached in Room
- Attachment detection via `Content-Type` headers â€” never touches part bodies during sync
- All mail I/O on `Dispatchers.IO`; flows collected with lifecycle-aware state
- StrictMode (thread + VM) in debug builds
- `baseline-prof.txt` + `profileinstaller` for AOT/JIT warmup of startup + inbox path
- Release: R8 full mode + resource shrinking (`android.enableR8.fullMode=true`)

## Build

```sh
./gradlew.bat :app:assembleDebug        # debug APK -> app/build/outputs/apk/debug/
./gradlew.bat :app:testDebugUnitTest    # unit tests (JUnit4 + Robolectric)
./gradlew.bat :app:assembleRelease      # minified + shrunk release APK
./gradlew.bat :app:lintDebug            # lint
```

Requirements: JDK 17+ (toolchain auto-provisioned via foojay), Android SDK 37 (`local.properties` â†’ `sdk.dir`).

## Notes

- `EncryptedSharedPreferences`/`MasterKey` are deprecated upstream but still the only shipped Keystore-backed prefs API; used deliberately (see `CredentialStore.kt` warnings).
- Gmail without OAuth means users must generate an app password; onboarding links straight to the provider help page.
- Accounts are deduplicated by email on add and merged on startup â€” prevents the split-credential bug where half the inbox pointed at a credential-less account id.
