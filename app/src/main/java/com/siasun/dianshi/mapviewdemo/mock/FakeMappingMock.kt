package com.siasun.dianshi.mapviewdemo.mock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Debug
import android.os.SystemClock
import android.util.Log
import android.os.Handler
import android.os.Looper
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.view.WorkMode
import com.siasun.dianshi.view.createMap.map3D.CreateMapView3D
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 3D 建图（推图）假数据源 —— 仅 debug 测试用。
 *
 * 数据格式严格对齐现场代码的实际读取方式：
 *  - ranges[0..5]           机器人位姿 x, y, theta(rad), z, roll, pitch
 *  - ranges[6 + i*6 .. +5]  第 i 个激光点（只读前 2 个分量，后 4 位补 0）
 *  - intensities[0..4]      height, width, originX, originY, resolution（type=1 扩展地图用）
 *  - rad0                   建图步数 = 关键帧 id；-1 表示"非关键帧"
 */
object FakeMappingMock {

    private const val TAG = "FakeMappingMock"
    private const val MEM_TAG = "MOCKMEM"

    /**
     * 假数据配置。
     *
     * 注意分辨率：地图像素 = worldW / res × worldH / res。
     * 500m×1000m 若用真机 PM.yaml 的 0.05 m/px，就是 10000×20000 = 4 亿像素，
     * RGB_565 单张就要 400MB，必然 OOM。故大地图默认降到 0.25 m/px（800 万像素）。
     */
    data class Config(
        val worldW: Float = 500f,      // 横向 500m
        val worldH: Float = 1000f,     // 纵向 1000m
        val res: Float = 0.25f,        // m/px
        val rays: Int = 1500,          // 每帧点位数（库内关键帧按 1/5 采样，1500 → 约 300 黑点/帧）
        val fps: Int = 10,             // 1 秒 10 帧
        val stepPerFrame: Float = 0.30f,// 每帧前进 0.3m = 1 秒 10 步（建图步数/秒 == fps）
        val viewScale: Float = 2.0f,   // 测试用初始缩放（0 表示不干预，使用自适应全图）
        val withOptPose: Boolean = false // 是否注入回环优化（会改写关键帧位姿，默认关）
    ) {
        val pxW: Int get() = (worldW / res).toInt()
        val pxH: Int get() = (worldH / res).toInt()
        val originX: Float get() = -worldW / 2f
        val originY: Float get() = -worldH / 2f
    }

    private var cfg = Config()
    private var job: Job? = null
    // 暂停：不发帧、不推进里程（等价"机器人停在原地、数据停发"）
    @Volatile
    private var paused = false
    // 直接驱动 view：不走 LiveEventBus，避免与真机通路互相覆盖（也与现场回调同样落在主线程）
    private val mainHandler = Handler(Looper.getMainLooper())
    private var wallsCache: List<RectF> = emptyList()

    // ────────────────────────────── 地图文件 ──────────────────────────────

    /** 生成假地图 PNG + YAML，返回 (pngPath, yamlPath)。写到 App 私有目录，不动真机真实地图。 */
    @Synchronized
    fun ensureMapFiles(ctx: Context, config: Config = Config()): Pair<String, String> {
        cfg = config
        val pxW = config.pxW
        val pxH = config.pxH
        val totalPx = pxW.toLong() * pxH
        Log.i(TAG, "生成假地图 ${config.worldW.toInt()}m×${config.worldH.toInt()}m " +
                "res=${config.res} -> ${pxW}×${pxH}px (${totalPx / 10000}万像素)")
        if (totalPx > 40_000_000L) {
            Log.w(TAG, "警告：像素数 ${totalPx}，RGB_565 预计占用 ${totalPx * 2 / 1024 / 1024}MB，很可能 OOM")
        }

        val dir = File(ctx.getExternalFilesDir(null), "mockmap").apply { mkdirs() }
        val png = File(dir, "PM.png")
        val yaml = File(dir, "PM.yaml")

        val bmp = Bitmap.createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // 未知区（浅蓝，对齐真机配色）
        canvas.drawColor(Color.rgb(174, 234, 254))
        // 自由区（米黄）
        val free = Paint().apply { color = Color.rgb(255, 255, 214); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, pxW.toFloat(), pxH.toFloat(), free)

        // 建筑/货架：浅灰填充 + 细黑边（贴近真实 SLAM 图：墙体是细线，黑点云才看得出对比）
        val fill = Paint().apply { color = Color.rgb(212, 212, 212); style = Paint.Style.FILL }
        val border = Paint().apply {
            color = Color.rgb(12, 12, 12); style = Paint.Style.STROKE; strokeWidth = 3f
            isAntiAlias = false
        }
        wallsCache = buildWalls(config)
        wallsCache.forEach {
            val r = worldToPixel(it, config)
            canvas.drawRect(r, fill)
            canvas.drawRect(r, border)
        }

        FileOutputStream(png).use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
        bmp.recycle()

        yaml.writeText(
            """
            image: PM.png
            resolution: ${config.res}
            origin: [${config.originX}, ${config.originY}, 0.0]
            occupied_thresh: 0.5
            free_thresh: 0.5
            negate: 0
            """.trimIndent()
        )
        Log.i(TAG, "假地图已生成: ${png.absolutePath} (${png.length()}B)")
        return png.absolutePath to yaml.absolutePath
    }

    /** 世界坐标 -> 图像像素：px=(wx-originX)/res, py=height-(wy-originY)/res */
    private fun worldToPixel(r: RectF, c: Config): RectF {
        val left = (r.left - c.originX) / c.res
        val right = (r.right - c.originX) / c.res
        val top = c.pxH - (r.top - c.originY) / c.res
        val bottom = c.pxH - (r.bottom - c.originY) / c.res
        return RectF(left, top, right, bottom)
    }

    /**
     * 世界坐标系下的墙体/建筑块：按 60m 网格铺满整张图，每块 40m 见方，留 20m 通道，
     * 模拟厂区建筑群。机器人沿通道蛇形推图，激光可打到建筑外墙。
     */
    private fun buildWalls(c: Config): List<RectF> {
        val list = ArrayList<RectF>(256)
        val cell = 60f
        val block = 50f       // 建筑 50m，通道 10m；机器人走通道中心，两侧 5m 即墙体
        val cols = (c.worldW / cell).toInt()
        val rows = (c.worldH / cell).toInt()
        for (r in 0 until rows) {
            for (col in 0 until cols) {
                val x0 = c.originX + col * cell
                val y0 = c.originY + r * cell
                list.add(RectF(x0, y0, x0 + block, y0 + block))
            }
        }
        // 外围围墙
        list.add(RectF(c.originX, c.originY, c.originX + c.worldW, c.originY + 1f))
        list.add(RectF(c.originX, c.originY + c.worldH - 1f, c.originX + c.worldW, c.originY + c.worldH))
        list.add(RectF(c.originX, c.originY, c.originX + 1f, c.originY + c.worldH))
        list.add(RectF(c.originX + c.worldW - 1f, c.originY, c.originX + c.worldW, c.originY + c.worldH))

        // 通道内散落的托盘/料架（1m 见方）。
        // 没有它们时，射线要么打在 5m 外的通道墙上、要么无命中直接落到 8m 最大量程，
        // 5~8m 之间永远没有点，累积点云会退化成一圈圈同心圆弧（看起来像"点云画好又丢了"）。
        // 加了障碍物后命中距离连续分布，点云才会像真实扫图那样铺满。
        for (r in 0 until rows) {
            val yc = c.originY + r * cell + cell - 5f   // 通道中心（与 buildRoute 一致：originY+60r+55）
            if (yc > c.originY + c.worldH - 5f) continue
            var x = c.originX + 12f
            var k = 0
            while (x < c.originX + c.worldW - 12f) {
                val oy = if ((k + r) % 2 == 0) yc - 2.4f else yc + 2.4f
                list.add(RectF(x, oy - 0.5f, x + 1f, oy + 0.5f))
                x += 14f
                k++
            }
        }
        return list
    }

    // ────────────────────────────── 推图数据流 ──────────────────────────────

    @Synchronized
    fun start(
        scope: CoroutineScope,
        view: CreateMapView3D,
        config: Config = Config(),
        onSteps: (Int) -> Unit = {}
    ) {
        if (job != null) {
            Log.w(TAG, "mock 已在运行，忽略重复启动")
            return
        }
        cfg = config
        if (wallsCache.isEmpty()) wallsCache = buildWalls(config)

        // 切到建图模式，使 keepRobotCentered() 生效，与真机推图行为一致
        view.setWorkMode(WorkMode.MODE_CREATE_MAP)
        view.isStartRevSubMaps = false

        val period = (1000L / config.fps.coerceAtLeast(1)).coerceAtLeast(20L)
        Log.i(TAG, "启动假数据流: ${config.fps}fps period=${period}ms rays=${config.rays} " +
                "map=${config.worldW.toInt()}×${config.worldH.toInt()}m walls=${wallsCache.size}")

        job = scope.launch(Dispatchers.Default) {
            // 0) 等地图元数据就绪再发数据。
            // 地图 PNG 是 Glide 异步解码的，就绪前 mapData 的 width/origin/resolution 还是旧值（甚至 0），
            // 此时下发的点云会按错误分辨率投影 → 启动瞬间出现错位的"点云线"；
            // 等地图像素到位、矩阵突变后，那批点又被打到别处，看起来就像"走十几步后突然清空重来"。
            var waitedMs = 0L
            while (isActive && !isMapDataReady(view) && waitedMs < 5000L) {
                delay(30L)
                waitedMs += 30L
            }
            // 再等一小段，让 setCentred()/尺寸变更带来的矩阵跳变落定
            delay(150L)
            Log.i(TAG, "地图就绪（等待 ${waitedMs}ms），开始推图")

            // 1) 先推一次"所有关键帧"（扩展地图页面初始化时会拉全量关键帧轨迹）
            val all = buildAllKeyFrames(config)
            mainHandler.post { view.parseKeyFramePose(all) }

            val rng = Random(20261117L)
            val route = buildRoute(config)
            var dist = 0f
            var frame = 0
            var kfId = 0
            var scaleApplied = false
            val t0 = SystemClock.uptimeMillis()
            var nextMemLog = 0L
            var lastLogFrame = 0
            var lastLogKf = 0
            var lastLogMs = SystemClock.uptimeMillis()
            // 用绝对时间轴排帧，避免 delay(period) 累积漂移，保证稳定 10 步/秒
            val periodNs = period * 1_000_000L
            var startNs = System.nanoTime()
            var pauseStartNs = 0L

            while (isActive) {
                if (paused) {
                    if (pauseStartNs == 0L) pauseStartNs = System.nanoTime()
                    delay(50)
                    continue
                }
                // 恢复时把暂停时长从时间轴上抹掉，避免"追赶"式补帧
                if (pauseStartNs != 0L) {
                    startNs += System.nanoTime() - pauseStartNs
                    pauseStartNs = 0L
                }
                val pose = poseOnRoute(route, dist)
                // 每帧前进 stepPerFrame 且每帧都产出关键帧 => 建图步数/秒 == fps（1 秒 10 步）
                val frameKfId = ++kfId

                val scan = buildScan(pose[0], pose[1], pose[2], frameKfId, rng, config)
                val step = frameKfId
                mainHandler.post {
                    view.parseLaserData(scan, 1)
                    onSteps(step)
                }

                // 回环触发间隔：原来 50 帧（≈1 秒一次）会把"全量重建点云"变成持续卡顿，
                // 真实回环由导航触发、几分钟才一次，这里按 2000 帧（≈40 秒）模拟
                if (config.withOptPose && frame > 0 && frame % 2000 == 0) {
                    val opt = buildOptPose(kfId, rng, config, route)
                    if (opt != null) mainHandler.post { view.parseOptPose(opt) }
                }

                // Glide 异步加载地图，等首帧稳定后再设置测试缩放
                // 地图解码是异步的：等 mapData 就绪后只设一次缩放。
                // 之前连设 3 次 + 每次重置平移，会看到两次明显的画面跳变（闪屏）。
                if (config.viewScale > 0f && !scaleApplied && view.mSrf.mapData.width > 0) {
                    scaleApplied = true
                    Log.i(TAG, "设置测试缩放 viewScale=${config.viewScale}")
                    view.setDebugScale(config.viewScale)
                }

                frame++
                dist += config.stepPerFrame

                if (SystemClock.uptimeMillis() >= nextMemLog) {
                    nextMemLog = SystemClock.uptimeMillis() + 5000L
                    val sec = ((SystemClock.uptimeMillis() - t0) / 1000f).coerceAtLeast(1f)
                    val nowMs = SystemClock.uptimeMillis()
                    val dFrames = frame - lastLogFrame
                    val dKf = kfId - lastLogKf
                    val dSec = ((nowMs - lastLogMs) / 1000f).coerceAtLeast(0.001f)
                    lastLogFrame = frame
                    lastLogKf = kfId
                    lastLogMs = nowMs
                    val pcStats = view.mMapOutline3D?.debugPointCloudStats() ?: "gl=null"
                    logMemory(
                        "frame=$frame kf=$kfId dist=${dist.toInt()}m elapsed=${sec.toInt()}s " +
                                "step/s=${"%.1f".format(dKf / dSec)} fps=${"%.1f".format(dFrames / dSec)} " +
                                "mSrfScale=${"%.3f".format(view.mSrf.scale)} " +
                                "map=${view.mSrf.mapData.width.toInt()}x${view.mSrf.mapData.height.toInt()}" +
                                "@${view.mSrf.mapData.resolution} | $pcStats"
                    )
                }
                // 按绝对时间轴排下一帧：落后就追赶，不落后就等到点，长期平均严格 10 帧/秒
                val nextAt = startNs + frame * periodNs
                val waitMs = (nextAt - System.nanoTime()) / 1_000_000L
                if (waitMs > 0) delay(waitMs)
            }
        }
    }

    /** 暂停：停止发帧，里程与步数保持不变（模拟原地不动/数据停发） */
    fun pause() {
        paused = true
        Log.i(TAG, "模拟已暂停")
    }

    /** 继续：从暂停位置接着发帧 */
    fun resume() {
        paused = false
        Log.i(TAG, "模拟继续")
    }

    fun isPaused(): Boolean = paused

    /** 重新开始：清掉当前地图里累积的关键帧/点云，从路线起点重新推图 */
    fun restart(
        scope: CoroutineScope,
        view: CreateMapView3D,
        config: Config = Config(),
        onSteps: (Int) -> Unit = {}
    ) {
        stop()
        paused = false
        view.resetMappingState()
        start(scope, view, config, onSteps)
    }

    @Synchronized
    fun stop() {
        job?.cancel()
        job = null
        mainHandler.removeCallbacksAndMessages(null)
        Log.i(TAG, "假数据流已停止")
    }

    fun isRunning(): Boolean = job?.isActive == true

    /**
     * 蛇形路线：沿每两条建筑行之间的通道（宽 10m，中心 y = originY + 60*r + 55）往返，
     * 换道处补竖直连接段，保证轨迹连续（不跳变）。覆盖整个纵向范围。
     */
    private fun buildRoute(c: Config): FloatArray {
        val pts = ArrayList<Float>()
        val cell = 60f
        val rows = (c.worldH / cell).toInt()
        val xMin = c.originX + 5f
        val xMax = c.originX + c.worldW - 5f
        var lastX = xMin
        var lastY = c.originY + 55f
        var started = false
        for (r in 0 until rows) {
            val y = c.originY + r * cell + 55f
            if (y > c.originY + c.worldH - 5f) break
            val startX = if (r % 2 == 0) xMin else xMax
            val endX = if (r % 2 == 0) xMax else xMin
            if (started && abs(y - lastY) > 0.01f) {
                // 换道竖直连接段
                pts.add(lastX); pts.add(lastY); pts.add(startX); pts.add(y)
            }
            pts.add(startX); pts.add(y); pts.add(endX); pts.add(y)
            lastX = endX
            lastY = y
            started = true
        }
        return pts.toFloatArray()
    }

    /** 按弧长取路径上的位姿 [x, y, theta] */
    /** 地图位图与元数据是否已就绪（Glide 异步解码完成） */
    private fun isMapDataReady(view: CreateMapView3D): Boolean {
        val md = view.mSrf.mapData
        if (md.width <= 0f || md.height <= 0f || md.resolution <= 0f) return false
        return view.mSrf.scale > 0f
    }

    private fun poseOnRoute(route: FloatArray, distance: Float): FloatArray {
        val segs = route.size / 4
        if (segs == 0) return floatArrayOf(0f, 0f, 0f)
        val lens = FloatArray(segs)
        var total = 0f
        for (i in 0 until segs) {
            lens[i] = hypot(route[i * 4 + 2] - route[i * 4], route[i * 4 + 3] - route[i * 4 + 1])
            total += lens[i]
        }
        if (total <= 0f) return floatArrayOf(0f, 0f, 0f)
        var d = distance % total
        for (i in 0 until segs) {
            if (d <= lens[i] || i == segs - 1) {
                val x0 = route[i * 4]; val y0 = route[i * 4 + 1]
                val x1 = route[i * 4 + 2]; val y1 = route[i * 4 + 3]
                val k = if (lens[i] > 0f) (d / lens[i]).coerceIn(0f, 1f) else 0f
                return floatArrayOf(
                    x0 + (x1 - x0) * k,
                    y0 + (y1 - y0) * k,
                    atan2(y1 - y0, x1 - x0)
                )
            }
            d -= lens[i]
        }
        return floatArrayOf(0f, 0f, 0f)
    }

    /** 生成一帧激光数据（6 表头 + N×6 点） */
    private fun buildScan(
        x: Float, y: Float, theta: Float, kfId: Int, rng: Random, c: Config
    ): laser_t {
        val stride = 6
        val ranges = FloatArray(6 + c.rays * stride)
        ranges[0] = x; ranges[1] = y; ranges[2] = theta
        ranges[3] = 0f; ranges[4] = 0f; ranges[5] = 0f

        val cosT = cos(theta)
        val sinT = sin(theta)
        val stepA = (2.0 * PI / c.rays).toFloat()
        var idx = 6
        for (i in 0 until c.rays) {
            val a = i * stepA
            val ldx = cos(a)
            val ldy = sin(a)
            val wdx = ldx * cosT - ldy * sinT
            val wdy = ldx * sinT + ldy * cosT
            var d = castRay(x, y, wdx, wdy)
            d += (rng.nextFloat() - 0.5f) * 0.012f
            if (d < 0.05f) d = 0.05f

            ranges[idx] = d * ldx
            ranges[idx + 1] = d * ldy
            idx += stride
        }

        val lt = laser_t()
        lt.ranges = ranges
        lt.intensities = floatArrayOf(c.pxH.toFloat(), c.pxW.toFloat(), c.originX, c.originY, c.res)
        lt.rad0 = kfId.toFloat()
        return lt
    }

    /** 射线与矩形求交（slab 法），返回最近命中距离；未命中返回最大量程 6m */
    private fun castRay(ox: Float, oy: Float, dx: Float, dy: Float): Float {
        val maxRange = 8f
        var best = maxRange
        for (r in wallsCache) {
            var tmin = 0f
            var tmax = maxRange
            if (abs(dx) < 1e-6f) {
                if (ox < r.left || ox > r.right) continue
            } else {
                var t1 = (r.left - ox) / dx
                var t2 = (r.right - ox) / dx
                if (t1 > t2) { val t = t1; t1 = t2; t2 = t }
                tmin = max(tmin, t1)
                tmax = min(tmax, t2)
            }
            if (abs(dy) < 1e-6f) {
                if (oy < r.top || oy > r.bottom) continue
            } else {
                var t1 = (r.top - oy) / dy
                var t2 = (r.bottom - oy) / dy
                if (t1 > t2) { val t = t1; t1 = t2; t2 = t }
                tmin = max(tmin, t1)
                tmax = min(tmax, t2)
            }
            if (tmax >= tmin && tmin in 0f..maxRange && tmin < best) best = tmin
        }
        return best
    }

    /** 构造全部关键帧轨迹（每 3 个一组 x,y,theta，无表头） */
    private fun buildAllKeyFrames(c: Config): laser_t {
        val route = buildRoute(c)
        val list = ArrayList<Float>()
        // 沿路线每 2m 取一个关键帧，上限 500 个：真机是几千个，
        // 但 AllKeyFrameView3D 每帧为每个关键帧 new 一个 FloatArray，
        // 数量太多会把 GC 压力刷满、盖过点云本身的表现，故测试时先控量。
        var dist = 0f
        var count = 0
        while (count < 500) {
            val p = poseOnRoute(route, dist)
            list.add(p[0]); list.add(p[1]); list.add(p[2])
            dist += 2f
            count++
        }
        val lt = laser_t()
        lt.ranges = list.toFloatArray()
        lt.intensities = FloatArray(0)
        lt.rad0 = 0f
        Log.i(TAG, "构造全部关键帧 $count 个")
        return lt
    }

    /** 构造回环优化数据：每 4 个一组 id,x,y,theta，长度刻意变化以绕过 lastRangeSize 去重 */
    private fun buildOptPose(maxId: Int, rng: Random, c: Config, route: FloatArray): laser_t? {
        if (maxId <= 0) return null
        val count = 8 + rng.nextInt(9)          // 8..16 组，size%4==0，避免越界
        val arr = FloatArray(count * 4)
        for (i in 0 until count) {
            val id = (maxId - 1 - i).coerceAtLeast(0)
            val p = poseOnRoute(route, id * c.stepPerFrame)
            arr[i * 4] = id.toFloat()
            arr[i * 4 + 1] = p[0] + (rng.nextFloat() - 0.5f) * 0.02f
            arr[i * 4 + 2] = p[1] + (rng.nextFloat() - 0.5f) * 0.02f
            arr[i * 4 + 3] = p[2]
        }
        val lt = laser_t()
        lt.ranges = arr
        lt.intensities = FloatArray(0)
        lt.rad0 = 0f
        return lt
    }

    /** 每 5s 打一次内存账单，用于观察是否存在只增不降 */
    private fun logMemory(label: String) {
        val rt = Runtime.getRuntime()
        val javaUsed = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024
        val javaMax = rt.maxMemory() / 1024 / 1024
        val mi = Debug.MemoryInfo()
        Debug.getMemoryInfo(mi)
        val native = mi.getMemoryStat("summary.native-heap")?.toIntOrNull()?.div(1024) ?: -1
        val graphics = mi.getMemoryStat("summary.graphics")?.toIntOrNull()?.div(1024) ?: -1
        Log.i(
            MEM_TAG,
            "$label | java=${javaUsed}/${javaMax}MB totalPss=${mi.totalPss / 1024}MB " +
                    "native=$native MB graphics=$graphics MB"
        )
    }
}
