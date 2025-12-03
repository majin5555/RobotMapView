package com.siasun.dianshi.mapviewdemo.viewmodel

import VirtualWallNew
import androidx.lifecycle.MutableLiveData
import com.siasun.dianshi.network.request.RequestCommonMapId
import com.pnc.core.network.callback.IApiErrorCallback
import com.siasun.dianshi.network.manager.ApiManager
import com.siasun.dianshi.network.request.RequestSaveVirtualWall


/**
 * @Author: MJ
 * @Date: 2025/8/22
 * @Description:
 */
class ShowMapViewModel : BaseViewModel() {
    val saveVirtualWall = MutableLiveData<Boolean>()
    val getVirtualWall = MutableLiveData<VirtualWallNew>()

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
}