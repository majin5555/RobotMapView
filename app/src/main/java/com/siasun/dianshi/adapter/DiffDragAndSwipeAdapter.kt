package com.siasun.dianshi.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter4.BaseDifferAdapter
import com.chad.library.adapter4.viewholder.QuickViewHolder
import com.hjq.shape.layout.ShapeConstraintLayout
import com.siasun.dianshi.bean.SweepingModeBean
import com.siasun.dianshi.bean.task.Sub_Task
import com.siasun.dianshi.framework.ext.onClick
import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.mapviewdemo.R

import java.util.Collections

/**
 * Create adapter
 */
class DiffDragAndSwipeAdapter() :
    BaseDifferAdapter<Sub_Task, QuickViewHolder>(DiffEntityCallback()) {
    var sweepingModeBeanData: MutableList<SweepingModeBean> = mutableListOf()

    private var mMapName: MutableMap<Int, String> = mutableMapOf()

    private var onItemDeleteClickListener: ((item: Sub_Task) -> Unit)? = null
    private var onItemSettingClickListener: ((subTask: Sub_Task, position: Int) -> Unit)? = null
    private var onItemClickListener: ((item: Sub_Task, position: Int) -> Unit)? = null

    private var selectedPosition = -1
    // private var scrollListener: RecyclerView.OnScrollListener? = null

    fun getSelectedPosition(): Int {
        return selectedPosition
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        // 移除了根据可视区域滚动自动选中的逻辑
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
    }

    fun setSelectedPosition(position: Int) {
        if (position == selectedPosition) return
        val previousPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousPosition)
        notifyItemChanged(selectedPosition)
    }
    @SuppressLint("NotifyDataSetChanged")
    fun setSweepingModeList(data: MutableList<SweepingModeBean>) {
        sweepingModeBeanData.addAll(data)
    }


    /**
     * 设置所有地图名称
     */
    @SuppressLint("NotifyDataSetChanged")
    fun setMapName(mMapName: MutableMap<Int, String>) {
        this.mMapName.putAll(mMapName)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        context: Context, parent: ViewGroup, viewType: Int
    ): QuickViewHolder = QuickViewHolder(R.layout.item_area_select, parent)

    /**
     * 拖拽交换顺序并刷新
     */
    override fun swap(fromPosition: Int, toPosition: Int) {
        val currentList = items.toMutableList()
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(currentList, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(currentList, i, i - 1)
            }
        }
        submitList(currentList)
        // 拖拽后，使高亮状态跟随拖拽的 item
        setSelectedPosition(toPosition)
    }

    override fun add(data: Sub_Task) {
        val currentSize = items.size
        val insertPos = if (selectedPosition in 0 until currentSize) {
            selectedPosition + 1
        } else {
            currentSize
        }
        super.add(insertPos, data)

        val previousPosition = selectedPosition
        selectedPosition = insertPos

        recyclerView.post {
            if (previousPosition in 0 until currentSize) {
                notifyItemChanged(previousPosition)
            }
            notifyItemChanged(selectedPosition)
        }
    }

    @SuppressLint("RecyclerView")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onBindViewHolder(
        holder: QuickViewHolder, position: Int, item: Sub_Task?
    ) {
        if (item == null) return

        holder.setText(R.id.tv_position, "${position + 1}")
            .setText(R.id.tv_map_value, "${mMapName[item.sub_region_layer]}")
            .setText(R.id.tv_area_name, item.sub_task_name)

        try {
            //显示清扫模式名称
            //任务下的模式的ID   固定模式的ID是清扫模式列表的角标 自定义模式是清扫模式列表的ID
            val customCleanModeID = item.customCleanMode
            sweepingModeBeanData.forEach { item ->
                if (item.id == customCleanModeID) {
//                    holder.setText(
//                        R.id.tv_area_mode, LanguageData(context).getCurrentModeName(item.modeName)
//                    )
                }
            }
        } catch (e: Exception) {
            LogUtil.e("显示当前清扫模式错误 e $e")
        }

        //区域设置
        holder.getView<TextView>(R.id.btn_area_setting).onClick {
            onItemSettingClickListener?.invoke(item, position)
        }
        //删除区域
        holder.getView<ImageView>(R.id.iv_delete).onClick {
            onItemDeleteClickListener?.invoke(item)
        }

        // 点击高亮显示
        holder.itemView.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION && currentPosition != selectedPosition) {
                val previousPosition = selectedPosition
                selectedPosition = currentPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
            }
            if (currentPosition != RecyclerView.NO_POSITION) {
                onItemClickListener?.invoke(item, currentPosition)
            }
        }

        val rootView = holder.itemView as? ShapeConstraintLayout
        if (rootView != null) {
            if (position == selectedPosition) {
                rootView.shapeDrawableBuilder.setStrokeSize(5).setStrokeColor(
                    ContextCompat.getColor(
                        context, R.color.app_bg
                    )
                ).intoBackground()
            } else {
                rootView.shapeDrawableBuilder.setStrokeSize(0).intoBackground()
            }
        }
    }

    /**
     * 删除区域
     */
    fun setOnItemDeleteClickListener(listener: (item: Sub_Task) -> Unit) {
        onItemDeleteClickListener = listener
    }

    /**
     * 区域设置
     */
    fun setOnItemSettingClickListener(listener: (subTask: Sub_Task, position: Int) -> Unit) {
        onItemSettingClickListener = listener
    }

    /**
     * item点击事件
     */
    fun setOnItemClickListener(listener: (item: Sub_Task, position: Int) -> Unit) {
        onItemClickListener = listener
    }

}