package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent


/**
 * Created by mj
 *
 */
data class CleanAreaRootNew(var cleanAreas: MutableList<CleanAreaNew> = mutableListOf()) : LiveEvent


/**
 *
 * 转换CleanAreaRootNew
 */
fun CleanAreaRootNew.toUpload(): UploadCleanAreaRoot {
    val uploadAreas = cleanAreas.map { area ->
        UploadCleanAreaNew(
            sub_name = area.sub_name,
            regId = area.regId,
            layer_id = area.layer_id,
            endPoint = area.endPoint,
            m_VertexPnt = area.m_VertexPnt,
            areaStartPoint = area.areaStartPoint,
            areaPathType = area.areaPathType,
            pathPlanInfo = area.pathPlanInfo,
            cleanShape = area.cleanShape,
            routeType = area.routeType,
            areaType = area.areaType
        )
    }
    return UploadCleanAreaRoot(uploadAreas)
}