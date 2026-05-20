package com.siasun.dianshi.adapter

import android.annotation.SuppressLint
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import android.view.LayoutInflater
import android.view.ViewGroup
import com.siasun.dianshi.ConstantBase.PAD_MAP_NAME_PNG
import com.siasun.dianshi.ConstantBase.getFilePath
import com.siasun.dianshi.bean.MapInfo
import com.siasun.dianshi.framework.adapter.BaseBindViewHolder
import com.siasun.dianshi.framework.adapter.BaseRecyclerViewAdapter
import com.siasun.dianshi.mapviewdemo.R
import com.siasun.dianshi.mapviewdemo.databinding.ItemSwitchMapBinding

/**
 * @Author: 切换地图
 * @Date: 2025/7/3 16:38
 * @Description:
 */
class SwitchMapAdapter : BaseRecyclerViewAdapter<MapInfo, ItemSwitchMapBinding>() {
    private var onItemListener: ((Int) -> Unit)? = null
    fun setOnItemClickListener(position: (Int) -> Unit) {
        onItemListener = position
    }

    var currentMapId: Int = -1
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun onBindDefViewHolder(
        holder: BaseBindViewHolder<ItemSwitchMapBinding>,
        item: MapInfo?,
        position: Int
    ) {
        holder.binding.apply {
            tvName.text = item?.mapName

            val isSelected = item?.mapId == currentMapId
            root.setBackgroundResource(if (isSelected) R.drawable.container_background_selected else R.drawable.container_background)
            ivSelect.setImageResource(if (isSelected) R.drawable.ic_radio_checked else R.drawable.ic_radio_unchecked)

            val filePath = getFilePath(item!!.mapId, PAD_MAP_NAME_PNG)

            // 使用 Glide 加载图片，自动处理异步加载和缓存
            Glide.with(holder.itemView.context)
                .load(filePath)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(100, 100) // 保持原有的缩略图大小逻辑
                .into(ivMap)

            ivSelect.setOnClickListener {
                if (!isSelected)
                    onItemListener?.invoke(position)
            }
        }
    }

    override fun getViewBinding(
        layoutInflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemSwitchMapBinding {
        return ItemSwitchMapBinding.inflate(layoutInflater, parent, false)
    }
}