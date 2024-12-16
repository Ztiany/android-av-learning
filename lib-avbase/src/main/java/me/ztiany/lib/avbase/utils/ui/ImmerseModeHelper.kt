package me.ztiany.lib.avbase.utils.ui

import android.view.MotionEvent
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import timber.log.Timber

class ImmerseModeHelper(private val activity: FragmentActivity) {

    private val windowInsetsController by lazy {
        WindowCompat.getInsetsController(activity.window, activity.window.decorView)
    }

    private val restoreAction = Runnable { setFullScreen() }

    private val statusBarHeight by lazy {
        SystemBarCompat.getStatusBarHeightIgnoreVisibility(activity)
    }

    private val navigationBarHeight by lazy {
        SystemBarCompat.getNavigationBarHeightIgnoreVisibility(activity)
    }

    private val screenHeight by lazy {
        SystemBarCompat.getNavigationBarHeightIgnoreVisibility(activity)
    }

    init {
        windowInsetsController.addOnControllableInsetsChangedListener { controller, typeMask ->
            Timber.d("addOnControllableInsetsChangedListener controller = $controller typeMask = $typeMask")
        }
    }

    fun setFullScreen() {
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        //extend to system bars
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        displayInNotch()
    }

    private fun displayInNotch() {
        if (AndroidVersion.atLeast(28)) {
            Timber.d("displayInNotch")
            val window = activity.window
            val attributes = window.attributes
            attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = attributes
        }
    }

    fun dispatchTouchEvent(motionEvent: MotionEvent) {
        val action = motionEvent.action
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            activity.window.decorView.postDelayed(restoreAction, 3000)
        } else if (action == MotionEvent.ACTION_DOWN) {
            val y = motionEvent.y
            if (y < statusBarHeight || y > screenHeight - navigationBarHeight) {
                activity.window.decorView.removeCallbacks(restoreAction)
            }
        }
    }

    fun onResume() {
        setFullScreen()
        activity.window.decorView.postDelayed(restoreAction, 500)
    }

}