import com.alibaba.fastjson.annotation.JSONField
import com.jeremyliao.liveeventbus.core.LiveEvent
import com.siasun.dianshi.bean.PointNew

/**
 * 虚拟墙
 */
data class VirtualWallNew(
    var LAYERSUM: Int = 1,
    var LAYER: MutableList<VirWallLayerNew> = mutableListOf(
        VirWallLayerNew(
            mutableListOf(),
            0,
            0
        )
    )
) : LiveEvent


data class VirWallLayerNew(
    var LINE: MutableList<VirtualWallLineNew> = mutableListOf(),
    var LAYERNUM: Int,
    var LINESUM: Int
) : LiveEvent

/***
 * 1重点虚拟墙
 * 2虚拟门
 * 普通虚拟墙
 */
data class VirtualWallLineNew(
    var BEGIN: PointNew,
    var END: PointNew,
    var LINENUM: Int,
    var CONFIG: Int,
    @JSONField(serialize = false)
    var isSelect: Boolean = false
) : LiveEvent



