package com.siasun.dianshi.createMap2D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
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

/**
 * 建图地图轮廓
 */
@SuppressLint("ViewConstructor")
class MapOutline2D(context: Context?, val parent: WeakReference<CreateMapView2D>) :
    SlamWareBaseView<CreateMapView2D>(context, parent) {
    private val TAG = this::class.java.simpleName

    // 控制是否绘制
    private var isDrawingEnabled: Boolean = true

    private val keyFrames2d = ConcurrentHashMap<Int, SubMapData>() //绘制地图的数据 建图时 2D

    /**
     * 1. 计算所有子图集合的右上角和左下角
     * 2. 计算所有子图集合的长度和宽度
     */
    private var maxTopRight = PointF(-10.0f, -10.0f) // 右上
    private var minBotLeft = PointF(10.0f, 10.0f) // 左下
    private var minTopLeft = PointF(10.0f, 10.0f) // 左上 - 初始化为极大值
    private var maxBottomRight = PointF(-10.0f, -10.0f) // 右下 - 初始化为极小值


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
        if (isDrawingEnabled && keyFrames2d.isNotEmpty()) {
            val mapView = parent.get() ?: return
            // 计算屏幕中心
            val centerX = width / 2f
            val centerY = height / 2f
            synchronized(keyFrames2d) {
                canvas.save()
                // 应用全局旋转（如果有）
                if (mapView.mRotateAngle != 0f) {
                    canvas.rotate(-mapView.mRotateAngle, centerX, centerY)
                }
                // 直接绘制子图（不进行坐标转换，相对于屏幕中心）
                for ((_, mSubMapData) in keyFrames2d.entries) {
                    val bitmap = mSubMapData.mBitmap ?: continue
                    // 计算子图相对于屏幕中心的位置
                    val subMapOffsetX =
                        centerX + (mSubMapData.leftTop.x - mapView.robotPose[0]) * mapView.mSrf.scale
                    val subMapOffsetY =
                        centerY + (mSubMapData.leftTop.y - mapView.robotPose[1]) * mapView.mSrf.scale
                    // 创建新矩阵
                    val matrix = Matrix().apply {
                        postScale(mapView.mSrf.scale, mapView.mSrf.scale)
                        postTranslate(subMapOffsetX, subMapOffsetY)
                    }
                    canvas.drawBitmap(bitmap, matrix, mPaint)
                }
            }
        }
    }


    /**
     * 设置是否启用绘制
     */
    fun setDrawingEnabled(enabled: Boolean) {
        this.isDrawingEnabled = enabled
        postInvalidate()
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
            Log.i(TAG, "子图 radID  子图索引 ${subMapData.id}")

            //子图 x方向格子数量 子图宽度
            subMapData.width = mLaserT.ranges[0]
            Log.i(
                TAG, "子图 mLaserT.ranges[0] x方向格子数量 子图宽度 ${subMapData.width}"
            )
            //子图 y方向格子数量 子图高度
            subMapData.height = mLaserT.ranges[1]
            Log.i(
                TAG, "子图 mLaserT.ranges[1] y方向格子数量 子图高度 ${subMapData.height}"
            )

            //新增读取子图右上角世界坐标
            subMapData.originX = mLaserT.ranges[2]
            Log.i(
                TAG,
                "子图 mLaserT.ranges[2] 新增读取子图右上角世界坐标 originX ${subMapData.originX}"
            )
            subMapData.originY = mLaserT.ranges[3]
            Log.i(
                TAG,
                "子图 mLaserT.ranges[3] 新增读取子图右上角世界坐标 originY ${subMapData.originY}"
            )
            subMapData.originTheta = mLaserT.ranges[4]
            Log.i(
                TAG,
                "子图 mLaserT.ranges[4] 新增读取子图右上角世界坐标 originTheta ${subMapData.originTheta}"
            )
            subMapData.optMaxTempX = mLaserT.ranges[5]
            Log.i(TAG, "子图 mLaserT.ranges[5] optMaxTempX  ${subMapData.optMaxTempX}")
            subMapData.optMaxTempY = mLaserT.ranges[6]
            Log.i(TAG, "子图 mLaserT.ranges[6] optMaxTempY  ${subMapData.optMaxTempY}")
            subMapData.optMaxTempTheta = mLaserT.ranges[7]
            Log.i(
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


            val mTempMatrix = Matrix()
            mTempMatrix.reset()
            mTempMatrix.setScale(mapView.mSrf.scale, mapView.mSrf.scale)

            // 计算子图在屏幕上的左上角坐标
            val screenLeftTop = mapView.worldToScreen(subMapData.leftTop.x, subMapData.leftTop.y)
            mTempMatrix.postTranslate(screenLeftTop.x, screenLeftTop.y)
            subMapData.matrix = mTempMatrix

            keyFrames2d[subMapData.id] = subMapData

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
        val bmpData =
            ByteArray((metaData.width * metaData.height * MapEditorConstants.MAP_PIXEL_SIZE).toInt())
        for (i in 0 until metaData.indexCount) {
            val index: Int = metaData.intensitiesList[i]
            val color = 0
            bmpData[index * MapEditorConstants.MAP_PIXEL_SIZE] = (color and 0x000000FF).toByte()
            bmpData[index * MapEditorConstants.MAP_PIXEL_SIZE + 1] = (color and 0x000000FF).toByte()
            bmpData[index * MapEditorConstants.MAP_PIXEL_SIZE + 2] = (color and 0x000000FF).toByte()
            bmpData[index * MapEditorConstants.MAP_PIXEL_SIZE + 3] = (-0x10000 shr 24).toByte()
        }

        val bitmap = createBitmap(metaData.width.toInt(), metaData.height.toInt())

        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bmpData))
        metaData.mBitmap = bitmap
        Log.i(TAG, "子图ID ${metaData.id}  宽 ${bitmap.width} 高 ${bitmap.height}")
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

//        Log.i(TAG,"左上 ${minTopLeft}")
//        Log.i(TAG,"左下 ${minBotLeft}")
//        Log.i(TAG,"右上 ${maxTopRight}")
//        Log.i(TAG,"右下 ${maxBottomRight}")
//        Log.i(TAG,"整张地图的宽度 ${mSrf.mapData.mWidth}")
//        Log.i(TAG,"整张地图的高度 ${mSrf.mapData.mHeight}")
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
