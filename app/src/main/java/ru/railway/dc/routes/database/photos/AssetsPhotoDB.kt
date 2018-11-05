package ru.railway.dc.routes.database.photos

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.location.Location

import ru.railway.dc.routes.App
import ru.railway.dc.routes.database.AssetsDBLoader
import ru.railway.dc.routes.utils.K
import ru.railway.dc.routes.utils.RUtils
import ru.railway.dc.routes.utils.StringUtils
import java.lang.StringBuilder
import java.util.*

class AssetsPhotoDB(private val context: Context) {

    private var db: SQLiteDatabase? = null

    fun open() {
        val dbHelper = DBHelper(context, DB_NAME, null, DB_VERSION)
        db = dbHelper.writableDatabase
    }

    fun close() {
        db?.close()
    }

    private fun getStation(selection: String): List<Station>? {
        return db!!.query(Schemas.TABLE_WITH_IMAGE, null, selection, null, null, null, null)
                .useCursor {
                    if (it.moveToFirst()) {
                        val result = mutableListOf<Station>()
                        val idColumn = it.getColumnIndex(Schemas.COLUMN_STATION_ID)
                        val nameColumn = it.getColumnIndex(Schemas.COLUMN_STATION_NAME)
                        val latitudeColumn = it.getColumnIndex(Schemas.COLUMN_STATION_LATITUDE)
                        val longitudeColumn = it.getColumnIndex(Schemas.COLUMN_STATION_LONGITUDE)
                        do {
                            val id = it.getInt(idColumn)
                            val name = it.getString(nameColumn).formatStationName()
                            val latitude = it.getFloat(latitudeColumn)
                            val longitude = it.getFloat(longitudeColumn)
                            result.add(Station(id, name, latitude, longitude))
                        } while (it.moveToNext())
                        return result
                    } else null
                }
    }

    private fun getImage(selection: String, withStation: Boolean = false): List<Image>? {
        return db!!.query(Schemas.TABLE_IMAGE, null, selection, null, null, null, null)
                .useCursor {
                    if (it.moveToFirst()) {
                        val result = mutableListOf<Image>()
                        val idColumn = it.getColumnIndex(Schemas.COLUMN_IMAGE_ID)
                        val urlColumn = it.getColumnIndex(Schemas.COLUMN_IMAGE_URL)
                        val descriptionColumn = it.getColumnIndex(Schemas.COLUMN_IMAGE_DESCRIPTION)
                        val imageStationIdColumn = it.getColumnIndex(Schemas.COLUMN_IMAGE_STATION_ID)
                        do {
                            val id = it.getInt(idColumn)
                            val url = it.getString(urlColumn)
                            val description = it.getString(descriptionColumn)
                            val imageStationId = it.getInt(imageStationIdColumn)
                            val station = if (withStation) getStation(Schemas.COLUMN_STATION_ID + " = " +
                                    imageStationId.toString()) else null
                            result.add(Image(id, url, description, station?.get(0)))
                        } while (it.moveToNext())
                        return result
                    } else null
                }
    }

    fun getStationListByName(stationName: String) = getStation(Schemas.COLUMN_STATION_NAME + " like '$stationName%'")

    fun getStationByName(stationName: String) = getStationListByName(stationName)?.get(0)

    fun getStationNameList(stationName: String) = getStationListByName(stationName)?.map { it.name }

    fun getPhotoList(stationName: String): List<Image>? {
        val sql = "$SQL_SELECT_IMAGE '$stationName%'"
        return db!!.rawQuery(sql, null).useCursor {
            if (it.moveToFirst()) {
                val imageList = mutableListOf<Image>()
                val idColumn = it.getColumnIndex(Schemas.COLUMN_IMAGE_ID)
                val urlColumn = it.getColumnIndex(Schemas.COLUMN_IMAGE_URL)
                val descriptionColumn = it.getColumnIndex(Schemas.COLUMN_IMAGE_DESCRIPTION)
                do {
                    val id = it.getInt(idColumn)
                    val url = it.getString(urlColumn).formatImageUrl()
                    val description = it.getString(descriptionColumn)
                    imageList.add(Image(id, url, description))
                } while (it.moveToNext())
                imageList
            } else null
        }
    }

    fun getNearestStation(location: Location, distanceEdge: Int): List<Pair<String, Double>>? {
        val selection = Schemas.COLUMN_STATION_LONGITUDE + " is not null AND " + Schemas.COLUMN_STATION_LONGITUDE +
                " != ? AND " + Schemas.COLUMN_STATION_LATITUDE + " is not null AND " + Schemas.COLUMN_STATION_LATITUDE + " != ?"
        db!!.query(Schemas.TABLE_STATION, arrayOf(Schemas.COLUMN_STATION_NAME, Schemas.COLUMN_STATION_LATITUDE,
                Schemas.COLUMN_STATION_LONGITUDE), selection, arrayOf("", ""), null, null, null).useCursor {
            if (it.moveToFirst()) {
                val currentLat = location.latitude
                val currentLon = location.longitude

                val list = mutableListOf<Pair<String, Double>>()
                do {
                    val lat = it.getDouble(1)
                    val lon = it.getDouble(2)
                    val distance = RUtils.convertDistanceToKm(currentLat, currentLon, lat, lon)
                    if (distance < distanceEdge) list.add(Pair(it.getString(0), distance))
                } while (it.moveToNext())

                list.sortBy { item -> item.second }
                return if (list.isNotEmpty()) list else null
            }
            return null
        }
    }

    fun addStationToHistory(stationName: String) {
        val station = getStationByName(stationName)
        val latitude = station?.latitude
        val longitude = station?.longitude

        val cv = ContentValues()
        cv.put(Schemas.COLUMN_HISTORY_STATION, stationName)
        cv.put(Schemas.COLUMN_HISTORY_LATITUDE, latitude)
        cv.put(Schemas.COLUMN_HISTORY_LONGITUDE, longitude)
        cv.put(Schemas.COLUMN_HISTORY_TIME, GregorianCalendar.getInstance().timeInMillis)
        db!!.insertWithOnConflict(Schemas.TABLE_HISTORY, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getStationHistoryList(): List<StationHistory>? {
        db!!.query(Schemas.TABLE_HISTORY, null, null, null, null,
                null, Schemas.COLUMN_IMAGE_ID + " DESC").useCursor {
            if (it.moveToFirst()) {
                val list = mutableListOf<StationHistory>()
                val idColumn = it.getColumnIndex(Schemas.COLUMN_HISTORY_ID)
                val stationColumn = it.getColumnIndex(Schemas.COLUMN_HISTORY_STATION)
                val longitudeColumn = it.getColumnIndex(Schemas.COLUMN_HISTORY_LONGITUDE)
                val latitudeColumn = it.getColumnIndex(Schemas.COLUMN_HISTORY_LATITUDE)
                val timeColumn = it.getColumnIndex(Schemas.COLUMN_HISTORY_TIME)
                do {
                    val id = it.getInt(idColumn)
                    val station = it.getString(stationColumn)
                    val longitude = it.getFloat(longitudeColumn)
                    val latitude = it.getFloat(latitudeColumn)
                    val time = it.getFloat(timeColumn)
                    list.add(StationHistory(Station(id, station, latitude, longitude), time))
                } while (it.moveToNext())
                return list
            }
            return null
        }
    }

    fun addImageListToFavourite(imageListId: List<Int>) {
        db!!.beginTransaction()
        for (imageId in imageListId) {
            val image = getImage(Schemas.COLUMN_IMAGE_ID + " = $imageId", true)?.get(0)
            if (image != null) {
                val cv = ContentValues()
                cv.put(Schemas.COLUMN_FAVOURITE_ID, image.id)
                cv.put(Schemas.COLUMN_FAVOURITE_IMAGE_URL, image.url)
                cv.put(Schemas.COLUMN_FAVOURITE_IMAGE_DESCRIPTION, image.description)
                cv.put(Schemas.COLUMN_FAVOURITE_STATION_NAME, image.station?.name)
                cv.put(Schemas.COLUMN_FAVOURITE_LONGITUDE, image.station?.longitude)
                cv.put(Schemas.COLUMN_FAVOURITE_LATITUDE, image.station?.latitude)
                db!!.insertWithOnConflict(Schemas.TABLE_FAVOURITE, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
        }
        db!!.setTransactionSuccessful()
        db!!.endTransaction()
    }

    fun removeImageListFromFavourite(imageListId: List<Int>) {
        val ids = StringBuilder()
        for (i in 0 until imageListId.size) {
            val imageId = imageListId[i]
            ids.append(if (i != 0) ',' else '(')
            ids.append("'$imageId'")
        }
        if (ids.isNotEmpty()) {
            ids.append(")")
            db!!.delete(Schemas.TABLE_FAVOURITE, Schemas.COLUMN_FAVOURITE_ID + " in $ids", null)
        }
    }

    fun isImageFavourite(imageId: Int) =
            db!!.query(Schemas.TABLE_FAVOURITE, null, Schemas.COLUMN_FAVOURITE_ID + " = $imageId", null,
                    null, null, null).useCursor {
                it.moveToFirst()
            }

    fun getImageFavouriteList(): List<Image>? {
        db!!.query(Schemas.TABLE_FAVOURITE, null, null, null, null,
                null, null).useCursor {
            if (it.moveToFirst()) {
                val list = mutableListOf<Image>()
                val idColumn = it.getColumnIndex(Schemas.COLUMN_FAVOURITE_ID)
                val stationNameColumn = it.getColumnIndex(Schemas.COLUMN_FAVOURITE_STATION_NAME)
                val latitudeColumn = it.getColumnIndex(Schemas.COLUMN_FAVOURITE_LATITUDE)
                val longitudeColumn = it.getColumnIndex(Schemas.COLUMN_FAVOURITE_LONGITUDE)
                val imageUrlColumn = it.getColumnIndex(Schemas.COLUMN_FAVOURITE_IMAGE_URL)
                val imageDescriptionColumn = it.getColumnIndex(Schemas.COLUMN_FAVOURITE_IMAGE_DESCRIPTION)
                do {
                    val id = it.getInt(idColumn)
                    val stationName = it.getString(stationNameColumn)
                    val latitude = it.getFloat(latitudeColumn)
                    val longitude = it.getFloat(longitudeColumn)
                    val imageUrl = it.getString(imageUrlColumn).formatImageUrl()
                    val imageDescription = it.getString(imageDescriptionColumn)
                    list.add(Image(id, imageUrl, imageDescription, Station(0, stationName, latitude, longitude)))
                } while (it.moveToNext())
                return list
            }
            return null
        }
    }

    private fun String.formatStationName(): String {
        var result = this
        if (length > STATION_MAX_LENGTH) {
            result = StringUtils.getShortStation(this, STATION_MAX_LENGTH)
        }
        val brace = result.indexOf('(')
        if (brace != -1) result = result.substring(0, brace)
        return result
    }

    private fun String.formatImageUrl() =
            PREFIX_IMAGE_URL + this + SUFFIX_IMAGE_URL

    class DBHelper(context: Context, name: String, factory: SQLiteDatabase.CursorFactory?, version: Int) : SQLiteOpenHelper(context, name, factory, version) {
        override fun onCreate(db: SQLiteDatabase) {
            K.d("onCreate")
            createFavouriteTable(db)
            createHistoryTable(db)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

        private fun createFavouriteTable(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + Schemas.TABLE_FAVOURITE + "("
                    + Schemas.COLUMN_FAVOURITE_ID + " INTEGER PRIMARY KEY,"
                    + Schemas.COLUMN_FAVOURITE_IMAGE_URL + " TEXT,"
                    + Schemas.COLUMN_FAVOURITE_IMAGE_DESCRIPTION + " TEXT,"
                    + Schemas.COLUMN_FAVOURITE_LATITUDE + " REAL,"
                    + Schemas.COLUMN_FAVOURITE_LONGITUDE + " REAL,"
                    + Schemas.COLUMN_FAVOURITE_STATION_NAME + " TEXT, UNIQUE ("
                    + Schemas.COLUMN_FAVOURITE_IMAGE_URL + ") ON CONFLICT REPLACE)")
        }

        private fun createHistoryTable(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + Schemas.TABLE_HISTORY + "("
                    + Schemas.COLUMN_HISTORY_ID + " INTEGER PRIMARY KEY,"
                    + Schemas.COLUMN_HISTORY_STATION + " TEXT,"
                    + Schemas.COLUMN_HISTORY_TIME + " REAL,"
                    + Schemas.COLUMN_HISTORY_LATITUDE + " REAL,"
                    + Schemas.COLUMN_HISTORY_LONGITUDE + " REAL, UNIQUE ("
                    + Schemas.COLUMN_HISTORY_STATION + ") ON CONFLICT REPLACE)")
        }
    }

    companion object {

        private const val STATION_MAX_LENGTH = 33

        const val SQL_SELECT_IMAGE = "select image._id, url, description from image, station where image.stationID = station._id and station.name like"
        const val SQL_SELECT_STATION = "SELECT _id, name, latitude, longitude FROM stationWithImage WHERE name LIKE"
        const val PREFIX_IMAGE_URL = "https://railwayz.info/photolines/images/"
        const val SUFFIX_IMAGE_URL = "_s.jpg"

        // Database params
        private const val DB_NAME = "photos.db"
        private const val DB_VERSION = 2
        private const val KEY_PREF_ASSETS_PHOTO_DB_CURRENT_VERSION = "assets_photo_db_current_version"

        fun configure(cnxt: Context) {
            val lastDbVersion = App.pref.getInt(KEY_PREF_ASSETS_PHOTO_DB_CURRENT_VERSION, -1)
            if (lastDbVersion != DB_VERSION) {
                AssetsDBLoader(cnxt, DB_NAME).copyDBFromAssets()
                App.pref.edit().putInt(KEY_PREF_ASSETS_PHOTO_DB_CURRENT_VERSION, DB_VERSION).apply()
                K.d("database: copy database photos.db")
            } else {
                K.d("database: database photos.db for this version exists")
            }
        }
    }
}

inline fun <R> Cursor.useCursor(block: (Cursor) -> R): R {
    try {
        return block(this)
    } catch (e: Throwable) {
        throw  e
    } finally {
        close()
    }
}