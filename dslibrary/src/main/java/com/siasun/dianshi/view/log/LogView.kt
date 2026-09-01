package com.siasun.dianshi.view.log

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.siasun.dianshi.R
import com.siasun.dianshi.databinding.ViewLogBinding

/**
 * 日志界面自定义 View
 *
 * 封装日志筛选（故障/提示/信息）、编辑模式（编辑/全选/删除）与日志列表，
 * 列表数据加载、删除等业务逻辑通过 [Callback] 交由外部处理。
 *
 * 三个操作按钮的文字、背景与左侧图标支持两种方式传入：
 * 1. XML 自定义属性：editText/cancelEditText/selectAllText/cancelSelectAllText/deleteText、
 *    editBackground/selectAllBackground/deleteBackground、editDrawableStart/selectAllDrawableStart/deleteDrawableStart
 * 2. 函数调用：setEditText()/setSelectAllText()/setDeleteText()/setEditBackground()/setSelectAllBackground()/setDeleteBackground()、
 *    setEditDrawableStart()/setSelectAllDrawableStart()/setDeleteDrawableStart()
 *
 * 故障/告警筛选 CheckBox 的文字同样支持两种方式传入：
 * 1. XML 自定义属性：errorText/warnText
 * 2. 函数调用：setErrorText()/setWarnText()/setFilterText()
 *
 * @author majin
 */
class LogView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    companion object {
        /** 故障 */
        const val TYPE_ERROR = 1

        /** 提示 */
        const val TYPE_WARN = 2

        /** 编辑按钮水波纹颜色（半透明蓝） */
        private const val RIPPLE_EDIT = 0x335672FF

        /** 全选按钮水波纹颜色（半透明灰蓝） */
        private const val RIPPLE_SELECT_ALL = 0x33D9E3F2

        /** 删除按钮水波纹颜色（半透明红） */
        private const val RIPPLE_DELETE = 0x33FF6D64

    }

    /** 当前筛选的类型列表 */
    private val typeList: MutableList<Int> = mutableListOf()

    /** 是否处于编辑模式 */
    private var isEditMode = false

    /** 列表当前是否全选 */
    private var allSelected = false

    // ===== 可配置文字 =====
    private var editText: String = "编辑"
    private var cancelEditText: String = "取消编辑"
    private var selectAllText: String = "全选"
    private var cancelSelectAllText: String = "取消全选"
    private var deleteText: String = "删除"

    // ===== 筛选 CheckBox 文字 =====
    private var errorText: String = "故障信息"
    private var warnText: String = "告警信息"

    // ===== 可配置背景 =====
    private var editBackground: Drawable? = null
    private var selectAllBackground: Drawable? = null
    private var deleteBackground: Drawable? = null

    // ===== 可配置左侧图标 =====
    private var editDrawableStart: Drawable? = null
    private var selectAllDrawableStart: Drawable? = null
    private var deleteDrawableStart: Drawable? = null

    private val mBinding: ViewLogBinding =
        ViewLogBinding.inflate(LayoutInflater.from(context), this, true)

    /**
     * 与外部交互的回调
     */
    interface Callback {
        /** 筛选类型变化 */
        fun onTypeChanged(types: List<Int>)

        /** 编辑模式切换 */
        fun onEditModeChanged(isEditMode: Boolean)

        /** 点击全选/取消全选，currentAllSelected 为点击前的状态 */
        fun onSelectAllClick(currentAllSelected: Boolean)

        /** 点击删除 */
        fun onDeleteClick()
    }

    var callback: Callback? = null

    /** 日志列表 */
    val recyclerView: RecyclerView
        get() = mBinding.rvSystemLog

    /** 当前筛选类型列表 */
    val selectedTypes: List<Int>
        get() = typeList.toList()

    /** 当前是否编辑模式 */
    fun isEditMode(): Boolean = isEditMode

    init {
        parseAttributes(attrs)
        applyActionBackgrounds()
        applyActionDrawables()
        applyFilterTexts()
        initFilterCheckBox()
        initActionArea()
        applyClickEffects()
    }

    /**
     * 解析 XML 自定义属性
     */
    private fun parseAttributes(attrs: AttributeSet?) {
        attrs ?: return
        val a = context.obtainStyledAttributes(attrs, R.styleable.LogView)
        editText = a.getString(R.styleable.LogView_editText) ?: editText
        cancelEditText = a.getString(R.styleable.LogView_cancelEditText) ?: cancelEditText
        selectAllText = a.getString(R.styleable.LogView_selectAllText) ?: selectAllText
        cancelSelectAllText =
            a.getString(R.styleable.LogView_cancelSelectAllText) ?: cancelSelectAllText
        deleteText = a.getString(R.styleable.LogView_deleteText) ?: deleteText
        editBackground =
            a.getDrawable(R.styleable.LogView_editBackground) ?: editBackground
        selectAllBackground =
            a.getDrawable(R.styleable.LogView_selectAllBackground) ?: selectAllBackground
        deleteBackground =
            a.getDrawable(R.styleable.LogView_deleteBackground) ?: deleteBackground
        editDrawableStart =
            a.getDrawable(R.styleable.LogView_editDrawableStart) ?: editDrawableStart
        selectAllDrawableStart =
            a.getDrawable(R.styleable.LogView_selectAllDrawableStart) ?: selectAllDrawableStart
        deleteDrawableStart =
            a.getDrawable(R.styleable.LogView_deleteDrawableStart) ?: deleteDrawableStart
        errorText = a.getString(R.styleable.LogView_errorText) ?: errorText
        warnText = a.getString(R.styleable.LogView_warnText) ?: warnText
        a.recycle()
    }

    /**
     * 设置日志列表 Adapter
     */
    fun setAdapter(adapter: RecyclerView.Adapter<*>) {
        mBinding.rvSystemLog.adapter = adapter
    }

    /**
     * 设置是否编辑模式
     */
    fun setEditMode(edit: Boolean) {
        if (edit == isEditMode) return
        isEditMode = edit
        updateEditActionViews()
        callback?.onEditModeChanged(edit)
    }

    /**
     * 同步全选状态，由外部根据列表选中情况调用
     */
    fun setAllSelected(allSelected: Boolean) {
        this.allSelected = allSelected
        mBinding.tvSelectAll.text = if (allSelected) cancelSelectAllText else selectAllText
    }

    /**
     * 设置三个操作按钮的文字（传 null 表示不修改）
     */
    fun setActionText(
        editText: String? = null,
        cancelEditText: String? = null,
        selectAllText: String? = null,
        cancelSelectAllText: String? = null,
        deleteText: String? = null
    ) {
        this.editText = editText ?: this.editText
        this.cancelEditText = cancelEditText ?: this.cancelEditText
        this.selectAllText = selectAllText ?: this.selectAllText
        this.cancelSelectAllText = cancelSelectAllText ?: this.cancelSelectAllText
        this.deleteText = deleteText ?: this.deleteText
        applyActionText()
    }

    /**
     * 设置三个操作按钮的背景（传 null 表示不修改）
     */
    fun setActionBackground(
        editBackground: Drawable? = null,
        selectAllBackground: Drawable? = null,
        deleteBackground: Drawable? = null
    ) {
        if (editBackground != null) {
            this.editBackground = editBackground
            mBinding.tvEdit.background = wrapWithRipple(editBackground, RIPPLE_EDIT)
        }
        if (selectAllBackground != null) {
            this.selectAllBackground = selectAllBackground
            mBinding.tvSelectAll.background = wrapWithRipple(selectAllBackground, RIPPLE_SELECT_ALL)
        }
        if (deleteBackground != null) {
            this.deleteBackground = deleteBackground
            mBinding.tvDelete.background = wrapWithRipple(deleteBackground, RIPPLE_DELETE)
        }
    }

    /**
     * 设置编辑按钮文字（非编辑状态 / 编辑状态）
     */
    fun setEditText(editText: String, cancelEditText: String) {
        this.editText = editText
        this.cancelEditText = cancelEditText
        applyActionText()
    }

    /**
     * 设置全选按钮文字（未全选 / 已全选）
     */
    fun setSelectAllText(selectAllText: String, cancelSelectAllText: String) {
        this.selectAllText = selectAllText
        this.cancelSelectAllText = cancelSelectAllText
        applyActionText()
    }

    /**
     * 设置删除按钮文字
     */
    fun setDeleteText(deleteText: String) {
        this.deleteText = deleteText
        applyActionText()
    }

    /**
     * 设置筛选 CheckBox 文字（故障/告警），传 null 表示不修改
     */
    fun setFilterText(errorText: String? = null, warnText: String? = null) {
        if (errorText != null) {
            this.errorText = errorText
            mBinding.checkErrorMsg.text = errorText
        }
        if (warnText != null) {
            this.warnText = warnText
            mBinding.checkWarnMsg.text = warnText
        }
    }

    /**
     * 设置故障筛选文字
     */
    fun setErrorText(errorText: String) {
        this.errorText = errorText
        mBinding.checkErrorMsg.text = errorText
    }

    /**
     * 设置告警筛选文字
     */
    fun setWarnText(warnText: String) {
        this.warnText = warnText
        mBinding.checkWarnMsg.text = warnText
    }

    /**
     * 设置编辑按钮背景
     */
    fun setEditBackground(background: Drawable?) {
        this.editBackground = background
        mBinding.tvEdit.background = wrapWithRipple(background, RIPPLE_EDIT)
    }

    /**
     * 设置全选按钮背景
     */
    fun setSelectAllBackground(background: Drawable?) {
        this.selectAllBackground = background
        mBinding.tvSelectAll.background = wrapWithRipple(background, RIPPLE_SELECT_ALL)
    }

    /**
     * 设置删除按钮背景
     */
    fun setDeleteBackground(background: Drawable?) {
        this.deleteBackground = background
        mBinding.tvDelete.background = wrapWithRipple(background, RIPPLE_DELETE)
    }

    /**
     * 设置三个操作按钮的左侧图标（传 null 表示不修改）
     */
    fun setActionDrawableStart(
        editDrawableStart: Drawable? = null,
        selectAllDrawableStart: Drawable? = null,
        deleteDrawableStart: Drawable? = null
    ) {
        if (editDrawableStart != null) {
            setEditDrawableStart(editDrawableStart)
        }
        if (selectAllDrawableStart != null) {
            setSelectAllDrawableStart(selectAllDrawableStart)
        }
        if (deleteDrawableStart != null) {
            setDeleteDrawableStart(deleteDrawableStart)
        }
    }

    /**
     * 设置编辑按钮左侧图标
     */
    fun setEditDrawableStart(drawable: Drawable?) {
        this.editDrawableStart = drawable
        mBinding.tvEdit.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
    }

    /**
     * 设置全选按钮左侧图标
     */
    fun setSelectAllDrawableStart(drawable: Drawable?) {
        this.selectAllDrawableStart = drawable
        mBinding.tvSelectAll.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
    }

    /**
     * 设置删除按钮左侧图标
     */
    fun setDeleteDrawableStart(drawable: Drawable?) {
        this.deleteDrawableStart = drawable
        mBinding.tvDelete.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
    }

    /**
     * 初始化筛选 CheckBox
     */
    private fun initFilterCheckBox() {
        if (mBinding.checkErrorMsg.isChecked) typeList.add(TYPE_ERROR)
        if (mBinding.checkWarnMsg.isChecked) typeList.add(TYPE_WARN)

        mBinding.checkErrorMsg.setOnCheckedChangeListener { _, isChecked ->
            handleFilterChanged(TYPE_ERROR, isChecked, mBinding.checkErrorMsg)
        }
        mBinding.checkWarnMsg.setOnCheckedChangeListener { _, isChecked ->
            handleFilterChanged(TYPE_WARN, isChecked, mBinding.checkWarnMsg)
        }
    }

    /**
     * 筛选变化处理，保证至少保留一个筛选类型
     */
    private fun handleFilterChanged(type: Int, isChecked: Boolean, checkBox: CheckBox) {
        if (isChecked) {
            if (!typeList.contains(type)) {
                typeList.add(type)
                notifyTypeChanged()
            }
        } else {
            if (typeList.contains(type) && typeList.size > 1) {
                typeList.remove(type)
                notifyTypeChanged()
            }
            if (typeList.contains(type) && typeList.size == 1) {
                checkBox.isChecked = true
            }
        }
    }

    private fun notifyTypeChanged() {
        callback?.onTypeChanged(selectedTypes)
    }

    /**
     * 初始化操作按钮
     */
    private fun initActionArea() {
        mBinding.tvEdit.setOnClickListener { setEditMode(!isEditMode) }
        mBinding.tvSelectAll.setOnClickListener {
            callback?.onSelectAllClick(allSelected)
        }
        mBinding.tvDelete.setOnClickListener { callback?.onDeleteClick() }
    }

    /**
     * 刷新按钮文字
     */
    private fun applyActionText() {
        mBinding.tvEdit.text = if (isEditMode) cancelEditText else editText
        mBinding.tvSelectAll.text = if (allSelected) cancelSelectAllText else selectAllText
        mBinding.tvDelete.text = deleteText
    }

    /**
     * 应用按钮背景（包裹点击水波纹效果）
     */
    private fun applyActionBackgrounds() {
        editBackground?.let { mBinding.tvEdit.background = wrapWithRipple(it, RIPPLE_EDIT) }
        selectAllBackground?.let {
            mBinding.tvSelectAll.background = wrapWithRipple(it, RIPPLE_SELECT_ALL)
        }
        deleteBackground?.let { mBinding.tvDelete.background = wrapWithRipple(it, RIPPLE_DELETE) }
    }

    /**
     * 为按钮背景包裹点击水波纹效果。
     * 背景可能来自 XML 属性或外部动态传入，统一在此包装以支持点击反馈。
     */
    private fun wrapWithRipple(background: Drawable?, rippleColor: Int): Drawable? {
        background ?: return null
        if (background is RippleDrawable) return background
        return RippleDrawable(ColorStateList.valueOf(rippleColor), background, null)
    }

    /**
     * 无自定义背景的按钮，使用前景水波纹兜底，保证点击有反馈
     */
    private fun applyClickEffects() {
        applyClickEffect(mBinding.tvEdit, RIPPLE_EDIT)
        applyClickEffect(mBinding.tvSelectAll, RIPPLE_SELECT_ALL)
        applyClickEffect(mBinding.tvDelete, RIPPLE_DELETE)
    }

    private fun applyClickEffect(view: View, rippleColor: Int) {
        if (view.background == null) {
            view.foreground = RippleDrawable(ColorStateList.valueOf(rippleColor), null, null)
        }
    }

    /**
     * 应用按钮左侧图标
     */
    private fun applyActionDrawables() {
        editDrawableStart?.let {
            mBinding.tvEdit.setCompoundDrawablesRelativeWithIntrinsicBounds(it, null, null, null)
        }
        selectAllDrawableStart?.let {
            mBinding.tvSelectAll.setCompoundDrawablesRelativeWithIntrinsicBounds(it, null, null, null)
        }
        deleteDrawableStart?.let {
            mBinding.tvDelete.setCompoundDrawablesRelativeWithIntrinsicBounds(it, null, null, null)
        }
    }

    /**
     * 应用筛选 CheckBox 文字
     */
    private fun applyFilterTexts() {
        mBinding.checkErrorMsg.text = errorText
        mBinding.checkWarnMsg.text = warnText
    }

    /**
     * 刷新编辑模式按钮显示状态
     */
    private fun updateEditActionViews() {
        if (isEditMode) {
            mBinding.tvSelectAll.visibility = View.VISIBLE
            mBinding.tvDelete.visibility = View.VISIBLE
            mBinding.tvEdit.text = cancelEditText
        } else {
            mBinding.tvSelectAll.visibility = View.GONE
            mBinding.tvDelete.visibility = View.GONE
            mBinding.tvEdit.text = editText
        }
    }
}
