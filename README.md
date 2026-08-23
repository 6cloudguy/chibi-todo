# Momo's Day

Momo's Day is a local-first **Expo + React Native + TypeScript** to-do app with a real Android home-screen companion widget. The app is intentionally a simple, cozy productivity tool: tasks live in the main application, while Momo lives on the Android home screen as a native widget. There is no chatbot, AI service, account requirement, or backend dependency in the MVP.

> **Experience:** open the app for a useful daily list; return to the home screen for a small, expressive chibi companion.

## Included Features

| Area | Delivered behavior |
|---|---|
| To-do app | Create, complete, delete, and persist tasks locally. The Today screen separates active and completed tasks and shows real progress. |
| Companion | Seven mood states, non-repeating supportive messages, companion mood selection, and replaceable PNG artwork. |
| Personalization | Local-only companion and nickname fields, color mood choice, personality choice, custom messages, and a next-task widget preference. |
| Android widget | Standard launcher-picker discovery, direct placement, compact and expanded layouts, chibi-tap mood changes, a dedicated pet-play action, app-open message action, resize support, preview, and app-widget updates. |
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
| Local Android build | `pnpm exec expo prebuild --clean --platform android` then `pnpm exec expo run:android --device` | You have Android Studio, the Android SDK, and an Android device or emulator. |
| EAS cloud development build | `npx eas-cli@latest login` then `npx eas-cli@latest build --platform android --profile development` | You want Expo's build service to produce an installable development APK. |
| Internal APK | `npx eas-cli@latest build --platform android --profile preview` | You want an installable APK without the development-client workflow. |

The `expo-dev-client` dependency and `eas.json` development and preview profiles are already included. After installing a development build, start the server with `pnpm exec expo start` and open the development client on the device. Rebuild native Android when changing app configuration, native widget code, native dependencies, or widget PNGs; ordinary TypeScript-only edits do not need an APK rebuild. [1] [2]

## Install an APK on an Android Phone

For a local build, connect the Android phone with USB debugging enabled and use:

```sh
pnpm exec expo run:android --device
```

For an EAS build, open the completed build's download link or scan its installation QR code on the Android device. If Android asks, allow installation from the browser or file manager used to open the APK. The resulting install is the native application needed for the widget; do not test widget placement in Expo Go. [1]

## Add the Chibi Widget

After installing the APK, go to the Android home screen. Long-press an empty area, open **Widgets**, find **Momo's Day**, and place **Momo Companion**. The widget now places directly from the picker. Tap Momo to change expression and message, tap **pet Momo** for small companion reactions, and tap the message to open the to-do app. Long-press the placed widget to use Android's standard launcher drag mode and move it around your home screen.

> **Important:** The direct-placement fix is native Android code. Build and install a fresh APK after this change; the earlier APK will retain its old widget metadata. If your launcher still lists an old entry, remove its existing widget, uninstall the older Momo's Day build, then install the newly built APK before adding the widget again.

Android launcher widgets are intended to present concise, glanceable content and can be resized by the user when the provider supports it. Momo's Day declares horizontal and vertical resizing and selects an expanded layout as more horizontal room becomes available. [3]

## Replace Chibi Assets

The mood artwork is stored in `assets/chibi/`. Replace `idle.png`, `happy.png`, `love.png`, `sleepy.png`, `excited.png`, `shy.png`, and `sad.png` with square transparent PNGs that retain these names. Then regenerate native resources and build a new APK:

```sh
pnpm exec expo prebuild --clean --platform android
pnpm exec expo run:android --device
```

The current launcher artwork is copied to `assets/images/icon.png`, `splash-icon.png`, `favicon.png`, and `android-icon-foreground.png`. Its display configuration is in `app.config.ts`.

## Architecture Notes

The main application uses `AsyncStorage` for detailed local tasks and settings. The widget receives only a compact, normalized snapshot via the `ChibiWidgetBridge` native module and Android shared preferences. This keeps the launcher widget independent of an open React Native process while allowing it to update immediately when the application changes data. See [the detailed native widget guide](docs/ANDROID_WIDGET.md) for source locations and the exact data flow.

## Validation and Known Limits

`pnpm check` validates the TypeScript project. `pnpm test` runs deterministic tests for mood cycling, message selection, and widget snapshot construction. Android prebuild has been run successfully and generated the widget declarations, layouts, provider, Kotlin bridge, and direct-placement widget metadata.

This environment does not include an Android SDK, so a local Gradle APK compilation could not be completed here. Build the project on a machine with Android Studio/Android SDK installed or use the EAS cloud build path above. The widget does not support marking a task complete directly from the launcher in this MVP; tasks are mutated in the app, and the widget reflects the resulting state after sync.

## References

[1]: https://docs.expo.dev/develop/development-builds/introduction/ "Expo — Introduction to development builds"
[2]: https://docs.expo.dev/guides/adopting-prebuild/ "Expo — Adopt Prebuild"
[3]: https://developer.android.com/develop/ui/views/appwidgets/overview "Android Developers — App widgets overview"
