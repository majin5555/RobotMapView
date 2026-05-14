package com.siasun.dianshi.bean

import com.siasun.dianshi.mapviewdemo.utils.GsonUtil

/******************************************
 * 类描述：
 *
 * @author: why
 * @time: 2026/5/14 08:43
 ******************************************/

data class LcmStrToPPBean(
    var strTo: String,
    var CleanPathPlanParam: CleanPathPlanParam,
)

data class CleanPathPlanParam(var m_iCleanAreaEdgeType: Int,var m_bIsAccountCleanStartPoint :Int = 0)

