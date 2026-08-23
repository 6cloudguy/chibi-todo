package __CHIBI_WIDGET_PACKAGE__

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.content.pm.ServiceInfo
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import org.json.JSONObject

class MomoOverlayService : Service() {
  private lateinit var windowManager: WindowManager
  private var overlay: FrameLayout? = null
  private var overlayParams: WindowManager.LayoutParams? = null
  private var portrait: ImageView? = null
  private var speech: TextView? = null
  private var touchX = 0f
  private var touchY = 0f
  private var startX = 0
  private var startY = 0
  private var moved = false
  private val handler = Handler(Looper.getMainLooper())
  private var sceneIndex = 0

  private val unlockReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if (Intent.ACTION_USER_PRESENT == intent.action) dockBottomCenter()
    }
  }

  private val idleRunnable = object : Runnable {
    override fun run() {
      if (overlay == null) return
      advanceIdleScene()
      handler.postDelayed(this, 5500)
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        preferences().edit().putBoolean(OVERLAY_ENABLED, false).apply()
        stopSelf()
        return START_NOT_STICKY
      }
      ACTION_START -> {
        if (!Settings.canDrawOverlays(this)) {
          stopSelf()
          return START_NOT_STICKY
        }
        preferences().edit().putBoolean(OVERLAY_ENABLED, true).apply()
        startInForeground()
        showOverlay()
      }
      null -> {
        if (preferences().getBoolean(OVERLAY_ENABLED, false) && Settings.canDrawOverlays(this)) {
          startInForeground()
          showOverlay()
        }
      }
    }
    return START_STICKY
  }

  override fun onDestroy() {
    handler.removeCallbacksAndMessages(null)
    try { unregisterReceiver(unlockReceiver) } catch (_: Exception) { }
    overlay?.let { windowManager.removeView(it) }
    overlay = null
    super.onDestroy()
  }

  private fun showOverlay() {
    if (overlay != null) return
    windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    val density = resources.displayMetrics.density
    val width = (252 * density).toInt()
    val height = (260 * density).toInt()
    val root = FrameLayout(this).apply { setBackgroundColor(Color.TRANSPARENT) }
    val bubble = TextView(this).apply {
      background = bubbleBackground()
      gravity = Gravity.CENTER
      maxLines = 2
      setTextColor(Color.rgb(97, 59, 71))
      textSize = 13f
      setPadding((12 * density).toInt(), (7 * density).toInt(), (12 * density).toInt(), (7 * density).toInt())
    }
    val image = ImageView(this).apply {
      contentDescription = "Momo desktop companion. Drag to move; tap to play."
      scaleType = ImageView.ScaleType.FIT_CENTER
    }
    root.addView(bubble, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
    root.addView(image, FrameLayout.LayoutParams((190 * density).toInt(), (190 * density).toInt(), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
    root.setOnTouchListener(::handleTouch)

    overlayParams = WindowManager.LayoutParams(
      width,
      height,
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
      PixelFormat.TRANSLUCENT,
    ).apply {
      gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
      y = (26 * density).toInt()
    }
    overlay = root
    portrait = image
    speech = bubble
    windowManager.addView(root, overlayParams)
    updateFromSnapshot()
    handler.postDelayed(idleRunnable, 5500)
    val unlockFilter = IntentFilter(Intent.ACTION_USER_PRESENT)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerReceiver(unlockReceiver, unlockFilter, Context.RECEIVER_NOT_EXPORTED) else registerReceiver(unlockReceiver, unlockFilter)
  }

  private fun handleTouch(view: View, event: MotionEvent): Boolean {
    val params = overlayParams ?: return false
    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        touchX = event.rawX
        touchY = event.rawY
        startX = params.x
        startY = params.y
        moved = false
        return true
      }
      MotionEvent.ACTION_MOVE -> {
        val dx = (event.rawX - touchX).toInt()
        val dy = (event.rawY - touchY).toInt()
        if (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8) moved = true
        params.x = startX + dx
        params.y = startY - dy
        windowManager.updateViewLayout(view, params)
        return true
      }
      MotionEvent.ACTION_UP -> {
        if (!moved) playWithMomo()
        return true
      }
    }
    return false
  }

  private fun playWithMomo() {
    val data = snapshot()
    val scenes = listOf(
      "happy" to "Momo does a tiny happy hop!",
      "excited" to "zoom zoom—Momo is playing!",
      "love" to "a heart for you ♡",
      "shy" to "Momo peeks out bashfully…",
    )
    val scene = scenes[sceneIndex % scenes.size]
    sceneIndex += 1
    data.put("mood", scene.first)
    data.put("message", scene.second)
    data.put("updatedAt", System.currentTimeMillis().toString())
    saveSnapshot(data)
    applyScene(data)
    ChibiWidgetProvider.refreshAll(this)
  }

  private fun advanceIdleScene() {
    val data = snapshot()
    val scenes = listOf(
      "idle" to "Momo is watching over you.",
      "sleepy" to "a little stretch…",
      "happy" to "hehe, hi!",
    )
    val scene = scenes[sceneIndex % scenes.size]
    sceneIndex += 1
    data.put("mood", scene.first)
    data.put("message", scene.second)
    saveSnapshot(data)
    applyScene(data)
  }

  private fun dockBottomCenter() {
    val params = overlayParams ?: return
    params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
    params.x = 0
    params.y = (26 * resources.displayMetrics.density).toInt()
    overlay?.let { windowManager.updateViewLayout(it, params) }
    val data = snapshot().put("message", "I’m back at the bottom ♡")
    saveSnapshot(data)
    applyScene(data)
  }

  private fun updateFromSnapshot() = applyScene(snapshot())

  private fun applyScene(data: JSONObject) {
    val mood = data.optString("mood", "idle")
    val resource = resources.getIdentifier("chibi_$mood", "drawable", packageName).takeIf { it != 0 } ?: R.drawable.chibi_idle
    portrait?.setImageResource(resource)
    speech?.text = data.optString("message", "hi, I’m Momo ♡")
  }

  private fun bubbleBackground() = GradientDrawable().apply {
    setColor(Color.argb(246, 255, 252, 253))
    setStroke((1 * resources.displayMetrics.density).toInt(), Color.rgb(241, 188, 197))
    cornerRadius = 18 * resources.displayMetrics.density
  }

  private fun startInForeground() {
    val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Momo desktop companion", NotificationManager.IMPORTANCE_MIN))
    }
    val notification = Notification.Builder(this, CHANNEL_ID)
      .setContentTitle("Momo is keeping you company")
      .setContentText("Tap the companion to play, or drag Momo anywhere.")
      .setSmallIcon(android.R.drawable.btn_star_big_on)
      .setOngoing(true)
      .build()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  private fun preferences() = getSharedPreferences(ChibiWidgetProvider.PREFERENCES, 0)
  private fun isEnabled() = preferences().getBoolean(OVERLAY_ENABLED, false)
  private fun snapshot(): JSONObject = try { JSONObject(preferences().getString(ChibiWidgetProvider.SNAPSHOT_KEY, "{}") ?: "{}") } catch (_: Exception) { JSONObject() }
  private fun saveSnapshot(data: JSONObject) { preferences().edit().putString(ChibiWidgetProvider.SNAPSHOT_KEY, data.toString()).apply() }

  companion object {
    const val ACTION_START = "chibi.todo.overlay.START"
    const val ACTION_STOP = "chibi.todo.overlay.STOP"
    const val OVERLAY_ENABLED = "desktop_pet_enabled"
    private const val CHANNEL_ID = "momo_desktop_pet"
    private const val NOTIFICATION_ID = 2407
  }
}
