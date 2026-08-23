package __CHIBI_WIDGET_PACKAGE__

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import org.json.JSONObject

class ChibiWidgetProvider : AppWidgetProvider() {
  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    appWidgetIds.forEach { appWidgetId -> updateWidget(context, appWidgetManager, appWidgetId) }
  }

  override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
    updateWidget(context, appWidgetManager, appWidgetId)
  }

  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      ACTION_CYCLE_MOOD -> {
        cycleMood(context)
        refreshAll(context)
        return
      }
    }
    super.onReceive(context, intent)
  }

  private fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
    val data = snapshot(context)
    val views = RemoteViews(context.packageName, R.layout.chibi_widget_compact)
    val mood = data.optString("mood", "idle")
    val message = data.optString("message", "you got this!")
    val artResource = context.resources.getIdentifier("chibi_$mood", "drawable", context.packageName).takeIf { it != 0 } ?: R.drawable.chibi_idle

    views.setImageViewResource(R.id.widget_chibi_image, artResource)
    views.setTextViewText(R.id.widget_message, if (message.isBlank()) "hi, I’m Momo ♡" else message)

    val cycleIntent = Intent(context, ChibiWidgetProvider::class.java).setAction(ACTION_CYCLE_MOOD)
    val cyclePendingIntent = PendingIntent.getBroadcast(context, appWidgetId, cycleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    views.setOnClickPendingIntent(R.id.widget_chibi_image, cyclePendingIntent)

    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    if (launchIntent != null) {
      val launchPendingIntent = PendingIntent.getActivity(context, appWidgetId + 1000, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
      views.setOnClickPendingIntent(R.id.widget_message, launchPendingIntent)
    }
    manager.updateAppWidget(appWidgetId, views)
  }

  private fun cycleMood(context: Context) {
    val data = snapshot(context)
    val moods = listOf("idle", "happy", "love", "sleepy", "excited", "shy", "sad")
    val current = data.optString("mood", "idle")
    val currentIndex = moods.indexOf(current).takeIf { it >= 0 } ?: 0
    val next = moods[(currentIndex + 1) % moods.size]
    val messages = mapOf(
      "idle" to "hehe hi",
      "happy" to "you got this!",
      "love" to "miss you ♡",
      "sleepy" to "remember to take a break",
      "excited" to "tiny win incoming",
      "shy" to "quietly cheering for you",
      "sad" to "be gentle with yourself",
    )
    data.put("mood", next)
    data.put("message", messages[next])
    data.put("updatedAt", System.currentTimeMillis().toString())
    context.getSharedPreferences(PREFERENCES, 0).edit().putString(SNAPSHOT_KEY, data.toString()).apply()
  }

  private fun snapshot(context: Context): JSONObject = try {
    JSONObject(context.getSharedPreferences(PREFERENCES, 0).getString(SNAPSHOT_KEY, "{}") ?: "{}")
  } catch (_: Exception) {
    JSONObject()
  }

  companion object {
    const val PREFERENCES = "chibi_widget_preferences"
    const val SNAPSHOT_KEY = "snapshot"
    private const val ACTION_CYCLE_MOOD = "chibi.todo.widget.CYCLE_MOOD"

    fun refreshAll(context: Context) {
      val manager = AppWidgetManager.getInstance(context)
      val component = ComponentName(context, ChibiWidgetProvider::class.java)
      val widgetIds = manager.getAppWidgetIds(component)
      if (widgetIds.isEmpty()) return
      context.sendBroadcast(Intent(context, ChibiWidgetProvider::class.java).apply {
        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
      })
    }
  }
}
