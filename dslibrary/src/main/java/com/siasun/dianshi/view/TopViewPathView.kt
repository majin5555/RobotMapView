//package com.siasun.view
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import android.graphics.Path
//import android.graphics.PointF
//import com.pnc.core.framework.log.LogUtil
//import com.siasun.dianshi.view.MapView
//import org.json.JSONArray
//import org.json.JSONException
//import org.json.JSONObject
//import java.io.IOException
//import java.lang.ref.WeakReference
//
///**
// * 顶视路线
// */
//@SuppressLint("ViewConstructor")
//class TopViewPathView(context: Context?, var parent: WeakReference<MapView>) :
//    SlamWareBaseView(context, parent) {
//    private val LINE_WIDTH = 1f
//    private val mPaint: Paint = Paint()
//    private val routePath: Path = Path()
//    var dataArray: JSONArray? = null
//
//    // 用于减少绘制点数的参数，每隔10个点绘制一个
//    val sampleRate = 20
//
//    init {
//        mPaint.isAntiAlias = true
//        mPaint.style = Paint.Style.STROKE
//        mPaint.color = Color.parseColor("#CC33FF")
//        mPaint.strokeWidth = 1f
//
//        loadRouteData()
//    }
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        mPaint.strokeWidth = LINE_WIDTH * scale
//        routePath.reset()
//        var isFirstPoint = true
//
//        for (i in 0 until dataArray!!.length() step sampleRate) {
//            val pointObject = dataArray!!.getJSONObject(i)
//            val x = pointObject.getDouble("x").toFloat()
//            val y = pointObject.getDouble("y").toFloat()
//
//            val pnt: PointF = parent.get()!!.worldToScreen(x, y)
//
//            if (isFirstPoint) {
//                routePath.moveTo(pnt.x, pnt.y)
//                isFirstPoint = false
//            } else {
//                routePath.lineTo(pnt.x, pnt.y)
//            }
//        }
//        canvas.drawPath(routePath, mPaint)
//    }
//
//    fun setTopViewPath() {
//        postInvalidate()
//    }
//
//
//    private fun loadRouteData() {
//        try {
//            // 读取JSON文件
//            val `is` = context.assets.open("mergedPose.json")
//            val size = `is`.available()
//            val buffer = ByteArray(size)
//            `is`.read(buffer)
//            `is`.close()
//            val json = String(buffer, charset("UTF-8"))
//
//            // 解析JSON数据
//            val jsonObject = JSONObject(json)
//            dataArray = jsonObject.getJSONArray("data")
//
//
//            routePath.reset()
//            var isFirstPoint = true
//
//            for (i in 0 until dataArray!!.length() step sampleRate) {
//                val pointObject = dataArray!!.getJSONObject(i)
//                val x = pointObject.getDouble("x").toFloat()
//                val y = pointObject.getDouble("y").toFloat()
//
//                val pnt: PointF = parent.get()!!.worldToScreen(x, y)
//
//                if (isFirstPoint) {
//                    routePath.moveTo(pnt.x, pnt.y)
//                    isFirstPoint = false
//                } else {
//                    routePath.lineTo(pnt.x, pnt.y)
//                }
//            }
//
//            LogUtil.d("loadRouteData routePoints " + dataArray!!.length())
//        } catch (e: IOException) {
//            e.printStackTrace()
//        } catch (e: JSONException) {
//            e.printStackTrace()
//        }
//    }
//}