package io.railway.station.image.tools

import io.railway.station.image.App
import io.railway.station.image.database.photos.AssetsPhotoDB
import io.railway.station.image.database.photos.Image
import io.railway.station.image.utils.K
import io.railway.station.image.utils.NetUtils
import io.reactivex.Observable

@SuppressWarnings("CheckResult")
class InvalidUrlCleaner {

    private val assetsPhotoDB = AssetsPhotoDB(App.instance)
    private val invalidImageIdList = mutableListOf<Int>()

    init {
        assetsPhotoDB.open()
    }

    fun runObservable(startImageId: Int, checkImageUrlCount: Int) = Observable.fromCallable {

        fun List<Image>.subList(): List<Image> {
            var checkImageUrlCount = checkImageUrlCount
            var endImageId = startImageId
            val result = mutableListOf<Image>()
            var isStarted = false

            for (i in 0 until size) {
                val image = get(i)
                if (image.id == startImageId) isStarted = true
                if (isStarted) {
                    endImageId = image.id
                    result.add(image)
                    if (--checkImageUrlCount <= 0) break
                }
            }
            K.d("$LOG_TAG starting... [$startImageId, $endImageId]")
            return result
        }

        val imageList = assetsPhotoDB.getFullPhotoList() ?: return@fromCallable 0
        for (image in imageList.subList()) {
            val isUrlInvalid = !NetUtils.checkUrl(image.url)
                    && !NetUtils.checkUrl(image.getFullImageUrl())
            if (isUrlInvalid) {
                K.d("$LOG_TAG ${image.id} - ${image.getFullImageUrl()} is invalid")
                invalidImageIdList.add(image.id)
            }
        }
        return@fromCallable invalidImageIdList.size
    }

    fun removeInvalidUrlIfNeed() {
        K.d("$LOG_TAG result = $invalidImageIdList")
        if (!invalidImageIdList.isNullOrEmpty()) {
            assetsPhotoDB.removeImageList(invalidImageIdList)
        }
    }

    companion object {
        private const val LOG_TAG = "#InvalidUrlCleaner"
    }
}