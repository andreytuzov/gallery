package ru.railway.dc.routes.utils

import android.util.Log

object K {
    private val LOG_TAG = "stationGallery"

    fun d(msg: String) {
        Log.d(LOG_TAG, msg)
    }

    fun e(msg: String, error: Throwable) {
        Log.e(LOG_TAG, msg, error)
    }
}
