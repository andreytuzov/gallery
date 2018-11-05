package ru.railway.dc.routes.utils

import android.animation.Animator
import android.animation.ValueAnimator
import com.wang.avi.indicators.*;
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