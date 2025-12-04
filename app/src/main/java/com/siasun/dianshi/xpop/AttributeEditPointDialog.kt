//package com.siasun.dianshi.xpop
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.widget.RadioButton
//import androidx.core.view.get
//import com.lxj.xpopup.core.CenterPopupView
//import com.pnc.core.framework.ext.onClick
//import com.software.siasun.cleanrobot.crl.R
//import com.software.siasun.cleanrobot.crl.databinding.DialogAttributeEditPointBinding
//import yourlib.attr.NodeBaseAttr
//import java.text.DecimalFormat
//import kotlin.experimental.and
//import kotlin.math.pow
//
///******************************************
// * 类描述：
// *
// * @author: why
// * @time: 2025/8/8 15:12
// ******************************************/
//
//class AttributeEditPointDialog(context: Context, private val nodeBaseAttr: NodeBaseAttr) :
//    CenterPopupView(context) {
//
//    private lateinit var binding: DialogAttributeEditPointBinding
//
//    private val df = DecimalFormat("0.0000")
//    private val chNodeType by lazy {
//        arrayOf(
//            binding.checkNodeType1,
//            binding.checkNodeType2,
//            binding.checkNodeType3,
//            binding.checkNodeType4,
//            binding.checkNodeType5,
//            binding.checkNodeType6,  // ⚠️ 重复了，确认是否为 checkNodeType6？
//            binding.checkNodeType7,
//            binding.checkNodeType8,
//            binding.checkNodeType9,
//            binding.checkNodeType10
//        )
//    }
//    private val chAvgType by lazy {
//        arrayOf(
//            binding.ckAgvType1,
//            binding.ckAgvType2,
//            binding.ckAgvType3,
//            binding.ckAgvType4,
//            binding.ckAgvType5,
//            binding.ckAgvType6,
//            binding.ckAgvType7,
//            binding.ckAgvType8,
//        )
//
//    }
//
//    var onConfirmCall: ((nodeBaseAttr: NodeBaseAttr) -> Unit)? = null
//
//    override fun getImplLayoutId(): Int {
//        return R.layout.dialog_attribute_edit_point
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        binding = DialogAttributeEditPointBinding.bind(popupImplView)
//
//        initParam()
//
//        binding.btnDismiss.onClick {
//            dismiss()
//        }
//
//        //确定
//        binding.btnSure.onClick {
//            if (binding.tvPointX.text.toString() == "" || binding.tvPointY.text.toString() == "") {
//                return@onClick
//            }
//            nodeBaseAttr.x = (binding.tvPointX.text.toString()).toDouble()
//            nodeBaseAttr.y = (binding.tvPointY.text.toString()).toDouble()
//            nodeBaseAttr.m_uOnLine =
//                if ((binding.rgTabOnline[0] as RadioButton).isChecked) 1 else 0
//
//            var nodeType = 0
//            for (num in 0..9) if (chNodeType[num].isChecked) nodeType += 2.0.pow(num)
//                .toInt()
//
//            var agvType = 0
//            for (num in 0..7) if (chAvgType[num].isChecked) agvType += 2.0.pow(num)
//                .toInt()
//
//            nodeBaseAttr.m_uType = nodeType.toShort()
//            nodeBaseAttr.m_uCarrierType = agvType.toShort()
//            onConfirmCall?.invoke(nodeBaseAttr)
//
//            dialog.dismiss()
//        }
//
//
//    }
//
//    private fun initParam() {
//
//        setTitle()
//        setXY()
//        setOnline()
//        setNoteType()
//        setAgvType()
//    }
//
//    @SuppressLint("SetTextI18n")
//    private fun setTitle() {
//        binding.title.text =
//            "${nodeBaseAttr.m_uId}${context.getString(R.string.num)}${context.getString(R.string.attribute_edit_point_title)}"
//    }
//
//
//    /**
//     * 设置XY 点坐标
//     */
//    private fun setXY() {
//        binding.tvPointX.setText(df.format(nodeBaseAttr.x).toString())
//        binding.tvPointY.setText(df.format(nodeBaseAttr.y).toString())
//    }
//
//    /**
//     * 允许上线
//     */
//    private fun setOnline() {
//        if (nodeBaseAttr.m_uOnLine == 1.toShort()) (binding.rgTabOnline[0] as RadioButton).isChecked =
//            true
//        else (binding.rgTabOnline[1] as RadioButton).isChecked = true
//    }
//
//    /**
//     * 节点类型
//     */
//    private fun setNoteType() {
//        var temp = 0
//        for (i in 0..9) {
//            temp = 2.0.pow(i).toInt()
//            if (nodeBaseAttr.m_uType and temp.toShort() == temp.toShort()) chNodeType[i].isChecked =
//                true
//        }
//    }
//
//    /**
//     * 车辆类型
//     */
//    private fun setAgvType() {
//        var temp = 0
//        for (i in 0..7) {
//            temp = 2.0.pow(i).toInt()
//            if (nodeBaseAttr.m_uCarrierType and temp.toShort() == temp.toShort()) chAvgType[i].isChecked =
//                true
//        }
//    }
//
//}