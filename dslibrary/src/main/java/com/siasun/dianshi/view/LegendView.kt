package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import com.siasun.dianshi.R
import com.siasun.dianshi.databinding.MapViewLegendBinding
import java.lang.ref.WeakReference

/**
 * 图例
 */
@SuppressLint("ViewConstructor")
class LegendView(context: Context, attrs: AttributeSet, parent: WeakReference<MapView>) :
    LinearLayout(context) {
    private lateinit var mBinding: MapViewLegendBinding

    /**
     * 初始化
     *
     * @param context 上下文
     * @param attrs   attrs
     */
    private fun init(context: Context, attrs: AttributeSet?) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mBinding = MapViewLegendBinding.inflate(inflater, this, true)
    }

    init {
        init(context, attrs)

        // 为每个CheckBox添加点击变化监听器
        // 上激光点云
        mBinding.cbUpLaserPointCloud.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parent.get()?.mUpLaserScanView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parent.get()?.mUpLaserScanView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //下激光点云
        mBinding.cbLowerLaserPointCloud.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parent.get()?.mDownLaserScanView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parent.get()?.mDownLaserScanView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //虚拟墙 
        mBinding.cbVirtualWall.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parent.get()?.mWallView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parent.get()?.mWallView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
//        //顶视觉路线
//        cbTopViewPath.setOnCheckedChangeListener { _, isChecked ->
//
//            if (isChecked) {
//                parent.get()?.mTopViewPathView?.visibility = View.VISIBLE
//            } else {
//                parent.get()?.mTopViewPathView?.visibility = View.GONE
//            }
//        }
//        //清扫区域
//        cbArea.setOnCheckedChangeListener { _, isChecked ->
//
//            if (isChecked) {
//                parent.get()?.mAreasView?.visibility = View.VISIBLE
//            } else {
//                parent.get()?.mAreasView?.visibility = View.GONE
//            }
//        }
//         //路径
//        cbWorldPath.setOnCheckedChangeListener { _, isChecked ->
//
//            if (isChecked) {
//                parent.get()?.mPathView?.visibility = View.VISIBLE
//            } else {
//                parent.get()?.mPathView?.visibility = View.GONE
//            }
//        }
//        //混行区
//        cbMixArea.setOnCheckedChangeListener { _, isChecked ->
//
//            if (isChecked) {
//                parent.get()?.mMixAreasView?.visibility = View.VISIBLE
//            } else {
//                parent.get()?.mMixAreasView?.visibility = View.GONE
//            }
//        }
//        //站点
//        cbStations.setOnCheckedChangeListener { _, isChecked ->
//
//            if (isChecked) {
//                parent.get()?.mStationView?.visibility = View.VISIBLE
//            } else {
//                parent.get()?.mStationView?.visibility = View.GONE
//            }
//        }
//        //在线点
//        cbOnlinePose.setOnCheckedChangeListener { _, isChecked ->
//
//            if (isChecked) {
//                parent.get()?.mOnlinePoseView?.visibility = View.VISIBLE
//            } else {
//                parent.get()?.mOnlinePoseView?.visibility = View.GONE
//            }
//
//        }
//        //充电站
//        cbChargeStation.setOnCheckedChangeListener { _, isChecked ->
//
//            if (isChecked) {
//                parent.get()?.mHomeDockView?.visibility = View.VISIBLE
//            } else {
//                parent.get()?.mHomeDockView?.visibility = View.GONE
//            }
//        }
    }

    /**
     * 设置当前地图名称
     */
    @SuppressLint("SetTextI18n")
    fun setMapName(name: String) {
        mBinding.tvCurrentMap.text = "${context.getString(R.string.current_map)}${name}"
    }

    @SuppressLint("SetTextI18n")
    fun setAgvX(x: Double) {
        mBinding.tvPointX.text = "X:${String.format("%.3f", x)}"
    }

    @SuppressLint("SetTextI18n")
    fun setAgvY(y: Double) {
        mBinding.tvPointY.text = "Y:${String.format("%.3f", y)}"
    }

    @SuppressLint("SetTextI18n")
    fun setAgvT(theta: Double) {
        mBinding.tvPointTheta.text = "T:${String.format("%.3f", Math.toRadians(theta).toFloat())}"
    }

    @SuppressLint("SetTextI18n")
    fun setAgvZ(z: Double) {
        mBinding.tvPointZ.text = "Z:${String.format("%.3f", Math.toRadians(z).toFloat())}"
    }

    /**
     * 设置屏幕坐标
     */
    @SuppressLint("SetTextI18n")
    fun setScreen(point: PointF) {
        mBinding.tvScreenPointX.text = "X:${String.format("%.3f", point.x)}"
        mBinding.tvScreenPointY.text = "Y:${String.format("%.3f", point.y)}"
    }

//    mSlamMapView.setOnWorldCoordinateListener {
//        mBinding.tvScreenPointX.text= "X:${String.format("%.3f", it.x)}"
//        mBinding.tvScreenPointY.text = "Y:${String.format("%.3f", it.y)}"
//    }

    /**
     * 获取上激光点云CheckBox
     */
    fun getCbUpLaserPointCloud(): CheckBox = mBinding.cbUpLaserPointCloud

    /**
     * 获取下激光点云CheckBox
     */
    fun getCbLowerLaserPointCloud(): CheckBox = mBinding.cbLowerLaserPointCloud

    /**
     * 获取虚拟墙CheckBox
     */
    fun getCbVirtualWall(): CheckBox = mBinding.cbVirtualWall

//    fun getcbTopViewPath(): CheckBox = cbTopViewPath
//
//    fun getCbArea(): CheckBox = cbArea
//
//    fun getCbWorldPath(): CheckBox = cbWorldPath
//
//    fun getCbMixArea(): CheckBox = cbMixArea
//
//    fun getCbStations(): CheckBox = cbStations
}
