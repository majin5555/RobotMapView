package com.siasun.dianshi.mapviewdemo.ui

import android.os.Bundle
import android.util.Log
import com.jeremyliao.liveeventbus.LiveEventBus
import com.ngu.lcmtypes.laser_t
import com.ngu.lcmtypes.robot_control_t
import com.siasun.dianshi.ConstantBase
import com.siasun.dianshi.base.BaseMvvmActivity
import com.siasun.dianshi.controller.MainController
import com.siasun.dianshi.framework.ext.onClick
import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.mapviewdemo.KEY_AGV_COORDINATE
import com.siasun.dianshi.mapviewdemo.KEY_BOTTOM_CURRENT_POINT_CLOUD
import com.siasun.dianshi.mapviewdemo.KEY_CURRENT_POINT_CLOUD
import com.siasun.dianshi.mapviewdemo.R
import com.siasun.dianshi.mapviewdemo.RunningState
import com.siasun.dianshi.mapviewdemo.TaskState
import com.siasun.dianshi.mapviewdemo.databinding.ActivityVirtualwallBinding
import com.siasun.dianshi.mapviewdemo.viewmodel.ShowMapViewModel
import com.siasun.dianshi.network.constant.KEY_NEY_IP
import com.siasun.dianshi.view.WorkMode
import com.tencent.mmkv.MMKV

/**
 * 虚拟墙
 */
class VirtualWallViewActivity : BaseMvvmActivity<ActivityVirtualwallBinding, ShowMapViewModel>() {

    val mapId = 1
    override fun initView(savedInstanceState: Bundle?) {
        MMKV.defaultMMKV().encode(KEY_NEY_IP, "192.168.3.101");

        MainController.init()
        //加载地图
        mBinding.mapView.loadMap(
            ConstantBase.getFilePath(mapId, ConstantBase.PAD_MAP_NAME_PNG),
            ConstantBase.getFilePath(mapId, ConstantBase.PAD_MAP_NAME_YAML)
        )

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
        //加载虚拟墙
        mViewModel.getVirtualWall(mapId, onComplete = { virtualWall ->
            LogUtil.d("加载虚拟墙 ${virtualWall}")
            virtualWall?.let {
                mBinding.mapView.setVirtualWall(it)
            }
        })
    }

    private fun initListener() {

        mBinding.rgTabType.setOnCheckedChangeListener { _, checkedId ->

            when (checkedId) {

                R.id.rb_create -> {
                    // 创建虚拟墙模式
                    mBinding.mapView.setWorkMode(WorkMode.MODE_VIRTUAL_WALL_ADD)
                    // 默认创建普通虚拟墙
                    mBinding.mapView.addVirtualWall(3)
                }

                R.id.rb_edit -> {
                    // 编辑虚拟墙模式
                    mBinding.mapView.setWorkMode(WorkMode.MODE_VIRTUAL_WALL_EDIT)
                }

                R.id.rb_delete -> {
                    // 删除虚拟墙模式
                    mBinding.mapView.setWorkMode(WorkMode.MODE_VIRTUAL_WALL_DELETE)
                }
            }
        }
        //保存
        mBinding.btnConfirm.onClick {
            Log.d("VirtualWallViewActivity", mBinding.mapView.getVirtualWall().toString())
            mBinding.mapView.getVirtualWall()
                ?.let { it1 ->
                    it1.LAYERSUM = 1
                    mViewModel.saveVirtualWall(mapId, it1)
                }
        }

    }

}