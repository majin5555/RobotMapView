package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.LayoutInflater
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
    private val parentRef: WeakReference<MapView> = parent

    // 懒加载字符串资源，避免重复获取
    private val currentMapText by lazy { context.getString(R.string.current_map) }
    private val pointXText by lazy { "X:" }
    private val pointYText by lazy { "Y:" }
    private val pointTText by lazy { "T:" }
    private val pointZText by lazy { "Z:" }
    private val screenPointXText by lazy { "X:" }
    private val screenPointYText by lazy { "Y:" }

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
                parentRef.get()?.mUpLaserScanView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mUpLaserScanView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //下激光点云
        mBinding.cbLowerLaserPointCloud.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mDownLaserScanView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mDownLaserScanView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //虚拟墙
        mBinding.cbVirtualWall.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mWallView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mWallView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //顶视觉路线
        mBinding.cbTopViewPath.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mTopViewPathView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mTopViewPathView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //避让点
        mBinding.cbStations.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mStationView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mStationView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //上线点
        mBinding.cbOnlinePose.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mOnlinePoseView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mOnlinePoseView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //充电站
        mBinding.cbChargeStation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mHomeDockView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mHomeDockView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //乘梯点
        mBinding.cbElevator.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mElevatorView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mElevatorView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //定位区域
        mBinding.cbPositingArea.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mPostingAreasView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mPostingAreasView?.setDrawingEnabled(false) // 禁用绘制
            }
        }

        //清扫区域
        mBinding.cbArea.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mPolygonEditView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mPolygonEditView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //路径
        mBinding.cbWorldPath.setOnCheckedChangeListener { _, isChecked ->
//            if (isChecked) {
//                parentRef.get()?.mPathView?.setDrawingEnabled(true) // 启用绘制
//            } else {
//                parentRef.get()?.mPathView?.setDrawingEnabled(false) // 禁用绘制
//            }

        }
        //混行区
        mBinding.cbMixArea.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mMixAreaView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mMixAreaView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
    }

    /**
     * 设置当前地图名称
     */
    @SuppressLint("SetTextI18n")
    fun setMapName(name: String) {
        mBinding.tvCurrentMap.text = "${currentMapText}${name}"
    }

    @SuppressLint("SetTextI18n")
    fun setAgvX(x: Double) {
        mBinding.tvPointX.text = "${pointXText}${String.format("%.3f", x)}"
    }

    @SuppressLint("SetTextI18n")
    fun setAgvY(y: Double) {
        mBinding.tvPointY.text = "${pointYText}${String.format("%.3f", y)}"
    }

    @SuppressLint("SetTextI18n")
    fun setAgvT(theta: Double) {
        mBinding.tvPointTheta.text =
            "${pointTText}${String.format("%.3f", Math.toRadians(theta).toFloat())}"
    }

    @SuppressLint("SetTextI18n")
    fun setAgvZ(z: Double) {
        mBinding.tvPointZ.text =
            "${pointZText}${String.format("%.3f", Math.toRadians(z).toFloat())}"
    }

    /**
     * 设置屏幕坐标
     */
    @SuppressLint("SetTextI18n")
    fun setScreen(point: PointF) {
        mBinding.tvScreenPointX.text = "${screenPointXText}${String.format("%.3f", point.x)}"
        mBinding.tvScreenPointY.text = "${screenPointYText}${String.format("%.3f", point.y)}"
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

    /**
     * 获取顶视路线CheckBox
     */
    fun getCbTopViewPath(): CheckBox = mBinding.cbTopViewPath

    /**
     * 获取避让点CheckBox
     */
    fun getCbStations(): CheckBox = mBinding.cbStations

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理所有监听器，防止内存泄漏
        mBinding.cbUpLaserPointCloud.setOnCheckedChangeListener(null)
        mBinding.cbLowerLaserPointCloud.setOnCheckedChangeListener(null)
        mBinding.cbVirtualWall.setOnCheckedChangeListener(null)
        mBinding.cbTopViewPath.setOnCheckedChangeListener(null)
        mBinding.cbStations.setOnCheckedChangeListener(null)
        mBinding.cbOnlinePose.setOnCheckedChangeListener(null)
        mBinding.cbChargeStation.setOnCheckedChangeListener(null)
        mBinding.cbElevator.setOnCheckedChangeListener(null)
        mBinding.cbPositingArea.setOnCheckedChangeListener(null)
    }
}
