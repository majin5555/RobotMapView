package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.siasun.dianshi.R
import java.lang.ref.WeakReference

/**
 * 机器人图标 实时位置 、有任务状态下的路径
 */
@SuppressLint("ViewConstructor")
class RobotView(context: Context?, val parent: WeakReference<MapView>) :
    SlamWareBaseView<MapView>(context, parent) {

    private var agvPose: DoubleArray? = null

    // 控制是否绘制
    private var isDrawingEnabled: Boolean = true

    // 机器人相关
    private var robotDrawable: Drawable? = null
    private var customTarget: CustomTarget<Drawable>? = null
    private var iconWidth: Int = 0
    private var iconHeight: Int = 0

    init {
        loadRobotDrawable()
    }

    private fun loadRobotDrawable() {
        context?.let { ctx ->
            // 使用 BitmapFactory 仅解码边界，获取系统基于 density 缩放后的标准尺寸
            // 这样能确保 Glide 加载的尺寸与原生 decodeResource 保持一致，避免渲染过大及占用过多内存
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeResource(ctx.resources, R.mipmap.current_location, options)
            iconWidth = options.outWidth
            iconHeight = options.outHeight

            val targetWidth =
                if (iconWidth > 0) iconWidth else com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
            val targetHeight =
                if (iconHeight > 0) iconHeight else com.bumptech.glide.request.target.Target.SIZE_ORIGINAL

            customTarget = object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    robotDrawable = resource
                    postInvalidate()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    robotDrawable = null
                    postInvalidate()
                }
            }
            Glide.with(ctx.applicationContext)
                .load(R.mipmap.current_location)
                .override(targetWidth, targetHeight)
                .into(customTarget!!)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mapView = parent.get() as? MapView ?: return
        if (isDrawingEnabled) {
            agvPose?.let { pose ->
                robotDrawable?.let { drawable ->
                    // 将世界坐标转换为屏幕坐标
                    val screenPos = mapView.worldToScreen(pose[0].toFloat(), pose[1].toFloat())

                    // 保存画布状态
                    canvas.save()

                    // 平移画布到目标坐标点
                    canvas.translate(screenPos.x, screenPos.y)
                    // 以当前原点（即目标坐标点）为轴心进行旋转
                    canvas.rotate(-pose[2].toFloat())

                    // 设置 Drawable 的边界，使其中心与原点对齐
                    // 修复图标过大问题：直接使用 BitmapFactory 解码出的标准宽高，不使用 drawable.intrinsicWidth
                    // 因为 Glide 返回的 Drawable 可能会二次计算 density 导致尺寸异常放大
                    val width = if (iconWidth > 0) iconWidth else drawable.intrinsicWidth
                    val height = if (iconHeight > 0) iconHeight else drawable.intrinsicHeight
                    val halfWidth = width / 5
                    val halfHeight = height / 5
                    drawable.setBounds(-halfWidth, -halfHeight, halfWidth, halfHeight)

                    // 直接使用 Glide 加载出的 Drawable 进行渲染
                    drawable.draw(canvas)

                    // 恢复画布状态
                    canvas.restore()
                }
            }
        }
    }


    /**
     * 车体实时坐标
     */
    fun setAgvData(array: DoubleArray) {
        if (!isDrawingEnabled) return
        agvPose = array
        postInvalidate()
    }

    /**
     * 获取车体实时坐标
     */
    fun getAgvData(): DoubleArray? = agvPose

    /**
     * 设置是否启用绘制
     */
    fun setDrawingEnabled(enabled: Boolean) {
        this.isDrawingEnabled = enabled
        postInvalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清除 Glide 的加载任务，释放资源防止内存泄漏
        context?.let { ctx ->
            customTarget?.let { target ->
                Glide.with(ctx.applicationContext).clear(target)
            }
        }
        
        robotDrawable = null
        customTarget = null

        // 清理其他资源
        agvPose = null
    }
}
