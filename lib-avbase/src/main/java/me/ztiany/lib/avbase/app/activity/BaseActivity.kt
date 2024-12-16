package me.ztiany.lib.avbase.app.activity

import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import androidx.viewbinding.ViewBinding
import com.android.base.delegate.State
import com.android.base.delegate.simpl.DelegateActivity
import me.ztiany.lib.avbase.utils.ui.inflateBindingWithParameterizedType
import timber.log.Timber

abstract class BaseActivity<VB : ViewBinding> : DelegateActivity() {

    private lateinit var _binding: VB

    protected val binding: VB
        get() = _binding

    override fun provideLayout(): View {
        _binding = inflateBindingWithParameterizedType(layoutInflater)
        return _binding.root
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun isDestroyed(): Boolean {
        return if (Build.VERSION.SDK_INT >= 17) {
            super.isDestroyed()
        } else {
            getCurrentState() === State.DESTROY
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (BackHandlerHelper.handleBackPress(this)) {
            Timber.d("onBackPressed() called but child fragment handle it")
        } else {
            superOnBackPressed()
        }
    }

    protected open fun superOnBackPressed() {
        super.onBackPressed()
    }

}