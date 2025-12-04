//package com.siasun.dianshi.xpop
//
//import android.content.Context
//import com.lxj.xpopup.core.BottomPopupView
//import com.pnc.core.framework.ext.onClick
//import com.software.siasun.cleanrobot.crl.R
//import com.software.siasun.cleanrobot.crl.databinding.DialogMapOptionBinding
//
///******************************************
// * 类描述：地图选项
// *
// * @author: why
// * @time: 2025/7/31 13:40
// ******************************************/
//
//class MapOptionDialog(context: Context, private val mapName: String) : BottomPopupView(context) {
//
//    private lateinit var binding: DialogMapOptionBinding
//
//    var onMapManagerCall: (() -> Unit)? = null
//    var onPathManagerCall: (() -> Unit)? = null
//    var onAreaManagerCall: (() -> Unit)? = null
//    var onStationManagerCall: (() -> Unit)? = null
//
//
//    override fun getImplLayoutId(): Int {
//        return R.layout.dialog_map_option
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        binding = DialogMapOptionBinding.bind(popupImplView)
//
//        binding.tvMapName.text =
//            "${context.resources.getString(R.string.current_map_name)}${mapName}"
//
//        binding.tvStationManager.onClick {
//            onStationManagerCall?.invoke()
//            dismiss()
//        }
//
//        binding.tvMapManager.onClick {
//            onMapManagerCall?.invoke()
//            dismiss()
//        }
//
//        binding.tvPathManager.onClick {
//            onPathManagerCall?.invoke()
//            dismiss()
//        }
//
//        binding.tvAreaManager.onClick {
//            onAreaManagerCall?.invoke()
//            dismiss()
//        }
//
//    }
//}