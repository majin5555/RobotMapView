//package com.siasun.view
//
//import VirtualWallLineNew
//import android.annotation.SuppressLint
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Paint
//import com.siasun.dianshi.view.MapView
//import java.lang.ref.WeakReference
//
//@SuppressLint("ViewConstructor")
//class VirtualLineView(
//    context: Context?, var parent: WeakReference<MapView>, mColor: Int
//) : SlamWareBaseView(context, parent) {
//    private val LINE_WIDTH = 1f
//    private var radius = 5f
//
//    private val PROPORTION = 1000//虚拟墙文件上的是毫米 在本地显示要除1000
//
//    private val mPaint: Paint = Paint()
//    private var lines: MutableList<VirtualWallLineNew>? = null
//
//    init {
//        mPaint.isAntiAlias = true
//        mPaint.style = Paint.Style.FILL
//        mPaint.setColor(mColor)
//        mPaint.strokeWidth = 1f
//    }
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        lines?.let {
//            mPaint.strokeWidth = LINE_WIDTH * scale
//
//            for (line in it) {
//                val start = parent.get()!!.worldToScreen(
//                    line.BEGIN.X / PROPORTION, line.BEGIN.Y / PROPORTION
//                )
//                val end = parent.get()!!.worldToScreen(
//                    line.END.X / PROPORTION, line.END.Y / PROPORTION
//                )
//                canvas.drawLine(start.x, start.y, end.x, end.y, mPaint)
//                canvas.drawCircle(start.x, start.y, radius * scale, mPaint)
//                canvas.drawCircle(end.x, end.y, radius * scale, mPaint)
//            }
//        }
//    }
//
//    /**
//     * 设置虚拟墙
//     */
//    fun setLines(lines: MutableList<VirtualWallLineNew>) {
//        this.lines = lines
//        postInvalidate()
//    }
//}
//
