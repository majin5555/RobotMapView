package com.siasun.dianshi.view

import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.view.ViewGroup
import com.siasun.dianshi.utils.RadianUtil
import java.lang.ref.WeakReference

abstract class SlamWareBaseView(context: Context?, parent: WeakReference<MapView>) :
    ViewGroup(context) {
    var mParent: WeakReference<MapView>
    var scale: Float protected set
    var mRotation: Float//旋转角度
    var mMatrix: Matrix//矩阵


    init {
        setBackgroundColor(Color.TRANSPARENT)
        setWillNotDraw(false)
        mParent = parent
        scale = 1.0f
        mRotation = 0f
        mMatrix = Matrix()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {}
    open fun setMatrix(mMatrix: Matrix) {
        this.mMatrix = mMatrix
        postInvalidate()
    }

    open fun setMatrixWithScale(matrix: Matrix, scale: Float) {
        mMatrix = matrix
        this.scale = scale
        postInvalidate()
    }

    open fun setMatrixWithRotation(matrix: Matrix, rotation: Float) {
        mMatrix = matrix
        mRotation += RadianUtil.toAngel(rotation)
        postInvalidate()
    }
}
