package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import java.lang.ref.WeakReference

/**
 * 过门View
 */
@SuppressLint("ViewConstructor")
class CrossDoorView(
    context: Context?, val parent: WeakReference<MapView>
) : SlamWareBaseView(context, parent) {


    // 当前工作模式
    private var currentWorkMode: MapView.WorkMode = MapView.WorkMode.MODE_SHOW_MAP


    /***
     * 1重点虚拟墙
     * 2虚拟门
     * 3普通虚拟墙
     */
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mapView = parent.get() ?: return

    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: MapView.WorkMode) {
        currentWorkMode = mode

        postInvalidate()
    }

    /**
     * 设置是否启用绘制
     */
    fun setDrawingEnabled(enabled: Boolean) {
        postInvalidate()
    }
}


