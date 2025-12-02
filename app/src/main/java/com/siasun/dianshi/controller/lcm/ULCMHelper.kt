package com.siasun.dianshi.controller.lcm

import com.google.gson.Gson
import com.ngu.lcmtypes.robot_control_t
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.AVOIDDX_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.CLEAN_SERVICE_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.CMS_SERVICE_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.CTRL_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.NAVI_SERVICE_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.PLAN_PATH_CONTROL_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.SERVICE_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.SERVICE_CONTROL_COMMAND
import com.pnc.software.siasun.cleanrobot.crl.controller.lcm.SUBSCRIBE_CHANNEL
import com.siasun.dianshi.bean.CmsPadInteraction_
import com.siasun.dianshi.bean.PlanPathControlCommand
import com.siasun.dianshi.bean.PointNew
import com.siasun.dianshi.bean.robot_control_t_new
import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.mapviewdemo.CLEAN_PATH_PLAN
import com.siasun.dianshi.mapviewdemo.TAG_LCM
import lcm.lcm.LCM
import lcm.lcm.LCMEncodable
import lcm.lcm.LCMSubscriber

class ULCMHelper internal constructor() {
    private var lcm: LCM = LCM.getSingleton()
    private var rcmsg: robot_control_t? = null
    private var robotControlNew: robot_control_t_new? = null
    private var cmsPI: CmsPadInteraction_? = null
    private var mPlanPathControlCommand: PlanPathControlCommand? = null
    private var thisOne: LCMSubscriber? = null
    private val gson = Gson()

    /**
     * Init the LCMSubscriber start the LCM
     */
    fun initLCM(subscribeChannel: String, one: LCMSubscriber) {
        thisOne = one
        try {
            lcm.subscribe(subscribeChannel, one)
            mPlanPathControlCommand = PlanPathControlCommand()
//            LogUtil.i("LCM 注册成功:$SUBSCRIBE_CHANNEL", null, TAG_LCM)
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtil.i("LCM 连接异常", e, TAG_LCM)
        }
    }

    /*** 申请路径规划 FreeRegion */
    private var mIPathPlanPublicId = 1
    fun sendRoutePathCommand(
        mIPathPlanType: Int,
        mPstStart: FloatArray,
        mVertexPnt: MutableList<PointNew>,
        mLayerId: Int,
        mRegID: Int,
        mCleanShape: Int
    ) {
        mPlanPathControlCommand?.m_strFrom = "pad";
        mPlanPathControlCommand?.m_strTo = "PathPlanSet";
        mPlanPathControlCommand?.m_iPathPlanPublicId = mIPathPlanPublicId++
        mPlanPathControlCommand?.m_iPathPlanType = mIPathPlanType
        if (mIPathPlanType != 100) {
            mPlanPathControlCommand?.m_uLayerNumber = mLayerId.toShort()
            mPlanPathControlCommand?.m_iRegionNumber = mRegID
        }
        if (mIPathPlanType == 100) {
            mPlanPathControlCommand?.m_iCleanPathPanType = 0
            mPlanPathControlCommand?.m_uLayerNumber = 0
            mPlanPathControlCommand?.m_iPathPlanRegionChoose = 2 // 0：无效 1:全部区域  2：指定区域
            mPlanPathControlCommand?.m_fCleanPathPlanStartPosBuffer?.set(0, mPstStart[0]);
            mPlanPathControlCommand?.m_fCleanPathPlanStartPosBuffer?.set(1, mPstStart[1]);
            mPlanPathControlCommand?.m_fCleanPathPlanStartPosBuffer?.set(2, 0.0f);
            mPlanPathControlCommand?.m_iRegionPoints = 0
            mPlanPathControlCommand?.m_fRegionPointsBuffer = floatArrayOf(0F, 0F)
        }

        when (mIPathPlanType) {
            CLEAN_PATH_PLAN -> {
                mPlanPathControlCommand?.m_iCleanPathPanType = mCleanShape //4：回字形 3：弓字型 6：混合型
                mPlanPathControlCommand?.m_iPathPlanRegionChoose = 2 // 0：无效 1:全部区域  2：指定区域
                mPlanPathControlCommand?.m_fCleanPathPlanStartPosBuffer?.set(0, mPstStart[0])
                mPlanPathControlCommand?.m_fCleanPathPlanStartPosBuffer?.set(1, mPstStart[1])
                mPlanPathControlCommand?.m_fCleanPathPlanStartPosBuffer?.set(2, 0.0f)
                mPlanPathControlCommand?.m_iRegionPoints =
                    mVertexPnt.size * 2 //清扫区域顶点个数的二倍  由于需要将X Y都要装到一起
                mPlanPathControlCommand?.m_fRegionPointsBuffer = FloatArray(mVertexPnt.size * 2)
                var j = 0
                for (i in mVertexPnt.indices) {
                    mPlanPathControlCommand?.m_fRegionPointsBuffer!![j++] = mVertexPnt[i].X
                    mPlanPathControlCommand?.m_fRegionPointsBuffer!![j++] = mVertexPnt[i].Y
                }
            }
        }
    }

    /**
     * pad—>cms 控制器
     */
    fun sendCMS(
        commandId: Byte,
        dParams: DoubleArray?,
        iParams: IntArray?,
        sParams: Array<String?>?,
        bParams: ByteArray?,
        channelName: String
    ): Boolean {
        return try {
            cmsPI = CmsPadInteraction_()
            cmsPI?.commandid = commandId
            if (dParams != null) {
                cmsPI?.dparams = dParams
                cmsPI?.ndparams = dParams.size
            }
            if (iParams != null) {
                cmsPI?.iparams = iParams
                cmsPI?.niparams = iParams.size
            }
            if (sParams != null) {
                cmsPI?.sparams = sParams
                cmsPI?.nsparams = sParams.size
            }
            if (bParams != null) {
                cmsPI?.bparams = bParams
                cmsPI?.nbparams = bParams.size.toLong()
            }
            sendLcmMsg(channelName)
            true
        } catch (var8: Exception) {
            false
        }
    }

    /**
     *
     * pad—>导航
     * pad—>车体
     * pad—>所有端
     */
    fun sendAllParams(
        commandId: Byte,
        dParams: DoubleArray?,
        iParams: ByteArray?,
        sParams: Array<String?>?,
        bParams: ByteArray?,
        channelName: String
    ): Boolean {
        return try {
            when (channelName) {
                SERVICE_COMMAND, CLEAN_SERVICE_COMMAND, CTRL_COMMAND, SERVICE_CONTROL_COMMAND, AVOIDDX_COMMAND -> {
                    rcmsg = robot_control_t()
                    rcmsg!!.commandid = commandId
                    if (dParams != null) {
                        rcmsg!!.dparams = dParams
                        rcmsg!!.ndparams = dParams.size.toByte()
                    }
                    if (iParams != null) {
                        rcmsg!!.iparams = iParams
                        rcmsg!!.niparams = iParams.size.toByte()
                    }
                    if (sParams != null) {
                        rcmsg!!.sparams = sParams
                        rcmsg!!.nsparams = sParams.size.toByte()
                    }
                    if (bParams != null) {
                        rcmsg!!.bparams = bParams
                        rcmsg!!.nbparams = bParams.size.toLong()
                    }

                }

                NAVI_SERVICE_COMMAND -> {
                    robotControlNew = robot_control_t_new()
                    robotControlNew?.commandid = commandId
                    if (dParams != null) {
                        robotControlNew?.dparams = dParams
                        robotControlNew?.ndparams = dParams.size
                    }
                    if (iParams != null) {
                        robotControlNew?.iparams = iParams
                        robotControlNew?.niparams = iParams.size.toByte()
                    }
                    if (sParams != null) {
                        robotControlNew?.sparams = sParams
                        robotControlNew?.nsparams = sParams.size.toByte()
                    }
                    if (bParams != null) {
                        robotControlNew?.bparams = bParams
                        robotControlNew?.nbparams = bParams.size.toLong()
                    }
                }
            }
            sendLcmMsg(channelName)
            true
        } catch (var8: Exception) {
            false
        }
    }

    fun sendLcmMsg(channelName: String): Boolean {
        return try {
            run {
                sendSnyMsg {
                    try {
                        var toJson = ""
                        when (channelName) {
                            CMS_SERVICE_COMMAND -> {
                                lcm.publish(channelName, cmsPI)
                                toJson = gson.toJson(cmsPI)
                            }

                            SERVICE_COMMAND, CLEAN_SERVICE_COMMAND, CTRL_COMMAND, SERVICE_CONTROL_COMMAND, AVOIDDX_COMMAND -> {
                                lcm.publish(channelName, rcmsg)
                                toJson = gson.toJson(rcmsg)
                            }

                            NAVI_SERVICE_COMMAND -> {
                                lcm.publish(channelName, robotControlNew)
                                toJson = gson.toJson(robotControlNew)
                            }

                            PLAN_PATH_CONTROL_COMMAND -> {
                                lcm.publish(channelName, mPlanPathControlCommand)
                                toJson = gson.toJson(mPlanPathControlCommand)
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
                true
            }
        } catch (var2: Exception) {
            false
        }
    }

    /**
     * 多参数
     */
    fun sendLcmMsg(channelName: String, encodable: LCMEncodable): Boolean {
        return try {
            run {
                sendSnyMsg {
                    try {
                        lcm.publish(channelName, encodable)
                    } catch (var2: java.lang.Exception) {
                        var2.printStackTrace()
                    }
                }
                true
            }
        } catch (var2: Exception) {
            false
        }
    }

    private fun sendSnyMsg(runnable: Runnable) {
        Thread(runnable).start()
    }

    fun unsubscribe() {
        lcm.unsubscribe(SUBSCRIBE_CHANNEL, thisOne)
        //  LogUtil.d("LCM 取消注册:$SUBSCRIBE_CHANNEL", null, TAG_LCM)
    }
}
