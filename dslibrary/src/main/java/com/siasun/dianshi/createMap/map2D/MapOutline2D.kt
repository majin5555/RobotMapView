package com.siasun.dianshi.createMap.map2D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
    private var currentWorkMode = CreateMapView2D.WorkMode.MODE_SHOW_MAP

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
    fun setWorkMode(mode: CreateMapView2D.WorkMode) {
        if (currentWorkMode == mode) return // 避免重复设置

        currentWorkMode = mode

    }

    companion object {
        val mPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            strokeWidth = 2f
            color = Color.BLACK
            isFilterBitmap = true
            isDither = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }
    }


    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 只有在绘制启用状态下才绘制点云
        if (keyFrames2d.isNotEmpty()) {
            val mapView = parent.get() ?: return

            keyFrames2d.values.forEach { subMap ->
                val leftTop = mapView.mSrf.worldToScreen(
                    subMap.leftTop.x, subMap.leftTop.y
                )
                canvas.drawBitmap(
                    subMap.mBitmap!!, leftTop.x, leftTop.y, mPaint
                )
            }
        }
        canvas.restore()

    }


    /**
     * 外部接口：更新子图数据 2D
     */
    fun parseSubMaps2D(mLaserT: laser_t, type: Int) {
        synchronized(keyFrames2d) {
            val mapView = parent.get() ?: return

            val subMapData = SubMapData()
            //子图ID
            subMapData.id = mLaserT.rad0.toInt()
            Log.d(TAG, "子图 radID  子图索引 ${subMapData.id}")

            //子图 x方向格子数量 子图宽度
            subMapData.width = mLaserT.ranges[0]
            Log.d(
                TAG, "子图 mLaserT.ranges[0] x方向格子数量 子图宽度 ${subMapData.width}"
            )
            //子图 y方向格子数量 子图高度
            subMapData.height = mLaserT.ranges[1]
            Log.d(
                TAG, "子图 mLaserT.ranges[1] y方向格子数量 子图高度 ${subMapData.height}"
            )

            //新增读取子图右上角世界坐标
            subMapData.originX = mLaserT.ranges[2]
            Log.d(
                TAG,
                "子图 mLaserT.ranges[2] 新增读取子图右上角世界坐标 originX ${subMapData.originX}"
            )
            subMapData.originY = mLaserT.ranges[3]
            Log.d(
                TAG,
                "子图 mLaserT.ranges[3] 新增读取子图右上角世界坐标 originY ${subMapData.originY}"
            )
            subMapData.originTheta = mLaserT.ranges[4]
            Log.d(
                TAG,
                "子图 mLaserT.ranges[4] 新增读取子图右上角世界坐标 originTheta ${subMapData.originTheta}"
            )
            subMapData.optMaxTempX = mLaserT.ranges[5]
            Log.d(TAG, "子图 mLaserT.ranges[5] optMaxTempX  ${subMapData.optMaxTempX}")
            subMapData.optMaxTempY = mLaserT.ranges[6]
            Log.d(TAG, "子图 mLaserT.ranges[6] optMaxTempY  ${subMapData.optMaxTempY}")
            subMapData.optMaxTempTheta = mLaserT.ranges[7]
            Log.d(
                TAG, "子图 mLaserT.ranges[7] optMaxTempXTheta   ${subMapData.optMaxTempTheta}"
            )

            // 各格子概率值
            subMapData.indexCount = mLaserT.intensities.size
            //所有概率点的集合
            for (intensity in mLaserT.intensities) {
                subMapData.intensitiesList.add(intensity.toInt())
            }

            //创建子图bitmap对象
            buildSubMapTileLine(subMapData)

            //右上角角物理坐标   (世界坐标系)
            subMapData.rightTop.x = subMapData.originX
            subMapData.rightTop.y = subMapData.originY

            //右下角角物理坐标   (世界坐标系)
            subMapData.rightBottom.x = subMapData.originX
            subMapData.rightBottom.y = subMapData.originY - (subMapData.percent * subMapData.height)

            //左上角物理坐标  (世界坐标系)
            subMapData.leftTop.x = subMapData.rightTop.x - (subMapData.percent * subMapData.width)
            subMapData.leftTop.y = subMapData.originY

            //左下角坐标 (世界坐标系)
            subMapData.leftBottom.x =
                subMapData.rightTop.x - (subMapData.percent * subMapData.width)
            subMapData.leftBottom.y = subMapData.originY - (subMapData.percent * subMapData.height)


            //扩展时
            if (type == 1) {

            } else {
                //新建地图时
                calBinding()
            }

//
//            val mTempMatrix = Matrix()
//            mTempMatrix.reset()
//            mTempMatrix.setScale(mapView.mSrf.scale, mapView.mSrf.scale)
//
//            // 计算子图在屏幕上的左上角坐标
//            val screenLeftTop = mapView.worldToScreen(subMapData.leftTop.x, subMapData.leftTop.y)
//            mTempMatrix.postTranslate(screenLeftTop.x, screenLeftTop.y)
//            subMapData.matrix = mTempMatrix
//            mTempMatrix.reset()

            mapView.isRouteMap = true
            mapView.isStartRevSubMaps = true

            Log.e(
                TAG, "整张 地图的信息  keyFrames2d keyFrames2d.size ${keyFrames2d.size}"
            )
            Log.w(TAG, "整张 地图的信息  mSrf.mapData ${mapView.mSrf.mapData}")
        }
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

        // 提前计算常量，避免在循环中重复计算
        val colorBlue = 0.toByte()
        val colorAlpha = (-0x10000 shr 24).toByte()

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
    private fun calBinding() {
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
        mapView.mSrf.mapData.width = abs((maxTopRight.x - minBotLeft.x) / 0.05f)
        mapView.mSrf.mapData.height = abs((maxTopRight.y - minBotLeft.y) / 0.05f)

//        Log.d(TAG,"左上 ${minTopLeft}")
//        Log.d(TAG,"左下 ${minBotLeft}")
//        Log.d(TAG,"右上 ${maxTopRight}")
//        Log.d(TAG,"右下 ${maxBottomRight}")
//        Log.d(TAG,"整张地图的宽度 ${mSrf.mapData.mWidth}")
//        Log.d(TAG,"整张地图的高度 ${mSrf.mapData.mHeight}")
    }

    /**
     * 回环检测2D
     * 输入数据 世界坐标系下的位姿态
     */
    fun updateOptPose2D(mLaserT: laser_t, type: Int) {

//        LogUtil.w("回环检测2D  start")
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
        }

//        updateKeyFrame2d()
        //扩展时
        if (type == 1) {
        } else {
            //新建地图时
            calBinding()
        }
//        LogUtil.w("回环检测2D  end")
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
