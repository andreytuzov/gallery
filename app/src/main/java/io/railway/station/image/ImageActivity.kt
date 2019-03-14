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
import com.arasthel.spannedgridlayoutmanager.SpannedGridLayoutManager
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.core.CrashlyticsCore
import com.miguelcatalan.materialsearchview.MaterialSearchView
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import io.fabric.sdk.android.Fabric
import io.railway.station.image.adapters.ImageRecyclerAdapter
import io.railway.station.image.data.StationViewModel
import io.railway.station.image.database.photos.AssetsPhotoDB
import io.railway.station.image.database.photos.Image
import io.railway.station.image.helpers.AppCompatBottomAppBar
import io.railway.station.image.helpers.MultiplyImageActionModeController
import io.railway.station.image.utils.*
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.stellio.player.Helpers.setOnClickDebounceListener

class ImageActivity : RxAppCompatActivity() {

    lateinit var adapter: ImageRecyclerAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var stationViewModel: StationViewModel

    private val requestHistory = mutableListOf<String>()

    private lateinit var searchView: MaterialSearchView
    private lateinit var bottomAppBar: AppCompatBottomAppBar
    private lateinit var fab: FloatingActionButton
    lateinit var snackBarHelper: SnackBarHelper
        private set
    private lateinit var root: View
    private val multiplyImageActionMode by lazy { MultiplyImageActionModeController(bottomAppBar, fab, this) }

    // To avoid extra requests
    private var currentStationName: String? = null
    private var permissionActions: MutableMap<Int, () -> Unit> = mutableMapOf()

    private val turnOnGpsSubject by lazy { PublishSubject.create<Boolean>() }
    private var locationSubject = PublishSubject.create<Location>()

    private var isLocationRegistred = false

    private val locationService by lazy {

        object : LocationListener {
            override fun onLocationChanged(location: Location?) {
                if (location != null && !locationSubject.hasComplete()) {
                    locationSubject.onNext(location)
                    locationSubject.onComplete()
                }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String?) {}
            override fun onProviderDisabled(provider: String?) {}
        }
    }

    private var compositeDisposable = CompositeDisposable()
    private var showNearestImageDisposable: Disposable? = null
    private var showNearestStationDisposable: Disposable? = null
    private var isNeedStartSearch: Boolean = false

    private fun getLocationObservable(): Observable<Location> {
        locationSubject.onComplete()
        locationSubject = PublishSubject.create()
        return locationSubject
                .doOnSubscribe { registerLocationListener() }
                .doFinally { unregisterLocationListener() }
    }

    private fun showNearestStationSuggestion() {
        if (isLocationRegistred) return
        executeAfterGetPermission(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_ACCESS_FINE_LOCATION) {
            executeAfterTurnOnGps {
                showNearestStationDisposable = getLocationObservable()
                        .flatMap { stationViewModel.getNearestStationByLocation(it) }
                        .io(bindUntilEvent<Any>(ActivityEvent.DESTROY))
                        .subscribe({
                            if (it != null) {
                                searchView.setSuggestions(it.map { it.first }.toTypedArray())
                                isNeedStartSearch = true
                                searchView.hideKeyboard(searchView)
                            }
                        }, {
                            K.e("Error during getting nearest station", it)
                        })
            }
        }
    }

    private fun showStationSuggestion(pattern: String) {
        val disposable = stationViewModel.getStationByPattern(pattern)
                .io(bindUntilEvent<Any>(ActivityEvent.DESTROY))
                .subscribe({
                    if (it.isNotEmpty()) searchView.setSuggestions(it.toTypedArray())
                }, {
                    K.e("Error during getting list of station name", it)
                })
        compositeDisposable.add(disposable)
    }


    private fun showHistoryStationSuggestion() {
        val disposable = stationViewModel.getHistoryStation()
                .io(bindUntilEvent<Any>(ActivityEvent.DESTROY))
                .subscribe({
                    if (it.isNotEmpty()) {
                        searchView.setSuggestions(it.map { it.station.name }.toTypedArray())
                    }
                }, {
                    K.e("Error during get history station", it)
                })
        compositeDisposable.add(disposable)
    }

    private fun showImage(stationName: String) {
        if (stationName != currentStationName) {
            if (stationName == IMAGE_FAVOURITE_LIST) showFavouriteImage()
            else {
                val disposable = stationViewModel.getImageByPattern(stationName)
                        .io(bindUntilEvent<Any>(ActivityEvent.DESTROY))
                        .subscribe({
                            updateStationData(it, stationName)
                            val disposable = stationViewModel.saveStationToHistory(stationName)
                                    .io(bindUntilEvent<Any>(ActivityEvent.DESTROY))
                                    .emptySubscribe("Error during save station to history")
                            compositeDisposable.add(disposable)
                        }, {
                            K.e("Error during loading image", it)
                        })
                compositeDisposable.add(disposable)
            }
        }
    }

    fun showNearestImage() {
        if (isLocationRegistred) {
            unregisterLocationListener()
            if (showNearestImageDisposable?.isDisposed == false) showNearestImageDisposable?.dispose()
            showNearestImageDisposable = null
            return
        }
        executeAfterGetPermission(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_ACCESS_FINE_LOCATION) {
            executeAfterTurnOnGps {
                showNearestImageDisposable = getLocationObservable()
                        .flatMap { stationViewModel.getNearestImageByLocation(it) }
                        .io(bindUntilEvent<Any>(ActivityEvent.DESTROY))
                        .subscribe({
                            updateStationData(it.second, it.first)
                        }, {
                            snackBarHelper.show(getString(R.string.image_msg_not_found))
                            K.e("Error during find nearest image", it)
                        })
            }
        }
    }

    private fun showFavouriteImage(isImagesUpdate: Boolean = false) {
        if (isImagesUpdate || currentStationName != IMAGE_FAVOURITE_LIST) {
            val disposable = stationViewModel.getFavouriteImage()
                    .io(bindUntilEvent<Any>(ActivityEvent.DESTROY))
                    .subscribe({ updateStationData(it, IMAGE_FAVOURITE_LIST) },
                            { K.e("Error during get favourite image", it) })
            compositeDisposable.add(disposable)
        }
    }

    private fun updateStationData(images: List<Image>?, stationName: String) {
        searchView.setSuggestions(null)
        if (images?.isNotEmpty() != true) snackBarHelper.show(getString(R.string.image_msg_not_found))
        else {
            val msg = if (stationName.isFavouriteScreen()) getString(R.string.favourite)
            else stationName.formatStationName().limitWithLastConsonant(AssetsPhotoDB.STATION_MAX_LENGTH)
            snackBarHelper.show(msg)
            saveStationRequest(stationName)
            adapter.updateData(images)
        }
    }

    private fun saveStationRequest(stationName: String) {
        currentStationName = stationName
        if (requestHistory.contains(stationName)) requestHistory.remove(stationName)
        requestHistory.add(stationName)
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
        val maxSpansSize = App.pref.getInt(PREF_MAX_IMAGE_SIZE, 3)
        adapter = ImageRecyclerAdapter(this, maxSpansSize)
        adapter.setMultiplyImageActionModeController(multiplyImageActionMode)
        recyclerView.adapter = adapter
        val layoutManager = SpannedGridLayoutManager(SpannedGridLayoutManager.Orientation.VERTICAL, maxSpansSize)
        layoutManager.spanSizeLookup = SpannedGridLayoutManager.SpanSizeLookup {
            adapter.getSpanSizeByPosition(it)
        }
        recyclerView.layoutManager = layoutManager

        stationViewModel = StationViewModel()
        lifecycle.addObserver(stationViewModel)

        searchView.post {
            showSearchBar(false)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterLocationListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!compositeDisposable.isDisposed) compositeDisposable.dispose()
        if (showNearestImageDisposable?.isDisposed == false) showNearestImageDisposable?.dispose()
        if (showNearestStationDisposable?.isDisposed == false) showNearestStationDisposable?.dispose()
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

    /**
     * If gps is not enabled then we show the dialog to enable gps and result goes into onActivityResult method
     */
    fun executeAfterTurnOnGps(block: () -> Unit) {
        val disposable = turnOnGpsSubject
                .firstElement()
                .subscribe {
                    if (it) block()
                    else snackBarHelper.show(getString(R.string.request_turn_on_gps))
                }
        LocationUtils.checkGpsEnabled(this, turnOnGpsSubject)
        compositeDisposable.add(disposable)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
        } else if (requestCode == REQUEST_TURN_ON_GPS) {
            turnOnGpsSubject.onNext(resultCode == Activity.RESULT_OK)
        } else multiplyImageActionMode.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        when {
            multiplyImageActionMode.mIsInActionMode -> multiplyImageActionMode.finishActionMode()
            searchView.isSearchOpen -> searchView.closeSearch()
            else -> if (!restoreStationRequest()) super.onBackPressed()
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
        searchView.setOnSearchViewListener(object : MaterialSearchView.SearchViewListener {
            override fun onSearchViewShown() {
            }

            override fun onSearchViewClosed() {
                if (showNearestStationDisposable?.isDisposed == false) showNearestStationDisposable?.dispose()
            }
        })
    }

    private fun initBottomBar() {
        fab = findViewById(R.id.fab)
        fab.setOnClickDebounceListener { showNearestImage() }
        bottomAppBar = findViewById(R.id.bottomAppBar)
        setSupportActionBar(bottomAppBar)
        snackBarHelper = SnackBarHelper(findViewById(R.id.snackBar))
    }


    private fun registerLocationListener() {
        if (isLocationRegistred) return
        // Check permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            // If gps is not available
            if (gpsEnabled || networkEnabled) {
                isLocationRegistred = true
                App.handler.post {
                    searchView.setIsProgressBarVisible(true)
                }
                // Request location
                if (gpsEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0F, locationService, Looper.getMainLooper())
                }
                if (networkEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0F, locationService, Looper.getMainLooper())
                }
                fab.isActivated = true
            }
        }
    }

    private fun unregisterLocationListener() {
        if (!isLocationRegistred) return
        // Check permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            isLocationRegistred = false
            App.handler.post {
                searchView.setIsProgressBarVisible(false)
            }
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.removeUpdates(locationService)
            fab.isActivated = false
        }
    }

    fun addImageListToFavourite(ids: List<Int>) {
        stationViewModel.addImageToFavourite(ids)
    }

    fun removeImageListFromFavourite(ids: List<Int>) {
        stationViewModel.removeImageFromFavourite(ids)
        showFavouriteImage(true)
    }

    private fun restoreStationRequest(): Boolean {
        val size = requestHistory.size
        if (size <= 1) return false
        requestHistory.removeAt(size - 1)
        val stationName = requestHistory[size - 2]
        showImage(stationName)
        snackBarHelper.show(stationName)
        return true
    }

    fun isFavouriteScreen() = currentStationName.isFavouriteScreen()

    fun String?.isFavouriteScreen() = this == IMAGE_FAVOURITE_LIST

    private fun String.formatStationName(): String {
        val startBrace = indexOf('(')
        if (startBrace != -1) return substring(0, startBrace)
        return this
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
        const val REQUEST_TURN_ON_GPS = 200

        const val IMAGE_FAVOURITE_LIST = "imageFavouriteList"

        private const val PREF_MAX_IMAGE_SIZE = "max_image_size"
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
