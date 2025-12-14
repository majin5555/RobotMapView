//package com.siasun.dianshi.mapviewdemo.utils
//
//import ConstantBase.PAD_CMS_STATIONS_PATH
//import ConstantBase.MAP_BUILDER_AREAS_PATH
//import ConstantBase.PAD_AREAS_NAME
//import ConstantBase.PAD_AREAS_PATH
//import ConstantBase.PAD_CMS_CONFIG_PATH
//import ConstantBase.PAD_CMS_ELEVATOR
//import ConstantBase.PAD_CMS_WORK_AREAS_LIST_PATH
//import ConstantBase.PAD_INIT_POSE_NAME
//import ConstantBase.PAD_JOBS_NAME
//import ConstantBase.PAD_MERGED_POSE_NAME
//import ConstantBase.PAD_PM_PATH
//import ConstantBase.PAD_PM_PNG_PATH
//import ConstantBase.PAD_PM_YAML_PATH
//import ConstantBase.PAD_VIRTUAL_WALL
//import ConstantBase.PAD_WORLD_NAME
//import ConstantBase.PAD_WORLD_PATH
//import ConstantBase.PRODUCT_ID_PATH_TEXT
//import ConstantBase.VIRTUAL_WALL_PATH
//import ConstantBase.getFilePath
//import ConstantBase.getFolderPath
//import VirtualWallNew
//import android.content.Context
//import android.os.Build
//import androidx.annotation.RequiresApi
//import bean.area.CleanAreaNew
//import bean.area.CleanAreaRootNew
//import bean.cms_config.AreasDoor
//import bean.cms_config.CmsConfigRootNew
//import bean.cms_config.CmsDoorsRoot
//import bean.cms_config.CmsStationsRoot
//import bean.cms_config.CmsWorkAreasListRoot
//import bean.cms_config.Station
//import bean.cms_config.WorkAreasNew
//import bean.elevator.ElevatorPoint
//import bean.elevator.ElevatorRoot
//import bean.init_pose.InitPose
//import bean.init_pose.InitPoseRoot
//import bean.task.Task
//import bean.task.TaskRoot
//import bean.top_vew_route.MergedPoseBean
//import com.alibaba.fastjson.JSONObject
//import com.alibaba.fastjson.serializer.SerializeConfig
//import com.alibaba.fastjson.serializer.SerializerFeature
//import com.pnc.baselibrary.utils.TimeUtil
//import com.pnc.core.common.model.SweepingModeListBean
//import com.pnc.core.common.model.UpdateFileMany
//import com.siasun.dianshi.log.LogUtil
//import com.pnc.software.siasun.cleanrobot.crl.controller.MainController
//import newmapeditorlibofagv.Robot.VirtualWall.PointNew
//import utils.FileUtil
//import utils.JsonUtils
//import yourlib.Geometry.LineNew
//import yourlib.utils.io.FileIOUtil
//import yourlib.world.World
//import java.io.BufferedReader
//import java.io.File
//import java.io.IOException
//import java.io.InputStreamReader
//import java.nio.file.Files
//import java.nio.file.Paths
//import java.nio.file.StandardCopyOption
//
//
///**
// * 将实体写成Json文件
// *
// * @param obj  实体Bean
// * @param path Json文件路径
// */
//fun writeJsonFile(obj: Any?, path: String?) {
//    FileIOUtil.writeFileFromString(
//        path, JSONObject.toJSONString(
//            obj,
//            SerializeConfig(true),
//            SerializerFeature.PrettyFormat,
//            SerializerFeature.DisableCircularReferenceDetect,
//            SerializerFeature.WriteMapNullValue,
//            SerializerFeature.WriteNonStringKeyAsString,
////            SerializerFeature.BrowserCompatible
//        )
//    )
//    FileIOUtil.fileSync()
//}
//
///**
// * 读取json文件，转成实体
// *
// * @return
// */
//fun readStringToObject(path: String?, clazz: Class<*>?): Any? {
//    val jsonStr = FileIOUtil.readFile2String(path, "utf-8")
//    if (null == jsonStr || jsonStr.isEmpty()) {
//        return null
//    }
//    //将Json格式转成对象
//    return JSONObject.parseObject<Any>(jsonStr, clazz)
//}
//
///*******************************************************************************************
// ******************************************************************************************/
//
///**
// * 多地图
// */
//fun createResourceFile(mapId: Int) {
//    //混行区文件
//    if (!File(PAD_CMS_WORK_AREAS_LIST_PATH).exists()) writeJsonFile(
//        CmsWorkAreasListRoot(mutableListOf<WorkAreasNew>()), PAD_CMS_WORK_AREAS_LIST_PATH
//    )
//    //站点文件
//    if (!File(PAD_CMS_STATIONS_PATH).exists()) writeJsonFile(
//        CmsStationsRoot(mutableListOf<Station>()), PAD_CMS_STATIONS_PATH
//    )
//    //过门文件
//    if (!File(PAD_CMS_DOORS_PATH).exists()) writeJsonFile(
//        CmsDoorsRoot(mutableListOf<AreasDoor>()), PAD_CMS_DOORS_PATH
//    )
//
//    val mAllFileName = mutableListOf(
//        PAD_VIRTUAL_WALL,
//        PAD_AREAS_NAME,
//        PAD_INIT_POSE_NAME,
//        PAD_CMS_ELEVATOR,
//        PAD_WORLD_NAME,
//    )
//
//    //初始化
//    mAllFileName.forEach {
//        when (it) {
//            // VirtualWall.json
//            PAD_VIRTUAL_WALL -> saveVirtualWall(mapId, VirtualWallNew(1))
//
//            //PadAreas.json
//            PAD_AREAS_NAME -> savePadAreasJson(mapId, mutableListOf<CleanAreaNew>())
//
//            //InitPose.json
//            PAD_INIT_POSE_NAME -> saveInitPose(mapId, mutableListOf<InitPose>())
//
//
//            //CmsElevator.json
//            PAD_CMS_ELEVATOR -> saveElevator(mapId, mutableListOf<ElevatorPoint>())
//
//            //world_pad.dat
//            PAD_WORLD_NAME -> World().saveWorld(
//                getFolderPath(mapId), it
//            )
//        }
//    }
//}
//
///**
// * 复制资源文件 到指定文件夹中
// */
//@RequiresApi(Build.VERSION_CODES.O)
//fun moveResourceFies(mapId: Int) {
//    val fileCms = File(PAD_CMS_CONFIG_PATH)
//    if (fileCms.exists()) {
//        val cmsConfig = readStringToObject(
//            PAD_CMS_CONFIG_PATH, CmsConfigRootNew::class.java
//        ) as CmsConfigRootNew
//        //混行区文件
//        writeJsonFile(
//            CmsWorkAreasListRoot(cmsConfig.cms.workAreasList), PAD_CMS_WORK_AREAS_LIST_PATH
//        )
//        //站点文件
//        writeJsonFile(CmsStationsRoot(cmsConfig.cms.stations), PAD_CMS_STATIONS_PATH)
//        //过门文件
//        writeJsonFile(CmsDoorsRoot(cmsConfig.cms.doors), PAD_CMS_DOORS_PATH)
//        //删除原来CmsConfig.json
//        fileCms.delete()
//    }
//
//    val mAllFileName = mutableListOf(
//        PAD_PM_PNG_PATH,
//        PAD_PM_YAML_PATH,
//        VIRTUAL_WALL_PATH,
//        PAD_AREAS_PATH,
//        INIT_POSE_PATH,
//        PAD_WORLD_PATH,
//        MAP_BUILDER_AREAS_PATH,
//    )
//    val destinationDir = getFolderPath(mapId)
//    mAllFileName.forEach {
//        copyAndDeleteFile(it, destinationDir)
//    }
//    //删除文件夹
//    deleteFolder(File(PAD_PM_PATH))
//}
//
//
///**
// * 项目初始化时候调用
// *
// * 创建产品 id 文件夹
// */
//fun createProductID() = FileUtil.createOrExistsDir(PRODUCT_ID_PATH_TEXT)
//
//
///**
// * 多地图
// * 文件更新
// * PM.pgm 环境地图
// * VirtualWall.json 虚拟墙
// * CmsConfig.json
// * world_pad.dat
// * PadAreas.json
// *
// */
//fun upDataMsgToChassis(list: MutableList<String>, mapId: Int) {
//    val updateFile = UpdateFileMany(list, TimeUtil.getTime(), mapId)
//    LogUtil.i("多地图热更新发送 ${JsonUtils.convertObjectToJSON(updateFile)}")
//    MainController.sendUpload(JsonUtils.convertObjectToJSON(updateFile))
//}
//
///*********************************************保存******************************************************/
//
//
///*********************************************加载******************************************************/
//
///**
// * 顶视路线
// */
//fun loadTopViewRoute(mapId: Int = 0): MergedPoseBean? {
//    return readStringToObject(
//        getFilePath(mapId, PAD_MERGED_POSE_NAME), MergedPoseBean::class.java
//    ) as MergedPoseBean?
//}
//
///**
// * 读取PadJobs.json
// */
//fun loadPadJobs(mapId: Int = 0): TaskRoot? {
//    return readStringToObject(getFilePath(mapId, PAD_JOBS_NAME), TaskRoot::class.java) as TaskRoot?
//}
//
///**
// * 保存PadJobs.json
// */
//
//fun savePadJobs(mapId: Int, jobs: MutableList<Task>) {
//    writeJsonFile(TaskRoot(jobs), getFilePath(mapId, PAD_JOBS_NAME))
//}
//
///**
// * 加载 VirtualWall.json
// */
//fun loadVirtualWall(mapId: Int): VirtualWallNew? {
//    if (!File(getFilePath(mapId, PAD_VIRTUAL_WALL)).exists()) {
//        writeJsonFile(VirtualWallNew(1), getFilePath(mapId, PAD_VIRTUAL_WALL))
//    }
//    return readStringToObject(
//        getFilePath(mapId, PAD_VIRTUAL_WALL), VirtualWallNew::class.java
//    ) as VirtualWallNew?
//}
//
///**
// * 保存 VirtualWall.json
// */
//fun saveVirtualWall(mapId: Int, virtualWall: VirtualWallNew) {
//    writeJsonFile(virtualWall, getFilePath(mapId, PAD_VIRTUAL_WALL))
//}
//
///**
// * 加载 padAreas.json
// */
//fun loadPadAreas(mapId: Int): CleanAreaRootNew? {
//    return readStringToObject(
//        getFilePath(mapId, PAD_AREAS_NAME), CleanAreaRootNew::class.java
//    ) as CleanAreaRootNew?
//}
//
///**
// * 保存padAreas.json
// */
//fun savePadAreasJson(mapID: Int, cleanAreas: MutableList<CleanAreaNew>) {
//    writeJsonFile(CleanAreaRootNew(cleanAreas), getFilePath(mapID, PAD_AREAS_NAME))
//}
//
//
///**
// * 加载 InitPose.json
// */
//fun loadInitPose(mapID: Int): InitPoseRoot? {
//    return readStringToObject(
//        getFilePath(mapID, PAD_INIT_POSE_NAME), InitPoseRoot::class.java
//    ) as InitPoseRoot?
//}
//
///**
// *  InitPose.json
// */
//fun saveInitPose(mapID: Int, list: MutableList<InitPose>) {
//    writeJsonFile(InitPoseRoot(list), getFilePath(mapID, PAD_INIT_POSE_NAME))
//}
//
//
///**
// * 加载 乘梯点
// */
//fun loadElevator(mapId: Int): ElevatorRoot? {
//    return readStringToObject(
//        getFilePath(mapId, PAD_CMS_ELEVATOR), ElevatorRoot::class.java
//    ) as ElevatorRoot?
//}
//
//
///**
// * 保存 乘梯点 CmsElevator.json
// */
//fun saveElevator(mapId: Int, list: MutableList<ElevatorPoint>) {
//    writeJsonFile(
//        ElevatorRoot(list), getFilePath(mapId, PAD_CMS_ELEVATOR)
//    )
//}
//
///**
// * 加载 CmsWorkAreasList.json 混行区文件
// */
//fun loadCmsWorkAreasList(mapId: Int): MutableList<WorkAreasNew> {
//    val list: MutableList<WorkAreasNew> = mutableListOf()
//    (readStringToObject(
//        PAD_CMS_WORK_AREAS_LIST_PATH, CmsWorkAreasListRoot::class.java
//    ) as CmsWorkAreasListRoot?)?.let {
//        for (workAreasNew in it.workAreasList) {
//            if (mapId == workAreasNew.floor) {
//                list.add(workAreasNew)
//            }
//        }
//    }
//
//    return list
//}
//
///**
// * 保存 CmsWorkAreasList.json 混行区文件
// */
//fun saveCmsWorkAreasList(workAreasList: MutableList<WorkAreasNew>) {
//    writeJsonFile(CmsWorkAreasListRoot(workAreasList), PAD_CMS_WORK_AREAS_LIST_PATH)
//}
//
//
///**
// *  加载CmsDoors.json （过门配置）
// */
//fun loadCmsDoors(mapId: Int): MutableList<AreasDoor> {
//    val list: MutableList<AreasDoor> = mutableListOf()
//    (readStringToObject(
//        PAD_CMS_DOORS_PATH, CmsDoorsRoot::class.java
//    ) as CmsDoorsRoot?)?.let {
//        for (door in it.doors) {
//            if (mapId == door.loraID) {
//                list.add(door)
//            }
//        }
//    }
//
//    return list
//}
//
///**
// * 保存 CmsDoors.json（过门配置）
// */
//fun saveCmsDoors(doors: MutableList<AreasDoor>) {
//    writeJsonFile(CmsDoorsRoot(doors), PAD_CMS_DOORS_PATH)
//}
//
///**
// * 加载 CmsStations.json （站点文件）
// */
//fun loadCmsStations(mapId: Int): MutableList<Station> {
//    val list: MutableList<Station> = mutableListOf()
//    (readStringToObject(
//        PAD_CMS_STATIONS_PATH, CmsStationsRoot::class.java
//    ) as CmsStationsRoot?)?.let {
//        for (station in it.stations) {
//            if (mapId == station.floor) {
//                list.add(station)
//            }
//        }
//    }
//
//    return list
//}
//
///**
// * 加载 非当前楼层下的站点文件 CmsStations.json （站点文件）
// */
//fun loadAllCmsStations(mapId: Int): MutableList<Station> {
//    val list: MutableList<Station> = mutableListOf()
//    (readStringToObject(
//        PAD_CMS_STATIONS_PATH, CmsStationsRoot::class.java
//    ) as CmsStationsRoot?)?.let {
//        for (station in it.stations) {
//            if (mapId != station.floor) {
//                list.add(station)
//            }
//        }
//    }
//
//    return list
//}
///**
// * 保存 CmsStations.json （站点文件）
// */
//
//fun saveCmsStations(station: MutableList<Station>) {
//    writeJsonFile(CmsStationsRoot(station), PAD_CMS_STATIONS_PATH)
//}
//
//
///**
// * 加载 清扫模式文件
// */
//fun loadSweepingMode(path: String): SweepingModeListBean? {
//    return readStringToObject(
//        path, SweepingModeListBean::class.java
//    ) as SweepingModeListBean?
//}
//
//
///**
// * 保存 清扫模式文件
// */
//
//fun saveSweepingMode(mSweepingModeListBean: SweepingModeListBean, filePath: String) {
//    writeJsonFile(mSweepingModeListBean, filePath)
//}
//
///**
// * 读取assets本地json
// * @param fileName
// * @param context
// * @return
// */
//fun getJson(fileName: String?, context: Context): String {
//    //将json数据变成字符串
//    val stringBuilder = StringBuilder()
//    try {
//        //获取assets资源管理器
//        val assetManager = context.assets
//        //通过管理器打开文件并读取
//        val bf = BufferedReader(
//            InputStreamReader(
//                assetManager.open(fileName!!)
//            )
//        )
//        var line: String?
//        while (bf.readLine().also { line = it } != null) {
//            stringBuilder.append(line)
//        }
//    } catch (e: IOException) {
//        e.printStackTrace()
//    }
//    return stringBuilder.toString()
//}
//
//fun jsonToMap(jsonString: String): MutableMap<String, Any> {
//    val json = org.json.JSONObject(jsonString)
//    val map = mutableMapOf<String, Any>()
//
//    for (key in json.keys()) {
//        val value = json.get(key)
//        map[key] = value
//    }
//
//    return map
//}
//
///**
// * sourcePath 原文件地址
// * destinationDir 目标文件地址
// */
//@RequiresApi(Build.VERSION_CODES.O)
//fun copyAndDeleteFile(sourcePath: String, destinationDir: String) {
//    val sourceFile = Paths.get(sourcePath)
//    val destinationFile = Paths.get(destinationDir).resolve(sourceFile.fileName)
//    try {
//        Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING)
//        Files.delete(sourceFile)
//    } catch (e: Exception) {
//        LogUtil.d("copyAndDeleteFile Error")
//    }
//}
//
///**
// * 删除文件夹
// */
//fun deleteFolder(folder: File) {
//    if (folder.isDirectory) {
//        val files = folder.listFiles()
//        if (files != null) {
//            for (file in files) {
//                deleteFolder(file)
//            }
//        }
//    }
//    folder.delete()
//}
//
//
//fun createPolygonLineNew(mPolygonLine: MutableList<LineNew>, mVertexPnt: MutableList<PointNew>) {
//    for (i in mVertexPnt.indices) {
//        val line = LineNew(PointNew(), PointNew())
//        mPolygonLine.add(line)
//    }
//    for (i in mVertexPnt.indices) {
//        if (i < mVertexPnt.size - 1) {
//            mPolygonLine[i].ptStart.X = mVertexPnt[i].X
//            mPolygonLine[i].ptStart.Y = mVertexPnt[i].Y
//            mPolygonLine[i].ptEnd.X = mVertexPnt[i + 1].X
//            mPolygonLine[i].ptEnd.Y = mVertexPnt[i + 1].Y
//        } else if (i == mVertexPnt.size - 1) {
//            mPolygonLine[i].ptStart.X = mVertexPnt[i].X
//            mPolygonLine[i].ptStart.Y = mVertexPnt[i].Y
//            mPolygonLine[i].ptEnd.X = mVertexPnt[0].X
//            mPolygonLine[i].ptEnd.Y = mVertexPnt[0].Y
//        }
//    }
//}
//
