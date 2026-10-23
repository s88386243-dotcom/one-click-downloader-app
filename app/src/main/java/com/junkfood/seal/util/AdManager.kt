package com.junkfood.seal.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import java.lang.ref.WeakReference

object AdManager {
    private const val TAG = "AdManager"
    const val APP_ID = "209468455"
    private var activityRef: WeakReference<Activity>? = null

    fun init(context: Context) {
        try {
            StartAppSDK.init(context, APP_ID, false)
            StartAppSDK.enableReturnAds(false)
            Log.d(TAG, "StartApp SDK initialized successfully with App ID $APP_ID")
        } catch (e: Throwable) {
            Log.e(TAG, "Error initializing StartApp SDK", e)
        }
    }

    fun updateActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun showInterstitial(context: Context? = null) {
        try {
            val act = activityRef?.get() ?: (context as? Activity)
            if (act != null) {
                StartAppAd.showAd(act)
            } else if (context != null) {
                StartAppAd.showAd(context)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error showing interstitial ad", e)
        }
    }
}
