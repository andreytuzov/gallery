package io.railway.station.image

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.speech.RecognizerIntent
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.TextView
import android.widget.Toast
import com.arasthel.spannedgridlayoutmanager.SpanSize
import com.arasthel.spannedgridlayoutmanager.SpannedGridLayoutManager
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.core.CrashlyticsCore
import com.miguelcatalan.materialsearchview.MaterialSearchView
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import io.fabric.sdk.android.Fabric
import io.reactivex.Maybe
import io.reactivex.MaybeOnSubscribe
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.railway.station.image.adapters.ImageRecyclerAdapter
import io.railway.station.image.database.photos.AssetsPhotoDB
import io.railway.station.image.database.photos.Image
import io.railway.station.image.helpers.AppCompatBottomAppBar
import io.railway.station.image.helpers.MultiplyImageActionModeController
import io.railway.station.image.utils.*
import java.util.*
import kotlin.math.roundToInt

class ImageActivity : RxAppCompatActivity() {

    private lateinit var assetsPhotoDB: AssetsPhotoDB

    lateinit var adapter: ImageRecyclerAdapter
    private lateinit var recyclerView: RecyclerView

    private val requestHistory = mutableListOf<String>()

    private lateinit var searchView: MaterialSearchView
    private lateinit var bottomAppBar: AppCompatBottomAppBar
    private lateinit var fab: FloatingActionButton
    lateinit var snackBarHelper: SnackBarHelper
        private set
    private lateinit var root: View
    private val multiplyImageActionMode by lazy { MultiplyImageActionModeController(bottomAppBar, fab, this) }

    private var lastSearchStationName: String? = null
    private var permissionActions: MutableMap<Int, () -> Unit> = mutableMapOf()

    private val locationService by lazy {
        object : LocationListener {
            override fun onLocationChanged(location: Location?) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String?) {}
            override fun onProviderDisabled(provider: String?) {}
        }
    }

    private var loadStationDisposable: Disposable? = null
    private var historyStationDisposable: Disposable? = null
    private var showFavouriteImageDisposable: Disposable? = null
    private var showImageDisposable: Disposable? = null

    private var isNeedStartSearch: Boolean = false

    private fun saveStationToHistory(stationName: String) {
        Observable.fromCallable {
            assetsPhotoDB.addStationToHistory(stationName)
        }.io(bindUntilEvent<Any>(ActivityEvent.DESTROY))
                .emptySubscribe("Error during save last station")
    }

    private fun showNearestStationSuggestion() {
        executeAfterGetPermission(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_ACCESS_FINE_LOCATION) {
            getNearestStation().io(bindUntilEvent<Any>(ActivityEvent.DESTROY))
                    .subscribe({
                        if (it != null) {
                            searchView.setSuggestions(it.map { item -> item.first }.toTypedArray())
                            isNeedStartSearch = true
                            searchView.hideKeyboard(searchView)
                        }
                    }, {
                        K.e("Error during getting nearest station", it)
                    })
        }
    }

    private fun showStationSuggestion(pattern: String) {
        loadStationDisposable = loadStationObservable(pattern).io(bindUntilEvent<Any>(ActivityEvent.DESTROY))
                .subscribe({
                    searchView.setSuggestions(it.toTypedArray())
                }, {
                    K.e("Error during getting list of station name", it)
                })
    }


    private fun showHistoryStationSuggestion() {
        historyStationDisposable = getHistoryStation().io(bindUntilEvent<Any>(ActivityEvent.DESTROY))
                .subscribe({ stationHistoryList ->
                    searchView.setSuggestions(stationHistoryList!!.map { it.station.name }.toTypedArray())
                }, {
                    K.e("Error during get history station", it)
                })
    }

    private fun showImage(stationName: String?) {
        if (stationName != null && stationName != lastSearchStationName) {
            if (stationName == IMAGE_FAVOURITE_LIST) showFavouriteImage()
            else {
                saveRequest(stationName)
                lastSearchStationName = stationName
                showImageDisposable = loadImageObservable(stationName)
                        .io(bindUntilEvent<Any>(ActivityEvent.DESTROY))
                        .subscribe({
                            searchView.setSuggestions(null)
                            updateData(it)
                            saveStationToHistory(stationName)
                        }, {
                            K.e("Error during loading image", it)
                        })
            }
        }
    }

    fun showNearestImage() {
        executeAfterGetPermission(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_ACCESS_FINE_LOCATION) {
            getNearestStation().map { stationNameList ->
                if (stationNameList.isEmpty()) emptyList()
                else {
                    val stationName = stationNameList[0].first
                    if (stationName.isEmpty()) emptyList()
                    else {
                        if (lastSearchStationName == stationName) listOf(Image.createEmptyImage())
                        else {
                            saveRequest(stationName)
                            lastSearchStationName = stationName
                            val images = assetsPhotoDB.getPhotoList(stationName)
                            if (images != null) {
                                val imageList = mutableListOf<Image>()
                                imageList.addAll(images)
                                imageList.shuffle(Random(System.currentTimeMillis()))
                                this@ImageActivity.runOnUiThread {
                                    snackBarHelper.show(StringUtils.getShortStation(stationName.formatStationName(), 33))
                                }
                                imageList
                            } else null
                        }
                    }
                }
            }.io(bindUntilEvent<Any>(ActivityEvent.DESTROY))
                    .subscribe({
                        if (it!!.isEmpty() || !it[0].isEmptyImage()) updateData(it)
                    }, {
                        K.e("Error during showing nearest station", it)
                    })
        }
    }

    private fun showFavouriteImage(isImagesUpdate: Boolean = false) {
        if (isImagesUpdate || lastSearchStationName != IMAGE_FAVOURITE_LIST) {
            saveRequest(IMAGE_FAVOURITE_LIST)
            lastSearchStationName = IMAGE_FAVOURITE_LIST
            showFavouriteImageDisposable = getFavouriteImageList().io(bindUntilEvent<Any>(ActivityEvent.DESTROY))
                    .subscribe({
                        searchView.setSuggestions(null)
                        updateData(it)
                        snackBarHelper.show("Избранное")
                    }, {
                        K.e("Error during get favourite image", it)
                    })
        }
    }

    private fun updateData(data: List<Image>?) {
        adapter.updateData(data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_activity)

        Fabric.with(this, Crashlytics.Builder()
                .core(CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .answers(Answers())
                .build())

        root = findViewById(R.id.root)

        initToolbar()
        initBottomBar()

        if (Build.VERSION.SDK_INT >= 21) {
            window.navigationBarColor = Color.parseColor("#F0F0F0")
            window.statusBarColor = Color.parseColor("#F0F0F0")
            if (Build.VERSION.SDK_INT >= 23) {
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                if (Build.VERSION.SDK_INT >= 26) {
                    window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                }
            }
        }

        // Get name of station
        recyclerView = findViewById(R.id.recyclerView)
        adapter = ImageRecyclerAdapter(this)
        adapter.setMultiplyImageActionModeController(multiplyImageActionMode)
        val layoutManager = SpannedGridLayoutManager(SpannedGridLayoutManager.Orientation.VERTICAL, 3)
        layoutManager.spanSizeLookup = SpannedGridLayoutManager.SpanSizeLookup {
            if (ImageRecyclerAdapter.isBigImage(it)) SpanSize(2, 2)
            else SpanSize(1, 1)
        }
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        // Get information
        assetsPhotoDB = AssetsPhotoDB(this)
        assetsPhotoDB.open()

        searchView.post {
            showSearchBar(false)
        }
    }

    override fun onStart() {
        super.onStart()
        registerLocationListener()
    }

    override fun onStop() {
        super.onStop()
        unregisterLocationListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (showImageDisposable?.isDisposed == false) showImageDisposable?.dispose()
        if (loadStationDisposable?.isDisposed == false) loadStationDisposable?.dispose()
        if (historyStationDisposable?.isDisposed == false) historyStationDisposable?.dispose()
        if (showFavouriteImageDisposable?.isDisposed == false) showFavouriteImageDisposable?.dispose()
        assetsPhotoDB.close()
    }

    fun executeAfterGetPermission(permissionName: String, requestCode: Int, block: () -> Unit) {
        if (ContextCompat.checkSelfPermission(App.instance, permissionName) ==
                PackageManager.PERMISSION_GRANTED) {
            block()
        } else {
            permissionActions[requestCode] = block
            ActivityCompat.requestPermissions(this, arrayOf(permissionName), requestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == REQUEST_ACCESS_FINE_LOCATION) registerLocationListener()
            permissionActions[requestCode]?.invoke()
        } else {
            permissionActions.remove(requestCode)
            snackBarHelper.show(getString(R.string.need_permission))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == MaterialSearchView.REQUEST_VOICE && resultCode == Activity.RESULT_OK && data != null) {
            val matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (matches?.isNotEmpty() == true) {
                val text = matches[0]
                if (text.isNotEmpty()) searchView.setQuery(text, true)
            }
        } else multiplyImageActionMode.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        when {
            multiplyImageActionMode.mIsInActionMode -> multiplyImageActionMode.finishActionMode()
            searchView.isSearchOpen -> searchView.closeSearch()
            else -> if (!restoreRequest()) super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_image, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.action_search -> showSearchBar()
            R.id.action_favourite -> showFavouriteImage()
        }
        val result = multiplyImageActionMode.onOptionsItemSelected(item)
        return if (result) result else super.onOptionsItemSelected(item)
    }

    private fun showSearchBar(animate: Boolean = true) {
        showHistoryStationSuggestion()
        searchView.showSearch(animate)
        lastSearchStationName = null
    }

    private fun initToolbar() {
        searchView = findViewById(R.id.searchView)
        searchView.setOnLocationBtnClickListener { showNearestStationSuggestion() }
        searchView.setSubmitOnClick(true)
        searchView.setVoiceSearch(true)
        searchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {

            private var oldText: String = ""

            override fun onQueryTextSubmit(query: String) =
                    if (query.length > SEARCH_IMAGE_QUERY_THRESHOLD) {
                        showImage(query)
                        false
                    } else true

            override fun onQueryTextChange(newText: String) =
                    when (newText.length) {
                        SEARCH_STATION_QUERY_THRESHOLD -> {
                            if (oldText != newText || isNeedStartSearch) {
                                isNeedStartSearch = false
                                oldText = newText
                                showStationSuggestion(newText)
                                true
                            } else false
                        }
                        else -> false
                    }
        })
    }

    private fun initBottomBar() {
        fab = findViewById(R.id.fab)
        fab.setOnClickListener { showNearestImage() }
        bottomAppBar = findViewById(R.id.bottomAppBar)
        setSupportActionBar(bottomAppBar)
        snackBarHelper = SnackBarHelper(findViewById(R.id.snackBar))
    }

    private fun loadImageObservable(pattern: String) =
            Maybe.create(MaybeOnSubscribe<List<Image>?> {
                val data = assetsPhotoDB.getPhotoList(pattern)
                if (data != null)
                    it.onSuccess(data)
                else
                    this@ImageActivity.runOnUiThread {
                        Toast.makeText(this@ImageActivity, R.string.image_msg_not_found, Toast.LENGTH_SHORT).show()
                    }
            })


    private fun getNearestStation() = Observable.fromCallable {
        val location = SystemUtils.getLastKnownLocation(this)
        if (location != null) assetsPhotoDB.getNearestStation(location, 10000)
        else emptyList()
    }

    private fun registerLocationListener() {
        // Check permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            // If gps is not available
            if (gpsEnabled || networkEnabled) {
                // Request location
                locationManager.requestLocationUpdates(if (gpsEnabled) LocationManager.GPS_PROVIDER
                else LocationManager.NETWORK_PROVIDER, 1000L, 0F, locationService)
            }
        }
    }

    private fun unregisterLocationListener() {
        // Check permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.removeUpdates(locationService)
        }
    }

    private fun loadStationObservable(pattern: String) =
            Maybe.create(MaybeOnSubscribe<List<String>> {
                val stationNameList = assetsPhotoDB.getStationNameList(pattern)
                if (stationNameList?.isNotEmpty() == true) it.onSuccess(stationNameList)
            }).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())

    private fun getHistoryStation() = Observable.fromCallable {
        assetsPhotoDB.getStationHistoryList()
    }

    fun addImageListToFavourite(ids: List<Int>) {
        assetsPhotoDB.addImageListToFavourite(ids)
    }

    fun removeImageListFromFavourite(ids: List<Int>) {
        assetsPhotoDB.removeImageListFromFavourite(ids)
        showFavouriteImage(true)
    }

    private fun getFavouriteImageList() = Observable.fromCallable {
        assetsPhotoDB.getImageFavouriteList() ?: emptyList()
    }

    private fun saveRequest(request: String) {
        if (requestHistory.contains(request)) requestHistory.remove(request)
        requestHistory.add(request)
    }

    private fun restoreRequest(): Boolean {
        val size = requestHistory.size
        if (size <= 1) return false
        requestHistory.removeAt(size - 1)
        val stationName = requestHistory[size - 2]
        showImage(stationName)
        snackBarHelper.show(stationName)
        return true
    }

    fun isFavouriteScreen() = lastSearchStationName == IMAGE_FAVOURITE_LIST

    private fun String.formatStationName(): String {
        val startBrace = indexOf('(')
        if (startBrace != -1) return substring(0, startBrace)
        return this
    }

    private fun Double.formatDistance(): String {
        val value = this / 1000
        return when {
            value >= 10 -> String.format("%.1f км", value)
            value >= 1 -> String.format("%.2f км", value)
            else -> String.format("%s м", (value * 1000).roundToInt())
        }
    }

    class SnackBarHelper(private val snackBarView: ViewGroup) {

        private var mIsActive = false
        private var mIsQueue = false
        private var mMessage: String? = null
        private val mHandler = Handler(Looper.getMainLooper())
        private val textView = snackBarView.findViewById<TextView>(R.id.textView)

        fun show(message: String, duration: Long = SHORT_DURATION) {
            if ((mIsActive && mMessage == message) || mIsQueue) return
            if (mIsActive) mIsQueue = true
            else mIsActive = true
            mMessage = message

            val from = 0F
            val to = 1F

            val onAnimationUpdate: (ValueAnimator) -> Unit = {
                val value = it.animatedValue as Float
                snackBarView.alpha = value
                snackBarView.requestLayout()
            }

            textView.text = message
            snackBarView.visibility = View.VISIBLE

            ViewUtils.createAnimation(from, to, TIME_ANIMATION_APPEARANCE, onAnimationUpdate) {
                mHandler.postDelayed({
                    ViewUtils.createAnimation(to, from, TIME_ANIMATION_DISAPPEARANCE, onAnimationUpdate,
                            onAnimationEnd = {
                                snackBarView.visibility = View.GONE
                                mIsActive = mIsQueue
                                mIsQueue = false
                            }).start()
                }, duration)
            }.start()
        }


        companion object {
            const val SHORT_DURATION = 2000L
            const val MIDDLE_DURATION = 3000L
            const val LONG_DURATION = 5000L

            const val TIME_ANIMATION_APPEARANCE = 150L
            const val TIME_ANIMATION_DISAPPEARANCE = 150L
        }
    }

    companion object {
        const val SEARCH_STATION_QUERY_THRESHOLD = 2
        const val SEARCH_IMAGE_QUERY_THRESHOLD = 2

        const val REQUEST_ACCESS_FINE_LOCATION = 123
        const val REQUEST_WRITE_EXTERNAL_STORAGE = 124

        const val IMAGE_FAVOURITE_LIST = "imageFavouriteList"
    }
}

class DoAsync<T>(
        private val actionInBackground: () -> T,
        private val subscribe: (T) -> Unit
) : AsyncTask<Void, Void, T>() {
    override fun doInBackground(vararg params: Void?): T? {
        actionInBackground()
        return null
    }

    override fun onPostExecute(result: T) {
        super.onPostExecute(result)
        subscribe(result)
    }
}
