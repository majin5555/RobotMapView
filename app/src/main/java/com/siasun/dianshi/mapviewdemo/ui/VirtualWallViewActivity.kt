package com.siasun.dianshi.mapviewdemo.ui

import android.graphics.Bitmap
import android.os.Bundle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.jeremyliao.liveeventbus.LiveEventBus
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.base.BaseMvvmActivity
import com.siasun.dianshi.ConstantBase
import com.siasun.dianshi.controller.MainController
import com.siasun.dianshi.mapviewdemo.KEY_BOTTOM_CURRENT_POINT_CLOUD
import com.siasun.dianshi.mapviewdemo.KEY_CURRENT_POINT_CLOUD
import com.siasun.dianshi.mapviewdemo.R
import com.siasun.dianshi.mapviewdemo.databinding.ActivityVirtualwallBinding
import com.siasun.dianshi.mapviewdemo.viewmodel.ShowMapViewModel
import com.siasun.dianshi.utils.YamlNew
import java.io.File

/**
 * 虚拟墙
 */
class VirtualWallViewActivity : BaseMvvmActivity<ActivityVirtualwallBinding, ShowMapViewModel>() {

    val mapId = 1
    override fun initView(savedInstanceState: Bundle?) {
        MainController.init()

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


        initListener()


    }

    override fun initData() {
        super.initData()

        //上激光点云
        LiveEventBus.get<laser_t>(KEY_CURRENT_POINT_CLOUD).observe(this) {
            mBinding.mapView.setUpLaserScan(it)
        }

        //下激光点云
        LiveEventBus.get<laser_t>(KEY_BOTTOM_CURRENT_POINT_CLOUD).observe(this) {
            mBinding.mapView.setDownLaserScan(it)
        }

        mViewModel.getVirtualWall(mapId)

        //加载虚拟墙
        mViewModel.getVirtualWall.observe(this) {
            mBinding.mapView.setVirtualWall(it)
        }

    }

    private fun initListener() {

        mBinding.rgTabType.setOnCheckedChangeListener { _, checkedId ->

            when (checkedId) {
                R.id.rb_move_map -> {
//                    mMapView.setWorkMode(SLAMMapView.MODE_SHOW_MAP)
//                    btnConfirm.gone()
                }

                R.id.rb_create -> {
//                    mMapView.setWorkMode(SLAMMapView.MODE_VIRTUAL_WALL_ADD)
//                    VmChoiceDialog.Builder(this)
//                        .setOnChoiceVMListener { mMapView.addVirtualWall(it) }.create().show()
//                    btnConfirm.visible()
                }

                R.id.rb_edit -> {
//                    mMapView.setWorkMode(SLAMMapView.MODE_VIRTUAL_WALL_EDIT)
//                    btnConfirm.visible()
                }

                R.id.rb_delete -> {
//                    mMapView.setWorkMode(SLAMMapView.MODE_VIRTUAL_WALL_DELETE)
//                    btnConfirm.visible()
                }
            }
        }
    }

}