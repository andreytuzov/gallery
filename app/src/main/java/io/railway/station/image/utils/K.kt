package io.railway.station.image.utils

import android.util.Log
import io.railway.station.image.BuildConfig

object K {
    private val LOG_TAG = "stationGallery"

    fun d(msg: String) {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, msg)
    }

    fun e(msg: String, error: Throwable) {
        if (BuildConfig.DEBUG) Log.e(LOG_TAG, msg, error)
    }
}
