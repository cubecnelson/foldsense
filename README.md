# FoldSense

**FoldSense** 係一個細型 Android library（AAR），用系統 `Configuration.smallestScreenWidthDp`（同可選 API 30+ `WindowMetrics`）去推斷 foldable **展開／摺埋**，**唔需要** `androidx.window`。

適合 dependency catalog 鎖死、唔可以亂升 AndroidX／加新 Jetpack 模組嘅 app。

| | |
|---|---|
| Package | `com.cicadahk.foldsense` |
| Version | `1.0.0` (`FoldSense.VERSION`) |
| minSdk | 21 |
| compileSdk | 34 |
| Language | Kotlin（Java 可呼叫，`@JvmStatic`） |
| License | MIT |
| Repo | https://github.com/cubecnelson/foldsense |

**Modules**

| Module | 說明 |
|--------|------|
| `:foldsense` | 可發布 library |
| `:sample` | Demo Activity |

延伸文件：[`docs/integration.md`](docs/integration.md) · [`docs/api.md`](docs/api.md) · [`docs/fold-detection.md`](docs/fold-detection.md)

---

## 繁中（香港）

### 點解唔用 androidx.window？

官方摺機姿勢 API（`WindowInfoTracker`／`FoldingFeature`）要加：

```text
androidx.window:window
```

呢個依賴好多時會拉高 `activity`、`lifecycle` 等 transitive 版本。如果你個工程 catalog 已經鎖死（例如 activity `1.1.0`、appcompat `1.1.0`），加 window **可能逼你升一串 library**。

FoldSense 刻意：

- **零** `androidx.window`
- runtime 只靠 Android SDK：`Activity.registerComponentCallbacks` + `Configuration`
- `androidx.annotation` 只係 `compileOnly`（注解，唔會逼消費者升級）
- API 30+ 用系統 `WindowManager.currentWindowMetrics`（同樣唔使 window Jetpack）

代價：偵測嘅係「最小螢幕闊度變咗」，**唔係** hinge 角度／半開 Flex Mode。詳見 [限制](#限制) 同 [`docs/fold-detection.md`](docs/fold-detection.md)。

### 安裝（JitPack）

1. 根專案 `settings.gradle.kts`（或舊式 `allprojects`）加 JitPack：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
```

2. app module：

```kotlin
dependencies {
    // 用 Git tag（建議喺 GitHub 開 tag：1.0.0 或 v1.0.0）
    implementation("com.github.cubecnelson.foldsense:foldsense:1.0.0")

    // 或者直接釘死 commit（而家 main 可用）：
    // implementation("com.github.cubecnelson.foldsense:foldsense:a7846c157a21806b4a2c405a3c34758e0a186263")
}
```

JitPack multi-module 座標格式：`com.github.<user>.<repo>:<module>:<tagOrCommit>`。  
Repo 內有 `jitpack.yml`（OpenJDK 17）。首次 build 可能要等 JitPack 編譯完成。

### 安裝（本地 module）

```kotlin
// settings.gradle.kts
include(":foldsense")
project(":foldsense").projectDir = file("../path/to/FoldSense/foldsense")

// app/build.gradle.kts
dependencies {
    implementation(project(":foldsense"))
}
```

唔會改你其他 dependency version。

### Quick start

```kotlin
import com.cicadahk.foldsense.FoldSense
import com.cicadahk.foldsense.FoldState

class MainActivity : AppCompatActivity() {
    private var foldSense: FoldSense? = null

    override fun onStart() {
        super.onStart()
        foldSense = FoldSense.attach(this)
            .addListener { unfolded, smallestWidthDp, state ->
                // unfolded == true  → smallestWidthDp 變大（典型 unfold）
                // unfolded == false → smallestWidthDp 變細（典型 fold）
                when (state) {
                    FoldState.UNFOLDED -> { /* 闊 layout */ }
                    FoldState.FOLDED -> { /* 窄 layout */ }
                    FoldState.UNKNOWN -> { /* 未有足夠樣本 */ }
                }
            }
    }

    override fun onStop() {
        foldSense?.detach()  // 會 unregister + clear listeners
        foldSense = null
        super.onStop()
    }
}
```

Fragment：用 `FoldSense.attach(requireActivity())`，同樣喺 `onStart`／`onStop` 管理。

查而家狀態：

```kotlin
foldSense?.isLikelyUnfolded()       // state == UNFOLDED
foldSense?.currentState()          // FOLDED / UNFOLDED / UNKNOWN
foldSense?.currentSmallestWidthDp()
FoldSense.VERSION                  // "1.0.0"
```

可選輔助（唔係 fold 本體）：

```kotlin
FoldSense.isLandscapeBoundsOrFalse(this)  // API < 30 一律 false
WindowMetricsHelper.currentWidthPx(this)
WindowMetricsHelper.currentHeightPx(this)
```

純比較（唔使 attach）：

```kotlin
val unfolded: Boolean? = FoldSense.detectTransition(previousSwDp = 320, currentSwDp = 600)
// true = 變大；false = 變細；null = 無變／previous 無效
```

### Manifest `configChanges`（可選）

`:sample` 示範咗：

```xml
<activity
    android:name=".MainActivity"
    android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboardHidden"
    android:exported="true">
```

- **有** `configChanges`：Activity 唔 recreate，FoldSense 經 `ComponentCallbacks` 收到 `onConfigurationChanged`，listener 會响同一個 Activity 實例觸發。
- **無** `configChanges`：系統可能 recreate Activity。你唔使手動 forward `onConfigurationChanged`；只要喺新實例 `onStart` 再 `FoldSense.attach(...)`。首次 attach 後 `currentState()` 係 `UNKNOWN`，直至下一次 SW dp 轉變。

FoldSense **唔要求**你 override `Activity.onConfigurationChanged`。

### API 一覽

| 類型 | 重點 |
|------|------|
| `FoldSense` | `attach`／`detach`／listener／`isLikelyUnfolded`／`currentState`／`detectTransition`／`isLandscapeBoundsOrFalse` |
| `FoldListener` | `onFoldChanged(unfolded, smallestWidthDp, state)`（`fun interface`，可用 lambda） |
| `FoldState` | `FOLDED`、`UNFOLDED`、`UNKNOWN` |
| `WindowMetricsHelper` | `isLandscapeBounds`（API 30+）、`currentWidthPx`、`currentHeightPx` |

完整表：[`docs/api.md`](docs/api.md)。

### 限制

- **唔係** hinge angle、**唔係** `FoldingFeature`、**唔偵測**「半開／書本／桌面」姿勢。
- 分屏、外接螢幕、部分翻蓋、字體／density 導致 SW 變，都可能觸發。
- attach 當下唔會估而家係 fold 定 unfold（`UNKNOWN`），要等一次 SW 轉變。
- Listener 喺 configuration callback thread 呼叫；更新 UI 請 `runOnUiThread`（sample 已示範）。
- `detach()` 會 **清晒** listeners。

### Sample module

```text
:sample → MainActivity
```

`onStart` attach + listener；顯示 `FoldState`、SW dp、window px、`isLandscapeBoundsOrFalse`。用 Android Studio 開 repo，Run `:sample`（建議真機／摺機 emulator）。

### 本機 build

Android Studio Giraffe+（AGP 8.2）Sync 即可。若未有 wrapper jar：

```bash
gradle wrapper --gradle-version 8.2
./gradlew :foldsense:assembleRelease
./gradlew :foldsense:test
```

---

## English

### Overview

FoldSense is a small Android AAR that infers foldable **unfold** / **fold** when `Configuration.smallestScreenWidthDp` **increases** / **decreases**. It registers via `Activity.registerComponentCallbacks` — **no** `androidx.window`, so it fits apps whose AndroidX catalog cannot take new Jetpack modules or forced upgrades.

Optional API 30+ helpers read `WindowManager.currentWindowMetrics` (platform API only).

### Why not androidx.window?

`androidx.window:window` pulls FoldingFeature / hinge APIs but often upgrades transitive `activity` / `lifecycle` artifacts. FoldSense avoids that entirely: only `compileOnly` `androidx.annotation` at compile time for the library itself.

Trade-off: configuration-size heuristics, not hinge posture. See [Limitations](#limitations) and [`docs/fold-detection.md`](docs/fold-detection.md).

### Install (JitPack)

```kotlin
// settings.gradle.kts
maven(url = "https://jitpack.io")

// app
implementation("com.github.cubecnelson.foldsense:foldsense:1.0.0")
// or pin a commit:
// implementation("com.github.cubecnelson.foldsense:foldsense:a7846c157a21806b4a2c405a3c34758e0a186263")
```

Coordinate shape: `com.github.<user>.<repo>:<module>:<tagOrCommit>`.

### Install (module)

```kotlin
implementation(project(":foldsense"))
```

### Quick start

```kotlin
private var foldSense: FoldSense? = null

override fun onStart() {
    super.onStart()
    foldSense = FoldSense.attach(this).addListener { unfolded, swDp, state ->
        // handle layout
    }
}

override fun onStop() {
    foldSense?.detach()
    foldSense = null
    super.onStop()
}
```

### Manifest note

Optional `android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboardHidden"` keeps the Activity instance across fold transitions (as in `:sample`). FoldSense does **not** require forwarding `onConfigurationChanged`.

### API summary

- **`FoldSense`** — lifecycle attach/detach, listeners, `isLikelyUnfolded()`, `currentState()`, `currentSmallestWidthDp()`, `detectTransition()`, `isLandscapeBoundsOrFalse()`, `VERSION`
- **`FoldListener`** — `onFoldChanged(Boolean, Int, FoldState)`
- **`FoldState`** — `FOLDED` | `UNFOLDED` | `UNKNOWN`
- **`WindowMetricsHelper`** — bounds helpers without androidx.window

Full reference: [`docs/api.md`](docs/api.md). Integration with locked catalogs: [`docs/integration.md`](docs/integration.md).

### Limitations

Not hinge-angle / Flex Mode. Multi-window, external displays, and some flip devices can change SW dp. State stays `UNKNOWN` until the first SW delta after `attach`. `detach()` clears all listeners. Callbacks may need `runOnUiThread` for UI work.

### Sample & license

Run `:sample`. MIT © 2026 Nelson Cheung.
