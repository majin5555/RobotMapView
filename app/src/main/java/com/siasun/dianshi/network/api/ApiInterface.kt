package com.siasun.dianshi.network.api

import VirtualWallNew
import com.siasun.dianshi.network.request.RequestCommonMapId
import com.pnc.core.network.response.BaseResponse
import com.siasun.dianshi.bean.CmsStation
import com.siasun.dianshi.bean.ElevatorRoot
import com.siasun.dianshi.bean.InitPoseRoot
import com.siasun.dianshi.bean.MachineStation
import com.siasun.dianshi.bean.MergedPoseBean
import com.siasun.dianshi.network.request.RequestSaveVirtualWall
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * @author fuxing.che
 * @date   2023/2/27 19:07
 * @desc   API接口类
 */
interface ApiInterface {

    //    /**
//     * 上传地图信息
//     */
//    @POST("/save_map")
//    suspend fun upLoadMapInfo(@Body mapInfo: RequestSaveMap): BaseResponse<MutableList<MapInfo>>
//
//    /**
//     * 获取地图ID
//     */
//    @POST("/get_new_map_id")
//    suspend fun getMapId(): BaseResponse<MapIdBean>
//
//    /**
//     * 获取地图列表
//     */
//    @POST("/get_map")
//    suspend fun getMapListData(): BaseResponse<MutableList<MapInfo>>
//
//    /**
//     * 切换地图
//     */
//    @POST("/switch_map")
//    suspend fun switchMap(@Body mapInfo: SwitchMapBean): BaseResponse<MapInfo>
//
//    @POST("/delete_map")
//    suspend fun deleteMapInfo(@Body mapIdBean: DeleteMapIdBean): BaseResponse<MutableList<MapInfo>>
//
//    /**
//     * 获取当前地图
//     */
//    @POST("/get_current_mapId")
//    suspend fun getCurrentMap(): BaseResponse<MapInfo?>
//
//    /**/
//    @POST("/get_zone")
//    suspend fun getZone(): BaseResponse<MutableList<CleanArea>>?
//
//    @POST("/set_zone")
//    suspend fun setZone(@Body data: MutableList<SaveZoneRequest>): BaseResponse<MutableList<CleanArea>>?
//
//    /**
//     * @description 获取版本号
//     * @author CheFuX1n9
//     * @since 2024/7/15 15:50
//     */
//    @POST("/version")
//    suspend fun getVersion(): BaseResponse<VersionBean?>?
//
//    /**
//     * mrc05截取黑匣子
//     */
//    @POST("/black_box")
//    suspend fun getBlackBox(): BaseResponse<BlackBoxBean>
//
//    /**
//     * mrc05一键备份
//     */
//    @POST("/backup")
//    suspend fun oneClickBackup(): BaseResponse<BackupMigrationBean?>?
//
//    /**
//     * mrc05一键迁移
//     */
//    @POST("/restore")
//    suspend fun oneClickRestore(): BaseResponse<BackupMigrationBean>
//
//    /**
//     * 文件下载
//     */
//    @GET
//    @Streaming // 使用 Streaming 避免下载大文件时内存溢出
//    fun downloadFile(@Url fileUrl: String): Call<ResponseBody>
//
//    /**
//     * 获取错误日志
//     * error_level : 0: 显示 普通信息、告警、错误信息
//     * error_level : 2: 只显示错误信息
//     */
//    @POST("/get_error_list")
//    suspend fun getLog(@Body errorLevel: RequestGetLog): BaseResponse<MutableList<LogBean>>
//
//
//    /**
//     * 区域删除校验
//     */
//    @POST("/check_region_usage")
//    suspend fun checkRegionUsage(@Body task: RequestDeletePadArea): BaseResponse<TaskVerification>
//
//
//    @POST("/get_pad_areas")
//    suspend fun getAreas(@Body area: RequestCommonMapId): BaseResponse<CleanAreaRootNew>
//
//    @POST("/save_pad_areas")
//    suspend fun saveAreas(@Body area: RequestSaveArea): BaseResponse<CleanAreaRootNew>
//
    @POST("/save_virtual_wall")
    suspend fun saveVirtualWall(@Body vw: RequestSaveVirtualWall): BaseResponse<Any>

    //
    @POST("/get_virtual_wall")
    suspend fun getVirtualWall(@Body vw: RequestCommonMapId): BaseResponse<VirtualWallNew>
//
//    /**
//     * 查询电梯
//     */
//    @POST("/get_elevators")
//    suspend fun getElevators(): BaseResponse<MutableList<ElevatorBean>?>
//
//    /**
//     * 新建电梯
//     */
//    @POST("/add_elevator")
//    suspend fun addElevator(@Body elevator: RequestSaveElevator): BaseResponse<ElevatorBean?>
//
//    /**
//     * 更新电梯
//     */
//    @POST("/update_elevator")
//    suspend fun updateElevator(@Body elevator: RequestUpdateElevator): BaseResponse<ElevatorBean?>
//
//    /**
//     * 删除电梯
//     */
//    @POST("/delete_elevator")
//    suspend fun deleteElevator(@Body elevator: RequestDeleteElevator): BaseResponse<ElevatorBean?>
//
//    /**
//     * 设置特殊区域
//     */
//    @POST("/set_regions")
//    suspend fun setSpecialArea(@Body data: RequestSaveSpecialArea): BaseResponse<MutableList<CleanAreaNew>>?
//
//    /**
//     * 获取特殊区域
//     */
//    @POST("/get_regions")
//    suspend fun getSpecialArea(@Body data: RequestGetSpecialArea): BaseResponse<MutableList<CleanAreaNew>>?
//
//
//    /**
//     * 保存过门区
//     */
//    @POST("/save_cms_doors")
//    suspend fun saveCmsDoors(@Body data: RequestSaveDoors): BaseResponse<Any>
//
//    /**
//     * 获取过门区
//     */
//    @POST("/get_cms_doors")
//    suspend fun getCmsDoors(@Body data: RequestCommonMapId): BaseResponse<MutableList<AreasDoor>>
//
//    /**
//     * 版本更新
//     */
//    @POST("/check_apk")
//    suspend fun checkApk(@Body data: RequestCheckApk): BaseResponse<CheckApkBean>?
//
//
//    /**
//     * 获取混行区
//     */
//    @POST("/get_cms_work_areas_list")
//    suspend fun getCmsWorkAreas(@Body data: RequestCommonMapId): BaseResponse<CmsWorkAreasListRoot>
//
//    @POST("/save_cms_work_areas_list")
//    suspend fun saveCmsWorkAreas(@Body data: RequestSaveCmsWorkArea): BaseResponse<Any>
//
//
//    @POST("/save_integrated_machine")
//    suspend fun saveMachineStation(@Body date: RequestMachineStation): BaseResponse<Any>
//
    @POST("/get_integrated_machine")
    suspend fun getMachineStation(): BaseResponse<MutableList<MachineStation>>
//
//    @POST("/save_cms_stations_data")
//    suspend fun saveCmsStation(@Body data: RequestCmsStation): BaseResponse<Any>
//
    @POST("/get_cms_stations_data")
    suspend fun getCmsStation(@Body data: RequestCommonMapId): BaseResponse<MutableList<CmsStation>>
//
//    @POST("/save_init_pose")
//    suspend fun saveInitPose(@Body init: RequestInitPose): BaseResponse<Any>
//
    @POST("/get_init_pose")
    suspend fun getInitPose(@Body data: RequestCommonMapId): BaseResponse<InitPoseRoot>
//
    @POST("/get_merged_pose")
    suspend fun getMergedPose(@Body date: RequestCommonMapId): BaseResponse<MergedPoseBean>
//
//    @POST("/save_cms_elevator")
//    suspend fun saveCmsElevator(@Body data: RequestCmsElevator): BaseResponse<Any>
//
    @POST("/get_cms_elevator")
    suspend fun getCmsElevator(@Body date: RequestCommonMapId): BaseResponse<ElevatorRoot>
//
//    @POST("/trigger_hot_reload")
//    suspend fun hotReload(@Body date:RequestHotReload):BaseResponse<BaseResponse<Any>>

}