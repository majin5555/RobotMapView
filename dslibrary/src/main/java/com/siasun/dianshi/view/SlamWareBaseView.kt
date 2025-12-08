package com.siasun.dianshi.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
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
     * 绘制线
     */
    fun drawLine(canvas: Canvas, mStart: PointF, mEnd: PointF, paint: Paint) {
        canvas.drawLine(
            mStart.x, mStart.y, mEnd.x, mEnd.y, paint
        )
    }

    /**
     * 绘制路径
     */
    fun drawPath(canvas: Canvas, path: Path, paint: Paint) {
        canvas.drawPath(path, paint)
    }

    /**
     * 绘制标签
     * @param canvas 画布
     * @param text 标签文本
     * @param x 图标中心 x 坐标
     * @param y 图标中心 y 坐标
     */
    fun drawLabel(canvas: Canvas, text: String, mPoint: PointF, paint: Paint) {
        canvas.drawText(text, mPoint.x, mPoint.y, paint)
    }

    /**
     * 绘制圆
     */
    fun drawCircle(canvas: Canvas, mPoint: PointF, radius: Float, paint: Paint) {
        canvas.drawCircle(mPoint.x, mPoint.y, radius, paint)
    }

    /**
     * 绘制等边三角形（朝上的）
     */
    fun drawTriangle(canvas: Canvas, mPoint: PointF, paint: Paint, size: Float = 10f) {
        val path = Path().apply {
            moveTo(mPoint.x, mPoint.y - size) // 顶点（上）
            lineTo(mPoint.x - size, mPoint.y + size) // 左下
            lineTo(mPoint.x + size, mPoint.y + size) // 右下
            close()
        }
        canvas.drawPath(path, paint)
    }

    /**
     * 绘制菱形
     */
    fun drawDiamond(canvas: Canvas, mPoint: PointF, paint: Paint, size: Float = 10f) {
        val path = Path().apply {
            moveTo(mPoint.x, mPoint.y - size) // 上
            lineTo(mPoint.x + size, mPoint.y) // 右
            lineTo(mPoint.x, mPoint.y + size) // 下
            lineTo(mPoint.x - size, mPoint.y) // 左
            close()
        }
        canvas.drawPath(path, paint)
    }

    /**
     * 绘制矩形
     */
    fun drawRect(
        canvas: Canvas, mPoint: PointF, paint: Paint, width: Float = 20f, height: Float = 20f
    ) {
        val left = mPoint.x - width / 2
        val top = mPoint.y - height / 2
        val right = mPoint.x + width / 2
        val bottom = mPoint.y + height / 2
        canvas.drawRect(left, top, right, bottom, paint)
    }

}
