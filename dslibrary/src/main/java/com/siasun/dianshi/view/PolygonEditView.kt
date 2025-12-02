//package com.siasun.view
//
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import android.graphics.Path
//import android.graphics.PointF
//import android.view.GestureDetector
//import android.view.GestureDetector.SimpleOnGestureListener
//import android.view.MotionEvent
//import android.view.ScaleGestureDetector
//import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
//import com.siasun.dianshi.view.MapView
//import com.siasun.view.SlamWareBaseView
//import java.lang.ref.WeakReference
//import kotlin.math.hypot
//import kotlin.math.max
//import kotlin.math.min
//
//class PolygonEditView(context: Context?, parent: WeakReference<MapView>) :
//    SlamWareBaseView(context, parent) {
//    private var polygonPaint: Paint? = null
//    private var pointPaint: Paint? = null
//    private var edgePaint: Paint? = null
//    private var points: MutableList<PointF>? = null
//    private var selectedPointIndex = -1
//    private val touchRadius = 60f // 点击半径
//
//    // 平移缩放
//    private var scaleFactor = 1.0f
//    private var offsetX = 0f
//    private var offsetY = 0f
//    private var lastTouchX = 0f
//    private var lastTouchY = 0f
//    private var isDraggingMap = false
//    private var gestureDetector: GestureDetector? = null
//    private var scaleDetector: ScaleGestureDetector? = null
//
//    init {
//        polygonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
//        polygonPaint!!.setColor(Color.RED)
//        polygonPaint!!.style = Paint.Style.STROKE
//        polygonPaint!!.strokeWidth = 5f
//        pointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
//        pointPaint!!.setColor(Color.RED)
//        pointPaint!!.style = Paint.Style.FILL
//        edgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
//        edgePaint!!.setColor(Color.GRAY)
//        edgePaint!!.style = Paint.Style.FILL
//
//        // 初始矩形
//        points = ArrayList()
//        (points as ArrayList<PointF>).add(PointF(300f, 300f))
//        (points as ArrayList<PointF>).add(PointF(600f, 300f))
//        (points as ArrayList<PointF>).add(PointF(600f, 600f))
//        (points as ArrayList<PointF>).add(PointF(300f, 600f))
//
//        // 单击/双击检测
//        gestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
//            override fun onDoubleTap(e: MotionEvent): Boolean {
//                // 双击删除顶点
//                val touch = screenToWorld(e.x, e.y)
//                for (i in (points as ArrayList<PointF>).indices) {
//                    val p = (points as ArrayList<PointF>).get(i)
//                    if (hypot(
//                            (p.x - touch.x).toDouble(), (p.y - touch.y).toDouble()
//                        ) < touchRadius / scaleFactor
//                    ) {
//                        if ((points as ArrayList<PointF>).size > 3) {
//                            (points as ArrayList<PointF>).removeAt(i)
//                            invalidate()
//                        }
//                        break
//                    }
//                }
//                return true
//            }
//
//            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
//                // 单击边缘 → 插入新点
//                val touch = screenToWorld(e.x, e.y)
//                val insertIndex = findEdgeNear(touch.x, touch.y)
//                if (insertIndex != -1) {
//                    (points as ArrayList<PointF>).add(insertIndex + 1, touch)
//                    invalidate()
//                }
//                return true
//            }
//        })
//
//        // 缩放手势检测
//        scaleDetector = context?.let {
//            ScaleGestureDetector(it, object : SimpleOnScaleGestureListener() {
//                override fun onScale(detector: ScaleGestureDetector): Boolean {
//                    scaleFactor *= detector.getScaleFactor()
//                    scaleFactor = max(0.5, min(scaleFactor.toDouble(), 3.0)).toFloat() // 限制缩放范围
//                    invalidate()
//                    return true
//                }
//            })
//        }
//    }
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        canvas.save()
//        // 平移 + 缩放
//        canvas.translate(offsetX, offsetY)
//        canvas.scale(scaleFactor, scaleFactor)
//
//        // 多边形
//        if (points!!.size > 1) {
//            val path = Path()
//            path.moveTo(points!![0].x, points!![0].y)
//            for (i in 1 until points!!.size) {
//                path.lineTo(points!![i].x, points!![i].y)
//            }
//            path.close()
//            canvas.drawPath(path, polygonPaint!!)
//        }
//
//        // 顶点
//        for (p in points!!) {
//            canvas.drawCircle(p.x, p.y, 20f / scaleFactor, pointPaint!!)
//        }
//
//        // 边缘中点 + 符号
//        for (i in points!!.indices) {
//            val a = points!![i]
//            val b = points!![(i + 1) % points!!.size]
//            val midX = (a.x + b.x) / 2
//            val midY = (a.y + b.y) / 2
//            canvas.drawCircle(midX, midY, 15f / scaleFactor, edgePaint!!)
//            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
//            textPaint.setColor(Color.WHITE)
//            textPaint.textSize = 30f / scaleFactor
//            textPaint.textAlign = Paint.Align.CENTER
//            canvas.drawText("+", midX, midY + 10f / scaleFactor, textPaint)
//        }
//        canvas.restore()
//    }
//
//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        scaleDetector!!.onTouchEvent(event)
//        gestureDetector!!.onTouchEvent(event)
//        val x = event.x
//        val y = event.y
//        val world = screenToWorld(x, y)
//        when (event.actionMasked) {
//            MotionEvent.ACTION_DOWN -> {
//                // 检查是否点中顶点
//                selectedPointIndex = -1
//                var i = 0
//                while (i < points!!.size) {
//                    val p = points!![i]
//                    if (hypot(
//                            (p.x - world.x).toDouble(), (p.y - world.y).toDouble()
//                        ) < touchRadius / scaleFactor
//                    ) {
//                        selectedPointIndex = i
//                        break
//                    }
//                    i++
//                }
//                if (selectedPointIndex == -1) {
//                    isDraggingMap = true
//                    lastTouchX = x
//                    lastTouchY = y
//                }
//            }
//
//            MotionEvent.ACTION_MOVE -> if (selectedPointIndex != -1) {
//                points!![selectedPointIndex][world.x] = world.y
//                invalidate()
//            } else if (isDraggingMap && !scaleDetector!!.isInProgress) {
//                val dx = x - lastTouchX
//                val dy = y - lastTouchY
//                offsetX += dx
//                offsetY += dy
//                lastTouchX = x
//                lastTouchY = y
//                invalidate()
//            }
//
//            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                selectedPointIndex = -1
//                isDraggingMap = false
//            }
//        }
//        return true
//    }
//
//    // 判断是否点中了边缘附近
//    private fun findEdgeNear(x: Float, y: Float): Int {
//        for (i in points!!.indices) {
//            val a = points!![i]
//            val b = points!![(i + 1) % points!!.size]
//            val midX = (a.x + b.x) / 2
//            val midY = (a.y + b.y) / 2
//            if (hypot((midX - x).toDouble(), (midY - y).toDouble()) < touchRadius / scaleFactor) {
//                return i
//            }
//        }
//        return -1
//    }
//
//    // 屏幕坐标 -> 世界坐标
//    private fun screenToWorld(x: Float, y: Float): PointF {
//        val worldX = (x - offsetX) / scaleFactor
//        val worldY = (y - offsetY) / scaleFactor
//        return PointF(worldX, worldY)
//    }
//
//    val polygonPoints: List<PointF>
//        // 获取多边形点坐标
//        get() = ArrayList(points)
//}
