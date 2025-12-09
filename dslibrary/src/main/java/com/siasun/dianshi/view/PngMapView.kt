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
    // 优化：使用伴生对象创建Paint实例，避免重复创建
    companion object {
        private val mPaint: Paint by lazy {
            Paint().apply {
                isDither = false
                color = Color.BLUE
                isAntiAlias = true // 添加抗锯齿，提升绘制质量
            }
        }
    }
    
    private val mOuterMatrix = Matrix() // 使用val而不是var，避免重复创建
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
        // 优化：复用Matrix对象，避免重复创建
        mOuterMatrix.set(matrix)
        postInvalidate()
    }

    /**
     * 设置地图
     *
     * @param bitmap
     */
    fun setBitmap(bitmap: Bitmap) {
        // 清理旧的Bitmap资源
        if (mPngBitmap != null && mPngBitmap != bitmap && !mPngBitmap!!.isRecycled) {
            mPngBitmap!!.recycle()
        }
        mPngBitmap = bitmap
        postInvalidate()
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        
        // 清理资源，防止内存泄漏
        if (mPngBitmap != null && !mPngBitmap!!.isRecycled) {
            mPngBitmap!!.recycle()
            mPngBitmap = null
        }
        // 重置Matrix
        mOuterMatrix.reset()
    }
}
