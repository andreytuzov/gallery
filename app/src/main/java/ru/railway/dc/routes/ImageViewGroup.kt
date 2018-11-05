package ru.railway.dc.routes

import android.content.Context
import android.graphics.drawable.Animatable
import android.net.Uri
import android.support.v4.widget.NestedScrollView
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.stfalcon.frescoimageviewer.ImageViewer
import ru.railway.dc.routes.database.photos.Image
import ru.railway.dc.routes.helpers.MultiplyImageActionModeController
import ru.railway.dc.routes.utils.K
import ru.railway.dc.routes.utils.RUtils
import java.util.*

class ImageViewGroup : ViewGroup, ViewTreeObserver.OnScrollChangedListener {

    private var mHeight = 0
    private var mImageSize = RUtils.getScreenWidth(context.getSystemService(Context.WINDOW_SERVICE)
            as WindowManager) / FIELD_WIDTH
    private var mPadding: Int = 0

    private val mImgLocationList = mutableListOf<ImageLocation>()
    private lateinit var mImageList: List<Image>
    private var lastLoadImageIndex = -1

    private lateinit var mScrollView: NestedScrollView
    private var mEdgePosition = 0
    private var loadScrollStepPosition = 0
    private var firstLoadScrollStepPosition = 0

    private var mTargetLoadPosition = 0

    private var selected: MutableMap<Int, Image>? = null
    private var countSelected: Int = -1
    private var controller: MultiplyImageActionModeController? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (parent != null && parent is NestedScrollView) {
            mScrollView = parent as NestedScrollView
            mScrollView.viewTreeObserver.addOnScrollChangedListener(this)
        }
    }

    override fun onScrollChanged() {
        if (!isAllImageLoaded()) locatePartImage()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMeasureSpec = MeasureSpec.makeMeasureSpec(mHeight * mImageSize + mPadding, MeasureSpec.EXACTLY)
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val lp = child.layoutParams as LayoutParams
            var l = mImageSize.toFloat() * lp.x
            l += mPadding

            var t = mImageSize * lp.y
            t += mPadding

            var r = mImageSize * (lp.x + lp.w)
            if (lp.x + lp.w == FIELD_WIDTH) r -= mPadding

            val b: Float = mImageSize.toFloat() * (lp.y + lp.h)
            child.layout(l.toInt(), t, r, b.toInt())
        }
    }

    private fun isItemSelected(imageId: Int) = selected?.contains(imageId) == true

    private fun onItemLongClick(image: Image) {
        if (controller != null) {
            if (!controller!!.mIsInActionMode) {
                selected = mutableMapOf(Pair(image.id, image))
                countSelected = 1
                controller!!.startActionMode(selected!!)
            } else onItemClick(image)
        }
    }

    private fun onItemClick(image: Image) {
        if (selected!!.contains(image.id)) {
            selected!!.remove(image.id)
            countSelected--
            if (countSelected == 0) {
                selected = null
                countSelected = -1
                controller!!.finishActionMode()
            } else {
                controller!!.updateSelectedData(selected!!)
            }
        } else {
            countSelected++
            selected!![image.id] = image
            controller!!.updateSelectedData(selected!!)
        }
    }

    fun setMultiplyImageActionModeController(controller: MultiplyImageActionModeController) {
        this.controller = controller
    }

    private fun init(attrs: AttributeSet) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ImageViewGroup)
        try {
            mPadding = a.getDimensionPixelSize(R.styleable.ImageViewGroup_ivg_padding, 2)
            val preloadPageCount = a.getInteger(R.styleable.ImageViewGroup_ivg_preload_page_count, 2)
            firstLoadScrollStepPosition = RUtils.getScreenHeight(context.getSystemService(Context.WINDOW_SERVICE) as WindowManager) * preloadPageCount
            loadScrollStepPosition = firstLoadScrollStepPosition / 4
        } finally {
            a.recycle()
        }
    }

    private fun isAllImageLoaded() = lastLoadImageIndex >= count()

    private fun count() = mImgLocationList.size

    private fun getCurrentLoadPosition() = when {
        lastLoadImageIndex == -1 -> 0
        isAllImageLoaded() -> mImgLocationList.last().y * mImageSize
        else -> mImgLocationList[lastLoadImageIndex].y * mImageSize
    }

    private fun locatePartImage(position: Int = mScrollView.scrollY) {
        K.d("position = $position, mEdgePosition = $mEdgePosition, mTargetLoadPosition = $mTargetLoadPosition")
        if (position >= mEdgePosition) {
            var currentLoadPosition = getCurrentLoadPosition()
            mTargetLoadPosition += if (mTargetLoadPosition == 0) firstLoadScrollStepPosition
            else loadScrollStepPosition
            while (currentLoadPosition < mTargetLoadPosition && ++lastLoadImageIndex < mImgLocationList.size) {
                val imgLocation = mImgLocationList[lastLoadImageIndex]
                val imageWidth = imgLocation.width * mImageSize
                val imageHeight = imgLocation.height * mImageSize
                val imageView = createImage(mImageList, lastLoadImageIndex, imageWidth, imageHeight)
                addImageToLayout(imageView, imgLocation)
                currentLoadPosition = getCurrentLoadPosition()
            }
            mEdgePosition += loadScrollStepPosition
        }
    }

    private fun resetParams() {
        lastLoadImageIndex = -1
        mEdgePosition = 0
        mTargetLoadPosition = 0
        mHeight = 0
        mImgLocationList.clear()
    }

    fun addImage(imageList: List<Image>?) {
        mScrollView.scrollTo(0, 0)
        resetParams()
        removeAllViews()
        if (imageList != null) {
            mImageList = imageList
            mImgLocationList.addAll(randomImageLocation(imageList))
            locatePartImage()
        }
    }

    private fun addImageToLayout(imageView: ImageView, imgLocation: ImageLocation) {
        val lp = if (imageView.layoutParams != null)
            imageView.layoutParams as LayoutParams
        else LayoutParams()

        lp.x = imgLocation.x
        lp.y = imgLocation.y
        lp.w = imgLocation.width
        lp.h = imgLocation.height

        imageView.layoutParams = lp

        addView(imageView, lp)
    }

    private fun randomImageLocation(imageList: List<Image>): List<ImageLocation> {
        val random = Random(imageList.hashCode().toLong())
        val list = mutableListOf<ImageLocation>()
        val field: Array<BooleanArray> = Array(FIELD_HEIGHT) { BooleanArray(FIELD_WIDTH) { false } }
        var x = 0
        var y = 0
        for (image in imageList) {
            while (field[y][x]) {
                x++
                if (x == FIELD_WIDTH) {
                    x = 0
                    y++
                }
            }
            var maxWidth = 0
            var maxHeight = 0
            while (maxWidth < IMAGE_MAX_WIDTH && x + maxWidth < FIELD_WIDTH && !field[y][x + maxWidth]) maxWidth++
            while (maxHeight < IMAGE_MAX_HEIGHT && !field[y + maxHeight][x]) maxHeight++

            var height = random.nextInt(maxHeight) + 1
            val width = random.nextInt(maxWidth) + 1

            for (i in x until x + width)
                for (j in y until y + height) {
                    field[j][i] = true
                }
            list.add(ImageLocation(x, y, width, height))
        }
        val size = list.size
        for (i in 1..FIELD_WIDTH) {
            if (size - i < 0) break
            mHeight = Math.max(list[size - i].y + list[size - i].height, mHeight)
        }
        return list
    }

    private fun createImage(data: List<Image>, position: Int, width: Int, height: Int): ImageView {
        val imageView = SimpleDraweeView(context)
        imageView.hierarchy.roundingParams = RoundingParams.fromCornersRadius(RUtils.convertDpToPixels(5, context))
        imageView.setOnLongClickListener {
            onItemLongClick(data[position])
            true
        }
        val request = ImageRequestBuilder
                .newBuilderWithSource(Uri.parse(data[position].getFullImageUrl()))
                .setResizeOptions(ResizeOptions.forDimensions(width, height))

        val listener = object : BaseControllerListener<ImageInfo>() {
            override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
                imageView.setOnClickListener {
                    ImageViewer.Builder<String>(context, data.map { it.getFullImageUrl() })
                            .setStartPosition(position)
                            .setImageChangeListener { Toast.makeText(context, data[it].description, Toast.LENGTH_LONG).show() }
                            .show()
                }
            }
        }

        val controller = Fresco.newDraweeControllerBuilder()
                .setOldController(imageView.controller)
                .setControllerListener(listener)
                .setImageRequest(request.build())
                .build()

        imageView.controller = controller
        return imageView
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?) =
            p is LayoutParams

    override fun generateLayoutParams(attrs: AttributeSet?) =
            LayoutParams(context, attrs)

    override fun generateDefaultLayoutParams() =
            super.generateDefaultLayoutParams() as LayoutParams

    class LayoutParams : ViewGroup.LayoutParams {

        var x = 0
        var y = 0
        var w = 0
        var h = 0

        constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ImageViewGroup_layout)
            try {
                x = a.getResourceId(R.styleable.ImageViewGroup_layout_ivg_layout_x, 0)
                y = a.getResourceId(R.styleable.ImageViewGroup_layout_ivg_layout_y, 0)
                w = a.getResourceId(R.styleable.ImageViewGroup_layout_ivg_layout_width, 0)
                h = a.getResourceId(R.styleable.ImageViewGroup_layout_ivg_layout_height, 0)
            } finally {
                a.recycle()
            }
        }

        constructor() : super(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    data class ImageLocation(
            val x: Int,
            val y: Int,
            val width: Int,
            val height: Int
    )

    companion object {
        private const val FIELD_WIDTH = 3
        private const val FIELD_HEIGHT = 1000

        private const val IMAGE_MAX_WIDTH = 3
        private const val IMAGE_MAX_HEIGHT = 2
    }
}