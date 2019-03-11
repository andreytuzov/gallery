package io.railway.station.image.helpers

import android.view.View
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

abstract class DebouncedOnClickListener : View.OnClickListener {

    private val subject = PublishSubject.create<View>()

    init {
        subject.throttleFirst(THRESHOLD_MILLIS, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onClicked(it) })
    }

    override fun onClick(v: View) {
        subject.onNext(v)
    }

    abstract fun onClicked(v: View)

    companion object {
        private const val THRESHOLD_MILLIS = 600L
    }

}