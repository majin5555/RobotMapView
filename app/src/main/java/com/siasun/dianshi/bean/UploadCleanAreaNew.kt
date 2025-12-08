package com.siasun.dianshi.bean

import android.graphics.PointF
import java.util.Vector

/******************************************
 * 类描述：上传用的CleanAreaNew 去掉本地存储的字段
 *
 * @author: why
 * @time: 2025/8/6 11:57
 ******************************************/

data class UploadCleanAreaNew(
    var sub_name: String = "",
    var regId: Int = -1,
    var layer_id: Int = -1,
    var endPoint: Int = -1,
    var m_VertexPnt: Vector<PointNew> = Vector(),
    var areaStartPoint: PointF = PointF(),
    var areaPathType: Int = 0,
    var pathPlanInfo: PathPlanInfo? = null,
    var cleanShape: Int = 4,
    var routeType: Int = 0,
    var areaType: Int = 1
)