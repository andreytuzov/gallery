package ru.railway.dc.routes.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import okhttp3.Request
import okio.Okio
import ru.railway.dc.routes.App
import ru.railway.dc.routes.api.StationsApi
import java.io.OutputStream
import java.util.*

object SystemUtils {

    fun hasConnection(context: Context) =
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                    .activeNetworkInfo?.isConnectedOrConnecting == true

    fun requestCurrentLocation(waitTime: Long, context: Context, block: (Location?) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!gpsEnabled && !networkEnabled) block(getLastKnownLocation(context))
            else {
                val timer = Timer()
                val locationService = object : LocationListener {
                    override fun onLocationChanged(location: Location?) {
                        timer.cancel()
                        locationManager.removeUpdates(this)
                        block(location)
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String?) {}
                    override fun onProviderDisabled(provider: String?) {}
                }

                locationManager.requestLocationUpdates(if (gpsEnabled) LocationManager.GPS_PROVIDER
                else LocationManager.NETWORK_PROVIDER, 1000L, 0F, locationService)

                timer.schedule(object : TimerTask() {
                    override fun run() {
                        locationManager.removeUpdates(locationService)
                        block(getLastKnownLocation(context))
                    }
                }, waitTime)
            }
        } else block(null)
    }

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

    fun downloadToStream(url: String, outputStream: OutputStream) {
        val request = Request.Builder().url(url).build()
        val responseBody = StationsApi.okHttpClient.newCall(request).execute().body()
        if (responseBody != null) {
            val sink = Okio.buffer(Okio.sink(outputStream))
            sink.writeAll(responseBody.source())
        }
    }

    fun hasAccessFineLocationPermission() =
            ContextCompat.checkSelfPermission(App.instance, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED


    fun hasWriteExternalStoragePermission() =
            ContextCompat.checkSelfPermission(App.instance, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
}