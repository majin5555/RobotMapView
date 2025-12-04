package com.siasun.dianshi.mapviewdemo.viewmodel

import VirtualWallNew
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.siasun.dianshi.network.request.RequestCommonMapId
import com.pnc.core.network.callback.IApiErrorCallback
import com.siasun.dianshi.bean.CmsStation
import com.siasun.dianshi.bean.InitPoseRoot
import com.siasun.dianshi.bean.MachineStation
import com.siasun.dianshi.bean.MergedPoseBean
import com.siasun.dianshi.network.manager.ApiManager
import com.siasun.dianshi.network.request.RequestSaveVirtualWall
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


/**
 * @Author: MJ
 * @Date: 2025/8/22
 * @Description:
 */
class ShowMapViewModel : BaseViewModel() {
    val saveVirtualWall = MutableLiveData<Boolean>()
    val getVirtualWall = MutableLiveData<VirtualWallNew>()

    val saveMachineStationLiveData = MutableLiveData<Boolean>()
    val saveCmsStationLiveData = MutableLiveData<Boolean>()

    /**
     * 保存虚拟墙
     */
    fun saveVirtualWall(layerId: Int, virtualWallNew: VirtualWallNew) {
        launchUIWithResult(responseBlock = {
            val requestVirtualWall = RequestSaveVirtualWall(layerId, virtualWallNew)
            ApiManager.api.saveVirtualWall(requestVirtualWall)
        }, errorCall = object : IApiErrorCallback {
            override fun onError(code: Int?, error: String?) {
                saveVirtualWall.postValue(false)
            }
        }, successBlock = {
            it?.let {
                saveVirtualWall.postValue(true)
            }
        })
    }

    /**
     * 获取虚拟墙
     */
    fun getVirtualWall(mapId: Int) {
        launchUIWithResult(responseBlock = {
            val requestVirtualWall = RequestCommonMapId(mapId)
            // 确保返回 BaseResponse<VirtualWallNew> 类型
            ApiManager.api.getVirtualWall(requestVirtualWall)
        }, errorCall = object : IApiErrorCallback {
            override fun onError(code: Int?, error: String?) {
                // 移除对父类方法的调用，添加适当的错误处理
                getVirtualWall.postValue(null)
            }
        }, successBlock = {
            it?.let {
                getVirtualWall.postValue(it)
            }
        })
    }


    /**
     * 获取定位页面数据
     * 虚拟墙,上线点,顶视路线
     */
    fun getMergedPose(layerId: Int, onComplete: (mergedPoses: MergedPoseBean?) -> Unit) {
        viewModelScope.launch {

            val mergedDeferred = async {
                ApiManager.api.getMergedPose(RequestCommonMapId(layerId))
            }
            val mergedPoses = mergedDeferred.await()
            onComplete.invoke(mergedPoses.data)
        }
    }

    /**
     * 获取定位页面数据
     *  上线点
     */
    fun getInitPose(
        layerId: Int, onComplete: (initPoses: InitPoseRoot?) -> Unit
    ) {

        viewModelScope.launch {
            val poseDeferred = async {
                ApiManager.api.getInitPose(RequestCommonMapId(layerId))
            }
            val initPoses = poseDeferred.await()
            onComplete.invoke(initPoses.data)
        }
    }

    //获取充电站
    fun getMachineStation(
        onComplete: (machineStations: MutableList<MachineStation>?) -> Unit
    ) {
        viewModelScope.launch {

            val machineStationsDeferred = async {
                ApiManager.api.getMachineStation()
            }
            val machineStations = machineStationsDeferred.await()
            onComplete.invoke(machineStations.data)
        }
    }

    fun getStationData(
        layerId: Int, onComplete: (cmsStations: MutableList<CmsStation>?) -> Unit
    ) {
        viewModelScope.launch {

            val cmsStationDeferred = async {
                ApiManager.api.getCmsStation(RequestCommonMapId(layerId))
            }

            val cmsStations = cmsStationDeferred.await()

            onComplete.invoke(cmsStations.data)

        }
    }

//    fun saveMachineStation(machineStation: MutableList<MachineStation>) {
//        launchUIWithResult(responseBlock = {
//            val requestGetArea = RequestMachineStation(machineStation)
//            ApiManager.api.saveMachineStation(requestGetArea)
//        }, errorCall = object : IApiErrorCallback {
//            override fun onError(code: Int?, error: String?) {
//                saveMachineStationLiveData.postValue(false)
//            }
//        }, successBlock = {
//            saveMachineStationLiveData.postValue(true)
//        })
//    }


//    fun saveCmsStation(mapId: Int, cmsStation: MutableList<CmsStation>) {
//        launchUIWithResult(responseBlock = {
//            val saveCmsStation = RequestCmsStation(mapId, cmsStation)
//            ApiManager.api.saveCmsStation(saveCmsStation)
//        }, errorCall = object : IApiErrorCallback {
//            override fun onError(code: Int?, error: String?) {
//                saveCmsStationLiveData.postValue(false)
//            }
//        }, successBlock = {
//            saveCmsStationLiveData.postValue(true)
//        })
//    }

}