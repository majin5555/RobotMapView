package com.siasun.dianshi.mapviewdemo.utils

import android.content.Context
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.io.IOException

/******************************************
 * 类描述：
 *
 * @author: why
 * @time: 2025/8/15 14:25
 ******************************************/
object GsonUtil {
    private var gson: Gson? = null

    init {
        if (gson == null) {
            gson = Gson()
        }
    }

    /**
     * 转成json
     *
     * @param object
     * @return
     */
    fun gsonString(`object`: Any?): String? {
        var gsonString: String? = null
        if (gson != null) {
            gsonString = gson!!.toJson(`object`)
        }
        return gsonString
    }

    fun loadJSONFromAsset(context: Context, fileName: String?): String? {
        var json: String? = null
        json = try {
            val `is` = context.assets.open(fileName!!)
            val size = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            String(buffer)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }

    fun <T> assetJsonToBean(context: Context, assetName: String?, cls: Class<T>?): T {
        val json = loadJSONFromAsset(context, assetName)
        return gsonToBean(json, cls)!!
    }

    /**
     * 转成bean
     *
     * @param gsonString
     * @param cls
     * @return
     */
    fun <T> gsonToBean(gsonString: String?, cls: Class<T>?): T? {
        var t: T? = null
        if (gson != null) {
            t = gson!!.fromJson(gsonString, cls)
        }
        return t
    }

    /**
     * 转成list
     * 泛型在编译期类型被擦除导致报错
     *
     * @param gsonString
     * @param cls
     * @return
     */
    fun <T> gsonToList(gsonString: String?, cls: Class<T>?): MutableList<T>? {
        var list: MutableList<T>? = null
        if (gson != null) {
            list = gson!!.fromJson(gsonString, object : TypeToken<MutableList<T>?>() {}.type)
        }
        return list
    }

    /**
     * 转成list
     * 解决泛型问题
     *
     * @param json
     * @param cls
     * @param <T>
     * @return
    </T> */
    fun <T> jsonToList(json: String?, cls: Class<T>?): MutableList<T> {
        val gson = Gson()
        val list: MutableList<T> = ArrayList()
        val array = JsonParser().parse(json).asJsonArray
        for (elem in array) {
            list.add(gson.fromJson(elem, cls))
        }
        return list
    }

    /**
     * 转成list中有map的
     *
     * @param gsonString
     * @return
     */
    fun <T> gsonToListMaps(gsonString: String?): List<Map<String?, T>?>? {
        var list: List<Map<String?, T>?>? = null
        if (gson != null) {
            list = gson!!.fromJson(
                gsonString,
                object : TypeToken<List<Map<String?, T>?>?>() {}.type
            )
        }
        return list
    }

    /**
     * 转成map的
     *
     * @param gsonString
     * @return
     */
    fun <T> gsonToMaps(gsonString: String?): Map<String?, T>? {
        var map: Map<String?, T>? = null
        if (gson != null) {
            map = gson!!.fromJson(gsonString, object : TypeToken<Map<String?, T>?>() {}.type)
        }
        return map
    }
    // -------
    /**
     * 按章节点得到相应的内容
     *
     * @param jsonString json字符串
     * @param note       节点
     * @return 节点对应的内容
     */
    fun getNoteJsonString(jsonString: String?, note: String?): String {
        if (TextUtils.isEmpty(jsonString)) {
            throw RuntimeException("json字符串")
        }
        if (TextUtils.isEmpty(note)) {
            throw RuntimeException("note标签不能为空")
        }
        val element = JsonParser().parse(jsonString)
        if (element.isJsonNull) {
            throw RuntimeException("得到的jsonElement对象为空")
        }
        return element.asJsonObject[note].toString()
    }

    /**
     * 按照节点得到节点内容，然后传化为相对应的bean数组
     *
     * @param jsonString 原json字符串
     * @param note       节点标签
     * @param beanClazz  要转化成的bean class
     * @return 返回bean的数组
     */
    fun <T> parserJsonToArrayBeans(
        jsonString: String?,
        note: String?,
        beanClazz: Class<T>?
    ): List<T> {
        val noteJsonString = getNoteJsonString(jsonString, note)
        return parserJsonToArrayBeans(noteJsonString, beanClazz)
    }

    /**
     * 按照节点得到节点内容，转化为一个数组
     *
     * @param jsonString json字符串
     * @param beanClazz  集合里存入的数据对象
     * @return 含有目标对象的集合
     */
    fun <T> parserJsonToArrayBeans(jsonString: String?, beanClazz: Class<T>?): List<T> {
        if (TextUtils.isEmpty(jsonString)) {
            throw RuntimeException("json字符串为空")
        }
        val jsonElement = JsonParser().parse(jsonString)
        if (jsonElement.isJsonNull) {
            throw RuntimeException("得到的jsonElement对象为空")
        }
        if (!jsonElement.isJsonArray) {
            throw RuntimeException("json字符不是一个数组对象集合")
        }
        val jsonArray = jsonElement.asJsonArray
        val beans: MutableList<T> = ArrayList()
        for (jsonElement2 in jsonArray) {
            val bean = Gson().fromJson(jsonElement2, beanClazz)
            beans.add(bean)
        }
        return beans
    }

    /**
     * 把相对应节点的内容封装为对象
     *
     * @param jsonString json字符串
     * @param clazzBean  要封装成的目标对象
     * @return 目标对象
     */
    fun <T> parserJsonToArrayBean(jsonString: String?, clazzBean: Class<T>?): T {
        if (TextUtils.isEmpty(jsonString)) {
            throw RuntimeException("json字符串为空")
        }
        val jsonElement = JsonParser().parse(jsonString)
        if (jsonElement.isJsonNull) {
            throw RuntimeException("json字符串为空")
        }
        if (!jsonElement.isJsonObject) {
            throw RuntimeException("json不是一个对象")
        }
        return Gson().fromJson(jsonElement, clazzBean)
    }

    /**
     * 按照节点得到节点内容，转化为一个数组
     *
     * @param jsonString json字符串
     * @param note       json标签
     * @param clazzBean  集合里存入的数据对象
     * @return 含有目标对象的集合
     */
    fun <T> parserJsonToArrayBean(jsonString: String?, note: String?, clazzBean: Class<T>?): T {
        val noteJsonString = getNoteJsonString(jsonString, note)
        return parserJsonToArrayBean(noteJsonString, clazzBean)
    }

    /**
     * 把bean对象转化为json字符串
     *
     * @param obj bean对象
     * @return 返回的是json字符串
     */
    fun toJsonString(obj: Any?): String {
        return if (obj != null) {
            Gson().toJson(obj)
        } else {
            throw RuntimeException("对象不能为空")
        }
    }
}