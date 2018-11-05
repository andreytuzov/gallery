package ru.railway.dc.routes.database.photos

import android.arch.persistence.room.Entity

@Entity
data class Country(
        val id: Int,
        val name: String,
        val listRegion: MutableList<Region> = mutableListOf()
)

data class Region(
        val id: Int,
        val name: String,
        val listStation: MutableList<Station> = mutableListOf()
)

data class Station(
        val id: Int,
        val name: String,
        val latitude: Float? = null,
        val longitude: Float? = null,
        val listImage: MutableList<Image> = mutableListOf()
)

data class StationHistory(
        val station: Station,
        val time: Float
)

data class Image(val id: Int, val url: String, val description: String?, val station: Station? = null) {
    fun getFullImageUrl() = url.replace("_s", "")
}
