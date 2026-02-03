package com.siasun.dianshi.mapviewdemo.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.ToastUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.jxd.jxd_core.intent.startActivity
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.GlobalVariable.SEND_NAVI_HEART
import com.siasun.dianshi.base.BaseMvvmActivity
import com.siasun.dianshi.bean.ConstraintNode
import com.siasun.dianshi.bean.UpdateMapBean
import com.siasun.dianshi.controller.MainController
import com.siasun.dianshi.dialog.CommonEditDialog
import com.siasun.dianshi.dialog.CommonWarnDialog
import com.siasun.dianshi.framework.ext.onClick
import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.mapviewdemo.CREATE_MAP
import com.siasun.dianshi.mapviewdemo.KEY_CONFIGURATION_PARAMETERS
import com.siasun.dianshi.mapviewdemo.KEY_CONFIGURATION_PARAMETERS_RESULT
import com.siasun.dianshi.mapviewdemo.KEY_CONSTRAINT_CONSTRAINT_NODE_RESULT
import com.siasun.dianshi.mapviewdemo.KEY_CONSTRAINT_NODE
import com.siasun.dianshi.mapviewdemo.KEY_NAV_HEARTBEAT_STATE
import com.siasun.dianshi.mapviewdemo.KEY_OPT_POSE
import com.siasun.dianshi.mapviewdemo.KEY_UPDATE_MAP
import com.siasun.dianshi.mapviewdemo.KEY_UPDATE_POS
import com.siasun.dianshi.mapviewdemo.TAG_NAV
import com.siasun.dianshi.mapviewdemo.databinding.ActivityCreateMap3dDactivityBinding
import com.siasun.dianshi.mapviewdemo.viewmodel.CreateMap3DViewModel
import com.siasun.dianshi.network.constant.KEY_NEY_IP
import com.siasun.dianshi.utils.RadianUtil
import com.siasun.dianshi.view.createMap.CreateMapWorkMode
import com.tencent.mmkv.MMKV
import java.util.Timer
import java.util.TimerTask

/**
 * 创建3D地图
 */
class CreateMap3DActivity :
    BaseMvvmActivity<ActivityCreateMap3dDactivityBinding, CreateMap3DViewModel>() {
    //建图心跳定时器
    private val mTimer = Timer()

    val mapID = 100

    @RequiresApi(Build.VERSION_CODES.R)
    override fun initView(savedInstanceState: Bundle?) {
        MainController.init()
        MMKV.defaultMMKV().encode(KEY_NEY_IP, "192.168.1.198");
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
            mBinding.mapView.setWorkMode(CreateMapWorkMode.MODE_CREATE_MAP)
            MainController.startCreateEnvironment()
            showLoading("开始扫描")
            ToastUtils.showShort("开始扫描")
            LogUtil.i("开始扫描", null, TAG_NAV)
        }
        //添加节点
        mBinding.btnAddNode.onClick {
            MainController.send3DConstraintNode()
        }
        //匹配节点
        mBinding.btnMatchNode.onClick {
            CommonEditDialog.Builder(this).setOnCommonEditDialogListener(object :
                CommonEditDialog.Builder.CommonEditDialogListener {
                override fun confirm(str: String) {
                    MainController.send3DMatchingNode(str.toInt())
                }
            }).setTitle("请输入约束节点ID").create().show()
        }
        //修改配资
        mBinding.btnMatchNode.onClick {
            MainController.send3DReadConfig()
        }

        //停止扫描
        mBinding.tvStop.onClick {
            //结束建图指令
            MainController.stopCreateEnvironment()
            LogUtil.i("停止扫描")
            ToastUtils.showShort("停止扫描")
        }
        mBinding.tvExpend.onClick {
            startActivity<ExpandMap3DActivity>()
        }
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun initData() {
        super.initData()
        //下载地图结果
        LiveEventBus.get(KEY_UPDATE_MAP, UpdateMapBean::class.java).observe(this) {
            ToastUtils.showLong("PM.yaml  && PM.png 下载成功")
            startActivity<ExpandMap3DActivity>()
        }
        //建图导航心跳
        LiveEventBus.get(KEY_NAV_HEARTBEAT_STATE, ByteArray::class.java).observe(this) {
            navHeartbeatState(it)
        }
        //接收创建地图中车体位置 导航->PAD
        LiveEventBus.get(KEY_UPDATE_POS, laser_t::class.java).observe(this) {
            mBinding.mapView.parseLaserData(it)
            if (it.rad0 > 0f) {
                mBinding.tvMapSteps.text = "步数:${it.rad0}"
            }
        }

        //NAV 做回环时候给的数据  (NAV->PAD)
        LiveEventBus.get(KEY_OPT_POSE, laser_t::class.java).observe(this) {
            mBinding.mapView.parseOptPose(it)
        }

        //接收约束节点数据
        LiveEventBus.get<ConstraintNode>(KEY_CONSTRAINT_NODE).observe(this) {
            mBinding.mapView.addConstraintNodes(it)
        }

        //接收约束节点匹配结果
        LiveEventBus.get<Int>(KEY_CONSTRAINT_CONSTRAINT_NODE_RESULT).observe(this) {
            when (it) {
                0 -> {
                    ToastUtils.showLong("匹配成功")
                }

                1 -> {
                    ToastUtils.showLong("匹配失败")
                }
            }
        }

        //接收配置参数
        LiveEventBus.get<DoubleArray>(KEY_CONFIGURATION_PARAMETERS).observe(this) {
            LogUtil.w("接收配置参数 ${it}", null, TAG_NAV)

//            val list: MutableList<ConfigParam> = mutableListOf()
//            for (d in it) {
//                list.add(ConfigParam("", d))
//            }
//
//            list[0].title = getString(R.string.nav_param1)
//            list[1].title = getString(R.string.nav_param2)
//            list[2].title = getString(R.string.nav_param3)
//
//            XpopUtils(this).showConfigParams3DDialog(
//                onConfirmCall = {
//                    val dParams = DoubleArray(list.size)
//                    for (i in dParams.indices) {
//                        dParams[i] = list[i].value
//                    }
//                    MainController.send3DEditConfig(dParams)
//                },
//                list
//            )
        }

        //接收修改配置参数结果
        LiveEventBus.get<Int>(KEY_CONFIGURATION_PARAMETERS_RESULT).observe(this) {
            when (it) {
                0 -> {
                    ToastUtils.showLong("配置成功")
                }

                1 -> {
                    ToastUtils.showLong("配置失败")
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun navHeartbeatState(it: ByteArray) {
//        LogUtil.i("navHeartbeatState [0] = ${it[0].toInt()} ,navHeartbeatState [1] = ${it[1].toInt()} ")

        when (it[0].toInt()) {
            //定位
            1 -> {
                if (mBinding.mapView.isMapping) {
                    LogUtil.i(
                        "3D 此时导航从其他模式切换到定位，说明导航已经建图、优化、保存完成",
                        null,
                        TAG_NAV
                    )

                    mViewModel.downPngYaml(CREATE_MAP, mapID)

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
//                        mBinding.tvCreate.gone()
//                        mBinding.tvStop.visible()
                    }
                }
            }
            //结束建图
            3 -> {}
            //开始录制dx
            4 -> {
                LogUtil.d("录制DX ing", null, TAG_NAV)
            }

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

                    showSavaMapDialog()

                    mBinding.mapView.isStartRevSubMaps = false

                }
            }
            //正在保存地图
            3 -> {}
            //正在取消保存地图
            4 -> {}
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
                LogUtil.i("mBinding.mapView.mMapRotate ${mBinding.mapView.rotationRadians}")
                //开始保存地图 mBinding.mapView.rotationRadians就是弧度
                MainController.saveEnvironment(
                    1, rotate = mBinding.mapView.rotationRadians, mapId = mapID
                )
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