package io.railway.station.image.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import io.railway.station.image.App
import io.railway.station.image.ImageActivity
import io.reactivex.subjects.PublishSubject
import java.lang.Exception

object LocationUtils {

    fun getLocationManager(context: Context): LocationManager? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) locationManager
        else null
    }

    fun getLastKnownLocation(context: Context = App.instance): Location? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            val locationManager = getLocationManager(context) ?: return null

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

    fun checkGpsEnabled(activity: AppCompatActivity, subject: PublishSubject<Boolean>) {
        val request = LocationSettingsRequest.Builder()
                .addLocationRequest(LocationRequest.create()
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY))
                .build()
        val task = LocationServices.getSettingsClient(activity)
                .checkLocationSettings(request)
        task.addOnCompleteListener {
            try {
                it.getResult(ApiException::class.java)
                subject.onNext(true)
            } catch (e: ApiException) {
                if (e.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    try {
                        (e as ResolvableApiException).startResolutionForResult(activity,
                                ImageActivity.REQUEST_TURN_ON_GPS)
                    } catch (e: Exception) {
                        K.e("Error during the show gps turn on dialog", e)
                        subject.onNext(false)
                    }
                } else {
                    K.e("Error during check status of gps", e)
                    subject.onNext(false)
                }
            }
        }
    }
}