package com.siasun.dianshi.network.request

import VirtualWallNew

/******************************************
 * 类描述：
 *
 * @author: why
 * @time: 2025/8/5 09:38
 ******************************************/

/**
 * 请求参数为layer_id的通用请求体
 */
data class RequestCommonMapId(val layer_id: Int)

/**
 *  区域相关
 */
//data class RequestSaveArea(val layer_id: Int, val pad_areas: UploadCleanAreaRoot)
data class RequestDeletePadArea(val layer_id: Int, val reg_id: Int)

/**
 * 站点相关
 */
//data class RequestCmsStation(val layer_id: Int, val cms_stations: MutableList<CmsStation>)
//data class RequestMachineStation(val integrated_machine: MutableList<MachineStation>)

/**
 * 虚拟墙相关
 */
data class RequestSaveVirtualWall(val layer_id: Int, val virtual_wall: VirtualWallNew?)

/**
 * 电梯相关
 */
data class RequestSaveElevator(var elevator_id: String, var elevator_name: String)
data class RequestUpdateElevator(val elevator_id: String, val elevator_name: String, val id: Int)
data class RequestDeleteElevator(val id: Int)
//data class RequestCmsElevator(val layer_id: Int, val cms_elevator: ElevatorRoot)

/**
 * 特殊区域相关
 */
data class SaveZoneRequest(
    var sub_name: String,
    var regId: Int,
    var routeType: Int,
    var m_VertexPnt: MutableList<AvoidingObstaclePointBean>
)

data class AvoidingObstaclePointBean(var X: Float, var Y: Float)
data class ItemSpecialArea(
    var sub_name: String,
    var regId: Int,
    var routeType: Int,
    var m_VertexPnt: MutableList<AvoidingObstaclePointBean>
)

data class RequestSaveSpecialArea(val map_id: Int, val regions: MutableList<ItemSpecialArea>)
data class RequestGetSpecialArea(val map_id: Int, val areaType: Int)

/**
 * 过门区相关
 */
//data class RequestSaveDoors(val layer_id: Int, val cms_doors: MutableList<AreasDoor>)

/**
 * 保存混行区
 */
//data class RequestSaveCmsWorkArea(
//    val layer_id: Int,
//    val cms_work_areas_list: UpLoadCmsWorkAreasListRoot
//)

/**
 * 初始定位点
 */
//data class RequestInitPose(
//    val layer_id: Int, val init_pose: InitPoseRoot
//)

/**
 * 热加载
 */
data class RequestHotReload(
    val layer_id: Int?, val file_name: MutableList<String>
)


//版本号查询
data class RequestCheckApk(var version_code: Int, var apk_type: String)

//获取日志
data class RequestGetLog(val type_list: MutableList<Int>)

///**
// * 获取区域
// */
//data class RequestGetRegions(val map_id: Int, val routeType: Int)
//
///**
// * 获取当前楼层当前任务的区域
// */
//data class RequestCurrentJobsAreas(var sub_region_layer: String, var mTaskFlag: String)
//
//
///**
// * 任务查询
// */
//data class RequestGetPadJobs(var mTaskFlag: Int)
//
///**
// * 任务删除
// */
//data class RequestDeletePadJobs(var mTaskFlag: Int)
//
///**
// * 清扫模式删除
// */
//data class RequestDeletePadCleanMode(val customCleanMode: Int)
