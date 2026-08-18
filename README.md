# NetworkGuardian

A local-first Android app for managing devices on a network or hotspot **you own or are
explicitly authorized to administer**. NetworkGuardian helps you see who's connected, mark
devices as trusted, maintain a local blocklist, and understand — honestly — what Android
actually lets a third-party app enforce.

## What this app is (and isn't)

NetworkGuardian is a management and visibility tool, not an attack tool. It does not
implement deauthentication attacks, packet injection, ARP spoofing, credential theft, or any
technique for accessing or controlling a network without authorization. Every network-control
action either uses a documented Android API or is clearly labeled as unsupported — the app
never fakes success.

**The most important thing to understand before using it:** on stock Android, there is no
public API that lets an ordinary third-party app enumerate the client list of your own
hotspot, or force-disconnect/ban a device from it. NetworkGuardian's blocklist is real and
persistent (it survives restarts), but on most devices today, blocking is **list-only** — it
records your intent, and the Settings → Diagnostics screen will honestly show
`Persistent Block Enforcement: NOT AVAILABLE` rather than pretending otherwise.

## Architecture

- **Language:** Kotlin, Jetpack Compose (Material 3), MVVM
- **Async:** Kotlin Coroutines + Flow/StateFlow
- **Persistence:** Room (devices, trust, blocklist, history, settings, network profiles)
- **Background work:** WorkManager (periodic discovery), a foreground service only while
  monitoring is actively enabled
- **DI:** a small hand-rolled `AppGraph` (no Hilt/Dagger — kept intentionally lightweight)

```
app/src/main/java/com/networkguardian/
  data/            Room entities, DAOs, database, repositories
  domain/          Domain models and use cases
  network/         Discovery (subnet reachability + ARP table read), monitoring
                    (ConnectivityManager callback), device identification
  hotspot/         Honest wrapper around what Android exposes for hotspot state
  blocking/        BlockManager + NetworkController abstraction (for future router support)
  security/        PIN hashing, app-lock, biometric wrapper
  notifications/   Notification channels + category-gated sending
  ui/              Screens, navigation, theme, shared ViewModel
  workers/         Periodic discovery worker
```

## Capability detection — the honesty layer

`network/discovery/CapabilityDetector.kt` is the single source of truth for what this device
actually permits, checked at runtime rather than assumed. Every screen and every
block/unblock/pause action defers to it. The Settings screen exposes it directly as a
diagnostics table (`Supported` / `Limited` / `Not available`) so you always know what's real.

Known limitations this class encodes deliberately:

| Capability | Status on stock Android | Why |
|---|---|---|
| Local device discovery | Limited | Subnet reachability checks + `/proc/net/arp` reads — reflects recently-active hosts, not authoritative enumeration |
| Network state / monitoring | Supported | `ConnectivityManager`/`NetworkCapabilities` are public, documented APIs |
| Hotspot on/off detection | Not available | No stable public API for third-party apps |
| Hotspot client enumeration | Not available | No public API on stock, non-privileged Android |
| Hotspot client block/kick | Not available | No public API; this app does not use reflection/hidden APIs to fake it |

## Router support (extension point, not implemented)

`blocking/NetworkController.kt` defines the interface a real enforcement backend would
implement. `AndroidHotspotController` is the only shipped implementation, and it always
returns `Unsupported` — honestly reflecting Android's limits. A `RouterController` class is
scaffolded but intentionally empty: wiring it to a specific router vendor's official local
admin API (with credentials you provide) is future work, and doing so with anything other
than an official, documented API is out of scope for this project.

## Permissions

Only requested when an actual API needs them:

- `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` — reading connectivity/subnet info
- `CHANGE_WIFI_STATE` — required by some Wi-Fi state read paths on certain OS versions
- `POST_NOTIFICATIONS` — device-activity alerts (Android 13+)
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` — only while monitoring is active
- `USE_BIOMETRIC` — optional app-lock biometric prompt

No location, contacts, SMS, camera, microphone, or storage permissions are requested.

## Build instructions

Requires JDK 17 and the Android SDK (compileSdk 34).

```bash
# gradle/wrapper/gradle-wrapper.jar is not included in this generated project (binary
# artifacts can't be produced by the tool that generated this repo). Either:

# Option A: generate the wrapper jar with a local Gradle install, then use ./gradlew as usual
gradle wrapper --gradle-version 8.7
./gradlew testDebugUnitTest
./gradlew assembleDebug

# Option B: use a local Gradle 8.7+ install directly
gradle testDebugUnitTest
gradle assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions

`.github/workflows/build.yml` checks out the repo, sets up JDK 17 and the Android SDK,
provisions Gradle 8.7 via `gradle/actions/setup-gradle` (this sidesteps the missing wrapper
jar — see the Build instructions note above), runs unit tests, builds the debug APK, and
uploads it as a workflow artifact named `networkguardian-debug-apk`. A disabled
`release-build` job is included as a starting point; enable it and add signing secrets to
produce a signed release build.

## Privacy model

- All device/network data is stored locally in Room by default.
- No cloud account or backend is required for core functionality.
- No network data is uploaded anywhere by the core app.
- The app only performs actions you initiate and, for destructive ones (block, forget),
  explicitly confirm.

## Security model

- Optional app PIN, hashed with salted PBKDF2 (never stored in plaintext) and biometric
  unlock via `androidx.biometric`.
- Configurable auto-lock (immediately / 1 / 5 / 15 minutes / never).
- No secrets are transmitted off-device.

## Known Android limitations (read this before filing a "bug")

- Android does not universally allow third-party apps to permanently ban hotspot clients —
  this is a platform limitation, not a bug in this app. The app reports this honestly rather
  than working around it with hidden APIs.
- Discovery is best-effort and local-subnet-based; devices that haven't recently communicated
  on the link may not appear until they do.
- OEM skins (e.g. some Samsung/Xiaomi builds) may further restrict background network reads;
  the capability detector will reflect this at runtime where it can be detected.

## Testing

Unit tests cover blocklist logic (including the "never claim enforcement that didn't happen"
guarantee), device identification's conservative UNKNOWN-by-default behavior, PIN hashing, and
formatting utilities used by the UI. Run with `./gradlew testDebugUnitTest` (or `gradle
testDebugUnitTest`).
