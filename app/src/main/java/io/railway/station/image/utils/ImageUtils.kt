package io.railway.station.image.utils

import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Environment
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequestBuilder
import io.railway.station.image.App
import io.railway.station.image.R
import io.railway.station.image.database.photos.Image
import io.reactivex.Observable
import java.io.File

object ImageUtils {

    private const val STATION_DIR_NAME = "stations_images"
    private const val IMAGE_EXTENSION = ".jpeg"

    private fun getDownloadDir() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    private fun getImageDir() = File(getDownloadDir(), STATION_DIR_NAME)

    fun downloadImages(images: Collection<Image>, dirPath: String): Observable<Boolean> {
        if (!NetUtils.hasConnection(App.instance) || !FileUtils.hasWriteExternalStoragePermission()) {
            return Observable.just(false)
        }
        return Observable.fromCallable {
            val imageDir = getImageDir()
            if (!imageDir.exists()) imageDir.mkdirs()
            for (image in images) {
                val fileName = image.description.ifNullOrEmpty(image.id.toString()) + IMAGE_EXTENSION
                val outputStream = File(imageDir, fileName).outputStream()
                NetUtils.downloadToStream(image.getFullImageUrl(), outputStream)
            }
            true
        }
    }

}

fun SimpleDraweeView.loadImage(
        imageUrl: String,
        imageWidth: Int,
        imageHeight: Int = imageWidth,
        doAfterLoadImage: (() -> Unit)? = null,
        doAfterFailure: (() -> Unit)? = null
) {
    val request = ImageRequestBuilder
            .newBuilderWithSource(Uri.parse(imageUrl))
            .setResizeOptions(ResizeOptions.forDimensions(imageWidth, imageHeight))

    val listener = object : BaseControllerListener<ImageInfo>() {
        override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
            doAfterLoadImage?.invoke()
        }

        override fun onFailure(id: String?, throwable: Throwable?) {
            doAfterFailure?.invoke()
        }
    }

    hierarchy.setFailureImage(R.drawable.image_failure)
    hierarchy.setPlaceholderImage(R.drawable.image_loading)
    controller = Fresco.newDraweeControllerBuilder()
            .setOldController(controller)
            .setControllerListener(listener)
            .setImageRequest(request.build())
            .build()
}