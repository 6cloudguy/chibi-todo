# Widget Fix Research Notes

Official Android guidance confirms that a launcher widget consists of provider metadata, an `AppWidgetProvider` registered in the manifest, and RemoteViews XML layouts. A widget configuration activity is a normal activity invoked by the platform launcher, and it must complete its result flow correctly after the widget is placed. [1] [2]

The reported **“app not installed”** result is most plausibly caused by the declared configuration activity being unavailable to the launcher. The revised widget removes the nonessential configuration activity from provider metadata, so placement completes directly from the widget picker. The global next-task preference remains available in the app's Settings screen.

The subsequent HyperOS test confirms that the launcher can display the widget preview but rejects provider binding. The generated application ID and Kotlin provider package match, so the remaining launcher-facing compatibility point is the receiver declaration. The next revision will expose an explicitly fully qualified provider class with `android:exported="true"`; this gives HyperOS's launcher a bindable provider entry while retaining only the standard app-widget update filters.

Android launcher widgets cannot provide freeform drag gestures inside their RemoteViews. The launcher owns drag-and-drop movement of the widget itself; widgets support touch interactions and, where applicable, vertical scrolling. The pet interaction will therefore use an explicit, launcher-safe touch zone that cycles pet pose, energy, and message. Users can still long-press and move the widget using the normal launcher behavior. [3]

Android widget size is controlled by the launcher grid through provider sizing metadata. An art-first widget can request a small 1×1 target cell and render a transparent-looking image and small speech bubble, but it cannot choose a free pixel position, walk outside its allocated cell, or run continuous animation inside `RemoteViews`. [4]

A true free-roaming desktop pet is architecturally different from a widget: it requires an application overlay window. Android documents `TYPE_APPLICATION_OVERLAY` as requiring `SYSTEM_ALERT_WINDOW`; the system can move, resize, or hide overlays and adjusts the host process importance. Such an overlay needs a user-granted special permission and is less reliable on aggressive device-specific background managers, including HyperOS. The widget redesign will therefore deliver the safe art-first companion now, with an overlay pet treated as an optional, clearly permissioned later feature. [5]

Keeping an overlay visible only while the launcher is foreground requires observing foreground-app usage rather than relying on normal activity callbacks. `UsageStatsManager` exposes usage events but access is a separate user-granted Usage Access setting; this is the least-privileged available approach for a launcher-only overlay and will be optional. Without that consent, the desktop pet must remain disabled rather than risk appearing above other apps. [6]

Expo documents `Keyboard` events and `KeyboardAvoidingView` as the basic tools for keyboard-safe layouts. A custom sheet can react directly to keyboard show/change events and animate its bottom offset using the reported keyboard height, avoiding reliance on modal resize behavior that varies by Android device. [7]

## References

[1]: https://developer.android.com/develop/ui/views/appwidgets "Android Developers — Create a simple widget"
[2]: https://developer.android.com/develop/ui/views/appwidgets/configuration "Android Developers — Enable users to configure app widgets"
[3]: https://developer.android.com/develop/ui/views/appwidgets/overview "Android Developers — App widgets overview"
[4]: https://developer.android.com/develop/ui/views/appwidgets/layouts "Android Developers — Provide flexible widget layouts"
[5]: https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY "Android Developers — TYPE_APPLICATION_OVERLAY"
[6]: https://developer.android.com/reference/android/app/usage/UsageStatsManager "Android Developers — UsageStatsManager"
[7]: https://docs.expo.dev/guides/keyboard-handling/ "Expo — Keyboard handling"
