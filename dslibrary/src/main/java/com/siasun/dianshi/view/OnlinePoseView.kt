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
    private val onlinePointBitmap: Bitmap? by lazy {
        ContextCompat.getDrawable(context!!, R.drawable.online)!!.toBitmap() // 使用这个扩展函数可以直接转换
    }

    // 原点图标
    private val originPointBitmap: Bitmap? by lazy {
        ContextCompat.getDrawable(context!!, R.drawable.origin_point)!!.toBitmap() // 使用这个扩展函数可以直接转换
    }

    // 上线点列表
    private val initPoseList = mutableListOf<InitPose>()

    // 控制是否绘制原点与上线点
    private var isDrawingEnabled = true


    // 保存parent引用以便安全访问
    private var mapViewRef: WeakReference<MapView>? = parent
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mapViewRef?.get()?.let {
            // 绘制原点
            if (isDrawingEnabled) {
                // 设置绘制参数（根据缩放比例调整）
                paint.strokeWidth = BASE_LINE_WIDTH
                paint.textSize = BASE_TEXT_SIZE

                drawOriginPoint(canvas, it)

                // 绘制上线点
                drawInitPoses(canvas, it)
            }
        }
    }

    /**
     * 绘制原点图标和标签
     */
    private fun drawOriginPoint(canvas: Canvas, mapView: MapView) {
        originPointBitmap?.also { bitmap ->
            paint.color = Color.GREEN

            // 计算原点在视图中的位置
            val originPoint = mapView.worldToScreen(0f, 0f)

            // 绘制带有变换的原点图标
            drawIconWithTransform(canvas, bitmap, originPoint.x, originPoint.y, 0f)

            // 绘制原点标签
            drawLabel(
                canvas, context!!.getString(R.string.origin_point), PointF(
                    originPoint.x + LABEL_OFFSET,
                    originPoint.y + LABEL_OFFSET,
                ), paint
            )

        }
    }

    /**
     * 绘制所有上线点图标和标签
     */
    private fun drawInitPoses(canvas: Canvas, mapView: MapView) {
        if (initPoseList.isEmpty()) return

        paint.color = Color.BLACK

        for (initPose in initPoseList) {
            // 计算上线点在视图中的位置
            val point = mapView.worldToScreen(
                initPose.initPos[0], initPose.initPos[1]
            )

            onlinePointBitmap?.also { bitmap ->
                // 绘制带有变换的上线点图标
                drawIconWithTransform(
                    canvas,
                    bitmap,
                    point.x,
                    point.y,
                    -Math.toDegrees(initPose.initPos[2].toDouble()).toFloat()
                )

                // 绘制上线点标签
                drawLabel(canvas, initPose.name, point, paint)
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
        transformMatrix.postScale(scale, scale, x, y) // 应用缩放
        transformMatrix.postRotate(rotation, x, y) // 应用旋转

        // 绘制图标
        canvas.drawBitmap(bitmap, transformMatrix, paint)
    }


    /**
     * 设置上线点列表
     * @param data 上线点数据列表
     */
    fun setInitPoses(data: MutableList<InitPose>) {
        initPoseList.clear()
        initPoseList.addAll(data)
        postInvalidate()
    }

    /**
     * 追加上线点数据
     * @param data 要追加的上线点数据列表
     */
    fun addInitPoses(data: MutableList<InitPose>) {
        initPoseList.addAll(data)
        postInvalidate()
    }

    /**
     * 清除上线点列表
     */
    fun clearInitPoses() {
        initPoseList.clear()
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

