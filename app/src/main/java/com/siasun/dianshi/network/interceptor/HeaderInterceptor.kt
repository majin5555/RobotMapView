package com.pnc.core.network.interceptor

import com.siasun.dianshi.framework.log.LogUtil
import com.pnc.core.network.constant.ARTICLE_WEBSITE
import com.pnc.core.network.constant.COIN_WEBSITE
import com.pnc.core.network.constant.COLLECTION_WEBSITE
import com.pnc.core.network.constant.KEY_COOKIE
import com.pnc.core.network.constant.NOT_COLLECTION_WEBSITE
import com.siasun.dianshi.network.manager.CookiesManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * @author fuxing.che
 * @date   2023/3/27 07:25
 * @desc   头信息拦截器
 * 添加头信息
 */
class HeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val newBuilder = request.newBuilder()
        newBuilder.addHeader("Content-type", "application/json; charset=utf-8")

        val host = request.url.host
        val url = request.url.toString()

        //给有需要的接口添加Cookies
        if (host.isNotEmpty() && (url.contains(COLLECTION_WEBSITE)
                        || url.contains(NOT_COLLECTION_WEBSITE)
                        || url.contains(ARTICLE_WEBSITE)
                        || url.contains(COIN_WEBSITE))) {
            val cookies = CookiesManager.getCookies()
            LogUtil.e("HeaderInterceptor:cookies:$cookies", tag = "smy")
            if (!cookies.isNullOrEmpty()) {
                newBuilder.addHeader(KEY_COOKIE, cookies)
            }
        }
        return chain.proceed(newBuilder.build())
    }
}