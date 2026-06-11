package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import android.util.TypedValue
import com.siasun.dianshi.databinding.MapViewMapInfoBinding
import java.lang.ref.WeakReference
import java.util.Locale

/**
 * 图例 地图名称
 */
@SuppressLint("ViewConstructor")
class MapNameView(context: Context, parent: WeakReference<MapView>) : LinearLayout(context) {
    private lateinit var mBinding: MapViewMapInfoBinding


    // 懒加载字符串资源，避免重复获取
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
        mBinding.tvCurrentMapValue.text = name
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
            "${pointTText}${String.format(Locale.US, "%.3f", Math.toRadians(theta).toFloat())}"
    }

    @SuppressLint("SetTextI18n")
    fun setAgvZ(z: Double) {
        mBinding.tvPointZ.text =
            "${pointZText}${String.format(Locale.US, "%.3f", Math.toRadians(z).toFloat())}"
    }

    /**
     * 设置屏幕坐标
     */
    @SuppressLint("SetTextI18n")
    fun setScreen(point: PointF) {
        mBinding.tvScreenPointX.text =
            "${screenPointXText}${String.format(Locale.US, "%.3f", point.x)}"
        mBinding.tvScreenPointY.text =
            "${screenPointYText}${String.format(Locale.US, "%.3f", point.y)}"
    }

    /**
     * 设置显示隐藏坐标
     */
    fun showCoordinates(boolean: Boolean) {
        mBinding.shaCoordinates.visibility = if (boolean) View.VISIBLE else View.GONE
    }

    /**
     * 设置显示隐藏地图名称
     */
    fun showMapName(boolean: Boolean) {
        mBinding.conMap.visibility = if (boolean) View.VISIBLE else View.GONE
    }

    enum class Position {
        TOP_LEFT, BOTTOM_LEFT
    }

    /**
     * 设置位置：左上角或左下角
     */
    fun setPosition(position: Position) {
        val constraintLayout = mBinding.root as? ConstraintLayout ?: return
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        val margin10 = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 10f, context.resources.displayMetrics
        ).toInt()

        when (position) {
            Position.TOP_LEFT -> {
                // con_map 恢复到顶部
                constraintSet.clear(mBinding.conMap.id, ConstraintSet.BOTTOM)
                constraintSet.connect(
                    mBinding.conMap.id,
                    ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP,
                    0
                )

                // sha_coordinates 在 con_map 下方
                constraintSet.clear(mBinding.shaCoordinates.id, ConstraintSet.BOTTOM)
                constraintSet.connect(
                    mBinding.shaCoordinates.id,
                    ConstraintSet.TOP,
                    mBinding.conMap.id,
                    ConstraintSet.BOTTOM,
                    margin10
                )
            }

            Position.BOTTOM_LEFT -> {
                // sha_coordinates 到屏幕底部
                constraintSet.clear(mBinding.shaCoordinates.id, ConstraintSet.TOP)
                constraintSet.connect(
                    mBinding.shaCoordinates.id,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                    margin10
                )

                // con_map 在 sha_coordinates 上方
                constraintSet.clear(mBinding.conMap.id, ConstraintSet.TOP)
                constraintSet.connect(
                    mBinding.conMap.id,
                    ConstraintSet.BOTTOM,
                    mBinding.shaCoordinates.id,
                    ConstraintSet.TOP,
                    margin10
                )
            }
        }
        constraintSet.applyTo(constraintLayout)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

    }
}
