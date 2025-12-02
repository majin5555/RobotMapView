package com.pnc.software.siasun.cleanrobot.crl.controller.lcm

/****************************************************************
 *                            PAD 发送                            *
 ****************************************************************/
//PAD->AGV
const val SERVICE_COMMAND = "SERVICE_COMMAND"

//PAD->AGV
const val CLEAN_SERVICE_COMMAND = "CLEAN_SERVICE_COMMAND"

//PAD->NAV
const val NAVI_SERVICE_COMMAND = "NAVI_SERVICE_COMMAND"

//PAD->PP
const val PLAN_PATH_CONTROL_COMMAND = "PLAN_PATH_CONTROL_COMMADN"

//PAD->CMS
const val CMS_SERVICE_COMMAND = "CMS_SERVICE_COMMAND"

//PAD->ALL 热更新
const val CTRL_COMMAND = "CTRL_COMMAND"

//Pad -> Server
const val SERVICE_CONTROL_COMMAND = "SERVICE_CONTROL_COMMAND"

//Pad -> LP
const val AVOIDDX_COMMAND = "AVOIDDX_COMMAND"

//Pad -> LP
const val LP_COMMAND = "LP_COMMAND"

//PAD->感知 请求版本
const val PERCEPTION_SERVICE_COMMAND = "PERCEPTION_SERVICE_COMMAND"
//PAD->感知 相机标定
const val PERCEPTION_CALIBRATE_COMMAND = "PERCEPTION_CALIBRATE_COMMAND"
//感知->PAD- 相机标定
const val PERCEPTION_CALIBRESULT_COMMAND = "PERCEPTION_CALIBRESULT_COMMAND"

//PAD->PET 发送请求是否升级相机固件版本
const val PERCEPTION_ASK_CAMFIRMWARE_COMMAND = "PERCEPTION_ASK_CAMFIRMWARE_COMMAND"

//PAD->感知 PAD向PET请求hub上奥比相机USB是否合理
const val PERCEPTION_ASK_CAMHUB_COMMAND = "PERCEPTION_ASK_CAMHUB_COMMAND"

/****************************************************************
 *                            PAD 接收                            *
 ****************************************************************/
//AGV->PAD  示教路径信息
const val TEACH_PATH = "TEACH_PATH"

//AGV->PAD  洗地机器人传感器信息
const val CLEAN_UI_COMMAND = "CLEAN_UI_COMMAND"

//AGV->PAD
const val UI_COMMAND = "UI_COMMAND"

//CMS->PAD
const val CMS_UI_COMMAND = "CMS_UI_COMMAND"

//NAV->PAD
const val OPT_POSE = "OPT_POSE"

//NAV->PAD
const val UPDATE_POS = "UPDATE_POS"

//NAV->PAD  自动建图时向pad发送子图
const val UPDATE_SUBMAPS = "UPDATE_SUBMAPS"

//NAV->PAD
const val NAVI_UI_COMMAND = "NAVI_UI_COMMAND"

//NAV->PAD 上激光点云
const val CURRENT_POINTCLOUD = "CURRENT_POINTCLOUD"

//NAV->PAD 下激光点云
const val BOTTOM_POINTCLOUD = "BOTTOM_POINTCLOUD"

//NAV->PAD 发送定位信息
const val LOCINFO_COMMAND = "LOCINFO_COMMAND"

//PP->PAD 路径规划结果
const val PLAN_PATH_RESULT = "PLAN_PATH_RESULT"

//Server -> Pad
const val SERVICE_CONTROL_UI_COMMAND = "SERVICE_CONTROL_UI_COMMAND"

//AGV->Pad 顶视扫描
const val RECORD_IMAGE = "RECORD_IMAGE"

//PET->PAD 发送版本
const val PERCEPTION_PUBLISH_COMMAND = "PERCEPTION_PUBLISH_COMMAND"

//热更新PP结果
const val PP_CTRL_RESPONSE_COMMAND = "PP_CTRL_RESPONSE_COMMAND"

//热更新LP结果
const val LP_CTRL_RESPONSE_COMMAND = "LP_CTRL_RESPONSE_COMMAND"

//热更新CMS结果
const val CMS_CTRL_RESPONSE_COMMAND = "CMS_CTRL_RESPONSE_COMMAND"

//PET->PAD 发送是否升级相机固件版本
const val PERCEPTION_CAMFIRMWARE_COMMAND = "PERCEPTION_CAMFIRMWARE_COMMAND"

//PET向pad发送目前机器奥比相机排布是否合理
const val PERCEPTION_CAMHUB_COMMAND = "PERCEPTION_CAMHUB_COMMAND"


const val SUBSCRIBE_CHANNEL =
    "$UI_COMMAND|" +
            "$NAVI_UI_COMMAND|" +
            "$TEACH_PATH|" +
            "$CLEAN_UI_COMMAND|" +
            "$OPT_POSE|" +
            "$UPDATE_POS|" +
            "$UPDATE_SUBMAPS|" +
            "$CURRENT_POINTCLOUD|" +
            "$PLAN_PATH_RESULT|" +
            "$CMS_UI_COMMAND|" +
            "$CTRL_COMMAND|" +
            "$SERVICE_CONTROL_COMMAND|" +
            "$SERVICE_CONTROL_UI_COMMAND|" +
            "$PP_CTRL_RESPONSE_COMMAND|" +
            "$LP_CTRL_RESPONSE_COMMAND|" +
            "$CMS_CTRL_RESPONSE_COMMAND|" +
            "$AVOIDDX_COMMAND|" +
            "$BOTTOM_POINTCLOUD|" +
            "$RECORD_IMAGE|" +
            "$LP_COMMAND|" +
            "$PERCEPTION_PUBLISH_COMMAND|"+
            "$PERCEPTION_CALIBRESULT_COMMAND|"+
            "$LOCINFO_COMMAND|"+
            "$PERCEPTION_CAMHUB_COMMAND|"+
            "$PERCEPTION_CAMFIRMWARE_COMMAND|"
