package com.siasun.dianshi.dialog

import android.view.Gravity
import android.view.LayoutInflater
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.siasun.dianshi.adapter.SwitchMapAdapter
import com.siasun.dianshi.base.BaseDialog
import com.siasun.dianshi.base.BaseDialog.AnimStyle.DEFAULT
import com.siasun.dianshi.base.BaseDialogFragment
import com.siasun.dianshi.bean.MapInfo
import com.siasun.dianshi.framework.ext.onClick
import com.siasun.dianshi.framework.manager.AppManager
import com.siasun.dianshi.mapviewdemo.databinding.DialogSwichMapBinding

/**
 * @Author: MJ
 * @Date: 2025/7/3
 * @Description: 切换地图弹框
 */
class SwitchMapDialog {
    class Builder(
        val list: MutableList<MapInfo>, activity: FragmentActivity, val currentMapId: Int = -1
    ) : BaseDialogFragment.Builder<Builder>(activity) {
        private val mBinding: DialogSwichMapBinding =
            DialogSwichMapBinding.inflate(LayoutInflater.from(activity))
        private var onItemClickListener: ((MapInfo) -> Unit)? = null
        private var onCloseListener: (() -> Unit)? = null

        private var mAdapter: SwitchMapAdapter

        init {
            setContentView(mBinding.root)
            setWidth((AppManager.getScreenWidthPx() * 0.6).toInt())
            setHeight((AppManager.getScreenWidthPx() * 0.6).toInt())
            setAnimStyle(DEFAULT)
            setGravity(Gravity.CENTER)
            setCanceledOnTouchOutside(false)

            mAdapter = SwitchMapAdapter()
            mBinding.rvMap.apply {
                layoutManager = LinearLayoutManager(activity)
                adapter = mAdapter
//                divider(Color.parseColor("#DEDEDE"), 2, false)
            }

            mAdapter.setData(list)
            mAdapter.currentMapId = currentMapId

            mBinding.rvMap.post {
                val selectedPosition = list.indexOfFirst { it.mapId == currentMapId }
                if (selectedPosition != -1) {
                    mBinding.rvMap.scrollToPosition(selectedPosition)
                }
            }

            mBinding.imgClose.onClick {
                onCloseListener?.invoke()
                dismiss()
            }
            //点击事件
            mAdapter.setOnItemClickListener { position ->
                onItemClickListener?.invoke(mAdapter.getItem(position)!!)
                dismiss()
            }
        }

        fun setOnItemClickListener(cleanUpTask: (MapInfo) -> Unit): Builder {
            onItemClickListener = cleanUpTask
            return this
        }

        fun setOnCloseListener(cleanUpTask: () -> Unit): Builder {
            onCloseListener = cleanUpTask
            return this
        }

        override fun create(): BaseDialog {
            return super.create()
        }

    }
}