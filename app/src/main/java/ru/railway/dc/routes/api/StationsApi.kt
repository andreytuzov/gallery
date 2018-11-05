package ru.railway.dc.routes.api

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object StationsApi {
    val CONNECTION_TIMEOUT = 16000L
    val READ_TIMEOUT = 25000L

    val okHttpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .retryOnConnectionFailure(true)
            .connectionPool(ConnectionPool(3, 4, TimeUnit.MINUTES))
            .readTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
            .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
            .build()
}