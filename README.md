# FoldSense

**FoldSense** is a small Android library (AAR) that infers foldable **unfold / fold** from the system `Configuration.smallestScreenWidthDp` (plus optional API 30+ `WindowMetrics`). It does **not** require `androidx.window`.

Use it when your dependency catalog is locked and you cannot bump AndroidX or add new Jetpack modules.

| | |
|---|---|
| Package | `com.cicadahk.foldsense` |
| Version | `1.0.0` (`FoldSense.VERSION`) |
| minSdk | 21 |
| compileSdk | 34 |
| Language | Kotlin (callable from Java, `@JvmStatic`) |
| License | MIT |
| Repo | https://github.com/cubecnelson/foldsense |

**Modules**

| Module | Description |
|--------|-------------|
| `:foldsense` | Publishable library |
| `:sample` | Demo Activity |

Further docs: [`docs/integration.md`](docs/integration.md) · [`docs/api.md`](docs/api.md) · [`docs/fold-detection.md`](docs/fold-detection.md)

---

## Why not androidx.window?

The official fold posture APIs (`WindowInfoTracker` / `FoldingFeature`) require:

```text
androidx.window:window
```

That dependency often pulls newer transitive versions of `activity`, `lifecycle`, and related artifacts. If your catalog is already pinned (for example activity `1.1.0`, appcompat `1.1.0`), adding Window **can force a cascade of upgrades**.

FoldSense is designed so that:

- There is **no** `androidx.window` dependency
- Runtime uses only the Android SDK: `Activity.registerComponentCallbacks` + `Configuration`
- `androidx.annotation` is `compileOnly` (annotations only; it does not force consumers to upgrade)
- On API 30+, it can use the platform `WindowManager.currentWindowMetrics` (still no Window Jetpack)

Trade-off: it detects that **smallest screen width changed**, **not** hinge angle or half-open Flex Mode. See [Limitations](#limitations) and [`docs/fold-detection.md`](docs/fold-detection.md).

## Install (JitPack)

1. Add JitPack to the root `settings.gradle.kts` (or the older `allprojects` block):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
```

2. In the app module:

```kotlin
dependencies {
    // Prefer a Git tag (create tag 1.0.0 or v1.0.0 on GitHub)
    implementation("com.github.cubecnelson.foldsense:foldsense:1.0.0")

    // Or pin a commit (current main works):
    // implementation("com.github.cubecnelson.foldsense:foldsense:a7846c157a21806b4a2c405a3c34758e0a186263")
}
```

JitPack multi-module coordinate format: `com.github.<user>.<repo>:<module>:<tagOrCommit>`.  
The repo includes `jitpack.yml` (OpenJDK 17). The first build may wait while JitPack compiles.

## Install (local module)

```kotlin
// settings.gradle.kts
include(":foldsense")
project(":foldsense").projectDir = file("../path/to/FoldSense/foldsense")

// app/build.gradle.kts
dependencies {
    implementation(project(":foldsense"))
}
```

This does not change your other dependency versions.

## Quick start

```kotlin
import com.cicadahk.foldsense.FoldSense
import com.cicadahk.foldsense.FoldState

class MainActivity : AppCompatActivity() {
    private var foldSense: FoldSense? = null

    override fun onStart() {
        super.onStart()
        foldSense = FoldSense.attach(this)
            .addListener { unfolded, smallestWidthDp, state ->
                // unfolded == true  → smallestWidthDp grew (typical unfold)
                // unfolded == false → smallestWidthDp shrank (typical fold)
                when (state) {
                    FoldState.UNFOLDED -> { /* wide layout */ }
                    FoldState.FOLDED -> { /* narrow layout */ }
                    FoldState.UNKNOWN -> { /* not enough samples yet */ }
                }
            }
    }

    override fun onStop() {
        foldSense?.detach()  // unregisters and clears listeners
        foldSense = null
        super.onStop()
    }
}
```

From a Fragment, use `FoldSense.attach(requireActivity())` and manage it in `onStart` / `onStop` the same way.

Query current state:

```kotlin
foldSense?.isLikelyUnfolded()       // state == UNFOLDED
foldSense?.currentState()          // FOLDED / UNFOLDED / UNKNOWN
foldSense?.currentSmallestWidthDp()
FoldSense.VERSION                  // "1.0.0"
```

Optional helpers (not the fold detector itself):

```kotlin
FoldSense.isLandscapeBoundsOrFalse(this)  // always false below API 30
WindowMetricsHelper.currentWidthPx(this)
WindowMetricsHelper.currentHeightPx(this)
```

Pure comparison (no attach required):

```kotlin
val unfolded: Boolean? = FoldSense.detectTransition(previousSwDp = 320, currentSwDp = 600)
// true = grew; false = shrank; null = no change / invalid previous
```

## Manifest `configChanges` (optional)

`:sample` demonstrates:

```xml
<activity
    android:name=".MainActivity"
    android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboardHidden"
    android:exported="true">
```

- **With** `configChanges`: the Activity is not recreated. FoldSense receives `onConfigurationChanged` via `ComponentCallbacks`, and listeners fire on the same Activity instance.
- **Without** `configChanges`: the system may recreate the Activity. You do not need to forward `onConfigurationChanged` yourself; just call `FoldSense.attach(...)` again in the new instance’s `onStart`. After the first attach, `currentState()` is `UNKNOWN` until the next SW dp change.

FoldSense **does not require** you to override `Activity.onConfigurationChanged`.

## API overview

| Type | Highlights |
|------|------------|
| `FoldSense` | `attach` / `detach` / listeners / `isLikelyUnfolded` / `currentState` / `detectTransition` / `isLandscapeBoundsOrFalse` |
| `FoldListener` | `onFoldChanged(unfolded, smallestWidthDp, state)` (`fun interface`, lambda-friendly) |
| `FoldState` | `FOLDED`, `UNFOLDED`, `UNKNOWN` |
| `WindowMetricsHelper` | `isLandscapeBounds` (API 30+), `currentWidthPx`, `currentHeightPx` |

Full table: [`docs/api.md`](docs/api.md). Locked-catalog integration: [`docs/integration.md`](docs/integration.md).

## Limitations

- This is **not** hinge-angle detection, **not** `FoldingFeature`, and it does **not** detect half-open / book / tabletop postures.
- Split-screen, external displays, some flip devices, and font / density changes that alter SW can also fire the callback.
- Immediately after `attach`, FoldSense does not guess whether the device is currently folded or unfolded (`UNKNOWN`); it waits for an SW change.
- Listeners are invoked on the configuration callback thread; use `runOnUiThread` to update the UI (the sample does this).
- `detach()` **clears all** listeners.

## Sample module

```text
:sample → MainActivity
```

It attaches in `onStart` and shows `FoldState`, SW dp, window px, and `isLandscapeBoundsOrFalse`. Open the repo in Android Studio and Run `:sample` (a real foldable or foldable emulator is recommended).

## Build locally

Sync with Android Studio Giraffe+ (AGP 8.2). If the wrapper jar is missing:

```bash
gradle wrapper --gradle-version 8.2
./gradlew :foldsense:assembleRelease
./gradlew :foldsense:test
```

## License

MIT © 2026 Nelson Cheung
