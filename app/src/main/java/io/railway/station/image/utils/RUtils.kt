package io.railway.station.image.utils

import android.content.Context
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.WindowManager
import net.sf.geographiclib.Geodesic
import net.sf.geographiclib.GeodesicData
import net.sf.geographiclib.GeodesicMask

object RUtils {

    fun getIdFromAttr(attr: Int, c: Context): Int {
        val typedValue = TypedValue()
        return try {
            c.theme.resolveAttribute(attr, typedValue, true)
            typedValue.resourceId
        } catch (t: Throwable) {
            0
        }
    }

    fun getDimenFromAttr(attr: Int, c: Context): Int {
        val id = getIdFromAttr(attr, c)
        return if (id == 0) 0 else c.resources.getDimensionPixelSize(id)
    }

    fun getDimenFromRes(name: String, c: Context): Int {
        val resourceId = c.resources.getIdentifier(name, "dimen", "android")
        return if (resourceId > 0) c.resources.getDimensionPixelSize(resourceId) else 0
    }

    fun getScreenSize(windowManager: WindowManager): Point {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        return size
    }

    fun getScreenWidth(windowManager: WindowManager) = getScreenSize(windowManager).x

    fun getScreenHeight(windowManager: WindowManager) = getScreenSize(windowManager).y

    fun convertDpToPixels(value: Int, c: Context) = c.resources.displayMetrics.density * value

    fun convertPixelsToDp(value: Int, c: Context) = value / c.resources.displayMetrics.density

    fun getDrawableCompat(resourceId: Int, context: Context) =
            if (Build.VERSION.SDK_INT >= 21) context.getDrawable(resourceId) else context.resources.getDrawable(resourceId)

    // Haversine formula
    fun convertDistanceToKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double) =
            Geodesic.WGS84.Inverse(lat1, lon1, lat2, lon2, GeodesicMask.DISTANCE).s12

    fun convertDistanceToKmManually(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371e3
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.pow(Math.sin(dLat / 2), 2.0) +
                Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2.0)
        var c = 2 * Math.atan2(Math.pow(a, 0.5), Math.pow(1 - a, 0.5))
        return R * c
    }
}