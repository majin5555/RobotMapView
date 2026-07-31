package com.siasun.dianshi.mapviewdemo.ui

import android.content.Intent
import android.os.Bundle
import com.jxd.jxd_core.intent.startActivity
import com.siasun.dianshi.base.BaseMvvmActivity
import com.siasun.dianshi.framework.ext.onClick
import com.siasun.dianshi.mapviewdemo.databinding.AcTestBinding
import com.siasun.dianshi.mapviewdemo.ui.createMap.createMap3D.CreateMap3DActivity
import com.siasun.dianshi.mapviewdemo.viewmodel.TaskViewModel

class TestActivity : BaseMvvmActivity<AcTestBinding, TaskViewModel>()  {
    override fun initView(savedInstanceState: Bundle?) {


    mBinding.btn3d.onClick {

        startActivity<CreateMap3DActivity>()
    }
    }
}