package com.siasun.dianshi.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.view.ViewGroup
import com.siasun.dianshi.utils.RadianUtil
import java.lang.ref.WeakReference

abstract class SlamWareBaseView(context: Context?, parent: WeakReference<MapView>) :
    ViewGroup(context) {
    var mParent: WeakReference<MapView>
    var scale: Float protected set
    var mRotation: Float//旋转角度
    var mMatrix: Matrix//矩阵


    init {
        setBackgroundColor(Color.TRANSPARENT)
        setWillNotDraw(false)
        mParent = parent
        scale = 1.0f
        mRotation = 0f
        mMatrix = Matrix()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {}
    open fun setMatrix(mMatrix: Matrix) {
        this.mMatrix = mMatrix
        postInvalidate()
    }

    open fun setMatrixWithScale(matrix: Matrix, scale: Float) {
        mMatrix = matrix
        this.scale = scale
        postInvalidate()
    }

    open fun setMatrixWithRotation(matrix: Matrix, rotation: Float) {
        mMatrix = matrix
        mRotation += RadianUtil.toAngel(rotation)
        postInvalidate()
    }

    /**
     * 绘制标签
     * @param canvas 画布
     * @param text 标签文本
     * @param x 图标中心 x 坐标
     * @param y 图标中心 y 坐标
     */
    fun drawLabel(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint) {
        canvas.drawText(text, x, y, paint)
    }

    fun drawCircle(canvas: Canvas, x: Float, y: Float, radius: Float, paint: Paint) {
        canvas.drawCircle(x, y, radius, paint)
    }
}
