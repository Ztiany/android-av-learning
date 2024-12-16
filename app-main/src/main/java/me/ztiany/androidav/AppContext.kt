package me.ztiany.androidav

import android.app.Application
import android.content.Context
import me.ztiany.lib.avbase.utils.CrashHandler
import timber.log.Timber
import kotlin.properties.Delegates

class AppContext : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this

        //加载 native 库
        jniBridge = JNIBridge(this)

        //配置调试工具
        Timber.plant(Timber.DebugTree())
        CrashHandler.register(this)
    }

    companion object {

        private var appContext by Delegates.notNull<AppContext>()

        private var jniBridge by Delegates.notNull<JNIBridge>()

        @JvmStatic
        fun get(): Context {
            return appContext
        }

        fun getJNIBridge(): JNIBridge {
            return jniBridge
        }

    }

}