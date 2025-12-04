//package com.siasun.dianshi.xpop
//
//import VirtualWallLineNew
//import android.content.Context
//import android.widget.RadioButton
//import androidx.core.view.get
//import com.lxj.xpopup.core.CenterPopupView
//import com.pnc.core.framework.ext.onClick
//import com.software.siasun.cleanrobot.crl.R
//import com.software.siasun.cleanrobot.crl.databinding.DialogEditVmWallBinding
//
///******************************************
// * 类描述：
// *
// * @author: why
// * @time: 2025/8/11 14:10
// ******************************************/
//
//class VmWallEditDialog(context: Context, private val virtualWallLine: VirtualWallLineNew) :CenterPopupView(context) {
//
//    private lateinit var binding: DialogEditVmWallBinding
//    private var config: Int = -1
//
//    var editVmWallListener: ((virtualWallLine: VirtualWallLineNew) -> Unit)? = null
//    override fun getImplLayoutId(): Int {
//        return R.layout.dialog_edit_vm_wall
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//
//        binding = DialogEditVmWallBinding.bind(popupImplView)
//
//        binding.rgType.also {
//            config = virtualWallLine.CONFIG
//            when(config) {
//                1 -> {
//                    (it[0] as RadioButton).isChecked = true
//                }
//
//                2 -> {
//                    (it[1] as RadioButton).isChecked = true
//                }
//
//                3 -> {
//                    (it[2] as RadioButton).isChecked = true
//                }
//            }
//
//            it.setOnCheckedChangeListener { group, checkedId ->
//                val tag = group.findViewById<RadioButton>(checkedId).tag as String
//                config = tag.toInt()
//            }
//        }
//
//
//        binding.imgClose.onClick {
//            virtualWallLine.isSelect = false
//            editVmWallListener?.invoke(virtualWallLine)
//            dialog.dismiss()
//        }
//
//        binding.btnSure.onClick {
//            virtualWallLine.CONFIG = config
//            virtualWallLine.isSelect = true
//            editVmWallListener?.invoke(virtualWallLine)
//            dialog.dismiss()
//        }
//
//    }
//}