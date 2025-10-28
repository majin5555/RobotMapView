package com.pnc.core.network.constant

import com.tencent.mmkv.MMKV

/**
 * @author fuxing.che
 * @date   2023/3/27 07:58
 * @desc   Http常量类
 */

/**
 * base_url
 */
const val KEY_NEY_IP = "NET_IP"
const val HTTP_PORT = 9000
var IP = MMKV.defaultMMKV().decodeString(KEY_NEY_IP, "192.168.3.101")
var URL = "http://${IP}"
var BASE_URL = "http://${IP}:${HTTP_PORT}"

const val KEY_TOKEN = "token"
const val KEY_COOKIE = "Cookie"
const val KEY_SET_COOKIE = "set-cookie"

const val KEY_SAVE_USER_LOGIN = "user/login"
const val KEY_SAVE_USER_REGISTER = "user/register"

const val COLLECTION_WEBSITE = "lg/collect"
const val NOT_COLLECTION_WEBSITE = "lg/uncollect"
const val ARTICLE_WEBSITE = "article"
const val COIN_WEBSITE = "lg/coin"
