package __CHIBI_WIDGET_PACKAGE__

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

class WidgetConfigActivity : Activity() {
  private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setResult(RESULT_CANCELED)
    appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
      ?: AppWidgetManager.INVALID_APPWIDGET_ID
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
      finish()
      return
    }

    val density = resources.displayMetrics.density
    val padding = (24 * density).toInt()
    val column = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      gravity = Gravity.CENTER_HORIZONTAL
      setPadding(padding, padding, padding, padding)
      setBackgroundColor(Color.rgb(255, 249, 245))
    }
    val title = TextView(this).apply {
      text = "Set up your companion"
      textSize = 24f
      setTextColor(Color.rgb(76, 45, 56))
      gravity = Gravity.CENTER
    }
    val body = TextView(this).apply {
      text = "Choose whether this widget should show your next small task. You can always change it later in the app."
      textSize = 15f
      setTextColor(Color.rgb(120, 83, 94))
      gravity = Gravity.CENTER
      setPadding(0, (12 * density).toInt(), 0, (18 * density).toInt())
    }
    val showTask = Switch(this).apply {
      text = "Show my next task"
      textSize = 16f
      isChecked = getSharedPreferences(ChibiWidgetProvider.PREFERENCES, 0).getBoolean("widget_show_tasks_$appWidgetId", true)
    }
    val save = Button(this).apply {
      text = "Add companion"
      setOnClickListener {
        getSharedPreferences(ChibiWidgetProvider.PREFERENCES, 0).edit().putBoolean("widget_show_tasks_$appWidgetId", showTask.isChecked).apply()
        ChibiWidgetProvider.refreshAll(this@WidgetConfigActivity)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
      }
    }
    column.addView(title, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    column.addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    column.addView(showTask, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    column.addView(save, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = (20 * density).toInt() })
    setContentView(column)
  }
}
