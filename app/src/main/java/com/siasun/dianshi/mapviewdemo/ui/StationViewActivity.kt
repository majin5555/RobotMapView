package com.siasun.dianshi.mapviewdemo.ui

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.jeremyliao.liveeventbus.LiveEventBus
import com.ngu.lcmtypes.laser_t
import com.ngu.lcmtypes.robot_control_t
import com.siasun.dianshi.base.BaseMvvmActivity
import com.siasun.dianshi.ConstantBase
import com.siasun.dianshi.controller.MainController
import com.siasun.dianshi.framework.ext.onClick
import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.mapviewdemo.KEY_AGV_COORDINATE
import com.siasun.dianshi.mapviewdemo.KEY_BOTTOM_CURRENT_POINT_CLOUD
import com.siasun.dianshi.mapviewdemo.KEY_CURRENT_POINT_CLOUD
import com.siasun.dianshi.mapviewdemo.R
import com.siasun.dianshi.mapviewdemo.RunningState
import com.siasun.dianshi.mapviewdemo.TaskState
import com.siasun.dianshi.mapviewdemo.databinding.ActivityStationBinding
import com.siasun.dianshi.mapviewdemo.databinding.ActivityVirtualwallBinding
import com.siasun.dianshi.mapviewdemo.viewmodel.ShowMapViewModel
import com.siasun.dianshi.utils.YamlNew
import com.siasun.dianshi.view.MapView
import com.siasun.dianshi.xpop.XpopUtils
import java.io.File

/**
 * 站点
 */
class StationViewActivity : BaseMvvmActivity<ActivityStationBinding, ShowMapViewModel>() {

    val mapId = 1

    @RequiresApi(Build.VERSION_CODES.R)
    override fun initView(savedInstanceState: Bundle?) {
        MainController.init()

        val file = File(ConstantBase.getFilePath(mapId, ConstantBase.PAD_MAP_NAME_PNG))
        Glide.with(this).asBitmap().load(file).skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE).into(object : SimpleTarget<Bitmap?>() {
                override fun onResourceReady(
                    resource: Bitmap, transition: Transition<in Bitmap?>?
                ) {
                    val mPngMapData = YamlNew().loadYaml(
                        ConstantBase.getFilePath(mapId, ConstantBase.PAD_MAP_NAME_YAML),
                        resource.height.toFloat(),
                        resource.width.toFloat(),
                    )
                    mBinding.mapView.setBitmap(mPngMapData, resource)
                }
            })

        initListener()

    }

    override fun initData() {
        super.initData()
        //上激光点云
        LiveEventBus.get<laser_t>(KEY_CURRENT_POINT_CLOUD).observe(this) {
            mBinding.mapView.setUpLaserScan(it)
        }

        //下激光点云
        LiveEventBus.get<laser_t>(KEY_BOTTOM_CURRENT_POINT_CLOUD).observe(this) {
            mBinding.mapView.setDownLaserScan(it)
        }

        //接收车体坐标 AGV->PAD
        LiveEventBus.get<robot_control_t>(KEY_AGV_COORDINATE).observe(this) {
            mBinding.mapView.setAgvPose(it)

            //有任务才显示车体位置
            if (RunningState.CURRENT_TASK_STATE == TaskState.HAVE_TASK) {
                mBinding.mapView.setWorkingPath(it.dparams)
            }
        }


        mViewModel.getStationData(mapId, onComplete = { cmsStation ->
            LogUtil.d(cmsStation.toString())
            mBinding.mapView.setCmsStations(cmsStation)
        })
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun initListener() {
        mBinding.btnCreate.onClick {
            XpopUtils(this).showCmsStationDialog(onConfirmCall = { result ->
                result?.let {
//                            cmsStations.add(it)
//                            mBinding.mapView.setCmsStations(cmsStations)
//                            mViewModel.saveCmsStation(info.mapId, cmsStations)
                }

            }, onDeleteCall = {

            }, mapId
            )
        }

        mBinding.btnConfirm.onClick {

        }

    }

}