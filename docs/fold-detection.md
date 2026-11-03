# How FoldSense detection works

繁中（香港）為主 + English summary.

## 一句講晒

FoldSense **唔讀鉸鏈**。佢記住 Activity 嘅 `Configuration.smallestScreenWidthDp`，下次 configuration 變嗰陣，若果呢個值 **變大 → 當 unfold**，**變細 → 當 fold**。

## 點解用 `smallestScreenWidthDp`？

Android 用 `smallestScreenWidthDp`（常寫 **sw** dp）表示「唔理橫豎，最短嗰邊有幾多 dp」。多數摺機由摺到開，可用闊度明顯增加，系統會更新呢個值並派發 configuration change。

相對：

| 做法 | 依賴 | FoldSense |
|------|------|-----------|
| `androidx.window` `FoldingFeature` | 要加 Jetpack window | ❌ 刻意唔用 |
| 自己 override `onConfigurationChanged` | 要改 Activity | ⭕ 唔使；用 `registerComponentCallbacks` |
| `WindowMetrics` bounds | API 30+ platform | ⭕ 只做輔助，唔驅動 fold state |

## 生命週期同事件流

```text
FoldSense.attach(activity)
        │
        ├─ lastSmallestWidthDp = activity.resources.configuration.smallestScreenWidthDp
        ├─ state = UNKNOWN
        └─ activity.registerComponentCallbacks(this)

… later, system configuration change …

ComponentCallbacks.onConfigurationChanged(newConfig)
        │
        ├─ sw = newConfig.smallestScreenWidthDp
        ├─ if last > 0 && sw != last:
        │       unfolded = (sw > last)
        │       state = UNFOLDED or FOLDED
        │       notify FoldListener.onFoldChanged(unfolded, sw, state)
        └─ else: only update lastSmallestWidthDp
```

`detach()` → `unregisterComponentCallbacks` + `listeners.clear()`。

Activity 用 `WeakReference` 持有，避免 library 單獨 hold 住 leak；但你仍應喺 `onStop`／`onDestroy` 呼叫 `detach()`。

## 狀態機（實際行為）

```text
          attach()
             │
             v
         UNKNOWN ──────────────────────────────┐
             │                                 │
             │  sw increases                   │  sw decreases
             v                                 v
         UNFOLDED  <──────────────────────>  FOLDED
             │         (any later delta)       │
             └─────────────────────────────────┘
```

- **`UNKNOWN`**：剛 `attach`，或者未見過有效 transition。
- **`isLikelyUnfolded()`**：純粹 `state == UNFOLDED`。冷啟動未摺過就會係 `false`（因為仲係 `UNKNOWN`）。
- 第一次有效 delta 先會 notify listeners。

## `detectTransition`（無 Activity）

```kotlin
FoldSense.detectTransition(previousSwDp, currentSwDp): Boolean?
```

同內部判斷一致：

- `previousSwDp <= 0` → `null`
- 相等 → `null`
- `current > previous` → `true`（unfold）
- 否則 → `false`（fold）

方便你自己喺 `onCreate` 用 SharedPreferences／ViewModel 存嘅 baseline 做一次性比較。

## WindowMetrics 輔助（唔驅動 FoldState）

`WindowMetricsHelper`／`FoldSense.isLandscapeBoundsOrFalse` 讀而家 window 像素闊高，或者「係唔係橫向 bounds」。

用途：同 fold 狀態一齊做產品邏輯（例如展開 + 橫向 → 雙欄）。  
**唔會**自動改 `FoldState`。

## Manifest `configChanges` 關係

| Activity 設定 | 系統行為 | FoldSense |
|---------------|----------|-----------|
| 有 `smallestScreenSize` 等喺 `configChanges` | 唔 recreate，派 `onConfigurationChanged` | `ComponentCallbacks` 收到 → listener 觸發 |
| 無（預設） | 可能 destroy／create Activity | 舊實例應 `detach`；新實例重新 `attach`；中間唔會自動保留 `FoldState` |

唔使喺 Activity 覆寫 `onConfigurationChanged` 再手動轉發。

## 誤報／漏報（務必知）

**可能當 fold/unfold 嘅其他情況**

- 分屏／自由視窗改變可用闊度  
- 外接螢幕／投屏  
- 部分翻蓋機內外屏切換  
- 少見：字體／密度／顯示大小令 sw dp 變  

**偵測唔到嘅**

- 鉸鏈角度（半開、桌面模式）而未令 sw dp 變  
- 純 `FoldingFeature` occlusion／separating 狀態  

若產品一定要 hinge／Flex Mode，先至考慮加 `androidx.window`（會同「鎖死 catalog」目標衝突）。

## 線程

`onFoldChanged` 跟住 configuration callback 嘅線程（通常係 main，但唔應假設所有 OEM 一致）。更新 View 時用 `runOnUiThread`／主線程 dispatcher 最穩（見 `:sample`）。

## 同官方 API 對照（English）

FoldSense approximates posture from **smallest-width configuration deltas**. Google’s Jetpack WindowManager exposes **hinge and folding features**. Use FoldSense when dependency policy forbids `androidx.window`; switch to WindowManager when you need true folding features and can accept the dependency.

### Algorithm (English)

1. On `attach`, store `smallestScreenWidthDp`, set `UNKNOWN`.  
2. On each `ComponentCallbacks.onConfigurationChanged`, compare new SW to last.  
3. Increase → `UNFOLDED` + `unfolded=true`; decrease → `FOLDED` + `unfolded=false`.  
4. Notify all `FoldListener`s; ignore non-changes.
