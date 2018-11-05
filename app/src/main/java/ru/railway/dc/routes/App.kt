package ru.railway.dc.routes

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import com.facebook.drawee.backends.pipeline.Fresco
import org.apache.log4j.BasicConfigurator
import ru.railway.dc.routes.database.photos.AssetsPhotoDB
import ru.railway.dc.routes.utils.TryMe

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        handler = Handler()
        pref = getSharedPreferences(FILE_PREF, Context.MODE_PRIVATE)
        Thread.setDefaultUncaughtExceptionHandler(TryMe())
        super.onCreate()
        BasicConfigurator.configure()
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
    }
}