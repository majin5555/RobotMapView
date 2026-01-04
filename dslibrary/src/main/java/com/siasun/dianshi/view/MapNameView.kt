package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.siasun.dianshi.R
import com.siasun.dianshi.databinding.MapViewMapInfoBinding
import java.lang.ref.WeakReference

/**
 * 图例 地图名称
 */
@SuppressLint("ViewConstructor")
class MapNameView(context: Context, parent: WeakReference<MapView>) :
    LinearLayout(context) {
    private lateinit var mBinding: MapViewMapInfoBinding
    private val parentRef: WeakReference<MapView> = parent


    // 懒加载字符串资源，避免重复获取
    private val currentMapText by lazy { context.getString(R.string.current_map) }
    private val pointXText by lazy { "X:" }
    private val pointYText by lazy { "Y:" }
    private val pointTText by lazy { "T:" }
    private val pointZText by lazy { "Z:" }
    private val screenPointXText by lazy { "X:" }
    private val screenPointYText by lazy { "Y:" }

    /**
     * 初始化
     *
     * @param context 上下文
     * @param attrs   attrs
     */
    private fun init(context: Context) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mBinding = MapViewMapInfoBinding.inflate(inflater, this, true)
    }

    init {
        init(context)
    }

    /**
     * 设置当前地图名称
     */
    @SuppressLint("SetTextI18n")
    fun setMapName(name: String) {
        mBinding.tvCurrentMapValue.text = "${currentMapText}${name}"
    }

    @SuppressLint("SetTextI18n")
    fun setAgvX(x: Double) {
        mBinding.tvPointX.text = "${pointXText}${String.format("%.3f", x)}"
    }

    @SuppressLint("SetTextI18n")
    fun setAgvY(y: Double) {
        mBinding.tvPointY.text = "${pointYText}${String.format("%.3f", y)}"
    }

    @SuppressLint("SetTextI18n")
    fun setAgvT(theta: Double) {
        mBinding.tvPointTheta.text =
            "${pointTText}${String.format("%.3f", Math.toRadians(theta).toFloat())}"
    }

    @SuppressLint("SetTextI18n")
    fun setAgvZ(z: Double) {
        mBinding.tvPointZ.text =
            "${pointZText}${String.format("%.3f", Math.toRadians(z).toFloat())}"
    }

    /**
     * 设置屏幕坐标
     */
    @SuppressLint("SetTextI18n")
    fun setScreen(point: PointF) {
        mBinding.tvScreenPointX.text = "${screenPointXText}${String.format("%.3f", point.x)}"
        mBinding.tvScreenPointY.text = "${screenPointYText}${String.format("%.3f", point.y)}"
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

    }
}
