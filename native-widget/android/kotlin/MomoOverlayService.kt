package __CHIBI_WIDGET_PACKAGE__

import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import org.json.JSONObject
import kotlin.math.abs

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
  private var dragging = false
  private var velocityX = 3
  private var velocityY = 0
  private var lastForegroundPackage = ""
  private var lastUsageCheck = System.currentTimeMillis() - 3_600_000
  private val handler = Handler(Looper.getMainLooper())
  private var sceneIndex = 0
  private var receiversRegistered = false

  private val unlockReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if (Intent.ACTION_USER_PRESENT == intent.action) handler.postDelayed({ updateOverlayVisibility() }, 700)
    }
  }

  private val visibilityRunnable = object : Runnable {
    override fun run() {
      updateOverlayVisibility()
      handler.postDelayed(this, 350)
    }
  }

  private val walkingRunnable = object : Runnable {
    override fun run() {
      if (overlay != null && !dragging) stepWalk()
      handler.postDelayed(this, 48)
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
      ACTION_START, null -> {
        if (!Settings.canDrawOverlays(this) || !hasUsageAccess(this)) {
          stopSelf()
          return START_NOT_STICKY
        }
        preferences().edit().putBoolean(OVERLAY_ENABLED, true).apply()
        startInForeground()
        registerUnlockReceiver()
        handler.removeCallbacks(visibilityRunnable)
        handler.removeCallbacks(walkingRunnable)
        handler.post(visibilityRunnable)
        handler.post(walkingRunnable)
      }
    }
    return START_STICKY
  }

  override fun onDestroy() {
    handler.removeCallbacksAndMessages(null)
    if (receiversRegistered) {
      try { unregisterReceiver(unlockReceiver) } catch (_: Exception) { }
      receiversRegistered = false
    }
    hideOverlay()
    super.onDestroy()
  }

  private fun registerUnlockReceiver() {
    if (receiversRegistered) return
    val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerReceiver(unlockReceiver, filter, Context.RECEIVER_NOT_EXPORTED) else registerReceiver(unlockReceiver, filter)
    receiversRegistered = true
  }

  private fun updateOverlayVisibility() {
    if (!isEnabled() || !Settings.canDrawOverlays(this) || !hasUsageAccess(this)) {
      hideOverlay()
      return
    }
    if (isHomeLauncherForeground()) showOverlay() else hideOverlay()
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
      contentDescription = "Momo home-screen pet. Drag to pick up; tap to play."
      scaleType = ImageView.ScaleType.FIT_CENTER
    }
    root.addView(bubble, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
    root.addView(image, FrameLayout.LayoutParams((190 * density).toInt(), (190 * density).toInt(), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
    root.setOnTouchListener(::handleTouch)

    overlayParams = WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
      gravity = Gravity.TOP or Gravity.START
      x = (screenWidth() - width).coerceAtLeast(0) / 2
      y = floorY(height)
    }
    overlay = root
    portrait = image
    speech = bubble
    windowManager.addView(root, overlayParams)
    updateFromSnapshot()
  }

  private fun hideOverlay() {
    overlay?.let {
      try { windowManager.removeView(it) } catch (_: Exception) { }
    }
    overlay = null
    overlayParams = null
    portrait = null
    speech = null
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
        dragging = true
        setPickedUpState()
        return true
      }
      MotionEvent.ACTION_MOVE -> {
        val dx = (event.rawX - touchX).toInt()
        val dy = (event.rawY - touchY).toInt()
        if (abs(dx) > 8 || abs(dy) > 8) moved = true
        params.x = (startX + dx).coerceIn(0, maxX())
        params.y = (startY + dy).coerceIn(0, floorY())
        windowManager.updateViewLayout(view, params)
        return true
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        dragging = false
        restoreWalkingState()
        if (!moved) playWithMomo()
        return true
      }
    }
    return false
  }

  private fun stepWalk() {
    val params = overlayParams ?: return
    params.x += velocityX
    params.y += velocityY
    velocityY += 1
    if (params.x <= 0 || params.x >= maxX()) {
      params.x = params.x.coerceIn(0, maxX())
      velocityX *= -1
      setMessage("Momo found a wall—turning around!")
    }
    val floor = floorY()
    if (params.y >= floor) {
      params.y = floor
      velocityY = 0
    }
    overlay?.let { windowManager.updateViewLayout(it, params) }
  }

  private fun setPickedUpState() {
    portrait?.apply {
      val lifted = resources.getIdentifier("chibi_excited", "drawable", packageName).takeIf { it != 0 } ?: R.drawable.chibi_idle
      setImageResource(lifted)
      scaleX = 1.08f
      scaleY = 1.08f
    }
    speech?.text = "up we go!"
  }

  private fun restoreWalkingState() {
    portrait?.apply { scaleX = 1f; scaleY = 1f }
    updateFromSnapshot()
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

  private fun setMessage(message: String) {
    speech?.text = message
  }

  private fun isHomeLauncherForeground(): Boolean {
    val usage = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
    val now = System.currentTimeMillis()
    val events = usage.queryEvents(lastUsageCheck, now)
    val event = UsageEvents.Event()
    while (events.hasNextEvent()) {
      events.getNextEvent(event)
      val resumed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
      if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND || resumed) {
        lastForegroundPackage = event.packageName ?: lastForegroundPackage
      }
    }
    lastUsageCheck = now
    return lastForegroundPackage == homeLauncherPackage()
  }

  private fun homeLauncherPackage(): String {
    val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    return packageManager.resolveActivity(homeIntent, 0)?.activityInfo?.packageName.orEmpty()
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Momo home-screen pet", NotificationManager.IMPORTANCE_MIN))
    val notification = Notification.Builder(this, CHANNEL_ID)
      .setContentTitle("Momo waits on your home screen")
      .setContentText("Momo appears only on your launcher. Drag or tap her there.")
      .setSmallIcon(android.R.drawable.btn_star_big_on)
      .setOngoing(true)
      .build()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(NOTIFICATION_ID, notification)
  }

  private fun screenWidth() = resources.displayMetrics.widthPixels
  private fun screenHeight() = resources.displayMetrics.heightPixels
  private fun overlayWidth() = overlayParams?.width ?: (252 * resources.displayMetrics.density).toInt()
  private fun overlayHeight() = overlayParams?.height ?: (260 * resources.displayMetrics.density).toInt()
  private fun maxX() = (screenWidth() - overlayWidth()).coerceAtLeast(0)
  private fun floorY(height: Int = overlayHeight()) = (screenHeight() - height - (26 * resources.displayMetrics.density).toInt()).coerceAtLeast(0)
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

    fun hasUsageAccess(context: Context): Boolean {
      val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
      return appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
    }
  }
}
