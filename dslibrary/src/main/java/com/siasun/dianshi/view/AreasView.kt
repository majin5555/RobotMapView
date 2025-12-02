//package com.siasun.view
//
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import android.graphics.Path
//import android.graphics.PointF
//import android.view.GestureDetector
//import android.view.MotionEvent
//import android.view.View
//import bean.area.CleanAreaNew
//import com.siasun.dianshi.view.MapView
//import com.siasun.view.SlamWareBaseView
//import newmapeditorlibofagv.Robot.VirtualWall.PointNew
//import org.json.JSONObject
//import java.io.InputStream
//import java.lang.ref.WeakReference
//
///**
// * 区域绘制
// */
//class AreasView(context: Context?, parent: WeakReference<MapView>) :
//    SlamWareBaseView(context, parent), View.OnTouchListener {
//
//    private var mPaint: Paint = Paint()
//    private var mTextPaint: Paint = Paint()
//    private var mFillPaint: Paint = Paint()
//    private var pointPaint: Paint = Paint()
//    private var plusPaint: Paint = Paint() // 用于绘制加号的画笔
//
//    private var areaData: JSONObject? = null
//    private var radius = 5f
//    private val LINE_WIDTH = 1f
//    private val plusSize = 10f // 加号大小
//    private val touchRadius = 20f // 顶点点击判定半径
//
//    // 手势检测器
//    private var gestureDetector: GestureDetector
//    val areaList: MutableList<CleanAreaNew> = mutableListOf()
//
//    init {
//        // 边框画笔
//        pointPaint.strokeWidth = 1f
//        pointPaint.isAntiAlias = true
//        pointPaint.style = Paint.Style.FILL
//        pointPaint.color = Color.BLACK
//
//        // 边框画笔
//        mPaint.strokeWidth = 1f
//        mPaint.isAntiAlias = true
//        mPaint.style = Paint.Style.STROKE
//        mPaint.color = Color.BLACK
//
//        // 填充画笔
//        mFillPaint.strokeWidth = 1f
//        mFillPaint.isAntiAlias = true
//        mFillPaint.style = Paint.Style.FILL
//        mFillPaint.color = Color.argb(50, 0, 0, 255) // 半透明蓝色
//
//        // 文字画笔
//        mTextPaint.strokeWidth = 2f
//        mTextPaint.isAntiAlias = true
//        mTextPaint.style = Paint.Style.FILL
//        mTextPaint.color = Color.BLACK
//        mTextPaint.textSize = 30f
//
//        // 加号画笔
//        plusPaint.strokeWidth = 2f
//        plusPaint.isAntiAlias = true
//        plusPaint.color = Color.RED // 使用红色以便区分
//
//        // 加载区域数据
//        loadAreaData()
//
//        // 设置触摸监听器
//        setOnTouchListener(this)
//
//        // 初始化手势检测器
//        gestureDetector =
//            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
//                override fun onDoubleTap(e: MotionEvent): Boolean {
//                    handleDoubleTap(e)
//                    return true
//                }
//            })
//    }
//
//    private fun loadAreaData() {
//        try {
//            val inputStream: InputStream = context.assets.open("PadAreas.json")
//            val size: Int = inputStream.available()
//            val buffer = ByteArray(size)
//            inputStream.read(buffer)
//            inputStream.close()
//            val json = String(buffer, Charsets.UTF_8)
//            areaData = JSONObject(json)
//
//
//            // 遍历所有区域，检查是否双击在顶点上
//            areaData?.let { json ->
//                val cleanAreas = json.getJSONArray("cleanAreas")
//
//                for (i in 0 until cleanAreas.length()) {
//                    val area = cleanAreas.getJSONObject(i)
//                    val name = area.getString("sub_name")
//                    val vertices = area.getJSONArray("m_VertexPnt")
//                    val areaNew = CleanAreaNew()
//
//                    // 遍历顶点检查双击位置
//                    for (j in 0 until vertices.length()) {
//                        val vertex = vertices.getJSONObject(j)
//                        val x = vertex.getDouble("X").toFloat()
//                        val y = vertex.getDouble("Y").toFloat()
//                        areaNew.m_VertexPnt.add(PointNew(x, y))
//                    }
//                    areaNew.sub_name = name
//                    areaList.add(areaNew)
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    // 绘制加号的方法
//    private fun drawPlus(canvas: Canvas, x: Float, y: Float) {
//        // 绘制水平线
//        canvas.drawLine(x - plusSize, y, x + plusSize, y, plusPaint)
//        // 绘制垂直线
//        canvas.drawLine(x, y - plusSize, x, y + plusSize, plusPaint)
//    }
//
//    // 处理双击事件
//    private fun handleDoubleTap(event: MotionEvent) {
////        // 获取MapView实例
////        val mapView = mParent.get() ?: return
////
////        // 将屏幕坐标转换为世界坐标
////        val screenX = event.x
////        val screenY = event.y
////
////
////        // 遍历顶点检查双击位置
////        for (j in 0 until areaList.size) {
////            val vertices = areaList.get(j).m_VertexPnt
////
////            // 将世界坐标转换为屏幕坐标
////            val screenPoint = mapView.worldToScreen(x, y)
////
////            // 检查是否点击在顶点附近
////            if (hypot(
////                    (screenPoint.x - screenX).toDouble(),
////                    (screenPoint.y - screenY).toDouble()
////                ) < touchRadius
////            ) {
////                // 确保多边形至少保留3个顶点
////                if (vertices.size > 3) {
////                    // 创建新的顶点数组，移除被双击的顶点
////                    val newVertices = JSONArray()
////                    for (k in 0 until vertices.size) {
////                        if (k != j) {
////                            newVertices.put(vertices.getJSONObject(k))
////                        }
////                    }
////
////                    // 更新区域的顶点数据
////                    area.put("m_VertexPnt", newVertices)
////
////                    // 重新绘制
////                    invalidate()
////
////                    // 找到后返回，避免继续遍历
////                    return
////                }
////            }
////        }
//
//    }
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        pointPaint.strokeWidth = LINE_WIDTH * scale
//        plusPaint.strokeWidth = 2f * scale // 确保加号大小随缩放变化
//
//        // 获取MapView实例
//        val mapView = mParent.get() ?: return
//
//        for (j in 0 until areaList.size) {
//            val cleanAreaNew = areaList[j]
//            // 创建路径
//            val path = Path()
//
//            // 用于存储上一个顶点的屏幕坐标
//            var prevScreenPoint: PointF? = null
//
//            // 转换顶点坐标并构建路径
//            for (j in 0 until cleanAreaNew.m_VertexPnt.size) {
//                val pointNew = cleanAreaNew.m_VertexPnt[j]
//                val x = pointNew.X
//                val y = pointNew.Y
//
//                // 将世界坐标转换为屏幕坐标
//                val screenPoint = mapView.worldToScreen(x, y)
//
//                if (j == 0) {
//                    path.moveTo(screenPoint.x, screenPoint.y)
//                } else {
//                    path.lineTo(screenPoint.x, screenPoint.y)
//
//                    // 如果有上一个顶点，计算中点并绘制加号
//                    prevScreenPoint?.let {
//                        val midX = (it.x + screenPoint.x) / 2
//                        val midY = (it.y + screenPoint.y) / 2
//                        drawPlus(canvas, midX, midY)
//                    }
//                }
//                // 顶点
//                canvas.drawCircle(screenPoint.x, screenPoint.y, radius, pointPaint)
//
//                // 保存当前顶点作为下一次循环的上一个顶点
//                prevScreenPoint = screenPoint
//            }
//
//            // 闭合路径
//            path.close()
//            // 绘制填充背景
//            canvas.drawPath(path, mFillPaint)
//            // 绘制边框
//            canvas.drawPath(path, mPaint)
//
////            // 计算区域中心点用于绘制名称
////            if (cleanAreaNew.m_VertexPnt.size > 0) {
////                val firstVertex = vertices.getJSONObject(0)
////                val centerX = firstVertex.getDouble("X").toFloat()
////                val centerY = firstVertex.getDouble("Y").toFloat()
////                val centerPoint = mapView.worldToScreen(centerX, centerY)
////
////                // 绘制区域名称
////                canvas.drawText(name, centerPoint.x, centerPoint.y, mTextPaint)
////            }
////
//        }
//    }
//
//    // 实现触摸事件处理
//    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
//        if (event != null) {
//            return gestureDetector.onTouchEvent(event)
//        }
//        return false
//    }
//
//    fun setStations() {
//        postInvalidate()
//    }
//}
