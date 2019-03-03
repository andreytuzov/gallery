package io.railway.station.image.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.support.v4.content.ContextCompat
import okhttp3.Request
import okio.Okio
import io.railway.station.image.App
import io.railway.station.image.api.StationsApi
import java.io.OutputStream

object LocationUtils {

    fun getLastKnownLocation(context: Context): Location? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val locationGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            val gpsLocationTime = locationGps?.time ?: 0
            val netLocationTime = locationNet?.time ?: 0

            return when {
                gpsLocationTime == 0L && netLocationTime == 0L -> null
                gpsLocationTime > netLocationTime -> locationGps
                else -> locationNet
            }
        }
        return null
    }
}