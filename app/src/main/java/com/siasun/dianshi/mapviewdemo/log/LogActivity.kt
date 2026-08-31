package com.siasun.dianshi.mapviewdemo.log

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.ToastUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.siasun.dianshi.adapter.SystemLogAdapter
import com.siasun.dianshi.base.BaseMvvmActivity
import com.siasun.dianshi.dialog.CommonWarnDialog
import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.mapviewdemo.KEY_UPDATE_LOG
import com.siasun.dianshi.mapviewdemo.R
import com.siasun.dianshi.mapviewdemo.databinding.ActivityLogBinding
import com.siasun.dianshi.mapviewdemo.viewmodel.LogViewModel
import com.siasun.dianshi.network.constant.KEY_NEY_IP
import com.siasun.dianshi.network.request.RequestDeleteErrorList
import com.siasun.dianshi.network.request.RequestGetLog
import com.siasun.dianshi.view.log.LogView
import com.tencent.mmkv.MMKV

/**
 * @author majin
 * @create date 2025/12/26
 * @desc 日志
 */
class LogActivity : BaseMvvmActivity<ActivityLogBinding, LogViewModel>() {
    private lateinit var mAdapter: SystemLogAdapter

    override fun initView(savedInstanceState: Bundle?) {
        MMKV.defaultMMKV().encode(KEY_NEY_IP, "192.168.3.101")

        initAdapter()
        initAction()
    }

    /**
     * 查询日志
     */
    private fun httpGetLog() {
        showLoading()
        mViewModel.getLog(RequestGetLog(mBinding.logView.selectedTypes.toMutableList()))
    }

    override fun initData() {
        super.initData()
        httpGetLog()

        mViewModel.logLiveData.observe(this) {
            dismissLoading()
            mAdapter.setData(it)
            mAdapter.syncSelectionState()
            if (mAdapter.getCurrentLogCount() == 0 && mBinding.logView.isEditMode()) {
                exitEditMode()
            } else {
                updateEditActionViews()
            }
        }

        //监听server更新系统日志颜色按钮
        LiveEventBus.get<Int>(KEY_UPDATE_LOG).observe(this) {
            httpGetLog()
        }
    }

    private fun initAdapter() {
        val logMap = LanguageData(this).getLogMap()
        LogUtil.i("logMap ${logMap}")
        mAdapter = SystemLogAdapter(logMap)
        mAdapter.onSelectionChanged = { _, allSelected ->
            mBinding.logView.setAllSelected(allSelected)
        }

        mBinding.logView.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@LogActivity)
            adapter = mAdapter
        }
    }

    private fun initAction() {
        mBinding.logView.callback = object : LogView.Callback {
            override fun onTypeChanged(types: List<Int>) {
                httpGetLog()
            }

            override fun onEditModeChanged(isEditMode: Boolean) {
                //列表为空时不进入编辑模式
                if (isEditMode && mAdapter.getCurrentLogCount() == 0) {
                    mBinding.logView.setEditMode(false)
                    return
                }
                mAdapter.setEditMode(isEditMode)
            }

            override fun onSelectAllClick(currentAllSelected: Boolean) {
                mAdapter.toggleSelectAll()
                updateEditActionViews()
            }

            override fun onDeleteClick() {
                handleDeleteAction()
            }
        }
    }

    private fun exitEditMode() {
        mBinding.logView.setEditMode(false)
    }

    private fun updateEditActionViews() {
        mBinding.logView.setAllSelected(mAdapter.areAllSelected())
    }

    private fun handleDeleteAction() {
        if (mAdapter.getSelectedCount() == 0) {
            ToastUtils.showShort(R.string.choice_delete_log)
            return
        }
        val selectedIds = mAdapter.getSelectedIds()
        if (selectedIds.isEmpty()) {
            ToastUtils.showShort(R.string.choice_delete_log)
            return
        }

        CommonWarnDialog.Builder(this).setMsg(R.string.delete_this_log)
            .setOnCommonWarnDialogListener {
                showLoading()
                mViewModel.deleteErrorList(RequestDeleteErrorList(selectedIds)) { type ->
                    dismissLoading()
                    if (type == 1) {
                        exitEditMode()
                        httpGetLog()
                    }
                }
            }.create().show()
    }
}
