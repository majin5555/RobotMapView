package com.siasun.dianshi.bean.pp;


import com.siasun.dianshi.bean.Point2d;

import java.util.Vector;

public class BezierLineHighOrder {
    private final int INTEG_RESOLUTION = 300;

    public double dBzLineCurvaMax;             // 曲线最大曲率
    public double dBzLineCurvaMin;             // 曲线最小曲率
    public double dBzLineCurvaSt;              // 曲线起点曲率
    public double dBzLineCurvaEd;              // 曲线终点曲率
    public double dBzLineCurvaGradientMax;     // 曲线的曲率变化率的最大值
    public boolean bCurvatureIsValid;             // 曲线曲率是否满足要求
    //	///////////////////////////////////////////////////
    public float m_fOKTime;            //整个曲线的最短用时
    public double m_dCPLamdaStart;        //最优控制点比例，第一个到起点
    public double m_dCPLamdaEnd;        //最优控制点比例，最后一个到终点
    public double dArcTotalLength;             // 曲线长度
    public double dAgvTotalTime;               // AGV经过曲线所用时间
    public Vector<Point2d> m_vecControlPoint = new Vector<>();   // 按顺序存储曲线的控制点//贝塞尔曲线所有的控制点


    //	计算曲线特征需要的变量
//（0.1.1）固定的值，或用户写入的值
    //采样点数量
    int nSegCount = INTEG_RESOLUTION;
    double dResolution = (double) 1 / INTEG_RESOLUTION;
    //（距离最远的）舵轮的安装位置X值
    float Wheel_x = 0;
    //（距离最远的）舵轮的安装位置Y值
    float Wheel_y = 1;
    //（能力最弱的轮）最大的线速度
    float m_fVelMax = 1;
    //（能力最弱的轮）最大的角速度
    float m_fThitaDiffMax = 0.3f;//参与AGV舵轮每秒打舵角度
    //运动中心最大角速度
    float m_fAngVelMax = 0.6f;
    //运动中心最大角加速度
    float m_fAngVelACC = 0.3f;
    //运动中心最大线加速度
    float m_fVelACC = 0.3f;
    //端点预留的打舵距离
    float fLenForAngJump = 0.1f;    //	起始和结束处理舵角突变的路段距离
    //（能力最弱的轮）在端点处最大的角速度
    float m_fThitaDiffMaxForStAndEd = 0.3f;//参与AGV舵轮每秒打舵角度
    //根据最大打舵角度，得到的最大曲率限制
    double dCurvaMax;              // AGV曲率限制
    //（0.1.2）临时变量
    //各采样点的曲率
    double[] dCurva = new double[INTEG_RESOLUTION + 1];
    //各采样点的长度
    float[] fSegLen = new float[INTEG_RESOLUTION + 1];
    //各采样点的方向角
    float[] m_angTangent = new float[INTEG_RESOLUTION + 1];
    //（0.1.3）可能需要输出的变量
    //预计通过曲线时间
    float m_fTimetotal = 0.0f;
    //各采样点的速度（给定初值为1，存放后续计算得到的各采样点的速度）
    float[] fVelLimit = new float[INTEG_RESOLUTION + 1];
    //各采样点的角速度（给定初值为1，存放后续计算得到的各采样点的角速度）
    float[] wtemp = new float[INTEG_RESOLUTION + 1];


    // 起点位姿
    private double dStartPostureX;
    private double dStartPostureY;
    private double dStartPostureAng;
    // 终点位姿
    private double dEndPostureX;
    private double dEndPostureY;
    private double dEndPostureAng;
    private double dDistFromStartToEnd;    // 起点到终点的连线长度
    private double dAngleFromStartToEnd;   // 起点到终点的连线角度
    private double dLamdaStart;            // 第一个控制点的位置参数
    private double dLamdaEnd;              // 最后一个控制点的位置参数

    public BezierLineHighOrder() {
        for (int i = 0; i < INTEG_RESOLUTION + 1; i++) {
            dCurva[i] = 0;
            fSegLen[i] = 0;
            m_angTangent[i] = 0;
            fVelLimit[i] = 1;
            wtemp[i] = 1;
        }
        Init(1);
    }

    // 初始化函数
    public void Init(int idInput) {
//        _id = idInput;
//
//        SetPosturePoint(0, 0, 0, 0, 0, 0);
//
//        dLamdaStart = 0;
//        dLamdaEnd = 0;
//
//        m_vecControlPoint.clear();
//
//        dLineVelMax = 0;
//        dAngVelMax = 0;
//        dCurvaMax = 0;
//        dCurvaBoun = 0;
//
//        dVertexCurvaMax = TransSaltationRudderToCurva(AGVRUDDERSALTATIONMAX);
//        dCurvaGradientMax = dVertexCurvaMax;
//
//        dBzLineCurvaMax = 0;
//        dBzLineCurvaMin = 0;
//        dBzLineCurvaSt = 0;
//        dBzLineCurvaEd = 0;
//        dBzLineCurvaGradientMax = 0;
//
//        bCurvatureIsValid = false;
//
//        dArcTotalLength = 0;
//        dAgvTotalTime = 0;
    }

    // 载入用户输入参数
    public void LoadParam() {

    }

    // (重新)设置曲线起点和终点的位姿，(重新)计算两点间参数
    public void SetPosturePoint(double x1, double y1, double a1, double x2, double y2, double a2) {
        dStartPostureX = x1;
        dStartPostureY = y1;
        dStartPostureAng = a1;
        dEndPostureX = x2;
        dEndPostureY = y2;
        dEndPostureAng = a2;

        // 计算起点到终点连线的长度和角度
        ComputeParamFromStartToEnd();

        int iCtrlPtNum = m_vecControlPoint.size();   // 控制点的个数
        if (iCtrlPtNum > 1) {
            m_vecControlPoint.get(0).x = (float) dStartPostureX;
            m_vecControlPoint.get(0).y = (float) dStartPostureY;
            m_vecControlPoint.get(iCtrlPtNum - 1).x = (float) dEndPostureX;
            m_vecControlPoint.get(iCtrlPtNum - 1).y = (float) dEndPostureY;
        }
    }

    // 设置AGV限制参数，计算曲率分界点
    public void SetAgvParamLimit(double dAgvLineVelMax, double dAgvAngVelMax, double dAgvCurvaMax) {
//        dLineVelMax = dAgvLineVelMax;
//        dAngVelMax = dAgvAngVelMax;
        dCurvaMax = dAgvCurvaMax;
//        dCurvaBoun = dAngVelMax / dLineVelMax;
    }

    public void SetParam(float[] param) {
        //（距离最远的）舵轮的安装位置X值
        Wheel_x = 0;
        //（距离最远的）舵轮的安装位置Y值
        Wheel_y = 1;
        //（能力最弱的轮）最大的线速度
        m_fVelMax = 1;
        //（能力最弱的轮）最大的角速度
        m_fThitaDiffMax = 0.3f;//参与AGV舵轮每秒打舵角度
        //运动中心最大角速度
        m_fAngVelMax = 0.6f;
        //运动中心最大角加速度
        m_fAngVelACC = 0.3f;
        //运动中心最大线加速度
        m_fVelACC = 0.3f;
        //端点预留的打舵距离
        fLenForAngJump = 0.1f;    //	起始和结束处理舵角突变的路段距离
        //（能力最弱的轮）在端点处最大的角速度
        m_fThitaDiffMaxForStAndEd = 0.3f;//参与AGV舵轮每秒打舵角度

        Wheel_x = param[1];
        Wheel_y = param[2];
        m_fVelMax = param[3];
        m_fThitaDiffMax = param[4];
        m_fAngVelMax = param[5];
        m_fAngVelACC = param[6];
        m_fVelACC = param[7];
        fLenForAngJump = param[8];
        m_fThitaDiffMaxForStAndEd = param[9];
    }

    // 计算起点到终点连线的长度和角度，角度取值范围为[0,2*pi)
    public void ComputeParamFromStartToEnd() {
        double dDeltaX = dEndPostureX - dStartPostureX;
        double dDeltaY = dEndPostureY - dStartPostureY;

        dDistFromStartToEnd = Math.sqrt(Math.pow(dDeltaX, 2) + Math.pow(dDeltaY, 2));

        if (dDeltaY >= 0)
            dAngleFromStartToEnd = Math.acos(dDeltaX / dDistFromStartToEnd);
        else
            dAngleFromStartToEnd = Math.acos(-dDeltaX / dDistFromStartToEnd) + Math.PI;
    }

    // (重新)设置控制参数，生成第一个和最后一个控制点，重置控制点vector
    public void SetControlPointStEd(double dLamdaSt, double dLamdaEd) {
        dLamdaStart = dLamdaSt;
        dLamdaEnd = dLamdaEd;

        double dCtrlPtStFromStart = dLamdaStart * dDistFromStartToEnd;
        double dCtrlPtEdFromEnd = dLamdaEnd * dDistFromStartToEnd;

        double CtrlPtStX = dStartPostureX + dCtrlPtStFromStart * Math.cos(dStartPostureAng);
        double CtrlPtStY = dStartPostureY + dCtrlPtStFromStart * Math.sin(dStartPostureAng);
        double CtrlPtEdX = dEndPostureX + dCtrlPtEdFromEnd * Math.cos(dEndPostureAng + Math.PI);
        double CtrlPtEdY = dEndPostureY + dCtrlPtEdFromEnd * Math.sin(dEndPostureAng + Math.PI);

        // *** 载入控制点 ***
        m_vecControlPoint.clear();

        Point2d StartPt = new Point2d();    // { dStartPostureX, dStartPostureY };
        StartPt.x = (float) dStartPostureX;
        StartPt.y = (float) dStartPostureY;
        Point2d CtrlPtSt = new Point2d();
        ;   // { CtrlPtStX, CtrlPtStY };
        CtrlPtSt.x = (float) CtrlPtStX;
        CtrlPtSt.y = (float) CtrlPtStY;
        Point2d CtrlPtEd = new Point2d();
        ;   // { CtrlPtEdX, CtrlPtEdY };
        CtrlPtEd.x = (float) CtrlPtEdX;
        CtrlPtEd.y = (float) CtrlPtEdY;
        Point2d EndPt = new Point2d();
        ;      // { dEndPostureX, dEndPostureY };
        EndPt.x = (float) dEndPostureX;
        EndPt.y = (float) dEndPostureY;

        m_vecControlPoint.add(StartPt);
        m_vecControlPoint.add(CtrlPtSt);
        m_vecControlPoint.add(CtrlPtEd);
        m_vecControlPoint.add(EndPt);
    }

    private int Factorial(int n) {
        int m = 1;
        for (int i = 1; i <= n; i++)
            m *= i;

        return m;
    }

    // 计算组合数
    public int MyCombination(int iComN, int iComI) {
        double dValue = Factorial(iComN) / Factorial(iComI) / Factorial(iComN - iComI);

        return (int) dValue;
    }

    //（5）（有交点）根据比例更新第一个和最后一个控制点
// 更新第一个和最后一个控制点（通过dLamda参数修改）,dLenSt为起点到交点长度， dLenEd为终点到交点长度---20200406
    public boolean ChangeControlPointStEdSec(double dLamdaSt, double dLamdaEd, float dLenSt, float dLenEd) {
        dLamdaStart = dLamdaSt;
        dLamdaEnd = dLamdaEd;
        double dCtrlPtStFromStart = dLamdaStart * dLenSt;
        double dCtrlPtEdFromEnd = dLamdaEnd * dLenEd;
        double CtrlPtStX = dStartPostureX + dCtrlPtStFromStart * Math.cos(dStartPostureAng);
        double CtrlPtStY = dStartPostureY + dCtrlPtStFromStart * Math.sin(dStartPostureAng);
        double CtrlPtEdX = dEndPostureX + dCtrlPtEdFromEnd * Math.cos(dEndPostureAng + Math.PI);
        double CtrlPtEdY = dEndPostureY + dCtrlPtEdFromEnd * Math.sin(dEndPostureAng + Math.PI);
        int iCtrlPtNum = m_vecControlPoint.size();   // 控制点的个数
        if (iCtrlPtNum < 4) {
            //     printf(" 曲线初始化错误，不能更改dLamda参数!\n");
            return false;
        } else {
            m_vecControlPoint.get(1).x = (float) CtrlPtStX;
            m_vecControlPoint.get(1).y = (float) CtrlPtStY;
            m_vecControlPoint.get(iCtrlPtNum - 2).x = (float) CtrlPtEdX;
            m_vecControlPoint.get(iCtrlPtNum - 2).y = (float) CtrlPtEdY;
            return true;
        }
    }

    //（6）（无交点）根据比例更新第一个和最后一个控制点
// 更新第一个和最后一个控制点（通过dLamda参数修改）
    public boolean ChangeControlPointStEd(double dLamdaSt, double dLamdaEd) {
        dLamdaStart = dLamdaSt;
        dLamdaEnd = dLamdaEd;
        double dCtrlPtStFromStart = dLamdaStart * dDistFromStartToEnd;
        double dCtrlPtEdFromEnd = dLamdaEnd * dDistFromStartToEnd;
        double CtrlPtStX = dStartPostureX + dCtrlPtStFromStart * Math.cos(dStartPostureAng);
        double CtrlPtStY = dStartPostureY + dCtrlPtStFromStart * Math.sin(dStartPostureAng);
        double CtrlPtEdX = dEndPostureX + dCtrlPtEdFromEnd * Math.cos(dEndPostureAng + Math.PI);
        double CtrlPtEdY = dEndPostureY + dCtrlPtEdFromEnd * Math.sin(dEndPostureAng + Math.PI);
        int iCtrlPtNum = m_vecControlPoint.size();   // 控制点的个数
        if (iCtrlPtNum < 4) {
            //    printf(" 曲线初始化错误，不能更改dLamda参数!\n");
            return false;
        } else {
            m_vecControlPoint.get(1).x = (float) CtrlPtStX;
            m_vecControlPoint.get(1).y = (float) CtrlPtStY;
            m_vecControlPoint.get(iCtrlPtNum - 2).x = (float) CtrlPtEdX;
            m_vecControlPoint.get(iCtrlPtNum - 2).y = (float) CtrlPtEdY;
            return true;
        }
    }

    // 计算曲线特征(整理 - 包括曲率特征、AGV路程和时间)
    public void ComputeBzLineFeature() {
//        int nSegCount = INTEG_RESOLUTION;
//        double dResolution = (double)1 / INTEG_RESOLUTION;
//        float m_fTimetotal = 0;
//
//     //   float fVelLimit[301] = { 1 };
//        float[] fVelLimit = new float[INTEG_RESOLUTION+1];
//    //    float wtemp[301] = { 1 };
//        float[] wtemp = new float[INTEG_RESOLUTION+1];
//        float Wheel_x = 0;
//        float Wheel_y = 1;
//        float m_fVelMax = 1;
//        float m_fAngVelMax = 0.6f;
////        float m_angTangent[301];
//        float[] m_angTangent = new float[INTEG_RESOLUTION+1];
////        float fSegLen[301] = { 0 };
//        float[] fSegLen = new float[INTEG_RESOLUTION+1];
////        double dCurva[301] = { 0 };
//        double[] dCurva = new double[INTEG_RESOLUTION+1];
//        for(int i=0;i<INTEG_RESOLUTION+1;i++)
//        {
//            fVelLimit[i] = 1;
//            wtemp[i] = 1;
//            fSegLen[i] = 0;
//            dCurva[i] = 0;
//        }
//
//        float m_fThitaDiffMaxForStAndEd = 0.3f;//参与AGV舵轮每秒打舵角度
//        float m_fThitaDiffMax = 0.3f;//参与AGV舵轮每秒打舵角度
//        float m_fAngVelACC = 0.3f;
//        float m_fVelACC = 0.3f;
//        float fLenForAngJump = 0.1f;	//	起始和结束处理舵角突变的路段距离
        //	///////////////////////////////////////////////////
        //LS 20220329驻点计算曲率，速度，约束后的速度
        m_fTimetotal = 0.0f;
        for (int j = 0; j <= INTEG_RESOLUTION; j++) {
            double dResolutionI = j * dResolution;

            double dDeriv1stX = 0;//存放 1 阶导数，X
            double dDeriv1stY = 0;//存放 1 阶导数，Y
            double dDeriv2ndX = 0;//存放 2 阶导数，X
            double dDeriv2ndY = 0;//存放 2 阶导数，Y

            // 求导 - Start
            int iCtrlPtNum = m_vecControlPoint.size();   // 获取控制点的个数 3阶4点，m_vecControlPoint：保存所有控制点（包含起点终点）
            int n = iCtrlPtNum - 1;                      // n阶贝塞尔曲线 n=3

            for (int i = 0; i < iCtrlPtNum; i++) {
                double dCombination = MyCombination(n, i);

                // 一阶导数
                double dPow11 = (n - i - 1 >= 0) ? Math.pow(1 - dResolutionI, n - i - 1) : 0;
                double dPow12 = Math.pow(dResolutionI, i);
                double dPow13 = Math.pow(1 - dResolutionI, n - i);
                double dPow14 = (i - 1 >= 0) ? Math.pow(dResolutionI, i - 1) : 0;

                double dParamDeriv1st = dCombination * (-(n - i) * dPow11 * dPow12 + i * dPow13 * dPow14);

                dDeriv1stX += dParamDeriv1st * m_vecControlPoint.get(i).x;//控制点的x值
                dDeriv1stY += dParamDeriv1st * m_vecControlPoint.get(i).y;//控制点的y值

                // 二阶导数
                double dPow21 = (n - i - 2 >= 0) ? Math.pow(1 - dResolutionI, n - i - 2) : 0;
                double dPow22 = Math.pow(dResolutionI, i);
                double dPow23 = (n - i - 1 >= 0) ? Math.pow(1 - dResolutionI, n - i - 1) : 0;
                double dPow24 = (i - 1 >= 0) ? Math.pow(dResolutionI, i - 1) : 0;
                double dPow25 = Math.pow(1 - dResolutionI, n - i);
                double dPow26 = (i - 2 >= 0) ? Math.pow(dResolutionI, i - 2) : 0;

                double dParamDeriv2nd = dCombination * (
                        (n - i - 1) * (n - i) * dPow21 * dPow22
                                - 2 * i * (n - i) * dPow23 * dPow24
                                + i * (i - 1) * dPow25 * dPow26);

                dDeriv2ndX += dParamDeriv2nd * m_vecControlPoint.get(i).x;//控制点的x值
                dDeriv2ndY += dParamDeriv2nd * m_vecControlPoint.get(i).y;//控制点的y值

            }
            // 求导 - End

            // 计算曲线的曲率值
            double dCurNumeratorTmp = dDeriv1stX * dDeriv2ndY - dDeriv2ndX * dDeriv1stY;
            double dCurNumerator = Math.abs(dCurNumeratorTmp);
            double dCurDenominatorTmp = Math.pow(dDeriv1stX, 2) + Math.pow(dDeriv1stY, 2);
            double dCurDenominator = Math.sqrt(Math.pow(dCurDenominatorTmp, 3));
            double dCurvaNow = dCurNumerator / dCurDenominator;//计算得到的曲率

            //采样点处的曲率
            dCurva[j] = dCurvaNow;
            //	采样点处的方向角 ????????????????atan2可能有问题
            m_angTangent[j] = (float) Math.atan2(dDeriv1stY, dDeriv1stX);
            //采样点对应的长度
            fSegLen[j] = (float) (Math.sqrt(dDeriv1stX * dDeriv1stX + dDeriv1stY * dDeriv1stY) * 1.0f / nSegCount);
            //	///////////////////////////////////////////////////

            if (j == 0)//(j == 0.001)//
            {
                // 记录曲线起点曲率
                dBzLineCurvaSt = dCurvaNow;
                // 初始化曲线最大曲率
                dBzLineCurvaMax = dCurvaNow;
                // 初始化曲线最小曲率
                dBzLineCurvaMin = dCurvaNow;
            } else {
                // 记录曲线最大曲率
                if (dCurvaNow > dBzLineCurvaMax)
                    dBzLineCurvaMax = dCurvaNow;
                // 记录曲线最小曲率
                if (dCurvaNow < dBzLineCurvaMin)
                    dBzLineCurvaMin = dCurvaNow;
                // 记录曲线终点曲率
                if (j == INTEG_RESOLUTION)
                    dBzLineCurvaEd = dCurvaNow;
            }
            //	////////////////////////////////////////////////////////////////////////////
            float R = (float) (1 / dCurvaNow);                           // 曲率半径
            float f = R + Wheel_x;
            float L = (float) Math.sqrt(Wheel_y * Wheel_y + f * f);
            float Vmax = m_fVelMax;
            // 车轮速度不允许超过规定的车体最大角速度
            float Vw = m_fAngVelMax * L;
            if (Math.abs(f) >= Math.abs(Wheel_y))
                Vmax = 1.0f;
            else
                Vmax = m_fVelMax;
            Vmax = Math.min(Vmax, Vw);

            fVelLimit[j] = Math.abs(R * Vmax / L);
            wtemp[j] = fVelLimit[j] * (float) dCurvaNow;
        }
//速度限制
//	通过舵角的速度，加速度和角加速度   限制车的线速度
//	新加的限速，包括：起点、终点曲率突变，曲率变化率，加速度，角加速度限制
//	对起点曲率突变的限制
//	起点、终点额外限速 - 目前不适用于后退运行，不适用于S型的多阶曲线
//	通过曲率变化限制速度
//	指定长度，在指定长度上减速运行，以满足打舵需求
        int iStart = -1;        //	iStart之前低速运行，以满足打舵要求
        int iEnd = 501;            //	iEnd之后低速运行，以满足打舵要求
        float fLenFromStart = 0;//	需要低速运行路段的长度（起点）
        float fLenToEnd = 0;    //	需要低速运行路段的长度（终点）
        for (int i = 0; i < nSegCount; i++) {
            //	累加离散点之间的长度，长度满足：从起点到iStart的长度需要刚刚大于10cm（fLenForAngJump）
            fLenFromStart = fLenFromStart + fSegLen[i];
            if (fLenFromStart > fLenForAngJump) {
                iStart = i;
                break;
            }
        }
        for (int i = nSegCount - 1; i > 0; i--) {
            //	累加离散点之间的长度，长度满足：从iEnd到终点的长度需要刚刚大于10cm
            fLenToEnd = fLenToEnd + fSegLen[i];
            if (fLenToEnd > fLenForAngJump) {
                iEnd = i;
                break;
            }
        }
        if (iStart >= 0) {
            float fSegCurvatureStart = (float) dCurva[iStart];//10cm处的曲率
            float fCurTimeStart = fLenFromStart / fVelLimit[0];//以起点速度走过10cm路段的时间
            float RStart = 1 / fSegCurvatureStart;//iStart点的曲率半径         // 后退时，为 - 号
            float fThitaStart = (float) Math.atan2(Wheel_y, (RStart + Wheel_x));//iStart点对应的舵角
            if (fThitaStart > Math.PI / 2)
                fThitaStart = (float) (fThitaStart - Math.PI);//将舵角值转化到[-PI/2，PI/2]之间
            if (fThitaStart < -Math.PI / 2)
                fThitaStart = (float) (fThitaStart + Math.PI);//将舵角值转化到[-PI/2，PI/2]之间
            float fThitaDiffStart = Math.abs(fThitaStart / fCurTimeStart);//以起点速度运行时，计算起点附近舵角变化速度
            if (fThitaDiffStart > m_fThitaDiffMaxForStAndEd) {
                //如果舵角变化速度过高，通过舵角最高变化速度限制运行在该路段上的运行速度
                float fCurTimeChange = Math.abs(fThitaStart / m_fThitaDiffMaxForStAndEd);//以最大舵角变化速度转舵所需要的时间
                fVelLimit[0] = fLenFromStart / fCurTimeChange;
                wtemp[0] = fVelLimit[0] * fSegCurvatureStart;
                for (int j = 1; j < (iStart + 1); j++) {
                    fVelLimit[j] = fVelLimit[0];//将iStart点之前的速度统一进行限制
                    float fSegCStart = (float) dCurva[j];
                    wtemp[j] = fVelLimit[j] * fSegCStart;        //重新计算角速度
                }
            }
        }
        //最后一点
        if (iEnd < nSegCount + 2) {
            float fSegCurvatureEnd = (float) dCurva[iEnd];
            float fCurTimeEnd = fLenFromStart / fVelLimit[iEnd];//从iEnd点到终点的运行时间
            float REnd = 1 / fSegCurvatureEnd;//iEnd点的曲率半径
            float fThitaEnd = (float) Math.atan2(Wheel_y, (REnd + Wheel_x));//iEnd点的舵角
            if (fThitaEnd > Math.PI / 2)
                fThitaEnd = (float) (fThitaEnd - Math.PI);//将舵角值转化到[-PI/2，PI/2]之间
            if (fThitaEnd < -Math.PI / 2)
                fThitaEnd = (float) (fThitaEnd + Math.PI);//将舵角值转化到[-PI/2，PI/2]之间
            float fThitaDiffEnd = Math.abs(fThitaEnd / fCurTimeEnd);//计算舵角变化速度
            if (fThitaDiffEnd > m_fThitaDiffMaxForStAndEd) {
                //如果舵角变化速度过高，通过舵角最高变化速度限制运行在该路段上的运行速度
                float fCurTimeChange = Math.abs(fThitaEnd / m_fThitaDiffMaxForStAndEd);//以最大舵角变化速度转舵所需要的时间
                fVelLimit[iEnd] = fLenFromStart / fCurTimeChange;
                wtemp[iEnd] = fVelLimit[iEnd] * fSegCurvatureEnd;
                for (int j = iEnd + 1; j < nSegCount; j++) {
                    fVelLimit[j] = fVelLimit[iEnd];//将iEnd点之后的速度统一进行限制
                    float fSegCEnd = (float) dCurva[j];
                    wtemp[j] = fVelLimit[j] * fSegCEnd;        //重新计算角速度
                }
            }
        }
        //	曲率变化限制速度
        for (int i = 0; i < nSegCount - 1; i++) {
            float fCurLength = fSegLen[i];
            float fCurTime1 = fCurLength / fVelLimit[i];
            float fCurTime2 = fCurLength / fVelLimit[i + 1];
            float fSegC1 = (float) dCurva[i];        //第i点的曲率
            float fSegC2 = (float) dCurva[i + 1];    //第i+1点的曲率
            float R1 = 1 / fSegC1;        //第i点的曲率半径
            float R2 = 1 / fSegC2;        //第i+1点的曲率半径
            float fThita1 = (float) Math.atan2(Wheel_y, (R1 + Wheel_x));
            float fThita2 = (float) Math.atan2(Wheel_y, (R2 + Wheel_x));
            if (fThita1 > Math.PI / 2)
                fThita1 = (float) (fThita1 - Math.PI);//将舵角值转化到[-PI/2，PI/2]之间
            if (fThita1 < -Math.PI / 2)
                fThita1 = (float) (fThita1 + Math.PI);//将舵角值转化到[-PI/2，PI/2]之间

            if (fThita2 > Math.PI / 2)
                fThita2 = (float) (fThita2 - Math.PI);//将舵角值转化到[-PI/2，PI/2]之间
            if (fThita2 < -Math.PI / 2)
                fThita2 = (float) (fThita2 + Math.PI);//将舵角值转化到[-PI/2，PI/2]之间
//计算舵角变化速度
            float fThitaDiff1 = Math.abs((fThita2 - fThita1) / fCurTime1);
            float fThitaDiff2 = Math.abs((fThita2 - fThita1) / fCurTime2);
            float fCurTimeChange = 0;
            if (fThitaDiff1 > m_fThitaDiffMax) {
                fCurTimeChange = Math.abs((fThita2 - fThita1) / m_fThitaDiffMax);
                float VelChanged = fCurLength / fCurTimeChange;
                if (VelChanged < fVelLimit[i]) {
                    fVelLimit[i] = VelChanged;
                    wtemp[i] = fVelLimit[i] * fSegC1;        //重新计算角速度
                }
            }
            if (fThitaDiff2 > m_fThitaDiffMax) {
                fCurTimeChange = Math.abs((fThita2 - fThita1) / m_fThitaDiffMax);
                float VelChanged = fCurLength / fCurTimeChange;
                if (VelChanged < fVelLimit[i + 1]) {
                    fVelLimit[i + 1] = VelChanged;
                    wtemp[i + 1] = fVelLimit[i + 1] * fSegC2;        //重新计算角速度
                }
            }
        }
        //	角加速度限制
        //从后向前推
        float AWmax = m_fAngVelACC;  //角加速度最大值（地图限制）
        float fWCurLimitDown = 0;//LS	W 限制V
        float fWCurLimitUp = 0;//LS	W 限制V
        float fCurThita1;//i 到1+1 角度
        float fCurThita2;//i-1 到i 角度
        for (int i = nSegCount - 2; i >= 0; i--) {
            float fSegC1 = (float) dCurva[i];
            float fSegC2 = (float) dCurva[i + 1];
            //由进度确定当前的切线方向
            float angS = m_angTangent[i];
            //求解目标位置的切线方向
            float angE1 = m_angTangent[i + 1];
            //fCurThita1 = fabs(angE1.m_fRad - angS.m_fRad);
            fCurThita1 = Math.abs(angE1 - angS);
            fWCurLimitDown = (float) Math.sqrt((wtemp[i + 1] * wtemp[i + 1]) + (2 * AWmax * fCurThita1));
            //LS	负数开方等于0
            float fWCurLimitUpSquare = (wtemp[i + 1] * wtemp[i + 1]) - (2 * AWmax * fCurThita1);
            if (fWCurLimitUpSquare >= 0)
                fWCurLimitUp = (float) Math.sqrt(fWCurLimitUpSquare);
            else
                fWCurLimitUp = 0;
            if (wtemp[i] >= 0) {
                if (wtemp[i] > fWCurLimitDown)
                    wtemp[i] = fWCurLimitDown;
                if (wtemp[i] < fWCurLimitUp)
                    wtemp[i + 1] = (float) Math.sqrt((wtemp[i] * wtemp[i]) + (2 * AWmax * fCurThita1));
                if (Math.abs(wtemp[i] / fSegC1) < fVelLimit[i])
                    fVelLimit[i] = Math.abs(wtemp[i] / fSegC1);
                if (Math.abs(wtemp[i + 1] / fSegC2) < fVelLimit[i + 1])
                    fVelLimit[i + 1] = Math.abs(wtemp[i + 1] / fSegC2);
            } else {
                if (Math.abs(wtemp[i]) > fWCurLimitDown)
                    wtemp[i] = (float) (-1.0) * fWCurLimitDown;
                if (Math.abs(wtemp[i]) < fWCurLimitUp)
                    wtemp[i + 1] = (float) (-1.0) * (float) Math.sqrt((wtemp[i] * wtemp[i]) + (2 * AWmax * fCurThita1));
                if (Math.abs(wtemp[i] / fSegC1) < fVelLimit[i])
                    fVelLimit[i] = Math.abs(wtemp[i] / fSegC1);
                if (Math.abs(wtemp[i + 1] / fSegC2) < fVelLimit[i + 1])
                    fVelLimit[i + 1] = Math.abs(wtemp[i + 1] / fSegC2);
            }
        }
        //从前向后推
        for (int i = 1; i < nSegCount; i++) {
            float fSegC1 = (float) dCurva[i];
            float fSegC2 = (float) dCurva[i - 1];
            //由进度确定当前的切线方向
            float angS = m_angTangent[i];
            //求解目标位置的切线方向
            float angE2 = m_angTangent[i - 1];
            fCurThita2 = Math.abs(angS - angE2);
            fWCurLimitDown = (float) Math.sqrt((wtemp[i - 1] * wtemp[i - 1]) + (2 * AWmax * fCurThita2));
            //LS	负数开方等于0
            float fWCurLimitUpSquare = (wtemp[i - 1] * wtemp[i - 1]) - (2 * AWmax * fCurThita2);
            if (fWCurLimitUpSquare >= 0)
                fWCurLimitUp = (float) Math.sqrt(fWCurLimitUpSquare);
            else
                fWCurLimitUp = 0;
            if (wtemp[i] >= 0) {
                if (wtemp[i] > fWCurLimitDown)
                    wtemp[i] = fWCurLimitDown;
                if (wtemp[i] < fWCurLimitUp)
                    wtemp[i - 1] = (float) Math.sqrt((wtemp[i] * wtemp[i]) + (2 * AWmax * fCurThita2));
                if (Math.abs(wtemp[i] / fSegC1) < fVelLimit[i])
                    fVelLimit[i] = Math.abs(wtemp[i] / fSegC1);
                if (Math.abs(wtemp[i - 1] / fSegC2) < fVelLimit[i - 1])
                    fVelLimit[i - 1] = Math.abs(wtemp[i - 1] / fSegC2);
            } else {
                if (Math.abs(wtemp[i]) > fWCurLimitDown)
                    wtemp[i] = (float) (-1.0) * fWCurLimitDown;
                if (Math.abs(wtemp[i]) < fWCurLimitUp)
                    wtemp[i - 1] = (float) (-1.0) * (float) Math.sqrt((wtemp[i] * wtemp[i]) + (2 * AWmax * fCurThita2));
                if (Math.abs((wtemp[i] / fSegC1)) < fVelLimit[i])
                    fVelLimit[i] = Math.abs(wtemp[i] / fSegC1);
                if (Math.abs((wtemp[i - 1] / fSegC2)) < fVelLimit[i - 1])
                    fVelLimit[i - 1] = Math.abs(wtemp[i - 1] / fSegC2);
            }
        }
        //	新的最大速度规划
        float AMax = m_fVelACC;//加速度最大值 a
        float fCurLeg = 0;//第i小段曲线长度，采样点 i 到 i+1 的长度。
        float fVCurLimitDown = 0;//通过第i+1点反推的第i点最高速度
        float fVCurLimitUp = 0;//通过第i+1点反推的第i点的最低速度
        //从后向前推
        for (int i = nSegCount - 2; i >= 0; i--) {
            fCurLeg = fSegLen[i];//第i 小段曲线长度，采样点i 到i+1 的长度。
            fVCurLimitDown = (float) Math.sqrt((fVelLimit[i + 1] * fVelLimit[i + 1]) + (2 * AMax/*Amaxdown*/ * fCurLeg));
            //	负数开方等于0
            float fVCurLimitUpSquare = (fVelLimit[i + 1] * fVelLimit[i + 1]) - (2 * AMax/*Amaxup*/ * fCurLeg);
            if (fVCurLimitUpSquare >= 0)
                fVCurLimitUp = (float) Math.sqrt(fVCurLimitUpSquare);
            else
                fVCurLimitUp = 0;
            if (fVelLimit[i] > fVCurLimitDown)
                fVelLimit[i] = fVCurLimitDown;//如果第i点的速度大于最大值，则限制第i点的速度
            if (fVelLimit[i] < fVCurLimitUp)
                fVelLimit[i + 1] = (float) Math.sqrt((fVelLimit[i] * fVelLimit[i]) + (2 * AMax/*Amaxup*/ * fCurLeg));//如果第i点的速度小于最小值，则限制第i+1点的速度
        }
        //从前向后推
        for (int i = 1; i < nSegCount; i++) {
            fCurLeg = fSegLen[i - 1];//第i - 1 小段曲线长度，采样点i - 1 到i 的长度。
            fVCurLimitDown = (float) Math.sqrt((fVelLimit[i - 1] * fVelLimit[i - 1]) + (2 * AMax/*Amaxup*/ * fCurLeg));
            //LS	负数开方等于0
            float fVCurLimitUpSquare = (fVelLimit[i - 1] * fVelLimit[i - 1]) - (2 * AMax/*Amaxdown*/ * fCurLeg);
            if (fVCurLimitUpSquare >= 0)
                fVCurLimitUp = (float) Math.sqrt(fVCurLimitUpSquare);
            else
                fVCurLimitUp = 0;
            if (fVelLimit[i] > fVCurLimitDown)
                fVelLimit[i] = fVCurLimitDown;//如果第i点的速度大于最大值，则限制第i点的速度
            if (fVelLimit[i] < fVCurLimitUp)
                fVelLimit[i - 1] = (float) Math.sqrt((fVelLimit[i] * fVelLimit[i]) + (2 * AMax/*Amaxdown*/ * fCurLeg));//如果第i点的速度小于最小值，则限制第i-1点的速度
        }
        //根据采样点对应长度和速度，计算可能的用时，累加后得到通过曲线的时间
        for (int k = 0; k <= INTEG_RESOLUTION; k++) {
            if (fVelLimit[k] == 0) {
                m_fTimetotal = 99999;
                break;
            } else {
                m_fTimetotal = m_fTimetotal + (fSegLen[k] / fVelLimit[k]);
            }
        }

    }

    //（8）贝塞尔计算主函数（BezierLineHighOrder）
// 高阶贝塞尔曲线主函数 - 计算曲线特征参数
    public void BezierLineHighOrderMain() {
        ComputeBzLineFeature();//计算曲线特征，主要函数
        JudgeCurvatureIsValid();//判断曲线曲率是否满足要求
    }

    //（12）判断曲线曲率是否满足要求
// 判断曲线曲率是否满足要求
    public void JudgeCurvatureIsValid() {
        bCurvatureIsValid = false;    //默认不满足要求

        if (dBzLineCurvaMax <= dCurvaMax) {
            m_dCPLamdaStart = dLamdaStart;
            m_dCPLamdaEnd = dLamdaEnd;
            m_fOKTime = m_fTimetotal;
            bCurvatureIsValid = true;//如果最大曲率满足要求，则设置为满足条件
        }
    }
}
