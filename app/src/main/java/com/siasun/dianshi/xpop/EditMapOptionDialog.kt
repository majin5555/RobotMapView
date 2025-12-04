//package com.siasun.dianshi.xpop
//
//import android.content.Context
//import com.lxj.xpopup.core.BottomPopupView
//import com.pnc.core.framework.ext.onClick
//import com.software.siasun.cleanrobot.crl.R
//import com.software.siasun.cleanrobot.crl.databinding.DialogEditMapOptionBinding
//
///******************************************
// * 类描述：编辑地图选项
// *
// * @author: why
// * @time: 2025/7/14 13:53
// ******************************************/
//
//class EditMapOptionDialog(context: Context, private val mapName: String) :
//    BottomPopupView(context) {
//
//    private lateinit var binding: DialogEditMapOptionBinding
//
//    var onClearCall: (() -> Unit)? = null
//    var onExtendCall: (() -> Unit)? = null
//    var onTopCall: (() -> Unit)? = null
//    var onRecordDxCall: (() -> Unit)? = null
//    var onPreviewMapCall: (() -> Unit)? = null
//    override fun getImplLayoutId(): Int {
//        return R.layout.dialog_edit_map_option
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        binding = DialogEditMapOptionBinding.bind(popupImplView)
//
//        binding.tvMapName.text = mapName
//
//        binding.tvClear.onClick {
//            onClearCall?.invoke()
//            dismiss()
//        }
//
//        binding.tvExtend.onClick {
//            onExtendCall?.invoke()
//            dismiss()
//        }
//
//        binding.tvTop.onClick {
//            onTopCall?.invoke()
//            dismiss()
//        }
//        binding.tvRecordDx.onClick {
//            onRecordDxCall?.invoke()
//            dismiss()
//        }
//
//        binding.tvPreview.onClick {
//            onPreviewMapCall?.invoke()
//            dismiss()
//        }
//    }
//}