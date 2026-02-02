package com.siasun.dianshi.mapviewdemo.viewmodel

import androidx.lifecycle.viewModelScope
import com.jeremyliao.liveeventbus.LiveEventBus
import com.siasun.dianshi.ConstantBase.PAD_MAP_NAME_PNG
import com.siasun.dianshi.ConstantBase.PAD_MAP_NAME_YAML
import com.siasun.dianshi.ConstantBase.getFolderPath
import com.siasun.dianshi.ConstantBase.getMRC05FolderPath
import com.siasun.dianshi.bean.UpdateMapBean
import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.ftp.FTPManager
import com.siasun.dianshi.mapviewdemo.KEY_UPDATE_MAP
import com.siasun.dianshi.mapviewdemo.TAG_NAV
import com.siasun.dianshi.mapviewdemo.utils.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class CreateMap2DViewModel : BaseViewModel() {
    /**
     * 下载地图
     */
    fun downPngYaml(type: Int, mapId: Int,) {

        viewModelScope.launch(Dispatchers.IO) {

            if (FileUtil.createOrExistsDir(getFolderPath(mapId))) {
                //下载yaml
                val downloadPmYaml = FTPManager.getInstance().downloadFile(
                    getMRC05FolderPath(mapId) + File.separator,
                    PAD_MAP_NAME_YAML,
                    getFolderPath(mapId)
                )
                LogUtil.i("下载PM.yaml文件 -> $downloadPmYaml", null, TAG_NAV)
                //下载png
                val downloadPmPng = FTPManager.getInstance().downloadFile(
                    getMRC05FolderPath(mapId) + File.separator,
                    PAD_MAP_NAME_PNG,
                    getFolderPath(mapId)
                )
                LogUtil.i("下载PM.png文件 -> $downloadPmPng", null, TAG_NAV)

                //有一个失败就认为是失败
                if (!downloadPmYaml || !downloadPmPng) LiveEventBus.get(
                    KEY_UPDATE_MAP, UpdateMapBean::class.java
                ).post(UpdateMapBean(false, type))

                //全部成功才是成功
                if (downloadPmYaml && downloadPmPng) {
                    //通知页面更新地图
                    LiveEventBus.get(KEY_UPDATE_MAP, UpdateMapBean::class.java)
                        .post(UpdateMapBean(true, type))


                    LogUtil.i("PM.yaml  && PM.png 下载成功")
                }
            }
        }
    }


}