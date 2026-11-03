# FoldSense API reference

Package: `com.cicadahk.foldsense`  
Version constant: `FoldSense.VERSION` = `"1.0.0"`  
Source of truth: `foldsense/src/main/java/com/cicadahk/foldsense/*.kt`

---

## `FoldSense`

Main entry. Constructor is **private**. Create with `FoldSense.attach(activity)`.

Implements `android.content.ComponentCallbacks` (internal); consumers do not call `onConfigurationChanged` themselves.

### Factory / companion

| Member | Signature | Description |
|--------|-----------|-------------|
| `VERSION` | `const val VERSION: String` | `"1.0.0"` |
| `attach` | `@JvmStatic fun attach(activity: Activity): FoldSense` | Builds instance and calls instance `attach()` |
| `detectTransition` | `@JvmStatic fun detectTransition(previousSwDp: Int, currentSwDp: Int): Boolean?` | `true` if SW grew (likely unfold), `false` if shrank, `null` if unchanged or `previousSwDp <= 0` |
| `isLandscapeBoundsOrFalse` | `@JvmStatic fun isLandscapeBoundsOrFalse(activity: Activity): Boolean` | API 30+: delegates to `WindowMetricsHelper.isLandscapeBounds`; else `false` |

### Instance — lifecycle

| Method | Returns | Description |
|--------|---------|-------------|
| `attach()` | `FoldSense` | Registers `ComponentCallbacks` on the Activity; snapshots current `smallestScreenWidthDp`; sets `state` to `UNKNOWN`. Idempotent if already attached. If Activity was GC’d from the `WeakReference`, returns `this` without attaching. |
| `detach()` | `Unit` | Unregisters callbacks (if attached), sets `attached = false`, **clears all listeners**. Safe if never attached (still clears listeners). |

### Instance — listeners

| Method | Returns | Description |
|--------|---------|-------------|
| `addListener(listener: FoldListener)` | `FoldSense` | Adds to a `CopyOnWriteArraySet`; chainable |
| `removeListener(listener: FoldListener)` | `FoldSense` | Removes one listener; chainable |

### Instance — state

| Method | Returns | Description |
|--------|---------|-------------|
| `currentState()` | `FoldState` | Last inferred state; `UNKNOWN` until first SW delta after attach |
| `currentSmallestWidthDp()` | `Int` | Live `resources.configuration.smallestScreenWidthDp`, or last cached value if Activity ref is gone |
| `isLikelyUnfolded()` | `Boolean` | `state == FoldState.UNFOLDED` only |

### Behaviour notes

- On SW increase: `state = UNFOLDED`, notifies listeners with `unfolded = true`.
- On SW decrease: `state = FOLDED`, notifies with `unfolded = false`.
- If `previous <= 0` or `sw == previous`, updates cache only; **no** listener call.
- `detach()` clears listeners — re-`addListener` after a new `attach` if needed.

### Typical Kotlin usage

```kotlin
val sense = FoldSense.attach(activity)
sense.addListener { unfolded, swDp, state -> /* ... */ }
sense.isLikelyUnfolded()
sense.currentState()
sense.currentSmallestWidthDp()
sense.detach()
```

### Typical Java usage

```java
FoldSense sense = FoldSense.attach(activity);
sense.addListener((unfolded, smallestWidthDp, state) -> {
    // ...
});
boolean open = sense.isLikelyUnfolded();
sense.detach();
```

---

## `FoldListener`

```kotlin
fun interface FoldListener {
    fun onFoldChanged(unfolded: Boolean, smallestWidthDp: Int, state: FoldState)
}
```

| Parameter | Meaning |
|-----------|---------|
| `unfolded` | `true` when current SW dp **>** previous (typical unfold); `false` when **<** (typical fold) |
| `smallestWidthDp` | New `Configuration.smallestScreenWidthDp` |
| `state` | `FoldState` after this transition (`UNFOLDED` or `FOLDED`) |

Kotlin SAM: lambda works with `addListener { ... }`.

---

## `FoldState`

```kotlin
enum class FoldState {
    FOLDED,    // last transition decreased SW dp
    UNFOLDED,  // last transition increased SW dp
    UNKNOWN    // attached but no transition observed yet
}
```

Not a hinge posture enum. See [fold-detection.md](fold-detection.md).

---

## `WindowMetricsHelper`

`object` — optional helpers; **still no androidx.window**.

| Method | API | Description |
|--------|-----|-------------|
| `isLandscapeBounds(activity)` | `@RequiresApi(30)` / `@JvmStatic` | `currentWindowMetrics.bounds` width > height |
| `currentWidthPx(activity)` | `@JvmStatic` | API 30+: metrics bounds width; else deprecated `defaultDisplay.width` |
| `currentHeightPx(activity)` | `@JvmStatic` | API 30+: metrics bounds height; else deprecated `defaultDisplay.height` |

Prefer `FoldSense.isLandscapeBoundsOrFalse` when you want a single call that never throws on old APIs.

**Note:** KDoc on `currentWidthPx` historically mentioned “`-1` if API < 30”; the **implementation** falls back to `Display` size instead. Documented here as implemented.

---

## Dependencies (library module)

From `foldsense/build.gradle.kts`:

- `compileOnly("androidx.annotation:annotation:1.7.1")` — not required at app runtime for FoldSense APIs to work
- `testImplementation("junit:junit:4.13.2")` — library unit tests only
- **No** `androidx.window`

Publishing coordinates (Maven publication inside module):  
`groupId` `com.github.cubecnelson`, `artifactId` `foldsense`, `version` `1.0.0`  
JitPack consumer coordinate for multi-module repo:  
`com.github.cubecnelson.foldsense:foldsense:<tag|commit>`

---

## 繁中速查

| 你想做 | 呼叫 |
|--------|------|
| 開始聽 | `FoldSense.attach(activity)` |
| 加回調 | `addListener { unfolded, sw, state -> }` |
| 而家展開咗未 | `isLikelyUnfolded()` / `currentState()` |
| 停止 | `detach()`（會清 listeners） |
| 自己比兩個 SW | `FoldSense.detectTransition(prev, curr)` |
| Window 闊高 | `WindowMetricsHelper.currentWidthPx` 等 |
