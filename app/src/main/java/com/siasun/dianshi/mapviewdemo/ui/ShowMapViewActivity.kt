package com.siasun.dianshi.mapviewdemo.ui

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlin.random.Random
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.jeremyliao.liveeventbus.LiveEventBus
import com.ngu.lcmtypes.laser_t
import com.ngu.lcmtypes.robot_control_t
import com.siasun.dianshi.AreaType
import com.siasun.dianshi.ConstantBase
import com.siasun.dianshi.ConstantBase.PAD_WORLD_NAME
import com.siasun.dianshi.ConstantBase.getFolderPath
import com.siasun.dianshi.base.BaseMvvmActivity
import com.siasun.dianshi.bean.CleanAreaNew
import com.siasun.dianshi.bean.CmsStation
import com.siasun.dianshi.bean.PlanPathResult
import com.siasun.dianshi.bean.PositingArea
import com.siasun.dianshi.controller.MainController
import com.siasun.dianshi.framework.ext.onClick
import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.mapviewdemo.KEY_AGV_COORDINATE
import com.siasun.dianshi.mapviewdemo.KEY_BOTTOM_CURRENT_POINT_CLOUD
import com.siasun.dianshi.mapviewdemo.KEY_CURRENT_POINT_CLOUD
import com.siasun.dianshi.mapviewdemo.RunningState
import com.siasun.dianshi.mapviewdemo.TaskState
import com.siasun.dianshi.mapviewdemo.databinding.ActivityShowMapViewBinding
import com.siasun.dianshi.mapviewdemo.viewmodel.ShowMapViewModel
import com.siasun.dianshi.utils.YamlNew
import com.siasun.dianshi.view.MapView
import com.siasun.dianshi.bean.SpArea
import com.siasun.dianshi.bean.WorkAreasNew
import com.siasun.dianshi.utils.World
import com.siasun.dianshi.framework.ext.toJson
import com.siasun.dianshi.mapviewdemo.CLEAN_PATH_PLAN
import com.siasun.dianshi.mapviewdemo.GLOBAL_PATH_PLAN
import com.siasun.dianshi.mapviewdemo.KEY_POSITING_AREA_VALUE
import com.siasun.dianshi.mapviewdemo.KEY_UPDATE_PLAN_PATH_RESULT
import com.siasun.dianshi.mapviewdemo.PATH_MODE
import com.siasun.dianshi.mapviewdemo.TAG_PP
import com.siasun.dianshi.mapviewdemo.utils.GsonUtil
import com.siasun.dianshi.mapviewdemo.utils.PathPlanningUtil
import com.siasun.dianshi.view.HomeDockView
import com.siasun.dianshi.view.MixAreaView
import com.siasun.dianshi.view.PolygonEditView
import com.siasun.dianshi.view.PostingAreasView
import com.siasun.dianshi.view.SpPolygonEditView
import com.siasun.dianshi.view.VirtualWallView
import com.siasun.dianshi.xpop.XpopUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 显示地图
 */
class ShowMapViewActivity : BaseMvvmActivity<ActivityShowMapViewBinding, ShowMapViewModel>() {

    val mapId = 1
    var cleanAreas: MutableList<CleanAreaNew> = mutableListOf()
    var mSpArea: MutableList<SpArea> = mutableListOf()
    var mMixArea: MutableList<WorkAreasNew> = mutableListOf()
    var cmsStation: MutableList<CmsStation> = mutableListOf()

    // 创建World对象并读取文件
    val world = World()

    @RequiresApi(Build.VERSION_CODES.R)
    override fun initView(savedInstanceState: Bundle?) {
        MainController.init()
//        val file = File(ConstantBase.getFilePath(mapId, ConstantBase.PAD_MAP_NAME_PNG))
//        Glide.with(this).asBitmap().load(file).skipMemoryCache(true)
//            .diskCacheStrategy(DiskCacheStrategy.NONE).into(object : SimpleTarget<Bitmap?>() {
//                override fun onResourceReady(
//                    resource: Bitmap, transition: Transition<in Bitmap?>?
//                ) {
//                    val mPngMapData = YamlNew().loadYaml(
//                        ConstantBase.getFilePath(mapId, ConstantBase.PAD_MAP_NAME_YAML),
//                        resource.height.toFloat(),
//                        resource.width.toFloat(),
//                    )
//                    mBinding.mapView.setBitmap(mPngMapData, resource)
//
//                }
//            })

        //加载地图
        mBinding.mapView.loadMap(
            ConstantBase.getFilePath(mapId, ConstantBase.PAD_MAP_NAME_PNG),
            ConstantBase.getFilePath(mapId, ConstantBase.PAD_MAP_NAME_YAML)
        )
        //移动模式
        mBinding.btnMove.setOnClickListener {
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_SHOW_MAP)
        }

        initMergedPose()
        initStation()
        iniVirtualWall()
        initRemoveNoise()
        initPostingArea()
        initCleanArea()
        initElevator()
        initPose()
        initMachineStation()
        initMixArea()
        initSpAreas()
        initPath()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun initPath() {
        // 地图加载完成后，加载路径数据
        readPathsFromFile()
        // 设置路径属性编辑回调监听器
        mBinding.mapView.setOnPathAttributeEditListener(object :
            com.siasun.dianshi.view.WorldPadView.OnPathAttributeEditListener {
            override fun onNodeSelected(
                node: com.siasun.dianshi.bean.pp.world.Node,
                path: com.siasun.dianshi.bean.pp.world.Path
            ) {
                // 处理选中节点事件
                Log.d(
                    "ShowMapViewActivity",
                    "选中节点: id=${node.m_uId}, x=${node.x}, y=${node.y}, 所属路段: ${path.GetStartNode()?.m_uId}->${path.GetEndNode()?.m_uId}"
                )
                // 这里可以显示节点属性编辑界面，或者执行其他操作
            }

            override fun onPathSelected(path: com.siasun.dianshi.bean.pp.world.Path) {
                // 处理选中路段事件
                val startNode = path.GetStartNode()
                val endNode = path.GetEndNode()
                Log.d("ShowMapViewActivity", "选中路段: ${startNode?.m_uId}->${endNode?.m_uId}")
                // 这里可以显示路段属性编辑界面，或者执行其他操作
            }

            override fun onPathDeleted(path: com.siasun.dianshi.bean.pp.world.Path) {
                // 处理删除路段事件
                // 直接使用Path对象中的节点ID属性，避免通过可能返回null的节点对象获取
                val startNodeId = path.m_uStartNode
                val endNodeId = path.m_uEndNode
                Log.d("ShowMapViewActivity", "删除路段: $startNodeId->$endNodeId")
                // 可以在这里添加删除路段后的业务逻辑
                onPathDataChanged()
            }

            override fun onNodeDeleted(node: com.siasun.dianshi.bean.pp.world.Node) {
                // 处理删除节点事件
                Log.d("ShowMapViewActivity", "删除节点: id=${node.m_uId}, x=${node.x}, y=${node.y}")
                // 可以在这里添加删除节点后的业务逻辑
                onPathDataChanged()
            }
        })

        // 编辑路线
        mBinding.btnEditPath.onClick {
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_PATH_EDIT)
        }
        //路段属性编辑
        mBinding.btnEditPathArr.onClick {
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_PATH_SEGMENT_ATTR_EDIT)

        }

        //节点属性编辑
        mBinding.btnEditPointArr.onClick {
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_PATH_NODE_ATTR_EDIT)
        }

        // 合并路线
        mBinding.btnMergePath.onClick {
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_PATH_MERGE)
        }

        // 删除路线
        mBinding.btnDeletePath.onClick {
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_PATH_DELETE)
        }

        // 删除多条路线
        mBinding.btnDeleteMultiplePaths.onClick {
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_PATH_DELETE_MULTIPLE)
        }

        // 曲线转直线
        mBinding.btnConvertPathToLine.onClick {
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_PATH_CONVERT_TO_LINE)
        }

        // 创建路线
        mBinding.btnCreatePath.onClick {
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_PATH_CREATE)
        }

        // 保存路线
        mBinding.btnSavePath.onClick {
            savePathsToFile()
        }
    }

    /**
     * 加载world_pad.dat文件中的路径数据
     */
    private fun readPathsFromFile() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val filePath = ConstantBase.getFilePath(mapId, PAD_WORLD_NAME)
                    val file = File(filePath)
                    // 检查文件是否存在
                    if (file.exists()) {
                        //读取world_pad.dat
                        val readWorld = world.readWorld(getFolderPath(mapId), PAD_WORLD_NAME)
                        if (readWorld) mBinding.mapView.setLayer(world.cLayer)

                    } else {
                        LogUtil.d("路径数据文件不存在，将创建新的World对象")
                        world.saveWorld(getFolderPath(mapId), PAD_WORLD_NAME)
                    }
                } catch (e: Exception) {
                    LogUtil.e("加载路径数据异常: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 保存路线到world_pad.dat文件
     */
    private fun savePathsToFile() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {

                    val filePath = ConstantBase.getFilePath(mapId, PAD_WORLD_NAME)
                    val file = File(filePath)

                    // 创建保存目录（如果不存在）
                    val directory = file.parentFile
                    if (directory != null && !directory.exists()) {
                        directory.mkdirs()
                    }

                    // 调用World类的写入方法保存文件
                    if (world.saveWorld(getFolderPath(mapId), PAD_WORLD_NAME)) {
                        LogUtil.d("成功保存路径数据到文件: $filePath")
                    } else {
                        LogUtil.e("保存路径数据失败")
                    }

                } catch (e: Exception) {
                    LogUtil.e("保存路径数据异常: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 路径数据更改回调
     */
    fun onPathDataChanged() {
        LogUtil.d("路径数据已更改")
        // 可以在这里添加数据更改的通知逻辑
    }


    /**
     * 特殊区域
     */
    private fun initSpAreas() {
        mViewModel.getSpecialArea(mapId, 5) { list ->
            mSpArea.addAll(list)
            mBinding.mapView.setSpAreaData(mSpArea)
        }
        //添加特殊区域
        mBinding.btnAddSpArea.onClick {
            // 设置地图的工作模式为添加清扫区域模式
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_SP_AREA_ADD)
            // 创建一个新的清扫区域
            val newArea = SpArea().apply {
                sub_name = "特殊区域${mSpArea.size + 1}"
                regId = mSpArea.size + 1
                layer_id = mapId
                routeType = 5
            }
            mSpArea.add(newArea)
            mBinding.mapView.createSpArea(newArea)
        }
        //编辑特殊区域
        mBinding.btnEditSpArea.onClick {
            if (mSpArea.isNotEmpty()) {
                // 生成0到positingAreas.size-1之间的随机索引
                val randomIndex = Random.nextInt(mSpArea.size)
                // 通过随机索引获取要删除的定位区域
                val randomArea = mSpArea[randomIndex]
                //切换特殊区域编辑模式
                mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_SP_AREA_EDIT)
                mBinding.mapView.setSelectedSpArea(randomArea)
            }
        }

        // 设置特殊区域编辑监听器
        mBinding.mapView.setOnSpAreaEditListener(object : SpPolygonEditView.OnSpAreaEditListener {

            override fun onVertexDragEnd(area: SpArea, vertexIndex: Int) {
                LogUtil.i("编辑特殊区域onVertexDragEnd    ${area.toJson()}")
            }

            override fun onVertexAdded(area: SpArea, vertexIndex: Int, x: Float, y: Float) {
                LogUtil.i("编辑特殊区域onVertexAdded    ${area.toJson()}")
            }

            override fun onEdgeRemoved(area: SpArea, edgeIndex: Int) {
                LogUtil.i("编辑特殊区域onEdgeRemoved    ${area.toJson()}")
            }

            override fun onAreaCreated(area: SpArea) {
                LogUtil.i("创建了特殊新的清扫区域    ${area.toJson()}")
            }
        })

        //删除特殊区域
        mBinding.btnDeleteSpArea.onClick {
            // 随机选择一个定位区域进行删除
            if (mSpArea.isNotEmpty()) {
                // 生成0到positingAreas.size-1之间的随机索引
                val randomIndex = Random.nextInt(mSpArea.size)

                // 通过随机索引获取要删除的定位区域
                val randomArea = mSpArea[randomIndex]

                // 更新本地列表
                mSpArea.remove(randomArea)
                mBinding.mapView.setSpAreaData(mSpArea)
            }
        }
        //保存特殊区域
        mBinding.btnSaveSpArea.onClick {
            mViewModel.saveSpecialArea(mapId, mBinding.mapView.getSpAreaData())
        }

    }

    private fun initMixArea() {
        //获取混行区域
        mViewModel.getMixAreaData(mapId) { workAreasNew ->
            workAreasNew?.let {
                mMixArea.addAll(workAreasNew.workAreasList)
                mBinding.mapView.setMixAreaData(mMixArea)
            }
        }
        //添加混行区域
        mBinding.btnAddMixArea.onClick {
            // 设置地图的工作模式为添加清扫区域模式
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_MIX_AREA_ADD)
            // 创建一个新的清扫区域
            val newArea = WorkAreasNew().apply {
                name = "混行区域${mMixArea.size + 1}"
                id = "${mMixArea.size + 1}"//随机申城
            }
            mMixArea.add(newArea)
            mBinding.mapView.createMixArea(newArea)
        }
        //编辑混行区
        mBinding.btnEditMixArea.onClick {
            if (mBinding.mapView.getMixAreaData().toMutableList().isNotEmpty()) {
                // 生成0到positingAreas.size-1之间的随机索引
                val randomIndex =
                    Random.nextInt(mBinding.mapView.getMixAreaData().toMutableList().size)

                // 通过随机索引获取要删除的定位区域
                val randomArea = mBinding.mapView.getMixAreaData().toMutableList()[randomIndex]

                // 设置地图的工作模式为编辑清扫区域模式
                mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_MIX_AREA_EDIT)

                // 将选中的区域设置到PolygonEditView中进行编辑
                mBinding.mapView.setSelectedMixArea(randomArea)
            }
        }

        //设置混行区域编辑监听器
        mBinding.mapView.setOnMixAreaEditListener(object : MixAreaView.OnMixAreaEditListener {

            override fun onVertexDragEnd(area: WorkAreasNew, vertexIndex: Int) {
                LogUtil.i("编辑区域onVertexDragEnd   ${area.toJson()}")
            }

            override fun onVertexAdded(
                area: WorkAreasNew, vertexIndex: Int, x: Float, y: Float
            ) {
                LogUtil.i("编辑混行区域onVertexAdded   ${area.toJson()}")
            }

            override fun onEdgeRemoved(area: WorkAreasNew, edgeIndex: Int) {
                LogUtil.i("编辑混行区域onEdgeRemoved  ${area.toJson()}")
            }

            override fun onAreaCreated(area: WorkAreasNew) {
                LogUtil.i("创建了新的混行区域   ${area.toJson()}")
            }
        })
        //删除混行区域
        mBinding.btnDeleteMixArea.onClick {
            if (mMixArea.isNotEmpty()) {
                // 随机生成一个索引
                val randomIndex = Random.nextInt(mMixArea.size)
                // 删除随机选中的清扫区域
                mMixArea.removeAt(randomIndex)
                // 更新清扫区域数据
                mBinding.mapView.setMixAreaData(mMixArea)
                LogUtil.d("随机删除了一个清扫区域，当前剩余 ${cleanAreas.size} 个清扫区域")
            } else {
                LogUtil.d("没有清扫区域可以删除")
            }
        }
        //保存混行区
        mBinding.btnSaveMixArea.onClick {
            LogUtil.d("保存混行区 ${mBinding.mapView.getMixAreaData().toJson()}")

            mViewModel.saveAreaLiveDate
        }
    }

    /**
     * 充电站
     */
    private fun initMachineStation() {
        //加载充电站
        mViewModel.getMachineStation(onComplete = { machineStation ->
            LogUtil.d("获取充电站信息 $machineStation")
            val result = machineStation?.find { it.mapId == mapId }
            mBinding.mapView.setMachineStation(result)
        })
        //编辑充电站
        mBinding.btnEditChargeStation.onClick {
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_MACHINE_STATION_EDIT)
        }

        //设置充电站点击监听器
        mBinding.mapView.setOnMachineStationClickListener(object :
            HomeDockView.OnMachineStationClickListener {
            override fun onMachineStationClick(
                station: com.siasun.dianshi.bean.MachineStation, type: Int
            ) {
                //处理充电站点击事件
                when (type) {
                    0 -> LogUtil.d("点击了对接点: $station")
                    1 -> LogUtil.d("点击了准备点: $station")
                    2 -> LogUtil.d("点击了等待点: $station")
                    3 -> LogUtil.d("点击了结束停放点: $station")
                }
                LogUtil.d("编辑充电站: $station, 类型: $type")
            }
        })

        //设置充电站删除监听器
        mBinding.mapView.setOnMachineStationDeleteListener(object :
            HomeDockView.OnMachineStationDeleteListener {
            override fun onMachineStationDelete(
                station: com.siasun.dianshi.bean.MachineStation, type: Int
            ) {
                //处理充电站删除事件
                when (type) {
                    0 -> LogUtil.d("删除了对接点: $station")
                    1 -> LogUtil.d("删除了准备点: $station")
                    2 -> LogUtil.d("删除了等待点: $station")
                    3 -> LogUtil.d("删除了结束停放点: $station")
                }
                LogUtil.d("删除充电站: $station, 类型: $type")

                // 这里可以添加实际的删除逻辑，比如网络请求删除该充电站点
            }
        })
        //删除充电站
        mBinding.btnDeleteChargeStation.onClick {
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_MACHINE_STATION_DELETE)
        }
        //保存充电站
        mBinding.btnSaveChargeStation.onClick {
//            mBinding.mapView.getMachineStation()
        }
    }

    private fun initPose() {
        //加载上线点
        mViewModel.getInitPose(mapId, onComplete = { initPoses ->
            initPoses?.let {
                mBinding.mapView.setInitPoseList(it.Initposes)
            }
        })
    }

    /**
     * 加载顶视路线
     */
    private fun initMergedPose() {
        mViewModel.getMergedPose(mapId, onComplete = { mergedPoses ->
            mergedPoses?.data?.let {
                mBinding.mapView.setTopViewPathDada(it)
            }
        })
    }

    /**
     * 乘梯点
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun initElevator() {
        //加载乘梯点
        mViewModel.getCmsElevator(mapId, onComplete = { elevatorPoint ->
            LogUtil.d("获取乘梯点 $elevatorPoint")
            mBinding.mapView.setElevators(elevatorPoint)
        })

        //添加
        mBinding.btnAddElevator.onClick {
            //弹框增加乘梯点
        }
        //编辑乘梯点
        mBinding.btnEditElevator.onClick {
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_ELEVATOR_EDIT)
        }

        //删除乘梯点
        mBinding.btnDeleteElevator.onClick {
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_ELEVATOR_DELETE)
        }

        //设置乘梯点编辑监听器
        mBinding.mapView.setOnElevatorEditListener(object :
            com.siasun.dianshi.view.ElevatorView.OnElevatorEditListener {
            override fun onElevatorEdit(elevator: com.siasun.dianshi.bean.ElevatorPoint) {
                //处理乘梯点编辑事件
                LogUtil.d("编辑乘梯点: $elevator")
            }
        })

        //设置乘梯点删除监听器
        mBinding.mapView.setOnElevatorDeleteListener(object :
            com.siasun.dianshi.view.ElevatorView.OnElevatorDeleteListener {
            override fun onElevatorDelete(elevator: com.siasun.dianshi.bean.ElevatorPoint) {
                //处理乘梯点删除事件
                LogUtil.d("删除乘梯点: $elevator")
            }
        })

        //保存乘梯点
        mBinding.btnDeleteElevator.onClick {
            //调接口保存乘梯点数据
        }
    }

    /**
     * 清扫区域
     */
    private fun initCleanArea() {
        //获取区域
        mViewModel.getAreaList(mapId, onComplete = { cleanAreasRoot ->
            cleanAreasRoot?.let {
                cleanAreas.addAll(it.cleanAreas)
                mBinding.mapView.setCleanAreaData(cleanAreas)
            }
        })
        //编辑清扫区域
        mBinding.btnEditArea.onClick {
            if (mBinding.mapView.getCleanAreaData().toMutableList().isNotEmpty()) {
                // 生成0到positingAreas.size-1之间的随机索引
                val randomIndex =
                    Random.nextInt(mBinding.mapView.getCleanAreaData().toMutableList().size)

                // 通过随机索引获取要删除的定位区域
                val randomArea = mBinding.mapView.getCleanAreaData().toMutableList()[randomIndex]

                // 设置地图的工作模式为编辑清扫区域模式
                mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_CLEAN_AREA_EDIT)

                // 将选中的区域设置到PolygonEditView中进行编辑
                mBinding.mapView.setSelectedArea(randomArea)
            }
        }

        // 设置清扫区域编辑监听器
        mBinding.mapView.setOnCleanAreaEditListener(object :
            PolygonEditView.OnCleanAreaEditListener {

            override fun onVertexDragEnd(area: CleanAreaNew, vertexIndex: Int) {
                LogUtil.d("onVertexDragEnd area $area")
                if (area.routeType == AreaType.AREA_AUTO) {
                    MainController.sendRoutePathCommand(CLEAN_PATH_PLAN, area)
                    LogUtil.i(
                        "编辑区域onVertexDragEnd  申请路径规划 ${area.toJson()}", null, TAG_PP
                    )
                }
            }

            override fun onVertexAdded(
                area: CleanAreaNew, vertexIndex: Int, x: Float, y: Float
            ) {
                if (area.routeType == AreaType.AREA_AUTO) {
                    MainController.sendRoutePathCommand(CLEAN_PATH_PLAN, area)
                    LogUtil.i("编辑区域onVertexAdded  申请路径规划 ${area.toJson()}", null, TAG_PP)
                }
            }

            override fun onEdgeRemoved(area: CleanAreaNew, edgeIndex: Int) {
                if (area.routeType == AreaType.AREA_AUTO) {
                    MainController.sendRoutePathCommand(CLEAN_PATH_PLAN, area)
                    LogUtil.i("编辑区域onEdgeRemoved  申请路径规划 ${area.toJson()}", null, TAG_PP)
                }
            }

            override fun onAreaCreated(area: CleanAreaNew) {
                // 将新创建的清扫区域添加到本地列表
                LogUtil.d("创建了新的清扫区域: ${area.sub_name}, ID: ${area.regId}")
                if (area.routeType == AreaType.AREA_AUTO) {
                    MainController.sendRoutePathCommand(CLEAN_PATH_PLAN, area)
                    LogUtil.i("创建了新的清扫区域  申请路径规划 ${area.toJson()}", null, TAG_PP)
                }
            }
        })

        //添加清扫区域
        mBinding.btnAddArea.onClick {
            // 设置地图的工作模式为添加清扫区域模式
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_CLEAN_AREA_ADD)
            // 创建一个新的清扫区域
            val newArea = CleanAreaNew().apply {
                sub_name = "清扫区域${cleanAreas.size + 1}"
                regId = cleanAreas.size + 1//随机申城
                layer_id = mapId
                routeType = 0 // 自动生成
                areaType = 1
                cleanShape = 3 // 回字型
                areaPathType = 0 // 普通清扫区域
            }
            cleanAreas.add(newArea)
            mBinding.mapView.createCleanArea(newArea)
        }

        //删除清扫区域
        mBinding.btnDeleteArea.onClick {
            if (cleanAreas.isNotEmpty()) {
                // 随机生成一个索引
                val randomIndex = Random.nextInt(cleanAreas.size)
                // 删除随机选中的清扫区域
                cleanAreas.removeAt(randomIndex)
                // 更新清扫区域数据
                mBinding.mapView.setCleanAreaData(cleanAreas)
                LogUtil.d("随机删除了一个清扫区域，当前剩余 ${cleanAreas.size} 个清扫区域")
            } else {
                LogUtil.d("没有清扫区域可以删除")
            }
        }
        //保存清扫区域
        mBinding.btnSaveArea.onClick {
            mViewModel.saveArea(mapId, cleanAreas)
        }
    }

    /**
     * 定位区域
     */
    private fun initPostingArea() {
        //创建定位区域
        mBinding.btnPostingAreaAdd.onClick {
            // 设置创建定位区域模式
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_POSITING_AREA_ADD)
        }

        // 设置定位区域创建监听器
        mBinding.mapView.setOnPositingAreaCreatedListener(object :
            PostingAreasView.OnPositingAreaCreatedListener {
            override fun onPositingAreaCreated(area: PositingArea) {
                // 切换回移动模式
                mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_SHOW_MAP)
            }
        })

        //编辑定位区域
        mBinding.btnPostingAreaEdit.setOnClickListener {
            // 随机选择一个定位区域高亮显示
            if (mBinding.mapView.getPositingAreas().toMutableList().isNotEmpty()) {
                mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_POSITING_AREA_EDIT)

                // 生成0到positingAreas.size-1之间的随机索引
                val randomIndex =
                    Random.nextInt(mBinding.mapView.getPositingAreas().toMutableList().size)

                // 通过随机索引获取定位区域对象
                val randomArea = mBinding.mapView.getPositingAreas().toMutableList()[randomIndex]

                // 方式1：通过对象设置选中区域
                mBinding.mapView.setSelectedPositingArea(randomArea)

            }
        }

        //删除定位区域
        mBinding.btnPostingAreaDel.onClick {
            // 随机选择一个定位区域进行删除
            if (mBinding.mapView.getPositingAreas().toMutableList().isNotEmpty()) {
                // 生成0到positingAreas.size-1之间的随机索引
                val randomIndex =
                    Random.nextInt(mBinding.mapView.getPositingAreas().toMutableList().size)

                // 通过随机索引获取要删除的定位区域
                val randomArea = mBinding.mapView.getPositingAreas().toMutableList()[randomIndex]

                // 删除该定位区域
                mBinding.mapView.deletePositingArea(randomArea)

                // 更新本地列表
                mBinding.mapView.getPositingAreas().toMutableList().remove(randomArea)

                LogUtil.d("随机删除了定位区域: ${randomArea.id}")
            }
        }

        //保存定位区域
        mBinding.btnPostingAreaCommit.setOnClickListener {
            MainController.sendPositingArea(
                mapId, mBinding.mapView.getPositingAreas().toMutableList()
            )
        }
    }

    /**
     * 删除噪点
     */
    private fun initRemoveNoise() {
        //删除噪点
        mBinding.btnRemoveNoise.setOnClickListener {
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_REMOVE_NOISE)
        }
        // 设置去除噪点监听器
        mBinding.mapView.setOnRemoveNoiseListener(object : MapView.IRemoveNoiseListener {
            override fun onRemoveNoise(leftTop: PointF, rightBottom: PointF) {
                // 处理噪点区域信息，这里可以添加日志或者发送到控制器
                LogUtil.d("去除噪点区域: 左上角(${leftTop.x}, ${leftTop.y}), 右下角(${rightBottom.x}, ${rightBottom.y})")
            }
        })
    }

    /**
     * 避让点
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun initStation() {
        //加载避让点
        mViewModel.getStationData(mapId, onComplete = { cmsStations ->
            if (cmsStations != null) {
                cmsStation.addAll(cmsStations)
                mBinding.mapView.setCmsStations(cmsStation)
            }
        })
        //添加避让点
        mBinding.btnCreateStation.onClick {
            XpopUtils(this).showCmsStationDialog(onConfirmCall = { result ->
                result?.let {
                    cmsStation.add(result)
                    mBinding.mapView.setCmsStations(cmsStation)
                }

            }, onDeleteCall = {

            }, mapId
            )
        }
        //编辑避让点
        mBinding.btnEditStation.onClick {
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_CMS_STATION_EDIT)
        }
        // 设置避让点点击监听器
        mBinding.mapView.setOnStationClickListener(object :
            com.siasun.dianshi.view.StationsView.OnStationClickListener {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onStationClick(station: CmsStation) {
                // 处理避让点点击事件
                LogUtil.d("点击了避让点: $station")
            }
        })
        //删除避让点
        mBinding.btnDeleteStation.onClick {
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_CMS_STATION_DELETE)
        }

        // 设置避让点删除监听器
        mBinding.mapView.setOnStationDeleteListener(object :
            com.siasun.dianshi.view.StationsView.OnStationDeleteListener {
            override fun onStationDelete(station: CmsStation) {
                // 处理避让点删除事件
                LogUtil.d("删除了避让点: $station")
                // 这里可以添加删除避让点的业务逻辑，比如调用API删除服务器上的避让点
            }
        })
    }

    /**
     * 虚拟墙
     */
    private fun iniVirtualWall() {
        //加载虚拟墙
        mViewModel.getVirtualWall(mapId, onComplete = { virtualWall ->
            virtualWall?.let {
                mBinding.mapView.setVirtualWall(it)
            }
        })
        //添加虚拟墙
        mBinding.btnVirAdd.setOnClickListener {
            // 创建虚拟墙模式
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_VIRTUAL_WALL_ADD)
            // 默认创建普通虚拟墙
            mBinding.mapView.addVirtualWall(3)
        }
        //编辑虚拟墙
        mBinding.btnVirEdit.setOnClickListener {
            // 编辑虚拟墙模式
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_VIRTUAL_WALL_EDIT)

        }
        //删除虚拟墙
        mBinding.btnVirDel.setOnClickListener {
            // 删除虚拟墙模式
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_VIRTUAL_WALL_DELETE)
        }
        //编辑虚拟墙类型
        mBinding.btnVirTypeEdit.setOnClickListener {
            // 编辑虚拟墙类型模式
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_VIRTUAL_WALL_TYPE_EDIT)
        }
        // 设置虚拟墙点击监听器
        mBinding.mapView.setOnVirtualWallClickListener(object :
            VirtualWallView.OnVirtualWallClickListener {
            override fun onVirtualWallClick(lineIndex: Int, config: Int) {
                // 处理虚拟墙点击事件
                // 这里可以显示一个对话框，让用户选择新的虚拟墙类型
                Log.d(
                    "ShowMapViewActivity", "Virtual wall clicked: index=$lineIndex, config=$config"
                )

                // 示例：将虚拟墙类型切换为下一种类型 (1:重点虚拟墙, 2:虚拟门, 3:普通虚拟墙)
                val newConfig = when (config) {
                    1 -> 2
                    2 -> 3
                    else -> 1
                }

                // 更新虚拟墙类型
                mBinding.mapView.updateVirtualWallType(lineIndex, newConfig)
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onDestroy() {
        super.onDestroy()
        // 释放资源
//        mBinding.mapView.descendantFocusability()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun initData() {
        super.initData()
        MainController.sendGetPositingAreas(mapId)

        //上激光点云
        LiveEventBus.get<laser_t>(KEY_CURRENT_POINT_CLOUD).observe(this) {
            mBinding.mapView.setUpLaserScan(it)
        }

        //下激光点云
        LiveEventBus.get<laser_t>(KEY_BOTTOM_CURRENT_POINT_CLOUD).observe(this) {
            mBinding.mapView.setDownLaserScan(it)
        }

        //接收车体坐标 AGV->PAD
        LiveEventBus.get<robot_control_t>(KEY_AGV_COORDINATE).observe(this) {
            mBinding.mapView.setAgvPose(it)

            //有任务才显示车体位置
            if (RunningState.CURRENT_TASK_STATE == TaskState.HAVE_TASK) {
                mBinding.mapView.setWorkingPath(it.dparams)
            }
        }

        //接收导航定位区域
        LiveEventBus.get<String>(KEY_POSITING_AREA_VALUE).observe(this) {
            val positingAreas = GsonUtil.jsonToList(it, PositingArea::class.java)
            mBinding.mapView.setPositingAreas(positingAreas)
        }

        //接收路径规划结果
        LiveEventBus.get<PlanPathResult>(KEY_UPDATE_PLAN_PATH_RESULT).observe(this) { result ->
            try {
                // 检查路径段数量是否为0
                if (result.m_iPathSum == 0) {
                    LogUtil.i(
                        "展示路径规划失败弹窗, 路径类型:${result.m_iPathPlanType}, 路段个数:${result.m_iPathSum}",
                        null,
                        TAG_PP
                    )

                    return@observe
                }

                // 检查路径点数量是否过大，防止内存溢出
                if (result.m_fElementBuffer.size > PathPlanningUtil.MAX_POINT_COUNT) {
                    LogUtil.e(
                        "路径点数量过大，可能导致内存溢出: ${result.m_fElementBuffer.size}",
                        null,
                        TAG_PP
                    )
                    return@observe
                }

                // 检查当前内存状态，防止内存溢出
                val memoryInfo = ActivityManager.MemoryInfo()
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.getMemoryInfo(memoryInfo)
                if (memoryInfo.lowMemory) {
                    // 内存不足时，清理旧路径和对象池
                    LogUtil.w("系统内存不足，清理旧路径和对象池", null, TAG_PP)
                    mBinding.mapView.clearPathPlan() // 假设MapView有清理路径的方法
                    PathPlanningUtil.clearObjectPools() // 清理对象池
                }

                if (result.m_iPathPlanType == CLEAN_PATH_PLAN) {
                    // 接收Pad申请的清扫路径规划结果
                    if (result.m_strTo == "pad") {
                        // 路段模式
                        if (result.m_iPlanResultMode == PATH_MODE) {
                            LogUtil.i(
                                "AutoGeneratePathActivity接收清扫路径规划", null, TAG_PP
                            )

                            // 清除旧的清扫路径
                            mBinding.mapView.clearCleanPathPlan()

                            val cleanPathPlanResultBean =
                                PathPlanningUtil.getPathPlanResultBean(result, mBinding.mapView)
                            if (cleanPathPlanResultBean.m_bIsPlanOk) {
                                mBinding.mapView.setCleanPathPlanResultBean(cleanPathPlanResultBean)
                            } else {
                                LogUtil.e("清扫路径规划解析失败", null, TAG_PP)
                            }
                        }
                    }
                }
                // 接收CMS申请的全局路径规划结果
                if (result.m_iPathPlanType == GLOBAL_PATH_PLAN) {
                    if (result.m_strTo == "CMS") {
                        // 路段模式
                        if (result.m_iPlanResultMode == PATH_MODE) {
                            LogUtil.i(
                                "CleanAutoActivity接收全局路径规划", null, TAG_PP
                            )

                            // 清除旧的全局路径
                            mBinding.mapView.clearGlobalPathPlan()

                            val globalPathPlanResultBean =
                                PathPlanningUtil.getPathPlanResultBean(result)
                            if (globalPathPlanResultBean.m_bIsPlanOk) {
                                mBinding.mapView.setGlobalPathPlanResultBean(
                                    globalPathPlanResultBean
                                )
                            } else {
                                LogUtil.e("全局路径规划解析失败", null, TAG_PP)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                LogUtil.e(
                    "接收路径规划异常: ${e.message}", null, TAG_PP
                )
                e.printStackTrace()
                // 异常处理：清理可能已创建的资源
//                mBinding.mapView.clearPathPlan() // 清理所有路径
                PathPlanningUtil.clearObjectPools() // 清理对象池
            }
        }
    }
}