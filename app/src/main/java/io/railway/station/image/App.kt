package io.railway.station.image

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.support.multidex.MultiDexApplication
import com.facebook.drawee.backends.pipeline.Fresco
import org.apache.log4j.BasicConfigurator
import io.railway.station.image.database.photos.AssetsPhotoDB
import io.railway.station.image.helpers.TooltipManager
import io.railway.station.image.helpers.TryMe

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        handler = Handler()
        pref = getSharedPreferences(FILE_PREF, Context.MODE_PRIVATE)
        tooltipManager = TooltipManager()
        Thread.setDefaultUncaughtExceptionHandler(TryMe())
        super.onCreate()
        if (BuildConfig.DEBUG) BasicConfigurator.configure()
        AssetsPhotoDB.configure(baseContext)
        Fresco.initialize(this)
    }

    companion object {
        const val FILE_PREF = "main_pref"

        lateinit var pref: SharedPreferences
            private set
        lateinit var instance: App
            private set
        lateinit var handler: Handler
            private set
        lateinit var tooltipManager: TooltipManager
            private set
    }
}