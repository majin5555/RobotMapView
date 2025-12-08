package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent

/******************************************
 * 类描述：
 *
 * @author: why
 * @time: 2025/8/15 15:14
 ******************************************/

data class CmsWorkAreasListRoot(var workAreasList: MutableList<WorkAreasNew> = mutableListOf()) :
    LiveEvent

/**
 *
 * 转换WorkAreasNew
 */
fun CmsWorkAreasListRoot.toUpload(): UpLoadCmsWorkAreasListRoot {
    val uploadAreas = workAreasList.map { area ->
        UpLoadWorkAreasNew(
            id = area.id,
            floor = area.floor,
            ip = area.ip,
            port = area.port,
            name = area.name,
            tc = area.tc,
            areaVertexPnt = area.areaVertexPnt,
            passPointsList = area.passPointsList
        )
    }
    return UpLoadCmsWorkAreasListRoot(uploadAreas)
}