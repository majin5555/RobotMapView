package com.pnc.core.network.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * @author fuxing.che
 * @date   2023/2/27 19:07
 * @desc   API接口类
 */
interface ApiInterface {

//    /**
//     * map_id 地图ID
//     * type 区域类型 -1查询全部
//     */
//    @POST("/get_regions")
//    suspend fun getZone(@Body data: RequestGetRegions): BaseResponse<MutableList<CleanAreaNew>>?
//
//    @POST("/set_regions")
//    suspend fun setZone(
//        @Body data: SaveRegionsRequest
//    ): BaseResponse<MutableList<CleanAreaNew>>?
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
    /**
     * 文件下载
     */
    @GET
    @Streaming // 使用 Streaming 避免下载大文件时内存溢出
    fun downloadFile(@Url fileUrl: String): Call<ResponseBody>
//
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
//     * 版本更新
//     */
//    @POST("/check_apk")
//    suspend fun checkApk(@Body data: RequestCheckApk): BaseResponse<CheckApkBean>?
//
//
//    /**
//     * 获取地图列表
//     */
//    @POST("/get_map")
//    suspend fun getMapListData(): BaseResponse<MutableList<MapInfo>>
//
//
//    /**
//     * 获取当前地图
//     */
//    @POST("/get_current_mapId")
//    suspend fun getCurrentMap(): BaseResponse<MapInfo>
//
//
//    /**
//     * 切换地图
//     */
//    @POST("/switch_map")
//    suspend fun switchMap(@Body mapInfo: SwitchMapBean): BaseResponse<MapInfo>
//
//    /**
//     * 保存任务
//     */
//    @POST("/save_pad_jobs")
//    suspend fun savePadJobs(@Body task: Task): BaseResponse<Task>
//
//    /**
//     * 获取所有任务
//     */
//    @POST("/get_pad_jobs_list")
//    suspend fun getPadJobsList(): BaseResponse<MutableList<Task>>
//
//    /**
//     * 获取任务详情
//     */
//    @POST("/get_pad_jobs")
//    suspend fun getPadJobs(@Body task: RequestGetPadJobs): BaseResponse<Task>
//
//    /**
//     * 任务删除
//     */
//    @POST("/delete_pad_jobs")
//    suspend fun deletePadJobs(@Body task: RequestDeletePadJobs): BaseResponse<DeleteTask>
//
//
//    /**
//     * 任务下发
//     */
//    @POST("/run_pad_job_task")
//    suspend fun runPadJobTask(@Body task: Task): BaseResponse<Task>
//
//
//    /**
//     * 区域删除校验
//     */
//    @POST("/check_region_usage")
//    suspend fun checkRegionUsage(@Body task: RequestDeletePadArea): BaseResponse<TaskVerification>
//
//    /**
//     * 清扫模式删除校验
//     */
//    @POST("/check_clean_mode_usage")
//    suspend fun checkCleanModeUsage(@Body mode: RequestDeletePadCleanMode): BaseResponse<TaskVerification>
//
//    /**
//     * 新建电梯
//     */
//    @POST("/add_elevator")
//    suspend fun addElevator(@Body elevator: RequestSaveElevator): BaseResponse<ElevatorBean?>
//
//    /**
//     * 删除电梯
//     */
//    @POST("/delete_elevator")
//    suspend fun deleteElevator(@Body elevator: RequestDeleteElevator): BaseResponse<ElevatorBean?>
//
//    /**
//     * 查询电梯
//     */
//    @POST("/get_elevators")
//    suspend fun getElevators(): BaseResponse<MutableList<ElevatorBean>?>
//
//    /**
//     * update_elevator
//     */
//    @POST("/update_elevator")
//    suspend fun updateElevator(@Body elevator: RequestUpdateElevator): BaseResponse<ElevatorBean?>
//
//
//    /**
//     * 获取mrc05时间
//     */
//    @POST("/get_date_time")
//    suspend fun getDatTime(): BaseResponse<DateTimeBean>?
//
//    /**
//     * 获取当前楼层下的当前任务的区域
//     */
//    @POST("/get_area_vertex_points")
//    suspend fun getCurrentJobAreas(@Body req: RequestCurrentJobsAreas): BaseResponse<MutableList<CleanAreaNew>?>
//
//    /**
//     * 获取当前楼层下的清扫区域
//     */
//    @POST("/get_pad_areas")
//    suspend fun getAreas(@Body area: RequestCommonMapId): BaseResponse<CleanAreaRootNew>
//
//    /**
//     * 获取当前楼层下的虚拟墙
//     */
//    @POST("/get_virtual_wall")
//    suspend fun getVirtualWall(@Body vw: RequestCommonMapId): BaseResponse<VirtualWallNew>
//
//    /**
//     * 保存当前楼层的上线点
//     */
//    @POST("/save_init_pose")
//    suspend fun saveInitPose(@Body init: RequestInitPose): BaseResponse<Any>
//
//    /**
//     * 获取当前楼层下的上线点
//     */
//    @POST("/get_init_pose")
//    suspend fun getInitPose(@Body data: RequestCommonMapId): BaseResponse<InitPoseRoot>
//
//    /**
//     * 获取当前楼层下的顶视路线
//     */
//    @POST("/get_merged_pose")
//    suspend fun getMergedPose(@Body date: RequestCommonMapId): BaseResponse<MergedPoseBean>

}