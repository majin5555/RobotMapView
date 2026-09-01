package com.siasun.dianshi.mapviewdemo.log

import android.content.Context
import com.siasun.dianshi.utils.AssetUtil

/**
 * 日志码多语言映射
 *
 * 根据系统语言从 dsLibrary 的 assets/{lang}/log_code.json 加载「错误码 -> 文案」映射，
 * 供 [com.siasun.dianshi.adapter.SystemLogAdapter] 将日志错误码翻译为可读文案。
 * 具体读取逻辑已迁移至 dsLibrary 的 [AssetUtil]。
 *
 * @author majin
 */
class LanguageData(private val context: Context) {

    /**
     * 获取多语言日志映射表（根据系统语言自动选择语言）
     */
    fun getLogMap(): MutableMap<String, Any> {
        return AssetUtil.getLogMap(context)
    }

    /**
     * 按指定语言获取日志码映射表
     */
    fun getLogMap(lang: String): MutableMap<String, Any> {
        return AssetUtil.getLogMap(context, lang)
    }

    /**
     * 读取assets本地json
     * @param fileName
     * @param context
     * @return
     */
    fun getJson(fileName: String?, context: Context): String {
        if (fileName.isNullOrEmpty()) return ""
        return AssetUtil.readAssets(context, fileName) ?: ""
    }

    fun jsonToMap(jsonString: String): MutableMap<String, Any> {
        return AssetUtil.jsonToMap(jsonString)
    }
}
