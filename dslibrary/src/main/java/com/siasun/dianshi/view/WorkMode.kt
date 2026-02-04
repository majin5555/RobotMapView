package com.siasun.dianshi.view

// 工作模式枚举
enum class WorkMode {
    MODE_SHOW_MAP,         // 移动地图模式
    MODE_VIRTUAL_WALL_ADD, // 创建虚拟墙模式
    MODE_VIRTUAL_WALL_EDIT,// 编辑虚拟墙模式
    MODE_VIRTUAL_WALL_TYPE_EDIT,// 编辑虚拟墙类型模式
    MODE_VIRTUAL_WALL_DELETE, // 删除虚拟墙模式
    MODE_CMS_STATION_EDIT,  // 修改避让点模式
    MODE_CMS_STATION_DELETE, // 删除避让点模式
    MODE_ELEVATOR_EDIT,    // 编辑乘梯点模式
    MODE_ELEVATOR_DELETE,  // 删除乘梯点模式
    MODE_MACHINE_STATION_EDIT,  // 编辑充电站模式
    MODE_MACHINE_STATION_DELETE, // 删除充电站模式
    MODE_REMOVE_NOISE,      // 擦除噪点模式
    MODE_POSITING_AREA_ADD, // 创建定位区域模式
    MODE_POSITING_AREA_EDIT, // 编辑定位区域模式
    MODE_POSITING_AREA_DELETE, // 删除定位区域模式
    MODE_CLEAN_AREA_EDIT, // 编辑清扫区域模式
    MODE_CLEAN_AREA_ADD, // 创建清扫区域模式
    MODE_SP_AREA_EDIT, // 编辑特殊区域模式
    MODE_SP_AREA_ADD, // 创建特殊区域模式
    MODE_MIX_AREA_ADD, // 创建混行区域模式
    MODE_MIX_AREA_EDIT, // 编辑混行区域模式
    MODE_PATH_EDIT, // 编辑路线模式
    MODE_PATH_MERGE, // 合并路线模式
    MODE_PATH_DELETE, // 删除路线模式
    MODE_PATH_DELETE_MULTIPLE, // 删除多条路线模式
    MODE_PATH_CONVERT_TO_LINE, // 曲线转直线模式 //暂时没有
    MODE_PATH_NODE_ATTR_EDIT, // 节点属性编辑模式
    MODE_PATH_SEGMENT_ATTR_EDIT, // 路段属性编辑模式
    MODE_PATH_CREATE, // 创建路线模式
    MODE_CROSS_DOOR_EDIT, // 编辑过门模式
    MODE_CROSS_DOOR_DELETE,// 删除过门模式
    MODE_DRAG_POSITION,// 拖拽定位模式
    MODE_CREATE_MAP,       // 创建地图模式
    MODE_EXTEND_MAP_ADD_REGION, // 扩展地图增加区域模式
}