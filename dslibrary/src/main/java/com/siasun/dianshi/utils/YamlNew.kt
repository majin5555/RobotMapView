package com.siasun.dianshi.utils

import com.siasun.dianshi.bean.MapData
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream

/**
 * 加载YAML 工具类
 */
class YamlNew {
    fun loadYaml(path: String, mapHeight: Float, mapWidth: Float): MapData {
        val metaData = MapData()
        val yaml = File(path)
        //文件不存在直接返回空
        if (yaml.exists()) {
            FileInputStream(yaml).use { inputYaml ->
                val resMap: LinkedHashMap<String, Any> = Yaml().load(inputYaml)

                resMap.forEach {
                    when (it.key) {
                        //分辨率
                        "resolution" -> {
                            metaData.resolution = it.value.toString().toFloat()
                        }
                        //原点坐标
                        "origin" -> {
                            metaData.originX = (it.value as ArrayList<Float>)[0]
                            metaData.originY = (it.value as ArrayList<Float>)[1]
                        }
                    }
                }

                // 解析图像宽度和高度
                metaData.width = mapWidth
                metaData.height = mapHeight
            }
        }
        return metaData
    }
}