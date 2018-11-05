package ru.railway.dc.routes.utils

import android.app.Activity
import com.github.mrengineer13.snackbar.SnackBar
import ru.railway.dc.routes.App

object ToastUtils {

    private var mIsActive = false
    private var mIsQueue = false
    private var mMessage: String? = null

    fun show(activity: Activity, messageId: Int, duration: Short = SnackBar.MED_SNACK) {
        show(activity, activity.resources.getString(messageId), duration)
    }

    fun show(activity: Activity, message: String, duration: Short = SnackBar.MED_SNACK) {
        if ((mIsActive && mMessage == message) || mIsQueue) return

        if (mIsActive) mIsQueue = true
        else mIsActive = true
        mMessage = message

        val snackBar = SnackBar.Builder(activity)
                .withMessage(message)
                .withDuration(duration)
                .show()


        val snackBarView = snackBar.containerView

        App.handler.postDelayed({
            mIsActive = mIsQueue
            mIsQueue = false
        }, duration.toLong())
    }

}