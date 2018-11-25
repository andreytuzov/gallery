package io.railway.station.image.helpers

import android.content.Context
import android.support.design.bottomappbar.BottomAppBar
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet

class AppCompatBottomAppBar : BottomAppBar {

    private val compatBehavior by lazy { CompatBehavior() }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun getBehavior(): CoordinatorLayout.Behavior<BottomAppBar> {
        return compatBehavior
    }

    private inner class CompatBehavior : BottomAppBar.Behavior() {
        fun show() {
            slideUp(this@AppCompatBottomAppBar)
        }

        fun hide() {
            slideDown(this@AppCompatBottomAppBar)
        }
    }

    fun show() {
        compatBehavior.show()
    }

    fun hide() {
        compatBehavior.hide()
    }
}