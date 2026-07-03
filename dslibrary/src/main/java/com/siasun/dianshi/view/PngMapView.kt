package com.siasun.dianshi.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withSave

/**
 * 绘制Png地图
 */
class PngMapView : View {
    // 优化：使用伴生对象创建Paint实例，避免重复创建
    companion object {
//        private val mPaint: Paint by lazy {
//            Paint().apply {
//                isDither = false
//                color = Color.BLUE
//                isAntiAlias = true // 添加抗锯齿，提升绘制质量
//            }
//        }
    }
    
    private val mOuterMatrix = Matrix() // 使用val而不是var，避免重复创建
    private var mPngBitmap: Bitmap? = null
    
    // 偏移量
    private var offsetX = 0f
    private var offsetY = 0f

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        setBackgroundColor(Color.WHITE)
        // 强制关闭硬件加速，转为 CPU 绘制，彻底解决 libGLES_mali.so (libhwui) 长时间高频渲染崩溃
        setLayerType(LAYER_TYPE_SOFTWARE, null) 
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mPngBitmap?.also {
            //使用withsave替换原本成对save和restore，并删除临时Matrix，通过concat构建，降低内存抖动
            canvas.withSave {
                // 如果有偏移，先应用偏移
                if (offsetX != 0f || offsetY != 0f) {
                    concat(mOuterMatrix)          // 组合矩阵
                    translate(offsetX, offsetY)   // 直接修改画布变换
                } else {
                    concat(mOuterMatrix)
                }
                drawBitmap(it, 0f, 0f, null)
            }
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
    fun setBitmap(bitmap: Bitmap?) {
        // 交由 Glide 管理 Bitmap，不要手动 recycle
        mPngBitmap = bitmap
        // 重置偏移
        offsetX = 0f
        offsetY = 0f
        postInvalidate()
    }

    /**
     * 设置绘制偏移量
     */
    fun setOffset(x: Float, y: Float) {
        if (offsetX != x || offsetY != y) {
            offsetX = x
            offsetY = y
            postInvalidate()
        }
    }

    /**
     * 获取地图位图宽度
     */
    fun getBitmapWidth(): Int {
        return mPngBitmap?.width ?: 0
    }

    /**
     * 获取地图位图高度
     */
    fun getBitmapHeight(): Int {
        return mPngBitmap?.height ?: 0
    }

    /**
     * 主动释放资源，应在 MapView 销毁时调用
     */
    fun release() {
        // 交由 Glide 管理 Bitmap，不要手动 recycle
        mPngBitmap = null
        mOuterMatrix.reset()
        offsetX = 0f
        offsetY = 0f
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()  // 复用清理逻辑
//        // 清理资源，防止内存泄漏
//        if (mPngBitmap != null && !mPngBitmap!!.isRecycled) {
//            mPngBitmap!!.recycle()
//            mPngBitmap = null
//        }
//        // 重置Matrix
//        mOuterMatrix.reset()
    }
}
