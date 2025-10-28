package com.siasun.dianshi.framework.log

import android.app.Application
import com.tencent.mars.xlog.Log
import com.tencent.mars.xlog.Xlog

/**
 * XLog的管理工具类
 */
class XLogger {
    /**
     * 初始化xlog
     */
    fun init(
        context: Application,
        isDebug: Boolean,
        logPath: String,
        namePrefix: String = "sumTea"
    ) {
        System.loadLibrary("c++_shared")
        System.loadLibrary("marsxlog")

        val cachePath = context.filesDir.absolutePath + "/xlog"

        if (isDebug) {
            Xlog.open(
                false,
                Xlog.LEVEL_ALL,
                Xlog.AppednerModeAsync,
                cachePath,
                logPath,
                namePrefix,
                ""
            )
        } else {
            Xlog.open(
                false,
                Xlog.LEVEL_INFO,
                Xlog.AppednerModeAsync,
                cachePath,
                logPath,
                namePrefix,
                ""
            )
        }

        val xlog = Xlog()
        xlog.setConsoleLogOpen(0, false)
        Log.setLogImp(xlog)
    }

    fun v(tag: String, msg: String) {
        Log.v(tag, msg)
    }

    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    fun i(tag: String, msg: String) {
        Log.i(tag, msg)
    }

    fun w(tag: String, msg: String) {
        Log.w(tag, msg)
    }

    fun e(tag: String, msg: String) {
        Log.e(tag, msg)
    }

    /**
     * 异常日志打印
     */
    fun logThrowable(tag: String, tr: Throwable, msg: String) {
        Log.printErrStackTrace(tag, tr, msg)
    }

    /**
     * 将缓存的日志刷新到文件内
     */
    fun flushLog() {
        Log.appenderFlush()
    }

    /**
     * 关闭日志，退出应用时调用
     */
    fun close() {
        Log.appenderClose()
    }
}
