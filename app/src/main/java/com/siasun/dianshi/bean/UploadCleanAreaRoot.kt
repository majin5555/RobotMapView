package com.siasun.dianshi.bean

/******************************************
 * 类描述：上传用的CleanAreaNew 去掉本地存储的字段
 *
 * @author: why
 * @time: 2025/8/6 12:00
 ******************************************/

data class UploadCleanAreaRoot(
 var cleanAreas: List<UploadCleanAreaNew> = emptyList()
)