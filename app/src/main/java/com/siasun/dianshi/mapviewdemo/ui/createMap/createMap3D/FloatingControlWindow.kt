package com.siasun.dianshi.mapviewdemo.ui.createMap.createMap3D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import com.jeremyliao.liveeventbus.LiveEventBus

class FloatingControlWindow(private val context: Context) {

    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var floatingView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isShowing = false   // 关键标志位

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingView(): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.parseColor("#DD000000"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
                clipToOutline = true
            }
        }

        val buttonTexts = arrayOf(
            "测试按钮", "显示关键帧", "恢复旋转角度", "添加节点",
            "匹配节点", "修改配置", "保存地图", "结束扫描", "扫描新环境", "扩展地图测试"
        )
        val eventKeys = arrayOf(
            CreateMap3DActivity.EVENT_FLOATING_BTN_TEST,
            CreateMap3DActivity.EVENT_FLOATING_BTN_SHOW_FRAME,
            CreateMap3DActivity.EVENT_FLOATING_BTN_RESET_ROTATION,
            CreateMap3DActivity.EVENT_FLOATING_BTN_ADD_NODE,
            CreateMap3DActivity.EVENT_FLOATING_BTN_MATCH_NODE,
            CreateMap3DActivity.EVENT_FLOATING_BTN_EDIT_CONFIG,
            CreateMap3DActivity.EVENT_FLOATING_BTN_SAVE,
            CreateMap3DActivity.EVENT_FLOATING_BTN_STOP,
            CreateMap3DActivity.EVENT_FLOATING_BTN_CREATE,
            CreateMap3DActivity.EVENT_FLOATING_BTN_EXPAND
        )

        for (i in buttonTexts.indices) {
            val button = Button(context).apply {
                text = buttonTexts[i]
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 8
                }
                setOnClickListener {
                    LiveEventBus.get(eventKeys[i], Boolean::class.java).post(true)
                }
            }
            layout.addView(button)
        }

        layout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams?.x ?: 0
                    initialY = layoutParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    layoutParams?.x = initialX + dx.toInt()
                    layoutParams?.y = initialY + dy.toInt()
                    floatingView?.let { windowManager.updateViewLayout(it, layoutParams) }
                    true
                }
                else -> false
            }
        }
        return layout
    }

    fun show() {
        if (isShowing) return
        if (floatingView == null) {
            floatingView = createFloatingView()
            layoutParams = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                format = PixelFormat.TRANSLUCENT
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 200
            }
        }
        if (floatingView?.parent == null) {
            try {
                windowManager.addView(floatingView, layoutParams)
                isShowing = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun hide() {
        if (!isShowing) return
        floatingView?.let {
            if (it.parent != null) {
                try {
                    windowManager.removeView(it)
                    isShowing = false
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun destroy() {
        hide()
        floatingView = null
        layoutParams = null
    }
}