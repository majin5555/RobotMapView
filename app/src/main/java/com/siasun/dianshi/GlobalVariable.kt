package com.siasun.dianshi

/**
 * 全局变量
 */
object GlobalVariable {

    val KEY_CH ="ch"    //汉语
    val KEY_EN ="en"    //英语
    val KEY_TH ="th"    //泰语
    val KEY_RU ="ru"    //俄语
    val KEY_ES ="es"    //西班牙语
    val KEY_PT ="pt"    //葡萄牙语
    val KEY_KO ="ko"    //韩语
    val KEY_JA ="ja"    //日语
    val KEY_PL ="pl"    //波兰语

    /***1-起点到终点 3-双向通行*/

    var pathParam: Short = 1

    /**
     * 正在建图标记
     */
    var isCreatingMap: Boolean = false

    var EMERGENCY_STOP = 0 // 急停状态(0:急停未触发；1:急停触发)

    var WARM_CAR_RESET = 0 // 复位状态(0:未触发；1:触发)

    var BATTERY_STATE_VALUE = 0 //0：满电 1：正常 2：电量低 3：电量极低

    var SWITCHING_PROMPT_VALUE = 0 //  0: 切到手动;1：和控制台连接中断;2：切到自动

    var LASER_VALUE = 1 //  （0:关 1:开）

    var mFaultIParam0 = 0
    var mFaultIParam1 = 0
    var mFaultIParam2 = 0
    var mFaultIParam3 = 0
    var mFaultIParam4 = 0
    var mFaultIParam5 = 0
    var mFaultIParam6 = 0
    var mFaultIParam7 = 0
    var mFaultIParam8 = 0
    var mFaultIParam9 = 0
    var mFaultIParam10 = 0
    var mFaultIParam11 = 0
    var mFaultIParam12 = 0
    var mFaultIParam13 = 0
    var mFaultIParam14 = 0
    var mFaultIParam15 = 0

    var FAULT_INFORMATION = false

    var CHARGE_CHANGE_WATER_VALUE =
        0 // (1:车体申请一体机中2:申请成功,车体等待接收到红外对射信号3:对射接收成功,等待推杆伸咄 4:推杆伸出到位,加排水进行中5:车体重新对接一体机进行中6:车体换水完成 7:车体处于只充电状态8:车体处于加排水出现超的时

    var CHARGE_ERROR_VALUE = 0 //充电故障( 0未故障   1 故障)

    var LONG_TERM_NO_CHANGE_VALUE = 0 //数据长时间不变（0: 未触发；1: 触发）

    var DRAINAGE_BUCKET_FULL_VALUE = 0 //一体机排水桶已满,请及时清理（0: 未触发；1: 触发）

    var DATA_EXCEPTION_VALUE = 0 //数据异常（0: 未触发；1: 触发）

    var ELECTRODE_HIGH_TEMPERATURE_VALUE = 0 // 电极高温（0: 未触发；1: 触发）

    var SEWAGE_TANK_FULL_VALUE = 0 //补给站污水箱满（0: 未触发；1: 触发）

    var CLEAN_WATER_EMPTY_VALUE = 0 //补给站清水箱空（0: 未触发；1: 触发）

    var REFLECTIVE_PLATE_RECOGNITION_FAILED_VALUE = 0 //补给站清水箱空（0: 未触发；1: 触发）

    var CLEANING_AREA_ID = -1 //当前清扫区域ID CMS 实时上报

    var DRAIN_VALVE = -1 //排污阀状态(0:关 1:开)


    var LOCATION_STATE = -1 //导航给pad定位状态

    var LOCATION_TYPE = -1 //导航给pad定位方式


    var MANUAL_SPEED_LEFT = 0.0 //左轮手动当前速度

    var MANUAL_SPEED_RIGHT = 0.0 //右轮手动当前速度


    var CURRENT_CLEAN_MOOD = 0 //当前清扫模式

    var CURRENT_CLEAN_LEVEL = 0 //当前清水箱水位0-50-100

    var CURRENT_SEWAGE_LEVEL = 0 //当前污水箱水位0-50-100

    var CURRENT_AGENT_LEVEL = 0 //节水量液位


    var WATER_LEVEL_DEFAULT = 35 //洒水量默认值35 （所有车型都是0-100）

    var SPEED_DEFAULT = 0.6f //速度默认值

    //手动推杆状态（0：空 1：前进 2：后退 ）
    var MANUAL_PUSH_ROD_STATUS = 0

    //与导航心跳开关
    var SEND_NAVI_HEART = false

    /**
     * 车体运行状态
     * 0：手动控制；1：任务准备中；2:任务准备完成；3:自动运行;4:空状态
     */
    var AGV_STATE = 0

    /**
     * (0:正常状态；1: 用户暂停）
     */
    var AGV_RUN_STATE = 0

    var CLEAN_WATER_LEVEL_STATE = -1 //（0:未加水；1:加水中）

    var SEWAGE_WATER_LEVEL_STATE = -1 //（0:未排水；1:排水中）

    //充电状态:  0-未充电, 1-充电中
    var BATTERY_STATE = 0
    var BATTERY_LEVEL = 0 //电量


    var LOCATION_VALVE = 0 //定位信息 （0:定位失败；1:定位成功）

    var TCS_STATE = 0 //  0 在线 1 逻辑在线 2 物理离线


    var ROD_LEVEL = 0 //当前推杆值

    var WATER_CURRENT_LEVEL = 0 //当前洒水量值

    var LP_VERSION = ""

    var CREATE_TYPE = ""

    var LANGUAGE = ""



}