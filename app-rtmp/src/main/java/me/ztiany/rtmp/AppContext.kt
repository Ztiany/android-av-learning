package me.ztiany.rtmp

import android.app.Application
import com.blankj.utilcode.util.Utils
import me.ztiany.lib.avbase.utils.CrashHandler
import timber.log.Timber

class AppContext : Application() {

    override fun onCreate() {
        super.onCreate()
        //工具类库
        Utils.init(this)
        //配置调试工具
        Timber.plant(Timber.DebugTree())
        //日志收集
        CrashHandler.register(this)
    }

}