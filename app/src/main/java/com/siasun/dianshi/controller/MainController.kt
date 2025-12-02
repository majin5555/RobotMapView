package com.siasun.dianshi.controller

import android.graphics.PointF
import com.siasun.dianshi.controller.lcm.LCMController

object MainController {
    /**
     * USE LCM Controller
     */
    var myController: AbsController = LCMController()

    fun init() {
        myController.init()
//        statusCheck()
    }

    fun destroy() {
        myController.destroy()
    }

    /**
     * 低速
     * 对应车体协议极低
     */
    fun setSpd1() {
        myController.mSetSpeed(12)
    }

    /**
     * 中速
     * 对应车体协议低
     */
    fun setSpd2() {
        myController.mSetSpeed(10)
    }

    /**
     * 高速
     * 对应车体协议高
     */
    fun setSpd3() {
        myController.mSetSpeed(11)
    }

    /************************* 发送指令 start***********************************/

    /**
     * 发送任务指令（开始清扫）
     */
    fun sendTaskSubmit(str: String) {
        myController.mSendTaskSubmit(str)
    }

    /**
     * pad->车体
     * 任务控制指令 （自动运行开始清扫）
     * cmd 1 任务暂停运行
     * cmd 2 任务恢复
     * cmd 3 清除任务（有回复信号））
     */
    fun sendPauseTask(cmd: Byte) = myController.sendPauseTask(cmd)

    /**
     *同时通知cms 和 车体）
     *任务继续执行
     */
    fun sendContinueTask() {
        myController.mSendContinueTask()
        sendPauseTask(2)
        myController.mSendContinue()
    }

    /**
     * 手动清扫模式
     */
    fun setMenuSwitch() = myController.mSetMenuSwitch();


    /**
     * 自动清扫模式
     */
    fun setAutoSwitch() = myController.mSetAutoSwitch()

    /**
     * 空状态
     */
    fun setNullSwitch() = myController.mSetNullSwitch()

    /**
     * 开始建立地图
     */
//    fun startCreateEnvironment() {
//        SEND_NAVI_HEART = true
//        myController.mMapStartCreate()
//    }

    /**
     * 结束建立地图
     */
    fun stopCreateEnvironment() = myController.mMapStopCreate()

    /**
     * 手动清扫 标准扫
     */
    fun manualCleanStandard(waterLevel: Byte, brushHeight: Byte, cleanSolution: Byte) =
        myController.mSendManualCleanMode(mode = 1, waterLevel, brushHeight, cleanSolution)

    /**
     * 手动清扫 重压
     */
    fun manualCleanPressure(waterLevel: Byte, brushHeight: Byte, cleanSolution: Byte) =
        myController.mSendManualCleanMode(mode = 2, waterLevel, brushHeight, cleanSolution)


    /**
     * 手动清扫 吸污区
     */
    fun manualCleanSuction() = myController.mSendManualCleanMode(
        mode = 3, waterLevel = 0, brushHeight = 0, cleanSolution = 0
    )

    /**
     * 手动清扫 干扫
     */
    fun manualCleanSwipe(brushHeight: Byte) = myController.mSendManualCleanMode(
        mode = 4, waterLevel = 0, brushHeight, cleanSolution = 0
    )

    /**
     * 手动清扫 行走
     */
    fun manualCleanGo() = myController.mSendManualCleanMode(
        mode = 5, waterLevel = 0, brushHeight = 0, cleanSolution = 0
    )

    /**
     * 手动清扫 尘推
     */
    fun manualDustMop() = myController.mSendManualCleanMode(
        mode = 6, waterLevel = 0, brushHeight = 0, cleanSolution = 0
    )

    /**
     *保存环境 1 保存 2 取消  3 旋转角度
     */
    fun saveEnvironment(cmdId: Byte, rotate: Float = 0f) =
        myController.mSaveEnvironment(cmdId, rotate)


    /**
     *声呐开关
     */
    fun sendSensorOnOff(swt: Boolean, laser: Int) = myController.mSendSensorOnOff(swt, laser)

    /**
     *激光开关
     */
    fun sendLaserOnOff(swt: Boolean, sensor: Int) = myController.mSendLaserOnOff(swt, sensor)

    /**
     *切区设置
     */
    fun sendLaserOnOff(onOffSonar: Byte, onOffLaser: Byte, onOffPLS: Byte) =
        myController.mSendPlsArea(onOffSonar, onOffLaser, onOffPLS)


    /**
     *发送定位点
     */
    fun sendOnlinePoint(layerId: Int, pointArray: FloatArray) =
        myController.mSendOnlinePoint(layerId, pointArray)

    /**
     *同时通知cms 和 车体）
     *清除所有任务
     */
    fun sendControlTask() {
        myController.mSendControlTask()
        sendPauseTask(2)
    }

    /**
     *强制充电
     */
    fun sendForceCharge(swt: Boolean) = myController.mSendForceCharge(swt)

    /**
     * 通知CMS结束充电
     */
    fun sendEndCharge() {
        myController.mSendEndCharge()
    }

    /**
     *自动标定
     */
    fun sendAutoCalibration(swt: Boolean, number: Byte = 0) =
        myController.mSendAutoCalibration(if (swt) 1 else 2, number)

    /**
     *去除噪点 左上角 xy 右下角 x y
     */
    fun sendEraseEvPoint(
        start: PointF, end: PointF, mapId: Int
    ) = myController.mSendEraseEvPoint(start, end, mapId)

    /**
     * 申请路径规划器
     */
//    fun sendRoutePathCommand(
//        pathPlanType: Int, mCleanArea: CleanAreaNew
//    ) = myController.mSendRoutePathCommand(pathPlanType, mCleanArea)

    /**
     * 开始士教
     */
    fun sendStartTeachRoute() = myController.mSendTeachRoute(1.toByte())

    /**
     * 结束士教
     */
    fun sendSopTeachRoute() = myController.mSendTeachRoute(2.toByte())

    /**
     * 通知车体重载文件 弃用
     */
    fun sendReloadFile() = myController.mSendReloadFile()
    fun sendReloadSpecialFile() = myController.mSendReloadSpecialFile()
    fun sendTeachPath(pathType: IntArray?, nodeLoc: DoubleArray?, areaId: ByteArray?) =
        myController.mSendTeachPath(pathType, nodeLoc, areaId)


    /**
     * 自动充电
     */
    fun sendManualAutoCharge() = myController.mSendManualAutoCharge()


    /**
     *耗材信息复位
     */
    fun sendConsumablesReset(index: Int) = myController.mSendConsumablesReset(index)

    /**
     *录制DX
     */
    fun recordDX(sta: Boolean) = myController.mRecordDX(sta)

    /**
     *环境扩展 弃用
     */
    fun sendLoadSubMapForExtendMap() = myController.mSendLoadSubMapForExtendMap()

    /**
     * 重发子图
     * @param ids 子图id数组
     *            -1表示所有子图都重发
     */
    fun sendReLoadSubMapForExtendMap(ids: ByteArray) =
        myController.mSendReLoadSubMapForExtendMap(ids)

    /**
     * @description 请求版本升级
     * @author CheFuX1n9
     * @since 2024/5/17 13:47
     */
    fun getOTAUpdate() {
        // 发送LCM协议, 获取OTA升级信息
        myController.getOTAUpdate()
    }


    /**
     *环境恢复
     */
    fun sendResetEv() = myController.mSendResetEv()

    /**
     *发送模版
     */
//    fun sendTemplateLoc(templateRoot: TemplateRoot) = myController.mSendTemplateLoc(templateRoot)

    fun sendExtendMap(extendType: Int) = myController.mSendExtendMap(extendType)

    /**
     *应答导航
     */
    fun sendAnswerCalibration() = myController.mAnswerCalibration()

    /**
     *发送写入指令
     */
    fun sendWriteCalibration() = myController.mWriteCalibration()

    /**
     *发送 停止手动接收
     *
     */
    fun sendStopWritePdf(cmd: Int) = myController.mSendStopWritePdf(cmd)

    /**
     * 外部控制台离线
     */

    fun sendOffline() = myController.sendOffline()


    /**
     * 准备 车体模型测试
     */
    fun mSendPrepareAGVTest() = myController.mSendPrepareAGVTest()

    /**
     * 开始 车体模型测试
     */
    fun sendStartAGVTest(speed: Int, distance: Int) =
        myController.mSendStartAGVTest(speed, distance)

    /**
     * 结束 车体模型测试
     */
    fun sendStopAGVTest() = myController.mSendStopAGVTest()


    /**
     * 准备 车体自旋测试
     */
    fun mSendPrepareAGVSpinTest() = myController.mSendPrepareAGVSpinTest()

    /**
     * 开始自旋测试
     */
    fun mSendStartAGVSpinTest(radian: Double) = myController.mSendStartAGVSpinTest(radian)

    /**
     *结束自旋测试
     */
    fun mSendStopAGVSpinTest() = myController.mSendStopAGVSpinTest()


    /**
     * 更新文件
     */
    fun sendUpload(str: String) {
        myController.mSendUpload(str)
    }

    /**
     * 通知LP 录制DX
     */
    fun sendRecordLPDx(isRecord: Boolean) = myController.mSendRecordLPDX(isRecord)

    /**
     * 通知server 清空 车体文件
     * CmsConfig.json、
     * VirtualWall,json、
     * PadAreas.json、
     * world_pad.dat
     */
    fun sendCleanFiles() = myController.mSendCleanFiles()

    /**
     *（Pad—>ServerControl）
     * 发送PadJobs.json文件更新指令（PadJobs.json）
     *
     */
    fun sendPadJobsUpdate() = myController.mSendPadJobsUpdate()

    /**
     * Pad—>导航
     * 开始顶视录制
     */
    fun sendStartRecordTop() = myController.mSendRecordTop(1)

    /**
     * Pad—>导航
     * 结束顶视录制
     */
    fun sendStopRecordTop() = myController.mSendRecordTop(2)

    /**
     * Pad—>导航
     * 发送定位区域和定位方式信息
     */
//    fun sendPositingArea(mapID: Int, mList: MutableList<PositingArea>) =
//        myController.mSendPositingArea(mapID, mList)

    /**
     * Pad—>导航
     * 请求定位列表
     */
    fun sendGetPositingAreas(mapID: Int) = myController.mSendGetPositingArea(mapID)

    /**
     * Pad—>导航
     * 开始局部更新
     */
//    fun sendStartPartialUpdate(mList: MutableList<PartialUpdateArea>, mapID: Int = 0) {
//        SEND_NAVI_HEART = true
//        myController.mSendStartPartialUpdate(mList, mapID)
//    }

    /**
     * 打开排污阀
     */
    fun openDrainValve() = myController.openDrainValve()

    /**
     * 关闭排污阀
     */
    fun closeDrainValve() = myController.closeDrainValve()

    /**
     * 打开反冲洗
     */
    fun openBackFlush() = myController.openBackFlush()

    /**
     * 关闭反冲洗
     */
    fun closeBackFlush() = myController.closeBackFlush()

    /**
     * 强制上线
     */
    fun forceOnline(mapID: Int) = myController.forceOnline(mapID)


    /************************* 发送指令 end***********************************/


    fun getCurrentVoiceType(): Int {
        return myController.getCurrentVoiceType()
    }

    /**
     * 发送取消定时任务
     */
    fun sendCancelScheduledTask() = myController.sendCancelScheduledTask()

    /**
     *请求版本号
     */
    fun applyVersion() {
        //CMS
        myController.applyCMSVersion()
        //NAV
        myController.applyNAVVersion()
        //AGV
        myController.applyAGVVersion()
        //PET
        myController.applyPETVersion()
        //server
        myController.applyServerVersion()
        //PP
//        sendRoutePathCommand(
//            PP_VERSION, CleanAreaNew()
//        )
    }

    /**
     * 发送恢复故障
     */
    fun sendOneKeyFullRecovery(position: Int, type: Int) =
        myController.oneKeyFullRecovery(position, type)


    /**
     * 相机标定 （name 0-4 代表0 1 2 3 4 号相机） 5 代表全部相机
     */
    fun sendCameraCalibration(name: String) = myController.cameraCalibration(name)

    /**
     * 向感知 发送申请固件版本
     */
    fun sendCamFirmware() = myController.askCamFirmware()

    /**
     * pad->PET
     * 相机USB是否合理
     */
    fun sendCameraUSBReasonable() = myController.sendCameraUSBReasonable()
}