package com.siasun.dianshi.mapviewdemo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pnc.core.network.callback.IApiErrorCallback
import com.siasun.dianshi.bean.LogBean
import com.siasun.dianshi.framework.toast.TipsToast
import com.siasun.dianshi.network.manager.ApiManager
import com.siasun.dianshi.network.request.RequestDeleteErrorList
import com.siasun.dianshi.network.request.RequestGetLog

/**
 * @Author: mj
 * @Date: 2025/02/19
 * @Description: 日志ViewModel
 */
class LogViewModel : BaseViewModel() {
    val logLiveData = MutableLiveData<MutableList<LogBean>?>()

    /**
     * @description 获取日志
     * @author Majin
     * @since 2025/01/19
     */
    fun getLog(errorLevel: RequestGetLog): LiveData<MutableList<LogBean>?> {
        launchUI(errorBlock = { _, error ->
            TipsToast.showTips(error)
            logLiveData.value = mutableListOf<LogBean>()
        }) {
            val data = ApiManager.api.getLog(errorLevel)?.data
            data?.apply {
                logLiveData.value = data
            }
        }
        return logLiveData
    }

    /**
     * @description 删除日志
     * @author Majin
     */
    fun deleteErrorList(req: RequestDeleteErrorList, onComplete: (type: Int) -> Unit) {
        launchUIWithResult(responseBlock = {
            ApiManager.api.deleteErrorList(req)
        }, errorCall = object : IApiErrorCallback {
            override fun onError(code: Int?, error: String?) {
                TipsToast.showTips(error)
                onComplete.invoke(0)
            }
        }, successBlock = {
            onComplete.invoke(1)
        })
    }
}
