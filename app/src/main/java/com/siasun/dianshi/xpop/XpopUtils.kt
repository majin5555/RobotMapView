package com.siasun.dianshi.xpop

import VirtualWallLineNew
import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import com.lxj.xpopup.XPopup
import com.siasun.dianshi.bean.CmsStation
import com.siasun.dianshi.bean.MachineStation

/******************************************
 * 类描述：
 *
 * @author: why
 * @time: 2025/7/14 13:52
 ******************************************/

class XpopUtils(var mActivity: Activity) {

//    fun showMapOptionDialog(
//        mapName: String,
//        onMapManagerCall: () -> Unit,
//        onPathManagerCall: () -> Unit,
//        onAreaManagerCall: () -> Unit,
//        onStationManagerCall: () -> Unit,
//    ) {
//
//        val popup = MapOptionDialog(mActivity, mapName).apply {
//            this.onMapManagerCall = onMapManagerCall
//            this.onPathManagerCall = onPathManagerCall
//            this.onAreaManagerCall = onAreaManagerCall
//            this.onStationManagerCall = onStationManagerCall
//        }
//
//        XPopup.Builder(mActivity).isDestroyOnDismiss(true).hasStatusBar(false)
//            .hasNavigationBar(false).dismissOnTouchOutside(true).dismissOnBackPressed(false)
//            .asCustom(popup).show()
//    }

//    fun showEditMapDialog(
//        mapName: String,
//        onClearCall: () -> Unit,
//        onExtendCall: () -> Unit,
//        onTopCall: () -> Unit,
//        onRecordDxCall: () -> Unit,
//        onPreviewMapCall: () -> Unit
//    ) {
//        val popup = EditMapOptionDialog(mActivity, mapName).apply {
//            this.onClearCall = onClearCall
//            this.onExtendCall = onExtendCall
//            this.onTopCall = onTopCall
//            this.onRecordDxCall = onRecordDxCall
//            this.onPreviewMapCall = onPreviewMapCall
//        }
//
//        XPopup.Builder(mActivity).isDestroyOnDismiss(true).hasStatusBar(false)
//            .hasNavigationBar(false).dismissOnTouchOutside(true).dismissOnBackPressed(false)
//            .asCustom(popup).show()
//    }

//    fun showExpandMapDialog(
//        onSelectorCall: (Int) -> Unit,
//    ) {
//        val popup = ExpandMapOptionDialog(mActivity).apply {
//            this.onSelectorCall = onSelectorCall
//        }
//
//        XPopup.Builder(mActivity).isDestroyOnDismiss(true).hasStatusBar(false)
//            .hasNavigationBar(false).dismissOnTouchOutside(true).dismissOnBackPressed(false)
//            .asCustom(popup).show()
//    }

//    fun showSelectorPathDialog(
//        onConfirmCall: (path: Int) -> Unit
//    ) {
//        val popup = SelectPathDialog(mActivity).apply {
//            this.onConfirmCall = onConfirmCall
//        }
//        XPopup.Builder(mActivity).isDestroyOnDismiss(true).hasStatusBar(false)
//            .hasNavigationBar(false).dismissOnTouchOutside(true).dismissOnBackPressed(false)
//            .asCustom(popup).show()
//    }

//    fun showAttributeEditPointDialog(
//        nodeBaseAttr: NodeBaseAttr,
//        onConfirmCall: (nodeBaseAttr: NodeBaseAttr) -> Unit
//    ) {
//        val popup = AttributeEditPointDialog(mActivity, nodeBaseAttr).apply {
//            this.onConfirmCall = onConfirmCall
//        }
//        XPopup.Builder(mActivity).isDestroyOnDismiss(true).hasStatusBar(false)
//            .hasNavigationBar(false).dismissOnTouchOutside(true).dismissOnBackPressed(false)
//            .asCustom(popup).show()
//    }


//    fun showAttributeEditPathDialog(
//        pathBaseAttr: PathBaseAttr,
//        onConfirmCall: (pathBaseAttr: PathBaseAttr) -> Unit
//    ) {
//        val popup = AttributeEditPathDialog(mActivity, pathBaseAttr).apply {
//            this.onConfirmCall = onConfirmCall
//        }
//        XPopup.Builder(mActivity).isDestroyOnDismiss(true).hasStatusBar(false)
//            .hasNavigationBar(false).dismissOnTouchOutside(true).dismissOnBackPressed(false)
//            .asCustom(popup).show()
//    }


//    fun showVmWallEditDialog(
//        virtualWallLine: VirtualWallLineNew,
//        editVmWallListener: (virtualWallLine: VirtualWallLineNew) -> Unit
//    ) {
//        val popup = VmWallEditDialog(mActivity, virtualWallLine).apply {
//            this.editVmWallListener = editVmWallListener
//        }
//        XPopup.Builder(mActivity).isDestroyOnDismiss(true).hasStatusBar(false)
//            .hasNavigationBar(false).dismissOnTouchOutside(true).dismissOnBackPressed(false)
//            .asCustom(popup).show()
//    }

//    fun showVmChoiceDialog(
//        onConfirmCall: (selectType: Int) -> Unit,
//        onCancelCall: () -> Unit,
//    ) {
//        val popup = VmChoiceDialog(mActivity).apply {
//            this.onConfirmCall = onConfirmCall
//            this.onCancelCall = onCancelCall
//        }
//        XPopup.Builder(mActivity).isDestroyOnDismiss(true).hasStatusBar(false)
//            .hasNavigationBar(false).dismissOnTouchOutside(false).dismissOnBackPressed(false)
//            .asCustom(popup).show()
//    }


//    fun showElevatorMngDialog(
//        onConfirmCall: (elevatorName: String, elevatorId: String, id: Int?) -> Unit,
//        elevatorBean: ElevatorBean? = null
//    ) {
//        val popup = ElevatorMngDialog(mActivity, elevatorBean).apply {
//            this.onConfirmCall = onConfirmCall
//        }
//        XPopup.Builder(mActivity).isDestroyOnDismiss(true).hasStatusBar(false)
//            .hasNavigationBar(false).dismissOnTouchOutside(true).dismissOnBackPressed(false)
//            .asCustom(popup).show()
//    }

//    @RequiresApi(Build.VERSION_CODES.R)
//    fun showMachineDialog(
//        onConfirmCall: (machineStation: MachineStation?) -> Unit,
//        onDeleteCall: () -> Unit,
//        mapId: Int,
//        machineStation: MachineStation? = null
//    ) {
//        val popup = MachineStationDialog(mActivity, mapId, machineStation).apply {
//            this.onConfirmCall = onConfirmCall
//            this.onDeleteCall = onDeleteCall
//        }
//        XPopup.Builder(mActivity).isDestroyOnDismiss(true).hasStatusBar(false)
//            .hasNavigationBar(false).dismissOnTouchOutside(true).dismissOnBackPressed(false)
//            .autoFocusEditText(false).autoFocusEditText(false)
//            .asCustom(popup).show()
//    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun showCmsStationDialog(
        onConfirmCall: (cmsStation: CmsStation?) -> Unit,
        onDeleteCall: () -> Unit,
        mapId: Int,
        cmsStation: CmsStation? = null
    ) {
        val popup = CmsStationDialog(mActivity, mapId, cmsStation).apply {
            this.onConfirmCall = onConfirmCall
            this.onDeleteCall = onDeleteCall
        }
        XPopup.Builder(mActivity).isDestroyOnDismiss(true).hasStatusBar(false)
            .hasNavigationBar(false).dismissOnTouchOutside(true).dismissOnBackPressed(false)
            .asCustom(popup).show()
    }

//    fun showPositionAreaTipsDialog() {
//        val popup = PositionAreaTipsDialog(mActivity).apply {
//        }
//        XPopup.Builder(mActivity).isDestroyOnDismiss(true).hasStatusBar(false)
//            .hasNavigationBar(false).dismissOnTouchOutside(true).dismissOnBackPressed(false)
//            .asCustom(popup).show()
//    }
}