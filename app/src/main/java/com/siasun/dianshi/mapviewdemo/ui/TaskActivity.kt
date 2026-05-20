package com.siasun.dianshi.mapviewdemo.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import com.siasun.dianshi.ConstantBase
import com.siasun.dianshi.bean.CleanAreaNew
import com.siasun.dianshi.view.TaskPolygonEditView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.view.isVisible
import com.chad.library.adapter4.dragswipe.QuickDragAndSwipe
import com.chad.library.adapter4.dragswipe.listener.DragAndSwipeDataCallback
import com.siasun.dianshi.AreaType
import com.siasun.dianshi.adapter.DiffDragAndSwipeAdapter
import com.siasun.dianshi.base.BaseMvvmActivity
import com.siasun.dianshi.bean.IntentParamTask
import com.siasun.dianshi.bean.MapInfo
import com.siasun.dianshi.bean.SweepingModeBean
import com.siasun.dianshi.bean.task.Sub_Task
import com.siasun.dianshi.dialog.CommonWarnDialog
import com.siasun.dianshi.dialog.SwitchMapDialog
import com.siasun.dianshi.framework.ext.onClick
import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.mapviewdemo.databinding.ActivityTaskBinding
import com.siasun.dianshi.mapviewdemo.viewmodel.TaskViewModel


/**
 * @author Mj
 * @create date 2025/12/6
 * 任务界面 创建、编辑共用
 */
class TaskActivity : BaseMvvmActivity<ActivityTaskBinding, TaskViewModel>() {
    private lateinit var intentParam: IntentParamTask
    var mapID = 0

    //所有地图名称
    val mMapName: MutableMap<Int, String> = mutableMapOf()

    //地图信息列表
    private val mMapInfoList: MutableList<MapInfo> = mutableListOf()


    //已选区域
    private lateinit var mSelectedAreaAdapter: DiffDragAndSwipeAdapter
    private var quickDragAndSwipe =
        QuickDragAndSwipe().setDragMoveFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN)

    //已选区域数据源
    private var mSelectList: MutableList<Sub_Task> = mutableListOf()

    //当前地图的区域数据
    private var currentAreas: MutableList<CleanAreaNew> = mutableListOf()


    @RequiresApi(Build.VERSION_CODES.R)
    override fun initView(savedInstanceState: Bundle?) {
        intentParam = intent.getSerializableExtra("key_jump_create_task") as IntentParamTask

        if (intentParam.createTask) {
//            mBinding.cusTitleBar.setTitle(Context.getString(R.string.create_task))
            //暂时用作任务ID
            intentParam.task.mTaskFlag = intentParam.task.hashCode()

        } else {
//            mBinding.cusTitleBar.setTitle(Context.getString(R.string.edit_task))
        }
        mBinding.mapView.showCoordinates(false)
        initAdapter()
        initListener()
        initCleaningParamsListBean()
        showEditTaskMsg()
    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun initData() {
        super.initData()
        //获取地图列表
        mViewModel.getMapList {
            lifecycleScope.launch(Dispatchers.Main) {
                it.forEach { item ->
                    mMapName[item.mapId] = item.mapName
                }
                mSelectedAreaAdapter.setMapName(mMapName)

                it.let { list ->
                    mMapInfoList.addAll(it)
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (mMapName.isNotEmpty()) {
                            val mapInfo = list[0]
                            mBinding.mapView.setMapName(mapInfo.mapName)
                            //设置当前地图ID
                            mapID = mapInfo.mapId
                            loadMap()
                            //获取当前地图下的区域
                            setAreaList()
                        }
                    }
                }
            }
        }
    }


    private fun initAdapter() {
        //已选列表
        mSelectedAreaAdapter = DiffDragAndSwipeAdapter()
        mBinding.rvSelected.apply {
            layoutManager = LinearLayoutManager(this@TaskActivity)
//            addItemDecoration(Decoration.LinearDecoration(0, dip2px(this@TaskActivity, 10F), 0, 0))
            adapter = mSelectedAreaAdapter
            mSelectedAreaAdapter.submitList(mSelectList)
            // 添加这行代码来禁用ItemAnimator，解决拖拽item视图重叠的问题
            itemAnimator = null
        }
    }

    /**
     * 清扫模式
     */
    private fun initCleaningParamsListBean() {
        mViewModel.getSweepingMode() {
            mSelectedAreaAdapter.setSweepingModeList(it.data)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun initListener() {
        //切换地图
        mBinding.btnMap.onClick {
            SwitchMapDialog.Builder(mMapInfoList, this, mapID).setOnItemClickListener {
                //判断区域文件存在不存在，不存在则 提示创建区域
                if (it.mapId != mapID) {
                    LogUtil.i("点击任务 切换的地图 $it")
                    mapID = it.mapId
                    mBinding.mapView.setMapName("${mMapName[it.mapId]}")
                    loadMap()
                    setAreaList()
                }

            }.create().show()
        }
        //修改清扫模式
//        mBinding.btnMode.onClick {
//            mViewModel.getSweepingMode() {
//                EditCleanModeDialog.Builder(this@TaskActivity, it)
//                    .setOnItemSelectedListener({ bean ->
//                        for (subTask in mSelectedAreaAdapter.items) {
//                            updateCleanMode(subTask, bean)
//                        }
//                        mSelectedAreaAdapter.notifyDataSetChanged()
//                    }).create().show()
//            }
//        }

        // 控制已选区域列表的显示与隐藏
        mBinding.ivToggleList.onClick {
            if (mBinding.rvSelected.isVisible) {
                mBinding.rvSelected.visibility = View.GONE
            } else {
                mBinding.rvSelected.visibility = View.VISIBLE
            }
        }

        //待选监听
        mBinding.mapView.setOnTaskAreaSelectedListener(object :
            TaskPolygonEditView.OnTaskAreaSelectedListener {
            override fun onSelectedAreaChanged(area: CleanAreaNew?) {
                if (area == null) return
                //待选区域点击事件
                val subTask = Sub_Task()
                //区域名称
                subTask.sub_task_name = area.sub_name
                //循环次数
                subTask.sub_cycle = 1
                subTask.sub_region_id = area.regId
                subTask.sub_region_layer = area.layer_id
                subTask.pathFrom = (area.areaPathType)
                //区域类型
                subTask.areaType = area.areaType
                subTask.pathType = (area.cleanShape)

                area.areaStartPoint.let {
                    val startPoint = FloatArray(2)
                    startPoint[0] = area.areaStartPoint.x
                    startPoint[1] = area.areaStartPoint.y
                    subTask.areaStartPnt = startPoint
                }
                subTask.cleanMode = 1
//                subTask.waterLevel = WATER_LEVEL_DEFAULT
//                subTask.speed = SPEED_DEFAULT
//                subTask.brushHeight = BRASH_HEIGHT_DEFAULT_750_STANDARD
                subTask.areaVertexPnt = (area.m_VertexPnt)
                mSelectedAreaAdapter.add(subTask)

                // 更新地图高亮
                mBinding.mapView.setCleanAreaHighlight(area)
            }
        })

        //已选滑动监听
        quickDragAndSwipe.attachToRecyclerView(mBinding.rvSelected)
            .setDataCallback(object : DragAndSwipeDataCallback {
                override fun dataMove(fromPosition: Int, toPosition: Int) {
                    mSelectedAreaAdapter.swap(fromPosition, toPosition)
                }

                override fun dataRemoveAt(position: Int) {
                    val item = mSelectedAreaAdapter.items[position]
                    mSelectedAreaAdapter.removeAt(position)

                    // 同步取消地图高亮
                    if (item.sub_region_layer == mapID) {
                        val hasSameArea =
                            mSelectedAreaAdapter.items.any { it.sub_region_id == item.sub_region_id && it.sub_region_layer == mapID }
                        if (!hasSameArea) {
                            currentAreas.find { it.regId == item.sub_region_id }?.let { area ->
//                                mBinding.mapView.setCleanAreaCancelHighlight(area)
                            }
                        }
                    }
                }
            })

        //已选区域删除监听
        mSelectedAreaAdapter.setOnItemDeleteClickListener { item ->
            CommonWarnDialog.Builder(this@TaskActivity).setMsg("删除")
                .setOnCommonWarnDialogListener {
                    mSelectedAreaAdapter.remove(item)

                    // 同步取消地图高亮
                    if (item.sub_region_layer == mapID) {
                        val hasSameArea =
                            mSelectedAreaAdapter.items.any { it.sub_region_id == item.sub_region_id && it.sub_region_layer == mapID }
                        if (!hasSameArea) {
                            currentAreas.find { it.regId == item.sub_region_id }?.let { area ->
//                               mBinding.mapView.setCleanAreaCancelHighlight(area)
                            }
                        }
                    }
                }.create().show()
        }
        //已选区域设置更改监听
        mSelectedAreaAdapter.setOnItemSettingClickListener { subTask, position ->
//            mViewModel.getSweepingMode() {
//                TaskCommonSettingDialog.Builder(this@TaskActivity, it)
//                    .setOnModeSelectedListener({ bean, mSubCycle ->
//                        updateCleanMode(subTask, bean)
//                        //清扫区域循环次数
//                        subTask.sub_cycle = mSubCycle
//                        //改变数据源
//                        mSelectedAreaAdapter[position] = subTask
//                    }).setEdit(subTask).create().show()
//            }
        }

        //已选区域点击高亮联动
        mSelectedAreaAdapter.setOnItemClickListener { subTask, position ->
            val areaMapId = subTask.sub_region_layer
            val areaRegId = subTask.sub_region_id

            if (areaMapId != mapID) {
                // 如果点击的区域是非当前地图，则切换地图后高亮显示
                val mapInfo = mMapInfoList.find { it.mapId == areaMapId }
                if (mapInfo != null) {
                    LogUtil.i("点击区域列表联动 切换地图 $mapInfo")
                    mapID = mapInfo.mapId
                    mBinding.mapView.setMapName("${mMapName[mapInfo.mapId]}")
                    loadMap()
                    setAreaList(areaRegId)
                }
            } else {
                // 如果是当前地图，直接高亮显示
                currentAreas.forEach { area ->
                    if (area.regId == areaRegId) mBinding.mapView.setCleanAreaHighlight(area)
                }

            }
        }

        mBinding.btnSaveTask.setOnClickListener {
//            //任务下已选区域
//            intentParam.task.sub_task = mSelectedAreaAdapter.items
//            SaveTaskDialog.Builder(intentParam, this).setOnSaveClickListener { mParam ->
//                //调取保存任务接口
//                mViewModel.savePadJobs(mParam.task) {
//                    if (it == 1000 || it == 0) {
//                        ToastUtils.showLong(R.string.warm_21)
//                    } else {
//                        // 如果是新建任务（createTask为true），将标识传回；如果是编辑，也把当前任务标识传回
//                        val resultIntent = Intent().putExtra(KEY_TASK, mParam)
//                            .putExtra(KEY_IS_NEW_TASK, mParam.createTask)
//                        Activity.setResult(RES_TASKLIST, resultIntent)
//                        Activity.finish()
//                    }
//                }
//            }.create().show()
        }
    }

    /**
     * 获取当前地图下的区域
     */
    fun setAreaList(highlightAreaId: Int? = null) {
        val selectedRegIds = mSelectedAreaAdapter.items
            .filter { it.sub_region_layer == mapID }
            .map { it.sub_region_id }

        mViewModel.getAreaList(mapID) { areas ->
            lifecycleScope.launch(Dispatchers.IO) {
                // 过滤掉routeType等于3、4、11的数据
                val filteredAreas = areas.filter {
                    it.routeType != AreaType.AREA_DOOR_LORIA_ID && it.routeType != AreaType.AREA_WORK && it.routeType != AreaType.AREA_DOOR_NO_LORIA_ID
                } as MutableList<CleanAreaNew>

                currentAreas.clear()
                currentAreas.addAll(filteredAreas)

                //排序
                if (filteredAreas.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        mBinding.mapView.setTaskAreaData(filteredAreas)

                        // 批量高亮显示已选中的区域
                        filteredAreas.forEach { area ->
                            if (selectedRegIds.contains(area.regId) || area.regId == highlightAreaId) {
                                mBinding.mapView.setCleanAreaHighlight(area)
                            }
                        }
                    }
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
//                        ToastUtils.showShort(R.string.tips_clean_areas)
                    }
                }
            }
        }
    }

    /**
     * 更新清扫模式
     */
    fun updateCleanMode(subTask: Sub_Task, bean: SweepingModeBean) {
        //清扫模式（车体识别）
        subTask.cleanMode = bean.mode
        //清扫模式ID（pad识别）
        subTask.customCleanMode = bean.id
        //清扫模式撒水量
        subTask.waterLevel = bean.sprinklerQuantity
        //清扫模式速度
        subTask.speed = bean.bodySpeed
        //清扫模式沙盘下降高度
        subTask.brushHeight = bean.brushDownHeight
        //潔尔亮
        subTask.cleanSolution = bean.detergent.toByte()
    }

    /**
     * 回显编辑信息 只有编辑任务时才回显示
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun showEditTaskMsg() {
        if (!intentParam.createTask) {
            intentParam.task.also { task ->
                //显示已选择区域
                mSelectedAreaAdapter.addAll(task.sub_task)
                mSelectedAreaAdapter.notifyDataSetChanged()
            }
        }
    }

    fun loadMap() {
        mBinding.mapView.loadMap(
            ConstantBase.getFilePath(mapID, ConstantBase.PAD_MAP_NAME_PNG),
            ConstantBase.getFilePath(mapID, ConstantBase.PAD_MAP_NAME_YAML)
        )
    }
}


