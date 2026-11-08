# Liquid Mail

Sleek Android email client targeting **API 37** with an OPPO/ColorOS-style **liquid glass** theme (frosted translucent cards, gradient accents, animated drifting backdrop), built performance- and battery-first.

## Providers

Plain **IMAP + SMTP** — no OAuth in v1. Onboarding auto-detects the provider from your email domain and prefills servers:

| Provider | IMAP | SMTP | Password type |
|---|---|---|---|
| Gmail | imap.gmail.com:993 SSL | smtp.gmail.com:465 SSL | Google **app password** (needs 2-Step Verification) |
| QQ Mail / Foxmail | imap.qq.com:993 SSL | smtp.qq.com:465 SSL | **授权码** (authorization code) |
| Outlook / Office365 | outlook.office365.com:993 SSL | smtp.office365.com:587 STARTTLS | password / app password |
| iCloud | imap.mail.me.com:993 SSL | smtp.mail.me.com:587 STARTTLS | Apple app-specific password |
| Yahoo | imap.mail.yahoo.com:993 SSL | smtp.mail.yahoo.com:465 SSL | app password |
| 163 / 126 / yeah.net | imap.163.com:993 SSL | smtp.163.com:465 SSL | 授权码 |
| Other | manual | manual | password |

## Architecture

```
ui/           Compose + Navigation3 screens, liquid-glass design system
  theme/        palette, typography, dark-first colour scheme
  components/   Modifier.glass(), GlassCard/TopBar/Fab/SearchBar, LiquidBackdrop
  screens/      Onboarding, Inbox, Thread, Compose, Accounts
  */ViewModel   per-screen VMs
core/
  mail/         ProviderCatalog, AccountConfig, MailEngine interface
  mail/engine/  ImapSmtpMailEngine (Eclipse Angus / Jakarta Mail 2.x)
  security/     CredentialStore (EncryptedSharedPreferences + Keystore)
data/
  db/           Room: accounts / folders / messages / attachments (+ PagingSource)
  settings/     DataStore: onboarded flag, active account
sync/           MailSyncWorker (WorkManager, 15-min periodic, network-constrained)
```

## Performance & battery

- Paged inbox: Room `PagingSource` -> `LazyColumn` (pageSize 40, prefetch 15)
- `MailSyncWorker`: 15-min periodic WorkManager job, `NetworkType.CONNECTED`, exponential backoff — **no IMAP IDLE push** (wakelock-heavy)
- IMAP envelope-only `FETCH` for sync; body fetched lazily and cached to `cacheDir/mail/`
- All mail I/O on `Dispatchers.IO`; flows collected via `collectAsStateWithLifecycle`-style state
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

Requirements: JDK 17+ (toolchain 25 auto-provisioned via foojay), Android SDK 37 (`local.properties` -> `sdk.dir`).

## Notes

- `EncryptedSharedPreferences`/`MasterKey` are deprecated upstream but still the only shipped Keystore-backed prefs API; used deliberately (see `CredentialStore.kt` warnings).
- Gmail without OAuth means users must generate an app password; onboarding links straight to the provider help page.
