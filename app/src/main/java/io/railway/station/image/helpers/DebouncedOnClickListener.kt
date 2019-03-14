package io.stellio.player.Helpers

import android.view.View
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

abstract class DebouncedOnClickListener(thresholdTime: Long = THRESHOLD_MILLIS) : View.OnClickListener {

    private val subject = PublishSubject.create<View>()

    init {
        subject.throttleFirst(thresholdTime, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onClicked(it) })
    }

    override fun onClick(v: View) {
        subject.onNext(v)
    }

    abstract fun onClicked(v: View)

    companion object {
        const val THRESHOLD_MILLIS = 300L
    }

}

fun View.setOnClickDebounceListener(
        listener: (View) -> Unit
) {
    setOnClickDebounceListener(listener, DebouncedOnClickListener.THRESHOLD_MILLIS)
}

fun View.setOnClickDebounceListener(
        listener: (View) -> Unit,
        thresholdTime: Long
) {
    setOnClickListener(object : DebouncedOnClickListener(thresholdTime) {
        override fun onClicked(v: View) {
            listener(v)
        }
    })
}