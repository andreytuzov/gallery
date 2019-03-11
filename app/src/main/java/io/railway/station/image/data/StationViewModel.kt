package io.railway.station.image.data

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.location.Location
import io.railway.station.image.App
import io.railway.station.image.database.photos.AssetsPhotoDB
import io.railway.station.image.database.photos.Image
import io.reactivex.Completable
import io.reactivex.Observable
import java.lang.IllegalArgumentException
import java.util.*

/**
 * Now it's include business logic functions
 */
class StationViewModel : LifecycleObserver {

    private lateinit var assetsPhotoDB: AssetsPhotoDB

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun connect() {
        assetsPhotoDB = AssetsPhotoDB(App.instance)
        assetsPhotoDB.open()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun disconnect() {
        assetsPhotoDB.close()
    }

    fun saveStationToHistory(stationName: String) =
            Completable.fromCallable { assetsPhotoDB.addStationToHistory(stationName) }

    fun getNearestStationByLocation(location: Location) = Observable.fromCallable {
        assetsPhotoDB.getNearestStation(location, 10000) ?: emptyList()
    }

    fun getNearestImageByLocation(location: Location) =
            getNearestStationByLocation(location).map {
                val stationName = it.getOrNull(0)?.first
                        ?: throw IllegalArgumentException("Station was not found")
                Pair(stationName, getImageByPattern(stationName)
                        .map {
                            it.shuffled(Random(System.currentTimeMillis()))
                        }.blockingFirst())
            }

    fun getHistoryStation() = Observable.fromCallable {
        assetsPhotoDB.getStationHistoryList() ?: emptyList()
    }

    fun getStationByPattern(pattern: String) =
            Observable.fromCallable { assetsPhotoDB.getStationNameList(pattern) ?: emptyList() }

    fun getImageByPattern(pattern: String): Observable<List<Image>> =
            Observable.fromCallable { assetsPhotoDB.getPhotoList(pattern) ?: emptyList() }

    fun getFavouriteImage() =
            Observable.fromCallable { assetsPhotoDB.getImageFavouriteList() ?: emptyList() }

    fun addImageToFavourite(ids: List<Int>) {
        assetsPhotoDB.addImageListToFavourite(ids)
    }

    fun removeImageFromFavourite(ids: List<Int>) {
        assetsPhotoDB.removeImageListFromFavourite(ids)
    }
}
