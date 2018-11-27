package io.railway.station.image.helpers

import android.net.Uri
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.listener.BaseRequestListener
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import io.railway.station.image.utils.K

class ImageLoadingManager(
        private val availableUrlList: MutableList<String>
) {
    private val callbackMap = mutableMapOf<Int, (String) -> Unit>()
    private var lastPosition = availableUrlList.size - 1

    private val loadingUrlMap = mutableMapOf<Int, String>()
    private var currentPoolSize: Int = 0

    fun loadImage(position: Int, callback: (imageUrl: String) -> Unit) {
        val imageUrl = loadingUrlMap[position]
        if (imageUrl != null) {
            K.d("check image loadImage: position = $position, imageUrl = $imageUrl")
            callback(imageUrl)
        } else {
            K.d("check image loadImage: position = $position")
            callbackMap[position] = callback
            checkNeedLoadingImage()
        }
    }

    fun clearImageCache() {
        for (url in loadingUrlMap.values) {
            Fresco.getImagePipeline().evictFromCache(Uri.parse(url))
        }
    }

    private fun preloadImage(imageUrl: String) {

        K.d("check image preloadImage start ")

        currentPoolSize++

        val requestListener = object : BaseRequestListener() {
            override fun onRequestSuccess(request: ImageRequest?, requestId: String?, isPrefetch: Boolean) {
                K.d("check image preloadImage success: imageUrl = $imageUrl ")
                currentPoolSize--
                findCallback(imageUrl)?.invoke(imageUrl)
            }

            override fun onRequestFailure(request: ImageRequest?, requestId: String?, throwable: Throwable?, isPrefetch: Boolean) {
                K.d("check image preloadImage error: imageUrl = $imageUrl ")
                lastPosition--
                currentPoolSize--
                checkNeedLoadingImage()
            }
        }

        val imageRequest = ImageRequestBuilder
                .newBuilderWithSource(Uri.parse(imageUrl))
                .setRequestListener(requestListener)
                .build()

        Fresco.getImagePipeline().prefetchToBitmapCache(imageRequest, null)
    }

    private fun findCallback(imageUrl: String): ((String) -> Unit)? {
        K.d("check image findCallback before sync")
        synchronized(ImageLoadingManager::class.java) {
            for (position in 0..lastPosition) {
                if (callbackMap.containsKey(position)) {
                    loadingUrlMap[position] = imageUrl
                    K.d("check image findCallback: position = $position, imageUrl = $imageUrl")
                    return callbackMap.remove(position)
                }
            }
            return null
        }
    }

    private fun checkNeedLoadingImage() {
        if (availableUrlList.isNotEmpty() && currentPoolSize < POOL_SIZE) {
            preloadImage(availableUrlList.removeAt(0))
        }
    }

    companion object {
        private var POOL_SIZE = 8
    }

}