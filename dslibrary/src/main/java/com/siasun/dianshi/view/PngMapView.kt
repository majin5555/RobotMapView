package com.siasun.dianshi.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withSave

/**
 * 绘制Png地图
 *
 * 方案A（单 Surface）说明：
 * 底图不再只由本 View 绘制，MapOutline3DGL 也会直接读取这里的位图/矩阵/偏移/缩放，
 * 在同一个 GL Surface 上与点云、机器人一起输出。为兼容两种绘制路径，本类同时提供：
 *  - [copyMatrixInto] / [pngOffsetX] / [pngOffsetY] / [pngDrawScale] / [pngBitmap] 给 GL 侧读取
 *  - [pngBitmapVersion] 供 GL 侧判断是否需要重传纹理
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

    // 矩阵读写锁：矩阵由 UI 线程写入，渲染线程（GL 线程 / 兜底 Canvas 线程）读取
    private val mMatrixLock = Any()

    // 方案A：以下三项会被 GL 渲染线程读取，需保证可见性
    @Volatile
    private var mPngBitmap: Bitmap? = null

    // 解码缩放比 = 地图原始像素 / 位图实际像素（1f 表示未压缩）
    @Volatile
    private var drawScale = 1f

    // 位图版本号：换图或换缩放时自增，供 GL 侧判断是否需要重新上传纹理
    @Volatile
    private var bitmapVersion = 0
    
    // 偏移量（单位：地图原始像素）
    @Volatile
    private var offsetX = 0f
    @Volatile
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
        synchronized(mMatrixLock) {
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
                    // 解码压缩后的位图需要用 drawScale 补偿回原始像素尺寸，
                    // 保证世界坐标->像素坐标的映射与原图完全一致
                    if (drawScale != 1f) {
                        scale(drawScale, drawScale)
                    }
                    drawBitmap(it, 0f, 0f, null)
                }
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {}
    
    fun setMatrix(matrix: Matrix) {
        // 优化：复用Matrix对象，避免重复创建
        synchronized(mMatrixLock) {
            mOuterMatrix.set(matrix)
        }
        postInvalidate()
    }

    /**
     * 拷贝一份当前矩阵快照（渲染线程读取时不持有 UI 线程锁）
     */
    fun copyMatrixInto(out: Matrix) {
        synchronized(mMatrixLock) {
            out.set(mOuterMatrix)
        }
    }

    /**
     * 设置地图
     *
     * @param bitmap 实际参与绘制的位图（可能是解码压缩后的）
     * @param drawScale 地图原始像素 / 位图实际像素，绘制时按该比例放大补偿
     */
    fun setBitmap(bitmap: Bitmap?, drawScale: Float = 1f) {
        // 交由 Glide 管理 Bitmap，不要手动 recycle
        mPngBitmap = bitmap
        this.drawScale = if (drawScale > 0f) drawScale else 1f
        bitmapVersion++
        // 重置偏移
        offsetX = 0f
        offsetY = 0f
        postInvalidate()
    }

    /**
     * 设置绘制偏移量（单位：地图原始像素）
     */
    fun setOffset(x: Float, y: Float) {
        if (offsetX != x || offsetY != y) {
            offsetX = x
            offsetY = y
            postInvalidate()
        }
    }

    /** 当前位图（GL 侧取纹理源） */
    val pngBitmap: Bitmap? get() = mPngBitmap

    /** 位图版本号（GL 侧判断是否需要重传纹理） */
    val pngBitmapVersion: Int get() = bitmapVersion

    /** 解码缩放比（GL 侧绘制时补偿） */
    val pngDrawScale: Float get() = drawScale

    val pngOffsetX: Float get() = offsetX

    val pngOffsetY: Float get() = offsetY

    /**
     * 获取地图位图宽度（地图原始像素尺寸，不是解码后位图的像素尺寸）
     */
    fun getBitmapWidth(): Int {
        val bmp = mPngBitmap ?: return 0
        return Math.round(bmp.width * drawScale)
    }

    /**
     * 获取地图位图高度（地图原始像素尺寸，不是解码后位图的像素尺寸）
     */
    fun getBitmapHeight(): Int {
        val bmp = mPngBitmap ?: return 0
        return Math.round(bmp.height * drawScale)
    }

    /**
     * 主动释放资源，应在 MapView 销毁时调用
     */
    fun release() {
        // 交由 Glide 管理 Bitmap，不要手动 recycle
        mPngBitmap = null
        synchronized(mMatrixLock) {
            mOuterMatrix.reset()
        }
        offsetX = 0f
        offsetY = 0f
        bitmapVersion++
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
