package com.siasun.dianshi.mapviewdemo

/**
 * @Author: MJ
 * @Date: 2026/1/31
 * @Description: LCM相关常量管理类
 */

/**
 * -------------------Pad To Server Command Id Start--------------------
 */


// Server发送心跳
const val SERVER_HEART: Byte = 11

/**
 * -------------------Server To Pad Command Id End----------------------
 */

enum class CarBody(val value: Int) {
    CLEAN_UI_COMMAND_SENSOR_INFORMATION(1),//车体传感器信息
    CLEAN_UI_COMMAND_CONSUMABLES(2),//洗地机器人上传易损件信息
    FAILURE_BACK_STATION(9),//故障回站
    INTEGRATED_MACHINE_INTERACTION(8),//洗地机器人一体机交互信息
    CHARGE_CHANGE_WATER_PAGE_STATE(10),//洗地机器人自动状态下弹出充电换水页面
    OCCUPYING_EQUIPMENT(11),//是否占用1体机

    UI_COMMAND_ROBOT_STATE(1),//车体状态信息
    UI_COMMAND_ROBOT_AGV_EVENT(5),//车体事件
    UI_COMMAND_ROBOT_MAG_SWITCH_STATE(8),//车体上报设置界面的状态给pad
    UI_COMMAND_ROBOT_AGV_VERSION(9),//车体上报AGV版本
    UI_COMMAND_ROBOT_AGV_SHUTDOWN(12),//车体上报设置界面的状态给pad
    UI_COMMAND_ROBOT_AGV_BATTERY_ERROR(14),//电池错误
    UI_COMMAND_PLAY_MUSIC(13),//播放音乐
    UI_COMMAND_CROSS_FLOOR_STAGE(30)//跨楼层状态

}

enum class CmsBody(val value: Int) {
    CMS_UI_COMMAND_CMS_VERSION(4),//CMS版本
    CMS_UI_COMMAND_TASK_STATE(6),//任务状态
    CMS_UI_COMMAND_AREA_STATE(8)//区域状态
}

enum class ServiceBody(val value: Int) {
    SERVICE_CONTROL_UI_COMMAND_RECEIVE_VERSION(1),//接收Server版本
    SERVICE_CONTROL_UI_COMMAND_RECEIVE_SERVER_INFO(4),//接收Server心跳信息
    SERVICE_CONTROL_UI_COMMAND_RECEIVE_SCHEDULED_TASK(8),//接收定时任务
    SERVICE_CONTROL_UI_COMMAND_RECEIVE_SWITCH_MAP(9),//接收Server切换地图
    SERVICE_CONTROL_UI_COMMAND_RECEIVE_SCHEDULED_TASK_REMINDER(10),//接收Server心跳信息
    SERVICE_CONTROL_UI_COMMAND_RECEIVE_OTA_UPDATE(11),//接收版本更新提示
    SERVICE_CONTROL_UI_COMMAND_RECEIVE_SYNC_PAD_JOBS(20),//接收同步padJobs.json
    RECEIVE_REGENERATE_PNG_SUCCESS(22),//接收地图PNG生成成功
    SERVICE_CONTROL_UI_COMMAND_RECEIVE_LOG_IMAGE(23),//更新titleBar小三角颜色
}

enum class NaviBody(val value: Int) {
    NAVI_UI_COMMAND_NAVIGATION_HEARTBEAT(51),//导航心跳
    NAVI_UI_COMMAND_REMOVE_NOISE_RESULT(52),//删除噪点结果
    NAVI_UI_COMMAND_NAV_VERSION(60),//版本
    NAVI_UI_COMMAND_CALIBRATION_DATA(61),//标定结果数据
    NAVI_UI_COMMAND_CALIBRATION_RESULT(62),//标定结果
    NAVI_UI_COMMAND_LOAD_EXTENDED_MAP_DATA_RESULT(63),//导航加载扩展地图data.pb文件结果

    NAVI_UI_COMMAND_LOAD_TOP_SCAN_STATE(64),//接收顶视状态
    NAVI_UI_COMMAND_LOAD_SCAN_STATE(65),//扫描新环境状态
    NAVI_UI_COMMAND_FINISH_WRITE_SLAM(66),//导航写完slam区域通知pad
    NAVI_UI_COMMAND_POSITING_AREA(67),//接收区域定位数据
    NAVI_UI_COMMAND_CONSTRAINT_NODE(69),//接收约束节点数据
    NAVI_UI_COMMAND_CONSTRAINT_CONSTRAINT_NODE_RESULT(70),//接收约束节点匹配结果
    NAVI_UI_COMMAND_CONFIGURATION_PARAMETERS(71),//接收配置参数
    NAVI_UI_COMMAND_CONFIGURATION_PARAMETERS_RESULT(72),//配置参数结果

    NAVI_UI_COMMAND_REFLECTOR_MAP_DATA_RESULT(74),//导航向pad发送反光板地图数据

    NAVI_UI_COMMAND_HIGHLIGHT_POINT_RESULT(75),//导航向pad发送高亮物体坐标

    NAVI_UI_COMMAND_CREATE_REFLECTOR_MAP_RESULT(76),//导航向pad发送生成反光板地图

    NAVI_UI_COMMAND_MAP_UNDERGO_SIGNIFICANT_CHANGES(77), //导航向pad发送 更新/拓展地图后发生较大变化

}

/**
 * 0：无任务
 * 1：有任务 - 正常显示CMS推送任务相关数据
 * 2：正在计算中（Pad显示） - CMS正在计算任务执行相关数据
 * 3：回站中
 * 4:等待点等待
 */
enum class TaskState {
    NO_TASK, HAVE_TASK, UNDER_CALCULATION, RETURNING_TO_THE_STATION, WAIT_POINT
}

/**
 * ------------------LCM常量 Start----------------
 */
const val KEY_SCHEDULED_TASK_REMINDER = "scheduled_task_reminder"
const val KEY_TASK_STATE = "key_task_state"
const val KEY_DRAIN_VALVE = "drain_valve"
const val RESULT_PP_RELOAD_FILE = "result_pp_reload_file"
const val RESULT_CMS_RELOAD_FILE = "result_cms_reload_file"
const val RESULT_LP_RELOAD_FILE = "result_lp_reload_file"
const val KEY_UPDATE_POSITIONING_AREA = "key_update_positioning_area"
const val KEY_SYNC_PAD_JOBS = "key_sync_pad_jobs"
const val KEY_CLEAN_WATER = "key_clean_water"
const val KEY_SEWAGE_WATER = "key_sewage_water"
const val KEY_DETERGENT_WATER = "key_detergent_water"
const val KEY_BATTERY_VALVE = "key_battery_valve"
const val KEY_LOCATION = "key_location"
const val KEY_AGV_STATE = "key_agv_state"
const val KEY_TCS_STATE = "key_tcs_state"
const val KEY_VEHICLE_SPEED_GEAR = "key_vehicle_speed_gear"
const val KEY_WARM_STOP_STATE = "key_warm_stop_state"
const val KEY_WARM_CAR_RESET = "key_warm_car_reset"
const val KEY_AGV_SPEED = "key_agv_speed"
const val KEY_OBSTACLE_DETECTED = "key_obstacle_detected"
const val KEY_BATTERY_STATE_VALUE = "key_battery_state_value"
const val KEY_CHARGE_STATE = "key_charge_state"
const val KEY_AGV_COORDINATE = "key_agv_coordinate"
const val KEY_AGV_RUN_STATE = "key_agv_run_state"
const val KEY_TEACH_STATE = "key_teach_state"
const val KEY_AGV_INIT_STATE = "key_agv_init_state"
const val KEY_SWITCHING_PROMPT = "key_switching_prompt"
const val KEY_AGV_EVENT = "key_agv_event"
const val KEY_CONSUMABLES = "key_consumables"
const val KEY_SONAR_VALUE = "key_sonar_value"
const val KEY_LASER_VALUE = "key_laser_value"
const val KEY_CHARGE_SWITCH_VALUE = "key_charge_switch_value"
const val KEY_CURRENT_PLS_VALUE = "key_current_pls_value"
const val KEY_AGV_SHUTDOWN_VALUE = "key_agv_shutdown_value"
const val KEY_MRC_RAM_VALUE = "key_mrc_ram_value"
const val KEY_POSITING_AREA_VALUE = "key_positing_area_value"
const val KEY_FINISH_WRITE_SLAM_VALUE = "key_finish_write_slam_value"
const val KEY_NAV_TOP_SCAN_STEPS_VALUE = "key_nav_top_scan_steps_value"
const val KEY_NAV_LOAD_SCAN_STATE_VALUE = "key_nav_load_scan_state_value"
const val KEY_NAV_LOAD_TOP_SCAN_STATE_VALUE = "key_nav_load_top_scan_state_value"
const val KEY_FRESH_TOP_ALARM_INFO = "key_fresh_top_alarm_info"
const val KEY_FRESH_TOP_ALARM_INFO_NORMAL = "key_fresh_top_alarm_info_normal"
const val KEY_FINISH_CLEAN_AREA_ID = "key_finish_clean_area_id"
const val KEY_CLEANING_ID = "key_cleaning_id"
const val KEY_UPDATE_SUB_MAPS = "key_update_sub_maps"
const val KEY_UPDATE_POS = "key_update_pos"
const val KEY_CURRENT_POINT_CLOUD = "key_current_point_cloud"
const val KEY_BOTTOM_CURRENT_POINT_CLOUD = "key_bottom_current_point_cloud"
const val KEY_TEACH_PATH = "key_teach_path"
const val KEY_OPT_POSE = "key_opt_pose"
const val KEY_REMOVE_NOISE_RESULT = "key_remove_noise_result"
const val KEY_DOWN_MAP = "key_down_map"
const val KEY_UPDATE_MAP = "key_update_map"
const val KEY_CALIBRATION_DATA = "key_calibration_data"
const val KEY_CALIBRATION_RESULT = "key_calibration_result"
const val KEY_EXTEND_LOAD_SUB_MAP = "key_extend_load_sub_map"
const val KEY_NAV_HEARTBEAT_STATE = "key_nav_heartbeat_state"
const val KEY_UP_LOAD_VW_FILES = "key_up_load_vw_files"
const val KEY_DOWN_LOAD_VW_FILES = "key_down_load_vw_files"
const val KEY_UPDATE_VIRTUAL_WALL = "key_update_virtual_wall"
const val KEY_UPDATE_AREA = "key_update_area"
const val KEY_UP_LOAD_TEMPLATE = "key_up_load_template"
const val KEY_UPDATE_VIEW_INIT_POST = "key_update_view_init_post"
const val KEY_UPDATE_TASK_CONTENT = "key_update_task_content"
//const val KEY_UPDATE_CLEAN_AUTO_ACTIVITY_CURRENT_TASK =
//    "key_update_clean_auto_activity_current_task"
//const val KEY_UPDATE_PAD_JOBS = "key_update_pad_jobs"

//const val KEY_EDIT_TASK = "key_edit_task"
//const val KEY_TASK_LIST_DELETE_OR_EDIT = "key_task_list_delete_or_edit"
const val KEY_SCHEDULED_TASK = "key_scheduled_task"
const val KEY_CMS_VERSION = "key_cms_version"
const val KEY_AGV_VERSION = "key_agv_version"
const val KEY_NAV_VERSION = "key_nav_version"
const val KEY_PP_VERSION = "key_pp_version"
const val KEY_SERVER_VERSION = "key_server_version"
const val KEY_PET_VERSION = "key_pet_version"
const val KEY_LOCATION_DRAG = "key_location_drag"
const val KEY_FAILURE_BACK_STATION = "key_failure_back_station"
const val KEY_UPDATE_CUSTOM_CLEANING_MODE = "key_update_custom_cleaning_mode"
const val KEY_PROMPT_SOUND = "key_prompt_sound"
const val KEY_FAILURE_BACK_STATION_MANUAL = "key_failure_back_station_manual"
const val KEY_FAILURE_BACK_STATION_AUTO = "key_failure_back_station_auto"
const val KEY_TOP_VIEW_VERSION = "key_top_view_version"
const val KEY_CHARGE_WATER = "key_charge_water"
const val KEY_CHARGE_ERROR = "key_charge_error"
const val KEY_LONG_TERM_NO_CHANGE = "key_long_term_no_change"
const val KEY_DRAINAGE_BUCKET_FULL = "key_drainage_bucket_full"
const val KEY_DATA_EXCEPTION = "key_data_exception"
const val KEY_ELECTRODE_HIGH_TEMPERATURE = "key_electrode_high_temperature"
const val KEY_SEWAGE_TANK_FULL = "key_sewage_tank_full"
const val KEY_CLEAN_WATER_EMPTY = "key_clean_water_empty"
const val KEY_REFLECTIVE_PLATE_RECOGNITION_FAILED = "key_reflective_plate_recognition_failed"
const val KEY_CHARGE_CHANGE_WATER_PAGE_VALUE = "key_charge_change_water_page_value"
const val KEY_DOWN_LOAD_CMS_CONFIG_FILE = "key_down_load_cms_config_file"
const val KEY_CAMERA_CALIBRATION_RESULT = "key_camera_calibration_result"
const val KEY_UPDATE_CMS_CONFIG_FILE = "key_update_cms_config_file"
const val KEY_UPDATE_PAD_AREAS_FILE = "key_update_pad_areas_file"
const val KEY_UPDATE_PAD_JOBS_FILE = "key_update_pad_jobs_file"
const val KEY_UPDATE_INIT_POSE_FILE = "key_update_init_pose_file"
const val KEY_UPDATE_WORLD_PAD_FILE = "key_update_world_pad_file"
const val KEY_UPDATE_MERGED_POSE_FILE = "key_update_merged_pose_file"
const val KEY_OCCUPYING_EQUIPMENT_VALUE = "key_occupying_equipment_value"
const val KEY_PET_CAMFIRMWARE = "key_pet_camfirmware"
const val KEY_LOC_INFO_COMMAND_STATE = "key_loc_info_command_state"
const val KEY_LOC_INFO_COMMAND_TYPE = "key_loc_info_command_type"
const val KEY_PET_CAMERA_USB_REASONABLE = "key_pet_camera_usb_reasonable"
const val KEY_UPDATE_PLAN_PATH_RESULT = "key_update_plan_path_result"
const val KEY_UPDATE_APK = "key_update_apk"
const val KEY_UPDATE_SCREEN_TIME = "key_update_screen_time"
const val KEY_UPDATE_LOG = "key_update_log"
const val CREATE_MAP_DESTROY = "create_map_destroy"

//路径管理  三个模式 1 士教 2 自动生成路径 3 自定义
const val KEY_MODE: String = "KEY_MODE"

//设置界面
//标准洒水量
const val KEY_STANDARD_WATERLEVEL: String = "KEY_STANDARD_WATERLEVEL"

//标准扫底盘下降高度
const val KEY_STANDARD_BRUSHHEIGHT: String = "KEY_STANDARD_BRUSHHEIGHT"

//重压洒水量
const val KEY_HEAVY_PRESS_WATERLEVEL: String = "KEY_HEAVY_PRESS_WATERLEVEL"

//重压底盘下降高度
const val KEY_HEAVY_PRESS_BRUSHHEIGHT: String = "KEY_HEAVY_PRESS_BRUSHHEIGHT"

//干扫刷盘下降高度
const val KEY_DRY_SWEEP_WATERLEVEL: String = "KEY_DRY_SWEEP_WATERLEVEL"

//用户类型 true 高级 false 普通 用户类型的属性名称，如果是true，则是管理员，否则是普通用户
const val KEY_USER_TYPE: String = "KEY_USER_TYPE"

//修改区域下的数值
const val KEY_FIX_AREA_NUM: String = "KEY_FIX_AREA_NUM"

//当前的手动模式
//const val KEY_MANUAL_CURRENT_MODE: String = "KEY_MANUAL_CURRENT_MODE"

//当前的手动模式名称
const val KEY_MANUAL_CURRENT_MODE_NAME: String = "KEY_MANUAL_CURRENT_MODE_NAME"

// 清洗剂开关
const val KEY_DETERGENT: String = "KEY_DETERGENT"

//pad 播放提示音
const val KEY_PAD_PLAY_MP3: String = "KEY_PAD_PLAY_MP3"

//屏幕保护
const val KEY_SCREEN_SAVER: String = "KEY_SCREEN_SAVER"

//pad 播放清扫音乐
const val KEY_PAD_PLAY_CLEAN_MP3: String = "KEY_PAD_PLAY_CLEAN_MP3"

//pad 更新地图
const val KEY_UPDATE_MODE: String = "KEY_UPDATE_MODE"
const val KEY_GO_POSITIONING_AREA_ACTIVITY: String = "key_go_positioning_area_activity"

//高级用户密码
const val KEY_ADVANCED_USER_PASS: String = "KEY_ADVANCED_USER_PASS"

//普通用户密码
const val KEY_ORDINARY_USER_PASS: String = "KEY_ORDINARY_USER_PASS"

//屏幕保护空闲时间
const val KEY_SCREEN_SAVER_FREE_TIME: String = "KEY_SCREEN_SAVER_FREE_TIME"

//下发任务 屏幕延时时间
const val KEY_SCREEN_SAVER_DELAY_TIME: String = "KEY_SCREEN_SAVER_DELAY_TIME"

//日志详情 -1全部日志 2 错误日志
const val KEY_LOG_DETAIL: String = "KEY_LOG_DETAIL"

//任务列表的添加任务->新建任务页面
//const val KEY_TASK_MODE: String = "KEY_TASK_MODE"
//
////是否是新建任务
const val IS_CREATE_TASK: String = "IS_CREATE_TASK"

//是否开始执行
//const val EXECUTE: String = "EXECUTE"
//当前的手动模式
const val KEY_MANUAL_CURRENT_MODE: String = "KEY_MANUAL_CURRENT_MODE"

//当前执行的任务
const val CURRENTTASK: String = "CURRENTTASK"
const val KEY_TASKLIST: String = "KEY_TASKLIST"

const val KEY_TASK: String = "KEY_TASK"

const val KEY_CROSS_FLOOR_STAGE = "key_cross_floor_stage"

//切换地图结果
const val KEY_SWITCH_MAP: String = "key_switch_map"

const val KEY_JUMP_SYNC_FILES: String = "key_jump_sync_files"

const val KEY_JUMP_CREATE_TASK: String = "key_jump_create_task"

const val KEY_JUMP_MNG_ROUTE: String = "key_jump_mng_route"

const val KEY_JUMP_MNG_AREA: String = "key_jump_mng_area"

const val KEY_JUMP_AUTO_GENERATE_PATH: String = "key_jump_auto_generate_path"

const val KEY_JUMP_CUSTOM_PATH: String = "key_jump_custom_path"

const val KEY_JUMP_AREA: String = "key_jump_area"

const val KEY_JUMP_MIX_AREA: String = "key_jump_mix_area"

const val KEY_JUMP_ELEVATOR: String = "key_jump_mix_area"

const val KEY_JUMP_EDIT_ENVIRONMENT: String = "key_jump_edit_environment"

const val KEY_JUMP_MNG_ENVIRONMENT: String = "key_jump_mng_environment"

const val KEY_JUMP_VIRTUAL_WALL: String = "key_jump_virtual_wall"

const val KEY_JUMP_POSITIONING_AREA: String = "key_jump_positioning_area"

const val KEY_JUMP_MNG_ELEVATOR: String = "key_jump_mng_elevator"

const val KEY_NEXT_CLEANING_AREA_ID = "key_next_cleaning_area_id"
const val KEY_CLEANING_LAYER = "key_cleaning_layer"

const val KEY_CONSTRAINT_NODE = "key_constraint_node"
const val KEY_CONSTRAINT_CONSTRAINT_NODE_RESULT = "key_constraint_constraint_node_result"
const val KEY_CONFIGURATION_PARAMETERS = "key_configuration_parameters"
const val KEY_CONFIGURATION_PARAMETERS_RESULT = "key_configuration_parameters_result"

//导航返回反光板地图数据
const val KEY_REFLECT_MAP_RESULT = "key_reflect_map_result"

//导航返回高亮物体
const val KEY_HIGHLIGHT_RESULT = "key_highlight_result"

//导航返回生成反光板地图
const val KET_CREATE_REFLECT_MAP_RESULT = "ket_create_reflect_map_result"

//导航返回 更新/扩展地图后地图发生较大变化
const val KET_MAP_UNDERGO_SIGNIFICANT_CHANGES_RESULT = "ket_map_undergo_significant_changes_result"

/**
 * ------------------LCM常量 End----------------
 */

object RunningState {
    var CURRENT_TASK_STATE: TaskState = TaskState.NO_TASK
}

/**
 * 下载地图  1环境预览  2 扫描新环境 3 环境扩展 4 去除噪点
 */
const val PREVIEW_MAP = 1
const val CREATE_MAP = 2
const val EXTEND_MAP = 3
const val REMOVE_NOISE_MAP = 4


// 全局路径规划
var GLOBAL_PATH_PLAN = 1

// 清扫路径规划
var CLEAN_PATH_PLAN = 2

// 示教路径规划
var TEACH_PATH_PLAN = 3
var PP_VERSION = 100 //PP版本号


var PATH_MODE = 0
var PATH_LINE = 0
var PATH_BEZIER = 1


