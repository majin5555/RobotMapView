package com.siasun.dianshi

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Environment
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogx.style.MaterialStyle
import com.siasun.dianshi.framework.helper.AppHelper
import com.siasun.dianshi.framework.manager.AppManager
import com.tencent.mars.xlog.Log
import com.tencent.mmkv.MMKV
import kotlin.properties.Delegates

class CRLApplication : Application() {
    companion object {
        var instance: CRLApplication by Delegates.notNull()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
//        // xCrash文件存储路径
//        val crashPath = Environment.getExternalStorageDirectory().absolutePath + "/padFiles_new/Log/xcrash"
//        // xCrash自定义设置
//        val initParameters = XCrash.InitParameters()
//            .setLogDir(crashPath)
//            .setAppVersion("${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})")
//        // 初始化xCrash
//        XCrash.init(this, initParameters)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppHelper.init(this, BuildConfig.DEBUG)
        AppManager.init(this)
        MMKV.initialize(this)
        initDialogX()
    }

    private fun initDialogX() {
        DialogX.init(this)
        //开启调试模式，在部分情况下会使用 Log 输出日志信息
        DialogX.DEBUGMODE = true
        //设置主题样式
        DialogX.globalStyle = MaterialStyle.style()
        //设置亮色/暗色（在启动下一个对话框时生效）
        DialogX.globalTheme = DialogX.THEME.LIGHT
        //设置对话框最大宽度（单位为像素）
        DialogX.dialogMaxWidth = 640
    }

    override fun onTerminate() {
        Log.appenderClose()
        super.onTerminate()
    }
}