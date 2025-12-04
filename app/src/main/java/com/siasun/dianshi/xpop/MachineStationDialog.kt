//package com.siasun.dianshi.xpop
//
//import android.content.Context
//import android.os.Build
//import androidx.annotation.RequiresApi
//import bean.cms_config.Gate
//import bean.cms_config.Locate
//import bean.cms_config.MachineStation
//import bean.cms_config.MachineWait
//import bean.cms_config.StationCoordinate
//import com.lxj.xpopup.core.CenterPopupView
//import com.pnc.core.framework.ext.gone
//import com.pnc.core.framework.ext.onClick
//import com.pnc.core.framework.ext.visible
//import com.software.siasun.cleanrobot.crl.R
//import com.software.siasun.cleanrobot.crl.databinding.DialogMachineStationBinding
//import com.software.siasun.cleanrobot.crl.scene.station.MachineStationActivity
//import com.software.siasun.cleanrobot.util.requireIntInRange
//import com.software.siasun.cleanrobot.util.requireNotEmpty
//import kotlin.random.Random
//
///******************************************
// * 类描述：
// *
// * @author: why
// * @time: 2025/8/18 15:29
// ******************************************/
//
//@RequiresApi(Build.VERSION_CODES.R)
//class MachineStationDialog(
//    context: Context,
//    private val mapId: Int,
//    private val machineStation: MachineStation?
//) : CenterPopupView(context) {
//
//    private lateinit var binding: DialogMachineStationBinding
//    private var chargeType = 2
//
//    var onConfirmCall: ((machineStation: MachineStation?) -> Unit)? = null
//    var onDeleteCall: (() -> Unit)? = null
//    override fun getImplLayoutId(): Int {
//        return R.layout.dialog_machine_station
//    }
//
//
//    override fun onCreate() {
//        super.onCreate()
//
//        binding = DialogMachineStationBinding.bind(popupImplView)
//
//        machineStation?.let {
//            binding.etLocX.setText(it.locate?.x?.toString())
//            binding.etLocY.setText(it.locate?.y?.toString())
//            binding.etLocTheta.setText(it.locate?.theta?.toString())
//
//            binding.etPreLocX.setText(it.gate?.x.toString())
//            binding.etPreLocY.setText(it.gate?.y.toString())
//            binding.etPreLocTheta.setText(it.gate?.theta.toString())
//
//            binding.etLoraId.setText(it.loraID.toString())
//
//            it.wait?.let { w ->
//
//                binding.ckWaitPoint.isChecked = true
//                binding.waitRoot.visible()
//                binding.linWaitTime.visible()
//
//                binding.etWaitPointX.setText(w.coordinate?.x.toString())
//                binding.etWaitPointY.setText(w.coordinate?.y.toString())
//                binding.etWaitPointT.setText(w.coordinate?.theta.toString())
//                binding.etWaitTime.setText(w.time.toString())
//
//            }
//
//            it.finish?.let { f ->
//                binding.ckFinishPoint.isChecked = true
//                binding.finishRoot.visible()
//                binding.etFinishPointX.setText(f.x.toString())
//                binding.etFinishPointY.setText(f.y.toString())
//                binding.etFinishPointT.setText(f.theta.toString())
//            }
//
//            chargeType = it.charge
//
//            if (chargeType == 1) {
//                binding.btnCharge.isChecked = true
//            } else {
//                binding.btnChargeWater.isChecked = true
//            }
//
//            binding.btnDelete.visible()
//        }
//
//        binding.btnGetLoc.onClick {
//            (context as? MachineStationActivity)?.let { activity ->
//                with(binding) {
//                    etLocX.setText("${(activity.mAgvX).toFloat()}")
//                    etLocY.setText("${(activity.mAgvY).toFloat()}")
//                    etLocTheta.setText("${(activity.mAgvTheta).toFloat()}")
//                }
//            }
//        }
//
//        binding.btnGetPreLoc.onClick {
//            (context as? MachineStationActivity)?.let { activity ->
//                with(binding) {
//                    etPreLocX.setText("${(activity.mAgvX).toFloat()}")
//                    etPreLocY.setText("${(activity.mAgvY).toFloat()}")
//                    etPreLocTheta.setText("${(activity.mAgvTheta).toFloat()}")
//                }
//            }
//        }
//        binding.btnWaitPoint.onClick {
//            (context as? MachineStationActivity)?.let { activity ->
//                with(binding) {
//                    etWaitPointX.setText("${(activity.mAgvX).toFloat()}")
//                    etWaitPointY.setText("${(activity.mAgvY).toFloat()}")
//                    etWaitPointT.setText("${(activity.mAgvTheta).toFloat()}")
//                }
//            }
//        }
//        binding.btnFinishPoint.onClick {
//            (context as? MachineStationActivity)?.let { activity ->
//                with(binding) {
//                    etFinishPointX.setText("${(activity.mAgvX).toFloat()}")
//                    etFinishPointY.setText("${(activity.mAgvY).toFloat()}")
//                    etFinishPointT.setText("${(activity.mAgvTheta).toFloat()}")
//                }
//            }
//        }
//
//        binding.ckWaitPoint.setOnCheckedChangeListener { _, isChecked ->
//            if (isChecked) {
//                binding.waitRoot.visible()
//                binding.linWaitTime.visible()
//            } else {
//                binding.waitRoot.gone()
//                binding.linWaitTime.gone()
//            }
//        }
//
//        binding.ckFinishPoint.setOnCheckedChangeListener { _, isChecked ->
//            if (isChecked) {
//                binding.finishRoot.visible()
//            } else {
//                binding.finishRoot.gone()
//            }
//        }
//
//
//        binding.rootCharge.setOnCheckedChangeListener { group, checkedId ->
//            when (checkedId) {
//                R.id.btn_charge -> {
//                    chargeType = 1
//                }
//
//                R.id.btn_charge_water -> {
//                    chargeType = 2
//                }
//            }
//        }
//
//        binding.btnCancel.onClick {
//            dismiss()
//        }
//
//        binding.btnDelete.onClick {
//            onDeleteCall?.invoke()
//            dismiss()
//        }
//
//        binding.btnConfirm.onClick {
//            with(binding) {
//                // 对接点
//                if (!etLocX.requireNotEmpty(context.getString(R.string.machine_locate_input_e)) ||
//                    !etLocY.requireNotEmpty(context.getString(R.string.machine_locate_input_e)) ||
//                    !etLocTheta.requireNotEmpty(context.getString(R.string.machine_locate_input_e))
//                ) return@onClick
//
//                // 准备点
//                if (!etPreLocX.requireNotEmpty(context.getString(R.string.machine_gate_input_e)) ||
//                    !etPreLocY.requireNotEmpty(context.getString(R.string.machine_gate_input_e)) ||
//                    !etPreLocTheta.requireNotEmpty(context.getString(R.string.machine_gate_input_e))
//                ) return@onClick
//
//                // LoraId
//                if (!etLoraId.requireNotEmpty(context.getString(R.string.machine_loraId_input_e)))
//                    return@onClick
//                if (!etLoraId.requireIntInRange(1..255, context.getString(R.string.lora_id_range)))
//                    return@onClick
//
//                // 等待点
//                if (ckWaitPoint.isChecked) {
//                    if (!etWaitPointT.requireNotEmpty(context.getString(R.string.machine_wait_input_e)) ||
//                        !etWaitPointY.requireNotEmpty(context.getString(R.string.machine_wait_input_e)) ||
//                        !etWaitPointT.requireNotEmpty(context.getString(R.string.machine_wait_input_e))
//                    ) return@onClick
//
//                    if (!etWaitTime.requireNotEmpty(context.getString(R.string.machine_wait_time_input_e)))
//                        return@onClick
//
//                    if (!etWaitTime.requireIntInRange(
//                            0..2000,
//                            context.getString(R.string.machine_wait_time_input_e1)
//                        )
//                    )
//                        return@onClick
//                }
//
//                // 结束点
//                if (ckFinishPoint.isChecked) {
//                    if (!etFinishPointX.requireNotEmpty(context.getString(R.string.machine_finish_input_e)) ||
//                        !etFinishPointY.requireNotEmpty(context.getString(R.string.machine_finish_input_e)) ||
//                        !etFinishPointT.requireNotEmpty(context.getString(R.string.machine_finish_input_e))
//                    ) return@onClick
//                }
//
//                // 全部通过
//                saveMachineStation()
//                dismiss()
//            }
//        }
//
//    }
//
//    private fun saveMachineStation() {
//        val machineStation = MachineStation()
//        machineStation.id = Random.nextInt(60001).toString()
//        machineStation.mapId = mapId
//
//        val locate = Locate()
//        locate.x = binding.etLocX.text.toString().toFloat()
//        locate.y = binding.etLocY.text.toString().toFloat()
//        locate.theta = binding.etLocTheta.text.toString().toFloat()
//        machineStation.locate = locate
//
//        val gate = Gate()
//        gate.x = binding.etPreLocX.text.toString().toFloat()
//        gate.y = binding.etPreLocY.text.toString().toFloat()
//        gate.theta = binding.etPreLocTheta.text.toString().toFloat()
//        machineStation.gate = gate
//
//        machineStation.loraID = binding.etLoraId.text.toString().toInt()
//
//        if (binding.ckWaitPoint.isChecked) {
//            val wait = MachineWait()
//            wait.time = binding.etWaitTime.text.toString().toInt()
//            val coordinate = StationCoordinate(
//                binding.etWaitPointX.text.toString().toFloat(),
//                binding.etWaitPointY.text.toString().toFloat(),
//                binding.etWaitPointT.text.toString().toFloat(),
//            )
//            wait.coordinate = coordinate
//            machineStation.wait = wait
//        }
//
//        if (binding.ckFinishPoint.isChecked) {
//            val finish = StationCoordinate(
//                binding.etFinishPointX.text.toString().toFloat(),
//                binding.etFinishPointY.text.toString().toFloat(),
//                binding.etFinishPointT.text.toString().toFloat(),
//            )
//
//            machineStation.finish = finish
//        }
//
//        machineStation.charge = chargeType
//
//        onConfirmCall?.invoke(machineStation)
//
//    }
//}