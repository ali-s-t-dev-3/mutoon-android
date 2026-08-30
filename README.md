# Mutoon

Mutoon is an open-source Android app for browsing book covers and reading bundled PDFs entirely offline. This repository currently contains only the foundation established by [MUT-2](https://ali-t.atlassian.net/browse/MUT-2); the library and reader features are intentionally deferred to later tickets.

## Product guardrails

- Android package and application ID: `io.github.alistdev3.mutoon`
- Android 9 or newer (`minSdk 28`)
- No Internet permission, ads, analytics, telemetry, crash reporting, accounts, or cloud services
- Kotlin, Jetpack Compose, and Material 3
- Code is GPL-3.0-only; book files and other content have separate rights and are not covered by the code license
- Do not commit PDFs, supplied screenshots, signing material, credentials, or user data

The planned PDF reader dependency (`androidx.pdf:pdf-viewer-fragment:1.0.0-beta01`) is deliberately not included until the reader ticket is implemented.

## Pinned toolchain

| Component | Version |
| --- | --- |
| JDK | 17 |
| Gradle wrapper | 9.4.1 |
| Android Gradle Plugin | 9.2.0 |
| Kotlin / Compose compiler plugin | 2.3.21 |
| Compose BOM | 2026.08.00 |
| Android compile/target SDK | 37 |
| Android SDK Build Tools | 36.0.0 |

Android Studio is optional. The free Android SDK command-line tools are sufficient.

## Command-line setup

1. Install a JDK 17 distribution and set `JAVA_HOME` to it.
2. Download the Android SDK command-line tools from the [official Android developer site](https://developer.android.com/studio#command-tools).
3. Set `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) to the SDK directory and add its `cmdline-tools/latest/bin` directory to `PATH`.
4. Accept the Android SDK licenses, then install the required packages with the current Android CLI:

   ```text
   sdkmanager --licenses
   android --sdk="$ANDROID_HOME" --no-metrics sdk install platform-tools platforms/android-37.0 build-tools/36.0.0
   ```

5. If the SDK is not exposed through an environment variable, create an untracked `local.properties` file containing `sdk.dir=<absolute SDK path>`.

## Build and verify

From a fresh checkout on Windows PowerShell:

```powershell
.\gradlew.bat clean assembleDebug lintDebug testDebugUnitTest
```

On macOS or Linux:

```sh
./gradlew clean assembleDebug lintDebug testDebugUnitTest
```

The debug APK is generated under `app/build/outputs/apk/debug/`. GitHub Actions runs the build, lint, and unit-test checks for pull requests and pushes to `main`.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Work is organized as one Jira ticket per branch and pull request.

## License and content rights

Source code is licensed under the [GNU General Public License v3.0 only](LICENSE). PDFs, covers, translations, and other book content are separate works and may be redistributed only when their rights have been confirmed and documented.
