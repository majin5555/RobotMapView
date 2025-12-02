//package com.siasun.dianshi.view
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Matrix
//import android.graphics.Paint
//import java.lang.ref.WeakReference
//
///**
// * 原点、上线点视图
// * 用于在地图上绘制原点和上线点位置及其标签
// */
//@SuppressLint("ViewConstructor")
//class OnlinePoseView(context: Context?, val parent: WeakReference<MapView>) :
//    SlamWareBaseView(context, parent) {
//
//    companion object {
//        private const val LINE_WIDTH = 3f
//        private const val TEXT_SIZE = 10f
//        private const val LABEL_OFFSET = 15f
//    }
//
//    private val paint = Paint().apply {
//        isAntiAlias = true
//        style = Paint.Style.FILL
//        strokeWidth = 1f
//    }
//
//    private val originMatrix = Matrix()
//    private val onlineMatrix = Matrix()
//
//    private var initPoseList: MutableList<InitPose> = mutableListOf()
//
//    // 上线点图标
//    private val onlinePointBitmap: Bitmap? by lazy {
//        BitmapFactory.decodeResource(resources, R.mipmap.online)
//    }
//
//    // 原点图标
//    private val originPointBitmap: Bitmap? by lazy {
//        BitmapFactory.decodeResource(resources, R.mipmap.origin_point)
//    }
//
//    @SuppressLint("DrawAllocation")
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//
//        // 获取MapViewNew实例，如果为空则不绘制
//        val mapView = parent.get() ?: return
//
//        // 设置绘制参数
//        paint.strokeWidth = LINE_WIDTH * scale
//        paint.textSize = TEXT_SIZE * scale
//
//        // 绘制原点
//        drawOriginPoint(canvas, mapView)
//
//        // 绘制上线点
//        drawInitPoses(canvas, mapView)
//    }
//
//    /**
//     * 绘制原点图标和标签
//     */
//    private fun drawOriginPoint(canvas: Canvas, mapView: MapView) {
//        originPointBitmap?.also { bitmap ->
//            paint.color = Color.GREEN
//
//            // 计算原点在视图中的位置
//            val originPoint = mapView.worldToScreen(0f, 0f)
//
//            // 重置并设置变换矩阵
//            originMatrix.reset()
//            // 先平移到目标位置
//            originMatrix.postTranslate(originPoint.x, originPoint.y)
//            // 然后应用缩放
//            originMatrix.postScale(scale, scale, originPoint.x, originPoint.y)
//            // 最后应用旋转（以图标中心为轴心）
//            originMatrix.postRotate(0f, originPoint.x, originPoint.y)
//
//            // 绘制原点图标
//            canvas.drawBitmap(bitmap, originMatrix, paint)
//
//            // 绘制原点标签
//            canvas.drawText(
//                context.getString(R.string.origin_point),
//                originPoint.x + LABEL_OFFSET,
//                originPoint.y + LABEL_OFFSET,
//                paint
//            )
//        }
//    }
//
//    /**
//     * 绘制所有上线点图标和标签
//     */
//    private fun drawInitPoses(canvas: Canvas, mapView: MapView) {
//        if (initPoseList.isEmpty()) return
//
//        paint.color = Color.BLACK
//
//        for (initPose in initPoseList) {
//            // 计算上线点在视图中的位置
//            val point = mapView.worldToScreen(
//                initPose.initPos[0],
//                initPose.initPos[1]
//            )
//
//            onlinePointBitmap?.also { bitmap ->
//                // 重置并设置变换矩阵
//                onlineMatrix.reset()
//                // 先平移到目标位置
//                onlineMatrix.postTranslate(point.x, point.y)
//                // 然后应用缩放
//                onlineMatrix.postScale(scale, scale, point.x, point.y)
//                // 最后应用旋转（以图标中心为轴心）
//                onlineMatrix.postRotate(
//                    -Math.toDegrees(initPose.initPos[2].toDouble()).toFloat(),
//                    point.x,
//                    point.y
//                )
//
//                // 绘制上线点图标
//                canvas.drawBitmap(bitmap, onlineMatrix, paint)
//
//                // 绘制上线点标签
//                canvas.drawText(
//                    initPose.name,
//                    point.x + LABEL_OFFSET,
//                    point.y + LABEL_OFFSET,
//                    paint
//                )
//            }
//        }
//    }
//
//    /**
//     * 设置上线点列表
//     */
//    fun setInitPosts(initPoseList: MutableList<InitPose>) {
//        this.initPoseList = initPoseList
//        postInvalidate()
//    }
//}
