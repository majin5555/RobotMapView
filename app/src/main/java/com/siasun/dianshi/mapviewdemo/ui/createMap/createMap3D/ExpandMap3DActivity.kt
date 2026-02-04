package com.siasun.dianshi.mapviewdemo.ui.createMap.createMap3D

import android.annotation.SuppressLint
import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.ToastUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.ngu.lcmtypes.laser_t
import com.ngu.lcmtypes.robot_control_t
import com.siasun.dianshi.ConstantBase
import com.siasun.dianshi.GlobalVariable
import com.siasun.dianshi.base.BaseMvvmActivity
import com.siasun.dianshi.bean.ExpandArea
import com.siasun.dianshi.bean.SwitchMapBean
import com.siasun.dianshi.controller.MainController
import com.siasun.dianshi.dialog.CommonEditDialog
import com.siasun.dianshi.dialog.CommonWarnDialog
import com.siasun.dianshi.framework.ext.onClick
import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.mapviewdemo.CREATE_MAP
import com.siasun.dianshi.mapviewdemo.KEY_AGV_COORDINATE
import com.siasun.dianshi.mapviewdemo.KEY_CURRENT_POINT_CLOUD
import com.siasun.dianshi.mapviewdemo.KEY_LOCATION
import com.siasun.dianshi.mapviewdemo.KEY_NAV_HEARTBEAT_STATE
import com.siasun.dianshi.mapviewdemo.KEY_OPT_POSE
import com.siasun.dianshi.mapviewdemo.KEY_UPDATE_POS
import com.siasun.dianshi.mapviewdemo.R
import com.siasun.dianshi.mapviewdemo.TAG_NAV
import com.siasun.dianshi.mapviewdemo.databinding.ActivityExpandMap3dDactivityBinding
import com.siasun.dianshi.mapviewdemo.viewmodel.CreateMap2DViewModel
import com.siasun.dianshi.view.WorkMode
import com.siasun.dianshi.view.createMap.ExpandAreaView
import java.util.Timer
import java.util.TimerTask

/**
 * 扩展3D地图
 */
class ExpandMap3DActivity :
    BaseMvvmActivity<ActivityExpandMap3dDactivityBinding, CreateMap2DViewModel>() {
    //建图心跳定时器
    private val mTimer = Timer()

    val mapID = 100

    //    val list: MutableList<ExpandArea> = mutableListOf()
    var mExpandArea: ExpandArea = ExpandArea(PointF(0f, 0f), PointF(0f, 0f))
    val robotPose = DoubleArray(6)

    @RequiresApi(Build.VERSION_CODES.R)
    override fun initView(savedInstanceState: Bundle?) {
        MainController.init()

        mTimer.schedule(object : TimerTask() {
            override fun run() {
                if (GlobalVariable.SEND_NAVI_HEART) {
                    MainController.myController.mSendNaviHeartBeat()
                }
            }
        }, 0, 500)

        //加载地图
        mBinding.mapView.loadMap(
            ConstantBase.getFilePath(mapID, ConstantBase.PAD_MAP_NAME_PNG),
            ConstantBase.getFilePath(mapID, ConstantBase.PAD_MAP_NAME_YAML)
        )

        //保存
        mBinding.tvSave.onClick {
            showSavaMapDialog()
        }
        //定位
        mBinding.btnLocation.onClick {
            ToastUtils.showShort("00定位")
            mViewModel.switchMapInfo(SwitchMapBean(100, 0.0, 0.0, 0.0, source = 10))
        }

        //停止扫描
        mBinding.tvStop.onClick {
            //结束建图指令
            MainController.stopCreateEnvironment()
            LogUtil.i("停止扫描")
            ToastUtils.showShort("停止扫描")
        }

        //扩展地图
        expandMap()
        //更新地图
        updateMap()
    }

    private fun updateMap() {

        //扩展地图
        mBinding.tvUpdate.onClick {
            mBinding.mapView.isStartRevSubMaps = false
            mBinding.mapView.setWorkMode(WorkMode.MODE_CREATE_MAP)


            //更新地图
            CommonEditDialog.Builder(this).setOnCommonEditDialogListener(object :
                CommonEditDialog.Builder.CommonEditDialogListener {
                override fun confirm(str: String) {

                    val dParams = DoubleArray(12)

                    LogUtil.i("robotPose = ${robotPose}")
                    dParams[0] = robotPose[0]
                    dParams[1] = robotPose[1]
                    dParams[2] = robotPose[2]
                    dParams[3] = robotPose[3]
                    dParams[4] = robotPose[4]
                    dParams[5] = robotPose[5]
                    dParams[6] = (-100).toDouble()
                    dParams[7] = str.toDouble()
                    dParams[8] = mExpandArea.start.x.toDouble()
                    dParams[9] = mExpandArea.start.y.toDouble()
                    dParams[10] = mExpandArea.end.x.toDouble()
                    dParams[11] = mExpandArea.end.y.toDouble()

                    MainController.send3DUpdateMap(dParams, mapID)

//                    ToastUtils.showLong(resources.getString(R.string.update_map_3d_star))

                    LogUtil.i("dParams = ${dParams.contentToString()}")
                    LogUtil.i("开始更新")
                }


            }).setTitle("请输入最大高度").setMsg(2.5.toString()).create().show()

            showLoading("开始扩展")
            ToastUtils.showShort("开始扩展")
            LogUtil.i("开始扩展", null, TAG_NAV)
        }


        //扩展地图 添加区域
        mBinding.btnSettingArea.onClick {
            mBinding.mapView.setWorkMode(WorkMode.MODE_EXTEND_MAP_ADD_REGION)
        }
        //添加区域监听 获取添加区域的世界坐标
        mBinding.mapView.getExpandAreaView()!!
            .setOnExpandAreaCreatedListener(object : ExpandAreaView.OnExpandAreaCreatedListener {
                override fun onExpandAreaCreated(area: ExpandArea) {
                    mExpandArea = area
                    LogUtil.i("扩展地图 $area")
                }
            })

        //扩展地图 重制区域
        mBinding.btnCleanArea.onClick {
//            list.clear()
            mBinding.mapView.resetExpandAreaView()
        }
    }

    /**
     * 地图更新（绘制区域）
     * 地图扩展（不绘制）
     *
     * 局部更新 扩展地图都传list
     * 局部更新list有数据
     * 扩展地图list无数据
     */
    private fun expandMap() {
        //扩展地图
        mBinding.tvExpend.onClick {
            mBinding.mapView.isStartRevSubMaps = false
            mBinding.mapView.setWorkMode(WorkMode.MODE_CREATE_MAP)

            MainController.startExtendMap(0, robotPose, mapID)

//            //更新地图
//            CommonEditDialog.Builder(this).setOnCommonEditDialogListener(object :
//                CommonEditDialog.Builder.CommonEditDialogListener {
//                override fun confirm(str: String) {
//
//                        val dParams = DoubleArray(12)
//
//                        LogUtil.i("mRobotCoordinateBean = ${mRobotCoordinateBean}")
//                        dParams[0] = mRobotCoordinateBean.x
//                        dParams[1] = mRobotCoordinateBean.y
//                        dParams[2] = mRobotCoordinateBean.t
//                        dParams[3] = mRobotCoordinateBean.z
//                        dParams[4] = mRobotCoordinateBean.roll
//                        dParams[5] = mRobotCoordinateBean.pitch
//                        dParams[6] = (-100).toDouble()
//                        dParams[7] = str.toDouble()
//                        dParams[8] = partialUpdate.start.x.toDouble()
//                        dParams[9] = partialUpdate.start.y.toDouble()
//                        dParams[10] = partialUpdate.end.x.toDouble()
//                        dParams[11] = partialUpdate.end.y.toDouble()
//                        MainController.send3DUpdateMap(dParams, mapID)
//
//                        ToastUtils.showLong(resources.getString(R.string.update_map_3d_star))
//
//                        LogUtil.i("dParams = ${dParams.contentToString()}")
//                        LogUtil.i("开始更新")
//                    }
//
//
//            }).setTitle("请输入最大高度").setMsg(2.5.toString()).create().show()

            showLoading("开始扩展")
            ToastUtils.showShort("开始扩展")
            LogUtil.i("开始扩展", null, TAG_NAV)
        }

    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun initData() {
        super.initData()

        //上激光点云
        LiveEventBus.get<laser_t>(KEY_CURRENT_POINT_CLOUD).observe(this) {
            mBinding.mapView.loadCurPointCloud(it)
        }

        //建图导航心跳
        LiveEventBus.get(KEY_NAV_HEARTBEAT_STATE, ByteArray::class.java).observe(this) {
            navHeartbeatState(it)
        }

        //接收创建地图中车体位置 导航->PAD
        LiveEventBus.get(KEY_UPDATE_POS, laser_t::class.java).observe(this) {
            mBinding.mapView.parseLaserData(it, 1)
            if (it.rad0 > 0f) {
                mBinding.tvMapSteps.text = "建图步数 ${it.rad0}"
            }
        }
        //NAV 做回环时候给的数据  (NAV->PAD)
        LiveEventBus.get(KEY_OPT_POSE, laser_t::class.java).observe(this) {
            mBinding.mapView.parseOptPose(it)
        }
        //接收车体坐标
        LiveEventBus.get<robot_control_t>(KEY_AGV_COORDINATE).observe(this) {
//            mBinding.mapView.setAgvPose(it)
            robotPose[0] = it.dparams[0]
            robotPose[1] = it.dparams[1]
            robotPose[2] = Math.toRadians(it.dparams[2])
            if (it.dparams.size > 8) {
                robotPose[3] = it.dparams[8]
                robotPose[4] = it.dparams[9]
                robotPose[5] = it.dparams[10]
            }
        }

        //接收定位信息
        LiveEventBus.get(KEY_LOCATION, Int::class.java).observeSticky(this) {
            if (it == 1) {
                mBinding.tvLocation.text = "定位成功"

            } else {
                mBinding.tvLocation.text = "定位失败"
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
                    GlobalVariable.SEND_NAVI_HEART = false
                    MainController.sendOnlinePoint(
                        100, mBinding.mapView.robotPose
                    )

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
                    GlobalVariable.SEND_NAVI_HEART = false

//                    if (mBinding.mapView.isRouteMap) {
//                        LogUtil.i("弹框---是否旋转地图", null, TAG_NAV)
//                    showRouteMapDialog()
//                    } else {
//                    LogUtil.i("没有收到任何子图，直接询问是否保存地图", null, TAG_NAV)
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
     * 弹出是否保存地图弹框
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun showSavaMapDialog() {
        CommonWarnDialog.Builder(this).setMsg("保存地图").setOnCommonWarnDialogListener(object :
            CommonWarnDialog.Builder.CommonWarnDialogListener {
            override fun confirm() {
                LogUtil.i("mBinding.mapView.mMapRotateRadians ${mBinding.mapView.rotationRadians}")
                //开始保存地图
                MainController.saveEnvironment(
                    1, rotate = mBinding.mapView.rotationRadians, mapId = mapID
                )
                GlobalVariable.SEND_NAVI_HEART = true
//                    showLoading("保存地图中")
                LogUtil.i("确定要保存地图么...点击确定", null, TAG_NAV)
            }

            override fun discard() {
                mBinding.mapView.isMapping = false
                MainController.saveEnvironment(2, mapId = mapID)
                GlobalVariable.SEND_NAVI_HEART = false
                LogUtil.i("确定要保存地图么...点击取消", null, TAG_NAV)
                finish()
            }
        }).create().show()
    }
}