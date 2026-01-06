package com.siasun.dianshi.mapviewdemo.ui

import android.app.ActivityManager
import android.content.Context
import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
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
import com.siasun.dianshi.bean.SpArea
import com.siasun.dianshi.bean.TeachPoint
import com.siasun.dianshi.bean.WorkAreasNew
import com.siasun.dianshi.bean.pp.world.PathIndex
import com.siasun.dianshi.controller.MainController
import com.siasun.dianshi.framework.ext.onClick
import com.siasun.dianshi.framework.ext.toBean
import com.siasun.dianshi.framework.ext.toJson
import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.mapviewdemo.CLEAN_PATH_PLAN
import com.siasun.dianshi.mapviewdemo.GLOBAL_PATH_PLAN
import com.siasun.dianshi.mapviewdemo.KEY_AGV_COORDINATE
import com.siasun.dianshi.mapviewdemo.KEY_BOTTOM_CURRENT_POINT_CLOUD
import com.siasun.dianshi.mapviewdemo.KEY_CURRENT_POINT_CLOUD
import com.siasun.dianshi.mapviewdemo.KEY_POSITING_AREA_VALUE
import com.siasun.dianshi.mapviewdemo.KEY_TEACH_PATH
import com.siasun.dianshi.mapviewdemo.KEY_UPDATE_PLAN_PATH_RESULT
import com.siasun.dianshi.mapviewdemo.PATH_MODE
import com.siasun.dianshi.mapviewdemo.TAG_PP
import com.siasun.dianshi.mapviewdemo.TEACH_PATH_PLAN
import com.siasun.dianshi.mapviewdemo.databinding.ActivityShowMapViewBinding
import com.siasun.dianshi.mapviewdemo.utils.GsonUtil
import com.siasun.dianshi.mapviewdemo.utils.PathPlanningUtil
import com.siasun.dianshi.mapviewdemo.utils.PathPlanningUtil1
import com.siasun.dianshi.mapviewdemo.viewmodel.ShowMapViewModel
import com.siasun.dianshi.utils.World
import com.siasun.dianshi.view.HomeDockView
import com.siasun.dianshi.view.MapView
import com.siasun.dianshi.view.MixAreaView
import com.siasun.dianshi.view.PolygonEditView
import com.siasun.dianshi.view.PostingAreasView
import com.siasun.dianshi.view.SpPolygonEditView
import com.siasun.dianshi.view.VirtualWallView
import com.siasun.dianshi.xpop.XpopUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import java.io.File
import kotlin.random.Random

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

        mBinding.btnAddTeachPath.onClick {
//            val json ="{\"dparams\":[],\"fparams\":[],\"iparams\":[],\"lparams\":[],\"m_cPathTypeBuffer\":[1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],\"m_fCleanPathPlanStartPosBuffer\":[25.439,-0.217,3.1620728E35],\"m_fElementBuffer\":[8.249333,25.276001,8.923692,25.303238,9.59374,25.286446,10.2694235,25.318441,10.2694235,25.318441,10.9451065,25.350437,11.630662,25.32176,12.30613,25.358152,12.30613,25.358152,12.981599,25.394547,13.649783,25.41992,14.325399,25.444454,14.325399,25.444454,15.001016,25.46899,15.679217,25.501083,16.357433,25.497608,16.357433,25.497608,17.03565,25.494135,17.718708,25.487988,18.396828,25.496233,18.396828,25.496233,19.07495,25.504478,19.750368,25.536098,20.42778,25.530249,20.42778,25.530249,21.105192,25.524399,21.786858,25.556099,22.461946,25.576025,22.461946,25.576025,23.137033,25.595953,23.811262,25.608814,24.48248,25.62012,24.48248,25.62012,25.153694,25.631424,25.816166,25.651073,26.48547,25.654915,26.48547,25.654915,27.154772,25.658758,27.817812,25.684868,28.489975,25.703259,28.489975,25.703259,29.162136,25.721647,29.847887,25.70055,30.52128,25.679981,30.52128,25.679981,31.194674,25.659412,31.868809,25.713581,32.539093,25.744427,32.539093,25.744427,33.209377,25.77527,33.89059,25.808552,34.556793,25.790512,34.556793,25.790512,35.222996,25.772472,35.93456,25.814667,36.578056,25.789368,36.578056,25.789368,37.221554,25.76407,38.024757,25.893396,38.585903,25.753487,38.585903,25.753487,39.147045,25.613577,40.16159,24.966307,40.267845,25.888182,40.267845,25.888182,40.3741,26.810059,39.209335,26.271946,38.664886,26.159811,38.664886,26.159811,38.120438,26.047676,37.315235,26.082268,36.679295,26.087416,36.679295,26.087416,36.04335,26.092564,35.34149,26.144768,34.682037,26.135273,34.682037,26.135273,34.022583,26.125778,33.348423,26.116991,32.6819,26.097761,32.6819,26.097761,32.01538,26.078531,31.343695,26.096624,30.672157,26.07961,30.672157,26.07961,30.000622,26.062597,29.321594,26.077816,28.64706,26.044224,28.64706,26.044224,27.972525,26.010632,27.291197,26.005909,26.616676,26.034624,26.616676,26.034624,25.942156,26.063341,25.272095,26.044626,24.596756,26.043123,24.596756,26.043123,23.921417,26.041618,23.23764,26.00162,22.563282,25.995903,22.563282,25.995903,21.888924,25.990187,21.220427,25.95364,20.54948,25.939045,20.54948,25.939045,19.878534,25.92445,19.213173,25.926922,18.541761,25.929474,18.541761,25.929474,17.87035,25.932024,17.196077,25.860195,16.524778,25.848413,16.524778,25.848413,15.85348,25.836634,15.189989,25.836359,14.518246,25.824003,14.518246,25.824003,13.846503,25.811647,13.192364,25.80264,12.512938,25.811275,12.512938,25.811275,11.833512,25.819908,11.211849,25.770672,10.504762,25.743578,10.504762,25.743578,9.797673,25.716482,9.250721,25.80671,8.472111,25.886366,8.472111,25.886366,7.6935,25.96602,7.5514383,24.608309,8.365087,24.70646,8.365087,24.70646,9.178735,24.804613,9.684663,24.84203,10.397864,24.838251,10.397864,24.838251,11.111066,24.834473,11.733423,24.92469,12.4181,24.902672,12.4181,24.902672,13.102778,24.880653,13.7655945,24.911427,14.441023,24.9303,14.441023,24.9303,15.116451,24.949173,15.772562,24.978827,16.44743,24.969488,16.44743,24.969488,17.122295,24.96015,17.805563,25.044138,18.482454,25.0178,18.482454,25.0178,19.159344,24.991463,19.832304,25.038403,20.50636,25.082832,20.50636,25.082832,21.180414,25.127262,21.863604,25.091661,22.536158,25.093225,22.536158,25.093225,23.208712,25.09479,23.866919,25.119944,24.539324,25.114492,24.539324,25.114492,25.21173,25.10904,25.890139,25.14869,26.565096,25.149519,26.565096,25.149519,27.240055,25.150349,27.913227,25.194122,28.588696,25.21285,28.588696,25.21285,29.264164,25.23158,29.936686,25.2167,30.614765,25.233763,30.614765,25.233763,31.292845,25.250824,31.964996,25.256907,32.64851,25.284145,32.64851,25.284145,33.332024,25.311382,33.99455,25.322626,34.69194,25.318373,34.69194,25.318373,35.38933,25.31412,35.992325,25.286716,36.750965,25.372177,36.750965,25.372177,37.509605,25.457638,38.506454,25.625359,39.317,25.439],\"m_fGloalPathPlanGoalPosBuffer\":[1.3759809E-38,0.0,0.0],\"m_fGloalPathPlanStartPosBuffer\":[0.4,1.5755796E-38,0.0],\"m_fRegionPointsBuffer\":[],\"m_iAddLaser\":0,\"m_iCleanPathPanType\":0,\"m_iElementSum\":384,\"m_iGloalPathPlanType\":0,\"m_iPathPlanPublicId\":2,\"m_iPathPlanPublicSubId\":0,\"m_iPathPlanRegionChoose\":53,\"m_iPathPlanType\":3,\"m_iPathSum\":48,\"m_iPlanResult\":1,\"m_iPlanResultMode\":0,\"m_iRegionNumber\":1109214364,\"m_iRegionPoints\":0,\"m_strAdditionInfo\":\"1.0.1.193\",\"m_strFrom\":\"PathPlan\",\"m_strTo\":\"pad\",\"m_uLayerNumber\":12844,\"ndparams\":0,\"nfparams\":0,\"niparams\":0,\"nlparams\":0,\"sparams\":\"\",\"utime\":0}"
//            val json ="{\"dparams\":[],\"fparams\":[],\"iparams\":[],\"lparams\":[],\"m_cPathTypeBuffer\":[1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],\"m_fCleanPathPlanStartPosBuffer\":[5.286,-0.009,3.1620728E35],\"m_fElementBuffer\":[8.376,5.269,9.052838,5.276127,9.735463,5.2419443,10.411838,5.279603,10.411838,5.279603,11.088211,5.317261,11.764164,5.343837,12.4395485,5.339864,12.4395485,5.339864,13.114933,5.3358903,13.787697,5.330263,14.461338,5.339382,14.461338,5.339382,15.134979,5.348501,15.809291,5.366262,16.481258,5.3805075,16.481258,5.3805075,17.153227,5.3947535,17.818878,5.3949447,18.491167,5.405807,18.491167,5.405807,19.163456,5.41667,19.841251,5.4500256,20.514572,5.4430795,20.514572,5.4430795,21.187893,5.436134,21.859995,5.450444,22.53188,5.478367,22.53188,5.478367,23.203764,5.50629,23.874968,5.52005,24.54712,5.5205603,24.54712,5.5205603,25.219269,5.5210705,25.896204,5.5235777,26.566658,5.5316567,26.566658,5.5316567,27.23711,5.5397353,27.900465,5.5099506,28.569067,5.535492,28.569067,5.535492,29.23767,5.5610332,29.90712,5.573303,30.576048,5.5988555,30.576048,5.5988555,31.244978,5.624408,31.92055,5.6250944,32.58731,5.6274443,32.58731,5.6274443,33.254074,5.629794,33.932755,5.6713533,34.590187,5.7026925,34.590187,5.7026925,35.247623,5.7340317,35.97141,5.725162,36.60018,5.712918,36.60018,5.712918,37.22895,5.7006736,38.060036,5.80508,38.579395,5.697625,38.579395,5.697625,39.09876,5.590171,40.298515,4.9978185,40.135834,5.7645416,40.135834,5.7645416,39.973152,6.5312643,38.8615,6.000282,38.28926,6.0468855,38.28926,6.0468855,37.71702,6.0934896,36.933033,5.9447503,36.288216,6.0254703,36.288216,6.0254703,35.643402,6.1061897,34.932304,6.1068234,34.268562,6.1110964,34.268562,6.1110964,33.60482,6.115369,32.935272,6.070629,32.266857,6.0729227,32.266857,6.0729227,31.598442,6.075217,30.922983,6.023684,30.251406,6.0120735,30.251406,6.0120735,29.57983,6.000463,28.901402,5.9902906,28.230501,5.9949327,28.230501,5.9949327,27.559603,5.9995747,26.8966,5.9766574,26.227045,5.956712,26.227045,5.956712,25.557487,5.936766,24.887749,5.9238753,24.216196,5.9126396,24.216196,5.9126396,23.544641,5.9014034,22.86917,5.9206457,22.196436,5.9072685,22.196436,5.9072685,21.5237,5.8938913,20.853062,5.8676333,20.180191,5.836571,20.180191,5.836571,19.50732,5.8055086,18.831089,5.8047986,18.156143,5.8069053,18.156143,5.8069053,17.4812,5.8090115,16.797672,5.795639,16.125566,5.7655244,16.125566,5.7655244,15.45346,5.7354097,14.779823,5.747781,14.112914,5.748028,14.112914,5.748028,13.446005,5.748275,12.761993,5.762222,12.10686,5.721673,12.10686,5.721673,11.451726,5.6811237,10.729786,5.7729936,10.111622,5.6605406,10.111622,5.6605406,9.493459,5.548088,8.590731,6.1170473,8.256349,5.34853,8.256349,5.34853,7.921966,4.5800123,9.137574,4.603643,9.712716,4.739748,9.712716,4.739748,10.287858,4.875853,11.058854,4.736226,11.70582,4.780468,11.70582,4.780468,12.352785,4.8247104,13.063596,4.822666,13.730262,4.8259797,13.730262,4.8259797,14.396928,4.829293,15.074096,4.878851,15.742485,4.884395,15.742485,4.884395,16.410873,4.8899393,17.075933,4.86804,17.74618,4.8900313,17.74618,4.8900313,18.416426,4.9120226,19.09241,4.9338465,19.765797,4.935681,19.765797,4.935681,20.439186,4.937515,21.122297,4.9309053,21.793554,4.9674473,21.793554,4.9674473,22.464813,5.003989,23.128086,4.9752026,23.797413,5.0012817,23.797413,5.0012817,24.466742,5.0273604,25.133162,5.009925,25.8043,5.040123,25.8043,5.040123,26.475441,5.070321,27.153925,5.0701838,27.826101,5.068209,27.826101,5.068209,28.498278,5.0662346,29.158777,5.0819592,29.831415,5.106915,29.831415,5.106915,30.504053,5.1318707,31.153864,5.1297526,31.838196,5.131048,31.838196,5.131048,32.522526,5.1323442,33.143555,5.185711,33.866016,5.186148,33.866016,5.186148,34.588478,5.186585,35.04707,5.2434597,35.955185,5.2176957,35.955185,5.2176957,36.8633,5.1919312,38.310753,5.312164,39.338,5.286],\"m_fGloalPathPlanGoalPosBuffer\":[1.3759809E-38,0.0,0.0],\"m_fGloalPathPlanStartPosBuffer\":[0.4,1.5755796E-38,0.0],\"m_fRegionPointsBuffer\":[],\"m_iAddLaser\":0,\"m_iCleanPathPanType\":0,\"m_iElementSum\":376,\"m_iGloalPathPlanType\":0,\"m_iPathPlanPublicId\":11,\"m_iPathPlanPublicSubId\":0,\"m_iPathPlanRegionChoose\":54,\"m_iPathPlanType\":3,\"m_iPathSum\":47,\"m_iPlanResult\":1,\"m_iPlanResultMode\":0,\"m_iRegionNumber\":1109219869,\"m_iRegionPoints\":0,\"m_strAdditionInfo\":\"1.0.1.193\",\"m_strFrom\":\"PathPlan\",\"m_strTo\":\"pad\",\"m_uLayerNumber\":12846,\"ndparams\":0,\"nfparams\":0,\"niparams\":0,\"nlparams\":0,\"sparams\":\"\",\"utime\":0}"
            val json ="{\"dparams\":[],\"fparams\":[],\"iparams\":[],\"lparams\":[],\"m_cPathTypeBuffer\":[1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],\"m_fCleanPathPlanStartPosBuffer\":[34.448,-0.197,3.1620728E35],\"m_fElementBuffer\":[-31.955,34.43,-31.278906,34.422024,-30.594679,34.469955,-29.919924,34.45202,-29.919924,34.45202,-29.245169,34.434086,-28.579124,34.471844,-27.906832,34.47458,-27.906832,34.47458,-27.234537,34.47731,-26.564053,34.53683,-25.891386,34.52824,-25.891386,34.52824,-25.218718,34.519653,-24.550268,34.5301,-23.87606,34.53009,-23.87606,34.53009,-23.20185,34.53008,-22.516884,34.5698,-21.84334,34.549236,-21.84334,34.549236,-21.169796,34.52867,-20.50532,34.558456,-19.834003,34.586823,-19.834003,34.586823,-19.162685,34.61519,-18.489586,34.63785,-17.816969,34.64818,-17.816969,34.64818,-17.144354,34.658504,-16.4721,34.652386,-15.799479,34.668816,-15.799479,34.668816,-15.126858,34.685246,-14.456918,34.68993,-13.7825985,34.69998,-13.7825985,34.69998,-13.108279,34.710033,-12.426639,34.723377,-11.751746,34.727657,-11.751746,34.727657,-11.076855,34.731934,-10.404874,34.740055,-9.730667,34.749786,-9.730667,34.749786,-9.05646,34.759518,-8.380449,34.74591,-7.706836,34.763878,-7.706836,34.763878,-7.0332227,34.78185,-6.363293,34.769028,-5.689811,34.802906,-5.689811,34.802906,-5.016329,34.83679,-4.339955,34.835125,-3.6656747,34.84416,-3.6656747,34.84416,-2.9913948,34.85319,-2.3276303,34.870556,-1.6499879,34.87496,-1.6499879,34.87496,-0.9723454,34.879368,-0.31316784,34.92984,0.37607393,34.90072,0.37607393,34.90072,1.0653157,34.871597,1.6856124,34.861023,2.2258222,34.2428,2.2258222,34.2428,2.7660317,33.624577,3.7847478,34.649666,3.0058618,34.967983,3.0058618,34.967983,2.2269757,35.286297,1.6734341,35.00544,0.9723597,35.07043,0.9723597,35.07043,0.27128538,35.13542,-0.36130044,35.066784,-1.0438424,35.048676,-1.0438424,35.048676,-1.7263844,35.030567,-2.4047701,35.02564,-3.0844827,35.00956,-3.0844827,35.00956,-3.764195,34.99348,-4.443157,35.02492,-5.117815,35.028057,-5.117815,35.028057,-5.792473,35.031193,-6.454396,34.97311,-7.1252875,34.9599,-7.1252875,34.9599,-7.79618,34.946686,-8.466016,34.959904,-9.137725,34.9542,-9.137725,34.9542,-9.809433,34.948498,-10.480868,34.931263,-11.154192,34.93813,-11.154192,34.93813,-11.827517,34.945,-12.505061,34.907715,-13.17865,34.915314,-13.17865,34.915314,-13.852239,34.922913,-14.5198145,34.87682,-15.193791,34.876007,-15.193791,34.876007,-15.867768,34.87519,-16.544344,34.855637,-17.220964,34.856747,-17.220964,34.856747,-17.897585,34.857853,-18.581085,34.82262,-19.257235,34.828075,-19.257235,34.828075,-19.933386,34.83353,-20.607124,34.809807,-21.27903,34.78637,-21.27903,34.78637,-21.950933,34.762928,-22.611887,34.78522,-23.284668,34.773224,-23.284668,34.773224,-23.957449,34.761227,-24.637884,34.724945,-25.311426,34.718132,-25.311426,34.718132,-25.984966,34.71132,-26.647076,34.676773,-27.321198,34.684643,-27.321198,34.684643,-27.995317,34.692516,-28.636858,34.55458,-29.325064,34.621063,-29.325064,34.621063,-30.01327,34.687546,-30.65203,34.476112,-31.30561,34.9859,-31.30561,34.9859,-31.95919,35.49569,-32.748844,34.257347,-31.96413,34.072998,-31.96413,34.072998,-31.179417,33.88865,-30.675285,34.381954,-29.964571,34.336952,-29.964571,34.336952,-29.253859,34.29195,-28.624887,34.3091,-27.942507,34.283,-27.942507,34.283,-27.260124,34.2569,-26.60173,34.316383,-25.924212,34.30835,-25.924212,34.30835,-25.246693,34.300312,-24.56632,34.324364,-23.890491,34.34698,-23.890491,34.34698,-23.214663,34.369595,-22.538067,34.334644,-21.86569,34.356773,-21.86569,34.356773,-21.193314,34.378902,-20.527546,34.331802,-19.857868,34.337166,-19.857868,34.337166,-19.18819,34.34253,-18.527325,34.384922,-17.85504,34.378315,-17.85504,34.378315,-17.182755,34.371704,-16.497553,34.383648,-15.82424,34.407253,-15.82424,34.407253,-15.150927,34.430862,-14.480206,34.412914,-13.809354,34.401257,-13.809354,34.401257,-13.138502,34.389595,-12.472526,34.4242,-11.802472,34.4292,-11.802472,34.4292,-11.13242,34.4342,-10.465532,34.433132,-9.793649,34.429813,-9.793649,34.429813,-9.121766,34.4265,-8.442716,34.437645,-7.7707143,34.46152,-7.7707143,34.46152,-7.0987134,34.4854,-6.435213,34.469173,-5.7623386,34.45935,-5.7623386,34.45935,-5.089464,34.44953,-4.410416,34.486755,-3.7360988,34.48007,-3.7360988,34.48007,-3.0617812,34.47338,-2.4005065,34.527573,-1.7231878,34.5001,-1.7231878,34.5001,-1.045869,34.472626,-0.42437974,34.801918,0.25163072,34.782784,0.25163072,34.782784,0.9276412,34.76365,1.5979455,34.60556,2.254,34.448],\"m_fGloalPathPlanGoalPosBuffer\":[1.3759809E-38,0.0,0.0],\"m_fGloalPathPlanStartPosBuffer\":[0.4,1.5755796E-38,0.0],\"m_fRegionPointsBuffer\":[],\"m_iAddLaser\":0,\"m_iCleanPathPanType\":0,\"m_iElementSum\":424,\"m_iGloalPathPlanType\":0,\"m_iPathPlanPublicId\":19,\"m_iPathPlanPublicSubId\":0,\"m_iPathPlanRegionChoose\":52,\"m_iPathPlanType\":3,\"m_iPathSum\":53,\"m_iPlanResult\":1,\"m_iPlanResultMode\":0,\"m_iRegionNumber\":1074807177,\"m_iRegionPoints\":0,\"m_strAdditionInfo\":\"1.0.1.193\",\"m_strFrom\":\"PathPlan\",\"m_strTo\":\"pad\",\"m_uLayerNumber\":13100,\"ndparams\":0,\"nfparams\":0,\"niparams\":0,\"nlparams\":0,\"sparams\":\"\",\"utime\":0}"

            val toBean1 = json.toBean<PlanPathResult>()
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

        //保存
        mBinding.btnSaveTeach.onClick {
            savePathsToFile()
            ToastUtils.showLong("保存试教")
        }


        //开始试教
        mBinding.btnStartTeach.onClick {
            MainController.sendStartTeachRoute()
            ToastUtils.showLong("开始试教")
        }
        //结束试教
        mBinding.btnEndTeach.onClick {
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
            mBinding.mapView.setWorkMode(MapView.WorkMode.MODE_SHOW_MAP)
        }
//
//        initMergedPose()
//        initStation()
//        iniVirtualWall()
//        initRemoveNoise()
//        initPostingArea()
//        initCleanArea()
//        initElevator()
//        initPose()
//        initMachineStation()
//        initMixArea()
//        initSpAreas()
        initPath()

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
                "CleanAutoActivity接收全局路径规划 ${pathPlanResultBean.toJson()}",
                null,
                TAG_PP
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
                        LogUtil.d("读取world_pad.dat ${readWorld}")
                        if (readWorld) mBinding.mapView.setLayer(world.cLayer)

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
        mBinding.mapView.setOnSpAreaEditListener(object :
            SpPolygonEditView.OnSpAreaEditListener {

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
                val randomArea =
                    mBinding.mapView.getCleanAreaData().toMutableList()[randomIndex]

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
                    LogUtil.i(
                        "编辑区域onVertexAdded  申请路径规划 ${area.toJson()}",
                        null,
                        TAG_PP
                    )
                }
            }

            override fun onEdgeRemoved(area: CleanAreaNew, edgeIndex: Int) {
                if (area.routeType == AreaType.AREA_AUTO) {
                    MainController.sendRoutePathCommand(CLEAN_PATH_PLAN, area)
                    LogUtil.i(
                        "编辑区域onEdgeRemoved  申请路径规划 ${area.toJson()}",
                        null,
                        TAG_PP
                    )
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
                val randomArea =
                    mBinding.mapView.getPositingAreas().toMutableList()[randomIndex]

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
                val randomArea =
                    mBinding.mapView.getPositingAreas().toMutableList()[randomIndex]

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
                    "ShowMapViewActivity",
                    "Virtual wall clicked: index=$lineIndex, config=$config"
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
                val activityManager =
                    getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
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
                                mBinding.mapView.setCleanPathPlanResultBean(
                                    cleanPathPlanResultBean
                                )
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