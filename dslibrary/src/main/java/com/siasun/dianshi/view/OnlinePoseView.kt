package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.siasun.dianshi.R
import com.siasun.dianshi.bean.InitPose
import java.lang.ref.WeakReference

/**
 * 原点、上线点视图
 * 用于在地图上绘制原点和上线点位置及其标签
 */
@SuppressLint("ViewConstructor")
class OnlinePoseView(context: Context?, parent: WeakReference<MapView>) :
    SlamWareBaseView(context, parent) {

    companion object {
        // 线宽（像素）
        private const val BASE_LINE_WIDTH = 3f

        // 文本大小（像素）
        private const val BASE_TEXT_SIZE = 10f

        // 标签偏移量（像素）
        const val LABEL_OFFSET = 15f
    }

    // 绘制画笔
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        strokeWidth = BASE_LINE_WIDTH
    }

    // 变换矩阵
    private val transformMatrix = Matrix()

    // 上线点图标
    private var onlinePointBitmap: Bitmap? = null

    // 原点图标
    private var originPointBitmap: Bitmap? = null

    // 上线点列表 - 线程安全处理
    private val initPoseList = mutableListOf<InitPose>()

    // 控制是否绘制原点与上线点
    private var isDrawingEnabled = true

    // 保存parent引用以便安全访问
    private val mapViewRef: WeakReference<MapView> = parent

    // 可复用的PointF对象，减少内存分配
    private var tempPoint = PointF()
    private val labelPoint = PointF()

    // 字符串资源缓存
    private val originPointText by lazy {
        context?.getString(R.string.origin_point) ?: ""
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mapView = mapViewRef.get() ?: return

        if (isDrawingEnabled) {
            // 绘制原点
            drawOriginPoint(canvas, mapView)

            // 绘制上线点
            drawInitPoses(canvas, mapView)
        }
    }

    /**
     * 绘制原点图标和标签
     */
    private fun drawOriginPoint(canvas: Canvas, mapView: MapView) {
        // 延迟初始化bitmap，避免在构造时创建
        if (originPointBitmap == null && context != null) {
            originPointBitmap = try {
                ContextCompat.getDrawable(context, R.drawable.origin_point)?.toBitmap()
            } catch (e: Exception) {
                null
            }
        }

        originPointBitmap?.also { bitmap ->
            paint.color = Color.GREEN

            // 计算原点在视图中的位置 - 复用tempPoint对象
            tempPoint = mapView.worldToScreen(0f, 0f)

            // 绘制带有变换的原点图标
            drawIconWithTransform(canvas, bitmap, tempPoint.x, tempPoint.y, 0f)

            // 绘制原点标签 - 复用labelPoint对象
            labelPoint.set(tempPoint.x + LABEL_OFFSET, tempPoint.y + LABEL_OFFSET)
            drawLabel(canvas, originPointText, labelPoint, paint)
        }
    }

    /**
     * 绘制所有上线点图标和标签
     */
    private fun drawInitPoses(canvas: Canvas, mapView: MapView) {
        // 创建列表副本避免并发修改问题
        val posesCopy = synchronized(initPoseList) {
            initPoseList.toList()
        }

        if (posesCopy.isEmpty()) return

        // 延迟初始化bitmap，避免在构造时创建
        if (onlinePointBitmap == null && context != null) {
            onlinePointBitmap = try {
                ContextCompat.getDrawable(context, R.drawable.online)?.toBitmap()
            } catch (e: Exception) {
                null
            }
        }

        paint.color = Color.BLACK

        for (initPose in posesCopy) {
            // 计算上线点在视图中的位置 - 复用tempPoint对象
            tempPoint = mapView.worldToScreen(initPose.initPos[0], initPose.initPos[1])

            onlinePointBitmap?.also { bitmap ->
                // 绘制带有变换的上线点图标
                drawIconWithTransform(
                    canvas,
                    bitmap,
                    tempPoint.x,
                    tempPoint.y,
                    -Math.toDegrees(initPose.initPos[2].toDouble()).toFloat()
                )

                // 绘制上线点标签
                drawLabel(canvas, initPose.name, tempPoint, paint)
            }
        }
    }

    /**
     * 绘制带有变换的图标
     * @param canvas 画布
     * @param bitmap 图标 bitmap
     * @param x 中心点 x 坐标
     * @param y 中心点 y 坐标
     * @param rotation 旋转角度（度）
     */
    private fun drawIconWithTransform(
        canvas: Canvas, bitmap: Bitmap, x: Float, y: Float, rotation: Float
    ) {
        // 重置变换矩阵
        transformMatrix.reset()

        // 计算图标的中心点偏移量
        val offsetX = bitmap.width / 2f
        val offsetY = bitmap.height / 2f

        // 设置变换矩阵
        transformMatrix.postTranslate(x - offsetX, y - offsetY) // 平移到中心位置
//        transformMatrix.postScale(scale, scale, x, y) // 应用缩放
        transformMatrix.postRotate(rotation, x, y) // 应用旋转

        // 绘制图标
        canvas.drawBitmap(bitmap, transformMatrix, paint)
    }


    /**
     * 设置上线点列表 - 线程安全处理
     * @param data 上线点数据列表
     */
    fun setInitPoses(data: MutableList<InitPose>) {
        synchronized(initPoseList) {
            initPoseList.clear()
            initPoseList.addAll(data)
        }
        postInvalidate()
    }

    /**
     * 追加上线点数据 - 线程安全处理
     * @param data 要追加的上线点数据列表
     */
    fun addInitPoses(data: MutableList<InitPose>) {
        synchronized(initPoseList) {
            initPoseList.addAll(data)
        }
        postInvalidate()
    }

    /**
     * 清除上线点列表 - 线程安全处理
     */
    fun clearInitPoses() {
        synchronized(initPoseList) {
            initPoseList.clear()
        }
        postInvalidate()
    }

    /**
     * 清理资源，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        // 清理bitmap资源
        onlinePointBitmap?.recycle()
        originPointBitmap?.recycle()

        // 清理数据
        synchronized(initPoseList) {
            initPoseList.clear()
        }

        // 禁用绘制
        isDrawingEnabled = false
    }


    /**
     * 设置是否启用绘制
     */
    fun setDrawingEnabled(enabled: Boolean) {
        this.isDrawingEnabled = enabled
        postInvalidate()
    }
}

