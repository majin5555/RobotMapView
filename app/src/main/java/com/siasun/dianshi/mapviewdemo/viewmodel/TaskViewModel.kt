package com.siasun.dianshi.mapviewdemo.viewmodel

import com.pnc.core.network.callback.IApiErrorCallback
import com.siasun.dianshi.bean.CleanAreaNew
import com.siasun.dianshi.bean.MapInfo
import com.siasun.dianshi.bean.SweepingModeListBean
import com.siasun.dianshi.bean.task.Task
import com.siasun.dianshi.network.manager.ApiManager
import com.siasun.dianshi.network.request.RequestCommonMapId

/**
 * @Author: MJ
 * @Date: 2025/7/02
 * @Description:任务
 */
class TaskViewModel : BaseViewModel() {

    /**
     * 获取所有的清扫模式
     */
    fun getSweepingMode(onComplete: (bean: SweepingModeListBean) -> Unit) {
        launchUIWithResult(responseBlock = {
            ApiManager.api.getSweepingMode()
        }, errorCall = object : IApiErrorCallback {
            override fun onError(code: Int?, error: String?) {
                super.onError(code, error)
                onComplete.invoke(SweepingModeListBean())
            }
        }, successBlock = {
            it?.let {
                onComplete.invoke(it)
            }
        })
    }
    /**
     * 获取地图列表
     */
    fun getMapList(onComplete: (list: MutableList<MapInfo>) -> Unit) {
        launchUIWithResult(responseBlock = {
            ApiManager.api.getMapListData()
        }, errorCall = object : IApiErrorCallback {
            override fun onError(code: Int?, error: String?) {
                super.onError(code, error)
            }
        }, successBlock = {
            it?.let {
                onComplete.invoke(it)
            }
        })
    }


    /**
     * 获取所有任务列表
     */
    fun getPadJobsList(onComplete: (list: MutableList<Task>) -> Unit) {
        launchUIWithResult(responseBlock = {
            ApiManager.api.getPadJobsList()
        }, errorCall = object : IApiErrorCallback {
            override fun onError(code: Int?, error: String?) {
                super.onError(code, error)
            }
        }, successBlock = {
            it?.let {
                onComplete.invoke(it)
            }
        })
    }
//
//    /**
//     * 获取所有任务详情
//     */
//    fun getPadJobs(task: RequestGetPadJobs, onComplete: (task: Task) -> Unit) {
//        launchUIWithResult(responseBlock = {
//            ApiManager.api.getPadJobs(task)
//        }, errorCall = object : IApiErrorCallback {
//            override fun onError(code: Int?, error: String?) {
//                super.onError(code, error)
//            }
//        }, successBlock = {
//            it?.let {
//                onComplete.invoke(it)
//            }
//        })
//    }


//    /**
//     * 保存任务
//     */
//    fun savePadJobs(task: Task, onComplete: (type: Int) -> Unit) {
//        launchUIWithResult(responseBlock = {
//            ApiManager.api.savePadJobs(task)
//        }, errorCall = object : IApiErrorCallback {
//            override fun onError(code: Int?, error: String?) {
//                super.onError(code, error)
//                LogUtil.e("保存任务失败 code ${code} error ${error}")
//                onComplete.invoke(code ?: 0)
//            }
//        }, successBlock = {
//            it?.let {
//                onComplete.invoke(1)
//            }
//        })
//    }
//
//    /**
//     * 相机自检
//     */
//    fun checkCameraState(onComplete: (type: CheckCameraResult) -> Unit) {
//        launchUIWithResult(responseBlock = {
//            ApiManager.api.checkCameraState()
//        }, errorCall = object : IApiErrorCallback {
//            override fun onError(code: Int?, error: String?) {
//                super.onError(code, error)
//                LogUtil.e("相机自检失败 code ${code} error ${error}")
//                val result = if (code == 404) {
//                    // 接口未部署
//                    CheckCameraResult.ServiceUnavailable(error ?: "接口未部署")
//                } else {
//                    CheckCameraResult.OtherError(code ?: 0, error ?: "未知错误")
//                }
//                onComplete(result)
////                onComplete.invoke(code ?: 0)
//            }
//        }, successBlock = {
//            it?.let {
////                onComplete.invoke(1)
//                if (it.result) {
//                    onComplete(CheckCameraResult.AllNormal)
//                } else {
//                    onComplete(CheckCameraResult.HasFailed(it.failed_cameras))
//                }
//            }
//        })
//    }
//
//    /**
//     * 删除任务接口
//     */
//    fun deletePadJob(task: RequestDeletePadJobs, onComplete: (task: DeleteTask) -> Unit) {
//        launchUIWithResult(responseBlock = {
//            ApiManager.api.deletePadJobs(task)
//        }, errorCall = object : IApiErrorCallback {
//            override fun onError(code: Int?, error: String?) {
//                super.onError(code, error)
//            }
//        }, successBlock = {
//            it?.let {
//                onComplete.invoke(it)
//            }
//        })
//    }

    /**
     * 获取当前mapID下的 区域列表
     */
    fun getAreaList(layerId: Int, onComplete: (list: MutableList<CleanAreaNew>) -> Unit) {
        launchUIWithResult(responseBlock = {
            val requestGetArea = RequestCommonMapId(layerId)
            ApiManager.api.getAreas(requestGetArea)
        }, errorCall = object : IApiErrorCallback {
            override fun onError(code: Int?, error: String?) {
                super.onError(code, error)
                onComplete.invoke(mutableListOf())
            }
        }, successBlock = {
            it?.let {
                onComplete.invoke(it.cleanAreas)
            }
        })
    }

    /**
     * 分析task对象，整理任务下的地图数量及集合信息
     */
//    fun analyzeTaskMapInfo(task: Task, onComplete: (task: MutableList<MapWithAreas>) -> Unit) {
//        // 创建一个Map，用于按地图ID分组CleanAreaNew对象
//        val mapAreasMap = mutableMapOf<Int, MutableList<CleanAreaNew>>()
//        // 遍历task.sub_task，将Sub_Task转换为CleanAreaNew并按地图ID分组
//        task.sub_task?.forEach { subTask ->
//            // 将Sub_Task转换为CleanAreaNew对象
//            val cleanArea = CleanAreaNew().apply {
//                regId = subTask.sub_region_id
//                layer_id = subTask.sub_region_layer
//                sub_name = subTask.sub_task_name
//                m_VertexPnt = subTask.areaVertexPnt
//            }
//            // 根据layer_id（地图ID）将cleanArea添加到对应的列表中
//            val areasList = mapAreasMap.getOrPut(cleanArea.layer_id) { mutableListOf() }
//            areasList.add(cleanArea)
//        }
//
//        // 创建新的数据列表，避免直接修改原列表
//        val newMapWithAreasList = mutableListOf<MapWithAreas>()
//
//        // 将分组后的结果转换为MapWithAreas对象并添加到新列表中
//        mapAreasMap.forEach { (mapId, areas) ->
//            val mapWithAreas = MapWithAreas(mapId, areas)
//            newMapWithAreasList.add(mapWithAreas)
//        }
//        onComplete.invoke(newMapWithAreasList)
//    }
}