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

The Android provider reads that same snapshot whenever it receives an update. The launcher widget renders only a small chibi and speech bubble; tapping the chibi cycles mood and message. The optional desktop pet uses the same snapshot, but is a separate application overlay that checks the foreground package through Android Usage Access. It is deliberately shown only while the configured home launcher is foreground. [1] [6]

## Supported Widget Behavior

| Interaction | Result |
|---|---|
| Long-press home screen → **Widgets** → **Momo's Day** | Shows the native **Momo Companion** entry from the standard launcher picker. |
| Place the widget | Adds a compact 1×2 art-first chibi and speech bubble directly to the home screen. |
| Tap the chibi | Cycles `idle → happy → love → sleepy → excited → shy → sad → idle` and refreshes the message. |
| Tap the speech bubble | Opens the main to-do app. |
| Add, complete, delete, or rename a task | The application writes a fresh snapshot and refreshes existing widgets. |
| Long-press the widget | Uses the Android launcher’s grid-based drag mode. A standard widget cannot move freely between home-screen cells or animate continuously inside `RemoteViews`. [2] [4] |

## Optional Launcher-Only Desktop Pet

The **Let Momo walk at home** setting is an opt-in Android overlay, not a widget. It needs two system permissions: **Display over other apps** to draw Momo, and **Usage Access** so the service can determine whether HyperOS's launcher is in front. Turn the switch on, grant each permission when prompted, return to Momo's Day, and turn it on once more.

Momo appears only on the home launcher, begins at the floor, and walks left and right under simple gravity. The pet is bounded by the screen edges; Android does not expose a safe public API for collision detection against individual launcher icons, folders, or wallpaper elements. When the user touches Momo, she changes into a lifted/picked-up state; while being dragged she follows the finger within screen bounds, and returns to the floor after release. Opening any other app hides the overlay within the foreground check interval.

> Android gives the system control of application overlays. HyperOS may stop the companion under battery management, when background pop-up permission is denied, when Usage Access is revoked, or after force stopping the app. Keep the small Momo notification enabled, allow display-over-other-apps and Usage Access, and exempt Momo's Day from battery restrictions if HyperOS offers those options. [5] [6]

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

The widget deliberately avoids direct task completion from the launcher. Android launcher widgets have interaction and `RemoteViews` constraints, so the current interaction set prioritizes reliable state updates over a fragile one-tap task mutation. [1] The launcher-only overlay uses a foreground service, so Android requires a visible service notification, user-granted overlay permission, and Usage Access. It can detect the launcher package but cannot determine the geometry of individual home-screen icons; its collision system therefore uses screen walls and floor boundaries. The app does not use AI or a backend. [5] [6]

## References

[1]: https://developer.android.com/develop/ui/views/appwidgets "Android Developers — Create a simple widget"
[2]: https://developer.android.com/develop/ui/views/appwidgets/overview "Android Developers — App widgets overview"
[3]: https://docs.expo.dev/guides/adopting-prebuild/ "Expo — Adopt Prebuild"
[4]: https://developer.android.com/develop/ui/views/appwidgets/layouts "Android Developers — Provide flexible widget layouts"
[5]: https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY "Android Developers — TYPE_APPLICATION_OVERLAY"
[6]: https://developer.android.com/reference/android/app/usage/UsageStatsManager "Android Developers — UsageStatsManager"
