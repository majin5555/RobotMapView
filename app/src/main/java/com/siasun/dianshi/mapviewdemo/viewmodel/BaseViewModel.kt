package com.siasun.dianshi.mapviewdemo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pnc.core.network.callback.IApiErrorCallback
import com.pnc.core.network.error.ApiException
import com.pnc.core.network.error.ERROR
import com.pnc.core.network.error.ExceptionHandler
import com.pnc.core.network.flow.requestFlow
import com.pnc.core.network.response.BaseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * @author fuxing.che
 * @date   2023/2/24 13:03
 * @desc   viewModel基类
 */
open class BaseViewModel : ViewModel() {
    /**
     * 运行在主线程中，可直接调用
     * @param errorBlock 错误回调
     * @param responseBlock 请求函数
     */
    fun launchUI(errorBlock: (Int?, String?) -> Unit, responseBlock: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.Main) {
            safeApiCall(errorBlock = errorBlock, responseBlock)
        }
    }

    /**
     * 需要运行在协程作用域中
     * @param errorBlock 错误回调
     * @param responseBlock 请求函数
     */
    private suspend fun <T> safeApiCall(
        errorBlock: suspend (Int?, String?) -> Unit,
        responseBlock: suspend () -> T?
    ): T? {
        try {
            return responseBlock()
        } catch (e: Exception) {
            e.printStackTrace()
            val exception = ExceptionHandler.handleException(e)
            errorBlock(exception.errCode, exception.errMsg)
        }
        return null
    }

    /**
     * 不依赖BaseRepository，运行在主线程中，可直接调用
     * @param errorCall 错误回调
     * @param responseBlock 请求函数
     * @param successBlock 请求回调
     */
    fun <T> launchUIWithResult(
        responseBlock: suspend () -> BaseResponse<T>?,
        errorCall: IApiErrorCallback?,
        successBlock: (T?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            val result = safeApiCallWithResult(errorCall = errorCall, responseBlock)
            successBlock(result)
        }
    }

    /**
     * 不依赖BaseRepository，需要在作用域中运行
     * @param errorCall 错误回调
     * @param responseBlock 请求函数
     */
    private suspend fun <T> safeApiCallWithResult(
        errorCall: IApiErrorCallback?,
        responseBlock: suspend () -> BaseResponse<T>?
    ): T? {
        try {
            val response = withContext(Dispatchers.IO) {
                withTimeout(60 * 1000) {
                    responseBlock()
                }
            } ?: return null
            if (response.isFailed()) {
                throw ApiException(response.errorCode, response.errorMsg)
            }
            return response.data
        } catch (e: Exception) {
            e.printStackTrace()
            val exception = ExceptionHandler.handleException(e)
            if (ERROR.UNLOGIN.code == exception.errCode) {
                errorCall?.onLoginFail(exception.errCode, exception.errMsg)
            } else {
                errorCall?.onError(exception.errCode, exception.errMsg)
            }
        }
        return null
    }

    /**
     * flow 运行在主线程中，可直接调用
     * @param errorCall 错误回调
     * @param requestCall 请求函数
     * @param showLoading 是否展示加载框
     * @param successBlock 请求结果
     */
    fun <T> launchFlow(
        errorCall: IApiErrorCallback? = null,
        requestCall: suspend () -> BaseResponse<T>?,
        showLoading: ((Boolean) -> Unit)? = null,
        successBlock: (T?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            val data = requestFlow(errorBlock = { code, error ->
                if (ERROR.UNLOGIN.code == code) {
                    errorCall?.onLoginFail(code, error)
                } else {
                    errorCall?.onError(code, error)
                }
            }, requestCall, showLoading)
            successBlock(data)
        }
    }
}