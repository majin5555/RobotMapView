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

    // ==================== 边框绘制控制方法 ====================

    /**
     * 控制是否绘制子图边框
     */
    fun setBorderVisible(visible: Boolean) {
        if (isBorderVisible == visible) return
        isBorderVisible = visible
        postInvalidate()
    }

    /**
     * 控制边框颜色模式
     * @param bySubMap true 表示按子图 ID 自动分配不同颜色（默认）；false 表示使用 [setBorderColor] 设置的固定颜色
     */
    fun setBorderColorMode(bySubMap: Boolean) {
        if (isBorderColorBySubMap == bySubMap) return
        isBorderColorBySubMap = bySubMap
        postInvalidate()
    }

    /**
     * 设置边框固定颜色（仅在 [setBorderColorMode] 传入 false 时生效），
     * 颜色中的 alpha 会被 [setBorderAlpha] 的透明度覆盖
     */
    fun setBorderColor(color: Int) {
        if (mBorderColor == color) return
        mBorderColor = color
        if (!isBorderColorBySubMap) postInvalidate()
    }

    /**
     * 设置边框线宽（像素）
     */
    fun setBorderStrokeWidth(width: Float) {
        val w = if (width < 0f) 0f else width
        if (mBorderPaint.strokeWidth == w) return
        mBorderPaint.strokeWidth = w
        postInvalidate()
    }

    /**
     * 设置边框透明度
     * @param alpha 取值 0-255，0 完全透明，255 完全不透明
     */
    fun setBorderAlpha(alpha: Int) {
        val a = alpha.coerceIn(0, 255)
        if (borderAlpha == a) return
        borderAlpha = a
        postInvalidate()
    }

    /**
     * 一键重置边框绘制为默认配置
     */
    fun resetBorderStyle() {
        isBorderVisible = true
        isBorderColorBySubMap = true
        mBorderColor = android.graphics.Color.rgb(0, 170, 255)
        borderAlpha = 200
        mBorderPaint.strokeWidth = 2f
        postInvalidate()
    }
    // ==========================================================

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

    // 复用子图绘制矩阵，防 onDraw 内存抖动
    private val mSubMapMatrix = android.graphics.Matrix()

    // 复用"世界坐标 → 地图像素坐标"矩阵（含 y 翻转），与 CoordinateConversion.worldToScreen 保持一致
    private val mWorldToMapPixelMatrix = android.graphics.Matrix()

    // ==================== 边框绘制控制配置 ====================
    // 是否绘制子图边框，默认开启
    private var isBorderVisible = false
    // 是否按子图 ID 自动分配不同颜色，true 时优先于 mBorderColor
    private var isBorderColorBySubMap = false
    // 固定边框颜色（isBorderColorBySubMap = false 时生效），默认蓝色
    private var mBorderColor = android.graphics.Color.rgb(0, 170, 255)
    // 边框透明度 0-255，两种颜色模式均生效
    private var borderAlpha = 200
    // ==========================================================

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        if (keyFrames2d.isNotEmpty()) {
            val mapView = parent.get() ?: return

            // 与 worldToScreen 保持同一把锁，避免 mapData 被并发修改
            synchronized(mapView.mSrf.mapData) {
                val mapData = mapView.mSrf.mapData
                val resolution = mapData.resolution
                if (resolution <= 0f) return

                keyFrames2d.values.forEach { subMap ->
                    val bitmap = subMap.mBitmap ?: return@forEach
                    val bmpWidth = bitmap.width.toFloat()
                    val bmpHeight = bitmap.height.toFloat()
                    if (bmpWidth <= 0f || bmpHeight <= 0f) return@forEach

                    // ============ 关键修复：子图与点云共用同一套坐标变换链 ============
                    // 点云/底图链路：世界坐标 → mSrf.worldToScreen(世界→地图像素，含y翻转)
                    //                           → mOuterMatrix.mapPoints(像素→屏幕)
                    // 子图链路（旧实现）只用 worldToScreen 转锚点再手动 rotate/scale，
                    // 缩放/旋转视图后与点云、底图不一致，产生轮廓错位。
                    // 修复：为 bitmap 构造完整矩阵，使 bitmap 每个像素都走同一变换链：
                    //   bitmap像素 → 子图局部坐标(percent, y向上)
                    //             → 绕 origin 旋转 originTheta(世界坐标系 y 向上逆时针)
                    //             → 平移到世界坐标 origin
                    //             → 世界坐标 → 地图像素坐标(含 y 翻转)
                    //             → mOuterMatrix → 屏幕坐标
                    // ==================================================================
                    mSubMapMatrix.reset()
                    // 1. bitmap 像素 → 子图局部坐标（米），rightTop 像素 (bmpWidth, 0) 对应 origin
                    mSubMapMatrix.postScale(subMap.percent, -subMap.percent)
                    mSubMapMatrix.postTranslate(-bmpWidth * subMap.percent, 0f)
                    // 2. 绕 origin 旋转 originTheta。Matrix.postRotate 为顺时针，取负角实现世界逆时针
                    mSubMapMatrix.postRotate(
                        -Math.toDegrees(subMap.originTheta.toDouble()).toFloat()
                    )
                    // 3. 平移到世界坐标 origin
                    mSubMapMatrix.postTranslate(subMap.originX, subMap.originY)
                    // 4. 世界坐标 → 地图像素坐标（含 y 翻转），与 CoordinateConversion.worldToScreen 一致
                    mWorldToMapPixelMatrix.setValues(
                        floatArrayOf(
                            1f / resolution, 0f, -mapData.originX / resolution,
                            0f, -1f / resolution, mapData.height + mapData.originY / resolution,
                            0f, 0f, 1f
                        )
                    )
                    mSubMapMatrix.postConcat(mWorldToMapPixelMatrix)
                    // 5. 地图像素 → 屏幕（视图缩放/旋转/平移全部由 mOuterMatrix 承担）
                    mSubMapMatrix.postConcat(mapView.outerMatrix)

                    canvas.save()
                    canvas.concat(mSubMapMatrix)

                    // 绘制子图
                    canvas.drawBitmap(bitmap, 0f, 0f, mPaint)

                    // 绘制子图边框（与 bitmap 处于同一变换坐标系，保证边框随子图一起缩放旋转）
                    if (isBorderVisible) {
                        if (isBorderColorBySubMap) {
                            // 颜色基于子图 ID，保证每张子图颜色不同
                            val hue = ((subMap.id * BORDER_HUE_STEP) % 360f + 360f) % 360f
                            mHsvTemp[0] = hue
                            mHsvTemp[1] = 0.85f
                            mHsvTemp[2] = 0.95f
                            mBorderPaint.color =
                                android.graphics.Color.HSVToColor(borderAlpha, mHsvTemp)
                        } else {
                            // 统一固定颜色（替换 alpha 为 borderAlpha）
                            mBorderPaint.color = (mBorderColor and 0x00FFFFFF) or (borderAlpha shl 24)
                        }
                        mBorderRect.set(0f, 0f, bmpWidth, bmpHeight)
                        canvas.drawRect(mBorderRect, mBorderPaint)
                    }

                    canvas.restore()
                }
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
