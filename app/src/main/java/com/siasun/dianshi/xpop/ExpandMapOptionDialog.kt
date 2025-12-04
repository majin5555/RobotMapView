//package com.siasun.dianshi.xpop
//
//import android.content.Context
//import com.lxj.xpopup.core.CenterPopupView
//import com.pnc.core.framework.ext.onClick
//import com.software.siasun.cleanrobot.crl.R
//import com.software.siasun.cleanrobot.crl.databinding.DialogExpandMapOptionBinding
//
///******************************************
// * 类描述：
// *
// * @author: why
// * @time: 2025/7/16 08:22
// ******************************************/
//
//class ExpandMapOptionDialog(context: Context) : CenterPopupView(context) {
//
//    private lateinit var binding: DialogExpandMapOptionBinding
//
//    var onSelectorCall: ((selectType: Int) -> Unit)? = null
//
//    private var selectType = 1
//    override fun getImplLayoutId(): Int {
//        return R.layout.dialog_expand_map_option
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        binding = DialogExpandMapOptionBinding.bind(popupImplView)
//
//        binding.rgTabType.also {
//            it.setOnCheckedChangeListener { _, checkedId ->
//                when (checkedId) {
//                    R.id.rb_yes -> selectType = 1
//                    R.id.rb_no -> selectType = 2
//                }
//            }
//        }
//
//        //取消
//        binding.btnDismiss.onClick {
//            dismiss()
//        }
//        //确定
//        binding.btnSure.onClick {
//            onSelectorCall?.invoke(selectType)
//            dismiss()
//        }
//    }
//}