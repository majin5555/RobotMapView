package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.siasun.dianshi.R
import com.siasun.dianshi.databinding.MapViewLegendBinding
import java.lang.ref.WeakReference
import androidx.core.view.isVisible

/**
 * 图例
 */
@SuppressLint("ViewConstructor")
class LegendView(context: Context, attrs: AttributeSet, parent: WeakReference<MapView>) :
    LinearLayout(context) {
    private lateinit var mBinding: MapViewLegendBinding
    private val parentRef: WeakReference<MapView> = parent

    /**
     * 初始化
     *
     * @param context 上下文
     * @param attrs   attrs
     */
    private fun init(context: Context, attrs: AttributeSet?) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mBinding = MapViewLegendBinding.inflate(inflater, this, true)
        setCheckboxVisibility(attrs)
    }

    init {
        init(context, attrs)

        mBinding.ivLegend.setOnClickListener {
            if (mBinding.conLegend.isVisible) {
                mBinding.ivLegend.setImageResource(R.drawable.iv_back)
                mBinding.conLegend.visibility = GONE
            } else {
                mBinding.ivLegend.setImageResource(R.drawable.iv_go)
                mBinding.conLegend.visibility = VISIBLE
            }
        }

        // 为每个CheckBox添加点击变化监听器
        // 上激光点云
        mBinding.cbUpLaserPointCloud.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mUpLaserScanView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mUpLaserScanView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //下激光点云
        mBinding.cbLowerLaserPointCloud.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mDownLaserScanView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mDownLaserScanView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //虚拟墙
        mBinding.cbVirtualWall.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mWallView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mWallView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //顶视觉路线
        mBinding.cbTopViewPath.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mTopViewPathView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mTopViewPathView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //避让点
        mBinding.cbStations.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mStationView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mStationView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //上线点
        mBinding.cbOnlinePose.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mOnlinePoseView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mOnlinePoseView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //充电站
        mBinding.cbChargeStation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mHomeDockView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mHomeDockView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //乘梯点
        mBinding.cbElevator.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mElevatorView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mElevatorView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //定位区域
        mBinding.cbPositingArea.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mPostingAreasView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mPostingAreasView?.setDrawingEnabled(false) // 禁用绘制
            }
        }

        //清扫区域
        mBinding.cbArea.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mPolygonEditView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mPolygonEditView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //路径
        mBinding.cbWorldPath.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mWorldPadView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mWorldPadView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
        //混行区
        mBinding.cbMixArea.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                parentRef.get()?.mMixAreaView?.setDrawingEnabled(true) // 启用绘制
            } else {
                parentRef.get()?.mMixAreaView?.setDrawingEnabled(false) // 禁用绘制
            }
        }
    }
    /**
     * 设置各个CheckBox的显示隐藏
     */
    @SuppressLint("CustomViewStyleable")
    private fun setCheckboxVisibility(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.MapView)

            // 从XML属性中读取各CheckBox的显示状态
            mBinding.cbUpLaserPointCloud.visibility = if (typedArray.getBoolean(
                    R.styleable.MapView_showUpLaserPointCloud, true
                )
            ) VISIBLE else GONE

            mBinding.cbLowerLaserPointCloud.visibility = if (typedArray.getBoolean(
                    R.styleable.MapView_showLowerLaserPointCloud, true
                )
            ) VISIBLE else GONE


            mBinding.cbVirtualWall.visibility = if (typedArray.getBoolean(
                    R.styleable.MapView_showVirtualWall, false
                )
            ) VISIBLE else GONE

            mBinding.cbTopViewPath.visibility = if (typedArray.getBoolean(
                    R.styleable.MapView_showTopViewPath, false
                )
            ) VISIBLE else GONE

            mBinding.cbStations.visibility =
                if (typedArray.getBoolean(
                        R.styleable.MapView_showStations,
                        false
                    )
                ) VISIBLE else GONE

            mBinding.cbOnlinePose.visibility = if (typedArray.getBoolean(
                    R.styleable.MapView_showOnlinePose, false
                )
            ) VISIBLE else GONE

            mBinding.cbChargeStation.visibility = if (typedArray.getBoolean(
                    R.styleable.MapView_showChargeStation, false
                )
            ) VISIBLE else GONE

            mBinding.cbElevator.visibility =
                if (typedArray.getBoolean(
                        R.styleable.MapView_showElevator,
                        false
                    )
                ) VISIBLE else GONE


            mBinding.cbPositingArea.visibility = if (typedArray.getBoolean(
                    R.styleable.MapView_showPositingArea, false
                )
            ) VISIBLE else GONE

            mBinding.cbArea.visibility =
                if (typedArray.getBoolean(R.styleable.MapView_showArea, false)) VISIBLE else GONE

            mBinding.cbWorldPath.visibility = if (typedArray.getBoolean(
                    R.styleable.MapView_showWorldPath, false
                )
            ) VISIBLE else GONE

            mBinding.cbMixArea.visibility =
                if (typedArray.getBoolean(R.styleable.MapView_showMixArea, false)) VISIBLE else GONE


            typedArray.recycle()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理所有监听器，防止内存泄漏
        mBinding.cbUpLaserPointCloud.setOnCheckedChangeListener(null)
        mBinding.cbLowerLaserPointCloud.setOnCheckedChangeListener(null)
        mBinding.cbVirtualWall.setOnCheckedChangeListener(null)
        mBinding.cbTopViewPath.setOnCheckedChangeListener(null)
        mBinding.cbStations.setOnCheckedChangeListener(null)
        mBinding.cbOnlinePose.setOnCheckedChangeListener(null)
        mBinding.cbChargeStation.setOnCheckedChangeListener(null)
        mBinding.cbElevator.setOnCheckedChangeListener(null)
        mBinding.cbPositingArea.setOnCheckedChangeListener(null)
    }
}
