package com.siasun.dianshi.bean

/******************************************
 * 类描述：类描述：上传用的WorkAreasNew 去掉本地存储的字段
 *
 * @author: why
 * @time: 2025/8/15 15:10
 ******************************************/

data class UpLoadCmsWorkAreasListRoot(
    var workAreasList: List<UpLoadWorkAreasNew> = emptyList()
)