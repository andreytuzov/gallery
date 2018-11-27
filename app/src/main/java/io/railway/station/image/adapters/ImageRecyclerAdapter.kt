package io.railway.station.image.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import com.arasthel.spannedgridlayoutmanager.SpanSize
import com.facebook.drawee.view.SimpleDraweeView
import com.stfalcon.frescoimageviewer.ImageViewer
import io.railway.station.image.R
import io.railway.station.image.database.photos.Image
import io.railway.station.image.helpers.MultiplyImageActionModeController
import io.railway.station.image.utils.RUtils
import io.railway.station.image.utils.loadImage
import kotlin.random.Random

class ImageRecyclerAdapter(
        val context: Context,
        val maxImageSize: Int
) : RecyclerView.Adapter<ImageRecyclerAdapter.ImageRecyclerViewHolder>() {

    val inflater = LayoutInflater.from(context)

    private var mData: MutableList<Image>? = null
    private val screenSize = RUtils.getScreenWidth(context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
    private var imgLocationList: List<ImageLocation>? = null

    private var selected: MutableMap<Int, Image>? = null
    private var countSelected: Int = -1
    private var controller: MultiplyImageActionModeController? = null

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
        val data = mData?.get(position)
        if (data != null) {
            val imgLocation = imgLocationList!![position]
            val imageWidth = screenSize * imgLocation.width / maxImageSize
            val imageHeight = screenSize * imgLocation.height / maxImageSize
            holder.itemView.isSelected = isItemSelected(data.id)
            loadImage(holder.imageView, imageWidth, imageHeight, position)
        }
    }

    private fun getImageUrl(position: Int) = mData!![position].getFullImageUrl()

    fun getSpanSizeByPosition(position: Int): SpanSize {
        val imgLocation = imgLocationList!![position]
        return SpanSize(imgLocation.width, imgLocation.height)
    }

    private fun loadImage(
            imageView: SimpleDraweeView,
            imageWidth: Int,
            imageHeight: Int,
            position: Int
    ) {
        var edge = MAX_COUNT_IMAGE_REQUEST - 1
        imageView.loadImage(getImageUrl(position), imageWidth, imageHeight) {
            if (edge-- > 0 && mData?.isNotEmpty() == true) {
                val randomPosition = Random(System.currentTimeMillis()).nextInt(0, mData!!.size)
                mData!![position] = mData!![randomPosition]
                loadImage(imageView, imageWidth, imageHeight, position)
            }
        }
    }

    fun updateData(data: List<Image>?) {
        if (!data.isNullOrEmpty()) {
            mData = data.toMutableList()
            imgLocationList = randomImageLocation(mData!!)
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


    private fun randomImageLocation(imageList: List<Image>): List<ImageLocation> {
        val random = java.util.Random(System.currentTimeMillis())
        val list = mutableListOf<ImageLocation>()
        val field: Array<BooleanArray> = Array(3000) { BooleanArray(maxImageSize) { false } }
        var x = 0
        var y = 0
        for (image in imageList) {
            while (field[y][x]) {
                x++
                if (x == maxImageSize) {
                    x = 0
                    y++
                }
            }
            var maxWidth = 0
            var maxHeight = 0
            while (maxWidth < maxImageSize && x + maxWidth < maxImageSize && !field[y][x + maxWidth]) maxWidth++
            while (maxHeight < maxImageSize && !field[y + maxHeight][x]) maxHeight++

            val width = random.nextInt(maxWidth) + 1
            val height = when (width) {
                1 -> 1
                maxImageSize -> maxImageSize - 1
                else -> random.nextInt(Math.min(width + 1, maxHeight)) + 1
            }

            for (i in x until x + width)
                for (j in y until y + height) {
                    field[j][i] = true
                }
            list.add(ImageLocation(x, y, width, height))
        }
        return list
    }

    data class ImageLocation(
            val x: Int,
            val y: Int,
            val width: Int,
            val height: Int
    )

    class ImageRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView = itemView.findViewById<SimpleDraweeView>(R.id.imageView)
    }

    companion object {
        private var MAX_COUNT_IMAGE_REQUEST = 10
    }
}