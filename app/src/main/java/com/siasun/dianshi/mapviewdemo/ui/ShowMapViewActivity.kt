package com.siasun.dianshi.mapviewdemo.ui

import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.jxd.jxd_core.intent.startActivity
import com.ngu.lcmtypes.laser_t
import com.ngu.lcmtypes.robot_control_t
import com.siasun.dianshi.AreaType
import com.siasun.dianshi.ConstantBase
import com.siasun.dianshi.ConstantBase.PAD_WORLD_NAME
import com.siasun.dianshi.ConstantBase.getFolderPath
import com.siasun.dianshi.base.BaseMvvmActivity
import com.siasun.dianshi.bean.CleanAreaNew
import com.siasun.dianshi.dialog.CommonWarnDialog
import com.siasun.dianshi.bean.CmsStation
import com.siasun.dianshi.bean.DragLocationBean
import com.siasun.dianshi.bean.ElevatorPoint
import com.siasun.dianshi.bean.Gate
import com.siasun.dianshi.bean.GatePointBean
import com.siasun.dianshi.bean.Inspection
import com.siasun.dianshi.bean.PassPoints
import com.siasun.dianshi.bean.PlanPathResult
import com.siasun.dianshi.bean.PositingArea
import com.siasun.dianshi.bean.PstParkBean
import com.siasun.dianshi.bean.RC.RCData
import com.siasun.dianshi.bean.RFID
import com.siasun.dianshi.bean.SameSwitchBean
import com.siasun.dianshi.bean.SpArea
import com.siasun.dianshi.bean.StationCoordinate
import com.siasun.dianshi.bean.TeachPoint
import com.siasun.dianshi.bean.WaitPointBean
import com.siasun.dianshi.bean.WorkAreasNew
import com.siasun.dianshi.bean.pp.world.PathIndex
import com.siasun.dianshi.controller.MainController
import com.siasun.dianshi.framework.ext.onClick
import com.siasun.dianshi.framework.ext.toBean
import com.siasun.dianshi.framework.ext.toJson
import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.mapviewdemo.CLEAN_PATH_PLAN
import com.siasun.dianshi.mapviewdemo.KEY_AGV_COORDINATE
import com.siasun.dianshi.mapviewdemo.KEY_BOTTOM_CURRENT_POINT_CLOUD
import com.siasun.dianshi.mapviewdemo.KEY_CURRENT_POINT_CLOUD
import com.siasun.dianshi.mapviewdemo.KEY_LOCATION_DRAG
import com.siasun.dianshi.mapviewdemo.KEY_POSITING_AREA_VALUE
import com.siasun.dianshi.mapviewdemo.KEY_TEACH_PATH
import com.siasun.dianshi.mapviewdemo.KEY_UPDATE_PLAN_PATH_RESULT
import com.siasun.dianshi.mapviewdemo.PATH_MODE
import com.siasun.dianshi.mapviewdemo.TAG_PP
import com.siasun.dianshi.mapviewdemo.TEACH_PATH_PLAN
import com.siasun.dianshi.mapviewdemo.databinding.ActivityShowMapViewBinding
import com.siasun.dianshi.mapviewdemo.ui.createMap.DragPositionViewActivity
import com.siasun.dianshi.mapviewdemo.utils.GsonUtil
import com.siasun.dianshi.mapviewdemo.utils.PathPlanningUtil1
import com.siasun.dianshi.mapviewdemo.viewmodel.ShowMapViewModel
import com.siasun.dianshi.network.constant.KEY_NEY_IP
import com.siasun.dianshi.utils.World
import com.siasun.dianshi.view.HomeDockView
import com.siasun.dianshi.view.InspectionView
import com.siasun.dianshi.view.MapView.ISingleTapListener
import com.siasun.dianshi.view.MixAreaView
import com.siasun.dianshi.view.PolygonEditView
import com.siasun.dianshi.view.PolygonEditViewPoint
import com.siasun.dianshi.view.PostingAreasView
import com.siasun.dianshi.view.RFIDView
import com.siasun.dianshi.view.SameSwitchView
import com.siasun.dianshi.view.SpPolygonEditView
import com.siasun.dianshi.view.VirtualWallView
import com.siasun.dianshi.view.WorkMode
import com.siasun.dianshi.xpop.XpopUtils
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import java.io.File
import java.util.UUID
import kotlin.random.Random

/**
 * 显示地图
 */
class ShowMapViewActivity : BaseMvvmActivity<ActivityShowMapViewBinding, ShowMapViewModel>() {
    private val mDragBean = DragLocationBean()
    private val mReflectorMaps = mutableListOf<com.siasun.dianshi.bean.ReflectorMapBean>()


    val mapId = 3
    var cleanAreas: MutableList<CleanAreaNew> = mutableListOf()
    var mSpArea: MutableList<SpArea> = mutableListOf()
    var mMixArea: MutableList<WorkAreasNew> = mutableListOf()
    var cmsStation: MutableList<CmsStation> = mutableListOf()

    // 创建World对象并读取文件
    val world = World()

    @RequiresApi(Build.VERSION_CODES.R)
    override fun initView(savedInstanceState: Bundle?) {
        MMKV.defaultMMKV().encode(KEY_NEY_IP, "192.168.3.101")

        MainController.init()
        //加载地图
        mBinding.mapView.loadMap(
            ConstantBase.getFilePath(mapId, ConstantBase.PAD_MAP_NAME_PNG),
            ConstantBase.getFilePath(mapId, ConstantBase.PAD_MAP_NAME_YAML)
        )
        mBinding.btnAddTeachPath888.onClick {
//            val json3="{\"dparams\":[],\"fparams\":[],\"iparams\":[],\"lparams\":[],\"m_cPathTypeBuffer\":[1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],\"m_fCleanPathPlanStartPosBuffer\":[15.683,-3.115,3.43618412E17],\"m_fElementBuffer\":[39.721333,15.8,39.04981,15.777804,38.374836,15.711217,37.703693,15.7055645,37.703693,15.7055645,37.03255,15.699911,36.36795,15.652758,35.69729,15.660104,35.69729,15.660104,35.026627,15.667449,34.35931,15.644395,33.68634,15.643832,33.68634,15.643832,33.013374,15.64327,32.331932,15.633541,31.658041,15.615267,31.658041,15.615267,30.984148,15.5969925,30.314318,15.5918045,29.64044,15.583754,29.64044,15.583754,28.966562,15.575703,28.290234,15.55923,27.616777,15.524065,27.616777,15.524065,26.943323,15.4889,26.27167,15.44839,25.596718,15.475716,25.596718,15.475716,24.921764,15.503041,24.242765,15.509002,23.566954,15.503495,23.566954,15.503495,22.891144,15.497988,22.21086,15.475346,21.538233,15.45624,21.538233,15.45624,20.865606,15.437133,20.200048,15.417529,19.530483,15.40477,19.530483,15.40477,18.860918,15.39201,18.197222,15.384541,17.526636,15.371365,17.526636,15.371365,16.85605,15.358188,16.176405,15.3674135,15.504976,15.352888,15.504976,15.352888,14.833548,15.338363,14.14159,15.283519,13.478365,15.272807,13.478365,15.272807,12.815141,15.262095,12.108112,15.199871,11.473271,15.216878,11.473271,15.216878,10.838431,15.233886,10.014508,15.08311,9.49446,15.324536,9.49446,15.324536,8.974412,15.565962,7.706413,15.804664,7.9560537,15.076769,7.9560537,15.076769,8.205694,14.348875,9.2460165,14.847717,9.833997,14.814517,9.833997,14.814517,10.421978,14.781318,11.176502,14.912625,11.828522,14.8818035,11.828522,14.8818035,12.480542,14.850981,13.178333,14.897853,13.846094,14.887654,13.846094,14.887654,14.513854,14.877456,15.19219,14.951988,15.864042,14.961325,15.864042,14.961325,16.535894,14.970662,17.213882,14.991205,17.886375,14.979115,17.886375,14.979115,18.558868,14.967023,19.229092,15.006846,19.900959,15.027129,19.900959,15.027129,20.572828,15.047412,21.249964,15.05349,21.921492,15.049951,21.921492,15.049951,22.593018,15.046411,23.259125,15.116554,23.929247,15.1157255,23.929247,15.1157255,24.599367,15.114896,25.266645,15.1319475,25.936481,15.151996,25.936481,15.151996,26.606318,15.172043,27.277252,15.200761,27.947973,15.19894,27.947973,15.19894,28.618692,15.197119,29.28871,15.225064,29.959688,15.22956,29.959688,15.22956,30.630669,15.234055,31.30367,15.217395,31.9738,15.260744,31.9738,15.260744,32.643932,15.304093,33.307415,15.218848,33.980423,15.259467,33.980423,15.259467,34.653427,15.300086,35.32185,15.29762,36.002396,15.3304205,36.002396,15.3304205,36.68294,15.363221,37.338768,15.290271,38.037964,15.399033,38.037964,15.399033,38.737156,15.507793,39.250114,14.63163,39.967865,15.0338335,39.967865,15.0338335,40.68561,15.436037,39.862907,16.376585,39.177155,16.115719,39.177155,16.115719,38.4914,15.854854,37.835323,16.031933,37.15928,16.048811,37.15928,16.048811,36.48323,16.065691,35.819504,16.071468,35.147125,16.061018,35.147125,16.061018,34.474743,16.050566,33.807068,16.043352,33.135254,16.03915,33.135254,16.03915,32.463436,16.034946,31.78731,16.054602,31.116398,16.028383,31.116398,16.028383,30.445486,16.002165,29.77593,16.020523,29.106627,16.00705,29.106627,16.00705,28.437325,15.993576,27.771326,16.019531,27.103144,16.014261,27.103144,16.014261,26.434961,16.008993,25.772673,15.943722,25.103209,15.95639,25.103209,15.95639,24.433744,15.96906,23.75869,15.932035,23.089613,15.903627,23.089613,15.903627,22.420534,15.87522,21.75599,15.910895,21.088,15.912212,21.088,15.912212,20.42001,15.91353,19.75589,15.899944,19.086098,15.887609,19.086098,15.887609,18.416307,15.875273,17.741932,15.822024,17.0698,15.8294115,17.0698,15.8294115,16.397667,15.836798,15.733293,15.822312,15.058986,15.812802,15.058986,15.812802,14.384679,15.803293,13.740151,15.789309,13.051661,15.788449,13.051661,15.788449,12.36317,15.787589,11.750727,15.775416,10.99712,15.762945,10.99712,15.762945,10.243512,15.750473,9.28469,15.693305,8.485,15.683],\"m_fGloalPathPlanGoalPosBuffer\":[1.3759809E-38,0.0,0.0],\"m_fGloalPathPlanStartPosBuffer\":[0.4,1.5755796E-38,0.0],\"m_fRegionPointsBuffer\":[],\"m_iAddLaser\":0,\"m_iCleanPathPanType\":0,\"m_iElementSum\":384,\"m_iGloalPathPlanType\":0,\"m_iPathPlanPublicId\":13,\"m_iPathPlanPublicSubId\":0,\"m_iPathPlanRegionChoose\":53,\"m_iPathPlanType\":3,\"m_iPathSum\":48,\"m_iPlanResult\":1,\"m_iPlanResultMode\":0,\"m_iRegionNumber\":1091027599,\"m_iRegionPoints\":0,\"m_strAdditionInfo\":\"1.0.1.193\",\"m_strFrom\":\"PathPlan\",\"m_strTo\":\"pad\",\"m_uLayerNumber\":12588,\"ndparams\":0,\"nfparams\":0,\"niparams\":0,\"nlparams\":0,\"sparams\":\"\",\"utime\":0}"
            val json4 =
                "{\"dparams\":[],\"fparams\":[],\"iparams\":[],\"lparams\":[],\"m_cPathTypeBuffer\":[1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],\"m_fCleanPathPlanStartPosBuffer\":[25.862,-3.117,3.1620728E35],\"m_fElementBuffer\":[40.062,25.804,39.387703,25.810602,38.716415,25.751215,38.04105,25.746592,38.04105,25.746592,37.365685,25.74197,36.689564,25.720613,36.01306,25.712933,36.01306,25.712933,35.33656,25.705252,34.655544,25.702478,33.979397,25.689482,33.979397,25.689482,33.30325,25.676487,32.62538,25.679302,31.951876,25.663973,31.951876,25.663973,31.278372,25.648643,30.614199,25.628904,29.941591,25.63131,29.941591,25.63131,29.268984,25.633715,28.597654,25.61176,27.922607,25.597229,27.922607,25.597229,27.247559,25.582699,26.566221,25.555826,25.890007,25.541916,25.890007,25.541916,25.21379,25.528004,24.540682,25.533407,23.864075,25.526226,23.864075,25.526226,23.18747,25.519045,22.502283,25.537409,21.827349,25.530523,21.827349,25.530523,21.152412,25.523638,20.487541,25.442387,19.814386,25.43702,19.814386,25.43702,19.141232,25.431652,18.465046,25.448082,17.793053,25.42156,17.793053,25.42156,17.121061,25.395037,16.452095,25.387903,15.781707,25.360962,15.781707,25.360962,15.111319,25.33402,14.448771,25.337297,13.775929,25.332735,13.775929,25.332735,13.103087,25.328173,12.432775,25.317726,11.754166,25.309414,11.754166,25.309414,11.075558,25.301102,10.426033,25.133507,9.724972,25.281557,9.724972,25.281557,9.02391,25.429605,8.535073,24.347897,7.8612576,24.80156,7.8612576,24.80156,7.187442,25.255222,8.161446,26.02088,8.813148,25.856398,8.813148,25.856398,9.464849,25.691914,10.160942,25.815859,10.832324,25.809998,10.832324,25.809998,11.503707,25.804134,12.176946,25.848244,12.857081,25.865353,12.857081,25.865353,13.537217,25.882462,14.182873,25.86833,14.882232,25.874533,14.882232,25.874533,15.581592,25.880735,16.128107,25.929327,16.899961,25.92127,16.899961,25.92127,17.671816,25.913212,17.937841,25.983894,18.978895,25.9451,18.978895,25.9451,20.019949,25.906307,21.204557,26.238676,19.18,25.602,19.18,25.602,19.73525,26.019388,20.448076,25.969776,21.115318,26.012808,21.115318,26.012808,21.78256,26.05584,22.474463,26.10246,23.146753,26.125605,23.146753,26.125605,23.819046,26.148748,24.48519,26.103867,25.15602,26.087572,25.15602,26.087572,25.82685,26.071278,26.500221,26.018898,27.168684,25.970318,27.168684,25.970318,27.837149,25.921738,28.497967,25.860212,29.166521,25.812199,29.166521,25.812199,29.835075,25.764185,30.510044,25.739374,31.179312,25.693724,31.179312,25.693724,31.84858,25.648071,32.5117,25.689852,33.182743,25.712933,33.182743,25.712933,33.85379,25.736012,34.52073,25.775736,35.198936,25.745604,35.198936,25.745604,35.877144,25.715471,36.53814,25.827837,37.240616,25.769354,37.240616,25.769354,37.943092,25.710869,38.98699,26.313087,39.393,25.819,39.393,25.819,39.558334,25.833334,39.723667,25.847666,39.889,25.862],\"m_fGloalPathPlanGoalPosBuffer\":[1.3759809E-38,0.0,0.0],\"m_fGloalPathPlanStartPosBuffer\":[0.4,1.5755796E-38,0.0],\"m_fRegionPointsBuffer\":[],\"m_iAddLaser\":0,\"m_iCleanPathPanType\":0,\"m_iElementSum\":272,\"m_iGloalPathPlanType\":0,\"m_iPathPlanPublicId\":4,\"m_iPathPlanPublicSubId\":0,\"m_iPathPlanRegionChoose\":46,\"m_iPathPlanType\":3,\"m_iPathSum\":34,\"m_iPlanResult\":1,\"m_iPlanResultMode\":0,\"m_iRegionNumber\":1109364310,\"m_iRegionPoints\":0,\"m_strAdditionInfo\":\"1.0.1.193\",\"m_strFrom\":\"PathPlan\",\"m_strTo\":\"pad\",\"m_uLayerNumber\":13618,\"ndparams\":0,\"nfparams\":0,\"niparams\":0,\"nlparams\":0,\"sparams\":\"\",\"utime\":0}"
            val json5 =
                "{\"dparams\":[],\"fparams\":[],\"iparams\":[],\"lparams\":[],\"m_cPathTypeBuffer\":[1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],\"m_fCleanPathPlanStartPosBuffer\":[24.825,-3.096,3.1620728E35],\"m_fElementBuffer\":[39.904,25.862333,39.236538,25.813992,38.571064,25.811943,37.901184,25.811747,37.901184,25.811747,37.231304,25.811552,36.55792,25.758274,35.887486,25.74727,35.887486,25.74727,35.217056,25.736265,34.549095,25.73786,33.878975,25.71846,33.878975,25.71846,33.20885,25.699059,32.534702,25.697628,31.865633,25.665777,31.865633,25.665777,31.196566,25.633926,30.533142,25.660868,29.86234,25.642149,29.86234,25.642149,29.191542,25.62343,28.50932,25.628143,27.838356,25.628967,27.838356,25.628967,27.167395,25.629793,26.506622,25.623482,25.836252,25.610641,25.836252,25.610641,25.165884,25.597801,24.489801,25.600819,23.818329,25.59774,23.818329,25.59774,23.146856,25.594664,22.479414,25.539263,21.808022,25.523426,21.808022,25.523426,21.13663,25.50759,20.460546,25.488852,19.789766,25.473562,19.789766,25.473562,19.118984,25.458275,18.45393,25.511044,17.783882,25.48073,17.783882,25.48073,17.113834,25.450415,16.439474,25.415682,15.768112,25.388884,15.768112,25.388884,15.09675,25.362085,14.417578,25.339277,13.748511,25.330856,13.748511,25.330856,13.079444,25.322435,12.408555,25.32908,11.747271,25.271276,11.747271,25.271276,11.085986,25.21347,10.387596,25.239616,9.744929,25.190437,9.744929,25.190437,9.102263,25.141258,8.4857645,24.1901,7.891653,24.851772,7.891653,24.851772,7.2975416,25.513445,8.349933,25.976414,8.9834585,25.859669,8.9834585,25.859669,9.616984,25.742922,10.330651,25.768108,10.993895,25.755903,10.993895,25.755903,11.657139,25.7437,12.32748,25.841076,12.994801,25.847013,12.994801,25.847013,13.662121,25.85295,14.330002,25.842897,14.999526,25.861525,14.999526,25.861525,15.669049,25.880154,16.34205,25.918203,17.014875,25.930832,17.014875,25.930832,17.687698,25.943462,18.364235,25.927807,19.038586,25.925137,19.038586,25.925137,19.712934,25.922466,20.388468,25.970095,21.063683,25.967657,21.063683,25.967657,21.738897,25.965221,22.41611,26.001621,23.090567,26.007391,23.090567,26.007391,23.765024,26.01316,24.441294,26.006721,25.112505,26.033777,25.112505,26.033777,25.783714,26.060831,26.443905,26.014008,27.114563,26.028484,27.114563,26.028484,27.785221,26.04296,28.456211,26.009645,29.129946,26.044598,29.129946,26.044598,29.80368,26.079548,30.480206,26.098366,31.15773,26.088495,31.15773,26.088495,31.835255,26.078625,32.51068,26.124659,33.192112,26.12884,33.192112,26.12884,33.873547,26.13302,34.54078,26.11557,35.23087,26.104263,35.23087,26.104263,35.920956,26.092955,36.554825,26.08199,37.272427,26.037868,37.272427,26.037868,37.990032,25.99375,38.508064,26.047205,39.223164,26.443409,39.223164,26.443409,39.938263,26.839613,40.435528,25.497486,39.584545,25.43407,39.584545,25.43407,38.733566,25.370657,38.247765,25.489344,37.529125,25.451601,37.529125,25.451601,36.810486,25.413858,36.20466,25.346724,35.52038,25.358265,35.52038,25.358265,34.836098,25.369806,34.174156,25.339708,33.497906,25.315048,33.497906,25.315048,32.821655,25.29039,32.151573,25.271238,31.47754,25.257513,31.47754,25.257513,30.803505,25.24379,30.131887,25.256529,29.458988,25.242989,29.458988,25.242989,28.78609,25.229446,28.118101,25.204939,27.444086,25.191,27.444086,25.191,26.770073,25.177061,26.089,25.136917,25.414835,25.129436,25.414835,25.129436,24.740667,25.121954,24.074514,25.075167,23.4009,25.070997,23.4009,25.070997,22.727285,25.06683,22.055286,25.019823,21.379658,25.012892,21.379658,25.012892,20.70403,25.005962,20.038412,25.00104,19.357279,24.991585,19.357279,24.991585,18.676146,24.98213,18.02717,24.98333,17.330086,24.953047,17.330086,24.953047,16.633001,24.922764,16.075932,24.92779,15.28514,24.91986,15.28514,24.91986,14.494349,24.91193,13.396252,24.905287,12.536,24.894,12.536,24.894,11.841026,24.819193,11.198787,24.823208,10.485987,24.817509,10.485987,24.817509,9.773187,24.811808,8.996069,24.850632,8.268,24.825],\"m_fGloalPathPlanGoalPosBuffer\":[1.3759809E-38,0.0,0.0],\"m_fGloalPathPlanStartPosBuffer\":[0.4,1.5755796E-38,0.0],\"m_fRegionPointsBuffer\":[],\"m_iAddLaser\":0,\"m_iCleanPathPanType\":0,\"m_iElementSum\":384,\"m_iGloalPathPlanType\":0,\"m_iPathPlanPublicId\":7,\"m_iPathPlanPublicSubId\":0,\"m_iPathPlanRegionChoose\":46,\"m_iPathPlanType\":3,\"m_iPathSum\":48,\"m_iPlanResult\":1,\"m_iPlanResultMode\":0,\"m_iRegionNumber\":1090800058,\"m_iRegionPoints\":0,\"m_strAdditionInfo\":\"1.0.1.193\",\"m_strFrom\":\"PathPlan\",\"m_strTo\":\"pad\",\"m_uLayerNumber\":13362,\"ndparams\":0,\"nfparams\":0,\"niparams\":0,\"nlparams\":0,\"sparams\":\"\",\"utime\":0}"
            val toBean1 = json5.toBean<PlanPathResult>()
            val pathPlanResultBean = PathPlanningUtil1.getPathPlanResultBean(
                toBean1
            )
            if (pathPlanResultBean.m_vecBezierOfPathPlan.isNotEmpty()) {
                mBinding.mapView.setCleanPathPlanResultBean(
                    PathPlanningUtil1.getPathPlanResultBean(toBean1, mBinding.mapView)
                )
            } else {
                ToastUtils.showLong("保存试教失败")
            }
        }
        //拖拽定位
        mBinding.btnDragPositing.onClick {
            startActivity<DragPositionViewActivity>(KEY_LOCATION_DRAG to mDragBean)
        }

        //保存
        mBinding.btnSaveTeach.onClick {
            savePathsToFile()
            ToastUtils.showLong("保存试教")
        }

        //开始试教
        mBinding.btnStartTeach.onClick {
            mBinding.mapView.setWorkMode(WorkMode.TEACH)
            MainController.sendStartTeachRoute()
            ToastUtils.showLong("开始试教")
        }
        //结束试教
        mBinding.btnEndTeach.onClick {
            mBinding.mapView.setWorkMode(WorkMode.MODE_SHOW_MAP)
            MainController.sendSopTeachRoute()
            ToastUtils.showLong("结束试教")
        }

        //接收路径规划结果
        LiveEventBus.get<PlanPathResult>(KEY_UPDATE_PLAN_PATH_RESULT).observe(this) { result ->
            try {
                if (result.m_iPathPlanType == TEACH_PATH_PLAN) {
                    // 接收Pad申请的示教径规划结果
                    // 路段模式
                    if (result.m_iPlanResultMode == PATH_MODE) {
                        LogUtil.i(
                            "CustomPathActivity1接收示教路径规划", null, TAG_PP
                        )
                        mBinding.mapView.setCleanPathPlanResultBean(
                            PathPlanningUtil1.getPathPlanResultBean(result, mBinding.mapView)
                        )
                    }
                }
            } catch (e: Exception) {
                LogUtil.i("接收试教路径规划失败")
            }
        }

        //接收士教中的数据
        LiveEventBus.get<TeachPoint>(KEY_TEACH_PATH).observe(this) {
//            LogUtil.i("接收士教中的数据 $it", null, TAG_PP)
            mBinding.mapView.setTeachPoint(
                com.siasun.dianshi.bean.TeachPoint(
                    it.x, it.y, it.theta
                )
            )
        }


        //加载地图
        mBinding.mapView.loadMap(
            ConstantBase.getFilePath(mapId, ConstantBase.PAD_MAP_NAME_PNG),
            ConstantBase.getFilePath(mapId, ConstantBase.PAD_MAP_NAME_YAML)
        )
        //移动模式
        mBinding.btnMove.setOnClickListener {
            mBinding.mapView.setWorkMode(WorkMode.MODE_SHOW_MAP)
        }

        mBinding.mapView.laserDrawingEnabled(false)

//        initMergedPose()
//        initStation()
//        iniVirtualWall()
//        initRemoveNoise()
//        initPostingArea()
//        initRemoveNoise()
//        initPostingArea()
//        initCleanArea()
//        initElevator()
//        initPose()
//        initMachineStation()
//        initMixArea()
//        initSpAreas()
//        initPath()
        initCrossDoor()
//        initRFId()
//        initInspectionView()
//        initSameSwitch()


        //  事实上
        mBinding.btnAddGloblePath.onClick {

            //全局
            val gloJson2 =
                "{\"dparams\":[],\"fparams\":[],\"iparams\":[0,0,0,0,0,0],\"lparams\":[250,250,250,250,250,250],\"m_cPathTypeBuffer\":[1,1,1,1,1,1],\"m_fCleanPathPlanStartPosBuffer\":[0.0,0.0,0.0],\"m_fElementBuffer\":[29.556,2.773,28.81566,2.2297862,28.331419,2.521509,28.374287,3.2572424,28.374287,3.2572424,28.417152,3.992976,28.285984,4.535049,28.325058,5.2433286,28.325058,5.2433286,28.364132,5.951608,28.317911,6.563604,28.332159,7.251959,28.332159,7.251959,28.346405,7.9403143,28.321201,8.552634,28.336176,9.260889,28.336176,9.260889,28.351149,9.969143,28.30004,10.470266,28.346638,11.310041,28.346638,11.310041,28.393236,12.1498165,28.121954,13.249701,27.914,14.092],\"m_fGloalPathPlanGoalPosBuffer\":[27.914,14.092,1.46],\"m_fGloalPathPlanStartPosBuffer\":[29.556,2.773,1.55],\"m_fRegionPointsBuffer\":[],\"m_iAddLaser\":0,\"m_iCleanPathPanType\":0,\"m_iElementSum\":48,\"m_iGloalPathPlanType\":1,\"m_iPathPlanPublicId\":65538,\"m_iPathPlanPublicSubId\":127,\"m_iPathPlanRegionChoose\":0,\"m_iPathPlanType\":1,\"m_iPathSum\":6,\"m_iPlanResult\":1,\"m_iPlanResultMode\":0,\"m_iRegionNumber\":0,\"m_iRegionPoints\":0,\"m_strAdditionInfo\":\"1.0.1.176-MultyLayers\",\"m_strFrom\":\"PathPlan\",\"m_strTo\":\"CMS\",\"m_uLayerNumber\":1,\"ndparams\":0,\"nfparams\":0,\"niparams\":6,\"nlparams\":6,\"sparams\":\"\",\"utime\":0}"

            val gloJson =
                " {\"dparams\":[],\"fparams\":[],\"iparams\":[],\"lparams\":[],\"m_cPathTypeBuffer\":[],\"m_fCleanPathPlanStartPosBuffer\":[0.0,0.0,0.0],\"m_fElementBuffer\":[],\"m_fGloalPathPlanGoalPosBuffer\":[49.193,7.008,-0.89],\"m_fGloalPathPlanStartPosBuffer\":[-0.078,0.007,0.05],\"m_fRegionPointsBuffer\":[],\"m_iAddLaser\":0,\"m_iCleanPathPanType\":0,\"m_iElementSum\":0,\"m_iGloalPathPlanType\":1,\"m_iPathPlanPublicId\":65538,\"m_iPathPlanPublicSubId\":127,\"m_iPathPlanRegionChoose\":0,\"m_iPathPlanType\":1,\"m_iPathSum\":0,\"m_iPlanResult\":8,\"m_iPlanResultMode\":0,\"m_iRegionNumber\":0,\"m_iRegionPoints\":0,\"m_strAdditionInfo\":\"1.0.1.176-MultyLayers\",\"m_strFrom\":\"PathPlan\",\"m_strTo\":\"CMS\",\"m_uLayerNumber\":2,\"ndparams\":0,\"nfparams\":0,\"niparams\":0,\"nlparams\":0,\"sparams\":\"\",\"utime\":0}"
            val toBean = gloJson2.toBean<PlanPathResult>()

            val pathPlanResultBean = PathPlanningUtil1.getPathPlanResultBean(
                toBean
            )
            LogUtil.d("pathPlanResultBean.m_vecLineOfPathPlan.size ${pathPlanResultBean.m_vecLineOfPathPlan.size}")
            LogUtil.d("pathPlanResultBean.m_vecBezierOfPathPlan.size ${pathPlanResultBean.m_vecBezierOfPathPlan.size}")

            LogUtil.i(
                "CleanAutoActivity接收全局路径规划 ${pathPlanResultBean.toJson()}", null, TAG_PP
            )

            if (pathPlanResultBean.m_vecBezierOfPathPlan.isNotEmpty()) {
                mBinding.mapView.setGlobalPathPlanResultBean(
                    pathPlanResultBean
                )
            } else {
                ToastUtils.showLong("全局路径规划失败")
            }
        }

        mBinding.btnAddCleanPath.onClick {
            val json =
                "{\"dparams\":[],\"fparams\":[],\"iparams\":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"lparams\":[251,251,251,251,251,251,251,251,255,255,255,255,255,255,255,255,255,255,255,255],\"m_cPathTypeBuffer\":[1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],\"m_fCleanPathPlanStartPosBuffer\":[29.555,2.786,1.55],\"m_fElementBuffer\":[27.914648,14.092113,27.932629,14.26437,27.950836,14.419361,27.968824,14.596,27.968824,14.596,27.986814,14.772638,28.010489,14.95114,28.026693,15.136461,28.026693,15.136461,28.042898,15.321781,28.07783,15.44616,28.06549,15.660672,28.06549,15.660672,28.053152,15.875183,28.275229,15.871054,28.41646,15.733574,28.41646,15.733574,28.557692,15.596093,28.528708,15.415093,28.495771,15.238,28.495771,15.238,28.462835,15.060905,28.455446,14.876434,28.433403,14.698689,28.433403,14.698689,28.411358,14.520945,28.396637,14.348806,28.375942,14.172338,28.375942,14.172338,28.385729,14.121772,28.395517,14.071205,28.405304,14.020639,28.405304,14.020639,28.238195,13.970711,28.071644,13.983357,27.907492,13.912709,27.907492,13.912709,27.74334,13.842061,27.491655,13.862228,27.536877,14.076725,27.536877,14.076725,27.582098,14.291223,27.580418,14.412807,27.600128,14.598263,27.600128,14.598263,27.619839,14.783718,27.63125,14.970391,27.648703,15.14286,27.648703,15.14286,27.666155,15.3153305,27.678793,15.490119,27.693468,15.643509,27.693468,15.643509,27.708145,15.796899,27.656052,16.055065,27.837917,16.086344,27.837917,16.086344,28.019781,16.117622,28.16111,16.024391,28.341843,16.041842,28.341843,16.041842,28.522573,16.059292,28.698399,15.948868,28.70547,15.761645,28.70547,15.761645,28.712538,15.574423,28.787008,15.427553,28.816484,15.253507,28.816484,15.253507,28.84596,15.079459,28.912094,14.931723,28.933083,14.754136,28.933083,14.754136,28.954071,14.5765505,29.066494,14.439697,29.058037,14.23402,29.058037,14.23402,29.04958,14.028343,28.55292,14.106234,28.405304,14.020639],\"m_fGloalPathPlanGoalPosBuffer\":[0.0,0.0,0.0],\"m_fGloalPathPlanStartPosBuffer\":[0.0,0.0,0.0],\"m_fRegionPointsBuffer\":[27.267136,16.693888,26.919456,13.138414,29.841938,13.705275,29.146591,16.533278],\"m_iAddLaser\":513894328,\"m_iCleanPathPanType\":3,\"m_iElementSum\":160,\"m_iGloalPathPlanType\":0,\"m_iPathPlanPublicId\":48469,\"m_iPathPlanPublicSubId\":0,\"m_iPathPlanRegionChoose\":2,\"m_iPathPlanType\":2,\"m_iPathSum\":20,\"m_iPlanResult\":1,\"m_iPlanResultMode\":0,\"m_iRegionNumber\":48469,\"m_iRegionPoints\":8,\"m_strAdditionInfo\":\"1.0.1.176-MultyLayers\",\"m_strFrom\":\"PathPlan\",\"m_strTo\":\"CMS\",\"m_uLayerNumber\":1,\"ndparams\":0,\"nfparams\":0,\"niparams\":20,\"nlparams\":20,\"sparams\":\"\",\"utime\":0}"
            val toBean1 = json.toBean<PlanPathResult>()
            val pathPlanResultBean = PathPlanningUtil1.getPathPlanResultBean(
                toBean1
            )
            if (pathPlanResultBean.m_vecBezierOfPathPlan.isNotEmpty()) {
                mBinding.mapView.setCleanPathPlanResultBean(
                    pathPlanResultBean
                )
            } else {
                ToastUtils.showLong("全局路径规划失败")
            }
        }
    }

    private fun initInspectionView() {
        val mInspection = mutableListOf<Inspection>()

        mBinding.btnAddInspection.onClick {
            mInspection.add(
                Inspection(
                    "1_${UUID.randomUUID()}", "巡检1", true, StationCoordinate(
                        mBinding.mapView.getAgvData()?.get(0)!!.toFloat(),
                        mBinding.mapView.getAgvData()?.get(1)!!.toFloat(),
                        mBinding.mapView.getAgvData()?.get(2)!!.toFloat()
                    )
                )
            )
            mBinding.mapView.setInspectionViewStations(mInspection)

            LogUtil.i("获取json ${mBinding.mapView.getInspectionViewStations().toJson()} ")
        }
        //编辑巡检点
        mBinding.btnEditInspection.onClick {
            mBinding.mapView.setWorkMode(WorkMode.MODE_INSPECTION_STATION_EDIT)
        }
        mBinding.mapView.setOnInspectionStationClickListener(object :
            InspectionView.OnStationClickListener {
            override fun onStationClick(station: Inspection) {
                LogUtil.d("点击了巡检点 $station")
            }
        })
        //编辑删除
        mBinding.btnDeleteInspection.onClick {
            mBinding.mapView.setWorkMode(WorkMode.MODE_INSPECTION_STATION_DELETE)
        }
        mBinding.mapView.setOnInspectionStationDeleteListener(object :
            InspectionView.OnStationDeleteListener {
            override fun onStationDelete(station: Inspection) {
                LogUtil.d("删除了巡检点 $station")
            }
        })
    }

    //过门
    fun initCrossDoor() {
        // 设置线点击事件监听器
        mBinding.mapView.setOnCrossDoorLineClickListener(object :
            com.siasun.dianshi.view.CrossDoorView.OnCrossDoorLineClickListener {
            override fun onCrossDoorLineClick(crossDoor: com.siasun.dianshi.bean.CrossDoor) {
                // 点击了过门线，弹框显示信息
                showCrossDoorDialog(crossDoor)
                LogUtil.d("999 点击了过门线 ${mBinding.mapView.getCrossDoors()}")
            }
        })

        // 设置线点击事件监听器
        mBinding.mapView.setOnCrossDoorDeleteClickListener(object :
            com.siasun.dianshi.view.CrossDoorView.OnCrossDoorDeleteClickListener {
            override fun onCrossDoorDeleteClick(crossDoor: com.siasun.dianshi.bean.CrossDoor) {
                // 点击了过门线，弹框显示信息
                showCrossDoorDialog(crossDoor)
            }
        })

        //添加过门
        mBinding.btnAddCrossDoor.onClick {
            val crossDoor = com.siasun.dianshi.bean.CrossDoor(
                id = 1,
                map_id = 2,
                door_msg = com.siasun.dianshi.bean.DoorMsg(
                    type = "DOOR_001"
                ),
            )
            mBinding.mapView.addCrossDoor(crossDoor)
            ToastUtils.showLong("已进入添加过门模式")
        }


        mBinding.btnDeleteCrossDoor.onClick {
            // 切换到删除模式
            mBinding.mapView.setWorkMode(WorkMode.MODE_CROSS_DOOR_DELETE)
            ToastUtils.showLong("已进入删除过门模式，点击线段进行删除")
        }

        mBinding.btnEditCrossDoor.onClick {
            // 切换到编辑模式
            mBinding.mapView.setWorkMode(WorkMode.MODE_CROSS_DOOR_EDIT)
            ToastUtils.showLong("已进入编辑过门模式，可拖动端点修改位置")
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun initPath() {
        showLoading("加载中")
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
                toast("选中节点: id=${node.m_uId}, x=${node.x}, y=${node.y}, 所属路段: ${path.GetStartNode()?.m_uId}->${path.GetEndNode()?.m_uId}")
                mBinding.mapView.getLayer()?.updateNodeAttr(node)

                // 这里可以显示节点属性编辑界面，或者执行其他操作
            }

            override fun onPathSelected(path: com.siasun.dianshi.bean.pp.world.Path) {
                // 处理选中路段事件
                val startNode = path.GetStartNode()
                val endNode = path.GetEndNode()
                Log.d("ShowMapViewActivity", "选中路段: ${startNode?.m_uId}->${endNode?.m_uId}")
                toast("选中路段: ${startNode?.m_uId}->${endNode?.m_uId}")

                mBinding.mapView.getLayer()?.updatePathAttr(path)
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
                Log.d(
                    "ShowMapViewActivity", "删除节点: id=${node.m_uId}, x=${node.x}, y=${node.y}"
                )
                // 可以在这里添加删除节点后的业务逻辑
                onPathDataChanged()
            }
        })

        // 编辑路线
        mBinding.btnEditPath.onClick {
            mBinding.mapView.setWorkMode(WorkMode.MODE_PATH_EDIT)
        }
        //路段属性编辑
        mBinding.btnEditPathArr.onClick {
            mBinding.mapView.setWorkMode(WorkMode.MODE_PATH_SEGMENT_ATTR_EDIT)
        }

        //节点属性编辑
        mBinding.btnEditPointArr.onClick {
            mBinding.mapView.setWorkMode(WorkMode.MODE_PATH_NODE_ATTR_EDIT)
        }

        // 合并路线
        mBinding.btnMergePath.onClick {
            mBinding.mapView.setWorkMode(WorkMode.MODE_PATH_MERGE)
        }

        // 删除路线
        mBinding.btnDeletePath.onClick {
            mBinding.mapView.setWorkMode(WorkMode.MODE_PATH_DELETE)
        }

        // 删除多条路线
        mBinding.btnDeleteMultiplePaths.onClick {
            mBinding.mapView.setWorkMode(WorkMode.MODE_PATH_DELETE_MULTIPLE)
        }

        // 曲线转直线
        mBinding.btnConvertPathToLine.onClick {
            mBinding.mapView.setWorkMode(WorkMode.MODE_PATH_CONVERT_TO_LINE)
        }

        // 创建路线
        mBinding.btnCreatePath.onClick {
            mBinding.mapView.setWorkMode(WorkMode.MODE_PATH_CREATE)
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
                        LogUtil.d("读取world_pad.dat ${readWorld}")
                        if (readWorld) mBinding.mapView.setLayer(world.cLayer)

                        withContext(Dispatchers.Main) {
                            dismissLoading()
                        }
                    } else {
                        LogUtil.d("路径数据文件不存在，将创建新的World对象")
                        world.saveWorld(getFolderPath(mapId), PAD_WORLD_NAME)
                    }
                } catch (e: NegativeArraySizeException) {
                    LogUtil.e("加载路径数据遇到NegativeArraySizeException: ${e}")
                    // 处理异常，例如创建一个新的World对象或显示错误信息
                    world.saveWorld(getFolderPath(mapId), PAD_WORLD_NAME)
                } catch (e: Exception) {
                    LogUtil.e("加载路径数据异常: ${e}")
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 保存路线到world_pad.dat文件
     */
    private fun savePathsToFile() {
        showLoading("保存中")
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
//                    // 过滤起点和终点重合的路径
//                    filterPathsWithSameStartAndEnd()

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
                        withContext(Dispatchers.Main) {
                            dismissLoading()
                        }
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
     * 过滤掉起点和终点重合的路径
     */
    private fun filterPathsWithSameStartAndEnd() {
        try {
            val pathBase = world.cLayer.m_PathBase
            if (pathBase == null || pathBase.m_pPathIdx == null || pathBase.m_uCount.toInt() == 0) {
                return
            }

            // 创建一个临时列表来存储需要保留的路径
            val validPaths = arrayListOf<PathIndex>()
            val epsilon = 1e-6f // 用于比较浮点数的小阈值

            for (i in 0 until pathBase.m_uCount) {
                val pathIndex = pathBase.m_pPathIdx[i]
                val path = pathIndex.m_ptr
                if (path == null) continue

                val startNode = path.GetStartNode()
                val endNode = path.GetEndNode()
                if (startNode == null || endNode == null) continue

                // 比较起点和终点的坐标是否重合
                val isSame =
                    Math.abs(startNode.x - endNode.x) < epsilon && Math.abs(startNode.y - endNode.y) < epsilon
                if (!isSame) {
                    validPaths.add(pathIndex)
                } else {
                    LogUtil.d("过滤掉起点和终点重合的路径: 路径ID=${path.m_uId}, 节点ID=${startNode.m_uId}")
                }
            }

            // 更新路径列表
            if (validPaths.size < pathBase.m_uCount) {
                val removedCount = pathBase.m_uCount - validPaths.size
                LogUtil.d("共过滤掉 $removedCount 条起点和终点重合的路径")

                // 创建新的路径索引数组
                pathBase.m_pPathIdx = validPaths.toTypedArray()
                pathBase.m_uCount = validPaths.size.toShort()
            }
        } catch (e: Exception) {
            LogUtil.e("过滤路径时发生异常: ${e.message}")
            e.printStackTrace()
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
            mBinding.mapView.setWorkMode(WorkMode.MODE_SP_AREA_ADD)
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
                mBinding.mapView.setWorkMode(WorkMode.MODE_SP_AREA_EDIT)
                mBinding.mapView.setSelectedSpArea(randomArea)
            }
        }

        // 设置特殊区域编辑监听器
        mBinding.mapView.setOnSpAreaEditListener(object : SpPolygonEditView.OnSpAreaEditListener {

            override fun onVertexDragEnd(area: SpArea, vertexIndex: Int, isInsideMap: Boolean) {
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
            mBinding.mapView.setWorkMode(WorkMode.MODE_MIX_AREA_ADD)
            // 创建一个新的清扫区域
            val newArea = WorkAreasNew().apply {
                name = "混行区域${mMixArea.size + 1}"
                id = "${mMixArea.size + 1}"//随机申城
                passPointsList = mutableListOf(
                    PassPoints(
                        "过渡点1",
                        gate = Gate(2.2f, 2.3f, 2.4f),
                    )
                )
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
                mBinding.mapView.setWorkMode(WorkMode.MODE_MIX_AREA_EDIT)

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

            override fun onEditPassPoint(passPoints: PassPoints?) {
                LogUtil.i("点击了过渡点   ${passPoints}")

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

            mBinding.mapView.setWorkMode(WorkMode.MODE_SHOW_MAP)
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
            mBinding.mapView.setWorkMode(WorkMode.MODE_MACHINE_STATION_EDIT)
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
            mBinding.mapView.setWorkMode(WorkMode.MODE_MACHINE_STATION_DELETE)
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
            val elevatorPoint = ElevatorPoint(
                "称梯点",
                pstPark = PstParkBean(1.1F, 1.2F, 1.3F, 0F, 0F, 0F),
                gatePoint = GatePointBean(2.1F, 2.2F, 2.3F, 0F, 0F, 0F),
                waitPoint = WaitPointBean(3.1F, 3.2F, 3.3F, 0F, 0F, 0F),
            )

            mBinding.mapView.setElevators(mutableListOf(elevatorPoint))
        }
        //编辑乘梯点
        mBinding.btnEditElevator.onClick {
            mBinding.mapView.setWorkMode(WorkMode.MODE_ELEVATOR_EDIT)
        }

        //删除乘梯点
        mBinding.btnDeleteElevator.onClick {
            mBinding.mapView.setWorkMode(WorkMode.MODE_ELEVATOR_DELETE)
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

        mBinding.btnEditAreaStartPoint.onClick {
            if (mBinding.mapView.getCleanAreaData().toMutableList().isNotEmpty()) {
                // 生成0到positingAreas.size-1之间的随机索引
                val randomIndex =
                    Random.nextInt(mBinding.mapView.getCleanAreaData().toMutableList().size)

                // 通过随机索引获取要删除的定位区域
                val randomArea = mBinding.mapView.getCleanAreaData().toMutableList()[randomIndex]
                mBinding.mapView.setWorkMode(WorkMode.EDIT_START_POINT)

                LogUtil.d("随机选择的区域为: $randomArea")
                // 将选中的区域设置到PolygonEditView中进行编辑
                mBinding.mapView.setSelectedArea(randomArea)
            }
        }
        //开始点回调舰艇
        mBinding.mapView.setOnStartPointEditListener(object :
            PolygonEditViewPoint.OnStartPointEditListener {
            override fun onStartPointDragEnd(
                area: CleanAreaNew,
            ) {
                LogUtil.d("拖动结束区域的开始点坐标为: $area")
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
                mBinding.mapView.setWorkMode(WorkMode.MODE_CLEAN_AREA_EDIT)

                // 将选中的区域设置到PolygonEditView中进行编辑
                mBinding.mapView.setSelectedArea(randomArea)
            }
        }

        // 设置清扫区域编辑监听器
        mBinding.mapView.setOnCleanAreaEditListener(object :
            PolygonEditView.OnCleanAreaEditListener {

            override fun onVertexDragEnd(
                area: CleanAreaNew, vertexIndex: Int, isInsideMap: Boolean
            ) {
                LogUtil.d("onVertexDragEnd area $area isInsideMap $isInsideMap")
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
                    LogUtil.i(
                        "编辑区域onVertexAdded  申请路径规划 ${area.toJson()}", null, TAG_PP
                    )
                }
            }

            override fun onEdgeRemoved(area: CleanAreaNew, edgeIndex: Int) {
                if (area.routeType == AreaType.AREA_AUTO) {
                    MainController.sendRoutePathCommand(CLEAN_PATH_PLAN, area)
                    LogUtil.i(
                        "编辑区域onEdgeRemoved  申请路径规划 ${area.toJson()}", null, TAG_PP
                    )
                }
            }

            override fun onVertexRemoved(area: CleanAreaNew, vertexIndex: Int) {
                LogUtil.i(
                    "编辑区域onVertexRemoved  删除了定点 $vertexIndex", null, TAG_PP
                )
                CommonWarnDialog.Builder(this@ShowMapViewActivity).setTitle("提示")
                    .setMsg("确定要删除该顶点吗？").setOnCommonWarnDialogListener(object :
                        CommonWarnDialog.Builder.CommonWarnDialogListener {
                        override fun confirm() {
                            mBinding.mapView.performDeleteVertex(area, vertexIndex)
                        }
                    }).create().show()
            }

            override fun onAreaCreated(area: CleanAreaNew) {
                // 将新创建的清扫区域添加到本地列表
                LogUtil.d("创建了新的清扫区域: ${area.sub_name}, ID: ${area.regId}")
                if (area.routeType == AreaType.AREA_AUTO) {
                    MainController.sendRoutePathCommand(CLEAN_PATH_PLAN, area)
                    LogUtil.i("创建了新的清扫区域  申请路径规划 ${area.toJson()}", null, TAG_PP)
                }
            }

            override fun onAreaDragEnd(area: CleanAreaNew, isInsideMap: Boolean) {
                LogUtil.d("onAreaDragEnd area $area isInsideMap $isInsideMap")
                if (!isInsideMap) {
                    ToastUtils.showLong("区域超出地图范围")
                }
                if (area.routeType == AreaType.AREA_AUTO) {
                    MainController.sendRoutePathCommand(CLEAN_PATH_PLAN, area)
                    LogUtil.i(
                        "编辑区域onAreaDragEnd  申请路径规划 ${area.toJson()}", null, TAG_PP
                    )
                }
            }
        })

        //添加清扫区域
        mBinding.btnAddArea.onClick {
            // 设置地图的工作模式为添加清扫区域模式
            mBinding.mapView.setWorkMode(WorkMode.MODE_CLEAN_AREA_ADD)
//            // 创建一个新的清扫区域
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

//            val points = listOf(
//                6.622922,
//                2.3767788,
//                8.178481,
//                -4.147859,
//                12.56447,
//                -4.355299,
//                14.053481,
//                -3.3443854,
//                14.37065,
//                0.22211342,
//                8.566684,
//                2.7157185
//            )
//
//            val area = CleanAreaNew().apply {
//                sub_name = "新区域"
//                regId = System.currentTimeMillis().toInt()
//            }
//
//            mBinding.mapView.setSmartCleanAreaData(points, area)
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
            mBinding.mapView.setWorkMode(WorkMode.MODE_POSITING_AREA_ADD)
        }

        // 设置定位区域创建监听器
        mBinding.mapView.setOnPositingAreaCreatedListener(object :
            PostingAreasView.OnPositingAreaCreatedListener {
            override fun onPositingAreaCreated(area: PositingArea) {
                // 切换回移动模式
                mBinding.mapView.setWorkMode(WorkMode.MODE_SHOW_MAP)
            }
        })

        //编辑定位区域
        mBinding.btnPostingAreaEdit.setOnClickListener {
            // 随机选择一个定位区域高亮显示
            if (mBinding.mapView.getPositingAreas().toMutableList().isNotEmpty()) {
                mBinding.mapView.setWorkMode(WorkMode.MODE_POSITING_AREA_EDIT)

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
     * 反光板
     */
    private fun initReflector() {
        // 设置初始数据
        mBinding.mapView.setReflectorMap(mReflectorMaps)

        // 添加反光板
        mBinding.btnAddReflector.setOnClickListener {
            mBinding.mapView.setWorkMode(WorkMode.WORK_MODE_ADD_REFLECTOR_AREA)
            ToastUtils.showLong("已进入添加反光板模式，滑动屏幕添加")
        }

        // 编辑反光板
        mBinding.btnEditReflector.setOnClickListener {
            mBinding.mapView.setWorkMode(WorkMode.WORK_MODE_EDIT_REFLECTOR)
            ToastUtils.showLong("已进入编辑反光板模式，拖动反光板边缘调整大小")
        }

        // 保存反光板
        mBinding.btnSaveReflector.setOnClickListener {
            val list = mBinding.mapView.getReflectorMap()
            mReflectorMaps.clear()
            mReflectorMaps.addAll(list)
            // 这里可以添加保存逻辑，例如发送给后台或保存到本地
            ToastUtils.showLong("保存成功，当前反光板数量：${list.size}")
            // 退出编辑模式
            mBinding.mapView.setWorkMode(WorkMode.MODE_SHOW_MAP)
        }
    }

    /**
     * 删除噪点
     */
    private fun initRemoveNoise() {
//        var mScle = 0f
//        var mX = 0f
//        var mY = 0f
        mBinding.mapView.set3D(true)
        //删除噪点
        mBinding.btnRemoveNoise.setOnClickListener {
            mBinding.mapView.setWorkMode(WorkMode.MODE_REMOVE_NOISE)
        }

        mBinding.btnViewConfig.setOnClickListener {
            LogUtil.d("加载地图mMapScale ${mBinding.mapView.mMapScale}")
            LogUtil.d("加载地图mMapCenterX ${mBinding.mapView.mMapCenterX}")
            LogUtil.d("加载地图mMapCenterY${mBinding.mapView.mMapCenterY}")
            //加载地图
            mBinding.mapView.reloadMap(
                ConstantBase.getFilePath(mapId, ConstantBase.PAD_MAP_NAME_PNG),
                ConstantBase.getFilePath(mapId, ConstantBase.PAD_MAP_NAME_YAML),
//                mScle,
//                mX,
//                mY
            )
        }

        //删除噪点
        mBinding.btnRemoveNoiseSure.setOnClickListener {
            // 获取所有去除噪点区域
            val removeNoiseRects = mBinding.mapView.getRemoveNoiseRects()
            LogUtil.d("获取所有去除噪点区域: $removeNoiseRects")
            MainController.send3DRemoveNoise(
                rectFs = removeNoiseRects,
                mapId = mapId,
                maxHigh = 20f,
            )
        }

        mBinding.mapView.setSingleTapListener(object : ISingleTapListener {
            override fun onSingleTapListener(
                mMapScale: Float, point: PointF
            ) {
//                mScle = mMapScale
//                mX = point.x
//                mY = point.y
                LogUtil.d("缩放级别:${mMapScale} 世界坐标 $point")
            }
        })

//        // 设置去除噪点监听器
//        mBinding.mapView.setOnRemoveNoiseListener(object : MapView.IRemoveNoiseListener {
//            override fun onRemoveNoise(leftTop: PointF, rightBottom: PointF) {
////                // 处理噪点区域信息，这里可以添加日志或者发送到控制器
////                LogUtil.d("去除噪点区域: 左上角(${leftTop.x}, ${leftTop.y}), 右下角(${rightBottom.x}, ${rightBottom.y})")
////                MainController.sendEraseEvPoint(leftTop, rightBottom, mapId)
////                // 不再立即清除绘制，支持显示多个噪点区域
////                // mBinding.mapView.clearRemoveNoiseDrawing()
////                ToastUtils.showLong("已发送去除噪点指令")
//            }
//
//            override fun onRemoveNoiseDeleted(rect: RectF) {
//                LogUtil.d("删除了噪点区域: $rect")
//                ToastUtils.showLong("已删除选中的噪点区域")
//                // 如果有对应的撤销指令，可以在这里发送
//            }
//        })
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
            XpopUtils(this).showCmsStationDialog(
                onConfirmCall = { result ->
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
            mBinding.mapView.setWorkMode(WorkMode.MODE_CMS_STATION_EDIT)
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
            mBinding.mapView.setWorkMode(WorkMode.MODE_CMS_STATION_DELETE)
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
            mBinding.mapView.setWorkMode(WorkMode.MODE_VIRTUAL_WALL_ADD)
            // 默认创建普通虚拟墙
            mBinding.mapView.addVirtualWall(3)
        }
        //编辑虚拟墙
        mBinding.btnVirEdit.setOnClickListener {
            // 编辑虚拟墙模式
            mBinding.mapView.setWorkMode(WorkMode.MODE_VIRTUAL_WALL_EDIT)

        }
        //删除虚拟墙
        mBinding.btnVirDel.setOnClickListener {
            // 删除虚拟墙模式
            mBinding.mapView.setWorkMode(WorkMode.MODE_VIRTUAL_WALL_DELETE)
        }
        //编辑虚拟墙类型
        mBinding.btnVirTypeEdit.setOnClickListener {
            // 编辑虚拟墙类型模式
            mBinding.mapView.setWorkMode(WorkMode.MODE_VIRTUAL_WALL_TYPE_EDIT)
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

    private fun initRFId() {
        mBinding.btnSaveRfid.onClick {
            val rfId1 = RFID(tag_x = 1.1F, tag_y = 2.2F, area = 1, channel = 2, tag_index = 3)
            val rfId2 = RFID(tag_x = 2.1F, tag_y = 2.2F, area = 1, channel = 2, tag_index = 3)
            val rfId3 = RFID(tag_x = 3.1F, tag_y = 2.2F, area = 1, channel = 2, tag_index = 3)
            mBinding.mapView.setRFId(mutableListOf(rfId1, rfId2, rfId3))
        }

        mBinding.btnEditRfid.onClick {
            mBinding.mapView.setWorkMode(WorkMode.WORK_MODE_EDIT_RF_ID)
        }

        mBinding.mapView.setOnRFIdClickListener(object : RFIDView.OnRFIdClickListener {
            override fun onRFIdClick(rfId: RFID) {
                LogUtil.i("点击的RFId = ${rfId}")
            }

        })
    }

    private fun initSameSwitch() {
        mBinding.btnAddSame.onClick {
            val same1 = SameSwitchBean(
                point_id = "1",
                point_name = "同层切换1",
                coordinate = StationCoordinate(1.1F, 2.2F, 0F)
            )
            mBinding.mapView.setSameSwitchStations(mutableListOf(same1))
        }

        mBinding.btnEditSame.onClick {
            mBinding.mapView.setWorkMode(WorkMode.WORK_MODE_SAME_SWITCH_EDIT)
        }

        mBinding.mapView.setOnSameClickListener(object : SameSwitchView.OnStationClickListener {
            override fun onStationClick(station: SameSwitchBean) {
                LogUtil.i("点击的station = ${station}")
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
        val rcData = RCData()

        //上激光点云
        LiveEventBus.get<laser_t>(KEY_CURRENT_POINT_CLOUD).observe(this) {
            rcData.f_create_map_data = it.ranges
            mDragBean.upRCData = rcData
            mBinding.mapView.setUpLaserScan(it)
        }

        //下激光点云
        LiveEventBus.get<laser_t>(KEY_BOTTOM_CURRENT_POINT_CLOUD).observe(this) {
            mBinding.mapView.setDownLaserScan(it)
        }

        //接收车体坐标 AGV->PAD
        LiveEventBus.get<robot_control_t>(KEY_AGV_COORDINATE).observe(this) {
            mBinding.mapView.setAgvPose(it)
            mBinding.mapView.setWorkingPath(it.dparams)

            //有任务才显示车体位置
//            if (RunningState.CURRENT_TASK_STATE == TaskState.HAVE_TASK) {
//                mBinding.mapView.setWorkingPath(it.dparams)
//            }
        }

        //接收导航定位区域
        LiveEventBus.get<String>(KEY_POSITING_AREA_VALUE).observe(this) {
            val positingAreas = GsonUtil.jsonToList(it, PositingArea::class.java)
            mBinding.mapView.setPositingAreas(positingAreas)
        }

//        //接收路径规划结果
//        LiveEventBus.get<PlanPathResult>(KEY_UPDATE_PLAN_PATH_RESULT).observe(this) { result ->
//            try {
//                // 检查路径段数量是否为0
//                if (result.m_iPathSum == 0) {
//                    LogUtil.i(
//                        "展示路径规划失败弹窗, 路径类型:${result.m_iPathPlanType}, 路段个数:${result.m_iPathSum}",
//                        null,
//                        TAG_PP
//                    )
//
//                    return@observe
//                }
//
//                // 检查路径点数量是否过大，防止内存溢出
//                if (result.m_fElementBuffer.size > PathPlanningUtil.MAX_POINT_COUNT) {
//                    LogUtil.e(
//                        "路径点数量过大，可能导致内存溢出: ${result.m_fElementBuffer.size}",
//                        null,
//                        TAG_PP
//                    )
//                    return@observe
//                }
//
//                // 检查当前内存状态，防止内存溢出
//                val memoryInfo = ActivityManager.MemoryInfo()
//                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
//                activityManager.getMemoryInfo(memoryInfo)
//                if (memoryInfo.lowMemory) {
//                    // 内存不足时，清理旧路径和对象池
//                    LogUtil.w("系统内存不足，清理旧路径和对象池", null, TAG_PP)
//                    mBinding.mapView.clearPathPlan() // 假设MapView有清理路径的方法
//                    PathPlanningUtil.clearObjectPools() // 清理对象池
//                }
//
//                if (result.m_iPathPlanType == CLEAN_PATH_PLAN) {
//                    // 接收Pad申请的清扫路径规划结果
//                    if (result.m_strTo == "pad") {
//                        // 路段模式
//                        if (result.m_iPlanResultMode == PATH_MODE) {
//                            LogUtil.i(
//                                "AutoGeneratePathActivity接收清扫路径规划", null, TAG_PP
//                            )
//
//                            // 清除旧的清扫路径
//                            mBinding.mapView.clearCleanPathPlan()
//
//                            val cleanPathPlanResultBean =
//                                PathPlanningUtil.getPathPlanResultBean(result, mBinding.mapView)
//                            if (cleanPathPlanResultBean.m_bIsPlanOk) {
//                                mBinding.mapView.setCleanPathPlanResultBean(
//                                    cleanPathPlanResultBean
//                                )
//                            } else {
//                                LogUtil.e("清扫路径规划解析失败", null, TAG_PP)
//                            }
//                        }
//                    }
//                }
//                // 接收CMS申请的全局路径规划结果
//                if (result.m_iPathPlanType == GLOBAL_PATH_PLAN) {
//                    if (result.m_strTo == "CMS") {
//                        // 路段模式
//                        if (result.m_iPlanResultMode == PATH_MODE) {
//                            LogUtil.i(
//                                "CleanAutoActivity接收全局路径规划", null, TAG_PP
//                            )
//
//                            // 清除旧的全局路径
//                            mBinding.mapView.clearGlobalPathPlan()
//
//                            val globalPathPlanResultBean =
//                                PathPlanningUtil.getPathPlanResultBean(result)
//                            if (globalPathPlanResultBean.m_bIsPlanOk) {
//                                mBinding.mapView.setGlobalPathPlanResultBean(
//                                    globalPathPlanResultBean
//                                )
//                            } else {
//                                LogUtil.e("全局路径规划解析失败", null, TAG_PP)
//                            }
//                        }
//                    }
//                }
//            } catch (e: Exception) {
//                LogUtil.e(
//                    "接收路径规划异常: ${e.message}", null, TAG_PP
//                )
//                e.printStackTrace()
//                // 异常处理：清理可能已创建的资源
////                mBinding.mapView.clearPathPlan() // 清理所有路径
//                PathPlanningUtil.clearObjectPools() // 清理对象池
//            }
//        }
    }

    /**
     * 显示过门信息对话框
     */
    private fun showCrossDoorDialog(crossDoor: com.siasun.dianshi.bean.CrossDoor) {
        android.app.AlertDialog.Builder(this).setTitle("过门信息").setMessage(
            "ID: ${crossDoor.id}\n" + "地图ID: ${crossDoor.map_id}\n" + "类型: ${crossDoor.door_msg.type}\n" + "起点: (${
                String.format(
                    "%.2f", crossDoor.start_point.x
                )
            }, ${String.format("%.2f", crossDoor.start_point.y)})\n" + "终点: (${
                String.format(
                    "%.2f", crossDoor.end_point.x
                )
            }, ${String.format("%.2f", crossDoor.end_point.y)})"
        ).setPositiveButton("确定", null).setNegativeButton("删除") { _, _ ->
            // 删除选中的过门
            mBinding.mapView.mCrossView?.removeCrossDoor(crossDoor)
//            ToastUtils.showLong("已删除过门: ${crossDoor.door_msg.door_sn}")
            LogUtil.d("999 点击了过门线 ${mBinding.mapView.getCrossDoors()}")

        }.show()
    }
}