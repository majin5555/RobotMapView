package com.siasun.dianshi.mapviewdemo.ui.createMap

import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.ConstantBase
import com.siasun.dianshi.base.BaseMvvmActivity
import com.siasun.dianshi.bean.DragLocationBean
import com.siasun.dianshi.controller.MainController
import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.mapviewdemo.KEY_LOCATION_DRAG
import com.siasun.dianshi.mapviewdemo.databinding.ActivityShowMapViewBinding
import com.siasun.dianshi.mapviewdemo.viewmodel.ShowMapViewModel
import com.siasun.dianshi.view.MapView

/**
 * 拖拽定位地图
 */
class DragPositionViewActivity : BaseMvvmActivity<ActivityShowMapViewBinding, ShowMapViewModel>() {

    val mapId = 1
    var mDragBean: DragLocationBean? = null
    private var mLasertData: laser_t = laser_t()

    @RequiresApi(Build.VERSION_CODES.R)
    override fun initView(savedInstanceState: Bundle?) {
        MainController.init()
        mDragBean = intent.getSerializableExtra(KEY_LOCATION_DRAG) as DragLocationBean?
        mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_DRAG_POSITION)

        mDragBean?.upRCData?.let {
            mLasertData.ranges = it.f_create_map_data
        }

        mBinding.mapView.setDragPositionData(mLasertData)

        //加载地图
        mBinding.mapView.loadMap(
            ConstantBase.getFilePath(mapId, ConstantBase.PAD_MAP_NAME_PNG),
            ConstantBase.getFilePath(mapId, ConstantBase.PAD_MAP_NAME_YAML)
        )

        //点击屏幕回调
        mBinding.mapView.setSingleTapListener(object : MapView.ISingleTapListener {
            override fun onSingleTapListener(point: PointF) {
                LogUtil.i("点击屏幕回调  ${point}")
                //上激光
                mLasertData.ranges[0] = point.x
                mLasertData.ranges[1] = point.y

                LogUtil.i("点击屏幕回调x  ${mLasertData.ranges[0]}")
                LogUtil.i("点击屏幕回调y ${mLasertData.ranges[1]}")
                LogUtil.i("点击屏幕回调t ${mLasertData.ranges[2]}")

                mBinding.mapView.setDragPositionData(mLasertData)

            }
        })


        val dragRobotPose = mBinding.mapView.getDragRobotPose()
    }

}