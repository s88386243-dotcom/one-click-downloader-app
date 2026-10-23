package com.junkfood.seal.util

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import java.lang.ref.WeakReference

object AdManager {
    private const val TAG = "AdManager"
    const val APP_ID = "209468455"
    private var activityRef: WeakReference<Activity>? = null
    private var startAppAd: StartAppAd? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isLoading = false

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
        mainHandler.post {
            try {
                if (startAppAd == null) {
                    startAppAd = StartAppAd(activity)
                }
                preloadAd()
            } catch (e: Throwable) {
                Log.e(TAG, "Error binding StartAppAd to activity", e)
            }
        }
    }

    fun preloadAd() {
        mainHandler.post {
            try {
                val act = activityRef?.get() ?: return@post
                if (startAppAd == null) {
                    startAppAd = StartAppAd(act)
                }
                if (isLoading || startAppAd?.isReady == true) return@post

                isLoading = true
                Log.d(TAG, "Requesting StartApp interstitial preload...")
                startAppAd?.loadAd(
                    StartAppAd.AdMode.AUTOMATIC,
                    object : AdEventListener {
                        override fun onReceiveAd(ad: Ad) {
                            isLoading = false
                            Log.d(TAG, "StartApp Interstitial loaded successfully and ready to show!")
                        }

                        override fun onFailedToReceiveAd(ad: Ad?) {
                            isLoading = false
                            Log.w(TAG, "Failed to load StartApp ad: ${ad?.errorMessage}")
                        }
                    },
                )
            } catch (e: Throwable) {
                isLoading = false
                Log.e(TAG, "Error preloading StartApp ad", e)
            }
        }
    }

    fun showInterstitial(context: Context? = null) {
        mainHandler.post {
            try {
                val act = activityRef?.get() ?: (context as? Activity)
                val ad = startAppAd ?: if (act != null) StartAppAd(act).also { startAppAd = it } else null

                if (ad != null && ad.isReady) {
                    Log.d(TAG, "Showing StartApp interstitial ad")
                    ad.showAd(
                        object : AdDisplayListener {
                            override fun adHidden(ad: Ad?) {
                                Log.d(TAG, "Ad hidden, preloading next ad")
                                preloadAd()
                            }

                            override fun adDisplayed(ad: Ad?) {
                                Log.d(TAG, "Ad displayed successfully")
                            }

                            override fun adClicked(ad: Ad?) {
                                Log.d(TAG, "Ad clicked")
                            }

                            override fun adNotDisplayed(ad: Ad?) {
                                Log.w(TAG, "Ad not displayed, preloading next ad")
                                preloadAd()
                            }
                        }
                    )
                } else if (ad != null) {
                    Log.d(TAG, "Ad not ready yet, loading and showing on receipt")
                    ad.loadAd(
                        StartAppAd.AdMode.AUTOMATIC,
                        object : AdEventListener {
                            override fun onReceiveAd(receivedAd: Ad) {
                                Log.d(TAG, "Fallback ad received, showing now")
                                ad.showAd()
                                preloadAd()
                            }

                            override fun onFailedToReceiveAd(failedAd: Ad?) {
                                Log.w(TAG, "Fallback ad load failed: ${failedAd?.errorMessage}")
                            }
                        },
                    )
                } else if (act != null) {
                    StartAppAd.showAd(act)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error showing interstitial ad", e)
            }
        }
    }
}
