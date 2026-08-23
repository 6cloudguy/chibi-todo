package __CHIBI_WIDGET_PACKAGE__

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
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
import android.widget.ImageView
import android.widget.TextView
import org.json.JSONObject
import kotlin.math.abs
import kotlin.random.Random

class MomoOverlayService : Service() {
  private lateinit var windowManager: WindowManager
  private var petView: ImageView? = null
  private var petParams: WindowManager.LayoutParams? = null
  private var bubbleView: TextView? = null
  private var bubbleParams: WindowManager.LayoutParams? = null
  private var touchX = 0f
  private var touchY = 0f
  private var startX = 0
  private var startY = 0
  private var moved = false
  private var dragging = false
  private var velocityX = 3
  private var velocityY = 0
  private var phaseTicks = 0
  private var behavior = Behavior.REST
  private val handler = Handler(Looper.getMainLooper())

  private val motionRunnable = object : Runnable {
    override fun run() {
      if (petView != null && !dragging) advanceBehavior()
      handler.postDelayed(this, FRAME_DELAY_MS)
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
        if (!Settings.canDrawOverlays(this)) {
          stopSelf()
          return START_NOT_STICKY
        }
        preferences().edit().putBoolean(OVERLAY_ENABLED, true).apply()
        startInForeground()
        showPet()
        handler.removeCallbacks(motionRunnable)
        handler.post(motionRunnable)
      }
    }
    return START_STICKY
  }

  override fun onDestroy() {
    handler.removeCallbacksAndMessages(null)
    hidePet()
    super.onDestroy()
  }

  private fun showPet() {
    if (petView != null) return
    windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    val pet = ImageView(this).apply {
      contentDescription = "Momo companion. Tap to play, or drag Momo to pick her up."
      scaleType = ImageView.ScaleType.FIT_CENTER
      setOnTouchListener(::handleTouch)
    }
    val bubble = TextView(this).apply {
      background = bubbleBackground()
      gravity = Gravity.CENTER
      maxLines = 2
      setTextColor(Color.rgb(97, 59, 71))
      textSize = 13f
      setPadding(dp(10), dp(6), dp(10), dp(6))
    }
    petParams = WindowManager.LayoutParams(petSize(), petSize(), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
      gravity = Gravity.TOP or Gravity.START
      x = (screenWidth() - petSize()).coerceAtLeast(0) / 2
      y = floorY()
    }
    bubbleParams = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.TRANSLUCENT).apply {
      gravity = Gravity.TOP or Gravity.START
    }
    petView = pet
    bubbleView = bubble
    windowManager.addView(pet, petParams)
    windowManager.addView(bubble, bubbleParams)
    enterRest("Momo is here ♡")
  }

  private fun hidePet() {
    petView?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }
    bubbleView?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }
    petView = null
    petParams = null
    bubbleView = null
    bubbleParams = null
  }

  private fun handleTouch(view: View, event: MotionEvent): Boolean {
    val params = petParams ?: return false
    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        touchX = event.rawX
        touchY = event.rawY
        startX = params.x
        startY = params.y
        moved = false
        dragging = true
        showStateAsset("pickedup")
        setBubble("up we go!")
        return true
      }
      MotionEvent.ACTION_MOVE -> {
        val dx = (event.rawX - touchX).toInt()
        val dy = (event.rawY - touchY).toInt()
        if (abs(dx) > dp(4) || abs(dy) > dp(4)) moved = true
        params.x = (startX + dx).coerceIn(0, maxX())
        params.y = (startY + dy).coerceIn(0, floorY())
        updateWindowPositions()
        return true
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        dragging = false
        if (moved) enterFall("wheee!") else playWithMomo()
        return true
      }
    }
    return false
  }

  private fun advanceBehavior() {
    when (behavior) {
      Behavior.REST -> {
        phaseTicks -= 1
        if (phaseTicks <= 0) {
          if (Random.nextInt(100) < 68) enterWalk() else enterClimb()
        }
      }
      Behavior.WALK -> {
        val params = petParams ?: return
        params.x += velocityX
        if (params.x <= 0 || params.x >= maxX()) {
          params.x = params.x.coerceIn(0, maxX())
          velocityX *= -1
          enterClimb("Momo is climbing the screen edge!")
          return
        }
        petView?.translationY = if (phaseTicks % 10 < 5) -dp(2).toFloat() else 0f
        phaseTicks -= 1
        updateWindowPositions()
        if (phaseTicks <= 0) enterRest("a tiny break…")
      }
      Behavior.CLIMB -> {
        val params = petParams ?: return
        params.y = (params.y - dp(3)).coerceAtLeast(dp(14))
        phaseTicks -= 1
        updateWindowPositions()
        if (phaseTicks <= 0 || params.y <= dp(14)) enterFall("down I go!")
      }
      Behavior.FALL -> {
        val params = petParams ?: return
        velocityY += dp(1)
        params.y += velocityY
        if (params.y >= floorY()) {
          params.y = floorY()
          velocityY = 0
          updateWindowPositions()
          enterRest("soft landing! ♡")
          return
        }
        updateWindowPositions()
      }
    }
  }

  private fun enterRest(message: String) {
    behavior = Behavior.REST
    phaseTicks = Random.nextInt(42, 96)
    petView?.translationY = 0f
    showStateAsset("rest")
    setBubble(message)
  }

  private fun enterWalk(message: String = "Momo is taking a little walk.") {
    behavior = Behavior.WALK
    phaseTicks = Random.nextInt(55, 150)
    velocityX = if (Random.nextBoolean()) dp(3) else -dp(3)
    showStateAsset("walk")
    setBubble(message)
  }

  private fun enterClimb(message: String = "Momo is climbing the screen edge!") {
    behavior = Behavior.CLIMB
    phaseTicks = Random.nextInt(30, 65)
    petView?.translationY = 0f
    showStateAsset("climb")
    setBubble(message)
  }

  private fun enterFall(message: String) {
    behavior = Behavior.FALL
    velocityY = dp(2)
    petView?.translationY = 0f
    showStateAsset("fall")
    setBubble(message)
  }

  private fun playWithMomo() {
    val data = snapshot()
    val scenes = listOf("happy" to "Momo does a tiny happy hop!", "excited" to "zoom zoom—Momo is playing!", "love" to "a heart for you ♡", "shy" to "Momo peeks out bashfully…")
    val scene = scenes.random()
    data.put("mood", scene.first)
    data.put("message", scene.second)
    data.put("updatedAt", System.currentTimeMillis().toString())
    saveSnapshot(data)
    behavior = Behavior.REST
    phaseTicks = Random.nextInt(40, 85)
    showStateAsset(scene.first)
    setBubble(scene.second)
    ChibiWidgetProvider.refreshAll(this)
  }

  private fun showStateAsset(state: String) {
    val resource = resources.getIdentifier("chibi_$state", "drawable", packageName).takeIf { it != 0 } ?: R.drawable.chibi_idle
    petView?.setImageResource(resource)
  }

  private fun updateWindowPositions() {
    val pet = petView ?: return
    val params = petParams ?: return
    windowManager.updateViewLayout(pet, params)
    val bubble = bubbleView ?: return
    val bubbleLayout = bubbleParams ?: return
    bubble.measure(View.MeasureSpec.makeMeasureSpec(screenWidth(), View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(screenHeight(), View.MeasureSpec.AT_MOST))
    bubbleLayout.x = (params.x + petSize() / 2 - bubble.measuredWidth / 2).coerceIn(0, (screenWidth() - bubble.measuredWidth).coerceAtLeast(0))
    bubbleLayout.y = (params.y - bubble.measuredHeight - dp(4)).coerceAtLeast(0)
    windowManager.updateViewLayout(bubble, bubbleLayout)
  }

  private fun setBubble(message: String) {
    bubbleView?.text = message
    updateWindowPositions()
  }

  private fun bubbleBackground() = GradientDrawable().apply {
    setColor(Color.argb(246, 255, 252, 253))
    setStroke(dp(1), Color.rgb(241, 188, 197))
    cornerRadius = dp(18).toFloat()
  }

  private fun startInForeground() {
    val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Momo companion", NotificationManager.IMPORTANCE_MIN))
    val notification = Notification.Builder(this, CHANNEL_ID).setContentTitle("Momo is exploring")
      .setContentText("Momo can walk, climb, rest, and play over your screen.")
      .setSmallIcon(android.R.drawable.btn_star_big_on).setOngoing(true).build()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(NOTIFICATION_ID, notification)
  }

  private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
  private fun screenWidth() = resources.displayMetrics.widthPixels
  private fun screenHeight() = resources.displayMetrics.heightPixels
  private fun petSize() = dp(190)
  private fun maxX() = (screenWidth() - petSize()).coerceAtLeast(0)
  private fun floorY() = (screenHeight() - petSize() - dp(30)).coerceAtLeast(0)
  private fun preferences() = getSharedPreferences(ChibiWidgetProvider.PREFERENCES, 0)
  private fun snapshot(): JSONObject = try { JSONObject(preferences().getString(ChibiWidgetProvider.SNAPSHOT_KEY, "{}") ?: "{}") } catch (_: Exception) { JSONObject() }
  private fun saveSnapshot(data: JSONObject) { preferences().edit().putString(ChibiWidgetProvider.SNAPSHOT_KEY, data.toString()).apply() }

  private enum class Behavior { REST, WALK, CLIMB, FALL }

  companion object {
    const val ACTION_START = "chibi.todo.overlay.START"
    const val ACTION_STOP = "chibi.todo.overlay.STOP"
    const val OVERLAY_ENABLED = "desktop_pet_enabled"
    private const val FRAME_DELAY_MS = 48L
    private const val CHANNEL_ID = "momo_desktop_pet"
    private const val NOTIFICATION_ID = 2407
  }
}
