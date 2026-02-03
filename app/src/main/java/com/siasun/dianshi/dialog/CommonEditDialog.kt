package com.siasun.dianshi.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.hjq.shape.view.ShapeTextView
import com.siasun.dianshi.mapviewdemo.R
import kotlin.let

/******************************************
 * 类描述：
 *
 * @author: why
 * @time: 2025/7/15 08:51
 ******************************************/

/**
 *公共编辑框
 */
class CommonEditDialog(context: Context?, themeResId: Int) : Dialog(context!!, themeResId) {

    class Builder(private val context: Context, style: Int = R.style.custom_dialog) {
        private var dialog: CommonEditDialog
        private lateinit var tvTitle: TextView
        private lateinit var mContent: EditText
        private lateinit var btnSure: ShapeTextView
        private lateinit var btnClo: ShapeTextView
        private var call: CommonEditDialogListener? = null

        init {
            dialog = CommonEditDialog(context, style)
            initView(R.layout.dialog_edit_common)
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
            return this
        }

        @RequiresApi(Build.VERSION_CODES.R)
        @SuppressLint("InflateParams")
        fun create(): CommonEditDialog {
            dialog.setCanceledOnTouchOutside(false)
            val window = dialog.window
            val attributes = window!!.attributes
            attributes?.width = (context.display!!.width * 0.3).toInt()

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
        fun setHint(msg: Int): Builder {
            mContent.hint = context.getString(msg)
            return this
        }

        /**
         * 设置提示
         */
        fun setHint(msg: String): Builder {
            if (msg == "") return this
            mContent.hint = msg
            return this
        }

        /**
         * 设置信息
         */
        fun setMsg(msg: String): Builder {
            mContent.setText(msg)
            return this
        }

        /**
         * 设置输入框类型
         */
        fun setMsgType(type: Int): Builder {
            mContent.inputType = type
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
            mContent = view.findViewById(R.id.edit_name)
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
                    if (mContent.text.toString() == "") {
                        return@setOnClickListener
                    }
                    call?.confirm(mContent.text.toString())
                    dialog.dismiss()
                }
            }
        }

        interface CommonEditDialogListener {
            fun confirm(str: String)
            fun discard() {}
        }

        /**
         * 设置监听
         */
        fun setOnCommonEditDialogListener(call: CommonEditDialogListener): Builder {
            this.call = call
            return this
        }


    }


}