package com.pnc.core.network.interceptor

import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.network.constant.KEY_SAVE_USER_LOGIN
import com.siasun.dianshi.network.constant.KEY_SAVE_USER_REGISTER
import com.siasun.dianshi.network.constant.KEY_SET_COOKIE
import com.siasun.dianshi.network.manager.CookiesManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * @author fuxing.che
 * @date   2023/3/27 07:26
 * @desc   Cookies拦截器
 */
class CookiesInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val newBuilder = request.newBuilder()

        val response = chain.proceed(newBuilder.build())
        val url = request.url.toString()
        val host = request.url.host

        // set-cookie maybe has multi, login to save cookie
        if ((url.contains(KEY_SAVE_USER_LOGIN) || url.contains(KEY_SAVE_USER_REGISTER))
                && response.headers(KEY_SET_COOKIE).isNotEmpty()
        ) {
            val cookies = response.headers(KEY_SET_COOKIE)
            val cookiesStr = CookiesManager.encodeCookie(cookies)
            CookiesManager.saveCookies(cookiesStr)
            LogUtil.e("CookiesInterceptor:cookies:$cookies", tag = "smy")
        }
        return response
    }
}