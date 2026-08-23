package __CHIBI_WIDGET_PACKAGE__

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class ChibiWidgetModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  override fun getName() = "ChibiWidgetBridge"

  @ReactMethod
  fun saveWidgetSnapshot(snapshot: String, promise: Promise) {
    try {
      reactApplicationContext.getSharedPreferences(ChibiWidgetProvider.PREFERENCES, 0).edit().putString(ChibiWidgetProvider.SNAPSHOT_KEY, snapshot).apply()
      promise.resolve(null)
    } catch (exception: Exception) {
      promise.reject("WIDGET_SAVE_FAILED", exception)
    }
  }

  @ReactMethod
  fun getWidgetSnapshot(promise: Promise) {
    try {
      promise.resolve(reactApplicationContext.getSharedPreferences(ChibiWidgetProvider.PREFERENCES, 0).getString(ChibiWidgetProvider.SNAPSHOT_KEY, null))
    } catch (exception: Exception) {
      promise.reject("WIDGET_READ_FAILED", exception)
    }
  }

  @ReactMethod
  fun refreshWidgets(promise: Promise) {
    try {
      ChibiWidgetProvider.refreshAll(reactApplicationContext)
      promise.resolve(null)
    } catch (exception: Exception) {
      promise.reject("WIDGET_REFRESH_FAILED", exception)
    }
  }

  @ReactMethod
  fun isDesktopPetPermissionGranted(promise: Promise) {
    promise.resolve(Settings.canDrawOverlays(reactApplicationContext))
  }

  @ReactMethod
  fun isLauncherUsageAccessGranted(promise: Promise) {
    promise.resolve(MomoOverlayService.hasUsageAccess(reactApplicationContext))
  }

  @ReactMethod
  fun isDesktopPetEnabled(promise: Promise) {
    promise.resolve(reactApplicationContext.getSharedPreferences(ChibiWidgetProvider.PREFERENCES, 0).getBoolean(MomoOverlayService.OVERLAY_ENABLED, false))
  }

  @ReactMethod
  fun openDesktopPetPermissionSettings(promise: Promise) {
    try {
      val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${reactApplicationContext.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      reactApplicationContext.startActivity(intent)
      promise.resolve(null)
    } catch (exception: Exception) {
      promise.reject("OVERLAY_PERMISSION_SETTINGS_FAILED", exception)
    }
  }

  @ReactMethod
  fun openLauncherUsageAccessSettings(promise: Promise) {
    try {
      reactApplicationContext.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      promise.resolve(null)
    } catch (exception: Exception) {
      promise.reject("USAGE_ACCESS_SETTINGS_FAILED", exception)
    }
  }

  @ReactMethod
  fun startDesktopPet(promise: Promise) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      promise.reject("DESKTOP_PET_UNSUPPORTED", "The desktop pet requires Android 8 or newer.")
      return
    }
    if (!Settings.canDrawOverlays(reactApplicationContext)) {
      promise.reject("OVERLAY_PERMISSION_REQUIRED", "Display-over-other-apps permission is required for the desktop pet.")
      return
    }
    if (!MomoOverlayService.hasUsageAccess(reactApplicationContext)) {
      promise.reject("USAGE_ACCESS_REQUIRED", "Usage Access is required so Momo can stay only on the home screen.")
      return
    }
    try {
      val intent = Intent(reactApplicationContext, MomoOverlayService::class.java).setAction(MomoOverlayService.ACTION_START)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) reactApplicationContext.startForegroundService(intent) else reactApplicationContext.startService(intent)
      promise.resolve(null)
    } catch (exception: Exception) {
      promise.reject("DESKTOP_PET_START_FAILED", exception)
    }
  }

  @ReactMethod
  fun stopDesktopPet(promise: Promise) {
    try {
      reactApplicationContext.startService(Intent(reactApplicationContext, MomoOverlayService::class.java).setAction(MomoOverlayService.ACTION_STOP))
      promise.resolve(null)
    } catch (exception: Exception) {
      promise.reject("DESKTOP_PET_STOP_FAILED", exception)
    }
  }
}
