package com.siasun.dianshi

import android.os.Environment
import java.io.File

object ConstantBase {
    //path
    val PAD_ROOT_PATH =
        Environment.getExternalStorageDirectory().toString() + File.separator + "padFiles_new"

    private const val SHARE_FILES_BACKUP_ZIP = "ShareFilesBackup.zip"//ShareFilesBackup.zip
    const val PAD_MAP_NAME = "PM.pgm"//下载 PM.pgm文件 (新版弃用)
    const val PAD_CMS_CONFIG = "CmsConfig.json"//站点文件 (新版弃用)
    const val PAD_CMS_WORK_AREAS_LIST = "CmsWorkAreasList.json"//混行区
    const val PAD_CMS_STATIONS = "CmsStations.json"//站点
    const val PAD_CMS_DOORS = "CmsDoors.json"//过门
    const val PAD_PRODUCT_ID = "productId.txt"//产品ID
    const val PAD_MAP_NAME_YAML = "PM.yaml"//YAML
    const val PAD_MAP_NAME_PNG = "PM.png"//png
    const val PAD_JOBS_NAME = "PadJobs.json"//任务
    const val PAD_WORLD_NAME = "world_pad.dat"//路径
    const val PAD_VIRTUAL_WALL = "VirtualWall.json"//虚拟墙
    const val PAD_INIT_POSE_NAME = "InitPose.json"//上线点
    const val PAD_AREAS_NAME = "PadAreas.json"//区域文件
    const val PAD_MAP_BUILDER_AREAS_NAME = "MapBuilderAreas.json"//外部控制台区域文件
    const val PAD_CMS_ELEVATOR = "CmsElevator.json"//乘梯点
    const val PAD_MERGED_POSE_NAME = "mergedPose.json"//顶视路线的点

    /**
     * 车型
     */
    private const val QD450C = "qd450c.json"
    private const val QD750C = "qd750c.json"
    private const val QD750E = "qd750e.json"
    private const val D700C = "d700c.json"
    private const val KD450C = "kd450c.json"
    private const val KD750F = "kd450c.json"



    private const val PAD_SWEEPING_MODE = "pad_sweeping_mode"//清扫模式
    private const val MRC05_NAV_PATH = "/userdata/CarryBoy/NAV"//车体文件夹 NAV地址
    const val MRC05_MAP = "/userdata/CarryBoy/MAP"//MRC05多地图文件地址
    const val MRC05_AGV_PATH = "/userdata/CarryBoy/AGV"//MRC05 AGV地址
    const val MRC05_SHARE_FILES_PATH = "/userdata/CarryBoy/ShareFiles"//MRC05共享文件夹
    const val MRC05_LOG_PATH = "/userdata/CarryBoy/log/pad"//日志上传文件夹
    val PAD_MUSIC = PAD_ROOT_PATH + File.separator + "music"//PAD本地音乐
    val PAD_PM_PATH = PAD_ROOT_PATH + File.separator + "ProbMap"//本地地图地址
    val PRODUCT_ID_PATH = PAD_ROOT_PATH + File.separator + "productId"//本地productId地址

    /***多地图MAP文件夹*/
    val PAD_MAP_ROOT_PATH = "${PAD_ROOT_PATH}${File.separator}MAP"

    /***多地图 文件地址 全路径*/
    fun getFilePath(mapId: Int, fileName: String): String =
        "${PAD_MAP_ROOT_PATH}${File.separator}${mapId}${File.separator}${fileName}"

    /***多地图 获取文件夹 全路径*/
    fun getFolderPath(mapId: Int): String = "${PAD_MAP_ROOT_PATH}${File.separator}${mapId}"

    /***多地图 获取Mrc05文件夹 全路径*/
    fun getMRC05FolderPath(mapId: Int): String = "${MRC05_MAP}${File.separator}${mapId}"


    val PAD_AREAS_PATH = PAD_ROOT_PATH + File.separator + PAD_AREAS_NAME//PadAreas.json
    val PAD_CMS_CONFIG_PATH = PAD_ROOT_PATH + File.separator + PAD_CMS_CONFIG//CmsConfig.json"
    val PAD_WORLD_PATH = PAD_ROOT_PATH + File.separator + PAD_WORLD_NAME//world_pad.dat
    val MAP_BUILDER_AREAS_PATH =
        PAD_ROOT_PATH + File.separator + PAD_MAP_BUILDER_AREAS_NAME//MapBuilderAreas.json
    val VIRTUAL_WALL_PATH = PAD_ROOT_PATH + File.separator + PAD_VIRTUAL_WALL//VirtualWall.json
    val PAD_JOBS_PATH = PAD_ROOT_PATH + File.separator + PAD_JOBS_NAME//PadJobs.json
    val INIT_POSE_PATH = PAD_ROOT_PATH + File.separator + PAD_INIT_POSE_NAME//InitPose.json
    val MERGED_POSE_ROUTE_PATH =
        PAD_ROOT_PATH + File.separator + PAD_MERGED_POSE_NAME//mergedPose.json
    val PRODUCT_ID_PATH_TEXT = PRODUCT_ID_PATH + File.separator + PAD_PRODUCT_ID//productId.txt
    val PAD_PM_PNG_PATH = PAD_PM_PATH + File.separator + PAD_MAP_NAME_PNG //PM.png
    val PAD_PM_YAML_PATH = PAD_PM_PATH + File.separator + PAD_MAP_NAME_YAML //PM.yaml
    val PAD_CMS_WORK_AREAS_LIST_PATH =
        PAD_MAP_ROOT_PATH + File.separator + PAD_CMS_WORK_AREAS_LIST// CmsWorkAreasList.json
    val PAD_CMS_STATIONS_PATH =
        PAD_MAP_ROOT_PATH + File.separator + PAD_CMS_STATIONS//CmsStations.json
    val PAD_CMS_DOORS_PATH = PAD_MAP_ROOT_PATH + File.separator + PAD_CMS_DOORS// CmsDoors.json

    val SWEEP_MODE_PATH_NAME: String =
        MRC05_SHARE_FILES_PATH + File.separator + PAD_SWEEPING_MODE//mrc05中清扫模式的地址

    val MRC05_BACKUP_PATH = PAD_ROOT_PATH + File.separator + "backup"//备份文件地址
    val MRC05_SHARE_FILES_BACKUP_ZIP_PATH =
        MRC05_BACKUP_PATH + File.separator + SHARE_FILES_BACKUP_ZIP
    val MRC05_RESTORE_FILES_PATH =
        MRC05_BACKUP_PATH + File.separator + "ShareFiles" + File.separator //一键迁移解压.zip文件目录
    val MRC05_PAD_MUSIC = MRC05_SHARE_FILES_PATH + File.separator + "pad_music" //pad 音频资源文件
    val MRC05_MERGED_POSE_PATH =
        MRC05_NAV_PATH + File.separator + "visloc" + File.separator + "config" + File.separator + "XS240605"//mrc05 mergedPose.json地址



    private val PAD_SWEEPING_MODE_PATH: String =
        PAD_ROOT_PATH + File.separator + PAD_SWEEPING_MODE//PAD本地存清扫模式的地址


}
