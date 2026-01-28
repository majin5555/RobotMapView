package com.siasun.dianshi.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.hjq.shape.view.ShapeTextView
import com.siasun.dianshi.mapviewdemo.R
import kotlin.let


/**
 *公共告警弹框
 */
class CommonWarnDialog(context: Context?, themeResId: Int) : Dialog(context!!, themeResId) {

    class Builder(private val context: Context, style: Int = R.style.custom_dialog) {
        private var dialog: CommonWarnDialog
        private lateinit var tvTitle: TextView
        private lateinit var tvMsg: TextView
        private lateinit var btnSure: Button
        private lateinit var btnClo: Button
        private var call: CommonWarnDialogListener? = null

        init {
            dialog = CommonWarnDialog(context, style)
            initView(R.layout.dialog_warn_common)
        }

        /**
         * 初始化视图
         */
        fun initView(layout: Int): Builder {
            val mRootView =
                (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
                    layout, null
                )
            getView(mRootView)
            dialog.setContentView(mRootView)
            hideBottomUIMenu(dialog.window!!)
            return this
        }

        @RequiresApi(Build.VERSION_CODES.R)
        @SuppressLint("InflateParams")
        fun create(): CommonWarnDialog {
            dialog.setCanceledOnTouchOutside(false)
            val window = dialog.window
            val attributes = window!!.attributes
            attributes?.width = (context.display!!.width * 0.6).toInt()

            window.attributes = attributes
            return dialog
        }


        /**
         * 设置标题
         */
        fun setTitle(title: Int): Builder {
            tvTitle.text = context.getString(title)
            return this
        }

        /**
         * 设置标题
         */
        fun setTitle(title: String): Builder {
            tvTitle.text = title
            return this

        }

        /**
         * 设置信息
         */
        fun setMsg(title: Int): Builder {
            tvMsg.text = context.getString(title)
            return this
        }

        /**
         * 设置信息
         */
        fun setMsg(msg: String): Builder {
            tvMsg.text = msg
            return this
        }

        /**
         * 取消按钮文字
         */
        fun setDiscard(discard: Int) {
            btnClo.text = context.getString(discard)
        }

        /**
         * 取消按钮显示
         */
        fun setDiscardVisibility(isShow: Boolean): Builder {
            btnClo.visibility = if (isShow) View.VISIBLE else View.GONE
            return this
        }

        /**
         * 确定按钮文字
         */
        fun setConfirm(confirm: Int) {
            btnSure.text = context.getString(confirm)
        }

        /**
         * 确定按钮文字
         */
        fun setConfirm(confirm: String) {
            btnSure.text = confirm
        }

        /**
         * 取消按钮显示
         */
        fun setCloVisibility(isShow: Boolean): Builder {
            btnClo.visibility = if (isShow) View.VISIBLE else View.GONE
            return this
        }

        /**
         * 确定按钮显示
         */
        fun setConfirmVisibility(isShow: Boolean): Builder {
            btnSure.visibility = if (isShow) View.VISIBLE else View.GONE
            return this
        }


        private fun getView(view: View) {
            tvTitle = view.findViewById(R.id.tv_title)
            tvMsg = view.findViewById(R.id.tv_msg)
            btnClo = view.findViewById(R.id.btn_dismiss)
            btnSure = view.findViewById(R.id.btn_sure)

            btnClo.let {
                it.setOnClickListener {
                    call?.discard()
                    dialog.dismiss()
                }
            }

            btnSure.let {
                it.setOnClickListener {
                    call?.confirm()
                    dialog.dismiss()
                }
            }
        }

        interface CommonWarnDialogListener {
            fun confirm()
            fun discard() {}
        }

        /**
         * 设置监听
         */
        fun setOnCommonWarnDialogListener(call: CommonWarnDialogListener): Builder {
            this.call = call
            return this
        }

        //隐藏虚拟按键，并且全屏
        private fun hideBottomUIMenu(window: Window) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            window.decorView.setOnSystemUiVisibilityChangeListener {
                var uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or  //布局位于状态栏下方
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or  //全屏
                        View.SYSTEM_UI_FLAG_FULLSCREEN or  //隐藏导航栏
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                uiOptions = uiOptions or 0x00001000
                window.decorView.systemUiVisibility = uiOptions
            }
        }
    }


}


