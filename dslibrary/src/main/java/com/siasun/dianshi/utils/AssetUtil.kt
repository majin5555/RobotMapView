package com.siasun.dianshi.utils

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

/**
 * Assets 资源读取工具
 *
 * 用于读取 dsLibrary 自带 assets 中的本地化数据（如各语言的 log_code.json）。
 * 日志码文件目录结构：assets/{lang}/log_code.json
 *
 * @author majin
 */
object AssetUtil {
    private const val TAG = "AssetUtil"

    /** 日志码支持的语言目录（与 assets 下语言目录保持一致） */
    private val LOG_LANGS = arrayOf("ch", "en", "es", "fr", "ja", "ko", "pl", "pt", "ru", "th")

    /** 默认语言目录 */
    private const val DEFAULT_LANG = "ch"

    /** 日志码文件名 */
    private const val LOG_CODE_FILE = "log_code.json"

    /**
     * 读取 assets 文件内容为字符串
     *
     * @param context  上下文
     * @param fileName assets 相对路径，如 "ch/log_code.json"
     * @return 文件内容；读取失败返回 null
     */
    fun readAssets(context: Context, fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            Log.e(TAG, "读取 assets 文件失败: $fileName", e)
            null
        }
    }

    /**
     * 根据系统语言加载日志码映射表（错误码 -> 文案）
     *
     * @param context 上下文
     * @return 映射表，解析失败返回空 Map
     */
    fun getLogMap(context: Context): MutableMap<String, Any> {
        return getLogMap(context, getLogLang())
    }

    /**
     * 按指定语言加载日志码映射表（错误码 -> 文案）
     *
     * 指定语言文件不存在时回退到默认语言 [DEFAULT_LANG]
     *
     * @param context 上下文
     * @param lang    语言目录，如 "ch"、"en"、"ja"
     * @return 映射表，解析失败返回空 Map
     */
    fun getLogMap(context: Context, lang: String): MutableMap<String, Any> {
        val fileName = "$lang/$LOG_CODE_FILE"
        val json = readAssets(context, fileName)
        if (json.isNullOrEmpty()) {
            if (lang != DEFAULT_LANG) {
                Log.w(TAG, "$fileName 不存在，回退到 $DEFAULT_LANG/$LOG_CODE_FILE")
                return getLogMap(context, DEFAULT_LANG)
            }
            return mutableMapOf()
        }
        return jsonToMap(json)
    }

    /**
     * 根据系统语言匹配 assets 语言目录
     *
     * @return 语言目录名，如 "ch"、"en"；不支持的语言回退 [DEFAULT_LANG]
     */
    fun getLogLang(): String {
        val lang = Locale.getDefault().language.lowercase(Locale.ROOT)
        return when {
            LOG_LANGS.contains(lang) -> lang
            else -> DEFAULT_LANG
        }
    }

    /**
     * 将 json 字符串解析为 Map（保持原始 value 类型）
     *
     * @param jsonString json 字符串
     * @return 解析结果；失败返回空 Map
     */
    fun jsonToMap(jsonString: String): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        return try {
            val json = JSONObject(jsonString)
            for (key in json.keys()) {
                map[key] = json.get(key)
            }
            map
        } catch (e: Exception) {
            Log.e(TAG, "json 解析失败", e)
            map
        }
    }
}
