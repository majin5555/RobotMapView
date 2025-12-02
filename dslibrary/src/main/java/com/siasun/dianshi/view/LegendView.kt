package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
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

    private var mCurrentMapName: TextView//当前地图
    private var mTvX: TextView//X
    private var mTvY: TextView//Y
    private var mTvTheta: TextView//T
    private var mTvZ: TextView//Z
    private var mTvScrX: TextView//屏幕转世界x
    private var mTvScrY: TextView//屏幕转世界y

    private var cbUpPoint: CheckBox//上激光点云选择框
    private var cbDownPoint: CheckBox//下激光点云选择框
//    private var cbVirtualWall: CheckBox//虚拟墙选择框
//    private var cbTopViewPath: CheckBox//顶视路线选择框
//    private var cbArea: CheckBox//区域显示
//    private var cbWorldPath: CheckBox//路线显示
//    private var cbMixArea: CheckBox//混行区显示
//    private var cbStations: CheckBox//站点显示
//    private var cbOnlinePose: CheckBox//上线点
//    private var cbChargeStation: CheckBox//充电站

    init {
        init(context, attrs)

        LayoutInflater.from(context).inflate(R.layout.map_view_legend, this)
        mCurrentMapName = findViewById(R.id.tv_current_map)
        mTvX = findViewById(R.id.tv_point_x)
        mTvY = findViewById(R.id.tv_point_y)
        mTvTheta = findViewById(R.id.tv_point_theta)
        mTvZ = findViewById(R.id.tv_point_z)
        mTvScrX = findViewById(R.id.tv_screen_point_x)
        mTvScrY = findViewById(R.id.tv_screen_point_y)
        cbUpPoint = findViewById(R.id.cb_up_laser_point_cloud)
        cbDownPoint = findViewById(R.id.cb_lower_laser_point_cloud)
//        cbVirtualWall = findViewById(R.id.cb_virtual_wall)
//        cbTopViewPath = findViewById(R.id.cb_top_view_route)
//        cbArea = findViewById(R.id.cb_area)
//        cbWorldPath = findViewById(R.id.cb_world_path)
//        cbMixArea = findViewById(R.id.cb_mix_area)
//        cbStations = findViewById(R.id.cb_stations)
//        cbOnlinePose = findViewById(R.id.cb_online_pose)
//        cbChargeStation = findViewById(R.id.cb_charge_station)

        // 为每个CheckBox添加点击变化监听器
        //上激光点云
        cbUpPoint.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {
                parent.get()?.mUpLaserScanView?.visibility = View.VISIBLE
            } else {
                parent.get()?.mUpLaserScanView?.visibility = View.GONE
            }
        }
        //下激光点云
        cbDownPoint.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {
                parent.get()?.mDownLaserScanView?.visibility = View.VISIBLE
            } else {
                parent.get()?.mDownLaserScanView?.visibility = View.GONE
            }
        }
        //虚拟墙 
//        cbVirtualWall.setOnCheckedChangeListener { _, isChecked ->
//
//            if (isChecked) {
//                parent.get()?.mWallView?.visibility = View.VISIBLE
//            } else {
//                parent.get()?.mWallView?.visibility = View.GONE
//            }
//        }
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
     * 初始化
     *
     * @param context 上下文
     * @param attrs   attrs
     */
    private fun init(context: Context, attrs: AttributeSet?) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mBinding = MapViewLegendBinding.inflate(inflater, this, true)
    }

    /**
     * 设置当前地图名称
     */
    @SuppressLint("SetTextI18n")
    fun setMapName(name: String) {
        mCurrentMapName.text = "${context.getString(R.string.current_map)}${name}"
    }

    @SuppressLint("SetTextI18n")
    fun setAgvX(x: Double) {
        mTvX.text = "X:${String.format("%.3f", x)}"
    }

    @SuppressLint("SetTextI18n")
    fun setAgvY(y: Double) {
        mTvY.text = "Y:${String.format("%.3f", y)}"

    }

    @SuppressLint("SetTextI18n")
    fun setAgvT(theta: Double) {
        mTvTheta.text = "T:${String.format("%.3f", Math.toRadians(theta).toFloat())}"
    }

    @SuppressLint("SetTextI18n")
    fun setAgvZ(z: Double) {
        mTvTheta.text = "Z:${String.format("%.3f", Math.toRadians(z).toFloat())}"
    }

    /**
     * 设置屏幕坐标
     */
    @SuppressLint("SetTextI18n")
    fun setScreen(point: PointF) {
        mTvScrX.text = "X:${String.format("%.3f", point.x)}"
        mTvScrY.text = "Y:${String.format("%.3f", point.y)}"
    }

//    mSlamMapView.setOnWorldCoordinateListener {
//        mTvScrX.text = "X:${String.format("%.3f", it.x)}"
//        mTvScrY.text = "Y:${String.format("%.3f", it.y)}"
//    }

    fun getCbUpPoint(): CheckBox = cbUpPoint

    fun getCbDownPoint(): CheckBox = cbDownPoint

//    fun getCbVirtualWall(): CheckBox = cbVirtualWall
//
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
