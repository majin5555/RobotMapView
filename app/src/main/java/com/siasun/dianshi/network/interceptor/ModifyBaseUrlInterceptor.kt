package com.pnc.core.network.interceptor

import com.pnc.core.network.constant.HTTP_PORT
import com.pnc.core.network.constant.URL
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * @author fuxing.che
 * @date   2023/3/27 07:25
 * @desc   头信息拦截器
 * 添加头信息
 */
class ModifyBaseUrlInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // 获取request
        val request = chain.request()
        // 获取url
        val url = request.url

        val baseUrl = URL.toHttpUrlOrNull()
        val newBuilder = baseUrl?.let {
            url.newBuilder()
                .scheme(it.scheme)
                .host(it.host)
                .port(HTTP_PORT)
                .build()
        }
        return chain.proceed(request.newBuilder().url(newBuilder!!).build())
    }
}