package ru.railway.dc.routes.utils

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.Animatable
import android.net.Uri
import android.widget.Toast
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.stfalcon.frescoimageviewer.ImageViewer
import com.wang.avi.indicators.*;
import ru.railway.dc.routes.database.photos.Image
import java.util.*

object ViewUtils {

    private val indicatorArray = arrayOf(
            BallBeatIndicator(),
            BallClipRotateIndicator(),
            BallClipRotateMultipleIndicator(),
            BallClipRotatePulseIndicator(),
            BallGridPulseIndicator(),
            BallGridBeatIndicator(),
            BallPulseIndicator(),
            BallPulseRiseIndicator(),
            BallPulseSyncIndicator(),
            BallScaleIndicator(),
            BallScaleMultipleIndicator(),
            BallScaleRippleIndicator(),
            BallScaleRippleMultipleIndicator(),
            BallSpinFadeLoaderIndicator(),
            BallTrianglePathIndicator(),
            BallZigZagDeflectIndicator(),
            BallZigZagIndicator(),
            CubeTransitionIndicator(),
            LineScaleIndicator(),
            LineScalePartyIndicator(),
            LineScalePulseOutIndicator(),
            LineScalePulseOutRapidIndicator(),
            LineSpinFadeLoaderIndicator(),
            PacmanIndicator(),
            SemiCircleSpinIndicator(),
            SquareSpinIndicator(),
            TriangleSkewSpinIndicator(),
            BallRotateIndicator())


    fun getRandomIndicator() =
            indicatorArray[Random(System.currentTimeMillis()).nextInt(indicatorArray.size)]


    fun createAnimation(
            from: Float,
            to: Float,
            duration: Long,
            onAnimationUpdate: ((ValueAnimator) -> Unit),
            onAnimationStart: (() -> Unit)? = null,
            onAnimationEnd: (() -> Unit)? = null
    ): ValueAnimator {
        val va = ValueAnimator.ofFloat(from, to)
        va.duration = duration
        if (onAnimationStart != null || onAnimationEnd != null) {
            va.addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(animation: Animator?) {
                    onAnimationEnd?.invoke()
                }

                override fun onAnimationStart(animation: Animator?) {
                    onAnimationStart?.invoke()
                }

                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {}
            })
        }

        va.addUpdateListener { onAnimationUpdate(it) }
        return va
    }

}

fun SimpleDraweeView.loadImage(context: Context, position: Int, imageList: List<Image>,
                               width: Int, height: Int = width) {
    val request = ImageRequestBuilder
            .newBuilderWithSource(Uri.parse(imageList[position].getFullImageUrl()))
            .setResizeOptions(ResizeOptions.forDimensions(width, height))

    val listener = object : BaseControllerListener<ImageInfo>() {
        override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
            setOnClickListener {
                ImageViewer.Builder<String>(context, imageList.map { it.getFullImageUrl() })
                        .setStartPosition(position)
                        .setImageChangeListener { Toast.makeText(context, imageList[it].description, Toast.LENGTH_LONG).show() }
                        .show()
            }
        }
    }

    controller = Fresco.newDraweeControllerBuilder()
            .setOldController(controller)
            .setControllerListener(listener)
            .setImageRequest(request.build())
            .build()
}