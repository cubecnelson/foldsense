# Integrating FoldSense into an app with a locked AndroidX catalog

繁中（香港）為主，English summary at the end.

## 目標

喺 **唔改現有 dependency version**、**唔加 `androidx.window`** 嘅前提下，將 FoldSense 接入你嘅 app，收到 unfold／fold 回調。

適用情境（對應原本需求）：catalog 已經鎖死舊版例如 `androidx.activity:activity:1.1.0`、`appcompat:1.1.0`，唔可以為咗摺機 API 去升 transitive。

## 方案 A — JitPack（推薦試用）

### 1. 加 repository

`settings.gradle.kts`：

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
```

舊式 root `build.gradle`：

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### 2. 加 dependency（唔升其他 library）

```kotlin
dependencies {
    implementation("com.github.cubecnelson.foldsense:foldsense:1.0.0")
}
```

釘 commit（可重現 build）：

```kotlin
implementation("com.github.cubecnelson.foldsense:foldsense:a7846c157a21806b4a2c405a3c34758e0a186263")
```

確認 Sync 之後 **Dependency 樹冇被逼升** activity／appcompat（FoldSense 冇 runtime AndroidX 依賴；只有 library 自己 compileOnly annotation）。

### 3. Activity 接入

```kotlin
import com.cicadahk.foldsense.FoldSense
import com.cicadahk.foldsense.FoldListener
import com.cicadahk.foldsense.FoldState

class HostActivity : AppCompatActivity() {

    private var foldSense: FoldSense? = null

    private val foldListener = FoldListener { unfolded, smallestWidthDp, state ->
        runOnUiThread {
            // 按 unfolded / state 切 layout、改 span count 等
        }
    }

    override fun onStart() {
        super.onStart()
        foldSense = FoldSense.attach(this).addListener(foldListener)
    }

    override fun onStop() {
        foldSense?.removeListener(foldListener) // 可選；detach 都會 clear
        foldSense?.detach()
        foldSense = null
        super.onStop()
    }
}
```

### 4. Manifest（可選但建議用於摺機）

```xml
<activity
    android:name=".HostActivity"
    android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboardHidden"
    ... />
```

有呢行：摺／開時 Activity 唔 recreate，listener 連續收到事件。  
無呢行：系統 recreate 後喺新實例 `onStart` 再 `attach` 即可；唔使 override `onConfigurationChanged`。

### 5. Fragment

```kotlin
override fun onStart() {
    super.onStart()
    foldSense = FoldSense.attach(requireActivity()).addListener { ... }
}
```

注意：多個 Fragment 同時 `attach` 同一個 Activity 會各自註冊一組 `ComponentCallbacks`（各自一個 `FoldSense` 實例）。通常只喺 host Activity 掛一次就夠。

## 方案 B — 源碼 module（完全離線／審核友好）

1. Clone 或 submodule：`https://github.com/cubecnelson/foldsense`
2. `settings.gradle.kts`：

```kotlin
include(":foldsense")
project(":foldsense").projectDir = file("third_party/foldsense/foldsense")
```

3. `implementation(project(":foldsense"))`

你嘅 app 仍然用自己嘅 AGP／Kotlin／AndroidX 版本；`:foldsense` 只要 `minSdk >= 21`、能 compile `Configuration`／API 30 `WindowMetrics` 即可。Library `compileSdk 34` 唔強制 app `targetSdk`。

## 同舊工程共存 checklist

| 檢查 | 期望 |
|------|------|
| 有冇加 `androidx.window` | 無 |
| activity／appcompat 版本 | 維持 catalog 原值 |
| ProGuard | library 自帶 keep `com.cicadahk.foldsense.**`；一般唔使再加 |
| Java 工程 | 用 `FoldSense.attach(activity)` + `FoldListener` 實作類即可 |
| 首次狀態 | `currentState()` = `UNKNOWN`，直至第一次 SW dp 變化 |

## 驗證

1. 跑 repo 內 `:sample`，或喺真摺機／Android Studio Foldable AVD 摺／開。
2. Log `onFoldChanged`：`unfolded`、`smallestWidthDp`、`state`。
3. 可選對照 `WindowMetricsHelper.currentWidthPx`／`FoldSense.isLandscapeBoundsOrFalse`（輔助，唔取代 fold 狀態）。

## 唔好做嘅事

- 唔好為咗 FoldSense 去升 `androidx.window` 或者整份 AndroidX BOM（無必要）。
- 唔好假設 `isLikelyUnfolded()` 喺冷啟動就準——要等 transition，或者自己用業務 baseline SW 呼叫 `FoldSense.detectTransition`。
- 唔好喺 `detach()` 之後重用同一個 listener 集合（已 clear）；要重新 `attach` + `addListener`。

---

## English summary

Add JitPack (`com.github.cubecnelson.foldsense:foldsense:<tag|commit>`) or include the `:foldsense` module. Call `FoldSense.attach(activity)` in `onStart`, `detach()` in `onStop`. No `androidx.window`; your locked activity/appcompat versions stay put. Optional `configChanges` avoids Activity recreation across folds. Full API: [api.md](api.md). Detection model: [fold-detection.md](fold-detection.md).
