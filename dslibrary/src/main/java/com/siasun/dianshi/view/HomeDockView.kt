//package com.siasun.view
//
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import com.siasun.dianshi.view.MapView
//import com.siasun.view.SlamWareBaseView
//import java.lang.ref.WeakReference
//
///**
// * 充电桩（充点电）位姿信息
// */
//class HomeDockView(context: Context?, parent: WeakReference<MapView>) :
//    SlamWareBaseView(context, parent) {
//    private var mPaint: Paint = Paint()
//
//    init {
//        mPaint.setColor(Color.GREEN)
//        mPaint.strokeWidth = 1f
//        mPaint.isAntiAlias = true
//        mPaint.style = Paint.Style.FILL
//    }
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//
//    }
//
//    fun setHomePose( ) {
//        postInvalidate()
//    }
//}
