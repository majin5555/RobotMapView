//package com.siasun.dianshi.xpop
//
//import android.content.Context
//import com.lxj.xpopup.core.CenterPopupView
//import com.pnc.core.framework.ext.onClick
//import com.software.siasun.cleanrobot.crl.R
//import com.software.siasun.cleanrobot.crl.databinding.DialogVmChoiceBinding
//
///******************************************
// * 类描述：
// *
// * @author: why
// * @time: 2025/8/11 14:28
// ******************************************/
//
//class VmChoiceDialog(context: Context) : CenterPopupView(context) {
//
//    private lateinit var binding: DialogVmChoiceBinding
//
//    var onConfirmCall: ((selectType: Int) -> Unit)? = null
//    var onCancelCall: (() -> Unit)? = null
//
//    private var selectType: Int = 1
//
//    override fun getImplLayoutId(): Int {
//        return R.layout.dialog_vm_choice
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        binding = DialogVmChoiceBinding.bind(popupImplView)
//
//        binding.rgType.setOnCheckedChangeListener { _, checkedId ->
//            when (checkedId) {
//                R.id.rb_1 -> selectType = 1
//
//                R.id.rb_2 -> selectType = 2
//
//                R.id.rb_3 -> selectType = 3
//
//            }
//        }
//
//        binding.btnDismiss.onClick {
//            dialog.dismiss()
//            onCancelCall?.invoke()
//        }
//
//        binding.btnSure.onClick {
//            dialog.dismiss()
//            onConfirmCall?.invoke(selectType)
//
//        }
//    }
//
//}