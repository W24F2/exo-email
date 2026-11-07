# Plan: "Liquid Mail" — API 37 Email Client with Liquid Glass UI

## Goal

Build a working Android email client (`com.example.email_client`, label **Liquid Mail**) targeting Android API 37 (Android 17), Jetpack Compose UI with a custom OPPO/ColorOS-style "liquid glass" theme, supporting Gmail, QQ Mail, Outlook and generic IMAP/SMTP providers, built performance- and battery-first (WorkManager sync, Room paging, Baseline Profiles, StrictMode, R8).

> **SCOPE DECISION (user, 2026-11-08): IMAP/SMTP only.** No OAuth, no AppAuth. Gmail and all providers authenticate with password / provider app-password (Gmail app password, QQ 授权码, etc.). All `AuthType` values are PASSWORD or APP_PASSWORD — the enum stays for provider metadata (help text/links) only.

## Current context / assumptions

- Root: `C:\Users\24F2\AndroidStudioProjects\Emailclient`. **Not a git repo** — plan starts with `git init`.
- Existing files: `build.gradle.kts`, `settings.gradle.kts` (foojay resolver 1.0.0), `gradle/libs.versions.toml`, `app/build.gradle.kts`, `AndroidManifest.xml`, template `res/values/*`, default mipmaps, `keepRules/rules.keep`, template tests.
- Toolchain already configured: **AGP 9.4.1** (`com.android.application`), **Gradle 9.6.0** wrapper, `compileSdk = release(37)`, `minSdk = 37`, `targetSdk = 37`. Java toolchain 25 via `gradle/gradle-daemon-jvm.properties` (fooled auto-provisioning; JDK 17 is on PATH as fallback — if daemon JVM 25 fails to provision, run with `-Dorg.gradle.java.installations.auto-detect=true` or edit `toolchainVersion` to 17).
- Android SDK at `A:\Andriod_SDK` (from `local.properties`; SDK 37 platform assumed installed — verify with `ls "A:/Andriod_SDK/platforms"`; if `android-37` is missing, run `"A:/Andriod_SDK/cmdline-tools/latest/bin/sdkmanager" "platforms;android-37"` or install via Android Studio).
- **AGP 9 has built-in Kotlin** — do NOT add `org.jetbrains.kotlin.android` or `kotlin-kapt`. Add `kotlinCompiler` block only if needed; KSP is the supported annotation processor.
- The template `Theme.Emailclient` is `Theme.MaterialComponents.DayNight.DarkActionBar` — will be replaced by a Compose `ComponentActivity` theme.
- Package name: `com.example.email_client` (existing namespace — keep it; do not rename).
- No emulator/device assumed: validation = `assembleDebug`, unit tests, and lint. Instrumented/UI tests are written but marked optional (require `connectedDebugAndroidTest` on an API 37 emulator).

## Architecture / proposed approach

Single `:app` module, single-activity Compose app, unidirectional data flow:

- **UI**: Compose BOM 2025.10+, Material 3 Expressive, Navigation3 (`androidx.navigation3`), custom `Glass` components — `Modifier.glass()` (blur + translucent gradient + 1dp gradient border + inner specular highlight; Android 13+ `RenderEffect`, layered-render-fallback on < Tiramisu is irrelevant since minSdk 37 — always real blur). Color scheme: dark-first "liquid" palette (deep space blue/charcoal base, iridescent cyan→violet accents, frosted surfaces ~ alpha 0.55–0.72), 28dp+ corner radii, edge-to-edge.
- **Data**: Room (`email_client.db`) — `accounts`, `folders`, `messages`, `attachments`; `PagingSource` → `LazyColumn` inbox (paged, battery-friendly). DataStore Preferences for settings.
- **Mail engine**: `core/mail/` — `MailEngine` interface, `MailEngineFactory`, `ImapSmtpMailEngine` backed by **Eclipse Angus/Jakarta Mail 2.x** (`org.eclipse.angus:angus-mail`) over IMAP IDLE-capable fetch (chunked `FETCH ENVELOPE`) + SMTP send. Provider presets table (`EmailProvider.kt`) covers Gmail, QQ Mail (imap.qq.com:993 / smtp.qq.com:465 — requires QQ "授权码" app password, documented in-app), Outlook/Office365, iCloud, Yahoo, 163/NetEase, plus full manual IMAP/SMTP entry. All providers use password / provider app-password auth (Gmail needs a Google app password — linked from onboarding).
- **Sync**: `MailSyncWorker` (WorkManager, periodic 15min, network-constrained, expedited initial sync) + IMAP as source of truth → Room. Credentials encrypted via `androidx.security:security-crypto` `EncryptedSharedPreferences` + Keystore.
- **Perf/battery**: Baseline Profile (`:baselineprofile` module or `baseline-prof.txt` + `profileinstaller`), StrictMode in debug, lifecycle-aware collectors (`collectAsStateWithLifecycle`), `Dispatchers.IO` for all mail I/O, `remember`/`derivedStateOf` discipline, LazyLayout prefetch off main thread, R8 full mode + resource shrinking in release.

## Step-by-step tasks

> Commit convention: `git add -A && git commit -m "<type>: <desc>"` after each green task. Use `git-bash` paths: `cd "/c/Users/24F2/AndroidStudioProjects/Emailclient"`.

### Task 0 — Repo init & sanity build
1. `cd "/c/Users/24F2/AndroidStudioProjects/Emailclient" && git init && git add -A && git commit -m "chore: import android studio template"`
2. Verify SDK 37 platform exists: `ls "A:/Andriod_SDK/platforms" | grep 37` → expect `android-37`. If absent, install it first (see assumptions).
3. `./gradlew.bat :app:assembleDebug` → expect `BUILD SUCCESSFUL`, APK at `app/build/outputs/apk/debug/app-debug.apk`.

### Task 1 — Version catalog (`gradle/libs.versions.toml`)
Rewrite the file wholesale:

```toml
[versions]
agp = "9.4.1"
kotlin = "2.2.20"               # bundled with AGP 9 built-in kotlin
composeBom = "2025.10.00"
activityCompose = "1.11.0"
lifecycle = "2.9.4"
navigation3 = "1.0.0"
room = "2.8.0"
ksp = "2.2.20-2.0.3"
work = "2.10.2"
datastore = "1.1.7"
securityCrypto = "1.1.0-alpha07"
angusMail = "2.0.4"
paging = "3.3.6"
profileinstaller = "1.4.1"
baselineprofile = "1.4.1"
material3Expressive = "1.5.0-alpha07"   # bump to stable if released
coreKtx = "1.17.0"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
robolectric = "4.15.1"
mockk = "1.13.16"
coroutinesTest = "1.10.2"
turbine = "1.2.1"
appcompat = "1.7.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3", version.ref = "material3Expressive" }
compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-navigation3-runtime = { group = "androidx.navigation3", name = "navigation3-runtime", version.ref = "navigation3" }
androidx-navigation3-ui = { group = "androidx.navigation3", name = "navigation3-ui", version.ref = "navigation3" }
androidx-paging-runtime = { group = "androidx.paging", name = "paging-runtime", version.ref = "paging" }
androidx-paging-compose = { group = "androidx.paging", name = "paging-compose", version.ref = "paging" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-paging = { group = "androidx.room", name = "room-paging", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }
angus-mail = { group = "org.eclipse.angus", name = "angus-mail", version.ref = "angusMail" }
angus-activation = { group = "org.eclipse.angus", name = "angus-activation", version.ref = "angusMail" }
androidx-profileinstaller = { group = "androidx.profileinstaller", name = "profileinstaller", version.ref = "profileinstaller" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

**Verify:** `./gradlew.bat :app:dependencies --configuration debugRuntimeClasspath | findstr "compose-bom angus-mail room-runtime"` → all three resolve (no "FAILED").

### Task 2 — `app/build.gradle.kts`: Compose + KSP + perf flags
Replace content with:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.email_client"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.email_client"
        minSdk = 37
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        resources {
            excludes += setOf("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/DEPENDENCIES")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.angus.mail)
    implementation(libs.angus.activation)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.core.ktx)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

> Note: if `kotlin { compilerOptions {} }` block fails on this AGP build, delete the block — AGP 9 defaults already target JVM 17+ when `compileOptions` is set.

Create empty `app/proguard-rules.pro` with Jakarta Mail rules:
```
-keep class jakarta.mail.** { *; }
-keep class com.sun.mail.** { *; }
-keep class org.eclipse.angus.** { *; }
-dontwarn jakarta.activation.**
-dontwarn com.sun.mail.**
-dontwarn jakarta.mail.**
```

**Verify:** `./gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL. Commit: `feat: compose + mail deps, KSP/Room, release minification`.

### Task 3 — Manifest + app entry
`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

    <application
        android:name=".LiquidMailApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.LiquidMail">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|uiMode"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="mailto" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`res/values/themes.xml` — replace with:
```xml
<resources>
    <style name="Theme.LiquidMail" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@android:color/black</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar">false</item>
    </style>
</resources>
```
Delete `values-night/themes.xml` (Compose handles dark/light). `strings.xml`: app_name → `Liquid Mail`.

New files:
- `app/src/main/java/com/example/email_client/LiquidMailApp.kt` — `Application` subclass: `StrictMode` policy in `BuildConfig.DEBUG` (thread: detectAll + penaltyLog; vm: detectLeakedClosableObjects, detectActivityLeaks, penaltyLog), schedule `MailSyncWorker` via `WorkManager` `ExistingPeriodicWorkPolicy.KEEP`.
- `app/src/main/java/com/example/email_client/MainActivity.kt` — `ComponentActivity`, `enableEdgeToEdge()`, `setContent { LiquidMailTheme { AppNavHost() } }`.

**Verify:** `./gradlew.bat :app:assembleDebug` → SUCCESSFUL. Commit `feat: manifest, application class, edge-to-edge activity`.

### Task 4 — Liquid Glass theme (`ui/theme/`)
Package `com.example.email_client.ui.theme`. Files:

- `Color.kt` — liquid palette:
```kotlin
package com.example.email_client.ui.theme
import androidx.compose.ui.graphics.Color
val SpaceBase = Color(0xFF070A12)          // deep-space backdrop
val SpaceElevated = Color(0xFF0E1420)
val GlassSurface = Color(0x99FFFFFF)        // frosted fill (applied with alpha)
val GlassStroke = Color(0x33FFFFFF)
val LiquidCyan = Color(0xFF5CE1E6)
val LiquidViolet = Color(0xFF9B7BFF)
val LiquidPink = Color(0xFFFF7AD9)
val TextPrimary = Color(0xFFF2F5FA)
val TextSecondary = Color(0xB3C6D2E4)
val OnAccent = Color(0xFF031018)
```
- `Type.kt` — `Typography` with `FontFamily.Default`, tighter letterSpacing (`-0.2.sp` titles), titleLarge 22.sp/bodyMedium 15.sp.
- `Theme.kt` — `darkColorScheme(primary = LiquidCyan, secondary = LiquidViolet, tertiary = LiquidPink, background = SpaceBase, surface = SpaceElevated, onBackground = TextPrimary, onSurface = TextPrimary)`; `LiquidMailTheme(content)` always dark-first (liquid glass reads dark); `LocalGlassTint` CompositionLocal exposing accent gradient `Brush.linearGradient(listOf(LiquidCyan, LiquidViolet, LiquidPink))`.
- `Glass.kt` (`ui/components/Glass.kt`) — the signature modifier + components:
```kotlin
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(28.dp),
    blurRadius: Dp = 24.dp,
    tintAlpha: Float = 0.14f,
): Modifier = this
    .graphicsLayer {
        renderEffect = BlurEffect(blurRadius.toPx(), blurRadius.toPx(), TileMode.DECAL) // API 31+
        clip = true
        this.shape = shape
    }
    .background(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = tintAlpha + 0.08f), Color.White.copy(alpha = tintAlpha))
        ),
        shape
    )
    .border(
        1.dp,
        Brush.verticalGradient(listOf(GlassStroke.copy(alpha = 0.55f), GlassStroke.copy(alpha = 0.12f))),
        shape
    )
```
Plus `GlassCard`, `GlassTopBar` (translucent `CenterAlignedTopAppBar` with `Color.Transparent` container), `GlassSearchBar`, `GlassFab` (gradient-filled `FloatingActionButton`, specular top highlight), `GlassListItem` (mail row: sender avatar circle with gradient ring, subject bold, preview 1 line, time). Put shared preview `@Composable LiquidMailTheme` wrappers in each file.
- Backdrop: `ui/components/LiquidBackdrop.kt` — `Box` filling screen with animated `Brush.radialGradient` blobs (two `rememberInfiniteTransition`-driven offsets, slow 14s/19s cycles — cheap: drawn once per frame as gradients, no particles), content `Column` on top.

**Verify:** `./gradlew.bat :app:compileDebugKotlin` → SUCCESSFUL; add one `@Preview` per component. Commit `feat: liquid glass design system`.

### Task 5 — Navigation skeleton + placeholder screens
`ui/navigation/AppNavHost.kt` using Navigation3 `NavDisplay` with `backStack = rememberNavBackStack(InboxRoute)`; serializable route objects in `ui/navigation/Routes.kt`: `@Serializable object OnboardingRoute`, `@Serializable object InboxRoute`, `@Serializable data class ThreadRoute(val threadId: String)`, `@Serializable data class ComposeRoute(val replyTo: String? = null)`, `@Serializable object AccountsRoute`, `@Serializable object SettingsRoute`.
Placeholder screens (each just `GlassCard` + title text) in `ui/screens/`: `OnboardingScreen`, `InboxScreen`, `ThreadScreen`, `ComposeScreen`, `AccountsScreen`, `SettingsScreen`. First-launch gate: `SettingsRepository.isOnboarded` (stub returns false) → show Onboarding.
**Verify:** `assembleDebug` green. Commit `feat: navigation shell + screen stubs`.

### Task 6 — Provider catalog (`core/mail/EmailProvider.kt`)
```kotlin
enum class AuthType { PASSWORD, APP_PASSWORD }
data class ServerSpec(val host: String, val port: Int, val ssl: Boolean)
data class EmailProvider(
    val id: String, val displayName: String, val domainHints: List<String>,
    val imap: ServerSpec, val smtp: ServerSpec, val auth: AuthType, val helpUrl: String?,
)
object ProviderCatalog {
    val all = listOf(
        EmailProvider("gmail", "Gmail", listOf("gmail.com","googlemail.com"),
            ServerSpec("imap.gmail.com",993,true), ServerSpec("smtp.gmail.com",465,true),
            AuthType.APP_PASSWORD, "https://support.google.com/accounts/answer/185833"),
        EmailProvider("qq", "QQ Mail", listOf("qq.com","foxmail.com"),
            ServerSpec("imap.qq.com",993,true), ServerSpec("smtp.qq.com",465,true),
            AuthType.APP_PASSWORD, "https://service.mail.qq.com/detail/0/75"),
        EmailProvider("outlook", "Outlook", listOf("outlook.com","hotmail.com","live.com","office365.com"),
            ServerSpec("outlook.office365.com",993,true), ServerSpec("smtp.office365.com",587,false),
            AuthType.PASSWORD, null),
        EmailProvider("icloud", "iCloud", listOf("icloud.com","me.com","mac.com"),
            ServerSpec("imap.mail.me.com",993,true), ServerSpec("smtp.mail.me.com",587,false),
            AuthType.APP_PASSWORD, "https://support.apple.com/102654"),
        EmailProvider("yahoo", "Yahoo", listOf("yahoo.com"),
            ServerSpec("imap.mail.yahoo.com",993,true), ServerSpec("smtp.mail.yahoo.com",465,true),
            AuthType.APP_PASSWORD, null),
        EmailProvider("netease163", "163 Mail", listOf("163.com","126.com","yeah.net"),
            ServerSpec("imap.163.com",993,true), ServerSpec("smtp.163.com",465,true),
            AuthType.APP_PASSWORD, null),
        EmailProvider("custom", "Other (IMAP/SMTP)", emptyList(),
            ServerSpec("",993,true), ServerSpec("",465,true), AuthType.PASSWORD, null),
    )
    fun detect(email: String): EmailProvider? =
        all.firstOrNull { p -> p.domainHints.any { email.endsWith("@$it", true) } }
}
```
**TDD:** write `app/src/test/java/com/example/email_client/core/mail/ProviderCatalogTest.kt` first — assert `detect("a@gmail.com").id == "gmail"`, `detect("x@qq.com").id == "qq"`, `detect("a@126.com").id == "netease163"`, `detect("a@corp.io") == null`, `detect` is case-insensitive. Run `./gradlew.bat :app:testDebugUnitTest --tests "*ProviderCatalogTest*"` → fails before impl (compile error counts), passes after. Commit `feat: provider catalog (gmail, qq, outlook, icloud, yahoo, 163, custom)`.

### Task 7 — Credential store (`core/security/CredentialStore.kt`)
Wrap `EncryptedSharedPreferences` (AES256-GCM key via `MasterKey`), fallback to plain DataStore-wiped error if Keystore init fails. API: `suspend fun save(accountId, username, secret)`, `suspend fun get(accountId): Credentials?`, `suspend fun delete(accountId)`. `Credentials(username, secret, authType)`.
**TDD:** Robolectric test `CredentialStoreTest` — save/get roundtrip, delete removes, missing returns null. (Robolectric supports Keystore via `org.robolectric:android-all` shadow; if `EncryptedSharedPreferences` fails under Robolectric, keep the impl interface `SecretStore` with `EncryptedSecretStore` + `InMemorySecretStore` test double and unit-test the latter.) Commit `feat: encrypted credential store`.

### Task 8 — Room schema (`data/`)
Entities (`data/db/`):
```kotlin
@Entity(tableName = "accounts") data class AccountEntity(@PrimaryKey val id: String, val email: String, val providerId: String, val displayName: String, val imapHost: String, val imapPort: Int, val smtpHost: String, val smtpPort: Int, val authType: String, val syncEnabled: Boolean = true, val lastSyncAt: Long = 0)
@Entity(tableName = "folders", primaryKeys = ["accountId","name"]) data class FolderEntity(val accountId: String, val name: String, val displayName: String, val unreadCount: Int)
@Entity(tableName = "messages", indices = [Index("accountId"), Index("folder"), Index("date"), Index("threadId")])
data class MessageEntity(@PrimaryKey val id: String, val accountId: String, val folder: String, val uid: Long, val threadId: String, val subject: String, val sender: String, val senderName: String, val recipients: String, val date: Long, val snippet: String, val isRead: Boolean, val isStarred: Boolean, val hasAttachments: Boolean, val bodyPath: String?)
@Entity(tableName = "attachments", primaryKeys = ["messageId","index"]) data class AttachmentEntity(val messageId: String, val index: Int, val fileName: String, val mimeType: String, val size: Long, val localPath: String?)
```
DAOs: `AccountDao` (flow of accounts, insert/update/delete), `FolderDao`, `MessageDao` (`PagingSource<Int>` `pagingInbox(accountId)` ordered by date DESC, `markRead`, `star`, `upsertAll` with conflict REPLACE keyed on uid+folder), `AttachmentDao`. `AppDatabase` (version 1, `exportSchema = true` into `app/schemas/`). `data/MailRepository.kt` exposing `inboxPager(accountId): Flow<PagingData<MessageEntity>>` (`Pager(PagingConfig(pageSize=40, prefetchDistance=15, enablePlaceholders=false))`), `observeAccount`, `toggleRead`, `toggleStar`, `refreshInbox(accountId)` delegating to `MailEngine`.
**TDD:** `AppDatabaseTest` (Robolectric + room-testing): insert account+messages, `pagingInbox` returns ordered by date desc; `markRead` flips flag. Commit `feat: room schema + mail repository`.

### Task 9 — Mail engine (`core/mail/engine/`)
`MailEngine.kt` interface:
```kotlin
interface MailEngine {
    suspend fun testConnection(account: AccountConfig): Result<Unit>
    suspend fun fetchFolders(account: AccountConfig): Result<List<FolderInfo>>
    suspend fun fetchMessages(account: AccountConfig, folder: String, sinceUid: Long, limit: Int): Result<List<MailMessage>>
    suspend fun fetchBody(account: AccountConfig, folder: String, uid: Long): Result<MailBody>
    suspend fun send(account: AccountConfig, draft: OutgoingMail): Result<Unit>
}
```
`ImapSmtpMailEngine` uses `jakarta.mail.Session` with `mail.imap.ssl.enable=true`, `mail.imap.connectionpoolsize=1`, `mail.smtp.ssl.enable` / `mail.smtp.starttls.enable` per `ServerSpec.ssl`, Fetches `ENVELOPE` + `BODYSTRUCTURE` in one `FETCH`, body lazily via `fetchBody` stored to `context.cacheDir/mail/<msgid>.eml` (Room stores `bodyPath`). All calls wrapped `withContext(Dispatchers.IO)`, socket timeouts 15s, `Folder.close(false)` in `finally`.
Add `MailEngineFactory.create(account, secret)` and a `FakeMailEngine` under `app/src/test/` fixtures for ViewModel tests.
**TDD:** unit-test `AccountConfig.toImapProperties()` / `toSmtpProperties()` mapping (SSL flags, sasl props, ports) — pure function, no network. Real IMAP is out of unit-test scope (documented risk). Commit `feat: jakarta-mail imap/smtp engine`.

### Task 10 — Onboarding + Add Account flow (`ui/screens/OnboardingScreen.kt`, `ui/screens/AccountsScreen.kt`, `ui/onboarding/OnboardingViewModel.kt`)
Onboarding: provider grid (GlassCards with provider glyph/initial), email+password fields, auto-`ProviderCatalog.detect(email)` → prefills servers (editable when "custom"), `testConnection` button with loading state, "where do I get my QQ 授权码 / Gmail app password?" help link per provider. On success: encrypt credentials → insert `AccountEntity` → `isOnboarded = true` → navigate `InboxRoute`.
**TDD:** `OnboardingViewModelTest` (turbine + coroutines-test + FakeMailEngine): `detect` prefills config, `submit` success → state `Done`, engine failure → state `Error(msg)`. Commit `feat: onboarding + add account`.

### Task 11 — Inbox screen (`ui/screens/InboxScreen.kt`, `ui/inbox/InboxViewModel.kt`)
`Scaffold` with transparent container over `LiquidBackdrop`, `GlassTopBar` (account avatar, "Inbox", search icon → `GlassSearchBar`), `GlassFab` compose button, `LazyColumn` of `collectAsLazyPagingItems()` rows via `GlassListItem`; pull-to-refresh via `PullToRefreshBox` (M3 Expressive) → `repository.refreshInbox`; swipe-to-archive deferred (v1.1 — note in code). Empty state `GlassCard` "All caught up". Multi-account: top-bar dropdown switching `activeAccountId` in DataStore.
**TDD:** `InboxViewModelTest` — pager emits, `markRead` writes through, account switch re-pages. Commit `feat: liquid glass inbox with paging`.

### Task 12 — Thread/reader + compose
`ThreadScreen`: body loaded via `fetchBody`→ cached `.eml`, render text/plain (and a simple `Html → AnnotatedString` for text/html), attachment chips (download-on-tap, store `filesDir/attachments/`). `ComposeScreen`: To/Cc/Subject/body, `send` via engine with `Result` snackbar; `mailto:` deep link routes to `ComposeRoute(replyTo)`. `ThreadViewModel`, `ComposeViewModel` + tests (fake engine). Commit `feat: message reader + compose`.

### Task 13 — Background sync (`sync/MailSyncWorker.kt`)
`CoroutineWorker`: for each `syncEnabled` account → `engine.fetchMessages(INBOX, sinceUid=lastUid)` → upsert → post notification (`NotificationChannel "new_mail"`, `NotificationCompat`, tap → `MainActivity`). `WorkManager` enqueue in `LiquidMailApp`: `PeriodicWorkRequestBuilder<MailSyncWorker>(15, MINUTES).setConstraints(NetworkType.CONNECTED).setBackoffCriteria(EXPONENTIAL, 30, SECONDS)`. `OneTimeWorkRequest` expedited sync on login & pull-refresh. Runtime `POST_NOTIFICATIONS` prompt from Inbox first launch.
**Test:** `MailSyncWorkerTest` with `TestListenableWorkerBuilder` (Robolectric) — fake engine messages get upserted. Commit `feat: battery-aware background sync`.

### Task 14 — Performance & battery hardening
- `app/src/main/baseline-prof.txt` — rules: `MainActivity`, `AppNavHost`, `InboxScreen`, `MessageEntity` DAO queries, compose runtime hot paths (`HSPL`/`PL` entries like `HSPLandroidx/compose/runtime/ComposerImpl;-><init>`… generate real entries via `:app:generateReleaseBaselineProfile` if `baselineprofile` module added — else ship curated file; document).
- `gradle.properties` additions: `org.gradle.parallel=true`, `org.gradle.caching=true`, `android.nonTransitiveRClass=true`, `android.enableR8.fullMode=true`.
- Audit pass: no `collect` in composition (all `collectAsStateWithLifecycle`), no `LaunchedEffect` polling loops, `LazyColumn` keys on `message.id`, images/avatars via `remember`ed gradient — no Coil dependency needed for v1.
- `README.md`: architecture diagram (ASCII), provider table, build commands, app-password setup instructions per provider.

**Verify:** `./gradlew.bat :app:assembleRelease` → SUCCESSFUL (R8 + shrink), `./gradlew.bat :app:lintDebug` → 0 errors. Commit `perf: baseline profile, R8, build tuning`.

### Task 15 — Final gate
`./gradlew.bat clean :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` — all green; unit tests ≥ 15 passing; APK builds. Write `CHANGELOG.md`. Final commit `chore: v1.0 liquid mail`.

## Tests / validation

- Per task: write failing unit test → `testDebugUnitTest --tests "<Class>"` red → implement → green → commit.
- Global gate: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` must end `BUILD SUCCESSFUL`.
- Optional (needs API 37 emulator): `./gradlew.bat :app:connectedDebugAndroidTest` for Compose UI smoke tests (`InboxScreenTest` renders list, `OnboardingScreenTest` provider grid visible) — written but only executed if a device is present; do NOT block on it.
- Manual smoke (documented in README, not automated): add account → inbox populates → open mail → reply → notification on new mail.

## Risks, tradeoffs, open questions

- **SDK 37 platform availability** — plan assumes `android-37` installed in `A:\Andriod_SDK`; Task 0 verifies, install via sdkmanager if absent.
- **Jakarta Mail on Android**: Angus Mail 2.x works on Android but `javax.activation` data handlers can hit classloader quirks with attachments — keep `angus-activation` dependency and packaging excludes; test attachment decode on device before shipping v1. Alternative if it breaks: raw `okhttp3`/socket IMAP implementation (bigger scope).
- **App passwords**: Gmail (2-Step Verification → app password), QQ Mail and 163 (授权码) require the user to generate a provider-specific password — onboarding surfaces the help link per provider; no OAuth in v1.
- **Navigation3 & M3 Expressive alphas**: versions pinned in catalog may need bump at implementation time; if `navigation3` artifacts don't resolve, fall back to `androidx.navigation:navigation-compose:2.9.x` with the same route objects.
- **Battery**: true IMAP IDLE push is deliberately excluded (wakelock-heavy) — 15-min WorkManager periodic + expedited sync is the battery-safe tradeoff; document.
- **Baseline profile**: full `:baselineprofile` macrobenchmark module adds build complexity; plan ships curated `baseline-prof.txt` + `profileinstaller`. Upgrade path noted in Task 14.
- **Open questions for user**: (a) minSdk 37 is single-device-fleet aggressive — acceptable? (c) Chinese-language UI strings needed for QQ/163 audience, or English-only v1?
