# Momo's Day

Momo's Day is a local-first **Expo + React Native + TypeScript** to-do app with a real Android home-screen companion widget. The app is intentionally a simple, cozy productivity tool: tasks live in the main application, while Momo lives on the Android home screen as a native widget. There is no chatbot, AI service, account requirement, or backend dependency in the MVP.

> **Experience:** open the app for a useful daily list; return to the home screen for a small, expressive chibi companion.

## Included Features

| Area | Delivered behavior |
|---|---|
| To-do app | Create, complete, delete, and persist tasks locally. The Today screen separates active and completed tasks and shows real progress. |
| Chibi system | Seven mood states, non-repeating supportive messages, replaceable PNG artwork, and companion reactions through the widget or desktop pet. |
| Personalization | Local-only color mood choice, personality choice, custom messages, a next-task widget preference, and desktop-pet controls. |
| Android widget | Standard launcher-picker discovery, direct placement, compact chibi-and-speech-bubble presentation, chibi-tap mood changes, preview, and app-widget updates. |
| Desktop pet | Optional user-granted Android overlay that docks at bottom centre on unlock, can be dragged freely over the launcher, and reacts with artwork swaps and speech bubbles. |
| Native integration | Config plugin plus native Kotlin bridge keep Android widget code isolated from the React Native UI while preserving Expo prebuild workflow. |

## Run the JavaScript App

Install dependencies and start the Expo development server from the project root:

```sh
pnpm install
pnpm dev
```

The web preview is useful for reviewing the React Native UI. It cannot host or test the Android launcher widget because that widget is native Android functionality.

## Create an Android Development Build

Expo Go cannot include this project's custom Kotlin widget provider. A development build is the app's own native runtime and supports custom native code, so use it for widget testing. [1]

| Approach | Commands | When to use it |
|---|---|---|
| Local Android build | `pnpm android` | Regenerates the native Android project and installs a fresh build, ensuring native widget changes are included. |
| EAS cloud development build | `npx eas-cli@latest login` then `npx eas-cli@latest build --platform android --profile development` | You want Expo's build service to produce an installable development APK. |
| Internal APK | `npx eas-cli@latest build --platform android --profile preview` | You want an installable APK without the development-client workflow. |

The `expo-dev-client` dependency and `eas.json` development and preview profiles are already included. After installing a development build, start the server with `pnpm exec expo start` and open the development client on the device. Rebuild native Android when changing app configuration, native widget code, native dependencies, or widget PNGs; ordinary TypeScript-only edits do not need an APK rebuild. [1] [2]

## Install an APK on an Android Phone

For a local build, connect the Android phone with USB debugging enabled and use:

```sh
pnpm android
```

For an EAS build, open the completed build's download link or scan its installation QR code on the Android device. If Android asks, allow installation from the browser or file manager used to open the APK. The resulting install is the native application needed for the widget; do not test widget placement in Expo Go. [1]

## Add the Chibi Widget

After installing the APK, go to the Android home screen. Long-press an empty area, open **Widgets**, find **Momo's Day**, and place **Momo Companion**. The widget now uses a small chibi-and-speech-bubble presentation rather than a large task card. Tap Momo to change expression and message; tap the bubble to open the app. The launcher still controls the widget's grid position.

> **Important:** The direct-placement and HyperOS receiver fixes are native Android code. `pnpm android` now runs a clean Expo prebuild before compiling, so the config-plugin changes are included in the new APK. Do not use `pnpm android:fast` for this fix; it reuses the current native folder. If HyperOS still lists an old entry, remove its existing widget, uninstall the older Momo's Day build, then install the newly built APK before adding the widget again.

## Turn On the Cross-App Explorer Pet

The explorer companion is intentionally separate from the launcher widget. Open **Settings** in Momo's Day, find **Desktop Pet · Android**, and turn on **Let Momo explore**. HyperOS will request only **Display over other apps**. Grant it, return to Momo's Day, and turn the setting on once more.

Momo uses a tight image-sized touch window instead of the previous oversized invisible rectangle. She can float visually above your apps, pauses to rest, takes short walks, climbs at screen edges, falls back to the floor, and uses dedicated motion poses for walking, climbing, falling, rest, and being picked up. Dragging shows the picked-up pose and release begins a gentle fall. Android does not expose the private geometry of app icons or other-app controls, so collisions are with the screen walls and floor rather than individual icons.

> The explorer pet uses an Android overlay service, so a small persistent notification is expected. HyperOS can stop overlays in aggressive battery modes. If Momo disappears, allow background pop-ups where offered, remove battery restrictions for Momo's Day, and re-enable **Let Momo explore**. Standard widgets cannot freely walk between launcher grid cells or run continuous animations; the optional overlay is the supported solution for that behavior. [3] [4]

## Replace Chibi Assets

The mood artwork is stored in `assets/chibi/`. Replace `idle.png`, `happy.png`, `love.png`, `sleepy.png`, `excited.png`, `shy.png`, and `sad.png` with square transparent PNGs that retain these names. Then regenerate native resources and build a new APK:

```sh
pnpm android
```

The current launcher artwork is copied to `assets/images/icon.png`, `splash-icon.png`, `favicon.png`, and `android-icon-foreground.png`. Its display configuration is in `app.config.ts`.

## Architecture Notes

The main application uses `AsyncStorage` for detailed local tasks and settings. The widget receives only a compact, normalized snapshot via the `ChibiWidgetBridge` native module and Android shared preferences. This keeps the launcher widget independent of an open React Native process while allowing it to update immediately when the application changes data. See [the detailed native widget guide](docs/ANDROID_WIDGET.md) for source locations and the exact data flow.

## Validation and Known Limits

`pnpm check` validates the TypeScript project. `pnpm test` runs deterministic tests for mood cycling, message selection, and widget snapshot construction. Android prebuild has been run successfully and generated the compact widget, overlay service, permissions, native bridge, and launcher-facing metadata.

The add-task sheet follows direct keyboard-height events and uses Android's `pan` soft-input mode, keeping the task message field and save action visible above the keyboard. [6]

This environment does not include an Android SDK, so a local Gradle APK compilation could not be completed here. Build the project on a machine with Android Studio/Android SDK installed or use the EAS cloud build path above. The widget does not support marking a task complete directly from the launcher in this MVP; tasks are mutated in the app, and the widget reflects the resulting state after sync.

## References

[1]: https://docs.expo.dev/develop/development-builds/introduction/ "Expo — Introduction to development builds"
[2]: https://docs.expo.dev/guides/adopting-prebuild/ "Expo — Adopt Prebuild"
[3]: https://developer.android.com/develop/ui/views/appwidgets/overview "Android Developers — App widgets overview"
[4]: https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY "Android Developers — TYPE_APPLICATION_OVERLAY"
[5]: https://docs.expo.dev/guides/keyboard-handling/ "Expo — Keyboard handling"
