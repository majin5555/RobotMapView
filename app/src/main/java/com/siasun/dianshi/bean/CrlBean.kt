package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent


/**
 * 音频测试
 */
data class Mp3Bean(var sName: String, var pName: String)


/**
 * type 0热更新
 */
data class UpdateFile(
    var fileList: List<String>, var time: String?, var type: Int = 0
)

/**
 * type 1热更新 切换地图
 */
data class UpdateFileMany(
    var fileList: List<String>, var time: String?, var mapId: Int, var type: Int = 0
)


/**
 *手动清扫实体类
 */
data class ManualBeanCleanManual(val img: Int, val name: Int)


/**
 *解析更新文件答复
 */
data class UpdateFileResult(
    var id: String,
    val type: Int,
    val ret: Int,
    val mapId: Int,
    val errorFileList: MutableList<String>
)


data class FixAreaNum(
    val speedLevel: Float, val waterLevel: Int, val brushHeight: Int, val cleanSolution: Byte
)


/**
 * 日志解析
 */
data class LogBean(
    val err_code: String,
    val create_time: String,
    val err_level: Int,
) : LiveEvent


/**
 * 获取地图ID
 */
data class MapIdBean(var id: Int)

/**
 * 上传地图信息
 */
data class RequestSaveMap(var mapId: Int = 0, var mapName: String = "", var floorId: String = "")

/**
 * 删除地图
 */
data class DeleteMapIdBean(var mapId: Int)


/**
 * 切换地图
 */
data class SwitchMapBean(
    val mapId: Int,
    var x: Double,
    var y: Double,
    var theta: Double,
    var z: Double = 0.00,
    var roll: Double = 0.00,
    var pitch: Double = 0.00,
    //3d 模式 10：普通定位 11：拖拽定位
    //2d 为0
    var source: Int = 0,

    var sync: Boolean = true,
)


/**
 * 加载资源
 */
data class LoadResSuccess(var mapId: Int) : LiveEvent


data class LcmErrorBean(var msg: String) : LiveEvent

/**
 * 顶视扫描
 */
data class TopScanBean(var code: Int) : LiveEvent

data class UpdateBean(var apkName: String, var downloadPath: String) : LiveEvent

/**
 * 顶视扫描 创建地图步数
 */
data class TopScanStepsBean(var steps: Int) : LiveEvent

//data class DragLocationBean(
//    var upRCData: RCData? = null, var bottomRCData: RCData? = null
//) : LiveEvent


/**
 * @author: Majin
 * @desc: 地图使用的对象
 */
data class MapInfo(
    var mapId: Int = 0,
    var mapName: String = "",
    var mapWidth: Int = 0,
    var mapHeight: Int = 0,
    var floorId: String = "",
)


/**
 * 顶视扫描
 * 0 成功 1不在等待建图状态 2 激光无数据 3系统错误 4打开顶视相机失败
 */
data class ScanBean(var code: Int) : LiveEvent


/**
 * 组合任务解析类（新建组合任务页面）
 */
data class AllFloorTaskTasksBean(
    var mapName: String,
    var mapId: Int,
    val jobList: MutableList<SingleTaskBean>,
    var allSelect: Boolean = false
) : LiveEvent

data class SingleTaskBean(
    var id: Int, val name: String, val mapId: Int, var taskSelect: Boolean = false
) : LiveEvent


data class ItemBean(var mapId: Int, val taskID: Int) : LiveEvent

/**
 * 保存组合任务成功 刷新组合任务列表页面
 */
data class Fresh(var code: Int) : LiveEvent

/**
 * 解析组合任务列表
 * 保存组合任务 请求server
 */
data class CombinedTasksBean(
    var cycle: Int,
    var name: String,
    var taskId: Long,
    var type: Int,
    var jobList: MutableList<CombinedTasksItemBean>,
    var executionTimeBeans: MutableList<ExecutionTimeBean> = mutableListOf(),
    var finishTimeBeans: MutableList<ExecutionTimeBean> = mutableListOf()
) : LiveEvent

data class CombinedTasksItemBean(val mapId: Int, var id: Int, var name: String) : LiveEvent


/**
 * 下发组合任务
 */
data class RequestCombinedTask(
    var taskId: Long,
    var type: Int,
) : LiveEvent


/**
 * 区域管理 删除区域
 */
data class RequestDeleteArea(var layer_id: Int, var regId: Int) : LiveEvent

/**
 * 区域管理 查询区域
 */
data class RequestGetArea(var layer_id: Int) : LiveEvent


/**
 * 文件备份迁移结果
 */
class BackupMigrationBean(val result: Int) : LiveEvent

class ExecutionTimeBean(val time: String, val week: MutableList<String>) : LiveEvent

data class BlackBoxBean(val blackbox_path: String) : LiveEvent


///**
// * 电梯配置
// */
//data class ElevatorPointBean(
//    val name: String, val pstPark: PstParkBean, val gatePoint: GatePointBean
//)
//
///**
// *电梯外停靠点
// */
//data class GatePointBean(val x: Double, val y: Double, val theta: Double)
//
///**
// *电梯内停靠点
// */
//data class PstParkBean(val x: Double, val y: Double, val theta: Double)
//



/**
 * 多地图切换地图LCM回执
 */

data class SwitchMap(
    var mapId: Int,
    var x: Double,
    var y: Double,
    var theta: Double,
) : LiveEvent


data class TaskVerification(val is_used: Boolean, val tasks: MutableList<String>) : LiveEvent

/**
 * @Author: Mj
 * @Date: 2025/7/21
 * @Description:
 */
data class ElevatorBean(var id: Int, var elevator_id: String, var elevator_name: String) : LiveEvent


/**
 * 检查apk版本是否升级返回
 */
data class CheckApkBean(var apkName: String, var downloadPath: String) : LiveEvent

/**
 * 获取mrc05时间
 */
data class DateTimeBean(var current_time: String, var current_date: String, var time_stamp: Long) :
    LiveEvent

data class DispatchingSystemBean(var name: String, var type: String, val id: Int)