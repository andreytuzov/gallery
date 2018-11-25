package io.railway.station.image.utils

import android.content.Context
import android.graphics.Point
import android.os.Handler
import android.view.View
import it.sephiroth.android.library.tooltip.Tooltip
import io.railway.station.image.App
import io.railway.station.image.R
import java.util.*

class TooltipManager {

    private var mState: Int
    private var mLoadState: Int
    private val mToolTipGroupMap = mutableMapOf<Int, ToolTipGroup>()
    private val mShowQueue = LinkedList<ToolTipGroup>()
    private var mIsShowing = false
    private var mCurrentGroup = UNKNOWN_GROUP

    init {
        mLoadState = if (App.pref.contains(PREF_TOOLTIP_STATE))
            App.pref.getInt(PREF_TOOLTIP_STATE, 0)
        else
            0b01111111_11111111_11111111_11110001
        mState = mLoadState
    }

    private val listener = object : ToolTipGroup.OnToolTipGroupHiddenListener {
        override fun onGroupHide(groupId: Int) {
            // Set flag in preference, that tooltips were shown
            removeGroup(groupId)
            // Show next tooltip
            val nextToolTipGroup = mShowQueue.poll()
            if (nextToolTipGroup != null) {
                mCurrentGroup = nextToolTipGroup.groupId
                nextToolTipGroup.show(this)
            } else {
                mIsShowing = false
                mCurrentGroup = UNKNOWN_GROUP
            }

        }
    }

    fun show(vararg groupIdList: Int) {
        groupIdList.forEach { show(it) }
    }

    fun show(groupId: Int) {
        if (!mToolTipGroupMap.containsKey(groupId) || groupId == mCurrentGroup) {
            return
        }
        if (isGroupShow(groupId)) {
            if (!mIsShowing) {
                mCurrentGroup = groupId
                Handler().postDelayed({
                    mToolTipGroupMap[groupId]!!.show(listener)
                }, 600)
                mIsShowing = true
            } else {
                mShowQueue.push(mToolTipGroupMap[groupId])
            }
        }
    }

    fun addToolTip(view: View, textId: Int, gravity: Tooltip.Gravity, groupId: Int, consume: Boolean = false, c: Context? = null) {
        if (isGroupShow(groupId)) {
            val toolTipGroup = getToolTipGroup(groupId, c)
            toolTipGroup.add(ToolTip(view, textId, gravity, consume))
        }
    }

    fun addToolTip(point: Point, textId: Int, gravity: Tooltip.Gravity, groupId: Int, consume: Boolean = false, c: Context? = null) {
        if (isGroupShow(groupId)) {
            val toolTipGroup = getToolTipGroup(groupId, c)
            toolTipGroup.add(ToolTip(point, textId, gravity, consume))
        }
    }

    fun addToolTip(toolTipList: List<ToolTip>, groupId: Int, c: Context? = null) {
        if (isGroupShow(groupId)) {
            val toolTipGroup = getToolTipGroup(groupId, c)
            toolTipList.forEach { toolTipGroup.add(it) }
        }
    }

    fun setContext(groupId: Int, c: Context) {
        if (isGroupShow(groupId) && mToolTipGroupMap.containsKey(groupId)) {
            getToolTipGroup(groupId, null).setContext(c)
        }
    }

    fun resetGroup(vararg groupIdList: Int) {
        groupIdList.forEach { mState = mState or (1 shl it) }
    }

    fun close(context: Context) {
        val removeList = mutableListOf<Int>()
        for (key in mToolTipGroupMap.keys) {
            val value = mToolTipGroupMap[key]
            if (value != null && value.isContext(context)) {
                removeList.add(key)
            }
        }
        removeList.forEach {
            mToolTipGroupMap[it]?.setContext(null)
            mToolTipGroupMap.remove(it)
            // If the tip was shown, that reset flag for this tip
            val temp = (1 shl it)
            if (mLoadState and temp == 0)
                mState = mState and temp.inv()
        }
    }

    fun clearGroup(vararg groupIdList: Int) {
        groupIdList.forEach { mToolTipGroupMap[it]?.clear() }
    }

    private fun removeGroup(groupId: Int) {
        // If tooltip for specified group has not been shown
        if (mToolTipGroupMap[groupId] == null || mToolTipGroupMap[groupId]!!.isContext(null))
            return
        mToolTipGroupMap.remove(groupId)
        val temp = (1 shl groupId).inv()
        mState = mState and temp
        // Save new value in preference
        val state = mLoadState and temp
        if (mLoadState != state) {
            App.pref.edit().putInt(PREF_TOOLTIP_STATE, state).apply()
            mLoadState = state
        }
        mCurrentGroup = UNKNOWN_GROUP
    }

    private fun isGroupShow(groupId: Int) = groupId in 0..31
            && (mState shr groupId and 1) != 0

    private fun getToolTipGroup(groupId: Int, c: Context? = null): ToolTipGroup =
            if (mToolTipGroupMap.containsKey(groupId))
                mToolTipGroupMap[groupId]!!
            else {
                val toolTipGroup = ToolTipGroup(c, groupId)
                mToolTipGroupMap.put(groupId, toolTipGroup)
                toolTipGroup
            }

    class ToolTipGroup(var c: Context?, val groupId: Int) {

        private val toolTipList: MutableList<ToolTip> = mutableListOf()
        private var isShowing = false

        interface OnToolTipGroupHiddenListener {
            fun onGroupHide(groupId: Int)
        }

        fun setContext(c: Context?) {
            if (!isShowing && this.c == null) {
                this.c = c
            }
        }

        fun clear() {
            toolTipList.clear()
        }

        fun isContext(c: Context?) = this.c === c

        fun add(toolTip: ToolTip) {
            if (!toolTipList.contains(toolTip) && !isShowing) {
                toolTipList.add(toolTip)
            }
        }

        fun show(listener: OnToolTipGroupHiddenListener) {
            isShowing = true
            if (toolTipList.isNotEmpty()) {
                var i = 0
                toolTipList[0].show(c!!, object : ToolTip.OnToolTipHiddenListener {
                    override fun onHide() {
                        i++
                        if (c != null && i < toolTipList.size)
                            toolTipList[i].show(c!!, this)
                        else
                            listener.onGroupHide(groupId)
                    }
                })
            }
        }
    }


    class ToolTip {

        private var view: View? = null
        private var point: Point? = null
        private var textId: Int = 0
        private lateinit var gravity: Tooltip.Gravity
        private var consume: Boolean = false


        constructor(view: View,
                    textId: Int,
                    gravity: Tooltip.Gravity,
                    consume: Boolean = false) {
            this.view = view
            init(textId, gravity, consume)
        }

        constructor(point: Point,
                    textId: Int,
                    gravity: Tooltip.Gravity,
                    consume: Boolean = false) {
            this.point = point
            init(textId, gravity, consume)
        }

        private fun init(textId: Int, gravity: Tooltip.Gravity, consume: Boolean) {
            this.textId = textId
            this.gravity = gravity
            this.consume = consume
        }

        interface OnToolTipHiddenListener {
            fun onHide()
        }

        fun show(c: Context, listener: OnToolTipHiddenListener) {
            val builder = Tooltip.Builder(101)
                    .closePolicy(Tooltip.ClosePolicy()
                            .insidePolicy(true, consume)
                            .outsidePolicy(true, consume), 3000)
                    .text(c.resources, textId)
                    .withArrow(true)
                    .withOverlay(true)
                    .withStyleId(R.style.TooltipLayoutStyle)
                    .floatingAnimation(Tooltip.AnimationBuilder.DEFAULT)
                    .fadeDuration(500)
                    .withCallback(object : Tooltip.Callback {
                        override fun onTooltipClose(p0: Tooltip.TooltipView?, p1: Boolean, p2: Boolean) {}

                        override fun onTooltipFailed(p0: Tooltip.TooltipView?) {}

                        override fun onTooltipShown(p0: Tooltip.TooltipView?) {}

                        override fun onTooltipHidden(p0: Tooltip.TooltipView?) {
                            listener.onHide()
                        }
                    })
            if (view != null) {
                builder.anchor(view, gravity)
            } else {
                builder.anchor(point, gravity)
            }

            Tooltip.make(c, builder.build()).show()
        }

        override fun hashCode(): Int {
            val prime = 31
            var result = 1
            result = result * prime + textId
            result = result * prime + consume.hashCode()
            result = result * prime + gravity.hashCode()
            if (point != null)
                result = result * prime + point!!.hashCode()
            if (view != null)
                result = result * prime + view!!.hashCode()
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other)
                return true
            if (null == other)
                return false
            if (other is ToolTip) {
                if (other.textId != textId)
                    return false
                if (other.consume != consume)
                    return false
                if (other.point != point)
                    return false
                if (other.view != view)
                    return false
                if (other.gravity != gravity)
                    return false
            }
            return true
        }

    }

    companion object {
        const val PREF_TOOLTIP_STATE = "tooltip_state"

        private const val UNKNOWN_GROUP = -1

        const val MAIN_GROUP = 0
    }

}