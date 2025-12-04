package com.siasun.dianshi.xpop

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.lxj.xpopup.core.CenterPopupView
import com.siasun.dianshi.bean.CmsStation
import com.siasun.dianshi.bean.StationCoordinate
import com.siasun.dianshi.framework.ext.onClick
import com.siasun.dianshi.framework.ext.visible
import com.siasun.dianshi.mapviewdemo.R
import com.siasun.dianshi.mapviewdemo.databinding.DialogCmsStationBinding
import java.util.UUID

/******************************************
 * 类描述：
 *
 * @author: why
 * @time: 2025/8/20 09:10
 ******************************************/
@RequiresApi(Build.VERSION_CODES.R)
class CmsStationDialog(
    context: Context, private val mapId: Int, private val cmsStation: CmsStation?
) : CenterPopupView(context) {

    private lateinit var binding: DialogCmsStationBinding

    var onConfirmCall: ((cmsStation: CmsStation?) -> Unit)? = null
    var onDeleteCall: (() -> Unit)? = null
    override fun getImplLayoutId(): Int {
        return R.layout.dialog_cms_station
    }


    override fun onCreate() {
        super.onCreate()

        binding = DialogCmsStationBinding.bind(popupImplView)

        binding.btnCancel.onClick {
            dismiss()
        }

        binding.btnDelete.onClick {
            onDeleteCall?.invoke()
            dismiss()
        }

        binding.btnGetLoc.onClick {
//            (context as? CmsStationActivity)?.let { activity ->
//                with(binding) {
//                    etLocX.setText("${(activity.mAgvX).toFloat()}")
//                    etLocY.setText("${(activity.mAgvY).toFloat()}")
//                    etLocTheta.setText("${(activity.mAgvTheta).toFloat()}")
//                }
//            }
        }

        binding.btnConfirm.onClick {
//            with(binding) {
//                if (!etPointName.requireNotEmpty(context.getString(R.string.cms_station_name_input_e)))
//                    return@onClick
//
//                if (!etLocX.requireNotEmpty(context.getString(R.string.cms_locate_input_e)) ||
//                    !etLocY.requireNotEmpty(context.getString(R.string.cms_locate_input_e)) ||
//                    !etLocTheta.requireNotEmpty(context.getString(R.string.cms_locate_input_e))
//                ) return@onClick
//
//                saveCmsStation()
//                dismiss()
//            }
        }

        cmsStation?.let {
            binding.etPointName.setText(it.evName)
            binding.etLocX.setText(it.coordinate?.x.toString())
            binding.etLocY.setText(it.coordinate?.y.toString())
            binding.etLocTheta.setText(it.coordinate?.theta.toString())
            binding.btnDelete.visible()
            binding.ckRotate.isChecked = it.isRotate
        }
    }

    private fun saveCmsStation() {
        val cmsStationNew = CmsStation()

        cmsStation?.let {
            cmsStationNew.id = it.id
        } ?: let {
            cmsStationNew.id = UUID.randomUUID().toString()
        }

        cmsStationNew.evName = binding.etPointName.text.toString()
        cmsStationNew.mapId = mapId
        cmsStationNew.type = 1
        cmsStationNew.isRotate = binding.ckRotate.isChecked
        val coordinate = StationCoordinate(
            binding.etLocX.text.toString().toFloat(),
            binding.etLocY.text.toString().toFloat(),
            binding.etLocTheta.text.toString().toFloat(),
        )

        cmsStationNew.coordinate = coordinate

        onConfirmCall?.invoke(cmsStationNew)

    }
}