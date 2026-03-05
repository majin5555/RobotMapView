package com.siasun.dianshi.view

import android.content.Context
import java.lang.ref.WeakReference

/******************************************
 * 类描述：
 *
 * @author: why
 * @time: 2026/3/5 11:45
 ******************************************/


class RFIDView(context: Context?, var parent: WeakReference<MapView>) :
    SlamWareBaseView<MapView>(context, parent) {

    // 当前工作模式
    private var currentWorkMode = WorkMode.MODE_SHOW_MAP

    // 控制是否绘制
    private var isDrawingEnabled: Boolean = true

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        currentWorkMode = mode
        // 根据工作模式调整绘制和交互行为
        postInvalidate()
    }

    /**
     * 设置是否启用绘制
     */
    fun setDrawingEnabled(enabled: Boolean) {
        this.isDrawingEnabled = enabled
        postInvalidate()
    }
}
