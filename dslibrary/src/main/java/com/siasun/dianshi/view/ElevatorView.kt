package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.siasun.dianshi.R
import com.siasun.dianshi.bean.ElevatorPoint
import java.lang.ref.WeakReference

/**
 * 乘梯点
 */
@SuppressLint("ViewConstructor")
class ElevatorView(context: Context?, parent: WeakReference<MapView>) :
    SlamWareBaseView<MapView>(context, parent) {

    // 乘梯点编辑监听器接口
    interface OnElevatorEditListener {
        fun onElevatorEdit(elevator: ElevatorPoint)
    }

    // 乘梯点删除监听器接口
    interface OnElevatorDeleteListener {
        fun onElevatorDelete(elevator: ElevatorPoint)
    }

    // 监听器实例
    private var onElevatorEditListener: OnElevatorEditListener? = null
    private var onElevatorDeleteListener: OnElevatorDeleteListener? = null

    // 当前工作模式
    private var currentWorkMode: WorkMode = WorkMode.MODE_SHOW_MAP

    // 绘图参数常量
    private companion object {
        const val BASE_RADIUS = 10f
        const val BASE_TEXT_SIZE = 10f

        // 使用伴生对象存储mPaint，避免重复创建
        val mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 1f
            style = Paint.Style.FILL
            textSize = BASE_TEXT_SIZE
        }
    }

    // 乘梯点 - 使用线程安全的列表
    private var elevatorsList = mutableListOf<ElevatorPoint>()

    // 控制是否绘制
    private var isDrawingEnabled: Boolean = true

    // 用于检测点击是否在乘梯点附近的阈值
    private val clickThreshold = 20f // 像素单位

    /**
     * 设置乘梯点数据
     */
    fun setElevators(list: MutableList<ElevatorPoint>?) {
        list?.let {
            synchronized(elevatorsList) {
                elevatorsList.clear()
                elevatorsList.addAll(it)
            }
            postInvalidate()
        }
    }

    /**
     * 获取乘梯点数据
     */
    fun getElevators(): MutableList<ElevatorPoint> {
        return elevatorsList
    }

    /**
     * 清除一体机数据
     */
    private fun clearElevators() {
        synchronized(elevatorsList) {
            elevatorsList.clear()
        }
        postInvalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isDrawingEnabled) return

        val mapView = mParent.get() ?: return

        // 创建局部副本避免并发修改问题
        val elevatorsCopy: List<ElevatorPoint>
        synchronized(elevatorsList) {
            elevatorsCopy = elevatorsList.toList()
        }

        // 预加载字符串资源，避免在循环中重复获取
        val gatePointText = context.getString(R.string.gate_point)
        val pstParkPointText = context.getString(R.string.pst_park_point)
        val waitPointText = context.getString(R.string.station3)

        // 复用PointF对象，减少内存分配
        val pointLocation = PointF()

        for (item in elevatorsCopy) {
            item.gatePoint?.let { gate ->
                mPaint.color = Color.GRAY
                // 获取世界坐标转屏幕坐标的结果
                val screenPoint = mapView.worldToScreen(gate.x, gate.y)
                // 将结果赋值给pointLocation对象
                pointLocation.set(screenPoint.x, screenPoint.y)

                // 绘制乘梯点本体
                drawCircle(canvas, pointLocation, BASE_RADIUS, mPaint)

                // 根据工作模式绘制外圈圆环
                if (currentWorkMode == WorkMode.MODE_ELEVATOR_EDIT) {
                    // 编辑模式：绘制绿色圆环
                    mPaint.color = Color.GREEN
                    mPaint.style = Paint.Style.STROKE
                    mPaint.strokeWidth = 2f
                    drawCircle(canvas, pointLocation, BASE_RADIUS + 5f, mPaint)
                    // 恢复填充样式
                    mPaint.style = Paint.Style.FILL
                } else if (currentWorkMode == WorkMode.MODE_ELEVATOR_DELETE) {
                    // 删除模式：绘制红色圆环
                    mPaint.color = Color.RED
                    mPaint.style = Paint.Style.STROKE
                    mPaint.strokeWidth = 2f
                    drawCircle(canvas, pointLocation, BASE_RADIUS + 5f, mPaint)
                    // 恢复填充样式
                    mPaint.style = Paint.Style.FILL
                }

                pointLocation.x += 10f
                pointLocation.y += 10f
                mPaint.color = Color.GRAY
                drawLabel(canvas, waitPointText, pointLocation, mPaint)
            }

            item.pstPark?.let { pstPark ->
                mPaint.color = Color.GRAY
                // 获取世界坐标转屏幕坐标的结果
                val screenPoint = mapView.worldToScreen(pstPark.x, pstPark.y)
                // 将结果赋值给pointLocation对象
                pointLocation.set(screenPoint.x, screenPoint.y)

                // 绘制乘梯点本体
                drawCircle(canvas, pointLocation, BASE_RADIUS, mPaint)

                // 根据工作模式绘制外圈圆环
                if (currentWorkMode == WorkMode.MODE_ELEVATOR_EDIT) {
                    // 编辑模式：绘制绿色圆环
                    mPaint.color = Color.GREEN
                    mPaint.style = Paint.Style.STROKE
                    mPaint.strokeWidth = 2f
                    drawCircle(canvas, pointLocation, BASE_RADIUS + 5f, mPaint)
                    // 恢复填充样式
                    mPaint.style = Paint.Style.FILL
                } else if (currentWorkMode == WorkMode.MODE_ELEVATOR_DELETE) {
                    // 删除模式：绘制红色圆环
                    mPaint.color = Color.RED
                    mPaint.style = Paint.Style.STROKE
                    mPaint.strokeWidth = 2f
                    drawCircle(canvas, pointLocation, BASE_RADIUS + 5f, mPaint)
                    // 恢复填充样式
                    mPaint.style = Paint.Style.FILL
                }


                pointLocation.x += 10f
                pointLocation.y += 10f
                mPaint.color = Color.GRAY
                drawLabel(
                    canvas, pstParkPointText, pointLocation, mPaint
                )
            }

            item.waitPoint?.let { waitPoint ->
                mPaint.color = Color.GRAY
                // 获取世界坐标转屏幕坐标的结果
                val screenPoint = mapView.worldToScreen(waitPoint.x, waitPoint.y)
                // 将结果赋值给pointLocation对象
                pointLocation.set(screenPoint.x, screenPoint.y)

                // 绘制乘梯点本体
                drawCircle(canvas, pointLocation, BASE_RADIUS, mPaint)

                // 根据工作模式绘制外圈圆环
                if (currentWorkMode == WorkMode.MODE_ELEVATOR_EDIT) {
                    // 编辑模式：绘制绿色圆环
                    mPaint.color = Color.GREEN
                    mPaint.style = Paint.Style.STROKE
                    mPaint.strokeWidth = 2f
                    drawCircle(canvas, pointLocation, BASE_RADIUS + 5f, mPaint)
                    // 恢复填充样式
                    mPaint.style = Paint.Style.FILL
                } else if (currentWorkMode == WorkMode.MODE_ELEVATOR_DELETE) {
                    // 删除模式：绘制红色圆环
                    mPaint.color = Color.RED
                    mPaint.style = Paint.Style.STROKE
                    mPaint.strokeWidth = 2f
                    drawCircle(canvas, pointLocation, BASE_RADIUS + 5f, mPaint)
                    // 恢复填充样式
                    mPaint.style = Paint.Style.FILL
                }


                pointLocation.x += 10f
                pointLocation.y += 10f
                mPaint.color =  Color.GRAY
                drawLabel(
                    canvas, pstParkPointText, pointLocation, mPaint
                )
            }
        }
    }


    /**
     * 设置是否启用绘制
     */
    fun setDrawingEnabled(enabled: Boolean) {
        this.isDrawingEnabled = enabled
        postInvalidate()
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(workMode: WorkMode) {
        this.currentWorkMode = workMode
    }

    /**
     * 设置乘梯点编辑监听器
     */
    fun setOnElevatorEditListener(listener: OnElevatorEditListener?) {
        this.onElevatorEditListener = listener
    }

    /**
     * 设置乘梯点删除监听器
     */
    fun setOnElevatorDeleteListener(listener: OnElevatorDeleteListener?) {
        this.onElevatorDeleteListener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) {
            return super.onTouchEvent(event)
        }

        val mapView = mParent.get() ?: return super.onTouchEvent(event)

        // 只有在编辑或删除模式下才处理点击事件
        if (currentWorkMode != WorkMode.MODE_ELEVATOR_EDIT &&
            currentWorkMode != WorkMode.MODE_ELEVATOR_DELETE
        ) {
            return super.onTouchEvent(event)
        }

        // 获取点击的屏幕坐标
        val screenX = event.x
        val screenY = event.y

        // 转换为世界坐标
        val worldPoint = mapView.screenToWorld(screenX, screenY)

        // 查找点击位置附近的乘梯点
        val clickedElevator = findElevatorNearPoint(worldPoint)

        clickedElevator?.let {
            when (currentWorkMode) {
                WorkMode.MODE_ELEVATOR_EDIT -> {
                    onElevatorEditListener?.onElevatorEdit(it)
                }

                WorkMode.MODE_ELEVATOR_DELETE -> {
                    onElevatorDeleteListener?.onElevatorDelete(it)
                    postInvalidate()
                }

                else -> {}
            }
            return true
        }

        return super.onTouchEvent(event)
    }

    /**
     * 查找指定世界坐标点附近的乘梯点
     */
    private fun findElevatorNearPoint(worldPoint: PointF): ElevatorPoint? {
        val mapView = mParent.get() ?: return null

        val elevatorsCopy: List<ElevatorPoint>
        synchronized(elevatorsList) {
            elevatorsCopy = elevatorsList.toList()
        }

        for (elevator in elevatorsCopy) {
            // 检查gatePoint
            elevator.gatePoint?.let {
                val gateScreenPoint = mapView.worldToScreen(it.x, it.y)
                if (isPointNearScreenPoint(gateScreenPoint, worldPoint, mapView)) {
                    return elevator
                }
            }

            // 检查pstPark
            elevator.pstPark?.let {
                val parkScreenPoint = mapView.worldToScreen(it.x, it.y)
                if (isPointNearScreenPoint(parkScreenPoint, worldPoint, mapView)) {
                    return elevator
                }
            }
        }

        return null
    }

    /**
     * 检查两个点是否在屏幕上足够接近
     */
    private fun isPointNearScreenPoint(
        screenPoint: PointF,
        worldPoint: PointF,
        mapView: MapView
    ): Boolean {
        // 将世界坐标转换为屏幕坐标
        val worldScreenPoint = mapView.worldToScreen(worldPoint.x, worldPoint.y)

        // 计算屏幕距离
        val dx = screenPoint.x - worldScreenPoint.x
        val dy = screenPoint.y - worldScreenPoint.y
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble())

        return distance <= clickThreshold
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理资源，防止内存泄漏
        clearElevators()
        // 设置为不可绘制，避免在视图分离后继续绘制
        isDrawingEnabled = false
    }
}
