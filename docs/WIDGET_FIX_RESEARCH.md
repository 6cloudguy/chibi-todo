# Widget Fix Research Notes

Official Android guidance confirms that a launcher widget consists of provider metadata, an `AppWidgetProvider` registered in the manifest, and RemoteViews XML layouts. A widget configuration activity is a normal activity invoked by the platform launcher, and it must complete its result flow correctly after the widget is placed. [1] [2]

The reported **“app not installed”** result is most plausibly caused by the declared configuration activity being unavailable to the launcher. The revised widget removes the nonessential configuration activity from provider metadata, so placement completes directly from the widget picker. The global next-task preference remains available in the app's Settings screen.

The subsequent HyperOS test confirms that the launcher can display the widget preview but rejects provider binding. The generated application ID and Kotlin provider package match, so the remaining launcher-facing compatibility point is the receiver declaration. The next revision will expose an explicitly fully qualified provider class with `android:exported="true"`; this gives HyperOS's launcher a bindable provider entry while retaining only the standard app-widget update filters.

Android launcher widgets cannot provide freeform drag gestures inside their RemoteViews. The launcher owns drag-and-drop movement of the widget itself; widgets support touch interactions and, where applicable, vertical scrolling. The pet interaction will therefore use an explicit, launcher-safe touch zone that cycles pet pose, energy, and message. Users can still long-press and move the widget using the normal launcher behavior. [3]

## References

[1]: https://developer.android.com/develop/ui/views/appwidgets "Android Developers — Create a simple widget"
[2]: https://developer.android.com/develop/ui/views/appwidgets/configuration "Android Developers — Enable users to configure app widgets"
[3]: https://developer.android.com/develop/ui/views/appwidgets/overview "Android Developers — App widgets overview"
