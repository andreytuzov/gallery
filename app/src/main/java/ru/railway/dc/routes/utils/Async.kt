package ru.railway.dc.routes.utils

import com.trello.rxlifecycle2.LifecycleTransformer
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

fun <T> Observable<T>.io(transformer: LifecycleTransformer<*>? = null): Observable<T> {
    val o = this.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    if (transformer != null) o.compose(transformer as LifecycleTransformer<T>)
    return o
}

fun <T> Maybe<T>.io(transformer: LifecycleTransformer<*>? = null): Maybe<T> {
    val o = this.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    if (transformer != null) o.compose(transformer as LifecycleTransformer<T>)
    return o
}

fun <T> Observable<T>.emptySubscribe(errorMsg: String) = this.subscribe({}, {
    K.e("$errorMsg", it)
})