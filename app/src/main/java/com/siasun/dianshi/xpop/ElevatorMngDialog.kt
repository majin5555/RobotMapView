//package com.siasun.dianshi.xpop
//
//import android.content.Context
//import com.blankj.utilcode.util.ToastUtils
//
///******************************************
// * 类描述：
// *
// * @author: why
// * @time: 2025/8/12 14:58
// ******************************************/
//
//class ElevatorMngDialog(context: Context, private val elevatorBean: ElevatorBean?) :
//    CenterPopupView(context) {
//
//    private lateinit var binding: DialogElevatorMngBinding
//
//    var onConfirmCall: ((elevatorName: String, elevatorId: String, id: Int?) -> Unit)? = null
//    override fun getImplLayoutId(): Int {
//        return R.layout.dialog_elevator_mng
//    }
//
//    override fun onCreate() {
//        binding = DialogElevatorMngBinding.bind(popupImplView)
//
//        elevatorBean?.let {
//            binding.etElevatorName.setText(it.elevator_name)
//            binding.etElevatorId.setText(it.elevator_id)
//        }
//
//        binding.btnDismiss.onClick {
//            dialog.dismiss()
//        }
//        binding.btnSure.onClick {
//            val strElevatorName = binding.etElevatorName.text?.trim().toString()
//            val strElevatorID = binding.etElevatorId.text?.trim().toString()
//
//            if (strElevatorName.isEmpty()) {
//                ToastUtils.showShort(R.string.in_put_elevator_name)
//                return@onClick
//            }
//            if (strElevatorID.isEmpty()) {
//                ToastUtils.showShort(R.string.in_put_elevator_id)
//                return@onClick
//            }
//            onConfirmCall?.invoke(strElevatorName, strElevatorID, elevatorBean?.id)
//            dialog.dismiss()
//        }
//
//    }
//}