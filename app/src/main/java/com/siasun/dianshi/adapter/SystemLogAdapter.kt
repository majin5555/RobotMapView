package com.siasun.dianshi.adapter;

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.siasun.dianshi.bean.LogBean
import com.siasun.dianshi.framework.adapter.BaseBindViewHolder
import com.siasun.dianshi.framework.adapter.BaseRecyclerViewAdapter
import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.mapviewdemo.R
import com.siasun.dianshi.mapviewdemo.databinding.ItemSystemLogBinding

/**
 * @author majin
 * @create date 2025/2/19
 * 系统日志
 */
class SystemLogAdapter(private val logMap: MutableMap<String, Any>) :
    BaseRecyclerViewAdapter<LogBean?, ItemSystemLogBinding>() {

    private var isEditMode = false
    private val selectedLogs = linkedSetOf<LogBean>()
    var onSelectionChanged: ((selectedCount: Int, allSelected: Boolean) -> Unit)? = null

    companion object {
        private val SELECTED_BG_COLOR = Color.parseColor("#143B82F6")
    }

    @SuppressLint("SetTextI18n")
    override fun onBindDefViewHolder(
        holder: BaseBindViewHolder<ItemSystemLogBinding>, item: LogBean?, position: Int
    ) {
        holder.binding.apply {
            try {
                val errCode = item?.err_code
                val logMessage = logMap[errCode] as? String
                tvName.text = if (logMessage != null) {
                    "$errCode  $logMessage"
                } else {
                    //告警码对不上时，显示"接口给的错误码是未知告警码"
                    tvName.context.getString(R.string.unknown_alarm_code, errCode ?: "")
                }
                tvTime.text = item?.create_time
                when (item?.err_level) {
                    3 -> imgLog.setImageResource(R.drawable.iv_info)
                    2 -> imgLog.setImageResource(R.drawable.iv_warm)
                    1 -> imgLog.setImageResource(R.drawable.iv_error)
                }
            } catch (e: Exception) {
                LogUtil.e("系统日志 ${e}")
            }

            val selected = item != null && selectedLogs.contains(item)
            cbSelect.isVisible = isEditMode
            cbSelect.setOnCheckedChangeListener(null)
            cbSelect.isChecked = selected
            cbSelect.setOnCheckedChangeListener { _, isChecked ->
                item?.let { log ->
                    updateSelection(log, isChecked)
                }
            }
            root.setBackgroundColor(if (selected && isEditMode) SELECTED_BG_COLOR else Color.TRANSPARENT)
            root.setOnClickListener {
                if (isEditMode && item != null) {
                    updateSelection(item, !cbSelect.isChecked)
                }
            }
        }
    }

    fun setEditMode(enable: Boolean) {
        isEditMode = enable
        if (!enable) {
            selectedLogs.clear()
        }
        notifyDataSetChanged()
        dispatchSelectionChanged()
    }

    fun toggleSelectAll() {
        val logs = getData().filterNotNull()
        if (logs.isEmpty()) return
        if (selectedLogs.size == logs.size) {
            selectedLogs.clear()
        } else {
            selectedLogs.clear()
            selectedLogs.addAll(logs)
        }
        notifyDataSetChanged()
        dispatchSelectionChanged()
    }

    fun clearSelection() {
        if (selectedLogs.isEmpty()) return
        selectedLogs.clear()
        notifyDataSetChanged()
        dispatchSelectionChanged()
    }

    fun getSelectedCount(): Int = selectedLogs.size

    fun getSelectedIds(): MutableList<Int> {
        return selectedLogs.mapNotNull { log ->
            log.id?.takeIf { it > 0 }
        }.toMutableList()
    }

    fun getCurrentLogCount(): Int = getData().filterNotNull().size

    fun areAllSelected(): Boolean {
        val logs = getData().filterNotNull()
        return logs.isNotEmpty() && selectedLogs.size == logs.size
    }

    fun syncSelectionState() {
        val currentLogs = getData().filterNotNull().toSet()
        if (selectedLogs.retainAll(currentLogs)) {
            dispatchSelectionChanged()
        } else {
            dispatchSelectionChanged()
        }
    }

    private fun updateSelection(log: LogBean, isSelected: Boolean) {
        if (isSelected) {
            selectedLogs.add(log)
        } else {
            selectedLogs.remove(log)
        }
        notifyDataSetChanged()
        dispatchSelectionChanged()
    }

    private fun dispatchSelectionChanged() {
        onSelectionChanged?.invoke(selectedLogs.size, areAllSelected())
    }

    override fun getViewBinding(
        layoutInflater: LayoutInflater, parent: ViewGroup, viewType: Int
    ): ItemSystemLogBinding = ItemSystemLogBinding.inflate(layoutInflater, parent, false)
}
