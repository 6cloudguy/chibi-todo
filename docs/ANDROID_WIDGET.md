# Android Widget Implementation

The Android widget is a **native launcher widget**, not an in-app imitation. Android exposes it through the normal widget picker because the project generates an `AppWidgetProvider` receiver, widget provider metadata, and RemoteViews layouts during Expo prebuild. Android widgets are launcher-hosted views rather than regular React Native screens, which is why this feature requires a development build or a release build rather than Expo Go. [1] [2]

## Native Structure

| Project location | Responsibility |
|---|---|
| `plugins/with-chibi-widget.js` | Adds the widget receiver to the generated Android manifest, copies resources, and registers the React Native bridge package during prebuild. |
| `native-widget/android/kotlin/ChibiWidgetProvider.kt` | Native `AppWidgetProvider` that selects a compact or expanded RemoteViews layout, renders the saved snapshot, opens the app from its message, and handles mood and pet-play taps. |
| `native-widget/android/kotlin/ChibiWidgetModule.kt` | React Native bridge that stores the normalized snapshot in shared preferences and asks Android to refresh widget instances. |
| `native-widget/android/res/` | Compact and expanded layouts, provider metadata, rounded background, and picker preview. |
| `assets/chibi/` | Replaceable mood artwork used by both the Expo UI and the generated Android resources. |

## App ↔ Widget Data Flow

The app owns the detailed task list, settings, mood, and messages in local `AsyncStorage`. After every state change, `TaskProvider` creates a small snapshot containing the current mood, message, next incomplete task, completion counts, the task-display preference, and companion name. The native bridge serializes this snapshot into Android `SharedPreferences`, then refreshes every placed widget.

The Android provider reads that same snapshot whenever it receives an update or resize event. Tapping the chibi cycles mood; tapping the **pet Momo** control advances a small play scene, including a new pose, energy count, and message. Both taps update the native snapshot immediately; when the app opens, it reconciles that most recent native mood and message before sending its next snapshot. Tapping the message launches the app. This uses the provider, metadata XML, and RemoteViews layout pattern described by Android's widget documentation. [1]

## Supported Widget Behavior

| Interaction | Result |
|---|---|
| Long-press home screen → **Widgets** → **Momo's Day** | Shows the native **Momo Companion** entry from the standard launcher picker. |
| Place the widget | Adds directly to the home screen; the global next-task preference remains in the app's Settings screen. |
| Tap the chibi | Cycles `idle → happy → love → sleepy → excited → shy → sad → idle` and refreshes the message. |
| Tap **pet Momo** | Lets the companion react in a short loop: wiggle, zoom, bring a heart, then cozy rest. |
| Tap the message | Opens the main to-do app. |
| Add, complete, delete, or rename a task | The application writes a fresh snapshot and refreshes existing widgets. |
| Resize the widget | The provider switches between compact and expanded XML layouts based on the launcher-reported width. |
| Long-press the widget | Uses the Android launcher’s own drag mode to move Momo around the home screen. Freeform drag gestures inside the widget are not available to standard RemoteViews widgets. [2] |

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

The widget deliberately avoids direct task completion from the launcher. Android launcher widgets have interaction and RemoteViews constraints, so the current interaction set prioritizes reliable state updates and a predictable app-open action over a fragile one-tap task mutation. [1] The widget does not use AI, requires no backend, and does not send notifications by default.

## References

[1]: https://developer.android.com/develop/ui/views/appwidgets "Android Developers — Create a simple widget"
[2]: https://developer.android.com/develop/ui/views/appwidgets/overview "Android Developers — App widgets overview"
[3]: https://docs.expo.dev/guides/adopting-prebuild/ "Expo — Adopt Prebuild"
