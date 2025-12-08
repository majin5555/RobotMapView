package com.siasun.dianshi.mapviewdemo.viewmodel

import VirtualWallNew
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.siasun.dianshi.network.request.RequestCommonMapId
import com.pnc.core.network.callback.IApiErrorCallback
import com.siasun.dianshi.bean.CleanAreaNew
import com.siasun.dianshi.bean.CleanAreaRootNew
import com.siasun.dianshi.bean.CmsStation
import com.siasun.dianshi.bean.CmsWorkAreasListRoot
import com.siasun.dianshi.bean.ElevatorPoint
import com.siasun.dianshi.bean.InitPoseRoot
import com.siasun.dianshi.bean.MachineStation
import com.siasun.dianshi.bean.MergedPoseBean
import com.siasun.dianshi.bean.RequestSaveArea
import com.siasun.dianshi.bean.RequestSaveCmsWorkArea
import com.siasun.dianshi.bean.SpArea
import com.siasun.dianshi.bean.WorkAreasNew
import com.siasun.dianshi.bean.toUpload
import com.siasun.dianshi.network.manager.ApiManager
import com.siasun.dianshi.network.request.RequestGetSpecialArea
import com.siasun.dianshi.network.request.RequestSaveSpecialArea
import com.siasun.dianshi.network.request.RequestSaveVirtualWall
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


/**
 * @Author: MJ
 * @Date: 2025/8/22
 * @Description:
 */
class ShowMapViewModel : BaseViewModel() {

    val saveAreaLiveDate = MutableLiveData<Boolean>()
    val getAreaListDate = MutableLiveData<CleanAreaRootNew>()

    val saveVirtualWall = MutableLiveData<Boolean>()
    val getVirtualWall = MutableLiveData<VirtualWallNew>()

    val saveMachineStationLiveData = MutableLiveData<Boolean>()
    val saveCmsStationLiveData = MutableLiveData<Boolean>()

    val saveSpecialArea = MutableLiveData<Boolean>()
    val getSpecialArea = MutableLiveData<MutableList<SpArea>>()


    val saveWorksArea = MutableLiveData<Boolean>()


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

    /**
     * 获取称梯点
     */
    fun getCmsElevator(
        layerId: Int, onComplete: (cmsStations: MutableList<ElevatorPoint>?) -> Unit
    ) {
        launchUIWithResult(responseBlock = {
            ApiManager.api.getCmsElevator(RequestCommonMapId(layerId))
        }, errorCall = object : IApiErrorCallback {
            override fun onError(code: Int?, error: String?) {
                super.onError(code, error)
            }
        }, successBlock = {
            it?.let {
                onComplete.invoke(it.elevators)
            }
        })
    }


    /**
     * 保存特殊区域
     */
    fun saveSpecialArea(layerId: Int, specialArea: MutableList<SpArea>) {
        launchUIWithResult(responseBlock = {
            val requestSpecialArea = RequestSaveSpecialArea(layerId, specialArea)
            ApiManager.api.setSpecialArea(requestSpecialArea)
        }, errorCall = object : IApiErrorCallback {
            override fun onError(code: Int?, error: String?) {
                saveSpecialArea.postValue(false)
            }
        }, successBlock = {
            saveSpecialArea.postValue(true)
        })
    }

    /**
     * 获取特殊区域
     */
    fun getSpecialArea(layerId: Int, areaType: Int) {
        launchUIWithResult(responseBlock = {
            val requestGetSpecialArea = RequestGetSpecialArea(layerId, areaType)
            ApiManager.api.getSpecialArea(requestGetSpecialArea)
        }, errorCall = object : IApiErrorCallback {
            override fun onError(code: Int?, error: String?) {
                super.onError(code, error)
            }
        }, successBlock = {
            it?.let {
                getSpecialArea.postValue(it)
            }
        })
    }

    /**
     * 获取区域列表
     */
    fun getAreaList(layerId: Int) {
        launchUIWithResult(responseBlock = {
            val requestGetArea = RequestCommonMapId(layerId)
            ApiManager.api.getAreas(requestGetArea)
        }, errorCall = object : IApiErrorCallback {
            override fun onError(code: Int?, error: String?) {
                super.onError(code, error)
            }
        }, successBlock = {
            it?.let {
                getAreaListDate.postValue(it)
            }
        })
    }

    /**
     * 保存区域
     */
    fun saveArea(layerId: Int, cleanAreas: MutableList<CleanAreaNew>) {
        launchUIWithResult(responseBlock = {
            val requestSaveArea = RequestSaveArea(layerId, CleanAreaRootNew(cleanAreas).toUpload())
            ApiManager.api.saveAreas(requestSaveArea)
        }, errorCall = object : IApiErrorCallback {
            override fun onError(code: Int?, error: String?) {
                saveAreaLiveDate.postValue(false)
            }
        }, successBlock = {
            it?.let {
                saveAreaLiveDate.postValue(true)
            }

        })

    }


    /**
     * 获取混行区页面需要的数据
     */
    fun getMixAreaData(
        layerId: Int, onComplete: (workList: CmsWorkAreasListRoot?) -> Unit
    ) {
        viewModelScope.launch {

            val workAreasNewDeferred = async {
                ApiManager.api.getCmsWorkAreas(RequestCommonMapId(layerId))
            }

            val workAreasNew = workAreasNewDeferred.await()

            onComplete.invoke(
                workAreasNew.data
            )
        }
    }
    /**
     * 保存混行区
     */
    fun saveWorkAreaList(layerId: Int, workAreasNew: MutableList<WorkAreasNew>) {
        launchUIWithResult(responseBlock = {
            val requestSaveArea =
                RequestSaveCmsWorkArea(layerId, CmsWorkAreasListRoot(workAreasNew).toUpload())
            ApiManager.api.saveCmsWorkAreas(requestSaveArea)
        }, errorCall = object : IApiErrorCallback {
            override fun onError(code: Int?, error: String?) {
                saveWorksArea.postValue(false)
            }
        }, successBlock = {
            saveWorksArea.postValue(true)
        })
    }

}