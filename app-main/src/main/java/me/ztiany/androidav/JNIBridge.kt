package me.ztiany.androidav

import android.content.Context
import android.content.res.AssetManager

/**
 *@author Ztiany
 */
class JNIBridge(
    context: Context
) {

    init {
        System.loadLibrary("androidav")
        initNative(context, context.assets)
    }

    private external fun initNative(context: Context, assetManager: AssetManager)

}