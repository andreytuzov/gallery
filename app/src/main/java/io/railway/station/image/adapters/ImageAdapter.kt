package io.railway.station.image.adapters

import android.content.Context
import android.graphics.drawable.Animatable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.controller.ControllerListener
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.stfalcon.frescoimageviewer.ImageViewer
import io.railway.station.image.R
import io.railway.station.image.database.photos.Image

class ImageAdapter(val context: Context, var data: List<Image>?) : BaseAdapter() {

    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getItem(position: Int) = data!![position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getCount() = if (data == null) 0 else data!!.size

    fun swapData(data: List<Image>?) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        val holder: ViewHolder
        if (convertView == null) {
            view = inflater.inflate(R.layout.item_grid_image_station, null)
            holder = ViewHolder(view)
            view.tag = holder
        } else holder = convertView.tag as ViewHolder

        val item = data!![position]

        holder.image.setOnClickListener {}
        val listener = object : BaseControllerListener<ImageInfo>() {
            override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
                holder.image.setOnClickListener {
                    ImageViewer.Builder<String>(context, data!!.map { it.getFullImageUrl() })
                            .setStartPosition(position)
                            .setImageChangeListener { Toast.makeText(context, data!![it].description, Toast.LENGTH_LONG).show() }
                            .show()
                }
            }
        }
        holder.image.isSaveEnabled = false
        loadImage(holder.image, item.getFullImageUrl(), listener)

        holder.description.text = item.description
        return view!!
    }

    private fun loadImage(image: SimpleDraweeView, url: String, listener: ControllerListener<ImageInfo>?) {
        val request = ImageRequestBuilder
                .newBuilderWithSource(Uri.parse(url))

        val controller = Fresco.newDraweeControllerBuilder()
                .setOldController(image.controller)
                .setControllerListener(listener)
                .setImageRequest(request.build())
                .build()

        image.controller = controller
    }

    private class ViewHolder(view: View) {
        val image = view.findViewById(R.id.img) as SimpleDraweeView
        val description = view.findViewById(R.id.description) as TextView
    }
}