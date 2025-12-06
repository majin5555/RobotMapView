package com.siasun.dianshi.network.manager

import com.siasun.dianshi.network.api.ApiInterface

/**
 * @author fuxing.che
 * @date   2023/2/27 21:14
 * @desc   API管理器
 */
object ApiManager {
    val api by lazy { HttpManager.create(ApiInterface::class.java) }
}