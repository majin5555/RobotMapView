package com.siasun.dianshi.bean.pp;

import com.siasun.dianshi.bean.Point2d;

import java.util.Vector;

public class FindOptControlPoint {
    public Point2d cpStartCtrlPt;                    // 临近起点的控制点
    public Point2d cpEndCtrlPt;                      // 临近终点的控制点
    public Vector<Point2d> m_vecMiddleCtrlPt;        // 中间的控制点
    public BezierLineHighOrder clsBezierLine = new BezierLineHighOrder();        // 存储被优化的曲线信息
    private boolean bOptCtrlToSE;        //控制点从交点向两个端点优化开关，true的时候启用 20200406
    private float fLenSt;            //交点到起点长度20200406
    private float fLenEd;            //交点到终点长度20200406
    private float ptTX;                //交点坐标X 20200406
    private float ptTY;                //交点坐标Y 20200406
    //20220320寻找最优控制点
    private double m_dBestCPLamdaStart;
    private double m_dBestCPLamdaEnd;
    //	///////////////////////////////////////////////////
    //预计最优时间
    private float m_fBestTimetotal;

    //	///////////////////////////////////////////////////
    // 构造函数
    public FindOptControlPoint() {
        Init();
    }

    // 初始化函数
    public void Init() {
//        clsBezierLine = new BezierLineHighOrder(1);
        clsBezierLine = new BezierLineHighOrder();
        cpStartCtrlPt = new Point2d();
        cpEndCtrlPt = new Point2d();
        cpStartCtrlPt.x = -1;
        cpStartCtrlPt.y = -1;
        cpEndCtrlPt.x = -1;
        cpEndCtrlPt.y = -1;

        m_vecMiddleCtrlPt = new Vector<>();
        m_vecMiddleCtrlPt.clear();

        SetControlPointStEd(0, 0);

        bOptCtrlToSE = false;
        fLenSt = 0;            //交点到起点长度20200406
        fLenEd = 0;            //交点到终点长度20200406
        ptTX = 0;                //交点坐标X 20200406
        ptTY = 0;                //交点坐标Y 20200406

        //    clsBezierLine = new BezierLineHighOrder(1);
//        clsBezierLine = new BezierLineHighOrder();
//        cpStartCtrlPt.x = -1;
//        cpStartCtrlPt.y = -1;
//        cpEndCtrlPt.x = -1;
//        cpEndCtrlPt.y = -1;
//
//        m_vecMiddleCtrlPt.clear();
//
//        SetControlPointStEd(0, 0);
//
//        bOptCtrlToSE = false;
//        fLenSt = 0;			//交点到起点长度20200406
//        fLenEd = 0;			//交点到终点长度20200406
//        ptTX = 0;				//交点坐标X 20200406
//        ptTY = 0;				//交点坐标Y 20200406
    }

    // (重新)设置曲线起点和终点的位姿
    public void SetPosturePoint(double x1, double y1, double a1, double x2, double y2, double a2) {
        clsBezierLine.SetPosturePoint(x1, y1, a1, x2, y2, a2);
    }

    // 设置AGV限制参数，计算曲率分界点
    public void SetAgvParamLimit(double dAgvLineVelMax, double dAgvAngVelMax, double dAgvCurvaMax) {
        clsBezierLine.SetAgvParamLimit(dAgvLineVelMax, dAgvAngVelMax, dAgvCurvaMax);
    }

    // jiang
    public void SetParam(float[] param) {
        clsBezierLine.SetParam(param);
    }

    /* (重新)设置控制参数，生成第一个和最后一个控制点，重置控制点vector
    注：需要更改dLamda参数时请使用ChangeControlPointStEd函数
    --- 否则会清空三阶以上贝塞尔曲线的其他控制点 */
    public void SetControlPointStEd(double dLamdaSt, double dLamdaEd) {
        clsBezierLine.SetControlPointStEd(dLamdaSt, dLamdaEd);

        int iCtrlPtNum = clsBezierLine.m_vecControlPoint.size();   // 控制点的个数

        if (iCtrlPtNum == 4) {
            cpStartCtrlPt.x = clsBezierLine.m_vecControlPoint.get(1).x;
            cpStartCtrlPt.y = clsBezierLine.m_vecControlPoint.get(1).y;
            cpEndCtrlPt.x = clsBezierLine.m_vecControlPoint.get(2).x;
            cpEndCtrlPt.y = clsBezierLine.m_vecControlPoint.get(2).y;

            m_vecMiddleCtrlPt.clear();
        }
    }

    //	设置一些参数（用于寻找控制点）
    //控制点由交点向两端推进，bCtrltoSE = true-启用, temppt-交点坐标，fSdist-起点到交点距离，fEdist-终点到交点距离
    //bCtrltoSE = false-启用控制点有两端向外推进，比例基数为起点到终点的距离
    public void SetOptCtrltoSE(boolean bCtrltoSE, float ptX, float ptY, float fSdist, float fEdist) {
        bOptCtrlToSE = bCtrltoSE;
        if (bCtrltoSE) {
            fLenSt = fSdist;            //交点到起点长度20200406
            fLenEd = fEdist;            //交点到终点长度20200406
            ptTX = ptX;                //交点坐标X 20200406
            ptTY = ptY;                //交点坐标Y 20200406
        }
    }

    //（3）寻找最有控制点起始
    // 端点切向相交处向两端方向寻找最优位置 两端dLamda依旧保持值相同 TestFor20200406
    public boolean FindOptCtrlPtStEdSameLamda() {
        double m_dBestCPLamdaStart = 0.95;
        double m_dBestCPLamdaEnd = 0.95;
        float m_fBestTimetotal = 99999;
        //bOptCtrlToSE = true时，控制点选取是从交点向端点推进，比例基数为交点到起点、终点的距离
        //bOptCtrlToSE = false时，控制点从两端向外推进，比例基数为起点到终点的距离
        if (bOptCtrlToSE) {
            double dSearchStep = 0.05;         // 寻优步长
            double dOptLamda = 0.95;// 2;	// 最优dLamda位置
            for (dOptLamda = 0.95; dOptLamda > 0; dOptLamda -= dSearchStep) {
                ChangeControlPointStEd(dOptLamda, dOptLamda);//根据比例更新第一个和最后一个控制点
                BezierLineHighOrderMain();//	贝塞尔计算主函数，
                //判断该控制点对应的曲线是否符合要求
                if (clsBezierLine.bCurvatureIsValid) {
                    //判断当前控制点所对应的曲线用时，是不是最短的
                    if (clsBezierLine.m_fOKTime < m_fBestTimetotal) {
                        m_fBestTimetotal = clsBezierLine.m_fOKTime;//更新最短用时
                        m_dBestCPLamdaStart = dOptLamda;//更新第一个控制点比例
                        m_dBestCPLamdaEnd = dOptLamda;//更新最后一个控制点比例
                    }
                }
            }
            ChangeControlPointStEd(m_dBestCPLamdaStart, m_dBestCPLamdaEnd);//根据比例更新第一个和最后一个控制点
            BezierLineHighOrderMain();//再次验证
            return clsBezierLine.bCurvatureIsValid;
        }
        //如果是S型曲线或者交点过远
        double dSearchStep = 0.05;                                             // 寻优步长
        double dOptLamda = dSearchStep;                                        // 最优dLamda位置
        for (dOptLamda = 0.05; dOptLamda < 2; dOptLamda += dSearchStep) {
            ChangeControlPointStEd(dOptLamda, dOptLamda);//根据比例更新第一个和最后一个控制点
            BezierLineHighOrderMain();
            if (clsBezierLine.bCurvatureIsValid) {
                if (clsBezierLine.m_fOKTime < m_fBestTimetotal) {
                    m_fBestTimetotal = clsBezierLine.m_fOKTime;
                    m_dBestCPLamdaStart = dOptLamda;
                    m_dBestCPLamdaEnd = dOptLamda;
                }
            }
        }
        ChangeControlPointStEd(m_dBestCPLamdaStart, m_dBestCPLamdaEnd);//根据比例更新第一个和最后一个控制点
        BezierLineHighOrderMain();
        return clsBezierLine.bCurvatureIsValid;
    }

    //（4）根据比例更新第一个和最后一个控制点（FindOptControlPoint）
    // 更新第一个和最后一个控制点（通过dLamda参数修改）
    public boolean ChangeControlPointStEd(double dLamdaSt, double dLamdaEd) {
        boolean ret;
        if (bOptCtrlToSE)
            ret = clsBezierLine.ChangeControlPointStEdSec(dLamdaSt, dLamdaEd, fLenSt, fLenEd);//（有交点）根据比例更新第一个和最后一个控制点
        else
            ret = clsBezierLine.ChangeControlPointStEd(dLamdaSt, dLamdaEd);//（无交点）根据比例更新第一个和最后一个控制点
        if (ret) {
            int iCtrlPtNum = clsBezierLine.m_vecControlPoint.size();   // 控制点的个数
            cpStartCtrlPt.x = clsBezierLine.m_vecControlPoint.get(1).x;        //更新第一个控制点的x值
            cpStartCtrlPt.y = clsBezierLine.m_vecControlPoint.get(1).y;        //更新第一个控制点的y值
            cpEndCtrlPt.x = clsBezierLine.m_vecControlPoint.get(iCtrlPtNum - 2).x;    //更新最后一个控制点的x值
            cpEndCtrlPt.y = clsBezierLine.m_vecControlPoint.get(iCtrlPtNum - 2).y;    //更新最后一个控制点的y值
        }
        return ret;
    }

    //（7）贝塞尔计算主函数（FindOptControlPoint）
// 高阶贝塞尔曲线主函数 - 计算曲线特征参数
    public void BezierLineHighOrderMain() {
        clsBezierLine.BezierLineHighOrderMain();//调用Bezier中
    }
}
