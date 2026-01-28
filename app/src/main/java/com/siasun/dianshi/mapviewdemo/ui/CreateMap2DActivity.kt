package com.siasun.dianshi.mapviewdemo.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.ToastUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.GlobalVariable.SEND_NAVI_HEART
import com.siasun.dianshi.base.BaseMvvmActivity
import com.siasun.dianshi.controller.MainController
import com.siasun.dianshi.createMap2D.CreateMapView2D
import com.siasun.dianshi.createMap2D.CreateMapView2D1
import com.siasun.dianshi.dialog.CommonWarnDialog
import com.siasun.dianshi.framework.ext.onClick
import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.mapviewdemo.CREATE_MAP
import com.siasun.dianshi.mapviewdemo.KEY_NAV_HEARTBEAT_STATE
import com.siasun.dianshi.mapviewdemo.KEY_OPT_POSE
import com.siasun.dianshi.mapviewdemo.KEY_UPDATE_POS
import com.siasun.dianshi.mapviewdemo.KEY_UPDATE_SUB_MAPS
import com.siasun.dianshi.mapviewdemo.TAG_NAV
import com.siasun.dianshi.mapviewdemo.databinding.ActivityCreateMap2DactivityBinding
import com.siasun.dianshi.mapviewdemo.viewmodel.CreateMap2DViewModel
import java.util.Timer
import java.util.TimerTask

/**
 * 创建2D地图
 */
class CreateMap2DActivity :
    BaseMvvmActivity<ActivityCreateMap2DactivityBinding, CreateMap2DViewModel>() {
    //建图心跳定时器
    private val mTimer = Timer()

    val mapID = 100
    @RequiresApi(Build.VERSION_CODES.R)
    override fun initView(savedInstanceState: Bundle?) {
        MainController.init()

        mTimer.schedule(object : TimerTask() {
            override fun run() {
                if (SEND_NAVI_HEART) {
                    MainController.myController.mSendNaviHeartBeat()
                }
            }
        }, 0, 500)

        //保存
        mBinding.tvSave.onClick {
            showSavaMapDialog()

        }

        //开始扫描
        mBinding.tvCreate.onClick {
            mBinding.mapView.isStartRevSubMaps = false
            mBinding.mapView.setWorkMode(CreateMapView2D.WorkMode.MODE_CREATE_MAP)
            MainController.startCreateEnvironment()
            showLoading("开始扫描")
            ToastUtils.showShort("开始扫描")
            LogUtil.i("开始扫描", null, TAG_NAV)
        }


        //停止扫描
        mBinding.tvStop.onClick {
            //结束建图指令
            MainController.stopCreateEnvironment()
            LogUtil.i("停止扫描")
            ToastUtils.showShort("停止扫描")
        }


    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun initData() {
        super.initData()
        //建图导航心跳
        LiveEventBus.get(KEY_NAV_HEARTBEAT_STATE, ByteArray::class.java).observe(this) {
            navHeartbeatState(it)
        }
        //接收子图数据
        LiveEventBus.get(KEY_UPDATE_SUB_MAPS, laser_t::class.java).observe(this) {
            mBinding.mapView.parseSubMaps2D(it, 1)
        }
        //子图优化 回环检测
        LiveEventBus.get(KEY_OPT_POSE, laser_t::class.java).observe(this) {
//            mBinding.mapView.updateOptPose2D(it, 1)
        }
        //接收创建地图中车体位置 导航->PAD
        LiveEventBus.get(KEY_UPDATE_POS, laser_t::class.java).observe(this) {
            mBinding.mapView.parseLaserData2D(it)
            if (it.rad0 > 0f) {
                mBinding.tvMapSteps.text = "建图步数 ${it.rad0}"
            }
        }

    }

    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun navHeartbeatState(it: ByteArray) {
        //扫描新环境和扩展环境 都不锁屏
//        LogUtil.i("导航心跳 MSG_NAVI_STATE it[0]---${it[0]}")
//        LogUtil.i("导航心跳 MSG_NAVI_STATE it[1]---${it[1]}")
//        LogUtil.i("iParams[2].toInt()---${it[2]}")
        //导航当前状态
        when (it[0].toInt()) {
            //定位
            1 -> {
                if (mBinding.mapView.isMapping) {
                    LogUtil.i(
                        "此时导航从其他模式切换到定位，说明导航已经建图、优化、保存完成", null, TAG_NAV
                    )

                    mViewModel.downPngYaml(CREATE_MAP, 1)

                    //从建图模式到定位模式 后恢复地图不可旋转
                    mBinding.mapView.isRouteMap = false
                    SEND_NAVI_HEART = false

                }
                mBinding.mapView.isMapping = false
            }
            //开始建图
            2 -> {
                if (!mBinding.mapView.isMapping) {
                    mBinding.mapView.isMapping = true
                    dismissLoading()
                    //it[2].toInt()  0 新建 1扩展
                    if (it[2].toInt() == 0) {
//                        mBinding.tvCreate.visibility = android.view.View.GONE
//                        mBinding.tvStop.visibility = android.view.View.VISIBLE
                    }
                }
            }
            //结束建图
            3 -> {}
            //开始录制dx
            4 -> LogUtil.d("录制DX ing", null, TAG_NAV)

        }
        //结束建图时，后端优化状态
        when (it[1].toInt()) {
            //正在优化中
            1 -> {
                LogUtil.i("地图正在优化中", null, TAG_NAV)
                ToastUtils.showShort("地图正在优化中")
            }
            //优化完成，询问pad是否保存地图
            2 -> {
                if (mBinding.mapView.isStartRevSubMaps) {
                    SEND_NAVI_HEART = false

//                    if (mBinding.mapView.isRouteMap) {
//                        LogUtil.i("弹框---是否旋转地图", null, TAG_NAV)
//                        showRouteMapDialog()
//                    } else {
                        LogUtil.i("没有收到任何子图，直接询问是否保存地图", null, TAG_NAV)
                        showSavaMapDialog()
//                    }
                    mBinding.mapView.isStartRevSubMaps = false
                }
            }
            //正在保存地图
            3 -> {}
            //正在取消保存地图
            4 -> {}
            else -> {}
        }

    }


    /**
     * 弹出是否旋转地图弹框
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun showRouteMapDialog() {
//        CommonWarnDialog.Builder(this).setMsg(R.string.warm_18)
//            .setOnCommonWarnDialogListener(object :
//                CommonWarnDialog.Builder.CommonWarnDialogListener {
//                @RequiresApi(Build.VERSION_CODES.R)
//                override fun discard() {
//                    mBinding.llRotate.gone()
//                    showSavaMapDialog()
//                }
//
//                override fun confirm() {
//                    mBinding.llRotate.visible()
//                }
//            }).create().show()
    }

    /**
     * 弹出是否保存地图弹框
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun showSavaMapDialog() {
        CommonWarnDialog.Builder(this).setMsg("保存地图").setOnCommonWarnDialogListener(object :
                CommonWarnDialog.Builder.CommonWarnDialogListener {
                override fun confirm() {
                    //开始保存地图
                    MainController.saveEnvironment(1, mapId = mapID)
                    SEND_NAVI_HEART = true
//                    showLoading("保存地图中")
                    LogUtil.i("确定要保存地图么...点击确定", null, TAG_NAV)
                }

                override fun discard() {
                    mBinding.mapView.isMapping = false
                    MainController.saveEnvironment(2, mapId = mapID)
                    SEND_NAVI_HEART = false
                    LogUtil.i("确定要保存地图么...点击取消", null, TAG_NAV)
                    finish()
                }
            }).create().show()
    }
}