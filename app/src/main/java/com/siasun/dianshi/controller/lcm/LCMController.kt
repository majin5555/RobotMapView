package com.siasun.dianshi.controller.lcm

import android.annotation.SuppressLint
import android.graphics.PointF
import android.os.Build
import androidx.annotation.RequiresApi
import com.jeremyliao.liveeventbus.LiveEventBus
import com.ngu.lcmtypes.laser_t
import com.ngu.lcmtypes.robot_control_t
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.AVOIDDX_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.BOTTOM_POINTCLOUD
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.CLEAN_SERVICE_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.CLEAN_UI_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.CMS_CTRL_RESPONSE_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.CMS_SERVICE_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.CMS_UI_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.CTRL_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.CURRENT_POINTCLOUD
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.LOCINFO_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.LP_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.LP_CTRL_RESPONSE_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.NAVI_SERVICE_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.NAVI_UI_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.OPT_POSE
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.PERCEPTION_ASK_CAMFIRMWARE_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.PERCEPTION_ASK_CAMHUB_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.PERCEPTION_CALIBRATE_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.PERCEPTION_CALIBRESULT_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.PERCEPTION_CAMFIRMWARE_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.PERCEPTION_CAMHUB_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.PERCEPTION_PUBLISH_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.PERCEPTION_SERVICE_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.PLAN_PATH_RESULT
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.PP_CTRL_RESPONSE_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.RECORD_IMAGE
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.SERVICE_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.SERVICE_CONTROL_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.SERVICE_CONTROL_UI_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.SUBSCRIBE_CHANNEL
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.TEACH_PATH
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.UI_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.UPDATE_POS
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.UPDATE_SUBMAPS
import com.siasun.dianshi.bean.CmsPadInteraction_
import com.siasun.dianshi.bean.PlanPathResult
import com.siasun.dianshi.bean.perception_t
import com.siasun.dianshi.bean.robot_control_t_new
import com.siasun.dianshi.controller.AbsController
import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.mapviewdemo.CarBody
import com.siasun.dianshi.mapviewdemo.CmsBody
import com.siasun.dianshi.mapviewdemo.KEY_BOTTOM_CURRENT_POINT_CLOUD
import com.siasun.dianshi.mapviewdemo.KEY_CLEANING_ID
import com.siasun.dianshi.mapviewdemo.KEY_CLEANING_LAYER
import com.siasun.dianshi.mapviewdemo.KEY_CROSS_FLOOR_STAGE
import com.siasun.dianshi.mapviewdemo.KEY_CURRENT_POINT_CLOUD
import com.siasun.dianshi.mapviewdemo.KEY_FINISH_CLEAN_AREA_ID
import com.siasun.dianshi.mapviewdemo.KEY_NEXT_CLEANING_AREA_ID
import com.siasun.dianshi.mapviewdemo.KEY_SCHEDULED_TASK_REMINDER
import com.siasun.dianshi.mapviewdemo.KEY_TASK_STATE
import com.siasun.dianshi.mapviewdemo.NaviBody
import com.siasun.dianshi.mapviewdemo.RunningState.CURRENT_TASK_STATE
import com.siasun.dianshi.mapviewdemo.SERVER_HEART
import com.siasun.dianshi.mapviewdemo.ServiceBody
import com.siasun.dianshi.mapviewdemo.TaskState
import lcm.lcm.LCM
import lcm.lcm.LCMDataInputStream
import lcm.lcm.LCMSubscriber
import java.util.Timer
import kotlin.concurrent.schedule


/**
 * Updated by XiaoMingliang
 * @date 2023/06/08
 * This class was merged both sender and receiver
 */
class LCMController : AbsController(), LCMSubscriber {
    /***********************控制台迭代结束 */

    private val mULCMHelper: ULCMHelper = ULCMHelper()
    override fun init() {
        mULCMHelper.initLCM(SUBSCRIBE_CHANNEL, this)
        Timer().schedule(0, 60000) {
            destroy()
            mULCMHelper.initLCM(SUBSCRIBE_CHANNEL, this@LCMController)
        }
    }

    override fun destroy() {
        mULCMHelper.unsubscribe()
    }


    /**
     * Message received from LCM
     * majin 接收返回信息
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun messageReceived(
        lcm: LCM, channel: String, lcmDataInputStream: LCMDataInputStream
    ) {
        try {
            when (channel) {
                //车体->PAD 接收示教信息
                TEACH_PATH -> receiveLcmTeachPoint(robot_control_t(lcmDataInputStream))
                //车体->PAD
                CLEAN_UI_COMMAND -> {
                    val rt = robot_control_t(lcmDataInputStream)
                    when (rt.commandid.toInt()) {
                        CarBody.CLEAN_UI_COMMAND_SENSOR_INFORMATION.value -> {
                            sensorInformation(rt.iparams)
                            receiveAgvSpeed(rt.dparams)
                        }

                        CarBody.CLEAN_UI_COMMAND_CONSUMABLES.value -> receiveConsumables(rt)
                        CarBody.FAILURE_BACK_STATION.value -> receiveFailureBackStation(rt)
                        CarBody.INTEGRATED_MACHINE_INTERACTION.value -> receiveIntegratedMachineInteraction(
                            rt
                        )

                        CarBody.CHARGE_CHANGE_WATER_PAGE_STATE.value -> receiveChargeChangeWaterPageState(
                            rt
                        )

                        CarBody.OCCUPYING_EQUIPMENT.value -> receiveOccupyingEquipmentState(rt)


                    }
                }
                //车体->PAD
                UI_COMMAND -> {
                    val rt = robot_control_t(lcmDataInputStream)
                    when (rt.commandid.toInt()) {
                        CarBody.UI_COMMAND_ROBOT_STATE.value -> recRobotState(rt)
                        CarBody.UI_COMMAND_ROBOT_AGV_EVENT.value -> recAgvEvent(rt)
                        CarBody.UI_COMMAND_ROBOT_AGV_VERSION.value -> sendAgvVersion(rt)
                        CarBody.UI_COMMAND_ROBOT_MAG_SWITCH_STATE.value -> receiveSettingViewState(
                            rt
                        )

                        CarBody.UI_COMMAND_ROBOT_AGV_SHUTDOWN.value -> receiveAgvShutdown(1)
                        CarBody.UI_COMMAND_ROBOT_AGV_BATTERY_ERROR.value -> receiveAgvBatteryError()
                        CarBody.UI_COMMAND_PLAY_MUSIC.value -> receivePlayMusic(rt)
                        CarBody.UI_COMMAND_CROSS_FLOOR_STAGE.value -> receiveCrossFloorStage(rt)


                    }
                }
                //CMS控制器->PAD
                CMS_UI_COMMAND -> {
                    val mCmsPt = CmsPadInteraction_(lcmDataInputStream)
                    when (mCmsPt.commandid.toInt()) {
                        CmsBody.CMS_UI_COMMAND_CMS_VERSION.value -> receiveCmsVersion(mCmsPt)
                        CmsBody.CMS_UI_COMMAND_TASK_STATE.value -> receiveCmsTaskState(mCmsPt)
                        CmsBody.CMS_UI_COMMAND_AREA_STATE.value -> receiveCmsAreaState(mCmsPt)
                    }
                }
                //导航->pas 自动建图时向pad发送子图
                UPDATE_SUBMAPS -> receiveSubMap(laser_t(lcmDataInputStream))
                //导航->pad 车体位置
                UPDATE_POS -> receiveRobotPos(laser_t(lcmDataInputStream))
                //导航->pad 子图优化
                OPT_POSE -> receiveOptSubMap(laser_t(lcmDataInputStream))
                //导航->pad  导航心跳
                NAVI_UI_COMMAND -> {
                    val rtNew = robot_control_t_new(lcmDataInputStream)
                    when (rtNew.commandid.toInt()) {
                        NaviBody.NAVI_UI_COMMAND_NAVIGATION_HEARTBEAT.value -> receiveNaviHeartbeatState(
                            rtNew
                        )

                        NaviBody.NAVI_UI_COMMAND_REMOVE_NOISE_RESULT.value -> recRemoveNoiseResult(
                            rtNew
                        )

                        NaviBody.NAVI_UI_COMMAND_NAV_VERSION.value -> receiveNAVVersion(rtNew)
                        NaviBody.NAVI_UI_COMMAND_CALIBRATION_DATA.value -> receiveCalibrationData(
                            rtNew
                        )

                        NaviBody.NAVI_UI_COMMAND_CALIBRATION_RESULT.value -> receiveCalibrationResult(
                            rtNew
                        )

                        NaviBody.NAVI_UI_COMMAND_LOAD_EXTENDED_MAP_DATA_RESULT.value -> {
                            sendReplayNavi(63)
                            receiveLoadExtendedMapDataResult(rtNew)
                        }

                        NaviBody.NAVI_UI_COMMAND_LOAD_TOP_SCAN_STATE.value -> receiveLoadTopScan(
                            rtNew
                        )

                        NaviBody.NAVI_UI_COMMAND_LOAD_SCAN_STATE.value -> receiveLoadScanState(rtNew)
                        NaviBody.NAVI_UI_COMMAND_FINISH_WRITE_SLAM.value -> receiveFinishWriteSlam()
                        NaviBody.NAVI_UI_COMMAND_POSITING_AREA.value -> receivePositingArea(rtNew)
                    }
                }
                //导航->pad 接受点云数据
                CURRENT_POINTCLOUD -> receiveCurrentPointCloud(laser_t(lcmDataInputStream))
                //导航->pad 下激光点云
                BOTTOM_POINTCLOUD -> receiveBottomCurrentPointCloud(laser_t(lcmDataInputStream))
                //导航->pad  顶视建图步数
                RECORD_IMAGE -> receiveTopScanSteps(robot_control_t_new(lcmDataInputStream))
                //路径规划器->pad
                PLAN_PATH_RESULT -> {
                    val result = PlanPathResult(
                        lcmDataInputStream
                    )
                    receivePlanPathResult(result)
                    receivePPVersion(result)
                }
                //热更新PP
                PP_CTRL_RESPONSE_COMMAND -> receivePPReloadFileResult(
                    robot_control_t(
                        lcmDataInputStream
                    )
                )
                //热更新LP结果
                LP_CTRL_RESPONSE_COMMAND -> receiveLPReloadFileResult(
                    robot_control_t(
                        lcmDataInputStream
                    )
                )
                //热更新CMS结果
                CMS_CTRL_RESPONSE_COMMAND -> receiveCMSReloadFileResult(
                    robot_control_t(
                        lcmDataInputStream
                    )
                )
                //中间服务->pad
                SERVICE_CONTROL_UI_COMMAND -> {
                    val rt = robot_control_t(lcmDataInputStream)
                    when (rt.commandid.toInt()) {
                        ServiceBody.SERVICE_CONTROL_UI_COMMAND_RECEIVE_VERSION.value -> receiveServerVersion(
                            rt
                        )

                        ServiceBody.SERVICE_CONTROL_UI_COMMAND_RECEIVE_SERVER_INFO.value -> receiveServerMrcRam(
                            rt.sparams[0]
                        )

                        ServiceBody.SERVICE_CONTROL_UI_COMMAND_RECEIVE_SCHEDULED_TASK.value -> receiveScheduledTask(
                            rt.sparams
                        )

                        ServiceBody.SERVICE_CONTROL_UI_COMMAND_RECEIVE_SCHEDULED_TASK_REMINDER.value -> receiveScheduledTaskReminder()
                        ServiceBody.SERVICE_CONTROL_UI_COMMAND_RECEIVE_OTA_UPDATE.value -> receiveOtaUpdate(
                            rt.bparams
                        )

                        ServiceBody.SERVICE_CONTROL_UI_COMMAND_RECEIVE_SYNC_PAD_JOBS.value -> receiveSyncPadJobs()
                        ServiceBody.SERVICE_CONTROL_UI_COMMAND_RECEIVE_LOG_IMAGE.value -> receiveSyncPadLog(
                            rt
                        )

                        ServiceBody.SERVICE_CONTROL_UI_COMMAND_RECEIVE_SWITCH_MAP.value -> receiveSwitchMap(
                            rt
                        )
                    }
                }
                //接收避障版本号
                LP_COMMAND -> receiveLPVersion(robot_control_t(lcmDataInputStream).bparams)
                //感知版本
                PERCEPTION_PUBLISH_COMMAND -> recPETVersion(perception_t(lcmDataInputStream))
                //获取相机标定结果
                PERCEPTION_CALIBRESULT_COMMAND -> recCameraCalibrationResult(
                    perception_t(
                        lcmDataInputStream
                    )
                )
                //NAV->PAD 发送定位状态信息
                LOCINFO_COMMAND -> recNavLocationInfo(robot_control_t_new(lcmDataInputStream))
                //否升级相机固件版本
                PERCEPTION_CAMFIRMWARE_COMMAND -> recPETCamFirmware(perception_t(lcmDataInputStream))
                //接收感知相机排布是否合理
                PERCEPTION_CAMHUB_COMMAND -> recCameraUSBReasonable(perception_t(lcmDataInputStream))

            }
        } catch (e: java.lang.Exception) {
            LogUtil.i("LCM 接收异常 ${e.stackTraceToString()}")
        }
    }


    /**
     * pad—>所有
     */
    override fun mSendUpload(file: String) = sendUpload(file)

    /**
     * pad—>发送更新文件指令
     */
    private fun sendUpload(file: String) =
        sendAllParams(3, null, null, null, file.toByteArray(), CTRL_COMMAND)

    /**
     * pad—>PP
     * 申请路径规划
     *
     */
//    override fun mSendRoutePathCommand(
//        mIPathPlanType: Int, mCleanArea: CleanAreaNew
//    ) = sendRoutePathCommand(mIPathPlanType, mCleanArea)


    /*** 申请路径规划 CleanArea */
//    private fun sendRoutePathCommand(mIPathPlanType: Int, mCleanArea: CleanAreaNew) {
//
//        val mPstStart = if (mCleanArea.areaStartPoint == null) {
//            floatArrayOf(0f, 0f)
//        } else {
//            floatArrayOf(mCleanArea.areaStartPoint.x, mCleanArea.areaStartPoint.y)
//        }
//
//        mULCMHelper.sendRoutePathCommand(
//            mIPathPlanType,
//            mPstStart,
//            mCleanArea.m_VertexPnt,
//            mCleanArea.layer_id,
//            mCleanArea.regId,
//            mCleanArea.cleanShape
//        )
//        mULCMHelper.sendLcmMsg(PLAN_PATH_CONTROL_COMMAND)
//    }


    /**********************************************************************************************/
    /***********************************************导航协议开始************************************/
    /**********************************************************************************************/


    /**
     * pad->导航
     * 设置导航工作模式——开始建图 5
     */
    override fun mMapStartCreate() = sendCreateMap(5)

    /**
     * pad->导航
     * 设置导航工作模式——结束建图 6
     */
    override fun mMapStopCreate() = sendCreateMap(6)

    /**
     * pad->导航
     * pad删除多区域（去除噪点）
     *
     */
    override fun mSendEraseEvPoint(start: PointF, end: PointF, mapId: Int) =
        sendEraseEvPoint(start, end, mapId = mapId)

    /**
     * pad->导航
     * 建立导航心跳
     *
     */
    override fun mSendNaviHeartBeat() = sendToNaviHeartBeat()

    /**
     * pad->导航
     * 录制dx文件
     *
     * @param sta true 创建 false 结束创建
     */
    override fun mRecordDX(sta: Boolean) = sendRecordDx(if (sta) 1 else 2)

    /**
     * pad->导航
     * 导航结束建图后，是否保存地图 （1：保存地图；2：不保存地图 3 旋转并保存地图）
     *
     */
    override fun mSaveEnvironment(cmdId: Byte, rotate: Float) = sendSaveEv(cmdId, rotate)

    /**
     * pad->导航
     * pad请求地图恢复 （重置环境文件）
     *
     */
    override fun mSendResetEv() = sendResetEv()

    /**
     * pad->导航
     * 发送模版信息
     *
     */
//    override fun mSendTemplateLoc(templateRoot: TemplateRoot) = sendTemplateLoc(templateRoot)

    /**
     * pad->导航
     * pad应答导航发送标定结果
     *
     */
    override fun mAnswerCalibration() = answerCalibration()

    /**
     * pad->导航
     * pad发送标定结果写入配置文件命令
     *
     */
    override fun mWriteCalibration() = writeCalibration()

    /**
     * pad->导航
     * 发送扩展地图命令
     *
     */
    override fun mSendLoadSubMapForExtendMap() = sendLoadSubMapForExtendMap()

    /**
     * pad->导航
     * 发送扩展地图方式
     * extendType 0- 不冻结原环境模型，1 冻结原环境模型
     */
    override fun mSendExtendMap(extendType: Int) = sendExtendMap(extendType)

    /**
     * pad->导航
     * 发送子图命令
     *
     * 如果Niparams 为0 ，则导航给pad 发送所有子图
     * 如果Niparams 为n，则导航给pad 发送n个子图，子图id在iparams数组中
     */
    override fun mSendReLoadSubMapForExtendMap(ids: ByteArray) = sendReLoadSubMapForExtendMap(ids)

    /**
     * pad->导航 录制顶视地图
     * type 1 开始顶视扫图  2 停止顶视扫图
     */
    override fun mSendRecordTop(type: Byte) = sendRecordTop(type)

    /**
     * pad->导航 发送定位区域和定位方式信息
     */
//    override fun mSendPositingArea(mapID: Int, mList: MutableList<PositingArea>) =
//        sendPositingArea(mapID, mList)


    /**
     * pad->导航 请求定位列表数据
     */
    override fun mSendGetPositingArea(mapID: Int) = sendGetPositingArea(mapID)

    /**
     * pad->导航 开始局部更新
     */
//    override fun mSendStartPartialUpdate(mList: MutableList<PartialUpdateArea>, mapID: Int) =
//        sendStartPartialUpdate(mList, mapID)

    /**
     * pad->导航 强制上线
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun forceOnline(mapID: Int) {
        sendRobotControlNew(40, null, null, arrayOf("${mapID}"), null, NAVI_SERVICE_COMMAND)
    }

    /***=========================================================================================**/
    /***=========================================================================================**/
    /***=========================================================================================**/

    /**
     * 建图指令
     * cmdId  5创建  6结束创建地图
     *
     */
    @SuppressLint("NewApi")
    private fun sendCreateMap(cmdId: Byte) {
        sendRobotControlNew(cmdId, null, null, null, null, NAVI_SERVICE_COMMAND)
    }

    /*** 擦除多个噪点*/
    @SuppressLint("NewApi")
    private fun sendEraseEvPoint(start: PointF, end: PointF, count: Int = 1, mapId: Int) {
        val minX: Float = start.x.coerceAtMost(end.x)
        val minY: Float = start.y.coerceAtMost(end.y)
        val maxX: Float = start.x.coerceAtLeast(end.x)
        val maxY: Float = start.y.coerceAtLeast(end.y)

        val iParams = ByteArray(1)
        iParams[0] = count.toByte()
        val dParams = DoubleArray(4)
        dParams[0] = minX.toDouble()
        dParams[1] = maxY.toDouble()

        dParams[2] = maxX.toDouble()
        dParams[3] = minY.toDouble()

        LogUtil.i("pad发送数据 去除噪点 iParams[0]${iParams[0]}")
        LogUtil.i("pad发送数据 去除噪点 dParams[0]${dParams[0]} ")
        LogUtil.i("pad发送数据 去除噪点 dParams[1]${dParams[1]} ")
        LogUtil.i("pad发送数据 去除噪点 dParams[2]${dParams[2]} ")
        LogUtil.i("pad发送数据 去除噪点 dParams[3]${dParams[3]} ")
        LogUtil.i("pad发送数据 去除噪点 mapId${mapId} ")

        sendRobotControlNew(10, dParams, iParams, arrayOf("$mapId"), null, NAVI_SERVICE_COMMAND)
    }

    /***向导航发送心跳指令*/
    @SuppressLint("NewApi")
    private fun sendToNaviHeartBeat() {
        sendRobotControlNew(20, null, null, null, null, NAVI_SERVICE_COMMAND)
    }

    /**
     * 录制dx文件
     *
     * @param cmdId 创建地图的指令 1-开始  2-结束
     */
    @SuppressLint("NewApi")
    private fun sendRecordDx(cmdId: Byte) {
        val mParam = ByteArray(1)
        mParam[0] = cmdId
        sendRobotControlNew(21, null, mParam, null, null, NAVI_SERVICE_COMMAND)
    }

    /**
     * 保存环境文件
     *
     * @param cmdId  （1：保存地图；2：不保存地图 3 旋转并保存地图）
     */
    @SuppressLint("NewApi")
    private fun sendSaveEv(cmdId: Byte, rotate: Float = 0f) {
        val mParam = ByteArray(1)
        mParam[0] = cmdId
        val dParams = doubleArrayOf(rotate.toDouble())
        sendRobotControlNew(22, dParams, mParam, null, null, NAVI_SERVICE_COMMAND)
    }

    /**重置环境文件**/
    @SuppressLint("NewApi")
    private fun sendResetEv() {
        sendRobotControlNew(23, null, null, null, null, NAVI_SERVICE_COMMAND)
    }



    /*** pad应答导航发送标定结果*/
    @SuppressLint("NewApi")
    private fun answerCalibration() {
        sendRobotControlNew(29, null, null, null, null, NAVI_SERVICE_COMMAND)
    }

    /*** pad发送标定结果写入配置文件命令*/
    @SuppressLint("NewApi")
    private fun writeCalibration() {
        sendRobotControlNew(30, null, null, null, null, NAVI_SERVICE_COMMAND)
    }

    /**
     * 开始 环境扩展
     */
    @SuppressLint("NewApi")
    private fun sendLoadSubMapForExtendMap() {
        sendRobotControlNew(31, null, null, null, null, NAVI_SERVICE_COMMAND)
    }

    /**
     *  发送扩展地图方式
     */
    @SuppressLint("NewApi")
    private fun sendExtendMap(extendType: Int) {
        val iParams = byteArrayOf(extendType.toByte())
        sendRobotControlNew(32, null, iParams, null, null, NAVI_SERVICE_COMMAND)
    }

    /**
     * 重发子图
     * @param ids 子图id数组
     *            -1表示所有子图都重发
     */
    @SuppressLint("NewApi")
    private fun sendReLoadSubMapForExtendMap(ids: ByteArray) {
        sendRobotControlNew(33, null, ids, null, null, NAVI_SERVICE_COMMAND)
    }

    /**
     * pad->导航
     * pad发送 对导航发来的命令的应答
     * iparams[0]: 63   pad应答导航发送的Commandid =63，表示pad已经收到导航发的Commandid =63的命令
     */
    @SuppressLint("NewApi")
    private fun sendReplayNavi(commandId: Byte) = sendRobotControlNew(
        34, null, byteArrayOf(commandId), null, null, NAVI_SERVICE_COMMAND
    )

    /**
     * pad->导航 录制顶视地图
     * type 1 开始顶视扫图  2 停止顶视扫图
     */
    @SuppressLint("NewApi")
    private fun sendRecordTop(type: Byte) {
        LogUtil.i("sendRecordTop type ${type}")
        sendRobotControlNew(
            35, null, byteArrayOf(type), null, null, NAVI_SERVICE_COMMAND
        )
    }


    /**
     * pad->导航 发送定位区域和定位方式信息
     *
     */
    @SuppressLint("NewApi")
//    private fun sendPositingArea(mapID: Int, mList: MutableList<PositingArea>) {
//        val rt = robot_control_t_new()
//        rt.commandid = 36
//
//        if (mList.size > 0) {
//            rt.niparams = (1 + (mList.size * 5)).toByte()
//            val mIparams: ByteArray = ByteArray((1 + (mList.size * 5)))
//            mIparams[0] = (mList.size).toByte()
//
//            mList.forEachIndexed { index, list ->
//                mIparams[index * 5 + 1] = list.slamMode.toByte()
//                mIparams[index * 5 + 2] = list.longCorridorMode.toByte()
//                mIparams[index * 5 + 3] = list.topViewFusion.toByte()
//                mIparams[index * 5 + 4] = list.id.toByte()
//            }
//
//            val mDparams: DoubleArray = DoubleArray((4 * mList.size))
//            mList.forEachIndexed { index, list ->
//                mDparams[index * 4] = list.start.x.toDouble()
//                mDparams[index * 4 + 1] = list.start.y.toDouble()
//                mDparams[index * 4 + 2] = list.end.x.toDouble()
//                mDparams[index * 4 + 3] = list.end.y.toDouble()
//            }
//
//            rt.iparams = mIparams
//            rt.ndparams = (4 * mList.size)
//            rt.dparams = mDparams
//        }
//        rt.nsparams = 1
//        rt.sparams = arrayOf("$mapID")
//        LogUtil.i("定位区域保存数据 mapID $mapID data${mList}")
//        mULCMHelper.sendLcmMsg(NAVI_SERVICE_COMMAND, rt)
//    }

    /**
     * pad->导航 获取定位区域列表数据
     */
    private fun sendGetPositingArea(mapID: Int) {
        sendRobotControlNew(37, null, null, arrayOf("$mapID"), null, NAVI_SERVICE_COMMAND)
//        LogUtil.i("pad->导航 获取定位区域列表数据", null, TAG_NAV)
    }

    /**
     * pad->导航 开始地图更新
     */
    @SuppressLint("NewApi")
//    private fun sendStartPartialUpdate(mList: MutableList<PartialUpdateArea>, mapID: Int) {
//        val rt = robot_control_t_new()
//        rt.commandid = 38
//
//        if (mList.size > 0) {
//            rt.niparams = 1
//            val mIparams = ByteArray(mList.size)
//            mIparams[0] = (mList.size).toByte()
//
//            val mDparams = DoubleArray((4 * mList.size))
//            mList.forEachIndexed { index, list ->
//                mDparams[index * 4] = list.start.x.toDouble()
//                mDparams[index * 4 + 1] = list.start.y.toDouble()
//                mDparams[index * 4 + 2] = list.end.x.toDouble()
//                mDparams[index * 4 + 3] = list.end.y.toDouble()
//            }
//
//            rt.iparams = mIparams
//            rt.ndparams = (4 * mList.size)
//            rt.dparams = mDparams
//
////            rt.nsparams = 1
////            rt.sparams[0] = "$mapID"
//        }
//        if (mList.size == 0) LogUtil.i("pad->导航 发送开始扩展地图  ")
//        else LogUtil.i("pad->导航 发送开始局部更新")
//
//        mULCMHelper.sendLcmMsg(NAVI_SERVICE_COMMAND, rt)
//    }

    /**
     * 和导航通信
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendRobotControlNew(
        commandId: Byte,
        dParams: DoubleArray?,
        iParams: ByteArray?,
        sParams: Array<String?>?,
        bParams: ByteArray?,
        channelName: String
    ): Boolean = mULCMHelper.sendAllParams(
        commandId, dParams, iParams, sParams, bParams, channelName
    )

    /**********************************************************************************************/
    /***********************************************导航协议结束************************************/
    /**********************************************************************************************/


    /**********************************************************************************************/
    /***********************************************CMS 控制器 协议开始*******************************/
    /**********************************************************************************************/

    /**
     *（Pad—>CMS）
     * 开始清扫
     *
     */
    override fun mSendTaskSubmit(str: String) = sendTaskSubmit(str)

    /**
     * Pad手动充电（Pad—>CMS）
     * 清除所有任务
     */
    override fun mSendControlTask() = sendControlTask()

    /**
     * Pad手动充电（Pad—>CMS）
     * 自动模式下，发送充电任务
     * */
    override fun mSendManualAutoCharge() = sendManualAutoCharge()

    /**
     *（Pad—>CMS）
     * 继续执行任务
     *
     */
    override fun mSendContinue() = sendContinue()
    /***=========================================================================================**/
    /***=========================================================================================**/
    /***=========================================================================================**/


    /**
     * Pad任务下发（Pad—>CMS）
     *
     * 手持pad使用，使用json的形式向RC发送任务指令
     *
     */
    private fun sendTaskSubmit(str: String) {
        sendAllCmsPad(1, null, null, null, str.toByteArray(), CMS_SERVICE_COMMAND)
    }

    /**
     * 任务控制指令
     * pad任务清除（Pad—>CMS）
     */
    private fun sendControlTask() {
        val iParams = IntArray(1)
        sendAllCmsPad(2, null, iParams, null, null, CMS_SERVICE_COMMAND)
    }


    /**
     * Pad手动充电（Pad—>CMS）
     * 自动模式下，发送充电任务
     * */
    private fun sendManualAutoCharge() {
        sendAllCmsPad(
            3, null, null, null, null, CMS_SERVICE_COMMAND
        )
    }

    /**
     * 重载CMS特殊点文件
     */
    private fun sendReloadSpecialFile() {
        sendAllCmsPad(5, null, null, null, null, CMS_SERVICE_COMMAND)
    }

    /**（Pad—>CMS）发送继续执行任务指令*/
    private fun sendContinue() {
        sendAllCmsPad(7, null, null, null, null, CMS_SERVICE_COMMAND)
    }


    /**
     * 发送狭窄区域示教点
     * @param pathType 每个路径类型（0：直线   1：贝塞尔）
     * @param nodeLoc  具体路径坐标（按路线顺序排列）
     * @param areaId 区域id（id为String类型转换成byte[]）
     */
    private fun sendTeachPath(pathType: IntArray?, nodeLoc: DoubleArray?, areaId: ByteArray?) {
        sendAllCmsPad(
            10, nodeLoc, pathType, null, areaId, CMS_SERVICE_COMMAND
        )
    }


    /*** 通知控制台 停止发送手动停止状态*/
    private fun sendStopWritePdf(cmd: Int) {
        val iParams = IntArray(1)
        iParams[0] = cmd
        sendAllCmsPad(13, null, iParams, null, null, CMS_SERVICE_COMMAND)
    }


    /**
     *  pad->cms
     * 外部控制台离线
     */
    override fun sendOffline() {
        sendAllCmsPad(14, null, null, null, null, CMS_SERVICE_COMMAND)
    }

    /**
     *（Pad—>CMS）
     * 和CMS控制台通信
     */
    private fun sendAllCmsPad(
        commandId: Byte,
        dParams: DoubleArray?,
        iParams: IntArray?,
        sParams: Array<String?>?,
        bParams: ByteArray?,
        channelName: String
    ): Boolean = mULCMHelper.sendCMS(commandId, dParams, iParams, sParams, bParams, channelName)
    /*****************************************pad接收CMS控制器新信号 start ****************************************/


    /***************************CMS_UI_COMMAND  方法  start***************************/

    /**
     *CMS反馈任务执行情况（CMS—>Pad）
     *
     * 上报频率：3秒上报1次
     *
     *0：无任务；
     *1：有任务：正常显示CMS推送任务相关数据；
     *2：正在计算中（Pad显示），CMS正在计算任务执行相关数据
     *3: 回站中
     *4: 等待点等待
     */
    private fun receiveCmsTaskState(rt: CmsPadInteraction_) {
        val taskState = rt.dparams[0].toInt()
        when (taskState) {
            0 -> {
                CURRENT_TASK_STATE = TaskState.NO_TASK
            }

            1 -> {
                CURRENT_TASK_STATE = TaskState.HAVE_TASK
            }

            2 -> {
                CURRENT_TASK_STATE = TaskState.UNDER_CALCULATION
            }

            3 -> {
                CURRENT_TASK_STATE = TaskState.RETURNING_TO_THE_STATION
            }

            4 -> {
                CURRENT_TASK_STATE = TaskState.WAIT_POINT
            }
        }
        LiveEventBus.get<CmsPadInteraction_>(KEY_TASK_STATE).post(rt)
    }

    /***CMS发送已清扫完区域id和即将清扫区域id（CMS—>Pad）*/

    private fun receiveCmsAreaState(mCmsPt: CmsPadInteraction_) {
        val iParams = mCmsPt.iparams
        finishCleanAreaId(iParams[0])
        nextCleaningAreaId(iParams[1])
        cleaningLayer(iParams[2])
        cleaningAreaId(iParams[3])
    }

    private fun finishCleanAreaId(value: Int) {
        LiveEventBus.get(KEY_FINISH_CLEAN_AREA_ID, Int::class.java).post(value)
    }

    private fun nextCleaningAreaId(value: Int) {
        LiveEventBus.get(KEY_NEXT_CLEANING_AREA_ID, Int::class.java).post(value)
    }

    private fun cleaningLayer(value: Int) {
        LiveEventBus.get(KEY_CLEANING_LAYER, Int::class.java).post(value)
    }

    private fun cleaningAreaId(value: Int) {
        LiveEventBus.get(KEY_CLEANING_ID, Int::class.java).post(value)
    }


    /***************************CMS_UI_COMMAND  方法  end***************************/


    /*****************************************pad接收CMS控制器新信号 end ****************************************/


    /**********************************************************************************************/
    /***********************************************CMS 控制器 协议结束*******************************/
    /**********************************************************************************************/


    /**********************************************************************************************/
    /***********************************************车体 协议开始*******************************/
    /**********************************************************************************************/

    /**
     * pad->车体
     * 手动清扫
     *
     */
    override fun mSetMenuSwitch() = sendChangeMode(1)

    /**
     * pad->车体
     * 自动动清扫
     */
    override fun mSetAutoSwitch() = sendChangeMode(2)

    /**
     * pad->车体
     * 空状态
     */
    override fun mSetNullSwitch() = sendChangeMode(3)


    //单机区域、agv混行区域切换 暂无使用
    override fun mSendChangeMultiMode(mod: Byte) = sendChangeMultiMode(mod)

    // （离线任务） 暂无使用
    override fun mSendOfflineTask(taskId: Byte) = sendOfflineTask(taskId)

    /**
     * pad->车体
     * 行走速度
     *
     */
    override fun mSetSpeed(spd: Byte) = sendChangeSpeed(spd)

    /**
     * pad->车体
     * 发送定位点 （发送上线点）
     *
     */
    override fun mSendOnlinePoint(layerId: Int, pointArray: FloatArray) =
        sendOnlinePoint(layerId, pointArray)

    /**
     * pad->车体
     * 声呐开关
     *
     */
    override fun mSendSensorOnOff(swt: Boolean, laser: Int) =
        if (swt) sendSafeOnOff(1, laser.toByte(), -1)
        else sendSafeOnOff(
            2, laser.toByte(), -1
        )

    /**
     * pad->车体
     * 激光开关
     *
     */
    override fun mSendLaserOnOff(swt: Boolean, sensor: Int) =
        if (swt) sendSafeOnOff(sensor.toByte(), 1, -1)
        else sendSafeOnOff(
            sensor.toByte(), 2, -1
        )

    //暂无使用
    override fun mSendTimeSync(
        year: Double, month: Double, day: Double, hour: Double, minute: Double, second: Double
    ) = sendTimeSync(year, month, day, hour, minute, second)

    //暂无使用
    override fun mSendTargetPoint(id: ByteArray) = sendTargetPoint(id)

    /**
     * pad->车体
     * 切区设置
     *
     */
    override fun mSendPlsArea(onOffSonar: Byte, onOffLaser: Byte, onOffPLS: Byte) = sendSafeOnOff(
        onOffSonar, onOffLaser, onOffPLS
    )

    /**
     * pad->车体
     * 重载文件
     *
     */
    override fun mSendReloadFile() = sendReloadFile()

    /**
     * pad->车体
     * 强制充电
     *
     */
    override fun mSendForceCharge(swt: Boolean) = sendForceCharge(if (swt) 1 else 0)

    /**
     * pad -> CMS
     * 结束充电
     */
    override fun mSendEndCharge() {
        sendAllCmsPad(9, null, null, null, null, CMS_SERVICE_COMMAND)
    }

    //暂无使用 排污水
    override fun mSendDrainage(swt: Boolean) = sendDrainage(if (swt) 0 else 1)

    /**
     * pad->车体
     * 发送开始/结束示教路线
     *
     */
    override fun mSendTeachRoute(cmd: Byte) = sendTeachRouteCmd(cmd)

    /**
     * pad->车体
     * 下发自动标定指令
     *
     */
    override fun mSendAutoCalibration(switchAuto: Byte, number: Byte) =
        sendAutoCalibration(switchAuto, number)


    /**
     * (pad->车体)
     * 任务继续执行
     * 有可能是老的协议？？？
     */
    override fun mSendContinueTask() = sendContinueTask()

    /**
     * pad->车体
     * 耗材重置
     *
     */

    override fun mSendConsumablesReset(index: Int) = sendConsumablesReset(index)

    /**
     * pad->车体
     * 开始排污水
     */
    override fun openDrainValve() {
        val iParams = ByteArray(1)
        iParams[0] = 1
        sendAllParams(5, null, iParams, null, null, CLEAN_SERVICE_COMMAND)
    }

    /**
     * pad->车体
     * 结束排污水
     */
    override fun closeDrainValve() {
        val iParams = ByteArray(1)
        iParams[0] = 0
        sendAllParams(5, null, iParams, null, null, CLEAN_SERVICE_COMMAND)
    }

    /**
     * pad->车体
     * 开始反冲洗
     */
    override fun openBackFlush() {
        val iParams = ByteArray(1)
        iParams[0] = 1
        sendAllParams(20, null, iParams, null, null, CLEAN_SERVICE_COMMAND)
    }

    /**
     * pad->车体
     * 关闭反冲洗
     */
    override fun closeBackFlush() {
        val iParams = ByteArray(1)
        iParams[0] = 0
        sendAllParams(20, null, iParams, null, null, CLEAN_SERVICE_COMMAND)
    }

    /**
     * pad->车体
     * 故障恢复
     */
    override fun oneKeyFullRecovery(position: Int, type: Int) {
        val iParams = ByteArray(16)
        iParams[position] = type.toByte()
        sendAllParams(22, null, iParams, null, null, CLEAN_SERVICE_COMMAND)
//        LogUtil.i("发送恢复故障 position $position", null, TAG_CAR_BODY)
    }

    /**
     * pad->PET 感知
     * 申请相机标定 （0-4）代表0-4号相机
     * 5 代表所有相机
     */
    override fun cameraCalibration(name: String) {
        val pet = perception_t()
        pet.name = name
        mULCMHelper.sendLcmMsg(PERCEPTION_CALIBRATE_COMMAND, pet)
//        LogUtil.i("PAD->PET 发送相机标定请求 ${name}号相机", null, TAG_PET)
    }

    /**
     * PAD->PET
     * 申请相机固件版本
     */
    override fun askCamFirmware() {
        val pet = perception_t()
        pet.name = "camfirmware"
        mULCMHelper.sendLcmMsg(PERCEPTION_ASK_CAMFIRMWARE_COMMAND, pet)
//        LogUtil.i("申请相机固件版本 ", null, TAG_PET)
    }

    /**
     * pad->PET
     * 相机USB是否合理
     */
    override fun sendCameraUSBReasonable() {
        val pet = perception_t()
        pet.name = "camhub"
        mULCMHelper.sendLcmMsg(PERCEPTION_ASK_CAMHUB_COMMAND, pet)
//        LogUtil.d("pad->pet 检测相机USB是否合理", null, TAG_PET)
    }

    /**
     * pad->车体
     * 一键关闭传感器
     * 暂无使用
     *
     */
    override fun mSendSwitchOffSensor() = sendSwitchOffSensor()

    /**
     * pad->车体
     * 车体模型测试准备
     */
    override fun mSendPrepareAGVTest() = sendAGVTest(flag = 1)


    /**
     * pad->车体
     * 车体模型测试开始
     */
    override fun mSendStartAGVTest(speed: Int, distance: Int) =
        sendAGVTest(flag = 2, speed, distance)

    /**
     * pad->车体
     * 车体模型测试结束
     */
    override fun mSendStopAGVTest() = sendAGVTest(flag = 3)

    /**
     * pad->车体
     * 车体自旋测试准备
     */
    override fun mSendPrepareAGVSpinTest() = sendAGVSpinTest(1)

    /**
     * pad->车体
     * 车体自旋测试开始
     */
    override fun mSendStartAGVSpinTest(radian: Double) = sendAGVSpinTest(flag = 2, radian)


    /**
     * pad->车体
     * 车体自旋测试结束
     */
    override fun mSendStopAGVSpinTest() = sendAGVSpinTest(flag = 3)


    /***=========================================================================================**/
    /***=========================================================================================**/
    /***=========================================================================================**/


    /**
     * pad->车体
     *（1：手动控制模式、2：自动运行模式、3：空模式）（注：自动运行模式触发即开始执行任务）
     *
     */
    private fun sendChangeMode(mode: Byte) {
        val iParams = ByteArray(1)
        iParams[0] = mode
        sendAllParams(1, null, iParams, arrayOf("PAD"), null, SERVICE_COMMAND)
    }

    /**
     * pad->车体
     * 用于多机模式切换（即车体连接控制台操作）
     *
     * @param mode 指令的id
     *
     * 暂无使用
     */
    private fun sendChangeMultiMode(mode: Byte) {
        val iParams = ByteArray(1)
        iParams[0] = mode
        sendAllParams(2, null, iParams, null, null, SERVICE_COMMAND)
    }

    /**
     * pad->车体
     * 指定离线运行任务
     *
     * @param taskId 任务的task id
     */
    private fun sendOfflineTask(taskId: Byte) {
        val iParams = ByteArray(1)
        iParams[0] = taskId
        sendAllParams(3, null, iParams, arrayOf("PAD"), null, SERVICE_COMMAND)
    }


    /**
     * pad->车体
     * 任务控制指令 （自动运行开始清扫）
     * iParams[0]
     * cmd 1 任务暂停运行
     * cmd 2 任务恢复
     * cmd 3 清除任务（有回复信号））
     *
     *sParams[0] 来源PAD
     */
    override fun sendPauseTask(cmd: Byte) {
        val iParams = ByteArray(1)
        iParams[0] = cmd
        val sParams = arrayOfNulls<String>(1)
        sParams[0] = "PAD"
        sendAllParams(4, null, iParams, sParams, null, SERVICE_COMMAND)
    }

    /**
     * 安全设备开关指令
     *
     * iparams[0]:（0-pls区域信号、1：打开声呐、2：关闭声呐）
     * iparams[1]:（0-pls区域信号、1：打开激光、2：关闭激光）
     * iparams[2]:（1-15：pls区域、0：关闭pls）
     */
    private fun sendSafeOnOff(onoffSonar: Byte, onoffLaser: Byte, onoffPLS: Byte) {
        val iParams = ByteArray(3)
        iParams[0] = onoffSonar
        iParams[1] = onoffLaser
        iParams[2] = onoffPLS
        sendAllParams(6, null, iParams, null, null, SERVICE_COMMAND)
    }


    /**
     * 时间同步指令
     *
     * @param year   年
     * @param month  月
     * @param day    日
     * @param hour   小时
     * @param minute 分
     * @param second 日
     * @author Lu Yu 2021-6-7
     */
    private fun sendTimeSync(
        year: Double, month: Double, day: Double, hour: Double, minute: Double, second: Double
    ) {
        val dParams = DoubleArray(6)
        dParams[0] = year
        dParams[1] = month
        dParams[2] = day
        dParams[3] = hour
        dParams[4] = minute
        dParams[5] = second
        sendAllParams(7, dParams, null, null, null, SERVICE_COMMAND)
    }

    /**
     * 手动控制指令
     * 10低速，11高速（1:前进、2:后退、3:自旋、4:反向自旋、5:停止、6:右前、7:右后、8：左后、9:左前、10-11:速度档位0、1）
     *
     * @param cmd
     */
    private fun sendChangeSpeed(cmd: Byte) {
        val iParams = ByteArray(2)
        iParams[0] = 1
        iParams[1] = cmd
        sendAllParams(10, null, iParams, null, null, SERVICE_COMMAND)
    }

    /**
     * 指定运行任务目标点（用于单机模式下车体规划路由）
     * iparams[0]、iparams[1]:目标点的节点id号（两个字节拼成一个short类型）
     * 暂无使用
     */
    private fun sendTargetPoint(id: ByteArray) {
        val iParams = ByteArray(2)
        iParams[0] = id[0]
        iParams[1] = id[1]
        sendAllParams(11, null, iParams, null, null, SERVICE_COMMAND)
    }


    /**
     * 在节点上进行定位
     *
     * @param pointArray 对应节点坐标 x,y,theta
     */
    private fun sendOnlinePoint(layerId: Int, pointArray: FloatArray) {
        LogUtil.i(" 在节点上进行定位 layerId = " + layerId + "，pointArray = " + pointArray[0] + ", " + pointArray[1] + ", " + pointArray[2])

        val iParams = ByteArray(1)
        iParams[0] = layerId.toByte()
        val pointArrayDouble = DoubleArray(3)
        for (i in pointArray.indices) {
            pointArrayDouble[i] = pointArray[i].toDouble()
        }
        sendAllParams(19, pointArrayDouble, iParams, null, null, SERVICE_COMMAND)
    }

    /**
     * 发送开始/结束示教路线
     *
     * @param cmd 1-开始 2-结束
     */
    private fun sendTeachRouteCmd(cmd: Byte) {
        val iParams = ByteArray(1)
        iParams[0] = cmd
        sendAllParams(21, null, iParams, arrayOf("PAD"), null, SERVICE_COMMAND)
    }

    /**********************************************************************************************/
    /***********************************************车体 协议结束*******************************/
    /**********************************************************************************************/


    override fun mSendReloadSpecialFile() = sendReloadSpecialFile()
    override fun mSendTeachPath(pathType: IntArray?, nodeLoc: DoubleArray?, areaId: ByteArray?) =
        sendTeachPath(pathType, nodeLoc, areaId)


    /*** 通知控制台 停止发送手动停止状态*/
    override fun mSendStopWritePdf(cmd: Int) = sendStopWritePdf(cmd)

    /*** 通知LP 录制DX*/
    override fun mSendRecordLPDX(isRecord: Boolean) = sendLPDx(isRecord)


    /**
     * (pad->车体)
     * 重载文件
     */
    private fun sendReloadFile() = sendAllParams(22, null, null, null, null, SERVICE_COMMAND)

    /**
     * (pad->车体)
     * 任务继续执行
     */
    private fun sendContinueTask() = sendAllParams(23, null, null, null, null, SERVICE_COMMAND)


    /**
     * (pad->车体)
     *  强制充电
     *
     * @param cmd
     */
    private fun sendForceCharge(cmd: Byte) {
        val iParams = ByteArray(1)
        iParams[0] = cmd
        sendAllParams(24, null, iParams, null, null, SERVICE_COMMAND)
    }

    /*** 一键关闭传感器*/
    private fun sendSwitchOffSensor() = sendAllParams(26, null, null, null, null, SERVICE_COMMAND)


    /*** 黑匣子接收回复*/
    private fun sendAnswerBlackBox(name: String) =
        sendAllParams(27, null, null, null, name.toByteArray(), SERVICE_COMMAND)

    /*** 下发自动标定指令*/
    private fun sendAutoCalibration(switchAuto: Byte, number: Byte) {

        val iParams = if (switchAuto.toInt() == 1) ByteArray(2) else ByteArray(1)
        iParams[0] = switchAuto
        if (switchAuto.toInt() == 1) iParams[1] = number

        sendAllParams(29, null, iParams, null, null, SERVICE_COMMAND)
    }

    /*** 排污水*/
    private fun sendDrainage(cmd: Byte) {
        val iParams = ByteArray(1)
        iParams[0] = cmd
        sendAllParams(31, null, iParams, null, null, SERVICE_COMMAND)
    }

    /*** 车体模型测试 */
    private fun sendAGVTest(flag: Int, speed: Int = 1, distance: Int = 1) {
        val iParams = ByteArray(3)

        iParams[0] = flag.toByte()
        if (flag == 2) {
            iParams[1] = speed.toByte()
            iParams[2] = distance.toByte()
        }

        sendAllParams(33, null, iParams, null, null, SERVICE_COMMAND)
    }

    /*** 车体自旋测试 */
    private fun sendAGVSpinTest(flag: Int, radian: Double = 0.0) {
        val iParams = ByteArray(1)
        val dParams = DoubleArray(1)

        iParams[0] = flag.toByte()
        if (flag == 2) dParams[0] = radian

        sendAllParams(34, dParams, iParams, null, null, SERVICE_COMMAND)
    }


    /**
     * 手动清扫模式选择
     * iparams[0]:（1:正常清扫、2:重压清扫、3:吸污水、4:干扫模式、5:行走、6：湿扫）
     * iparams[1]:（洒水量0-100%）
     * iparams[2]:（刷盘下降高度0-100mm）
     * iparams[3]:（清洗剂0-关 1-开）
     */
    override fun mSendManualCleanMode(
        mode: Byte, waterLevel: Byte, brushHeight: Byte, cleanSolution: Byte
    ) {
        val iParams = ByteArray(4)
        iParams[0] = mode
        iParams[1] = waterLevel
        iParams[2] = brushHeight
        iParams[3] = cleanSolution
        sendAllParams(
            1, null, iParams, null, null, CLEAN_SERVICE_COMMAND
        )
    }


    /**
     * 耗材重置
     *
     * @param index 耗材下标，从0开始
     */
    private fun sendConsumablesReset(index: Int) {
        val iParams = ByteArray(12)
        iParams[index - 1] = 1
        sendAllParams(4, null, iParams, null, null, CLEAN_SERVICE_COMMAND)
    }

    /*** 和车体通信***/
    private fun sendAllParams(
        commandId: Byte,
        dParams: DoubleArray?,
        iParams: ByteArray?,
        sParams: Array<String?>?,
        bParams: ByteArray?,
        channelName: String
    ) {
        mULCMHelper.sendAllParams(
            commandId, dParams, iParams, sParams, bParams, channelName
        )
    }


    /*****************************************pad接收车体信号 start ****************************************/


    /***************************UI_COMMAND  方法  start***************************/
// 车体状态信息
    private fun recRobotState(rt: robot_control_t) {
        recLcmRobotStateIParams(rt.iparams)
        recLcmRobotStateDParams(rt.dparams)
        receiveAGVXYT(rt)
    }

    private fun recLcmRobotStateIParams(iParams: ByteArray?) {
        if (iParams == null || iParams.isEmpty()) {
            LogUtil.i("接收车体状态信息 iParams null")
            return
        }
//        //// iparams
//        if (iParams.size > 11) receiveAgvRunState(iParams[11].toInt())
//
//        //急停触发
//        if (iParams.size > 12) receiveWarnStopStatus(iParams[12].toInt())
//
//        //充电状态
//        if (iParams.size > 13) receiveChargeState(iParams[13].toInt())
//
//        //电量信息
//        if (iParams.size > 14) receiveBatteryValve(iParams[14].toInt())
//
//        //示教状态
//        if (iParams.size > 15) receiveTeachState(iParams[15].toInt())
//
//        //定位信息
//        if (iParams.size > 18) receiveLocationValve(iParams[18].toInt())
//
//        //agv 状态
//        if (iParams.size > 19) receiveAgvState(iParams[19].toInt())
//
//        //车体初始化状态
//        if (iParams.size > 20) receiveInitInfo(iParams[20].toInt())
//
//        //接收 车体速度档位
//        if (iParams.size > 21) receiveVehicleSpeedGear(iParams[21].toInt())
//
//        //TCS
//        if (iParams.size > 22) receiveTcsState(iParams[22].toInt())
//
//        //控制台中断警告
//        if (iParams.size > 23) receiveModeInfo(iParams[23].toInt())
//
//        //（0: 正常1:避障中;）
//        if (iParams.size > 24) receiveObstacleDetected(iParams[24].toInt())
//
//        //电池状态（0：满电 1：正常 2：电量低 3：电量极低）
//        if (iParams.size > 25) receiveBatteryState(iParams[25].toInt())
//
//        //推拉杆数据异常
//        if (iParams.size > 26) MANUAL_PUSH_ROD_STATUS = iParams[26].toInt()
//
//        //复位按钮状态（0:未触发；1：触发）
//        if (iParams.size > 31) receiveCarReset(iParams[31].toInt())
    }

    private fun recLcmRobotStateDParams(dParams: DoubleArray?) {
        //// dParams
        if (dParams == null || dParams.isEmpty()) {
            LogUtil.i("接收车体状态信息 dParams null")
            return
        }

        if (dParams.size > 4) receiveCarSpeed(dParams[4])
    }


    /** 车体事件 获取系统日志*/
    @RequiresApi(Build.VERSION_CODES.O)
    private fun recAgvEvent(rt: robot_control_t) {
//        if (rt.bparams == null || rt.bparams.isEmpty()) {
//            LogUtil.i("接收车体事件信息 bParams null")
//            return
//        }
//        if (rt.iparams == null || rt.iparams.isEmpty()) {
//            LogUtil.i("接收车体事件信息 iParams null")
//            return
//        }
//
//        val iParams = rt.iparams
//
//        val isRecover = iParams[0].toInt()
//
////        LogUtil.d("接收车体事件信息 iParams.size ${iParams.size}")
//        val logID = if (iParams.size > 1) {
//            iParams[1].toInt()
//        } else {
//            //先用随机数 以后在改
//            (100..1000).random()
//        }
//
//        val value = String(rt.bparams)
//
//        val logEntry = LogEntry(isRecover, logID, value, LocalDateTime.now(), 1)
//
//        LiveEventBus.get(KEY_AGV_EVENT, LogEntry::class.java).post(logEntry)
    }


    /***************************UI_COMMAND  方法  end***************************/


    /***************************CLEAN_UI_COMMAND  方法  start***************************/

    /**
     * 洗地机器人传感器信息
     */
    private fun sensorInformation(iParams: ByteArray?) {
//        if (iParams == null || iParams.isEmpty()) {
//            LogUtil.e("接收洗地机器人传感器信息 iParams null")
//            return
//        }
//        //清水箱液位模拟量（0-100）
//        if (iParams.size > 6) {
//            receiveCleanWaterValve(iParams[6].toInt())
//        }
//        //污水箱液位模拟量（0-100）
//        if (iParams.size > 7) {
//            receiveSewageWaterValve(iParams[7].toInt())
//        }
//        //当前清扫模式
//        if (iParams.size > 8) {
//            CURRENT_CLEAN_MOOD = iParams[8].toInt()
//        }
//        //当前洒水档位（0-100）
//        if (iParams.size > 9) {
//            WATER_CURRENT_LEVEL = iParams[9].toInt()
//        }
//        //刷盘推杆下降档位（0-100）
//        if (iParams.size > 11) {
//            ROD_LEVEL = iParams[11].toInt()
//        }
//        //节水量液位
//        if (iParams.size > 13) {
//            receiveDetergentValve(iParams[13].toInt())
//        }
//        //清水状态 （0:未加水；1:加水中）
//        if (iParams.size > 14) {
//            CLEAN_WATER_LEVEL_STATE = iParams[14].toInt()
//        }
//        //污水状态（0:未排水；1:排水中）
//        if (iParams.size > 15) {
//            SEWAGE_WATER_LEVEL_STATE = iParams[15].toInt()
//        }
//        //排污阀状态 (0:关 1:开)
//        if (iParams.size > 18) {
//            receiveDrainValve(iParams[18].toInt())
//        }
    }

    /****轮子速度 左右轮速*/
    private fun receiveAgvSpeed(dParams: DoubleArray?) {
//        if (dParams == null || dParams.isEmpty()) {
//            LogUtil.e("接收洗地机器人 轮子速度 左右轮速 dParams null")
//            return
//        }
//        MANUAL_SPEED_LEFT = dParams[0]
//        MANUAL_SPEED_RIGHT = dParams[1]
    }


    /*** 洗地机器人上传易损件信息 耗材统计*/
    private fun receiveConsumables(rt: robot_control_t) {
//        val dParams = rt.dparams
//        if (dParams == null || dParams.isEmpty()) {
//            LogUtil.e("接收洗地机器人上传易损件信息 耗材统计 dParams null")
//            return
//        }
//        LiveEventBus.get(KEY_CONSUMABLES, DoubleArray::class.java).post(dParams)
    }


    private var faultInformation = false

    private var miParam0 = 0
    private var miParam1 = 0
    private var miParam2 = 0
    private var miParam3 = 0
    private var miParam4 = 0
    private var miParam5 = 0
    private var miParam6 = 0
    private var miParam7 = 0
    private var miParam8 = 0
    private var miParam9 = 0
    private var miParam10 = 0
    private var miParam11 = 0
    private var miParam12 = 0
    private var miParam13 = 0
    private var miParam14 = 0
    private var miParam15 = 0

    private fun receiveFailureBackStation(rt: robot_control_t) {
//        val iParams: ByteArray? = rt.iparams
//        if (iParams == null || iParams.isEmpty()) {
//            LogUtil.i("接收车体故障回站信息 iParams null")
//            return
//        }
//        //需要人为恢复
//        val iParam0 = iParams[0].toInt()
//        val iParam1 = iParams[1].toInt()
//        val iParam2 = iParams[2].toInt()
//        val iParam3 = iParams[3].toInt()
//        val iParam4 = iParams[4].toInt()
//        val iParam5 = iParams[5].toInt()
//        val iParam6 = iParams[6].toInt()
//        val iParam7 = iParams[7].toInt()
//        val iParam8 = iParams[8].toInt()
//        val iParam9 = iParams[9].toInt()
//        val iParam10 = iParams[10].toInt()
//        val iParam11 = iParams[11].toInt()
//        val iParam12 = iParams[12].toInt()
//        val iParam13 = iParams[13].toInt()
//        val iParam14 = iParams[14].toInt()
//        val iParam15 = iParams[15].toInt()
//
//        val show =
//            iParam0 == 1 || iParam1 == 1 || iParam2 == 1 || iParam3 == 1 || iParam4 == 1 || iParam5 == 1 || iParam6 == 1 || iParam7 == 1 || iParam8 == 1 || iParam9 == 1 || iParam10 == 1 || iParam11 == 1 || iParam12 == 1 || iParam13 == 1 || iParam14 == 1 || iParam15 == 1
//        //需要人为恢复的故障回站信息
//        //只要有一个上报 就要弹框
//        if (faultInformation != show) {
//            faultInformation = show
//            FAULT_INFORMATION = show
//            LogUtil.i("接收车体故障回站信息---------------", null, TAG_CAR_BODY)
//            LiveEventBus.get(KEY_FAILURE_BACK_STATION, Boolean::class.java).post(faultInformation)
//        }
//
//        //真空度过高导致故障回站
//        if (miParam0 != iParam0) {
//            miParam0 = iParam0
//            mFaultIParam0 = iParam0
//            LiveEventBus.get(KEY_FAILURE_BACK_STATION_MANUAL, ManualFaultBean::class.java)
//                .post(ManualFaultBean(0, iParam0))
//            LogUtil.i("真空度过高导致故障回站---${iParam0}---> 1 故障 0正常", null, TAG_CAR_BODY)
//        }
//
//        //真空度过低导致故障回站
//        if (miParam1 != iParam1) {
//            miParam1 = iParam1
//            mFaultIParam1 = iParam1
//            LiveEventBus.get(KEY_FAILURE_BACK_STATION_MANUAL, ManualFaultBean::class.java)
//                .post(ManualFaultBean(1, iParam1))
//            LogUtil.i("真空度过低导致故障回站---${iParam1}---> 1 故障 0正常", null, TAG_CAR_BODY)
//        }
//        //真空度故障导致故障回站
//        if (miParam2 != iParam2) {
//            miParam2 = iParam2
//            mFaultIParam2 = iParam2
//            LiveEventBus.get(KEY_FAILURE_BACK_STATION_MANUAL, ManualFaultBean::class.java)
//                .post(ManualFaultBean(2, iParam2))
//            LogUtil.i("真空度故障导致故障回站---${iParam2}--->1 故障 0正常", null, TAG_CAR_BODY)
//        }
//        //刷盘电机1故障导致回站
//        if (miParam3 != iParam3) {
//            miParam3 = iParam3
//            mFaultIParam3 = iParam3
//            LiveEventBus.get(KEY_FAILURE_BACK_STATION_MANUAL, ManualFaultBean::class.java)
//                .post(ManualFaultBean(3, iParam3))
//            LogUtil.i("刷盘电机1故障导致回站---${iParam3}--->1 故障 0正常", null, TAG_CAR_BODY)
//        }
//        //刷盘电机2故障导致回站
//        if (miParam4 != iParam4) {
//            miParam4 = iParam4
//            mFaultIParam4 = iParam4
//            LiveEventBus.get(KEY_FAILURE_BACK_STATION_MANUAL, ManualFaultBean::class.java)
//                .post(ManualFaultBean(4, iParam4))
//            LogUtil.i("刷盘电机2故障导致回站---${iParam4}--->1 故障 0正常", null, TAG_CAR_BODY)
//        }
//        //刷盘电机3故障导致回站
//        if (miParam5 != iParam5) {
//            miParam5 = iParam5
//            mFaultIParam5 = iParam5
//            LiveEventBus.get(KEY_FAILURE_BACK_STATION_MANUAL, ManualFaultBean::class.java)
//                .post(ManualFaultBean(5, iParam5))
//            LogUtil.i("刷盘电机3故障导致回站---${iParam5}--->1 故障 0正常", null, TAG_CAR_BODY)
//
//        }
//        //边刷电机故障导致回站
//        if (miParam6 != iParam6) {
//            miParam6 = iParam6
//            mFaultIParam6 = iParam6
//            LiveEventBus.get(KEY_FAILURE_BACK_STATION_MANUAL, ManualFaultBean::class.java)
//                .post(ManualFaultBean(6, iParam6))
//            LogUtil.i("边刷电机故障导致回站---${iParam6}--->1 故障 0正常", null, TAG_CAR_BODY)
//
//        }
//        //刷盘推杆升降不到位导致故障回站
//        if (miParam7 != iParam7) {
//            miParam7 = iParam7
//            mFaultIParam7 = iParam7
//            LiveEventBus.get(KEY_FAILURE_BACK_STATION_MANUAL, ManualFaultBean::class.java)
//                .post(ManualFaultBean(7, iParam7))
//            LogUtil.i(
//                "刷盘推杆升降不到位导致故障回站---${iParam7}--->1 故障 0正常", null, TAG_CAR_BODY
//            )
//        }
//        //刮水趴推杆升降不到位导致故障回站
//        if (miParam8 != iParam8) {
//            miParam8 = iParam8
//            mFaultIParam8 = iParam8
//            LiveEventBus.get(KEY_FAILURE_BACK_STATION_MANUAL, ManualFaultBean::class.java)
//                .post(ManualFaultBean(8, iParam8))
//            LogUtil.i(
//                "刮水趴推杆升降不到位导致故障回站---${iParam8}--->1 故障 0正常", null, TAG_CAR_BODY
//            )
//        }
//        //尘推推杆升降不到位导致故障回站
//        if (miParam9 != iParam9) {
//            miParam9 = iParam9
//            mFaultIParam9 = iParam9
//            LiveEventBus.get(KEY_FAILURE_BACK_STATION_MANUAL, ManualFaultBean::class.java)
//                .post(ManualFaultBean(9, iParam9))
//            LogUtil.i(
//                "尘推推杆升降不到位导致故障回站---${iParam9}--->1 故障 0正常", null, TAG_CAR_BODY
//            )
//        }
//        //清扫板通讯故障导致故障回站
//        if (miParam10 != iParam10) {
//            miParam10 = iParam10
//            mFaultIParam10 = iParam10
//            LiveEventBus.get(KEY_FAILURE_BACK_STATION_MANUAL, ManualFaultBean::class.java)
//                .post(ManualFaultBean(10, iParam10))
//            LogUtil.i(
//                "清扫板通讯故障导致故障回站---${iParam10}--->1 故障 0正常", null, TAG_CAR_BODY
//            )
//        }
//        //功率板通讯故障导致故障回站
//        if (miParam11 != iParam11) {
//            miParam11 = iParam11
//            mFaultIParam11 = iParam11
//            LiveEventBus.get(KEY_FAILURE_BACK_STATION_MANUAL, ManualFaultBean::class.java)
//                .post(ManualFaultBean(11, iParam11))
//            LogUtil.i(
//                "功率板通讯故障导致故障回站---${iParam11}--->1 故障 0正常", null, TAG_CAR_BODY
//            )
//        }
//        //电池通讯中断导致故障回站
//        if (miParam12 != iParam12) {
//            miParam12 = iParam12
//            mFaultIParam12 = iParam12
//            LiveEventBus.get(KEY_FAILURE_BACK_STATION_MANUAL, ManualFaultBean::class.java)
//                .post(ManualFaultBean(12, iParam12))
//            LogUtil.i(
//                "电池通讯中断导致故障回站---${iParam12}--->1 故障 0正常", null, TAG_CAR_BODY
//            )
//        }
//        //电池放电过流故障导致故障回站
//        if (miParam13 != iParam13) {
//            miParam13 = iParam13
//            mFaultIParam13 = iParam13
//            LiveEventBus.get(KEY_FAILURE_BACK_STATION_MANUAL, ManualFaultBean::class.java)
//                .post(ManualFaultBean(13, iParam13))
//            LogUtil.i(
//                "电池放电过流故障导致故障回站---${iParam13}--->1 故障 0正常", null, TAG_CAR_BODY
//            )
//        }
//        //电池放电温度过高故障导致故障回站
//        if (miParam14 != iParam14) {
//            miParam14 = iParam14
//            mFaultIParam14 = iParam14
//            LiveEventBus.get(KEY_FAILURE_BACK_STATION_MANUAL, ManualFaultBean::class.java)
//                .post(ManualFaultBean(14, iParam14))
//            LogUtil.i(
//                "电池放电温度过高导致故障回站---${iParam14}--->1 故障 0正常", null, TAG_CAR_BODY
//            )
//        }
//        //声呐没有返回值故障导致故障回站
//        if (miParam15 != iParam15) {
//            miParam15 = iParam15
//            mFaultIParam15 = iParam15
//            LiveEventBus.get(KEY_FAILURE_BACK_STATION_MANUAL, ManualFaultBean::class.java)
//                .post(ManualFaultBean(15, iParam15))
//            LogUtil.i(
//                "声呐没有返回值故障导致故障回站---${iParam15}--->1 故障 0正常", null, TAG_CAR_BODY
//            )
//        }
    }

    /*** 洗地机器人一体机交互信息（lora模块版本）*/
    private fun receiveIntegratedMachineInteraction(rt: robot_control_t) {
        val iParams = rt.iparams
        if (iParams == null || iParams.isEmpty()) {
            LogUtil.i("洗地机器人一体机交互信息（lora模块版本) iParams null")
            return
        }
        //先判断状态
        if (iParams.size > 39) receiveChargeChangeWater(iParams[39].toInt())

        //充电故障
        if (iParams.size > 31) receiveChargeError(iParams[31].toInt())
        //数据长时间不变
        if (iParams.size > 32) receiveLongTermNoChange(iParams[32].toInt())
        //一体机排水桶已满,请及时清理（0: 未触发；1: 触发）
        if (iParams.size > 33) receiveDrainageBucketFull(iParams[33].toInt())
        //数据异常（0: 未触发；1: 触发）
        if (iParams.size > 34) receiveDataException(iParams[34].toInt())
        //电极高温（0: 未触发；1: 触发）
        if (iParams.size > 35) receiveElectrodeHighTemperature(iParams[35].toInt())
        //补给站污水箱满（0: 未触发；1: 触发）
        if (iParams.size > 36) receiveSewageTankFull(iParams[36].toInt())
        //补给站清水箱空（0: 未触发；1: 触发）
        if (iParams.size > 37) receiveCleanWaterEmpty(iParams[37].toInt())
        //反光板识别失败
        if (iParams.size > 38) receiveReflectivePlateRecognitionFailed(iParams[38].toInt())
    }

    /*** 洗地机器人跳转充电换水页面信号）*/
    private var mChargeChangeWaterPageValue = 0
    private fun receiveChargeChangeWaterPageState(rt: robot_control_t) {
        val iParams = rt.iparams
        if (iParams == null || iParams.isEmpty()) {
            LogUtil.i("洗地机器人跳转充电换水页面 iParams null")
            return
        }
        val value = iParams[0].toInt()
        if (mChargeChangeWaterPageValue == value) return
        else {
//            LogUtil.i(
//                "变化 洗地机器人跳转充电换水页面状态:${value}  -->(0关闭 1跳转)", null, TAG_CAR_BODY
//            )
            mChargeChangeWaterPageValue = value
//            LiveEventBus.get(KEY_CHARGE_CHANGE_WATER_PAGE_VALUE, Int::class.java).post(value)
        }
    }


    /**充电故障(*/
    private var mChargeErrorValue = 0
    private fun receiveChargeError(value: Int) {
//        if (mChargeErrorValue == value) return
//        else {
//            LogUtil.i(
//                "变化 充电故障:${value}  -->( 0未故障   1 故障", null, TAG_CAR_BODY
//            )
//            mChargeErrorValue = value
//            CHARGE_ERROR_VALUE = value
//            LiveEventBus.get(KEY_CHARGE_ERROR, Int::class.java).post(value)
//        }
    }

    /**数据长时间不变(*/
    private var mLongTermNoChangeValue = 0
    private fun receiveLongTermNoChange(value: Int) {
//        if (mLongTermNoChangeValue == value) return
//        else {
//            LogUtil.i(
//                "变化 数据长时间不变 :${value}  -->( 0未故障   1 故障", null, TAG_CAR_BODY
//            )
//            mLongTermNoChangeValue = value
//            LONG_TERM_NO_CHANGE_VALUE = value
//            LiveEventBus.get(KEY_LONG_TERM_NO_CHANGE, Int::class.java).post(value)
//        }
    }

    /**一体机排水桶已满,请及时清理*/
    private var mDrainageBucketFull = 0
    private fun receiveDrainageBucketFull(value: Int) {
//        if (mDrainageBucketFull == value) return
//        else {
//            LogUtil.i(
//                "变化 一体机排水桶已满,请及时清理 :${value}  -->(0未故障 1 故障)",
//                null,
//                TAG_CAR_BODY
//            )
//            mDrainageBucketFull = value
//            DRAINAGE_BUCKET_FULL_VALUE = value
//            LiveEventBus.get(KEY_DRAINAGE_BUCKET_FULL, Int::class.java).post(value)
//        }
    }

    /**数据异常*/
    private var mDataExceptionValue = 0
    private fun receiveDataException(value: Int) {
//        if (mDataExceptionValue == value) return
//        else {
//            LogUtil.i(
//                "变化 数据异常 :${value}  -->（0: 未触发；1: 触发）", null, TAG_CAR_BODY
//            )
//            mDataExceptionValue = value
//            DATA_EXCEPTION_VALUE = value
//            LiveEventBus.get(KEY_DATA_EXCEPTION, Int::class.java).post(value)
//        }
    }

    /**电极高温*/
    private var mElectrodeHighTemperatureValue = 0
    private fun receiveElectrodeHighTemperature(value: Int) {
//        if (mElectrodeHighTemperatureValue == value) return
//        else {
//            LogUtil.i(
//                "变化 电极高温 :${value}  -->（0: 未触发；1: 触发）", null, TAG_CAR_BODY
//            )
//            mElectrodeHighTemperatureValue = value
//            ELECTRODE_HIGH_TEMPERATURE_VALUE = value
//            LiveEventBus.get(KEY_ELECTRODE_HIGH_TEMPERATURE, Int::class.java).post(value)
//        }
    }

    /**补给站污水箱满*/
    private var mSewageTankFull = 0
    private fun receiveSewageTankFull(value: Int) {
//        if (mSewageTankFull == value) return
//        else {
//            LogUtil.i(
//                "变化 补给站污水箱满 :${value}  -->（0: 未触发；1: 触发）", null, TAG_CAR_BODY
//            )
//            mSewageTankFull = value
//            SEWAGE_TANK_FULL_VALUE = value
//            LiveEventBus.get(KEY_SEWAGE_TANK_FULL, Int::class.java).post(value)
//        }
    }

    /**补给站清水箱空*/
    private var mCleanWaterEmpty = 0
    private fun receiveCleanWaterEmpty(value: Int) {
//        if (mCleanWaterEmpty == value) return
//        else {
//            LogUtil.i(
//                "变化 补给站清水箱空 :${value}  -->（0: 未触发；1: 触发）", null, TAG_CAR_BODY
//            )
//            mCleanWaterEmpty = value
//            CLEAN_WATER_EMPTY_VALUE = value
//            LiveEventBus.get(KEY_CLEAN_WATER_EMPTY, Int::class.java).post(value)
//        }
    }

    /**反光板识别失败*/
    private var mReflectivePlateRecognitionFailedValue = 0
    private fun receiveReflectivePlateRecognitionFailed(value: Int) {
//        if (mReflectivePlateRecognitionFailedValue == value) return
//        else {
//            LogUtil.i(
//                "变化 反光板识别失败 :${value}  -->（0: 未触发；1: 触发）", null, TAG_CAR_BODY
//            )
//            mReflectivePlateRecognitionFailedValue = value
//            REFLECTIVE_PLATE_RECOGNITION_FAILED_VALUE = value
//            LiveEventBus.get(KEY_REFLECTIVE_PLATE_RECOGNITION_FAILED, Int::class.java).post(value)
//        }
    }


    /**充电换水状态*/
    private var mChargeChangeWaterValue = 0
    private fun receiveChargeChangeWater(value: Int) {
//        if (mChargeChangeWaterValue == value) return
//        else {
//            LogUtil.i(
//                "变化 充电换水状态:${value}  -->(1:车体申请一体机中2:申请成功,车体等待接收到红外对射信号3:对射接收成功,等待推杆伸咄 4:推杆伸出到位,加排水进行中5:车体重新对接一体机进行中6:车体换水完成 7:车体处于只充电状态8:车体处于加排水出现超的时)",
//                null,
//                TAG_CAR_BODY
//            )
//            mChargeChangeWaterValue = value
//            CHARGE_CHANGE_WATER_VALUE = value
//            LiveEventBus.get(KEY_CHARGE_WATER, Int::class.java).post(value)
//        }
    }


    /***************************CLEAN_UI_COMMAND  方法  end***************************/

    /*****************************************pad接收车体信号 end ******************************************/


    /**********************************************************************************************/
    /***********************************************控制台协议结束**********************************/
    /**********************************************************************************************/

    /**********************************************************************************************/
    /***********************************************服务控制 协议开始*******************************/
    /**********************************************************************************************/

    /**
     * @description 获取OAT升级信息
     * @author CheFuX1n9
     * @since 2024/5/17 13:49
     */
    override fun getOTAUpdate() {
//        val iParams = ByteArray(1)
//        iParams[0] = AppUtils.getAppVersionCode().toByte()
//        sendAllParams(GET_OTA_UPDATE, null, iParams, null, null, SERVICE_CONTROL_COMMAND)
    }

    /**
     *（Pad—>ServerControl）
     * 清空车体文件
     *
     */
    override fun mSendCleanFiles() = sendCleanFiles()

    /**
     *（Pad—>ServerControl）
     * 发送PadJobs.json文件更新指令（PadJobs.json）
     *
     */
    override fun mSendPadJobsUpdate() = sendPadJobsUpdate()

    /***=========================================================================================**/
    /***=========================================================================================**/
    /***=========================================================================================**/

    /**
     *（Pad—>ServerControl）
     * 发送初始化车体文件指令（CmsConfig.json、VirtualWall,json、PadAreas.json、world_pad.dat）
     *
     */
    private fun sendCleanFiles() {
        sendAllParams(6, null, null, null, null, SERVICE_CONTROL_COMMAND)
    }

    /**
     *（Pad—>ServerControl）
     * 发送PadJobs.json文件更新指令（PadJobs.json）
     *
     */
    private fun sendPadJobsUpdate() {
        sendAllParams(7, null, null, null, null, SERVICE_CONTROL_COMMAND)
    }


    /**
     *（Pad—>CMS）
     * 申请控制台版本
     *
     */
    override fun applyCMSVersion() {
        sendAllCmsPad(4, null, null, null, null, CMS_SERVICE_COMMAND)
    }


    /**
     * (pad->车体)
     * 申请车体版本号
     */
    override fun applyAGVVersion() {
        sendAllParams(25, null, null, null, null, SERVICE_COMMAND)
    }

    /**
     * pad->NAV
     * 申请NAV
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun applyNAVVersion() {
        sendRobotControlNew(28, null, null, null, null, NAVI_SERVICE_COMMAND)
    }

    /**
     * pad->PET
     * 申请感知
     */
    override fun applyPETVersion() {
        val pet = perception_t()
        pet.name = "request"
        mULCMHelper.sendLcmMsg(PERCEPTION_SERVICE_COMMAND, pet)
    }

    /**
     * pad->Server
     * 申请服务端版本
     */
    override fun applyServerVersion() {
        sendAllParams(1, null, null, null, null, SERVICE_CONTROL_COMMAND)
    }

    /**********************************************************************************************/
    /***********************************************服务控制 协议开始*******************************/
    /**********************************************************************************************/

    /***************************LP_COMMAND  方法  start***************************/

    /**
     *（Pad—>LP）
     * 录制DX
     *
     */
    private fun sendLPDx(isRecord: Boolean) {
        sendAllParams(if (isRecord) 1 else 0, null, null, null, null, AVOIDDX_COMMAND)
    }

    /***************************LP_COMMAND  方法  end***************************/


    /***************************SERVICE_CONTROL_UI_COMMAND  方法  start***************************/
    private var mCurrentMrcRamValue = 0.0

    private fun receiveServerMrcRam(s: String) {
//        val ram = s.toBean<ServerInfoBean>().ram
//        if (mCurrentMrcRamValue == ram) return
//        else {
//            // LogUtil.i("变化 MRC RAM:$s  ", null, TAG_SERVER)
//            LiveEventBus.get(KEY_MRC_RAM_VALUE, Double::class.java)
//                .post(s.toBean<ServerInfoBean>().ram)
//        }
    }

    /**
     * @description OTA升级
     * @author CheFuX1n9
     * @since 2024/7/13 12:50
     */
    private fun receiveOtaUpdate(bParams: ByteArray) {
//        LogUtil.i("Server返回OTA信息:" + String(bParams))
//        LiveEventBus.get(KEY_UPDATE_APK, UpdateBean::class.java)
//            .post(String(bParams).toBean<UpdateBean>())
    }

    /**
     * @description 同步PadJobs.json
     * @author mj
     * @since 2024/11/06 13:56
     */
    private fun receiveSyncPadJobs() {
//        LogUtil.i("Server返回同步padJobs.json信号", null, TAG_SERVER)
//        LiveEventBus.get(KEY_SYNC_PAD_JOBS, Int::class.java).post(1)
    }

    /**
     * @description 定时任务
     * @author CheFuX1n9
     * @since 2024/7/13 12:51
     */
    private fun receiveScheduledTask(sParams: Array<String>?) {
//        if (!sParams.isNullOrEmpty()) {
//            LogUtil.i("Server下发定时任务:${sParams[0]}")
//            val task = String(sParams[0].toByteArray(Charsets.ISO_8859_1)).toBean<Task>()
//            LogUtil.i("定时任务字符串编码转换后:${task.toJson()}")
//            LiveEventBus.get(KEY_SCHEDULED_TASK, Task::class.java).post(task)
//        }
    }

    /**
     * @description 定时任务提醒
     * @author CheFuX1n9
     * @since 2024/7/13 12:51
     */
    private fun receiveScheduledTaskReminder() {
        LogUtil.i("Server下发定时任务5分钟提醒")
        LiveEventBus.get<String>(KEY_SCHEDULED_TASK_REMINDER).post("")
    }

    override fun sendCancelScheduledTask() {
        LogUtil.i("SERVICE_CONTROL_COMMAND-commandId = 8")
        sendAllParams(8, null, null, null, null, SERVICE_CONTROL_COMMAND)
    }

    override fun sendServerHeart() {
        sendAllParams(SERVER_HEART, null, null, null, null, SERVICE_CONTROL_COMMAND)
    }

    /**
     * @description 接收车体语音播放
     * @author CheFuX1n9
     * @since 2024/5/23 16:39
     */
    private var mCurrentVoiceType = -1
//    private val voiceTypeBean = VoiceTypeBean()

    private fun receivePlayMusic(rt: robot_control_t) {
//        val iParams = rt.iparams
//        if (iParams == null || iParams.isEmpty()) {
//            LogUtil.e("接收车体音乐 iParams null")
//            return
//        }
//        val voiceType = iParams[0].toInt()
//        voiceTypeBean.fromType = 2
//
//        //车体默认发非0
//        if (voiceType != 0) {
//            voiceTypeBean.voiceType = voiceType
//            LiveEventBus.get(VoiceTypeBean::class.java).post(voiceTypeBean)
//        } else {
//            //车体发0
//            if (mCurrentVoiceType == 0) return
//
//            voiceTypeBean.voiceType = 0
//            LiveEventBus.get(VoiceTypeBean::class.java).post(voiceTypeBean)
//            mCurrentVoiceType = 0
//        }
//
//        if (voiceType != mCurrentVoiceType) {
//            mCurrentVoiceType = voiceType
//            LogUtil.i("PAD语音播放类型改变:$mCurrentVoiceType", null, TAG_CAR_BODY)
//        }
    }

    override fun getCurrentVoiceType(): Int {
        return mCurrentVoiceType
    }
    /***************************SERVICE_CONTROL_UI_COMMAND  方法  end***************************/

    /**
     * @description 接收路径规划结果, 发送至页面
     * @author CheFuX1n9
     * @since 2024/6/3 15:45
     */
    private var mPathPlanPublicId = -1
    private fun receivePlanPathResult(result: PlanPathResult) {
//        if (result.m_strTo == "pad" || result.m_strTo == "CMS") {
//            if (result.m_iPathPlanType != PP_VERSION) {
//                LogUtil.i(
//                    "接收路径规划:${result.m_strTo}, 路径类型:${result.m_iPathPlanType}",
//                    null,
//                    TAG_PP
//                )
//                LogUtil.i("路径规划结果:${result.toJson()}", null, TAG_PP)
//                if (result.m_iPathPlanType == TEACH_PATH_PLAN) {
//                    LogUtil.i(
//                        "示教路径_m_iPathPlanPublicId:${result.m_iPathPlanPublicId}", null, TAG_PP
//                    )
//                    if (mPathPlanPublicId != result.m_iPathPlanPublicId) {
//                        mPathPlanPublicId = result.m_iPathPlanPublicId
//                        LiveEventBus.get(KEY_UPDATE_PLAN_PATH_RESULT, PlanPathResult::class.java)
//                            .post(result)
//                        LogUtil.i(
//                            "更新PP数据 接收示教路径规划:${mPathPlanPublicId}  ${result.toJson()}",
//                            null,
//                            TAG_PP
//                        )
//                    }
//                } else if (result.m_iPathPlanType == GLOBAL_PATH_PLAN) {
//                    LogUtil.i("更新PP数据接 收全局路径规划 ", null, TAG_PP)
//                    LiveEventBus.get(KEY_UPDATE_PLAN_PATH_RESULT, PlanPathResult::class.java)
//                        .post(result)
//                } else if (result.m_iPathPlanType == CLEAN_PATH_PLAN) {
//                    LogUtil.i("更新PP数据 接收清扫路径规划", null, TAG_PP)
//                    LiveEventBus.get(KEY_UPDATE_PLAN_PATH_RESULT, PlanPathResult::class.java)
//                        .post(result)
//                }
//            } else {
//                // result.m_iPathPlanType ==       PP_VERSION 为PP返回版本号, 现Pad不做处理
//            }
//        }
    }

    private fun receivePPReloadFileResult(rt: robot_control_t) {
//        val bParams = rt.bparams
//        if (bParams == null || bParams.isEmpty()) {
//            LogUtil.i("接收PP 热加载数据回复 bParams null")
//            return
//        }
//        val updateFileResult = String(bParams).toBean<UpdateFileResult>()
//        LiveEventBus.get(RESULT_PP_RELOAD_FILE, Int::class.java).post(updateFileResult.ret)
    }

    private fun receiveCMSReloadFileResult(rt: robot_control_t) {
//        val bParams = rt.bparams
//        if (bParams == null || bParams.isEmpty()) {
//            LogUtil.i("接收CMS 热加载数据回复 bParams null")
//            return
//        }
//        val updateFileResult = String(bParams).toBean<UpdateFileResult>()
//        LiveEventBus.get(RESULT_CMS_RELOAD_FILE, Int::class.java).post(updateFileResult.ret)
    }


    private fun receiveLPReloadFileResult(rt: robot_control_t) {
//        val bParams = rt.bparams
//        if (bParams == null || bParams.isEmpty()) {
//            LogUtil.i("接收LP 热加载数据回复 bParams null")
//            return
//        }
//        val updateFileResult = String(bParams).toBean<UpdateFileResult>()
//        LiveEventBus.get(RESULT_LP_RELOAD_FILE, Int::class.java).post(updateFileResult.ret)
    }


    /**
     * @description 接收排污阀开关状态
     * @param value 0-关, 1-开
     * @author CheFuX1n9
     * @since 2024/8/1 15:57
     */
// 默认排污阀状态
    private var mDefaultDrainValve = -1
    private fun receiveDrainValve(value: Int) {
//        if (value == mDefaultDrainValve) {
//            return
//        } else {
//            LogUtil.i("排污阀状态改变为:${value}")
//            mDefaultDrainValve = value
//            DRAIN_VALVE = value
//            LiveEventBus.get(KEY_DRAIN_VALVE, Int::class.java).post(value)
//        }
    }

    /**
     * @description 接收清水箱水位数值
     * @param value （0-100）
     * @author majin
     * @since 2024/11/23
     */
// 默认清水数值
    private var mDefaultCleanWaterValve: Int = 0
    private fun receiveCleanWaterValve(value: Int) {
//        if (value == mDefaultCleanWaterValve) return
//        else {
//            LogUtil.i("变化 当前清水箱水位:${value}", null, TAG_CAR_BODY)
//            mDefaultCleanWaterValve = value
//            CURRENT_CLEAN_LEVEL = value
//            LiveEventBus.get(KEY_CLEAN_WATER, Int::class.java).post(value)
//        }
    }

    /**
     * @description 接收污水箱水位数值
     * @param value （0-100）
     * @author majin
     * @since 2024/11/23
     */
// 默认污水数值
    private var mDefaultSewageWaterValve = 0
    private fun receiveSewageWaterValve(value: Int) {
//        if (value == mDefaultSewageWaterValve) return
//        else {
//            LogUtil.i("变化 当前污水箱水位:${value}", null, TAG_CAR_BODY)
//            mDefaultSewageWaterValve = value
//            CURRENT_SEWAGE_LEVEL = value
//            LiveEventBus.get(KEY_SEWAGE_WATER, Int::class.java).post(value)
//        }
    }

    /**
     * @description 清洁剂
     * @param value （0-100）
     * @author majin
     * @since 2024/11/23
     */
// 默认污水数值
    private var mDefaultDetergentValve = -1
    private fun receiveDetergentValve(value: Int) {
//        if (value == mDefaultDetergentValve) return
//        else {
//            LogUtil.i("变化 当前节水量液位:${value}", null, TAG_CAR_BODY)
//            mDefaultDetergentValve = value
//            CURRENT_AGENT_LEVEL = value
//            LiveEventBus.get(KEY_DETERGENT_WATER, Int::class.java).post(value)
//        }
    }


    /**
     * @description 电量
     * @param value （0-100）
     * @author majin
     * @since 2024/11/23
     */
    private var mCurrentElectricQuantity = -1
    private fun receiveBatteryValve(value: Int) {
//        if (value == mCurrentElectricQuantity) return
//        else {
//            LogUtil.i("变化 电量信息:${value}", null, TAG_CAR_BODY)
//            mCurrentElectricQuantity = value
//            BATTERY_LEVEL = value
//            LiveEventBus.get(KEY_BATTERY_VALVE, Int::class.java).post(value)
//        }
    }

    /**
     * @description 定位信息
     * @param value （0:定位失败；1:定位成功）
     * @author majin
     * @since 2024/11/23
     */
// 默认定位
    private var mCurrentLocationState = 0

    private fun receiveLocationValve(value: Int) {
//        if (value == mCurrentLocationState) return
//        else {
//            LogUtil.i("变化 定位信息:${value}  -->(0:定位失败；1:定位成功)", null, TAG_CAR_BODY)
//            mCurrentLocationState = value
//            LOCATION_VALVE = value
//            LiveEventBus.get(KEY_LOCATION, Int::class.java).post(value)
//        }
    }


    /**
     * @description agv 模式状态
     * @param value 0：手动控制；1：任务准备中；2:任务准备完成；3:自动运行;4:空状态
     * @since 2024/11/23
     */
    private var mCurrentAgvState = 0

    private fun receiveAgvState(value: Int) {
//        if (value == mCurrentAgvState) return
//        else {
//            LogUtil.i(
//                "变化 AGV模式 状态:${value}--->(0：手动控制；1：任务准备中；2:任务准备完成；3:自动运行;4:空状态)",
//                null,
//                TAG_CAR_BODY
//            )
//            mCurrentAgvState = value
//            AGV_STATE = value
//            LiveEventBus.get(KEY_AGV_STATE, Int::class.java).post(value)
//        }
//    }
}


/**
 * @description  TCS 信息
 * @param value  0 在线 1 逻辑在线 2 物理离线
 * @since 2024/11/23
 */
private var mCurrentTcsState = -1

private fun receiveTcsState(value: Int) {
//    if (value == mCurrentTcsState) return
//    else {
//        LogUtil.i("变化 TCS 状态:${value}  -->(0 在线 1 逻辑在线 2 物理离线)", null, TAG_CAR_BODY)
//        mCurrentTcsState = value
//        TCS_STATE = value
//        LiveEventBus.get(KEY_TCS_STATE, Int::class.java).post(value)
//    }
}

/**
 * @description  车体速度档位
 * @param value （0:空档；1: 低速档；2:高速档；3:极低速档）
 * @since 2024/11/23
 */

private var mCurrentSpeedValue = -1
private fun receiveVehicleSpeedGear(value: Int) {
//    if (value == mCurrentSpeedValue) return
//    else {
//        LogUtil.i(
//            "变化 车体速度档位 状态:${value} （0:空档；1: 低速档；2:高速档；3:极低速档）",
//            null,
//            TAG_CAR_BODY
//        )
//        mCurrentSpeedValue = value
//        LiveEventBus.get(KEY_VEHICLE_SPEED_GEAR, Int::class.java).post(value)
//    }
}

/**
 * @description  急停状态
 * @param value （0:急停未触发；1:急停触发）
 * @since 2024/11/23
 */

private var mCurrentWarmStopState = 0
private fun receiveWarnStopStatus(value: Int) {
//    if (value == mCurrentWarmStopState) return
//    else {
//        LogUtil.i("变化 急停状态状态:${value}  -->(0:急停未触发；1:急停触发)", null, TAG_CAR_BODY)
//        mCurrentWarmStopState = value
//        EMERGENCY_STOP = value
//        LiveEventBus.get(KEY_WARM_STOP_STATE, Int::class.java).post(value)
//    }
}


/**
 * @description  复位
 * @param value （0:未触发；1：触发）
 * @since 2024/11/23
 */

private var mCurrentCarReset = 0
private fun receiveCarReset(value: Int) {
//    if (value == mCurrentCarReset) return
//    else {
//        LogUtil.i("变化 复位按钮状态:${value}  -->(0:未触发；1：触发)", null, TAG_CAR_BODY)
//        mCurrentCarReset = value
//        WARM_CAR_RESET = value
//        LiveEventBus.get(KEY_WARM_CAR_RESET, Int::class.java).post(value)
//    }
}

/**
 * @description  速度
 * @param value
 * @since 2024/11/23
 */

private var mCurrentAgvSpeed: Double = 0.0
private fun receiveCarSpeed(value: Double) {
//    if (value == mCurrentAgvSpeed) return
//    else {
////        LogUtil.i("变化 AGV速度:${value}", null, TAG_CAR_BODY)
//        mCurrentAgvSpeed = value
//        LiveEventBus.get(KEY_AGV_SPEED, Double::class.java).post(value)
//    }
}

/**
 * @description  避障
 * @param value （0: 正常1:避障中)
 * @since 2024/11/23
 */

private var mCurrentObstacleDetectedValue: Int = -1
private fun receiveObstacleDetected(value: Int) {
//    if (value == mCurrentObstacleDetectedValue) return
//    else {
//        LogUtil.i("变化 避障状态:${value}  -->(0: 正常 1:避障中)", null, TAG_CAR_BODY)
//        mCurrentObstacleDetectedValue = value
//        LiveEventBus.get(KEY_OBSTACLE_DETECTED, Int::class.java).post(value)
//    }
}

/**
 * @description  电池状态
 * @param value  0：满电 1：正常 2：电量低 3：电量极低
 * @since 2024/11/23
 */

private var mCurrentBatteryStateValue: Int = -1
private fun receiveBatteryState(value: Int) {
//    if (value == mCurrentBatteryStateValue) return
//    else {
//        LogUtil.i(
//            "变化 电池状态:${value}  -->(0：满电 1：正常 2：电量低 3：电量极低)", null, TAG_CAR_BODY
//        )
//        mCurrentBatteryStateValue = value
//        BATTERY_STATE_VALUE = value
//        LiveEventBus.get(KEY_BATTERY_STATE_VALUE, Int::class.java).post(value)
//    }
}


/**
 * @description  充电
 * @param value  0-未充电, 1-充电中
 * @since 2024/11/23
 */

private var mCurrentChargeState = 0
private fun receiveChargeState(value: Int) {
//    // LCM返回充电状态与当前充电状态相同时, 不做处理
//    if (mCurrentChargeState == value) return
//    // LCM返回充电状态与当前充电状态不同时, 判断当前状态取反
//    else {
//        LogUtil.i("变化 当前充电状态:${value}  -->(0-未充电, 1-充电中)", null, TAG_CAR_BODY)
//        mCurrentChargeState = value
//        BATTERY_STATE = value
//        LiveEventBus.get(KEY_CHARGE_STATE, Int::class.java).post(value)
//    }
}

/**
 * @description  AGV 坐标
 * @param  x y theta
 * @since 2024/11/23
 */
private fun receiveAGVXYT(rt: robot_control_t) {
//    if (rt.dparams.isNotEmpty() && rt.dparams.size >= 3) {
//        LiveEventBus.get<robot_control_t>(KEY_AGV_COORDINATE).post(rt)
//        LogUtil.i(
//            "LCM接收转发至页面AGV坐标: x:${rt.dparams[0]} y:${rt.dparams[1]} theta:${rt.dparams[2]}",
//            null,
//            TAG_CAR_BODY
//        )
//    }
}

/**
 * @description  接收车体运行状态
 * @value   (0:正常状态；1: 用户暂停）（暂停按钮使用）
 * @since 2024/11/27
 */
private var mCurrentAgvRunState: Int = 0
private fun receiveAgvRunState(value: Int) {
//    if (mCurrentAgvRunState == value) return
//    else {
//        LogUtil.i(
//            "变化 接收车体运行状态:${value}  -->(0:正常状态；1: 用户暂停)", null, TAG_CAR_BODY
//        )
//        mCurrentAgvRunState = value
//        AGV_RUN_STATE = value
//        LiveEventBus.get(KEY_AGV_RUN_STATE, Int::class.java).post(value)
//    }
}

/**
 * @description  接收士教状态
 * @value    1 开始士教 2 结束士教
 * @since 2024/11/27
 */
private var mCurrentTeachState: Int = -1
private fun receiveTeachState(value: Int) {
//    if (mCurrentTeachState == value) return
//    else {
//        LogUtil.i(
//            "变化 接收士教状态:${value}  -->(1:开始士教 2:结束士教)", null, TAG_CAR_BODY
//        )
//        mCurrentTeachState = value
//        LiveEventBus.get(KEY_TEACH_STATE, Int::class.java).post(value)
//    }
}

/**
 * @description  车体初始化状态
 *
 * @value   bit0:车体初始化中  1
 *          bit1:车体初始化完成，导航初始化中 2
 *          bit2:车体初始化失败 4
 *          bit3:车体未初始化 8
 *          bit4:车体初始化完成,且导航初始化成功 16
 * @since 2024/11/27
 */
private var mCurrentInitInfo: Int = -1
private fun receiveInitInfo(value: Int) {
//    if (mCurrentInitInfo == value) return
//    else {
//        LogUtil.i(
//            "变化 接收车体初始化状态:${value}", null, TAG_CAR_BODY
//        )
//        mCurrentInitInfo = value
//        LiveEventBus.get(KEY_AGV_INIT_STATE, Int::class.java).post(value)
//    }
}

/**
 * @description  手动切自动失败提示
 * @value  0: 切到手动;1：和控制台连接中断;2：切到自动
 * @since 2024/11/27
 */
private var mCurrentSwitchingPrompt: Int = 0
fun receiveModeInfo(value: Int) {
//    if (mCurrentSwitchingPrompt == value) return
//    else {
//        LogUtil.i(
//            "变化 手动切自动失败提示:${value}  -->(0: 切到手动;1：和控制台连接中断;2：切到自动)",
//            null,
//            TAG_CAR_BODY
//        )
//        mCurrentSwitchingPrompt = value
//        SWITCHING_PROMPT_VALUE = value
//        LiveEventBus.get(KEY_SWITCHING_PROMPT, Int::class.java).post(value)
//    }
}

/**
 * 车体上报设置界面的状态给pad
 */
private fun receiveSettingViewState(rt: robot_control_t) {
    val iParams = rt.iparams
    if (iParams == null || iParams.isEmpty()) {
        LogUtil.e("接收设置界面的状态信息 iParams null")
        return
    }
    receiveSonarState(iParams[0].toInt())
    receiveLaserState(iParams[1].toInt())
    receiveChargeSwitchState(iParams[2].toInt())
    receiveCurrentPlsState(iParams[3].toInt())
}

/**
 * @description  接收声纳
 *
 * @value （0:关 1:开）
 * @since 2024/11/28
 */
private var mCurrentSonarValue: Int = -1

fun receiveSonarState(value: Int) {
//    if (mCurrentSonarValue == value) return
//    else {
//        LogUtil.i("变化 声纳状态:${value}  -->0:关 1:开）", null, TAG_CAR_BODY)
//        mCurrentSonarValue = value
//        LiveEventBus.get(KEY_SONAR_VALUE, Int::class.java).post(value)
//    }

}

/**
 * @description  接收激光
 *
 * @value （0:关 1:开）
 * @since 2024/11/27
 */
private var mCurrentLaserValue: Int = -1

fun receiveLaserState(value: Int) {
//
//    if (mCurrentLaserValue == value) return
//    else {
//        LogUtil.i("变化 激光状态:${value}  ->（0:关 1:开）", null, TAG_CAR_BODY)
//        mCurrentLaserValue = value
//        LASER_VALUE = value
//        LiveEventBus.get(KEY_LASER_VALUE, Int::class.java).post(value)
//    }
}


/**
 * @description  充电开关状态
 *
 * @value （0:关 1:开）
 * @since 2024/11/27
 */
private var mCurrentChargeSwitchValue: Int = -1

fun receiveChargeSwitchState(value: Int) {
//
//    if (mCurrentChargeSwitchValue == value) return
//    else {
//        LogUtil.i("变化 充电开关状态:${value}  ->（0:关；1:开）", null, TAG_CAR_BODY)
//        mCurrentChargeSwitchValue = value
//        LiveEventBus.get(KEY_CHARGE_SWITCH_VALUE, Int::class.java).post(value)
//    }
}

/**
 * @description  当前pls区域
 *
 * @value
 * @since 2024/11/27
 */
private var mCurrentCurrentPlsValue: Int = -1

fun receiveCurrentPlsState(value: Int) {
//    if (mCurrentCurrentPlsValue == value) return
//    else {
//        LogUtil.i("变化 当前pls区域:${value}", null, TAG_CAR_BODY)
//        mCurrentCurrentPlsValue = value
//        LiveEventBus.get(KEY_CURRENT_PLS_VALUE, Int::class.java).post(value)
//    }
}

/**
 * @description  车体上报关机信号
 *
 * @value
 * @since 2024/11/27
 */
private var mCurrentAgvShutdownValue: Int = -1

private fun receiveAgvShutdown(value: Int) {
//    if (mCurrentAgvShutdownValue == value) return
//    else {
//        LogUtil.i("变化 车体上报关机信号", null, TAG_CAR_BODY)
//        mCurrentAgvShutdownValue = value
//
//        LiveEventBus.get(KEY_AGV_SHUTDOWN_VALUE, Int::class.java).post(value)
//    }
}

/**
 * @description  车体上报电池故障
 *
 * @value
 * @since 2024/11/27
 */

private fun receiveAgvBatteryError() {
//    LogUtil.i("变化 车体上报电池故障", null, TAG_CAR_BODY)
//    LiveEventBus.get(KEY_AGV_BATTERY_ERROR_VALUE, Int::class.java).post(1)
}

/**
 * @description  接收定位区域数据源
 *
 * @value
 * @since 2024/11/27
 */
private fun receivePositingArea(rtNew: robot_control_t_new) {
//    val sParams = rtNew.sparams
//    if (sParams == null || sParams.isEmpty()) {
//        LogUtil.i("接收导航定位区域数据 sParams null")
//        return
//    }
//    val value = sParams[0]
//    LiveEventBus.get(KEY_POSITING_AREA_VALUE, String::class.java).post(value)
//    LogUtil.i("变化 接收导航定位区域${value}", null, TAG_NAV)
}


/**
 * @description  导航写完定位区域数据
 *
 * @value
 * @since 2024/11/27
 */
private fun receiveFinishWriteSlam() {
//    LiveEventBus.get(KEY_FINISH_WRITE_SLAM_VALUE, Int::class.java).post(1)
//    LogUtil.i("导航写完定位区域数据结果", null, TAG_NAV)
}

/**
 * @description  接收导航建图步数
 *
 * @value
 * @since 2024/11/27
 */
private var stepNumber = 0
private fun receiveTopScanSteps(rtNew: robot_control_t_new) {
//
//    val dParams = rtNew.dparams
//    if (dParams == null || dParams.isEmpty()) {
//        LogUtil.i("接收导航建图步数 dParams null")
//    }
//    val value = dParams[0].toInt()
//    if (stepNumber == value) return
//    else {
//        LogUtil.i("接收导航建图步数 $value", null, TAG_NAV)
//        stepNumber = value
//        LiveEventBus.get(KEY_NAV_TOP_SCAN_STEPS_VALUE, Int::class.java).post(value)
//    }
}

/**
 * @description  扫描新环境状态
 *
 * @value
 * @since 2024/11/27
 */
private var mCurrentLoadScanValue: Int = -1

private fun receiveLoadScanState(rtNew: robot_control_t_new) {
//    val value = rtNew.iparams[0].toInt()
//    if (mCurrentLoadScanValue == value) return
//    else {
//        LogUtil.i(
//            "接收扫描新环境状态${value}  ->(0 成功 1不在等待建图状态 2 激光无数据 3系统错误 4打开顶视相机失败)",
//            null,
//            TAG_NAV
//        )
//        mCurrentLoadScanValue = value
//        LiveEventBus.get(KEY_NAV_LOAD_SCAN_STATE_VALUE, Int::class.java).post(value)
//    }
}

/**
 * @description  顶视扫描
 *
 * @value
 * @since 2024/11/27
 */
private var mCurrentLoadTopScanValue: Int = -1

private fun receiveLoadTopScan(rtNew: robot_control_t_new) {
//    val value = rtNew.iparams[1].toInt()
//    if (mCurrentLoadTopScanValue == value) return
//    else {
//        LogUtil.i(
//            "接收顶视扫描状态${value}  ->(0-配置文件中未打开顶视 1-相机初始化失败, 2-无激光数据, 3-开始扫图  成功,4-不在顶视扫图状态,5-创建json文件失败,6-停止扫图 成功",
//            null,
//            TAG_NAV
//        )
//        mCurrentLoadTopScanValue = value
//        LiveEventBus.get(KEY_NAV_LOAD_TOP_SCAN_STATE_VALUE, Int::class.java).post(value)
//    }
}

/**
 * @description 导航->pad  导航自动建图时向pad发送子图
 * @value  laserT
 * @since 2024/12/03
 */

private fun receiveSubMap(laserT: laser_t) {
//    LiveEventBus.get(KEY_UPDATE_SUB_MAPS, laser_t::class.java).post(laserT)
}

/**
 * @description 导航->pad   车体位置
 * @value  laserT
 * @since 2024/12/03
 */
private fun receiveRobotPos(laserT: laser_t) {
//    LiveEventBus.get(KEY_UPDATE_POS, laser_t::class.java).post(laserT)
}


/**
 * @description 导航->pad  上激光点云
 * @value  laserT
 * @since 2024/12/03
 */

val rcTopData = laser_t()
private var receiveTopPointCloud = true
private fun receiveCurrentPointCloud(laserT: laser_t) {
    rcTopData.ranges = laserT.ranges
    LiveEventBus.get(KEY_CURRENT_POINT_CLOUD, laser_t::class.java).post(rcTopData)

    if (rcTopData.ranges.isEmpty()) {
        LogUtil.e("导航->pad 接收上激光点云数据异常")
    } else {
        if (receiveTopPointCloud) {
            LogUtil.i("导航->pad 接收上激光点云数据 X:${rcTopData.ranges[0]} Y:${rcTopData.ranges[1]} T:${rcTopData.ranges[2]}")
            receiveTopPointCloud = false
        }
    }
}

/**
 * @description 导航->pad  下激光点云
 * @value  laserT
 * @since 2024/12/05
 */
val rcBottomData = laser_t()
private var receiveBottomPointCloud = true
private fun receiveBottomCurrentPointCloud(laserT: laser_t) {
    rcBottomData.ranges = laserT.ranges
    LiveEventBus.get(KEY_BOTTOM_CURRENT_POINT_CLOUD, laser_t::class.java).post(laserT)

    if (rcBottomData.ranges.isEmpty()) {
        LogUtil.e("导航->pad 接收下激光点云数据异常")
    } else {
        if (receiveBottomPointCloud) {
            LogUtil.i("导航->pad 接收下激光点云数据 X:${rcBottomData.ranges[0]} Y:${rcBottomData.ranges[1]} T:${rcBottomData.ranges[2]}")
            receiveBottomPointCloud = false
        }
    }
}


/**
 * @description 车体->PAD 接收示教中信息
 * @value  robot_control_t
 * @since 2024/12/05
 */
private var mTeachX: Double = 0.0
private var mTeachY: Double = 0.0
private var mTeachT: Double = 0.0
private fun receiveLcmTeachPoint(rt: robot_control_t) {
//    val dParams = rt.dparams
//    if (dParams == null) {
//        LogUtil.i("接收示教中信息 dParams  null")
//        return
//    }
//    val x = dParams[0]
//    val y = dParams[1]
//    val theta = dParams[2]
//
//    if (x == mTeachX && y == mTeachY && mTeachT == theta) return
//    else {
////        LogUtil.d("变化 接收示教中坐标 X:${x} Y:${y} Theta:${theta}", null, TAG_CAR_BODY)
//        mTeachX = x
//        mTeachY = y
//        mTeachT = theta
//        LiveEventBus.get(KEY_TEACH_PATH, TeachPoint::class.java).post(TeachPoint(x, y, theta))
    }
}

/**
 * @description 导航->PAD
 * @value  robot_control_t
 * @since 2024/12/05
 */
private fun receiveOptSubMap(rt: laser_t) {
//    LiveEventBus.get(KEY_OPT_POSE, laser_t::class.java).post(rt)
}


/**
 * @description 导航->PAD 去除噪点结果
 * @value  iParams[0]:应答标志位（0：失败；1：成功）
 * @since 2024/12/05
 */

private fun recRemoveNoiseResult(rtNew: robot_control_t_new) {
    val iParams = rtNew.iparams
    if (iParams == null || iParams.isEmpty()) {
        LogUtil.i("去除噪点结果 iParams  null")
        return
    }
    val value = iParams[0].toInt()
    LogUtil.i("变化 接收去除噪点结果  value $value  （0：失败；1：成功）")
//    LiveEventBus.get(KEY_REMOVE_NOISE_RESULT, Int::class.java).post(value)
}

/**
 * @description 导航->PAD 导航发送标定结果
 * @value    rtNew
 * @since 2024/12/06
 */
private fun receiveCalibrationData(rtNew: robot_control_t_new) {
//    val dParams = rtNew.dparams
//    LogUtil.i("接收导航标定结果 dParams ${dParams[0]}")
//    LiveEventBus.get(KEY_CALIBRATION_DATA, robot_control_t_new::class.java).post(rtNew)
}

/**
 * @description 导航->PAD 导航写入标定结果应答
 * @value   0 成功 1失败
 * @since 2024/12/06
 */
private var mCalibrationResult = -1
private fun receiveCalibrationResult(rtNew: robot_control_t_new) {
//    val iParams = rtNew.iparams
//    if (iParams == null || iParams.isEmpty()) {
//        LogUtil.i("导航写入标定结果应答 iParams  null")
//        return
//    }
//    val value = iParams[0].toInt()
//    if (mCalibrationResult == value) return
//    else {
//        LogUtil.i("接收导航写入标定结果应答${value}  ->( 0 成功 1失败)", null, TAG_NAV)
//        mCalibrationResult = value
//        LiveEventBus.get(KEY_CALIBRATION_RESULT, Int::class.java).post(iParams[0].toInt())
//    }
}

/**
 * @description 导航->PAD 导航加载扩展地图data.pb文件结果
 * @value   0 – 读取失败 1 – 读取成功
 * @since 2024/12/06
 */
private fun receiveLoadExtendedMapDataResult(rtNew: robot_control_t_new) {
//    val iParams = rtNew.iparams
//    if (iParams == null || iParams.isEmpty()) {
//        LogUtil.i("导航加载扩展地图data.pb文件结果 iParams  null")
//        return
//    }
//    LiveEventBus.get(KEY_EXTEND_LOAD_SUB_MAP, ByteArray::class.java).post(iParams)
}

/**
 * @description 导航->PAD 接收导航心跳返回
 * @value   * iparams[0]:导航当前状态（1：定位；2开始建图；3：结束建图；4：开始录制dx）
 *          * iparams[1]:结束建图时，后端优化状态（1：正在优化中；2：优化完成，询问pad是否保存地图；3：正在保存地图； 4：正在取消保存地图）
 * @since 2024/12/06
 */
private fun receiveNaviHeartbeatState(rtNew: robot_control_t_new) {
//    val iParams = rtNew.iparams
//    if (iParams == null || iParams.isEmpty()) {
//        LogUtil.i("接收导航心跳返回 iParams  null")
//        return
//    }
//    LiveEventBus.get(KEY_NAV_HEARTBEAT_STATE, ByteArray::class.java).post(iParams)
}

/**
 * (CMS—>Pad)
 * 控制台版本
 */
private fun receiveCmsVersion(cms: CmsPadInteraction_) {
    val bParams: ByteArray = cms.bparams
//    LiveEventBus.get(KEY_CMS_VERSION, String::class.java).post(String(bParams))
}


/**
 * AGV->pad
 * AGV上报软件版本号
 */

private fun sendAgvVersion(rt: robot_control_t) {
    val bParams: ByteArray = rt.bparams
//    LiveEventBus.get(KEY_AGV_VERSION, String::class.java).post(String(bParams))
}

/**
 * NAV->pad
 * NAV上报软件版本号
 */
private fun receiveNAVVersion(rtNew: robot_control_t_new) {
//    val bParams: ByteArray = rtNew.bparams
//    LiveEventBus.get(KEY_NAV_VERSION, String::class.java).post(String(bParams))
//
//    if (rtNew.sparams.size > 0) {
//        val sparams = rtNew.sparams.get(0)
//        LiveEventBus.get(KEY_TOP_VIEW_VERSION, String::class.java).post(sparams)
//    }
}

/**
 * LP->pad
 * LP上报软件版本号
 */
private fun receiveLPVersion(bParams: ByteArray) {
//    val value = String(bParams)
//    if (LP_VERSION != value) {
//        LP_VERSION = value
//        LogUtil.i("变化接收LP版本 $value")
//    }
}

/**
 * PP->pad
 * PP上报软件版本号
 */
private fun receivePPVersion(result: PlanPathResult) {
//    if (result.m_iPathPlanType == PP_VERSION) {
//        LiveEventBus.get(KEY_PP_VERSION, String::class.java).post(result.m_strAdditionInfo)
//    }
}

/**
 * server->pad
 * server上报软件版本号
 */

private fun receiveServerVersion(rt: robot_control_t) {
//    val bParams: ByteArray = rt.bparams
//    LiveEventBus.get(KEY_SERVER_VERSION, String::class.java).post(String(bParams))
}

/**
 * PET->pad
 * PET上报软件版本号
 */
private fun recPETVersion(perceptionT: perception_t) {
//    LogUtil.i("PET->pad软件版本号${perceptionT.name}", null, TAG_PET)
//    LiveEventBus.get(KEY_PET_VERSION, String::class.java).post(perceptionT.name)
}

/**
 * PET->pad
 * PET上报相机标定结果
 */
private fun recCameraCalibrationResult(perceptionT: perception_t) {
//    LogUtil.i(
//        "PET->pad相机标定结果  相机编号${perceptionT.name}  结果 ${perceptionT.enabled}",
//        null,
//        TAG_PET
//    )
//    LiveEventBus.get(KEY_CAMERA_CALIBRATION_RESULT, CameraCalibrationBean::class.java)
//        .post(CameraCalibrationBean(perceptionT.name, perceptionT.enabled))
}

/*** 洗地机器人是否占用一体机）*/
private var mOccupyingEquipmentValue = 0
private fun receiveOccupyingEquipmentState(rt: robot_control_t) {
//    val iParams = rt.iparams
//    if (iParams == null || iParams.isEmpty()) {
//        LogUtil.i("洗地机器人弹出充电换水等待框 iParams null")
//        return
//    }
//    val value = iParams[0].toInt()
//    if (mOccupyingEquipmentValue == value) return
//    else {
//        LogUtil.i(
//            "变化 洗地机器人弹出充电换水等待框:${value}  -->(0关闭 1跳转)", null, TAG_CAR_BODY
//        )
//        mOccupyingEquipmentValue = value
//        LiveEventBus.get(KEY_OCCUPYING_EQUIPMENT_VALUE, Int::class.java).post(value)
//    }
}


/**
 * NAV->pad
 * 定位状态信息
 */

private fun recNavLocationInfo(rtNew: robot_control_t_new) {
//
//    val iParams = rtNew.iparams
//
//    if (iParams == null || iParams.isEmpty()) {
//        LogUtil.i("定位状态信息 iParams  null", null, TAG_NAV)
//        return
//    }
//
//    receiveNavLocationState(iParams[0].toInt())
//    receiveNavLocationType(iParams[1].toInt())
}

/**
 * 定位状态
 */

var localState = -1
private fun receiveNavLocationState(value: Int) {
//    if (localState != value) {
//        localState = value
//        LOCATION_STATE = value
//        LiveEventBus.get(KEY_LOC_INFO_COMMAND_STATE, Int::class.java).post(value)
//        LogUtil.i("接收 NAV->定位状态${value}", null, TAG_NAV)
//    }
}

/**
 * 定位类型
 */
var localType = -1

private fun receiveNavLocationType(value: Int) {
//    if (localType != value) {
//        localType = value
//        LOCATION_TYPE = value
//        LiveEventBus.get(KEY_LOC_INFO_COMMAND_TYPE, Int::class.java).post(value)
//        LogUtil.i("接收 NAV->定位类型${value}", null, TAG_NAV)
//    }
}

/**
 * PET->pad
 * 是否升级相机固件版本
 */
private fun recPETCamFirmware(perceptionT: perception_t) {
//    val mEnabled = perceptionT.enabled
//    LogUtil.w("接收感知相机版本升级 是否升级 $mEnabled 相机版本${perceptionT.name}", null, TAG_PET)
//    if (mEnabled) {
//        val version = perceptionT.name
//        val mUpdateCamFimWareBean = UpdateCamFimWareBean(mEnabled, version)
//        LiveEventBus.get(KEY_PET_CAMFIRMWARE, UpdateCamFimWareBean::class.java)
//            .post(mUpdateCamFimWareBean)
//    }
}

/*** 机器奥比相机排布是否合理*/
private fun recCameraUSBReasonable(pet: perception_t) {
//    LogUtil.d("机器奥比相机排布是否合理 ${pet.enabled}", null, TAG_PET)
//    LiveEventBus.get(KEY_PET_CAMERA_USB_REASONABLE, Boolean::class.java).post(pet.enabled)
}

/**
 * @description 更新pad日志信息  titleBar小三角颜色变为红色
 * @author mj
 * @since 2025/02/19
 */
private fun receiveSyncPadLog(rt: robot_control_t) {
//    val iParams = rt.iparams
//    if (iParams == null || iParams.isEmpty()) {
//        LogUtil.i("接收Server通知pad更新日志 iParams null")
//        return
//    }
//    val value = iParams[0].toInt()
//    LogUtil.i("Server通知pad更新titleBar日志图标为红色 $value", null, TAG_SERVER)
//    LiveEventBus.get(KEY_UPDATE_LOG, Int::class.java).post(value)
}


/*** 切换地图*/
private fun receiveSwitchMap(rt: robot_control_t) {
//    val mSwitchMap = String(rt.bparams).toBean<SwitchMap>()
//    LiveEventBus.get(KEY_SWITCH_MAP, SwitchMap::class.java).post(mSwitchMap)
//    LogUtil.i("多地图 LCMController 收到切换地图指令 要切换的地图 :${mSwitchMap}")
}

/**
 * @description 接收车体发送跨楼层进出电梯的状态
 * @author mj
 * @since 2025/07/31
 */
private fun receiveCrossFloorStage(rt: robot_control_t) {
    val iParams = rt.iparams
    if (iParams == null || iParams.isEmpty()) {
        LogUtil.i("接收车体发送跨楼层进出电梯的状态 iParams null")
        return
    }
    val value = iParams[0].toInt()
    LiveEventBus.get(KEY_CROSS_FLOOR_STAGE, Int::class.java).post(value)
}
