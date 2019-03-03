package io.railway.station.image.utils

import android.content.Context
import android.net.ConnectivityManager
import io.railway.station.image.api.StationsApi
import okhttp3.Request
import okio.Okio
import java.io.OutputStream

object NetUtils {

    fun hasConnection(context: Context) =
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                    .activeNetworkInfo?.isConnectedOrConnecting == true

    fun downloadToStream(url: String, outputStream: OutputStream) {
        val request = Request.Builder().url(url).build()
        val responseBody = StationsApi.okHttpClient.newCall(request).execute().body()
        if (responseBody != null) {
            val sink = Okio.buffer(Okio.sink(outputStream))
            sink.writeAll(responseBody.source())
        }
    }

}