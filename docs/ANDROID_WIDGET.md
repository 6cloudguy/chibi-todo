# Android Widget Implementation

The Android widget is a **native launcher widget**, not an in-app imitation. Android exposes it through the normal widget picker because the project generates an explicitly launcher-bindable `AppWidgetProvider` receiver, widget provider metadata, and RemoteViews layouts during Expo prebuild. Android widgets are launcher-hosted views rather than regular React Native screens, which is why this feature requires a development build or a release build rather than Expo Go. [1] [2]

## Native Structure

| Project location | Responsibility |
|---|---|
| `plugins/with-chibi-widget.js` | Adds the widget receiver and optional user-started overlay service to the generated Android manifest, copies resources, and registers the React Native bridge package during prebuild. |
| `native-widget/android/kotlin/ChibiWidgetProvider.kt` | Native `AppWidgetProvider` that renders a compact chibi and speech bubble, then cycles the chibi's mood when it is tapped. |
| `native-widget/android/kotlin/MomoOverlayService.kt` | User-started foreground overlay service for the optional draggable desktop pet, bottom-centre unlock docking, and animation-like scene changes. |
| `native-widget/android/kotlin/ChibiWidgetModule.kt` | React Native bridge that stores the normalized snapshot, refreshes the widget, and opens or controls the optional desktop pet. |
| `native-widget/android/res/` | Compact and expanded layouts, provider metadata, rounded background, and picker preview. |
| `assets/chibi/` | Replaceable mood artwork used by both the Expo UI and the generated Android resources. |

## App ↔ Widget Data Flow

The app owns the detailed task list, settings, mood, and messages in local `AsyncStorage`. After every state change, `TaskProvider` creates a small snapshot containing the current mood, message, next incomplete task, completion counts, the task-display preference, and companion name. The native bridge serializes this snapshot into Android `SharedPreferences`, then refreshes every placed widget.

The Android provider reads that same snapshot whenever it receives an update. The launcher widget renders only a small chibi and speech bubble; tapping the chibi cycles mood and message. The optional desktop pet uses the same snapshot, but is a separate application overlay that can be dragged and tapped for scene changes. Both update the native snapshot immediately; when the app opens, it reconciles the newest mood and message before sending its next snapshot. [1]

## Supported Widget Behavior

| Interaction | Result |
|---|---|
| Long-press home screen → **Widgets** → **Momo's Day** | Shows the native **Momo Companion** entry from the standard launcher picker. |
| Place the widget | Adds a compact 1×2 art-first chibi and speech bubble directly to the home screen. |
| Tap the chibi | Cycles `idle → happy → love → sleepy → excited → shy → sad → idle` and refreshes the message. |
| Tap the speech bubble | Opens the main to-do app. |
| Add, complete, delete, or rename a task | The application writes a fresh snapshot and refreshes existing widgets. |
| Long-press the widget | Uses the Android launcher’s grid-based drag mode. A standard widget cannot move freely between home-screen cells or animate continuously inside `RemoteViews`. [2] [4] |

## Optional Free-Roaming Desktop Pet

The **Let Momo roam** setting is an opt-in Android overlay, not a widget. In Settings, turn it on, grant the system's **Display over other apps** permission, then return to Momo's Day and turn it on again. The overlay opens at the bottom centre, reacts to taps with new artwork and speech bubbles, and can be dragged freely around the home screen or supported apps. While it remains enabled, the service listens for a user-unlock event and docks Momo back at bottom centre.

> Android gives the system control of application overlays. HyperOS may stop the companion under battery management, when background pop-up permission is denied, or after force stopping the app. Keep the small Momo notification enabled, allow display-over-other-apps, and exempt Momo's Day from battery restrictions if HyperOS offers those options. [5]

## Replacing Chibi Artwork

Use these exact file names, each as a square PNG with transparent background. The Expo app and the Android prebuild plugin both depend on this set.

| Mood | Asset path |
|---|---|
| Idle | `assets/chibi/idle.png` |
| Happy | `assets/chibi/happy.png` |
| Love | `assets/chibi/love.png` |
| Sleepy | `assets/chibi/sleepy.png` |
| Excited | `assets/chibi/excited.png` |
| Shy | `assets/chibi/shy.png` |
| Sad | `assets/chibi/sad.png` |

After replacing any PNG, rebuild native Android resources with `pnpm exec expo prebuild --clean --platform android` and then create a new development or release build. Native build code is regenerated from `app.config.ts` and the local config plugin, so direct edits under `android/` are intentionally not the source of truth. [3]

## Deliberate MVP Limits

The widget deliberately avoids direct task completion from the launcher. Android launcher widgets have interaction and `RemoteViews` constraints, so the current interaction set prioritizes reliable state updates over a fragile one-tap task mutation. [1] The free-roaming companion uses a foreground overlay service, so Android requires a visible service notification and user-granted overlay permission. The app does not use AI or a backend. [5]

## References

[1]: https://developer.android.com/develop/ui/views/appwidgets "Android Developers — Create a simple widget"
[2]: https://developer.android.com/develop/ui/views/appwidgets/overview "Android Developers — App widgets overview"
[3]: https://docs.expo.dev/guides/adopting-prebuild/ "Expo — Adopt Prebuild"
[4]: https://developer.android.com/develop/ui/views/appwidgets/layouts "Android Developers — Provide flexible widget layouts"
[5]: https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY "Android Developers — TYPE_APPLICATION_OVERLAY"
