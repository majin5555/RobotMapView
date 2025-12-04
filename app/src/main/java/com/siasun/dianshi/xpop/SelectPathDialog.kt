//package com.siasun.dianshi.xpop
//
//import android.content.Context
//import com.lxj.xpopup.core.CenterPopupView
//import com.pnc.core.framework.ext.onClick
//import com.software.siasun.cleanrobot.crl.R
//import com.software.siasun.cleanrobot.crl.databinding.DialogTeachSelectPathBinding
//
///******************************************
// * 类描述：
// *
// * @author: why
// * @time: 2025/8/7 15:49
// ******************************************/
//
//class SelectPathDialog(context: Context) :CenterPopupView(context) {
//
//    private lateinit var binding:DialogTeachSelectPathBinding
//    private var path: Int = 1
//
//    var onConfirmCall: ((path: Int) -> Unit)? = null
//    override fun getImplLayoutId(): Int {
//        return R.layout.dialog_teach_select_path
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        binding = DialogTeachSelectPathBinding.bind(popupImplView)
//
//        binding.rgTabType.setOnCheckedChangeListener { radioGroup, checkedId ->
//            when (checkedId) {
//                R.id.rb_dan -> path = 1
//                R.id.rb_shuang -> path = 3
//            }
//        }
//
//        binding.btnDismiss.onClick {
//                dismiss()
//        }
//
//
//        binding.btnSure.onClick {
//            onConfirmCall?.invoke(path)
//           dismiss()
//        }
//
//    }
//}