package __CHIBI_WIDGET_PACKAGE__

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class ChibiWidgetModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  override fun getName() = "ChibiWidgetBridge"

  @ReactMethod
  fun saveWidgetSnapshot(snapshot: String, promise: Promise) {
    try {
      reactApplicationContext.getSharedPreferences(ChibiWidgetProvider.PREFERENCES, 0)
        .edit()
        .putString(ChibiWidgetProvider.SNAPSHOT_KEY, snapshot)
        .apply()
      promise.resolve(null)
    } catch (exception: Exception) {
      promise.reject("WIDGET_SAVE_FAILED", exception)
    }
  }

  @ReactMethod
  fun getWidgetSnapshot(promise: Promise) {
    try {
      val snapshot = reactApplicationContext.getSharedPreferences(ChibiWidgetProvider.PREFERENCES, 0)
        .getString(ChibiWidgetProvider.SNAPSHOT_KEY, null)
      promise.resolve(snapshot)
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
}
