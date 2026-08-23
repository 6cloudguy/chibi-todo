# Chibi Todo Widget — Mobile Interface Design

## Product Direction

Chibi Todo Widget is a **one-handed, portrait-first (9:16)** daily planning companion. The application remains a calm, useful to-do list; the expressive chibi is reserved for the companion panel and the Android home-screen widget. The interface follows familiar iOS-style spacing, large tap targets, plain-language labels, sheets for configuration, and a single clearly visible primary action.

## Screen List

| Screen | Primary content and functionality | Mobile layout |
|---|---|---|
| Today | Greeting, daily progress, active task list, completed task list, add button, and a compact chibi encouragement card. | Scrollable single column with the add action fixed at the lower right above the tab bar. |
| Add task sheet | Task name, optional note, and create action. | Bottom sheet with keyboard-safe controls and a single confirmation action. |
| Companion | Larger chibi artwork, current mood, a fresh message, an expression-cycle action, and widget guidance. | Centered illustration with an accessible mood selector below it. |
| Settings | Girlfriend nickname, chibi name, favorite color theme, task behavior, custom messages, and personality style. | Grouped settings list with inline controls and dedicated edit sheets where needed. |
| Widget configuration activity | Mood/theme preference, whether the next task is shown, and message style for a newly placed widget. | Native Android activity that uses the same visual vocabulary, kept intentionally simple for reliable placement. |

## Key User Flows

The primary flow begins on **Today**, where the user taps the circular add button, enters a task in the bottom sheet, and returns immediately to the list. Tapping a task circle marks it complete with restrained haptic feedback, updates the progress display, and synchronizes a compact snapshot to the home-screen widget. A secondary flow opens **Companion**, where tapping the chibi cycles mood and chooses a non-repeating encouragement message; the state is persisted and mirrored to the widget.

The personalization flow begins in **Settings**. The user can name the companion, choose a theme, modify custom messages, and select whether task details appear on the widget. Widget placement is handled through Android's normal widget picker. When the widget is added, a compact native configuration activity stores the selected display preference, and the widget works without the main app remaining open.

## Color Choices

The visual language is built around a warm, romantic stationery palette rather than generic productivity colors. The light theme uses **Milk Tea** `#FFF9F5` as the canvas, **Strawberry Milk** `#F4A6AF` for the main action, **Rose Ink** `#4C2D38` for legible text, **Blush Card** `#FFF0F2` for grouped surfaces, **Lavender Mist** `#EDE7FA` as a soft secondary wash, and **Mint Tick** `#9ACBB7` for completion. The dark theme uses **Plum Night** `#241B24` as the canvas and preserves gentle pink and lavender highlights without reducing contrast.

## Native Widget Architecture

The Android widget is a real `AppWidgetProvider` hosted by the device launcher. A small native bridge accepts a normalized widget snapshot from the React Native app and writes it to Android `SharedPreferences`. The provider reads this snapshot, renders native `RemoteViews` layouts for compact and expanded sizes, responds to PendingIntent taps, and refreshes all widget instances after task, mood, message, or setting changes. The widget declaration, preview resources, and receiver registration are added by an Expo config plugin so Android-specific code remains grouped in the native-widget module.

| Boundary | Responsibility |
|---|---|
| React Native | Owns local task/settings state, messages, chibi state, and initiates widget refreshes. |
| Shared preferences | Stores a compact JSON-compatible snapshot accessible to React Native via a native module and to the launcher widget. |
| Native widget module | Contains the provider, RemoteViews layouts, configuration activity, update receiver, and bridge implementation. |
| Config plugin | Integrates the native module's manifest declarations and resources during Android prebuild without turning the widget into a mock in-app screen. |

## Accessibility and Interaction Principles

Every primary touch target is at least 44 points tall, task completion is never indicated by color alone, and semantic accessibility labels identify completion, deletion, and chibi mood controls. The task list is designed for thumb reach, using a floating add control on the lower right and avoiding dense controls at the top of the display.
