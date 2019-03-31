package io.railway.station.image.helpers

import android.Manifest
import android.content.Intent
import android.os.Environment
import android.support.design.bottomappbar.BottomAppBar
import android.support.design.widget.FloatingActionButton
import android.view.MenuItem
import com.trello.rxlifecycle2.android.ActivityEvent
import io.reactivex.Observable
import io.railway.station.image.ImageActivity
import io.railway.station.image.R
import io.railway.station.image.database.photos.Image
import io.railway.station.image.utils.*
import io.stellio.player.Helpers.setOnClickDebounceListener
import java.lang.StringBuilder

class MultiplyImageActionModeController(
        private val mBottomAppBar: AppCompatBottomAppBar,
        private val mFab: FloatingActionButton,
        private val mActivity: ImageActivity
) {

    private var mSelectedData: Map<Int, Image>? = null

    var mIsInActionMode: Boolean = false
        private set

    private val mStartOffset = mBottomAppBar.cradleVerticalOffset
    private val mOffset = RUtils.convertDpToPixels(40, mActivity)

    fun startActionMode(selectedData: Map<Int, Image>) {
        if (!mIsInActionMode) {
            mIsInActionMode = true
            mBottomAppBar.show()
            mBottomAppBar.hideOnScroll = false
            mFab.setImageDrawable(RUtils.getDrawableCompat(R.drawable.ic_action_mode_close, mActivity))
            createTransitionAnimation(true, if (mActivity.isFavouriteScreen()) R.menu.activity_mode_favourite_image
            else R.menu.activity_mode_image).start()
            mFab.setOnClickListener { finishActionMode() }
            updateSelectedData(selectedData)
        }
    }

    fun finishActionMode() {
        mActivity.adapter.resetActionModeData()
        mIsInActionMode = false
        mBottomAppBar.hideOnScroll = true
        mFab.setImageDrawable(RUtils.getDrawableCompat(R.drawable.ic_menu_search_location, mActivity))
        createTransitionAnimation(false, mActivity.getMenuResourceId()).start()
        mFab.setOnClickDebounceListener { mActivity.showNearestImage() }
    }

    fun updateSelectedData(selectedData: Map<Int, Image>) {
        if (mIsInActionMode) {
            mSelectedData = selectedData
        }
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_mode_download -> {
                if (NetUtils.hasConnection(mActivity)) {
                    downloadImages(mSelectedData!!.values, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path)
                    finishActionMode()
                } else mActivity.snackBarHelper.show(mActivity.getString(R.string.not_internet_connection))
                return true
            }
            R.id.action_mode_share -> {
                shareImages(mSelectedData!!.values)
                finishActionMode()
                return true
            }
            R.id.action_mode_favourite -> {
                addImagesToFavourite()
                finishActionMode()
                return true
            }
            R.id.action_mode_favourite_delete -> {
                removeImagesFromFavourite()
                finishActionMode()
                return true
            }
        }
        return false
    }

    private fun addImagesToFavourite() {
        if (mSelectedData != null) {
            Observable.fromCallable {
                mActivity.addImageListToFavourite(mSelectedData!!.keys.toList())
            }.io(mActivity.bindUntilEvent<Any>(ActivityEvent.DESTROY))
                    .emptySubscribe("Error during add images to favourite")
        }
    }

    private fun removeImagesFromFavourite() {
        if (mSelectedData != null) {
            Observable.fromCallable {
                mActivity.removeImageListFromFavourite(mSelectedData!!.keys.toList())
            }.io(mActivity.bindUntilEvent<Any>(ActivityEvent.DESTROY))
                    .emptySubscribe("Error during add images to favourite")
        }
    }

    private fun downloadImages(images: Collection<Image>, directoryPath: String) {
        mActivity.executeAfterGetPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, ImageActivity.REQUEST_WRITE_EXTERNAL_STORAGE) {
            ImageUtils.downloadImages(images, directoryPath)
                    .io(mActivity.bindUntilEvent<Any>(ActivityEvent.DESTROY))
                    .subscribe({
                        val msgId = if (it) R.string.all_image_loaded else R.string.error_load_image
                        mActivity.snackBarHelper.show(mActivity.getString(msgId))
                    }, {
                        mActivity.snackBarHelper.show(mActivity.getString(R.string.error_load_image))
                        K.e("Error during download images: $images", it)
                    })
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    }

    private fun shareImages(images: Collection<Image>) {
        val urls = StringBuilder()
        for (image in images) {
            urls.appendln(image.getFullImageUrl())
        }
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, urls.toString())
        mActivity.startActivity(Intent.createChooser(intent, "Поделиться"))
    }

    private fun createTransitionAnimation(isFabInCenter: Boolean = true, menuId: Int) =
            ViewUtils.createAnimation(mStartOffset, mOffset, ANIM_FAB_START_DURATION, {
                mBottomAppBar.cradleVerticalOffset = it.animatedValue as Float
                mBottomAppBar.requestLayout()
            }, {}, {
                mBottomAppBar.fabAlignmentMode =
                        if (isFabInCenter) BottomAppBar.FAB_ALIGNMENT_MODE_END else BottomAppBar.FAB_ALIGNMENT_MODE_CENTER
                mBottomAppBar.replaceMenu(menuId)
                ViewUtils.createAnimation(mOffset, mStartOffset, ANIM_FAB_END_DURATION, {
                    mBottomAppBar.cradleVerticalOffset = it.animatedValue as Float
                    mBottomAppBar.requestLayout()
                }).start()
            })

    companion object {
        private const val ANIM_FAB_START_DURATION = 100L
        private const val ANIM_FAB_END_DURATION = 150L
    }
}