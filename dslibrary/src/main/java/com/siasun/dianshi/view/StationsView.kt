//package com.siasun.view
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import com.siasun.dianshi.view.MapView
//import java.lang.ref.WeakReference
//
///**
// * 站点(避让点)
// */
//@SuppressLint("ViewConstructor")
//class StationsView(context: Context?, var parent: WeakReference<MapView>) :
//    SlamWareBaseView(context, parent) {
//    private val LINE_WIDTH = 3f
//    private var mPaint: Paint = Paint()
//    private var radius = 15f
//
//    init {
//        mPaint.setColor(Color.RED)
//        mPaint.isAntiAlias = true
//        mPaint.style = Paint.Style.FILL
//        mPaint.strokeWidth = 1f
//    }
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        mPaint.strokeWidth = LINE_WIDTH * scale
//
////        val oriPoint = parent.get()!!.worldToScreen(
////            0f, 0f
////        )
////        if (oriPoint != null) {
////            LogUtil.d("oriPoint ${oriPoint}")
////            canvas.drawCircle(oriPoint.x , oriPoint.y , radius  , mPaint)
////        }
//    }
//
//    fun setStations() {
//        postInvalidate()
//    }
//}
