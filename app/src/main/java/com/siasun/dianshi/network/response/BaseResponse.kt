package com.pnc.core.network.response

/**
 * @author fuxing.che
 * @date   2023/2/24 13:10
 * @desc   通用数据类
 */
data class BaseResponse<out T>(
    val data: T?,
    val errorCode: Int = 200,//服务器状态码 这里0表示请求成功
    val errorMsg: String = ""//错误信息
) {

    /**
     * 判定接口返回是否正常
     */
    fun isFailed(): Boolean {
        return errorCode != 200
    }
}
