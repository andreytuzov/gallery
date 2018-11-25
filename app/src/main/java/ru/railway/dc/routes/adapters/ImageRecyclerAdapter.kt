package ru.railway.dc.routes.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.SimpleDraweeView
import ru.railway.dc.routes.database.photos.Image
import ru.railway.dc.routes.helpers.MultiplyImageActionModeController
import ru.railway.dc.routes.utils.RUtils
import ru.railway.dc.routes.utils.loadImage

class ImageRecyclerAdapter(val context: Context) : RecyclerView.Adapter<ImageRecyclerAdapter.ImageRecyclerViewHolder>() {

    private var mData: List<Image>? = null
    private val screenSize = RUtils.getScreenWidth(context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
    private val minImageSize = screenSize / 3
    private val maxImageSize = screenSize / 2

    private var selected: MutableMap<Int, Image>? = null
    private var countSelected: Int = -1
    private var controller: MultiplyImageActionModeController? = null

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ImageRecyclerViewHolder {
        val image = SimpleDraweeView(context)
        image.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        image.isSaveEnabled = false
        image.hierarchy.roundingParams = RoundingParams.fromCornersRadius(RUtils.convertDpToPixels(IMAGE_CORNER_RADIUS, context))
        val padding = RUtils.convertDpToPixels(IMAGE_PADDING, context).toInt()
        image.setPadding(padding, padding, padding, padding)
        val holder = ImageRecyclerViewHolder(image)
        image.setOnLongClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                val data = mData?.get(adapterPosition)
                if (data != null) onItemLongClick(data)
            }
            true
        }
        return holder
    }

    override fun getItemCount() = mData?.size ?: 0

    override fun onBindViewHolder(holder: ImageRecyclerViewHolder, position: Int) {
        val data = mData?.get(position)
        if (data != null) {
            val imageView = holder.itemView as SimpleDraweeView
            imageView.foreground = ColorDrawable(if (isItemSelected(data.id)) Color.parseColor("#30000000") else Color.TRANSPARENT)
            imageView.loadImage(context, position, mData!!, if (isBigImage(position)) maxImageSize else minImageSize)
        }
    }

    fun updateData(data: List<Image>?) {
        mData = data
        notifyDataSetChanged()
    }

    fun resetActionModeData() {
        selected = null
        countSelected = -1
        notifyDataSetChanged()
    }

    fun setMultiplyImageActionModeController(controller: MultiplyImageActionModeController) {
        this.controller = controller
    }

    private fun isItemSelected(imageId: Int) = selected?.contains(imageId) == true

    private fun onItemLongClick(image: Image) {
        if (controller != null) {
            if (!controller!!.mIsInActionMode) {
                selected = mutableMapOf(Pair(image.id, image))
                countSelected = 1
                controller!!.startActionMode(selected!!)
            } else onItemClick(image)
            notifyDataSetChanged()
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

    class ImageRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    companion object {
        private const val IMAGE_PADDING = 2
        private const val IMAGE_CORNER_RADIUS = 2

        fun isBigImage(position: Int): Boolean {
            val index = position % 18
            return index == 1 || index == 9
        }
    }
}