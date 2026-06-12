package com.siasun.dianshi.mapviewdemo.ui.createMap.createMap3D

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.jxd.jxd_core.intent.startActivity
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.base.BaseMvvmActivity
import com.siasun.dianshi.bean.ConstraintNode
import com.siasun.dianshi.bean.SwitchMapBean
import com.siasun.dianshi.bean.UpdateMapBean
import com.siasun.dianshi.controller.MainController
import com.siasun.dianshi.dialog.CommonEditDialog
import com.siasun.dianshi.dialog.CommonWarnDialog
import com.siasun.dianshi.framework.ext.onClick
import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.mapviewdemo.BuildConfig
import com.siasun.dianshi.mapviewdemo.CREATE_MAP
import com.siasun.dianshi.mapviewdemo.KEY_CONFIGURATION_PARAMETERS
import com.siasun.dianshi.mapviewdemo.KEY_CONFIGURATION_PARAMETERS_RESULT
import com.siasun.dianshi.mapviewdemo.KEY_CONSTRAINT_CONSTRAINT_NODE_RESULT
import com.siasun.dianshi.mapviewdemo.KEY_CONSTRAINT_NODE
import com.siasun.dianshi.mapviewdemo.KEY_LOCATION
import com.siasun.dianshi.mapviewdemo.KEY_NAV_HEARTBEAT_STATE
import com.siasun.dianshi.mapviewdemo.KEY_OPT_POSE
import com.siasun.dianshi.mapviewdemo.KEY_UPDATE_MAP
import com.siasun.dianshi.mapviewdemo.KEY_UPDATE_POS
import com.siasun.dianshi.mapviewdemo.TAG_NAV
import com.siasun.dianshi.mapviewdemo.databinding.ActivityCreateMap3dDactivityBinding
import com.siasun.dianshi.mapviewdemo.viewmodel.CreateMap3DViewModel
import com.siasun.dianshi.network.constant.KEY_NEY_IP
import com.siasun.dianshi.utils.RadianUtil
import com.siasun.dianshi.view.WorkMode
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import kotlin.math.cos
import kotlin.math.sin

/**
 * 创建3D地图（悬浮窗版本）
 */
class CreateMap3DActivity :
    BaseMvvmActivity<ActivityCreateMap3dDactivityBinding, CreateMap3DViewModel>() {

    // 建图心跳定时器
    private val mTimer = Timer()
    val mapID = 11
    var boolean = false

    // 悬浮窗管理器
    private lateinit var floatingWindow: FloatingControlWindow

    // 事件 key 定义（用于悬浮窗与 Activity 通信）
    companion object {
        const val EVENT_FLOATING_BTN_TEST = "event_floating_btn_test"
        const val EVENT_FLOATING_BTN_SHOW_FRAME = "event_floating_btn_show_frame"
        const val EVENT_FLOATING_BTN_RESET_ROTATION = "event_floating_btn_reset_rotation"
        const val EVENT_FLOATING_BTN_ADD_NODE = "event_floating_btn_add_node"
        const val EVENT_FLOATING_BTN_MATCH_NODE = "event_floating_btn_match_node"
        const val EVENT_FLOATING_BTN_EDIT_CONFIG = "event_floating_btn_edit_config"
        const val EVENT_FLOATING_BTN_SAVE = "event_floating_btn_save"
        const val EVENT_FLOATING_BTN_STOP = "event_floating_btn_stop"
        const val EVENT_FLOATING_BTN_CREATE = "event_floating_btn_create"
        const val EVENT_FLOATING_BTN_EXPAND = "event_floating_btn_expand"
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun initView(savedInstanceState: Bundle?) {
        MainController.init()
        MMKV.defaultMMKV().encode(KEY_NEY_IP, "192.168.1.198")

        mBinding.mapView.setWorkMode(WorkMode.MODE_CREATE_MAP)

        mTimer.schedule(object : TimerTask() {
            override fun run() {
                MainController.myController.mSendNaviHeartBeat()
            }
        }, 0, 500)

        // 隐藏原有的按钮面板
        mBinding.llRotate.visibility = android.view.View.GONE

        // 初始化悬浮窗（会在 onStart 中显示）
        floatingWindow = FloatingControlWindow(this)

        // 订阅悬浮窗发送的事件
        subscribeFloatingEvents()
    }

    /**
     * 订阅悬浮窗按钮事件，执行原有逻辑
     */
    private fun subscribeFloatingEvents() {
        LiveEventBus.get(EVENT_FLOATING_BTN_TEST, Boolean::class.java).observe(this) {
            ToastUtils.showShort("测试按钮")
            Log.e("YZS", "测试按钮")
//            showInputDialog(this@CreateMap3DActivity) { userInput ->
//                // 这里可以处理用户输入，例如 Toast 提示或保存数据
////                Toast.makeText(this, "你输入了: $userInput", Toast.LENGTH_SHORT).show()
//                Log.e("YZS", "测试按钮\$userInput\"")
//            }
            showInputDialog { userInput ->
                Log.e("YZS", "测试按钮$userInput")
            }
        }

        LiveEventBus.get(EVENT_FLOATING_BTN_SHOW_FRAME, Boolean::class.java).observe(this) {
            boolean = !boolean
            mBinding.mapView.showKeyFrames(boolean)
        }

        LiveEventBus.get(EVENT_FLOATING_BTN_RESET_ROTATION, Boolean::class.java).observe(this) {
            LogUtil.e("恢复前 旋转的弧度 旋转了 ${mBinding.mapView.getViewRotation()} 弧度")
            LogUtil.i("恢复前 旋转的角度 旋转了 ${RadianUtil.toAngel(mBinding.mapView.getViewRotation())} 角度")
            LogUtil.i("----------------")
            mBinding.mapView.resetRotation()
            LogUtil.e("恢复后 旋转的弧度 旋转了 ${mBinding.mapView.getViewRotation()} 弧度")
            LogUtil.i("恢复后 旋转的角度 旋转了 ${RadianUtil.toAngel(mBinding.mapView.getViewRotation())} 角度")
        }

        LiveEventBus.get(EVENT_FLOATING_BTN_ADD_NODE, Boolean::class.java).observe(this) {
            MainController.send3DConstraintNode()
        }

        LiveEventBus.get(EVENT_FLOATING_BTN_MATCH_NODE, Boolean::class.java).observe(this) {
            CommonEditDialog.Builder(this).setOnCommonEditDialogListener(object :
                CommonEditDialog.Builder.CommonEditDialogListener {
                override fun confirm(str: String) {
                    MainController.send3DMatchingNode(str.toInt())
                }
            }).setTitle("请输入约束节点ID").create().show()
        }

        LiveEventBus.get(EVENT_FLOATING_BTN_EDIT_CONFIG, Boolean::class.java).observe(this) {
            MainController.send3DReadConfig()
        }

        LiveEventBus.get(EVENT_FLOATING_BTN_SAVE, Boolean::class.java).observe(this) {
            showSavaMapDialog()
        }

        LiveEventBus.get(EVENT_FLOATING_BTN_STOP, Boolean::class.java).observe(this) {
            MainController.stopCreateEnvironment()
            LogUtil.i("停止扫描")
            ToastUtils.showShort("停止扫描")
        }

        LiveEventBus.get(EVENT_FLOATING_BTN_CREATE, Boolean::class.java).observe(this) {
            mBinding.mapView.isStartRevSubMaps = false
            mBinding.mapView.setWorkMode(WorkMode.MODE_CREATE_MAP)
            MainController.startCreateEnvironment()
            showLoading("开始扫描")
            ToastUtils.showShort("开始扫描")
            LogUtil.i("开始扫描", null, TAG_NAV)
        }

        LiveEventBus.get(EVENT_FLOATING_BTN_EXPAND, Boolean::class.java).observe(this) {
            startActivity<ExpandMap3DActivity>()
        }
    }

    override fun onStart() {
        super.onStart()
        // 显示悬浮窗
        window.decorView.post {
            floatingWindow.show()
        }
    }

    override fun onStop() {
        super.onStop()
        // 隐藏悬浮窗

        floatingWindow.hide()
    }

    override fun onDestroy() {
        floatingWindow.destroy()
        mockJob?.cancel()
        mockJob = null
        super.onDestroy()
    }

    // ---------------- 以下为原有代码，未作改动（只保留必要部分） ----------------
    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun initData() {
        super.initData()
        if (BuildConfig.DEBUG) {
            mBinding.mapView.setWorkMode(WorkMode.MODE_CREATE_MAP)
            startMockPosStream()
        }
        // 下载地图结果
        LiveEventBus.get(KEY_UPDATE_MAP, UpdateMapBean::class.java).observe(this) {
            updateMap(it)
        }
        // 建图导航心跳
        LiveEventBus.get(KEY_NAV_HEARTBEAT_STATE, ByteArray::class.java).observe(this) {
            navHeartbeatState(it)
        }
        // 接收创建地图中车体位置
        LiveEventBus.get(KEY_UPDATE_POS, laser_t::class.java).observe(this) {
            mBinding.mapView.parseLaserData(it, 2)
            if (it.rad0 > 0f) {
                mBinding.tvMapSteps.text = "步数:${it.rad0}"
            }
        }
        // NAV 做回环时候给的数据
        LiveEventBus.get(KEY_OPT_POSE, laser_t::class.java).observe(this) {
            mBinding.mapView.parseOptPose(it)
        }
        // 接收约束节点数据
        LiveEventBus.get<ConstraintNode>(KEY_CONSTRAINT_NODE).observe(this) {
            mBinding.mapView.addConstraintNodes(it)
        }
        // 接收约束节点匹配结果
        LiveEventBus.get<Int>(KEY_CONSTRAINT_CONSTRAINT_NODE_RESULT).observe(this) {
            when (it) {
                0 -> ToastUtils.showLong("匹配成功")
                1 -> ToastUtils.showLong("匹配失败")
            }
        }
        // 接收配置参数
        LiveEventBus.get<DoubleArray>(KEY_CONFIGURATION_PARAMETERS).observe(this) {
            LogUtil.w("接收配置参数 ${it}", null, TAG_NAV)
            // 原有配置弹窗逻辑（已注释，如需启用请放开）
        }
        // 接收修改配置参数结果
        LiveEventBus.get<Int>(KEY_CONFIGURATION_PARAMETERS_RESULT).observe(this) {
            when (it) {
                0 -> ToastUtils.showLong("配置成功")
                1 -> ToastUtils.showLong("配置失败")
            }
        }
        // 接收定位信息
        LiveEventBus.get<Int>(KEY_LOCATION).observeSticky(this) {
            dismissLoading()
            if (it == 1) {
                LogUtil.e("接收定位信息s ${it}")
                mBinding.tvLocation.text = "定位成功"
            } else {
                mBinding.tvLocation.text = "定位失败"
                LogUtil.e("接收定位信息e ${it}")
            }
        }
        mViewModel.uploadMapInfoLiveData.observe(this) {
            lifecycleScope.launch {
                delay(2000)
                sendLastPose()
            }
        }
    }

    /**
     * 显示带输入框和两个按钮的对话框
     * @param context 上下文（Activity 或 ApplicationContext）
     * @param onConfirm 确认回调，返回用户输入的文本（可选）
     */
    fun showInputDialog( onConfirm: ((String) -> Unit)? = null) {
        // 动态创建 EditText 并设置参数
        val editText = EditText(this).apply {
            hint = "请输入内容"
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // 将 EditText 放入容器（AlertDialog 支持直接设置 View）
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 32)  // 避免贴边
            addView(editText)
        }

        // 构建对话框
        AlertDialog.Builder(this)
            .setTitle("输入对话框")
            .setView(container)
            .setPositiveButton("确定") { dialog, _ ->
                val inputText = editText.text.toString()
                onConfirm?.invoke(inputText)
                dialog.dismiss()  // 关闭对话框
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()  // 关闭对话框
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun navHeartbeatState(it: ByteArray) {
        when (it[0].toInt()) {
            1 -> {
                if (mBinding.mapView.isMapping) {
                    LogUtil.i("3D 此时导航从其他模式切换到定位，说明导航已经建图、优化、保存完成", null, TAG_NAV)
                    mViewModel.downPngYaml(CREATE_MAP, mapID)
                    mBinding.mapView.isMapping = false
                }
            }
            2 -> {
                if (!mBinding.mapView.isMapping) {
                    mBinding.mapView.isMapping = true
                    dismissLoading()
                }
            }
            3 -> {}
            4 -> {
                LogUtil.d("录制DX ing", null, TAG_NAV)
            }
        }
        when (it[1].toInt()) {
            1 -> {
                LogUtil.i("地图正在优化中", null, TAG_NAV)
                ToastUtils.showShort("地图正在优化中")
            }
            2 -> {
                if (mBinding.mapView.isStartRevSubMaps) {
                    showSavaMapDialog()
                    mBinding.mapView.isStartRevSubMaps = false
                }
            }
            3 -> {}
            4 -> {}
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun showSavaMapDialog() {
        CommonWarnDialog.Builder(this).setMsg("保存地图").setOnCommonWarnDialogListener(object :
            CommonWarnDialog.Builder.CommonWarnDialogListener {
            override fun confirm() {
                if (mBinding.mapView.getViewRotation() == 0f) {
                    MainController.saveEnvironment(1, mapId = mapID)
                    LogUtil.e("未旋转地图 ${mBinding.mapView.getViewRotation()}")
                } else {
                    MainController.saveEnvironment(3, rotate = -mBinding.mapView.getViewRotation(), mapId = mapID)
                }
                showLoading("保存地图中")
                LogUtil.i("确定要保存地图么...点击确定", null, TAG_NAV)
            }
            override fun discard() {
                mBinding.mapView.isMapping = false
                MainController.saveEnvironment(2, mapId = mapID)
                LogUtil.i("确定要保存地图么...点击取消", null, TAG_NAV)
                finish()
            }
        }).create().show()
    }

    private fun updateMap(it: UpdateMapBean) {
        dismissLoading()
        if (it.isSuccess && it.type == CREATE_MAP) {
            mViewModel.saveMapToService(mapID, "200", "2")
        }
    }

    private fun sendLastPose() {
        if (mBinding.mapView.getViewRotation() == 0f) {
            mViewModel.switchMapInfo(
                SwitchMapBean(
                    mapID,
                    mBinding.mapView.robotPose[0].toDouble(),
                    mBinding.mapView.robotPose[1].toDouble(),
                    mBinding.mapView.robotPose[2].toDouble(),
                    mBinding.mapView.robotPose[3].toDouble(),
                    mBinding.mapView.robotPose[4].toDouble(),
                    mBinding.mapView.robotPose[5].toDouble(),
                    10,
                )
            )
            LogUtil.e("地图无旋转", null, TAG_NAV)
        } else {
            val calculate = calculate(mBinding.mapView.robotPose, -mBinding.mapView.getViewRotation())
            mViewModel.switchMapInfo(
                SwitchMapBean(
                    mapID,
                    calculate[0].toDouble(),
                    calculate[1].toDouble(),
                    calculate[2].toDouble(),
                    calculate[3].toDouble(),
                    calculate[4].toDouble(),
                    calculate[5].toDouble(),
                    10,
                )
            )
            LogUtil.e("地图有旋转", null, TAG_NAV)
        }
    }

    fun calculate(array: FloatArray, rotationRadians: Float): FloatArray {
        val arr = FloatArray(6)
        val s = sin(rotationRadians)
        val c = cos(rotationRadians)
        val x: Float = array[0]
        val y: Float = array[1]
        arr[0] = c * x - s * y
        arr[1] = s * x + c * y
        val yaw = array[2]
        arr[2] = yaw + rotationRadians
        arr[3] = array[3]
        arr[4] = array[4]
        arr[5] = array[5]
        return arr
    }

    private var mockJob: Job? = null
//    private fun startMockPosStream() {
//        if (mockJob != null) return
//        mockJob = lifecycleScope.launch {
//            val targetKeyframes = 100000
//            var step = 0f
//            var angle = 0f
//            while (step < targetKeyframes) {
//                val lt = laser_t()
//                val numPoints = 360
//                val ranges = FloatArray(6 + numPoints * 6)
//                val startRadius = 0f
//                val radiusGrowth = 0.001f
//                val carRadius = startRadius + radiusGrowth * step
//                val x = cos(angle) * carRadius
//                val y = sin(angle) * carRadius
//                val theta = angle
//                ranges[0] = x
//                ranges[1] = y
//                ranges[2] = theta
//                ranges[3] = 0f
//                ranges[4] = 0f
//                ranges[5] = 0f
//                var idx = 6
//                val r = 2f
//                for (i in 0 until numPoints) {
//                    val a = i * (2f * Math.PI.toFloat() / numPoints)
//                    val px = cos(a) * r
//                    val py = sin(a) * r
//                    ranges[idx] = px
//                    ranges[idx + 1] = py
//                    ranges[idx + 2] = 0f
//                    ranges[idx + 3] = 0f
//                    ranges[idx + 4] = 0f
//                    ranges[idx + 5] = 0f
//                    idx += 6
//                }
//                lt.ranges = ranges
//                lt.intensities = floatArrayOf(1000f, 1000f, 0f, 0f, 0.05f)
//                lt.rad0 = step
//                LiveEventBus.get(KEY_UPDATE_POS, laser_t::class.java).post(lt)
//                step += 1f
//                angle += 0.05f
//                delay(50)
//            }
//        }
//    }



    private fun startMockPosStream() {
        if (mockJob != null) return
        mockJob = lifecycleScope.launch {
            val targetKeyframes = 3000            // 总关键帧数，避免轨迹太大
            var step = 0f
            var angle = 0f
            while (step < targetKeyframes) {
                val lt = laser_t()
                val numPoints = 360
                val ranges = FloatArray(6 + numPoints * 6)

                // 机器人轨迹：阿基米德螺旋，半径随 step 线性增长
                val radiusGrowth = 0.005f          // 每帧半径增加 5mm（原 1mm）
                val carRadius = radiusGrowth * step
                val x = cos(angle) * carRadius
                val y = sin(angle) * carRadius
                val theta = angle                 // 朝向始终沿切线方向

                ranges[0] = x
                ranges[1] = y
                ranges[2] = theta
                ranges[3] = 0f
                ranges[4] = 0f
                ranges[5] = 0f

                // 生成圆形激光点云（半径 2m）
                var idx = 6
                val r = 2f
                for (i in 0 until numPoints) {
                    val a = i * (2f * Math.PI.toFloat() / numPoints)
                    ranges[idx] = cos(a) * r
                    ranges[idx + 1] = sin(a) * r
                    ranges[idx + 2] = 0f
                    ranges[idx + 3] = 0f
                    ranges[idx + 4] = 0f
                    ranges[idx + 5] = 0f
                    idx += 6
                }

                lt.ranges = ranges
                lt.intensities = floatArrayOf(1000f, 1000f, 0f, 0f, 0.05f)
                lt.rad0 = step

                LiveEventBus.get(KEY_UPDATE_POS, laser_t::class.java).post(lt)

                step += 1f
                angle += 0.2f   // 每帧旋转 0.2 弧度（约 11.5°，原 0.05）
                if (step>1000){
                    delay(500)
                }else{
                    delay(30)       // 50ms 发送一次
                }
            }
        }
    }


//    private fun startMockPosStream() {
//        if (mockJob != null) return
//        mockJob = lifecycleScope.launch {
//            val totalKeyframes = 20
//            val spacing = 1f                // 关键帧间距（激光半径 2m 的 10 倍）
//            for (step in 0 until totalKeyframes) {
//                val lt = laser_t()
//                val numPoints = 360
//                val ranges = FloatArray(6 + numPoints * 6)
//
//                // 机器人沿 X 轴直线运动，朝向始终为 0（面向 X 正方向）
//                val x = step * spacing
//                val y = 0f
//                val theta = 0f
//
//                ranges[0] = x
//                ranges[1] = y
//                ranges[2] = theta
//                ranges[3] = 0f
//                ranges[4] = 0f
//                ranges[5] = 0f
//
//                // 生成半径为 2m 的圆形激光点云
//                var idx = 6
//                val r = 2f
//                for (i in 0 until numPoints) {
//                    val a = i * (2f * Math.PI.toFloat() / numPoints)
//                    ranges[idx] = cos(a) * r
//                    ranges[idx + 1] = sin(a) * r
//                    ranges[idx + 2] = 0f
//                    ranges[idx + 3] = 0f
//                    ranges[idx + 4] = 0f
//                    ranges[idx + 5] = 0f
//                    idx += 6
//                }
//
//                lt.ranges = ranges
//                lt.intensities = floatArrayOf(1000f, 1000f, 0f, 0f, 0.05f)
//                lt.rad0 = step.toFloat()
//
//                LiveEventBus.get(KEY_UPDATE_POS, laser_t::class.java).post(lt)
//                delay(500)   // 50ms 一帧
//            }
//        }
//    }

}