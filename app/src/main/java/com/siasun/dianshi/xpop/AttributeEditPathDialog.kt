//package com.siasun.dianshi.xpop
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.widget.RadioButton
//import androidx.core.view.get
//import kotlin.experimental.and
//import kotlin.math.pow
//
///******************************************
// * 类描述：
// *
// * @author: why
// * @time: 2025/8/8 16:10
// ******************************************/
//
//class AttributeEditPathDialog(context: Context, private val pathBaseAttr: PathBaseAttr) :
//    CenterPopupView(context) {
//
//    private lateinit var binding: DialogAttributeEditPathBinding
//
//    var onConfirmCall: ((pathBaseAttr: PathBaseAttr) -> Unit)? = null
//
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
//    override fun getImplLayoutId(): Int {
//        return R.layout.dialog_attribute_edit_path
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        binding = DialogAttributeEditPathBinding.bind(popupImplView)
//
//
//        initParam()
//
//        binding.btnDismiss.onClick {
//            dismiss()
//        }
//
//        binding.btnSure.onClick {
//            val mPVel = binding.etPMaxVel.text.toString()
//            val mNVel = binding.etNMaxVel.text.toString()
//            val mExtendedAttribute = binding.etExtendedAttribute.text.toString().toFloat()
//
//            //正
//            if (mPVel.isEmpty() || mPVel.toFloat() < 0 || mPVel.toFloat() > 1f) {
//                context.toast(context.getString(R.string.speed))
//                return@onClick
//            }
//            //逆
//            if (mNVel.isEmpty() || mNVel.toFloat() < 0 || mNVel.toFloat() > 1f) {
//                context.toast(context.getString(R.string.speed))
//                return@onClick
//            }
//            //路段扩充属性
//            if (mExtendedAttribute < 0 || mExtendedAttribute > 254f) {
//                context.toast(context.getString(R.string.extendedAttribute))
//                return@onClick
//            }
//
//            pathBaseAttr.m_fVeloLimit[0] = mPVel.toFloat()
//            pathBaseAttr.m_fVeloLimit[1] = mNVel.toFloat()
//
//            pathBaseAttr.m_uExtType = mExtendedAttribute.toInt().toShort()
//
//            //前进切区
//            pathBaseAttr.m_uFwdRotoScannerObstacle =
//                (binding.spFwdFwd.selectedIndex + (binding.spFwdBk.selectedIndex shl 4) + (binding.spFwdLeft.selectedIndex shl 8) + (binding.spFwdRight.selectedIndex shl 12))
//
//            //后退切区
//            pathBaseAttr.m_uBwdRotoScannerObstacle =
//                ((binding.spBkFwd.selectedIndex and 15) + (binding.spBkBk.selectedIndex shl 4) + (binding.spBkLeft.selectedIndex shl 8) + (binding.spBkRight.selectedIndex shl 12))
//
//
//            //允许上线
//            pathBaseAttr.m_uOnLine =
//                if ((binding.rgTabOnline[0] as RadioButton).isChecked) 1 else 0
//            var agvType = 0
//            for (num in 0..7) if (chAvgType[num]!!.isChecked) agvType += 2.0.pow(num)
//                .toInt()
//            //车辆类型
//            pathBaseAttr.m_uCarrierType = agvType.toShort()
//            onConfirmCall?.invoke(pathBaseAttr)
//        }
//    }
//
//    /**
//     * 初始化信息
//     */
//    private fun initParam() {
//        setTitle()
//        setPathLength()
//        setEdit()
//        setPlS()
//        setGuiDirection()
//        setWalkDirection()
//        setOnline()
//        setAgvType()
//    }
//
//    /**
//     * 设置标题
//     */
//    @SuppressLint("SetTextI18n")
//    private fun setTitle() {
//        binding.title.text =
//            "${pathBaseAttr.m_uId}号${context.getString(R.string.attribute_edit_path_title)}"
//    }
//
//
//    /**
//     * 设置路段长度
//     */
//    @SuppressLint("SetTextI18n")
//    private fun setPathLength() {
//        binding.tvPathLength.text = "${pathBaseAttr.m_fSize}m"
//    }
//
//    /**
//     * 设置正向 逆向 扩充
//     */
//    private fun setEdit() {
//        binding.etPMaxVel.setText(pathBaseAttr.m_fVeloLimit[0].toString())
//        binding.etNMaxVel.setText(pathBaseAttr.m_fVeloLimit[1].toString())
//        //0-254
//        binding.etExtendedAttribute.setText(pathBaseAttr.m_uExtType.toString())
//    }
//
//    /**
//     * 设置PLS
//     */
//    private fun setPlS() {
//        val list = context.resources.getStringArray(R.array.pls_arr).asList() as MutableList
//        binding.spFwdFwd.attachDataSource(list)
//        binding.spFwdBk.attachDataSource(list)
//        binding.spFwdLeft.attachDataSource(list)
//        binding.spFwdRight.attachDataSource(list)
//
//        binding.spBkFwd.attachDataSource(list)
//        binding.spBkBk.attachDataSource(list)
//        binding.spBkLeft.attachDataSource(list)
//        binding.spBkRight.attachDataSource(list)
//
//        //前进切区
//        val fwdArea: Int = pathBaseAttr.m_uFwdRotoScannerObstacle
//        var temp = fwdArea and 15
//        if (temp == 15) {
//            temp = 0
//        }
//
//        binding.spFwdFwd.selectedIndex = temp
//
//        temp = fwdArea shr 4 and 15
//        if (temp == 15) {
//            temp = 0
//        }
//        binding.spFwdBk.selectedIndex = temp
//
//        temp = fwdArea shr 8 and 15
//        if (temp == 15) {
//            temp = 0
//        }
//
//        binding.spFwdLeft.selectedIndex = temp
//
//        temp = fwdArea shr 12 and 15
//        if (temp == 15) {
//            temp = 0
//        }
//        binding.spFwdRight.selectedIndex = temp
//
//
//        //后退切区
//        val bkArea: Int = pathBaseAttr.m_uBwdRotoScannerObstacle
//        temp = bkArea and 15
//        if (temp == 15) {
//            temp = 0
//        }
//
//        binding.spBkFwd.selectedIndex = temp
//        temp = bkArea shr 4 and 15
//        if (temp == 15) {
//            temp = 0
//        }
//
//        binding.spBkBk.selectedIndex = temp
//        temp = bkArea shr 8 and 15
//        if (temp == 15) {
//            temp = 0
//        }
//
//        binding.spBkLeft.selectedIndex = temp
//        temp = bkArea shr 12 and 15
//        if (temp == 15) {
//            temp = 0
//        }
//        binding.spBkRight.selectedIndex = temp
//    }
//
//    /**
//     * 导航方向
//     */
//    private fun setGuiDirection() {
//        ((binding.rgNavigationDirection[pathBaseAttr.m_uGuideType.toInt() and 255]) as RadioButton).isChecked =
//            true
//    }
//
//    /**
//     * 行走方向
//     */
//    private fun setWalkDirection() {
//        ((binding.rgDirectionWalk[pathBaseAttr.m_uMoveHeading.toInt() - 1]) as RadioButton).isChecked =
//            true
//    }
//
//    /**
//     * 允许上线
//     */
//    private fun setOnline() {
//        if (pathBaseAttr.m_uOnLine == 1.toShort()) (binding.rgTabOnline[0] as RadioButton).isChecked =
//            true
//        else (binding.rgTabOnline[1] as RadioButton).isChecked = true
//    }
//
//
//    /**
//     * 车辆类型
//     */
//    private fun setAgvType() {
//        var temp = 0
//        for (i in 0..7) {
//            temp = 2.0.pow(i).toInt()
//            if (pathBaseAttr.m_uCarrierType and temp.toShort() == temp.toShort()) chAvgType[i]?.isChecked =
//                true
//        }
//    }
//}
