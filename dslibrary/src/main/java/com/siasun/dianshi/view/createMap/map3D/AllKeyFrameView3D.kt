package com.siasun.dianshi.view.createMap.map3D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.view.SlamWareBaseView
import com.siasun.dianshi.view.WorkMode
import java.lang.ref.WeakReference
import com.siasun.dianshi.bean.OldKeyFrame

/**
 * 扩展地图定位模式 所有关键帧
 */
@SuppressLint("ViewConstructor")
class AllKeyFrameView3D(context: Context?, val parent: WeakReference<CreateMapView3D>) :
    SlamWareBaseView<CreateMapView3D>(context, parent) {
    private val TAG = this::class.java.simpleName

    //定位模式地图关键帧地图路线
    private val mMapPath: MutableList<OldKeyFrame> = mutableListOf()

    private var currentWorkMode = WorkMode.MODE_SHOW_MAP

    // 预分配对象以避免在 onDraw 中高频创建
    private val mWorldToPixelMatrix = android.graphics.Matrix()
    private val mTotalMatrix = android.graphics.Matrix()

    companion object {
        private val paint: Paint = Paint().apply {
            color = Color.parseColor("#800080")
            strokeWidth = 10f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.FILL
        }

        val mArrowPath = Path().apply {
            moveTo(12f, 0f)
            lineTo(-6f, -4f)
            lineTo(-6f, 4f)
            close()
        }
        val mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        if (currentWorkMode == mode) return // 避免重复设置

        currentWorkMode = mode

    }

    /**
     * 外部接口：更新关键帧数据 拓展地图时显示所有关键帧位置
     */
    fun parseKeyFramePose(mLaserT: laser_t) {
        mMapPath.clear()
        if (mLaserT.ranges.isNotEmpty()) {
            // 每3个数据为一组: x, y, theta
            for (i in 0 until mLaserT.ranges.size) {
                //关键帧  x
                val radX: Float = mLaserT.ranges[3 * i]
                //关键帧  y
                val radY: Float = mLaserT.ranges[3 * i + 1]
                //关键帧角度 theta
                val theta: Float = mLaserT.ranges[3 * i + 2]
                mMapPath.add(OldKeyFrame(radX, radY, theta, i))
            }
        }
    }

    @SuppressLint("DrawAllocation", "DefaultLocale")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mapView = parent.get() ?: return
        canvas.save()

        var resolution = 0.05f
        mWorldToPixelMatrix.reset()
        synchronized(mapView.mSrf.mapData) {
            val mapData = mapView.mSrf.mapData
            resolution = mapData.resolution
            if (resolution <= 0) resolution = 0.05f

            mWorldToPixelMatrix.postTranslate(-mapData.originX, -mapData.originY)
            mWorldToPixelMatrix.postScale(1f / resolution, -1f / resolution)
            mWorldToPixelMatrix.postTranslate(0f, mapData.height.toFloat())
        }
        mTotalMatrix.set(mapView.outerMatrix)
        mTotalMatrix.preConcat(mWorldToPixelMatrix)
        canvas.concat(mTotalMatrix)

//           drawKeyFrame(this)
        drawKeyFrameAngles(canvas, mapView.mSrf.scale / resolution)
        canvas.restore()
        drawKeyFrameId(canvas)
    }

    private fun drawKeyFrame(canvas: Canvas) {
        val mapView = parent.get() ?: return

        for (point in mMapPath) {
            val worldToScreen = mapView.worldToScreen(point.x, point.y)
            // 使用局部变量减少重复计算
            val mPoints = floatArrayOf(worldToScreen.x, worldToScreen.y)
            canvas.drawPoints(mPoints, paint)
        }
    }

    /**
     * 绘制关键帧角度
     */
    private fun drawKeyFrameAngles(canvas: Canvas, totalScale: Float) {
        if (totalScale <= 0) return
        val inverseScale = 1f / totalScale

        paint.strokeWidth = 8f / totalScale

        for (frame in mMapPath) {
            canvas.save()
            canvas.translate(frame.x, frame.y)
            // frame.theta 为弧度，转换为角度（在翻转的Y轴坐标系中，正角度会自动逆时针旋转即向上）
            canvas.rotate(Math.toDegrees(frame.theta.toDouble()).toFloat() )
            // 缩放以保持屏幕上的恒定大小
            canvas.scale(inverseScale, inverseScale)
            canvas.drawPath(mArrowPath, paint)
            canvas.restore()
        }
    }

    private fun drawKeyFrameId(canvas: Canvas) {
        val mapView = parent.get() ?: return
        canvas.save()

        for (frame in mMapPath) {
            // 使用 worldToScreen 确保精确映射到屏幕坐标系
            val screenPt = mapView.worldToScreen(frame.x, frame.y)
            val text = "${frame.id}"

            val textX = screenPt.x
            // Y 坐标：关键帧中心。使得文字在Y轴上也居中对齐于关键帧中心点
            val textY = screenPt.y - (mTextPaint.descent() + mTextPaint.ascent()) / 2f

            canvas.drawText(text, textX, textY + 10, mTextPaint)
        }
        canvas.restore()

    }

    /**
     * 清理资源，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理父引用
        parent.clear()
    }
}
