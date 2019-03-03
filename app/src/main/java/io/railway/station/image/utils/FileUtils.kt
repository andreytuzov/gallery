package io.railway.station.image.utils

import android.Manifest
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import io.railway.station.image.App

object FileUtils {
    fun hasWriteExternalStoragePermission() =
            ContextCompat.checkSelfPermission(App.instance, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
}