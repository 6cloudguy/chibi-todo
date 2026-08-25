package __CHIBI_WIDGET_PACKAGE__

import android.app.AppOpsManager
import android.app.KeyguardManager
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
import android.content.pm.PackageManager
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
import kotlin.random.Random

class MomoOverlayService : Service() {
  private lateinit var windowManager: WindowManager
  private var rootContainer: FrameLayout? = null
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
  private var isClimbingRightWall = false
  private var isFlipped = false
  private var currentForegroundPackage = ""
  private var receiversRegistered = false
  private val handler = Handler(Looper.getMainLooper())

  private val screenStateReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      when (intent.action) {
        Intent.ACTION_SCREEN_OFF -> {
          // Screen turned off / locked -> immediately hide pet and pause loops
          hidePet()
          handler.removeCallbacks(motionRunnable)
          handler.removeCallbacks(visibilityRunnable)
        }
        Intent.ACTION_SCREEN_ON -> {
          // Screen turned on -> check if locked. If still on lock screen, stay hidden.
          val keyguard = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
          if (keyguard?.isKeyguardLocked == true) {
            hidePet()
          } else {
            handler.removeCallbacks(visibilityRunnable)
            handler.post(visibilityRunnable)
          }
        }
        Intent.ACTION_USER_PRESENT -> {
          // User unlocked the device -> resume loops and check visibility immediately
          handler.removeCallbacks(visibilityRunnable)
          handler.post(visibilityRunnable)
          handler.postDelayed({ updateOverlayVisibility() }, 150)
        }
      }
    }
  }

  private val visibilityRunnable = object : Runnable {
    override fun run() {
      updateOverlayVisibility()
      handler.postDelayed(this, 250)
    }
  }

  private val motionRunnable = object : Runnable {
    override fun run() {
      if (rootContainer != null && !dragging) advanceBehavior()
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
        registerScreenStateReceiver()
        showPet()
        handler.removeCallbacks(visibilityRunnable)
        handler.removeCallbacks(motionRunnable)
        handler.post(visibilityRunnable)
        handler.post(motionRunnable)
      }
    }
    return START_STICKY
  }

  override fun onDestroy() {
    handler.removeCallbacksAndMessages(null)
    if (receiversRegistered) {
      try { unregisterReceiver(screenStateReceiver) } catch (_: Exception) {}
      receiversRegistered = false
    }
    hidePet()
    super.onDestroy()
  }

  private fun registerScreenStateReceiver() {
    if (receiversRegistered) return
    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_SCREEN_OFF)
      addAction(Intent.ACTION_SCREEN_ON)
      addAction(Intent.ACTION_USER_PRESENT)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      registerReceiver(screenStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    } else {
      registerReceiver(screenStateReceiver, filter)
    }
    receiversRegistered = true
  }

  private fun updateOverlayVisibility() {
    if (!isEnabled() || !Settings.canDrawOverlays(this)) {
      hidePet()
      return
    }
    val keyguard = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
    if (keyguard?.isKeyguardLocked == true) {
      hidePet()
      return
    }
    if (hasUsageAccess(this)) {
      if (isHomeLauncherForeground()) {
        showPet()
      } else {
        hidePet()
      }
    } else {
      // If user hasn't granted usage access yet, fallback to showing
      showPet()
    }
  }

  private fun isHomeLauncherForeground(): Boolean {
    val usage = getSystemService(USAGE_STATS_SERVICE) as? UsageStatsManager ?: return true
    val now = System.currentTimeMillis()

    try {
      val events = usage.queryEvents(now - 60_000, now)
      val event = UsageEvents.Event()
      var latestTime = 0L
      var latestPkg = ""

      while (events.hasNextEvent()) {
        events.getNextEvent(event)
        val isResumed = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) ||
                        event.eventType == 1 // MOVE_TO_FOREGROUND
        if (isResumed && event.timeStamp >= latestTime) {
          latestTime = event.timeStamp
          latestPkg = event.packageName.orEmpty()
        }
      }

      if (latestPkg.isNotEmpty()) {
        currentForegroundPackage = latestPkg
      } else {
        // Secondary fallback to queryUsageStats
        val statsList = usage.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 86_400_000, now)
        val topStat = statsList?.maxByOrNull { it.lastTimeUsed }
        if (topStat != null && topStat.packageName.isNotEmpty()) {
          currentForegroundPackage = topStat.packageName
        }
      }
    } catch (_: Exception) {}

    if (currentForegroundPackage.isNotEmpty()) {
      if (currentForegroundPackage == packageName) {
        return false // Hide when user is inside the Chibi Todo app
      }
      val homePackages = getHomeLauncherPackages()
      return homePackages.contains(currentForegroundPackage)
    }

    return true
  }

  private fun getHomeLauncherPackages(): Set<String> {
    val packages = mutableSetOf<String>()
    try {
      val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
      val defaultLauncher = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
      if (!defaultLauncher.isNullOrEmpty() && defaultLauncher != "android") {
        packages.add(defaultLauncher)
      }
      val allLaunchers = packageManager.queryIntentActivities(homeIntent, 0)
      for (info in allLaunchers) {
        val pkg = info.activityInfo?.packageName
        if (!pkg.isNullOrEmpty() && pkg != "android") {
          packages.add(pkg)
        }
      }
    } catch (_: Exception) {}

    packages.addAll(listOf(
      "com.miui.home",
      "com.mi.android.globallauncher",
      "com.google.android.apps.nexuslauncher",
      "com.google.android.launcher",
      "com.sec.android.app.launcher",
      "com.android.launcher",
      "com.android.launcher3",
      "com.oppo.launcher",
      "com.oneplus.launcher",
      "com.huawei.android.launcher",
      "com.coloros.launcher",
      "com.transsion.launcher",
      "com.vivo.launcher",
      "com.bbk.launcher2",
      "com.teslacoilsw.launcher",
      "app.lawnchair"
    ))
    return packages
  }

  private fun showPet() {
    if (rootContainer != null) return
    windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    val root = FrameLayout(this).apply {
      clipChildren = false
      clipToPadding = false
    }
    val pet = ImageView(this).apply {
      contentDescription = "Momo companion. Tap to play, or drag Momo to pick her up."
      scaleType = ImageView.ScaleType.FIT_CENTER
      setOnTouchListener(::handleTouch)
    }
    root.addView(pet, FrameLayout.LayoutParams(petSize(), petSize()))

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
    rootContainer = root
    petView = pet
    bubbleView = bubble
    windowManager.addView(root, petParams)
    windowManager.addView(bubble, bubbleParams)
    enterRest("Momo is here ♡")

    handler.removeCallbacks(motionRunnable)
    handler.post(motionRunnable)
  }

  private fun hidePet() {
    rootContainer?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }
    bubbleView?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }
    rootContainer = null
    petView = null
    petParams = null
    bubbleView = null
    bubbleParams = null
    handler.removeCallbacks(motionRunnable)
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
        if (abs(dx) > dp(4) || abs(dy) > dp(4)) {
          moved = true
          if (dx < -dp(2)) setFlipped(true) // Dragging left -> flipped
          else if (dx > dp(2)) setFlipped(false) // Dragging right -> normal
        }
        params.x = (startX + dx).coerceIn(minWalkX(), maxWalkX())
        params.y = (startY + dy).coerceIn(ceilingY(), floorY())
        updateWindowPositions()
        return true
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        dragging = false
        if (moved) {
          val lastDx = (event.rawX - touchX).toInt()
          enterFall("wheee!", flyLeft = lastDx < 0)
        } else {
          playWithMomo()
        }
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
          enterWalk()
        }
      }
      Behavior.WALK -> {
        val params = petParams ?: return
        params.x += velocityX
        if (params.x <= minWalkX()) {
          params.x = leftWallClimbX()
          enterClimb(isRightWall = false, message = "Momo is climbing the screen edge!")
          return
        } else if (params.x >= maxWalkX()) {
          params.x = rightWallClimbX()
          enterClimb(isRightWall = true, message = "Momo is climbing the screen edge!")
          return
        }
        petView?.translationY = if (phaseTicks % 10 < 5) -dp(2).toFloat() else 0f
        phaseTicks -= 1
        updateWindowPositions()
        if (phaseTicks <= 0) enterRest("a tiny break…")
      }
      Behavior.CLIMB -> {
        val params = petParams ?: return
        params.x = if (isClimbingRightWall) rightWallClimbX() else leftWallClimbX()
        params.y = (params.y - dp(3)).coerceAtLeast(ceilingY())
        phaseTicks -= 1
        updateWindowPositions()
        if (phaseTicks <= 0 || params.y <= ceilingY()) {
          // Push off the wall and fly back toward the center of the screen
          enterFall("down I go!", flyLeft = isClimbingRightWall)
        }
      }
      Behavior.FALL -> {
        val params = petParams ?: return
        velocityY += dp(1)
        params.y += velocityY
        params.x = (params.x + velocityX).coerceIn(minWalkX(), maxWalkX())
        if (params.y >= floorY()) {
          params.y = floorY()
          velocityY = 0
          velocityX = 0
          updateWindowPositions()
          enterRest("soft landing! ♡")
          return
        }
        updateWindowPositions()
      }
    }
  }

  private fun setFlipped(flipped: Boolean) {
    isFlipped = flipped
    petView?.scaleX = if (flipped) -1f else 1f
    petView?.invalidate()
  }

  private fun enterRest(message: String) {
    behavior = Behavior.REST
    phaseTicks = Random.nextInt(42, 96)
    petView?.translationY = 0f
    setFlipped(false)
    showStateAsset("rest")
    setBubble(message)
  }

  private fun enterWalk(message: String = "Momo is taking a little walk.") {
    behavior = Behavior.WALK
    phaseTicks = Random.nextInt(60, 160)
    val params = petParams
    val walkRight = if (params != null) {
      if (params.x <= minWalkX() + dp(20)) true
      else if (params.x >= maxWalkX() - dp(20)) false
      else Random.nextBoolean()
    } else Random.nextBoolean()

    velocityX = if (walkRight) dp(3) else -dp(3)
    setFlipped(walkRight) // Flipped when walking right, normal when walking left
    showStateAsset("walk")
    setBubble(message)
  }

  private fun enterClimb(isRightWall: Boolean, message: String = "Momo is climbing the screen edge!") {
    behavior = Behavior.CLIMB
    isClimbingRightWall = isRightWall
    phaseTicks = Random.nextInt(35, 75)
    petView?.translationY = 0f
    setFlipped(isRightWall) // Flipped when climbing right wall, normal when climbing left wall
    showStateAsset("climb")
    setBubble(message)
  }

  private fun enterFall(message: String, flyLeft: Boolean? = null) {
    behavior = Behavior.FALL
    velocityY = dp(2)
    val shouldFlyLeft = flyLeft ?: (velocityX < 0 || (petParams?.x ?: 0) > screenWidth() / 2)
    velocityX = if (shouldFlyLeft) -dp(2) else dp(2)
    setFlipped(shouldFlyLeft) // Flipped when flying left, normal when flying right
    petView?.translationY = 0f
    showStateAsset("fall")
    setBubble(message)
  }

  private fun playWithMomo() {
    val data = snapshot()
    val scenes = listOf(
      "happy" to "Momo does a tiny happy hop!",
      "excited" to "zoom zoom—Momo is playing!",
      "love" to "a heart for you ♡",
      "shy" to "Momo peeks out bashfully…"
    )
    val scene = scenes.random()
    data.put("mood", scene.first)
    data.put("message", scene.second)
    data.put("updatedAt", System.currentTimeMillis().toString())
    saveSnapshot(data)
    behavior = Behavior.REST
    phaseTicks = Random.nextInt(40, 85)
    setFlipped(false)
    showStateAsset(scene.first)
    setBubble(scene.second)
    ChibiWidgetProvider.refreshAll(this)
  }

  private fun showStateAsset(state: String) {
    val resource = resources.getIdentifier("chibi_$state", "drawable", packageName).takeIf { it != 0 } ?: R.drawable.chibi_idle
    petView?.setImageResource(resource)
    petView?.scaleX = if (isFlipped) -1f else 1f
    petView?.invalidate()
  }

  private fun updateWindowPositions() {
    val root = rootContainer ?: return
    val params = petParams ?: return
    windowManager.updateViewLayout(root, params)
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
      .setContentText("Momo can walk, climb, rest, and play on your home screen.")
      .setSmallIcon(android.R.drawable.btn_star_big_on).setOngoing(true).build()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(NOTIFICATION_ID, notification)
  }

  private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
  private fun screenWidth() = resources.displayMetrics.widthPixels
  private fun screenHeight() = resources.displayMetrics.heightPixels
  private fun petSize() = dp(190)

  // Edge boundary calculations accounting for transparent padding in character art
  private fun minWalkX() = -dp(18)
  private fun maxWalkX() = screenWidth() - petSize() + dp(18)
  private fun leftWallClimbX() = -dp(32)
  private fun rightWallClimbX() = screenWidth() - petSize() + dp(32)
  private fun floorY() = (screenHeight() - petSize() - dp(24)).coerceAtLeast(0)
  private fun ceilingY() = dp(24)

  private fun preferences() = getSharedPreferences(ChibiWidgetProvider.PREFERENCES, 0)
  private fun isEnabled() = preferences().getBoolean(OVERLAY_ENABLED, false)
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

    fun hasUsageAccess(context: Context): Boolean {
      val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
      val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
      } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
      }
      return mode == AppOpsManager.MODE_ALLOWED
    }
  }
}
