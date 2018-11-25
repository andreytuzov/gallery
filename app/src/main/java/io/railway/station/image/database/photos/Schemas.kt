package io.railway.station.image.database.photos

object Schemas {
    const val TABLE_COUNTRY = "country"
    const val COLUMN_COUNTRY_ID = "_id"
    const val COLUMN_NAME_ID = "name"

    const val TABLE_REGION = "region"
    const val COLUMN_REGION_ID = "_id"
    const val COLUMN_REGION_NAME = "name"
    const val COLUMN_REGION_COUNTRY_ID = "countryID"

    const val TABLE_STATION = "station"
    const val COLUMN_STATION_ID = "_id"
    const val COLUMN_STATION_NAME = "name"
    const val COLUMN_STATION_LATITUDE = "latitude"
    const val COLUMN_STATION_LONGITUDE = "longitude"
    const val COLUMN_STATION_REGION_ID = "regionID"

    const val TABLE_IMAGE = "image"
    const val COLUMN_IMAGE_ID = "_id"
    const val COLUMN_IMAGE_URL = "url"
    const val COLUMN_IMAGE_DESCRIPTION = "description"
    const val COLUMN_IMAGE_STATION_ID = "stationID"

    const val TABLE_WITH_IMAGE = "stationWithImage"

    const val TABLE_HISTORY = "history"
    const val COLUMN_HISTORY_ID = "_id"
    const val COLUMN_HISTORY_STATION = "station"
    const val COLUMN_HISTORY_TIME = "time"
    const val COLUMN_HISTORY_LATITUDE = "latitude"
    const val COLUMN_HISTORY_LONGITUDE = "longitude"

    const val TABLE_FAVOURITE = "favourite"
    const val COLUMN_FAVOURITE_ID = "_id"
    const val COLUMN_FAVOURITE_IMAGE_URL = "image_url"
    const val COLUMN_FAVOURITE_IMAGE_DESCRIPTION = "image_description"
    const val COLUMN_FAVOURITE_STATION_NAME = "station"
    const val COLUMN_FAVOURITE_LATITUDE = "latitude"
    const val COLUMN_FAVOURITE_LONGITUDE = "longitude"
}