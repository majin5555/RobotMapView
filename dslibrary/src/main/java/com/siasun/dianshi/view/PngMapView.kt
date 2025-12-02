package com.siasun.dianshi.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * 绘制Png地图
 */
class PngMapView : View {
    private var mPaint: Paint = Paint()
    private var mOuterMatrix = Matrix()
    private var mPngBitmap: Bitmap? = null

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        setBackgroundColor(Color.TRANSPARENT)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        mPaint.isDither = false
        mPaint.setColor(Color.BLUE)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mPngBitmap?.also {
            canvas.save()
            canvas.drawBitmap(it, mOuterMatrix, mPaint)
            canvas.restore()
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {}
    fun setMatrix(matrix: Matrix) {
        mOuterMatrix = matrix
        postInvalidate()
    }

    /**
     * 设置地图
     *
     * @param bitmap
     */
    fun setBitmap(bitmap: Bitmap) {
        mPngBitmap = bitmap
        postInvalidate()
    }
}
