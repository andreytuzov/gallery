package io.railway.station.image.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import com.facebook.drawee.view.SimpleDraweeView
import com.stfalcon.frescoimageviewer.ImageViewer
import io.railway.station.image.App
import io.railway.station.image.R
import io.railway.station.image.database.photos.Image
import io.railway.station.image.helpers.MultiplyImageActionModeController
import io.railway.station.image.utils.K
import io.railway.station.image.utils.RUtils
import io.railway.station.image.utils.loadImage

class ImageRecyclerAdapter(
        private val context: Context,
        private var columnCount: Int,
        private var isLowQuality: Boolean,
        private val updateColumnCount: (Int) -> Unit
) : RecyclerView.Adapter<ImageRecyclerAdapter.ImageRecyclerViewHolder>() {

    private val inflater = LayoutInflater.from(context)

    private var mData: MutableList<Image>? = null

    private val flexColumnCount
        get() = Math.min(columnCount, mData?.size ?: columnCount)

    private val flexMaxColumnCount: Int
        get() {
            val maxColumnCount = screenSize / MIN_IMAGE_SIZE
            return Math.min(mData?.size ?: maxColumnCount, maxColumnCount)
        }

    private val screenSize = RUtils.getScreenWidth(context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)

    private var imageSize: Int = 0

    private var selected: MutableMap<Int, Image>? = null
    private var countSelected: Int = -1
    private var controller: MultiplyImageActionModeController? = null

    init {
        setColumnCount(columnCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ImageRecyclerViewHolder {
        val holder = ImageRecyclerViewHolder(inflater.inflate(R.layout.list_item_image, parent, false))
        holder.imageView.isSaveEnabled = false
        holder.itemView.setOnLongClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                val data = mData?.get(adapterPosition)
                if (data != null) onItemLongClick(data)
            }
            true
        }
        holder.itemView.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                if (controller?.mIsInActionMode == false) {
                    ImageViewer.Builder<String>(context, mData!!.map { it.getFullImageUrl() })
                            .setStartPosition(adapterPosition)
                            .setImageChangeListener { Toast.makeText(context, mData!![it].description, Toast.LENGTH_LONG).show() }
                            .show()
                } else {
                    onItemClick(mData!![adapterPosition])
                }
            }
        }
        return holder
    }

    override fun getItemCount() = mData?.size ?: 0

    override fun onBindViewHolder(holder: ImageRecyclerViewHolder, position: Int) {
        val data = mData?.get(position) ?: return
        holder.itemView.layoutParams = ViewGroup.LayoutParams(imageSize, imageSize)
        holder.itemView.isSelected = isItemSelected(data.id)
        val imageUrl = getImageUrl(position)
        holder.imageView.loadImage(imageUrl, imageSize) {
            K.d("Error during image loading: $imageUrl")
        }
    }

    private fun getImageUrl(position: Int) = if (isLowQuality) mData!![position].url
    else mData!![position].getFullImageUrl()

    private fun setColumnCount(columnCount: Int) {
        this.columnCount = columnCount
        updateImageSize()
    }

    private fun updateImageSize() {
        imageSize = screenSize / flexColumnCount
        updateColumnCount(flexColumnCount)
    }

    fun nextColumnCount() {
        val columnCount = if (columnCount >= flexMaxColumnCount) 1 else this.columnCount + 1
        setColumnCount(columnCount)
        notifyDataSetChanged()
    }

    fun nextImageQuality() {
        isLowQuality = !isLowQuality
        notifyDataSetChanged()
    }

    fun setData(data: List<Image>?) {
        if (!data.isNullOrEmpty()) {
            mData = data.toMutableList()
            updateImageSize()
            notifyDataSetChanged()
        }
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
                notifyDataSetChanged()
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
        notifyDataSetChanged()
    }


    class ImageRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView = itemView.findViewById<SimpleDraweeView>(R.id.imageView)
    }

    companion object {
        private val MIN_IMAGE_SIZE = RUtils.convertDpToPixels(100, App.instance).toInt()
    }
}