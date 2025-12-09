package com.siasun.dianshi.attr;


import com.siasun.dianshi.bean.Point2d;

public class PathBaseAttr {
    public int m_uId;            // 路段ID号
    public short m_uType;          // 路段类型码
    public short m_uExtType;       // 路段的扩展类型码
    public int m_uStartNode;     //起点
    public int m_uEndNode;       //终点
    public float[]  m_fVeloLimit = new float[2];  // The velocity limit on the path
    public short m_uGuideType;     //导航类型
    public int m_uFwdRotoScannerObstacle;
    public int m_uFwdObdetectorObstacle;
    public int m_uBwdRotoScannerObstacle;
    public int m_uBwdObdetectorObstacle;
    public short m_uPathHeading;			// 路段车头方向 起点 终点
    public short m_uMoveHeading;			// AGV行进方向
    public float  m_fSize;			        // 路段长度
    public float  m_fNavParam;             // Navigation parameter 盲走距离
    public short m_SamCount;              // 采样点计数
    public Point2d m_ptSam;              // 采样点

    public double  m_fangle;                // 直线路段的角度
    public double  m_fPathStartHeading;	    // 曲线路段起点方向角
    public double  m_fPathEndHeading;		// 曲线路段终点方向角

    public float  m_angShiftHeading;        // 单位为(弧度）在此路段上的车头方向角,用于硬侧移和软侧移设置车头方向

    //侧移情况，直线变为硬侧移，相应的路线类型变为SIDE_TYPE；
    //曲线变为软侧移,路段类型不变。
    //值为0，表示就是基本的类型，值为1，表示为侧移类型。
    public short  m_uShift;
    public int   m_nCountKeyPoints;
    public Point2d[] m_ptKey;
    public short m_uLayerId;
    public char[] buf = new char[100];

    //路段限制参数（轮最大线速度，车最大角速度，最大角加速度，舵角最大变化量，）
    public float	 m_fVelMax;				// 最弱的轮的最大线速度（m/s）
    public float	 m_fThitaDiffMax;		// 最弱的舵的最大角速度(rad/s)

    public float	 m_fAngVelMax;			// 运动中心最大角速度（rad/s）
    public float	 m_fAngVelACC;			// 运动中心最大角加速度(rad/s/s)
    public float	 m_fVelACC;				// 运动中心最大线加速度(m/s/s)

    public float	 m_fLenForAngJump;				// 路段端点的打舵距离(m)
    public float	 m_fThitaDiffMaxForStAndEd;		// 最弱的舵在路段端点的最大角速度(rad/s)

    public float     m_fLamdaStart;			// 贝塞尔的lamda值
    public float     m_fLamdaEnd;			// 贝塞尔的lamda值

    public short m_uCarrierType;  //车辆类型
    public short m_uOnLine;       //允许上线

    public	PathBaseAttr()
    {

    }
}
