# RobotMapView 机器人分层地图加载

-----------------------------------------
# 布局引用
<com.siasun.dianshi.view.MapView
android:id="@+id/map_view"
android:layout_width="match_parent"
android:layout_height="match_parent" />
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
# 加载虚拟墙
# mBinding.mapView.setVirtualWall(it)
