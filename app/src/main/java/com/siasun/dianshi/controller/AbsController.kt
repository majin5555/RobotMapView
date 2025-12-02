package com.siasun.dianshi.controller

import android.graphics.PointF

/**
 * @author xiaomingliang
 * @create date 2023/06/05
 * 抽象类Controller实现机器人通信，支持机器人回调接口。
 */
abstract class AbsController {
    /**
     * 初始化
     */
    abstract fun init()
    abstract fun destroy()
    abstract fun mSendTaskSubmit(str: String)
    abstract fun sendPauseTask(cmd: Byte)
    abstract fun sendOffline()
    abstract fun mMapStartCreate()
    abstract fun mMapStopCreate()
    abstract fun mRecordDX(sta: Boolean)
    abstract fun mSaveEnvironment(cmdId: Byte, rotate: Float = 0f)
    abstract fun mSendNaviHeartBeat()
//    abstract fun mSendTemplateLoc(templateRoot: TemplateRoot)
    abstract fun mAnswerCalibration()
    abstract fun mWriteCalibration()
    abstract fun mSetMenuSwitch()
    abstract fun mSetAutoSwitch()
    abstract fun mSetNullSwitch()
    abstract fun mSetSpeed(spd: Byte)
    abstract fun mSendChangeMultiMode(mod: Byte)
    abstract fun mSendOfflineTask(taskId: Byte)
    abstract fun mSendOnlinePoint(layerId: Int, pointArray: FloatArray)
    abstract fun mSendManualAutoCharge()
    abstract fun mSendSensorOnOff(swt: Boolean, laser: Int)
    abstract fun mSendLaserOnOff(swt: Boolean, sensor: Int)
    abstract fun mSendDrainage(swt: Boolean)
    abstract fun mSendForceCharge(swt: Boolean)
    abstract fun mSendEndCharge()
    abstract fun mSendTimeSync(
        year: Double, month: Double, day: Double, hour: Double, minute: Double, second: Double
    )

    abstract fun mSendReloadFile()
    abstract fun mSendManualCleanMode(
        mode: Byte, waterLevel: Byte, brushHeight: Byte, cleanSolution: Byte
    )

    abstract fun mSendTeachRoute(cmd: Byte)
    abstract fun mSendContinueTask()
    abstract fun mSendTargetPoint(id: ByteArray)
    abstract fun mSendConsumablesReset(index: Int)
    abstract fun getOTAUpdate()
    abstract fun mSendSwitchOffSensor()
    abstract fun mSendPrepareAGVTest()
    abstract fun mSendStartAGVTest(speed: Int, distance: Int)
    abstract fun mSendStopAGVTest()
    abstract fun mSendPrepareAGVSpinTest()
    abstract fun mSendStartAGVSpinTest(radian: Double)
    abstract fun mSendStopAGVSpinTest()
    abstract fun mSendAutoCalibration(switchAuto: Byte, number: Byte)
//    abstract fun mSendRoutePathCommand(mIPathPlanType: Int, mCleanArea: CleanAreaNew)
    abstract fun mSendControlTask()
    abstract fun mSendContinue()
    abstract fun mSendReloadSpecialFile()
    abstract fun mSendTeachPath(pathType: IntArray?, nodeLoc: DoubleArray?, areaId: ByteArray?)
    abstract fun mSendResetEv()
    abstract fun mSendExtendMap(extendType: Int)
    abstract fun mSendPlsArea(onOffSonar: Byte, onOffLaser: Byte, onOffPLS: Byte)
    abstract fun mSendUpload(file: String)
    abstract fun mSendEraseEvPoint(start: PointF, end: PointF, mapId: Int)
    abstract fun mSendLoadSubMapForExtendMap()
    abstract fun mSendReLoadSubMapForExtendMap(ids: ByteArray)
    abstract fun mSendRecordTop(type: Byte)
//    abstract fun mSendPositingArea(mapID: Int, mList: MutableList<PositingArea>)
    abstract fun mSendGetPositingArea(mapID: Int)
//    abstract fun mSendStartPartialUpdate(mList: MutableList<PartialUpdateArea>, mapID: Int)
    abstract fun forceOnline(mapID: Int)
    abstract fun mSendStopWritePdf(cmd: Int)
    abstract fun mSendRecordLPDX(isRecord: Boolean)
    abstract fun mSendCleanFiles()
    abstract fun mSendPadJobsUpdate()
    abstract fun getCurrentVoiceType(): Int
    abstract fun sendCancelScheduledTask()
    abstract fun sendServerHeart()
    abstract fun openDrainValve()
    abstract fun closeDrainValve()
    abstract fun openBackFlush()
    abstract fun closeBackFlush()
    abstract fun applyAGVVersion()
    abstract fun applyCMSVersion()
    abstract fun applyNAVVersion()
    abstract fun applyPETVersion()
    abstract fun applyServerVersion()
    abstract fun oneKeyFullRecovery(position: Int, type: Int)
    abstract fun cameraCalibration(name: String)
    abstract fun askCamFirmware()
    abstract fun sendCameraUSBReasonable()
}