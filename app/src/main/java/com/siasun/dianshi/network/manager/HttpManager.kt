package com.siasun.dianshi.network.manager

import android.util.Log
import com.siasun.dianshi.framework.helper.AppHelper
import com.siasun.dianshi.framework.utils.NetworkUtil
import com.siasun.dianshi.network.constant.BASE_URL
import com.pnc.core.network.error.NoNetWorkException
import com.pnc.core.network.error.ERROR
import com.pnc.core.network.interceptor.CookiesInterceptor
import com.pnc.core.network.interceptor.HeaderInterceptor
import com.pnc.core.network.interceptor.ModifyBaseUrlInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * @author fuxing.che
 * @date   2023/2/23 21:29
 * @desc  网络请求管理类
 */
object HttpManager {
    private val mRetrofit: Retrofit

    init {
        mRetrofit = Retrofit.Builder()
                .client(initOkHttpClient())
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }

    /**
     * 获取 apiService
     */
    fun <T> create(apiService: Class<T>): T {
        return mRetrofit.create(apiService)
    }

    /**
     * 初始化OkHttp
     */
    fun initOkHttpClient(): OkHttpClient {
        val build = OkHttpClient.Builder()
                .connectTimeout(6000, TimeUnit.SECONDS)
                .writeTimeout(6000, TimeUnit.SECONDS)
                .readTimeout(6000, TimeUnit.SECONDS)
        // 添加参数拦截器
        val interceptors = mutableListOf<Interceptor>()
        build.addInterceptor(ModifyBaseUrlInterceptor())
        build.addInterceptor(CookiesInterceptor())
        build.addInterceptor(HeaderInterceptor())

        //日志拦截器
        val logInterceptor = HttpLoggingInterceptor { message: String ->
            Log.i("okhttp", "data:$message")
        }
        if (AppHelper.isDebug()) {
            logInterceptor.level = HttpLoggingInterceptor.Level.BODY
        } else {
            logInterceptor.level = HttpLoggingInterceptor.Level.BASIC
        }
        build.addInterceptor(logInterceptor)
        //网络状态拦截
        build.addInterceptor(object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                if (NetworkUtil.isConnected(AppHelper.getApplication())) {
                    val request = chain.request()
                    return chain.proceed(request)
                } else {
                    throw NoNetWorkException(ERROR.NETWORD_ERROR)
                }
            }
        })
        return build.build()
    }
}