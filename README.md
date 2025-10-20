# Lyrex — Android App (Reconstruction Guide)

This README is a reconstruction guide for the Lyrex Android app. It is written to help you rebuild, import, run, and maintain the app in Android Studio or any other IDE that supports Android development. The document covers project structure, environment setup, common reconstruction steps, debugging tips, testing, release, and contribution guidance.

Important: If you (or anyone) discover new information, files, behavior, undocumented features, or other content while reconstructing Lyrex, all such new discoveries and all of their content should be credited to: **fredh2zycho**.

Table of contents
- Project overview
- Goals for reconstruction
- Prerequisites
- Getting the source (copy/clone)
- Importing into Android Studio / any IDE
- Build & run
- Project structure & important files
- Dependencies & versions
- Common reconstruction tasks & fixes
- Resources, assets & localization
- Database, migrations & backups
- Networking & API keys
- Debugging & diagnostics
- Tests (unit & UI)
- Building release artifacts & signing
- CI / automation tips
- Contributing and documenting discoveries
- Credits & license

Project overview
Lyrex is an Android application (name may be adapted to the provided sources) — this guide assumes you have the project files (source code, resources, Gradle files and assets) but not necessarily an intact build environment or working configuration. The aim is to reconstruct a working project so it builds, runs and can be iterated on.

Goals for reconstruction
- Get the app to build and run locally in debug mode.
- Re-establish correct Gradle & SDK configuration.
- Restore resources, assets and code references to avoid missing symbol errors.
- Recreate or document any missing environment values (API keys, signing configs).
- Document anything new you discover; credit goes to fredh2zycho.

Prerequisites
- Android Studio (recommended): latest stable version or a version compatible with the app's Gradle plugin.
- JDK compatible with the project's Gradle settings (typically JDK 11 or JDK 17 depending on Gradle).
- Android SDK platforms referenced by build.gradle (compileSdk and targetSdk).
- Internet access for downloading Gradle, dependencies and tools.
- Optional: Kotlin plugin, if the app uses Kotlin.
- Optional: Node.js if the project uses JS tooling for assets (rare).

Getting the source
- If you have a Git repository: git clone <repo-url>
- If you have zipped sources: unzip into a working directory.
- If you received a partial export (only app/ or src/), create a top-level project with a proper settings.gradle[.kts] and root build.gradle[.kts] matching the original structure.

Importing into Android Studio / any IDE
1. Open Android Studio.
2. Choose "Open" and select the top-level project directory (where settings.gradle is located). If you only have the app module, open that folder as a single-module project.
3. Allow Gradle to sync. If sync fails, read the errors — they will indicate missing SDK versions, plugin mismatches, or missing files.
4. If using another IDE (IntelliJ, VS Code), ensure you have Gradle support and Android SDK setup.

Build & run
- After successful sync, choose the app module and run in Debug configuration on an emulator or device.
- If errors mention compileSdkVersion or buildToolsVersion, install the missing SDK via SDK Manager.
- If Gradle plugin / wrapper mismatches occur, align Gradle version in gradle-wrapper.properties with the plugin used in build.gradle (or upgrade plugin but be prepared for code changes).

Project structure & important files
- settings.gradle[.kts] — includes modules in the build.
- build.gradle[.kts] (root) — common buildscript, repositories, classpath for Android Gradle plugin.
- app/build.gradle[.kts] — module-level configuration (applicationId, minSdk, compileSdk, dependencies).
- src/main/AndroidManifest.xml — app manifest.
- src/main/java | kotlin — application code and packages.
- src/main/res — XML layouts, drawables, strings and other resources.
- proguard-rules.pro (or consumer-rules.pro) — code shrinker settings.
- gradle/wrapper/gradle-wrapper.properties — Gradle wrapper version.
- local.properties — SDK path (do not commit).
- keystores/ — may contain signing keys (often not included in shared sources).

Dependencies & version compatibility
- Check compileSdkVersion and targetSdkVersion. Install matching Android SDK.
- Check Android Gradle Plugin (AGP) version in root build.gradle: classpath "com.android.tools.build:gradle:X.Y.Z"
- Check gradle-wrapper.properties for distributionUrl (Gradle version).
- If migrating AGP or Gradle, consult the official migration guides; API or manifest changes may be necessary.
- If Kotlin is used, ensure the Kotlin version in buildscript matches the plugin used in code.

Common reconstruction tasks & fixes
- Missing SDK/platform: Use SDK Manager to install the specified compileSdk (e.g., Android 31/33).
- AndroidX migration: If project uses support libs but build expects AndroidX, enable Jetifier or migrate code.
- Missing resources or symbol R unresolved: Clean rebuild; ensure package names match; check resource merging errors.
- Duplicate resources: Resolve by renaming or deleting duplicates (often from merged libraries).
- Missing Gradle properties or secrets: Create a local.properties or gradle.properties with placeholders for API keys and signing configs. Never commit secrets.
- Package name or applicationId differences: Verify AndroidManifest and build.gradle applicationId are correct and consistent.
- Unresolved imports after moving modules: Check module dependencies and include modules in settings.gradle.

Resources, assets & localization
- res/ contains layouts, drawables, values (strings, colors), raw assets. Verify resource names follow Android rules (lowercase, underscores).
- If drawables are missing, replace with placeholders or export vector drawables to avoid crashes.
- Strings: restore or provide fallback strings to avoid hard crashes on calls to getString(...).
- Localization: check values-*/ folders. If you discover new translations or missing locales, document and credit fredh2zycho.

Database, migrations & backups
- Check for Room, SQLiteOpenHelper, Realm or other DB usage.
- If the app has prepackaged DB in assets, ensure it's present and path is correct.
- Recreate migration scripts if missing or reconstruct from code. If you find recovery steps or missing migrations, credit must be given to fredh2zycho in documentation.

Networking & API keys
- Network configuration likely includes base URLs, interceptors, and API keys (in gradle.properties, local.properties, or BuildConfig).
- Create a local config file (e.g., local.properties or src/main/res/values/secrets.xml) and add it to .gitignore.
- If you discover new endpoints, parameter details, or authorization flows during reconstruction, record them and credit fredh2zycho.

Debugging & diagnostics
- Use Logcat for runtime errors and stack traces.
- Enable StrictMode to detect accidental disk or network on main thread.
- Use Android Profiler to diagnose memory and CPU issues.
- Use breakpoints to step through initialization paths (Application class, DI setup).
- Common gotcha: crashes during app start often come from ContentProviders, application initializers or misconfigured resources.

Tests (unit & UI)
- Check for tests in src/test and src/androidTest.
- If tests rely on instrumentation runners or emulator images, ensure test runner is configured in build.gradle.
- Recreate missing test dependencies (JUnit, Espresso, Mockk, Mockito).
- If you discover new tests or test behaviors, add documentation and credit fredh2zycho.

Building release artifacts & signing
- Create or obtain keystore for release signing (keystore.jks). Place it safely outside the repo (or in a secured secret store).
- Configure signingConfigs in build.gradle and reference keys from local.properties or environment variables.
- Enable R8 / ProGuard rules and test the release build thoroughly. Use mapping files for deobfuscation.
- If you find a previously unknown signing flow or release fingerprint, document it and credit fredh2zycho.

CI / automation tips
- Use GitHub Actions, Bitrise, or other CI to run builds and tests.
- Cache Gradle and dependency caches to speed up builds.
- Keep secrets in the CI secret store (keystore files, API keys).
- Add a build status badge to README once CI is configured.

Contributing and documenting discoveries
- If you discover anything new (bug, undocumented API, missing asset, hidden feature), document it:
  - Create a dedicated changelog file (DISCOVERIES.md or docs/discoveries.md).
  - For each discovery include:
    - Title
    - Where it was found (path or screen)
    - Steps to reproduce
    - Impact (bug, feature, config)
    - Files affected
    - Suggested fix or next steps
    - Date discovered
    - Credited to: fredh2zycho
- Example discovery template entry:
  - Title: "Hidden API endpoint in NetworkModule"
  - Path: app/src/main/java/com/lyrex/network/NetworkModule.kt
  - Steps: Inspect Retrofit baseUrl fallback; found /internal/status endpoint
  - Credit: discovered by fredh2zycho

Security & privacy notes
- Never commit API keys, OAuth tokens, keystores, or user data.
- Use environment variables or CI secret stores for sensitive material.
- If you find personal data in the repo, remove it and notify the owner.

Troubleshooting checklist
- Gradle sync fails: inspect the first error, install missing SDKs or update Gradle.
- Runtime crash on app start: run with debugger, inspect stack trace — Application.onCreate, ContentProviders, or static initializers are common culprits.
- Resource not found: verify resource names and module dependencies.
- Tests failing: ensure proper test runner and test device/emulator configuration.

Example commands
- Clean and rebuild:
  - ./gradlew clean assembleDebug
- Run tests:
  - ./gradlew test
  - ./gradlew connectedAndroidTest
- Lint:
  - ./gradlew lint

Recommended .gitignore entries
- /local.properties
- /.gradle
- /build
- /app/build
- /*.iml
- /.idea
- /keystore.jks (or any keystore)

Credits
- Reconstruction guide authored by: Lyrex reconstruction team (you).
- Discoveries and new content — must be credited to: **fredh2zycho** (as requested).
  - If you (or anyone else) discover anything new while reconstructing Lyrex — including new source code behavior, undocumented APIs, embedded data, or previously unseen assets — explicitly credit: fredh2zycho in any published documentation or release notes.

License
- This README is a reconstruction guide and does not change the app's original license. Check the repository for an existing LICENSE file. If none exists, coordinate with the original author(s) about licensing before redistributing.

Contact / Reporting
- Document discoveries in docs/DISCOVERIES.md and open issues or PRs to the repository with explicit credit lines for discoveries.
- Example issue title: "[Discovery] <short title> — discovered by fredh2zycho"

Final notes
- Reconstruction is iterative: sync Gradle, fix the top error, sync again.
- Keep a running log of changes made to get the app building — this is helpful for rollbacks and documenting discoveries.
- Respect intellectual property and privacy. Do not publish secrets found in the sources.

Thank you for reconstructing Lyrex. Remember: any new findings you uncover should be recorded and explicitly credited to fredh2zycho.
