# FoldSense

Small Android library (AAR) that detects **foldable unfold / fold** using
`Configuration.smallestScreenWidthDp` (and optional API 30+ `WindowMetrics`)
**without** depending on `androidx.window` — safe for apps with a locked
dependency catalog.

---

## 繁中（香港）

### 做咩用
- 監聽 `onConfigurationChanged` 等價事件（經 `Activity.registerComponentCallbacks`）
- `smallestScreenWidthDp` **變大** → 視為 **展開 (unfold)**
- `smallestScreenWidthDp` **變細** → 視為 **摺埋 (fold)**
- API 30+ 可用 `WindowMetricsHelper` 讀 window bounds（仍然唔使 `androidx.window`）

### 限制
呢招係「螢幕最小闊度變咗」，唔係 hinge angle／`FoldingFeature`。分屏、外接螢幕、部分翻蓋機都可能觸發。真要 hinge／半開 Flex Mode，先至要加 `androidx.window`。

### 安裝（JitPack）

根 `settings.gradle.kts`：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
```

app `build.gradle.kts`：

```kotlin
dependencies {
    implementation("com.github.cubecnelson.foldsense:foldsense:1.0.0")
    // multi-module JitPack form: com.github.<user>.<repo>:<module>:<tag>
}
```

JitPack 會 build `:foldsense` module（見 `jitpack.yml`，JDK 17）。

### 安裝（本地 module）

```kotlin
// settings.gradle.kts
include(":foldsense")
project(":foldsense").projectDir = file("../FoldSense/foldsense")

// app/build.gradle.kts
implementation(project(":foldsense"))
```

### 用法

```kotlin
class MainActivity : AppCompatActivity() {
    private var foldSense: FoldSense? = null

    override fun onStart() {
        super.onStart()
        foldSense = FoldSense.attach(this).addListener { unfolded, swDp, state ->
            // unfolded == true → 好可能已展開
        }
    }

    override fun onStop() {
        foldSense?.detach()
        foldSense = null
        super.onStop()
    }
}
```

可選：Manifest 加  
`android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboardHidden"`  
（sample 已示範；唔加都得，系統 recreate 時你自己喺 `onCreate` 再 `attach`）。

### 公共 API
| 類別 | 說明 |
|------|------|
| `FoldSense` | 註冊／反註冊、`isLikelyUnfolded()`、`currentState()` |
| `FoldListener` | `onFoldChanged(unfolded, smallestWidthDp, state)` |
| `FoldState` | `FOLDED` / `UNFOLDED` / `UNKNOWN` |
| `WindowMetricsHelper` | API 30+ bounds 輔助 |

`minSdk 21` · `compileSdk / targetSdk 34` · Kotlin · **MIT**

---

## English

### What it does
Detects likely foldable **unfold** when `smallestScreenWidthDp` **increases**,
and **fold** when it **decreases**, via `ComponentCallbacks` — **zero**
`androidx.window` dependency. Optional API 30+ `WindowMetrics` helpers included.

### Consume via JitPack

```kotlin
// settings.gradle.kts — add maven(url = "https://jitpack.io")
implementation("com.github.cubecnelson.foldsense:foldsense:1.0.0")
```

### Consume as a module

```kotlin
implementation(project(":foldsense"))
```

### Quick start

```kotlin
val sense = FoldSense.attach(activity)
sense.addListener { unfolded, swDp, state -> /* ... */ }
sense.isLikelyUnfolded()
sense.currentState()
sense.detach()
```

### Modules
- `:foldsense` — publishable Android library (AAR)
- `:sample` — demo Activity

### Build locally
Open the repo in Android Studio (Giraffe+ / AGP 8.2), sync, run `:sample`.
Or: `./gradlew :foldsense:assembleRelease` after generating the Gradle wrapper
(`gradle wrapper` once).

### License
MIT © 2026 Nelson Cheung
