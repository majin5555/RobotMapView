package com.siasun.dianshi.bean

import java.util.Vector

/**
 * Created by zy on 2023/06/15 at 08:43
 *
 * @description ：混行区配置
 * 类描述：上传用的WorkAreasNew 去掉本地存储的字段
 */
data class UpLoadWorkAreasNew(
    var id: String? = null, //混行区id
    var floor: Int = 0, //楼层号
    var ip: String? = null, //混行区ip
    var port: Int = 0, //混行区端口
    var name: String? = null, //混行区名称
    var tc: String? = null, //混行区tc
    var areaVertexPnt: MutableList<PointNew> = Vector<PointNew>(), //混行区点
    var passPointsList: MutableList<PassPoints> = ArrayList() //混行区过渡点

)