package com.siasun.dianshi.view.createMap.map2D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.Log
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.bean.createMap2d.MapEditorConstants
import com.siasun.dianshi.bean.createMap2d.SubMapData
import com.siasun.dianshi.view.SlamWareBaseView
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import androidx.core.graphics.createBitmap
import com.siasun.dianshi.view.WorkMode
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import kotlin.math.cos
import kotlin.math.sin
import kotlin.collections.iterator

/**
 * 建图地图轮廓
 */
@SuppressLint("ViewConstructor")
class MapOutline2D(context: Context?, val parent: WeakReference<CreateMapView2D>) :
    SlamWareBaseView<CreateMapView2D>(context, parent) {
    private val TAG = this::class.java.simpleName
    private var currentCreateMapWorkMode = WorkMode.MODE_SHOW_MAP

    //绘制地图的数据 建图时 2D
    private val keyFrames2d = ConcurrentHashMap<Int, SubMapData>()

    /**
     * 1. 计算所有子图集合的右上角和左下角
     * 2. 计算所有子图集合的长度和宽度
     */
    private var maxTopRight = PointF(-10.0f, -10.0f) // 右上
    private var minBotLeft = PointF(10.0f, 10.0f) // 左下
    private var minTopLeft = PointF(10.0f, 10.0f) // 左上 - 初始化为极大值
    private var maxBottomRight = PointF(-10.0f, -10.0f) // 右下 - 初始化为极小值


    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        if (currentCreateMapWorkMode == mode) return // 避免重复设置

        currentCreateMapWorkMode = mode

    }

    // 子图边框 Paint，单例共享，防内存抖动
    companion object {
        val mPaint = Paint().apply {
            isAntiAlias = true
        }

        val mBorderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        // 边框颜色 hue 偏移步长（质数），保证相邻子图颜色差异明显
        private const val BORDER_HUE_STEP = 37f

        // buildSubMapTileLine 用到的像素常量，放入 companion 避免每个实例重复构造
        val colorBlue: Byte = 0.toByte()
        val colorAlpha: Byte = (-0x10000 shr 24).toByte()
    }

    // 复用 RectF，防 onDraw 内存抖动
    private val mBorderRect = android.graphics.RectF()

    // 复用 HSV 颜色数组，防 onDraw 内存抖动
    private val mHsvTemp = FloatArray(3)

    // 复用 origin 屏幕坐标缓存，防 onDraw 内存抖动
    private val mCachedOriginScreen = PointF()

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        if (keyFrames2d.isNotEmpty()) {
            val mapView = parent.get() ?: return
            val scale = mapView.mMapScale

            keyFrames2d.values.forEach { subMap ->
                val bitmap = subMap.mBitmap ?: return@forEach
                val bmpWidth = bitmap.width.toFloat()
                val bmpHeight = bitmap.height.toFloat()
                if (bmpWidth <= 0f || bmpHeight <= 0f) return@forEach

                canvas.save()

                // 关键修复：
                // 1. 以子图 origin（= rightTop 世界坐标）作为绘制锚点
                //    worldToScreen 内部已经通过 mOuterMatrix 包含了视图旋转/缩放/平移，
                //    所以这里不再重复 rotate(getViewRotation)，避免子图相对底图双旋转错位。
                val originScreen = mapView.worldToScreen(subMap.originX, subMap.originY)
                mCachedOriginScreen.set(originScreen.x, originScreen.y)

                // 2. 应用每张子图自身的旋转 originTheta（绕 origin 点）
                //    这是之前完全缺失的步骤，也是子图叠加方向错位产生重影的核心原因
                canvas.rotate(
                    Math.toDegrees(subMap.originTheta.toDouble()).toFloat(),
                    mCachedOriginScreen.x,
                    mCachedOriginScreen.y
                )

                // 3. 应用地图缩放（绕 origin 点）
                canvas.scale(scale, scale, mCachedOriginScreen.x, mCachedOriginScreen.y)

                // 绘制基准点（origin = rightTop）对应 bitmap 的 right-top 像素：
                // bitmap 的像素坐标原点是左上角，宽 bmpWidth、高 bmpHeight
                // 所以 rightTop 像素 = bitmap 的 (bmpWidth, 0)，要把它对齐到画布原点（originScreen + 当前变换后的原点）
                // 即：绘制 bitmap 的左上角位置 = originScreen + (-bmpWidth, 0)
                val drawLeft = mCachedOriginScreen.x - bmpWidth
                val drawTop = mCachedOriginScreen.y

                // 绘制子图
                canvas.drawBitmap(bitmap, drawLeft, drawTop, mPaint)

                // 绘制子图边框，颜色基于子图 ID，保证每张子图颜色不同
                val hue = ((subMap.id * BORDER_HUE_STEP) % 360f + 360f) % 360f
                mHsvTemp[0] = hue
                mHsvTemp[1] = 0.85f
                mHsvTemp[2] = 0.95f
                mBorderPaint.color = android.graphics.Color.HSVToColor(200, mHsvTemp)
                mBorderRect.set(drawLeft, drawTop, drawLeft + bmpWidth, drawTop + bmpHeight)
                canvas.drawRect(mBorderRect, mBorderPaint)

                canvas.restore()
            }
        }
    }


    /**
     * 根据 originX/Y、percent、width/height 重新计算子图四个角的世界坐标
     * 注：未考虑 originTheta 旋转，仅用于对齐未旋转的 AABB 包围盒与整体地图边界
     */
    private fun recalculateSubMapCorners(subMap: SubMapData) {
        val widthInWorld = subMap.percent * subMap.width
        val heightInWorld = subMap.percent * subMap.height

        // 右上角 = origin
        subMap.rightTop.x = subMap.originX
        subMap.rightTop.y = subMap.originY

        // 右下角 = originX, originY - height
        subMap.rightBottom.x = subMap.originX
        subMap.rightBottom.y = subMap.originY - heightInWorld

        // 左上角 = originX - width, originY
        subMap.leftTop.x = subMap.originX - widthInWorld
        subMap.leftTop.y = subMap.originY

        // 左下角 = originX - width, originY - height
        subMap.leftBottom.x = subMap.originX - widthInWorld
        subMap.leftBottom.y = subMap.originY - heightInWorld
    }

    /**
     * 外部接口：更新子图数据 2D
     */
    fun parseSubMaps2D(mLaserT: laser_t, type: Int) {
        val mapView = parent.get() ?: return
        val subMapData = SubMapData()
        //子图ID
        subMapData.id = mLaserT.rad0.toInt()
        //子图 x方向格子数量 子图宽度
        subMapData.width = mLaserT.ranges[0]
        //子图 y方向格子数量 子图高度
        subMapData.height = mLaserT.ranges[1]

        //新增读取子图右上角世界坐标
        subMapData.originX = mLaserT.ranges[2]
        subMapData.originY = mLaserT.ranges[3]
        subMapData.originTheta = mLaserT.ranges[4]

        subMapData.optMaxTempX = mLaserT.ranges[5]
        subMapData.optMaxTempY = mLaserT.ranges[6]
        subMapData.optMaxTempTheta = mLaserT.ranges[7]

        // 各格子概率值
        subMapData.indexCount = mLaserT.intensities.size
        //所有概率点的集合
        for (intensity in mLaserT.intensities) subMapData.intensitiesList.add(intensity.toInt())

        //创建子图bitmap对象
        buildSubMapTileLine(subMapData)

        // 计算四个角的世界坐标
        recalculateSubMapCorners(subMapData)

        calBinding(type)

        //存储子图数据
        keyFrames2d[subMapData.id] = subMapData
        mapView.isStartRevSubMaps = true

//        Log.e(TAG, "整张 地图的信息  keyFrames2d keyFrames2d.size ${keyFrames2d.size}")
//        Log.w(TAG, "整张 地图的信息  mSrf.mapData ${mapView.mSrf.mapData}")
        postInvalidate()
    }

    /**
     * 创建子图bitmap对象
     */
    private fun buildSubMapTileLine(metaData: SubMapData) {
        val width = metaData.width.toInt()
        val height = metaData.height.toInt()
        val pixelSize = MapEditorConstants.MAP_PIXEL_SIZE
        val bmpDataSize = width * height * pixelSize

        val bmpData = ByteArray(bmpDataSize)

        // 使用普通for循环代替forEach，提高性能
        for (i in 0 until metaData.indexCount) {
            val index = metaData.intensitiesList[i] * pixelSize
            bmpData[index] = colorBlue
            bmpData[index + 1] = colorBlue
            bmpData[index + 2] = colorBlue
            bmpData[index + 3] = colorAlpha
        }

        val bitmap = createBitmap(width, height)
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bmpData))
        metaData.mBitmap = bitmap
    }


    /**
     * 计算新建地图宽高
     */
    private fun calBinding(type: Int) {
        val mapView = parent.get() ?: return

        for (item in keyFrames2d) {
            val subCreateMap = item.value

            // 更新右上角坐标 - 取所有子图中的最大值
            if (subCreateMap.originX > maxTopRight.x) {
                maxTopRight.x = subCreateMap.originX
            }

            // 更新左下角坐标 - 取所有子图中的最小值
            if (subCreateMap.leftBottom.x < minBotLeft.x) {
                minBotLeft.x = subCreateMap.leftBottom.x
            }

            if (subCreateMap.leftBottom.y < minBotLeft.y) {
                minBotLeft.y = subCreateMap.leftBottom.y
            }

            // 更新左上角坐标 - 取所有子图中的最小值
            if (subCreateMap.leftTop.x < minTopLeft.x) {
                minTopLeft.x = subCreateMap.leftTop.x
            }

            if (subCreateMap.leftTop.y > minTopLeft.y) {
                minTopLeft.y = subCreateMap.leftTop.y
            }

            // 更新右下角坐标 - 取所有子图中的最大值
            if (subCreateMap.rightBottom.x > maxBottomRight.x) {
                maxBottomRight.x = subCreateMap.rightBottom.x
            }

            if (subCreateMap.rightBottom.y < maxBottomRight.y) {
                maxBottomRight.y = subCreateMap.rightBottom.y
            }
        }

        // 计算整张地图的宽度和高度
        val width = abs((maxTopRight.x - minBotLeft.x) / 0.05f)
        val height = abs((maxTopRight.y - minBotLeft.y) / 0.05f)

        if (type == 0) {//更新 使用地图PNG原有的宽高


        } else if (type == 1) {//扩展 （1地图内的时候使用地图宽高、2地图外的时候使用子图计算的宽高）
            // 扩展地图时，如果子图的宽高大于底图的宽高，则使用子图的宽高
            if (width > mapView.mSrf.mapData.width) {
                mapView.mSrf.mapData.width = width
            }
            if (height > mapView.mSrf.mapData.height) {
                mapView.mSrf.mapData.height = height
            }
        } else {  // 新建地图时，直接使用计算出的宽高
            mapView.mSrf.mapData.width = width
            mapView.mSrf.mapData.height = height
        }
//        Log.d(TAG, "整张地图的宽度 ${mapView.mSrf.mapData.width}")
//        Log.d(TAG, "整张地图的高度 ${mapView.mSrf.mapData.height}")
    }

    /**
     * 回环检测2D
     * 输入数据 世界坐标系下的位姿态
     */
    fun updateOptPose2D(mLaserT: laser_t, type: Int) {
        Log.w(TAG, "回环检测2D  start")
        val optPose = mLaserT.ranges
//        LogUtil.w("回环检测optPose.size  ${optPose.size}")

        val IDList: MutableList<Int> = mutableListOf()
        //   按采样间隔遍历数据（步长为4*SAMPLE_INTERVAL，每个关键帧占4个Float）
        for (i in optPose.indices step 4) {
            val id = optPose[i].toInt()
            IDList.add(id)
//            LogUtil.w("回环检测id  $id")
            val globalX = optPose[i + 1]
//            LogUtil.w("回环检测globalX  $globalX")
            val globalY = optPose[i + 2]
//            LogUtil.w("回环检测globalY  $globalY")
            val globalTheta = optPose[i + 3]
//            LogUtil.w("回环检测globalT  $globalTheta")

            //关键帧
            // 获取关键帧数据（非空校验）
            val subMapData = keyFrames2d[id] ?: continue

//            LogUtil.d("第 $id 张子图  $subMapData")

            // Extract local pose
            val (localX, localY, localTheta) = Triple(
                subMapData.optMaxTempX.toDouble(),
                subMapData.optMaxTempY.toDouble(),
                subMapData.originTheta.toDouble()
            )

            // Build global transformation matrix [cos(θg) -sin(θg) xg; sin(θg) cos(θg) yg; 0 0 1]
            val globalMatrix = Array2DRowRealMatrix(
                arrayOf(
                    doubleArrayOf(
                        cos(globalTheta).toDouble(),
                        (-sin(globalTheta)).toDouble(),
                        globalX.toDouble()
                    ), doubleArrayOf(
                        sin(globalTheta).toDouble(), cos(globalTheta).toDouble(), globalY.toDouble()
                    ), doubleArrayOf(0.0, 0.0, 1.0)
                )
            )

            // Build local transformation matrix [cos(θl) -sin(θl) xl; sin(θl) cos(θl) yl; 0 0 1]
            val localMatrix = Array2DRowRealMatrix(
                arrayOf(
                    doubleArrayOf(cos(localTheta), -sin(localTheta), localX),
                    doubleArrayOf(sin(localTheta), cos(localTheta), localY),
                    doubleArrayOf(0.0, 0.0, 1.0)
                )
            )

            // Compute final transformation: T_global * T_local
            val resultMatrix = globalMatrix.multiply(localMatrix).data

            // Update submap metadata with global origin and orientation
            subMapData.originX = resultMatrix[0][2].toFloat()
            subMapData.originY = resultMatrix[1][2].toFloat()
            subMapData.originTheta = globalTheta

            // 重新计算子图四个角的世界坐标，防止回环后绘制用旧坐标导致重影
            recalculateSubMapCorners(subMapData)
        }


        calBinding(type)
        // 回环完成后主动刷新视图
        postInvalidate()
        Log.w(TAG, "回环检测2D  end")
    }


    /**
     * 清理资源，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理点云数据
        keyFrames2d.clear()
        // 清理父引用
        parent.clear()
    }
}
