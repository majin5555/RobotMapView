# 作者 MJ

# RobotMapView 机器人分层地图加载

-----------------------------------------

# ****************地图的所有工作模式****************

# enum class WorkMode {

# MODE_SHOW_MAP, // 移动地图模式

# MODE_VIRTUAL_WALL_ADD, // 创建虚拟墙模式

# MODE_VIRTUAL_WALL_EDIT,// 编辑虚拟墙模式

# MODE_VIRTUAL_WALL_DELETE, // 删除虚拟墙模式

# MODE_CMS_STATION_EDIT // 修改避让点模式

# MODE_REMOVE_NOISE //删除噪点模式

# }

# 设置地图的当前的模式

# mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_SHOW_MAP)

-----------------------------------------

# 布局引用

# <com.siasun.dianshi.view.MapView

# android:id="@+id/map_view"

# android:layout_width="match_parent"

# android:layout_height="match_parent" />
-----------------------------------------

# 加载地图png图片

# val mPngMapData = YamlNew().loadYaml(

# ConstantBase.getFilePath(mapId, ConstantBase.PAD_MAP_NAME_YAML),

# resource.height.toFloat(),

# resource.width.toFloat(),)

# mBinding.mapView.setBitmap(mPngMapData, resource)

-----------------------------------------

# 加载上激光点云

# mBinding.mapView.setUpLaserScan(it)

-----------------------------------------

# 加载下激光点云

# mBinding.mapView.setDownLaserScan(it)

-----------------------------------------

# 加载虚拟墙数据

# mBinding.mapView.setVirtualWall(it)
-----------------------------------------

# 获取虚拟墙数据

# mBinding.mapView.getVirtualWall()
-----------------------------------------

# 添加虚拟墙

# 创建虚拟墙模式      1 红色实线 重点虚拟墙  2 红色虚线 虚拟门  3蓝色实线 普通虚拟墙

# mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_VIRTUAL_WALL_ADD)

# 默认创建普通虚拟墙

# mBinding.mapView.addVirtualWall(3)

-----------------------------------------

# 编辑虚拟墙

# mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_VIRTUAL_WALL_EDIT)

-----------------------------------------

# 删除虚拟墙

# mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_VIRTUAL_WALL_DELETE)

-----------------------------------------

# 加载顶视路线

# mBinding.mapView.setTopViewPathDada(it)

-----------------------------------------

# 加载上线点

# mBinding.mapView.setInitPoseList(it.Initposes)

-----------------------------------------

# 加载工作路径 有任务的时候显示的路径

# mBinding.mapView.setWorkingPath(it.dparams)

-----------------------------------------

# 加载避让点

# mBinding.mapView.setCmsStations(cmsStation)

-----------------------------------------

# 加载充电站

# mBinding.mapView.setMachineStation(result)

-----------------------------------------

# 加载乘梯点

# mBinding.mapView.setElevators(elevatorPoint)

-----------------------------------------

# 删除噪点模式

# mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_REMOVE_NOISE)

# 删除噪点监听

# mBinding.mapView.setOnRemoveNoiseListener(object : MapView.IRemoveNoiseListener {

# override fun onRemoveNoise(leftTop: PointF, rightBottom: PointF) {

# }

# })

-----------------------------------------

# 显示清扫区域

# mBinding.mapView.setCleanAreaData(cleanAreas)
-----------------------------------------

# 编辑 设置地图的工作模式为编辑清扫区域模式

# mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_CLEAN_AREA_EDIT)

-----------------------------------------

# 添加清扫区域 设置地图的工作模式为添加清扫区域模式

# mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_CLEAN_AREA_ADD)
-----------------------------------------

# 设置清扫区域编辑监听器

# mBinding.mapView.setOnCleanAreaEditListener(object :

# PolygonEditView.OnCleanAreaEditListener {

# override fun onVertexDragEnd(area: CleanAreaNew, vertexIndex: Int) {}

# override fun onVertexAdded(

# area: CleanAreaNew, vertexIndex: Int, x: Float, y: Float) {}

# override fun onEdgeRemoved(area: CleanAreaNew, edgeIndex: Int) {}

# override fun onAreaCreated(area: CleanAreaNew) {

# // 将新创建的清扫区域添加到本地列表

# cleanAreas.add(area)

# }

# })