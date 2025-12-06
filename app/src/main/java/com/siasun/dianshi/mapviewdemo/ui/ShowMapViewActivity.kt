package com.siasun.dianshi.mapviewdemo.ui

import android.graphics.Bitmap
import android.graphics.PointF
import android.os.Bundle
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.serializer.SerializeConfig
import com.alibaba.fastjson.serializer.SerializerFeature
import com.bumptech.glide.Glide
import kotlin.random.Random
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.jeremyliao.liveeventbus.LiveEventBus
import com.ngu.lcmtypes.laser_t
import com.ngu.lcmtypes.robot_control_t
import com.siasun.dianshi.base.BaseMvvmActivity
import com.siasun.dianshi.ConstantBase
import com.siasun.dianshi.ConstantBase.PAD_AREAS_NAME
import com.siasun.dianshi.ConstantBase.getFilePath
import com.siasun.dianshi.bean.CleanAreaNew
import com.siasun.dianshi.bean.PositingArea
import com.siasun.dianshi.controller.MainController
import com.siasun.dianshi.framework.ext.onClick
import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.mapviewdemo.KEY_AGV_COORDINATE
import com.siasun.dianshi.mapviewdemo.KEY_BOTTOM_CURRENT_POINT_CLOUD
import com.siasun.dianshi.mapviewdemo.KEY_CURRENT_POINT_CLOUD
import com.siasun.dianshi.mapviewdemo.KEY_POSITING_AREA_VALUE
import com.siasun.dianshi.mapviewdemo.RunningState
import com.siasun.dianshi.mapviewdemo.TaskState
import com.siasun.dianshi.mapviewdemo.databinding.ActivityShowMapViewBinding
import com.siasun.dianshi.mapviewdemo.utils.GsonUtil
import com.siasun.dianshi.mapviewdemo.viewmodel.ShowMapViewModel
import com.siasun.dianshi.utils.YamlNew
import com.siasun.dianshi.view.MapView
import com.siasun.dianshi.bean.CleanAreaRootNew
import com.siasun.dianshi.framework.ext.toBean
import com.siasun.dianshi.mapviewdemo.utils.FileIOUtil
import com.siasun.dianshi.view.PostingAreasView
import java.io.File

/**
 * 显示地图
 */
class ShowMapViewActivity : BaseMvvmActivity<ActivityShowMapViewBinding, ShowMapViewModel>() {

    val mapId = 1
    var positingAreas = mutableListOf<PositingArea>()
    var cleanAreas: MutableList<CleanAreaNew> = mutableListOf()

    override fun initView(savedInstanceState: Bundle?) {
        MainController.init()
        initListener()
        val file = File(ConstantBase.getFilePath(mapId, ConstantBase.PAD_MAP_NAME_PNG))
        Glide.with(this).asBitmap().load(file).skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE).into(object : SimpleTarget<Bitmap?>() {
                override fun onResourceReady(
                    resource: Bitmap, transition: Transition<in Bitmap?>?
                ) {
                    val mPngMapData = YamlNew().loadYaml(
                        ConstantBase.getFilePath(mapId, ConstantBase.PAD_MAP_NAME_YAML),
                        resource.height.toFloat(),
                        resource.width.toFloat(),
                    )
                    mBinding.mapView.setBitmap(mPngMapData, resource)
                }
            })


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

    }

    private fun initListener() {
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

        //删除噪点
        mBinding.btnRemoveNoise.setOnClickListener {
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_REMOVE_NOISE)
            // 设置去除噪点监听器
            mBinding.mapView.setOnRemoveNoiseListener(object : MapView.IRemoveNoiseListener {
                override fun onRemoveNoise(leftTop: PointF, rightBottom: PointF) {
                    // 处理噪点区域信息，这里可以添加日志或者发送到控制器
                    LogUtil.d("去除噪点区域: 左上角(${leftTop.x}, ${leftTop.y}), 右下角(${rightBottom.x}, ${rightBottom.y})")
                }
            })
        }

        //移动模式
        mBinding.btnMove.setOnClickListener {
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_SHOW_MAP)
        }
        //选中定位区域
        mBinding.btnPostingAreaEdit.setOnClickListener {

            // 随机选择一个定位区域高亮显示
            if (positingAreas.isNotEmpty()) {
                mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_POSITING_AREA_EDIT)

                // 生成0到positingAreas.size-1之间的随机索引
                val randomIndex = Random.nextInt(positingAreas.size)

                // 通过随机索引获取定位区域对象
                val randomArea = positingAreas[randomIndex]

                // 方式1：通过对象设置选中区域
                mBinding.mapView.setSelectedPositingArea(randomArea)

                // 方式2：通过ID设置选中区域（可选，根据需求选择其中一种）
                // mBinding.mapView.setSelectedPositingAreaId(randomArea.id)
            }
        }
        //删除定位区域
        mBinding.btnPostingAreaDel.onClick {
            // 随机选择一个定位区域进行删除
            if (positingAreas.isNotEmpty()) {
                // 生成0到positingAreas.size-1之间的随机索引
                val randomIndex = Random.nextInt(positingAreas.size)

                // 通过随机索引获取要删除的定位区域
                val randomArea = positingAreas[randomIndex]

                // 删除该定位区域
                mBinding.mapView.deletePositingArea(randomArea)

                // 更新本地列表
                positingAreas.remove(randomArea)

                LogUtil.d("随机删除了定位区域: ${randomArea.id}")
            }
        }
        //创建定位区域
        mBinding.btnPostingAreaAdd.onClick {
            // 设置创建定位区域模式
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_POSITING_AREA_ADD)

            // 设置定位区域创建监听器
            mBinding.mapView.setOnPositingAreaCreatedListener(object :
                PostingAreasView.OnPositingAreaCreatedListener {
                override fun onPositingAreaCreated(area: PositingArea) {
                    // 添加新创建的定位区域到列表
                    positingAreas.add(area)
                    // 切换回移动模式
                    mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_SHOW_MAP)
                }
            })
        }

        //保存定位区域
        mBinding.btnPostingAreaCommit.setOnClickListener {
            MainController.sendPositingArea(
                mapId, mBinding.mapView.mPostingAreasView!!.getPositingAreas()
            )
        }

        //编辑清扫区域
        mBinding.btnEditArea.onClick {
            if (cleanAreas.isNotEmpty()) {
                // 生成0到positingAreas.size-1之间的随机索引
                val randomIndex = Random.nextInt(cleanAreas.size)

                // 通过随机索引获取要删除的定位区域
                val randomArea = cleanAreas[randomIndex]

                // 设置地图的工作模式为编辑清扫区域模式
                mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_CLEAN_AREA_EDIT)

                // 将选中的区域设置到PolygonEditView1中进行编辑
                mBinding.mapView.mPolygonEditView1?.setSelectedArea(randomArea)
            }
        }
        //保存清扫区域
        mBinding.btnSaveArea.onClick {
            savePadAreasJson(mapId, cleanAreas)
        }
    }

    /**
     * 保存padAreas.json
     */
    fun savePadAreasJson(mapID: Int, cleanAreas: MutableList<CleanAreaNew>) {
        FileIOUtil.writeFileFromString(
            getFilePath(mapID, PAD_AREAS_NAME), JSONObject.toJSONString(
                CleanAreaRootNew(cleanAreas),
                SerializeConfig(true),
                SerializerFeature.PrettyFormat,
                SerializerFeature.DisableCircularReferenceDetect,
                SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNonStringKeyAsString,
            )
        )
    }

    override fun initData() {
        super.initData()
        mViewModel.getVirtualWall(mapId)
        MainController.sendGetPositingAreas(mapId)

        //接收导航定位区域
        LiveEventBus.get<String>(KEY_POSITING_AREA_VALUE).observe(this) {
            LogUtil.d("json ${it}")
            positingAreas = GsonUtil.jsonToList(it, PositingArea::class.java)
            mBinding.mapView.setPositingAreas(positingAreas)

        }

        //加载虚拟墙
        mViewModel.getVirtualWall.observe(this) {
//            mBinding.mapView.setVirtualWall(it)
        }
        //加载顶视路线
        mViewModel.getMergedPose(mapId, onComplete = { mergedPoses ->
            mergedPoses?.data?.let {
                mBinding.mapView.setTopViewPathDada(it)
            }
        })
        //加载上线点
        mViewModel.getInitPose(mapId, onComplete = { initPoses ->
            initPoses?.let {
                mBinding.mapView.setInitPoseList(it.Initposes)
            }
        })
        //加载站点
        mViewModel.getStationData(mapId, onComplete = { cmsStation ->
            mBinding.mapView.setCmsStations(cmsStation)
        })
        //加载充电站
        mViewModel.getMachineStation(onComplete = { machineStation ->
            LogUtil.d("获取充电站信息 $machineStation")
            val result = machineStation?.find { it.mapId == mapId }
            mBinding.mapView.setMachineStation(result)
        })
        //加载乘梯点
        mViewModel.getCmsElevator(mapId, onComplete = { elevatorPoint ->
            LogUtil.d("获取乘梯点 $elevatorPoint")
            mBinding.mapView.setElevators(elevatorPoint)
        })

        val jsonStr = FileIOUtil.readFile2String(getFilePath(mapId, PAD_AREAS_NAME), "utf-8")

        val cleanAreaRoot = jsonStr.toBean<CleanAreaRootNew>()
        // 如果解析成功，可以在这里处理数据
        cleanAreaRoot.cleanAreas.let {
            cleanAreas.addAll(it)
            mBinding.mapView.setCleanAreaData(cleanAreas)
        }
    }
}