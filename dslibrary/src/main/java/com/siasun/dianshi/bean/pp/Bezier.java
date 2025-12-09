package com.siasun.dianshi.bean.pp;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Typeface;

import com.siasun.dianshi.bean.Point2d;
import com.siasun.dianshi.utils.CoordinateConversion;

import java.util.Vector;


public class Bezier {
    static final int BEZIER_CURVE_DEFAULT_SEG_COUNT = 200;
    private float dx1;
    private float dy1;
    private float dx2;
    private float dy2;

    private int m_nSampleCount;
    private CurveSamplePoint[] m_pSamplePoints;


    public int m_nCountKeyPoints; //4
    public Point2d[] m_ptKey;     //包括 起点、终点、两个控制点
    public Point2d m_pt;
    public Angle m_angTangent;
    public float m_fCurvature;
    public float m_fTotalLen;


    private FindOptControlPoint findOptControlPoint = new FindOptControlPoint();

    public boolean m_bCurvature; //是否曲率合适

    public Point2d TrajFun() {
        return m_pt;
    }

    public Angle TangentFun() {
        return m_angTangent;
    }

    // The curvature generation function
    public float CurvatureFun() {
        return m_fCurvature;
    }


    ///////////////////////////////////////////////////////////////////////////////

    /**
     * x的nRank次方
     *
     * @param x
     * @param nRank
     * @return
     */
    float FloatPowerInt(float x, int nRank) {
        float f = x;

        if (nRank == 0) return 1;

        for (int i = 1; i < nRank; i++)
            f *= x;

        return f;
    }

    //阶乘
    private int Factorial(int n) {
        int m = 1;
        for (int i = 1; i <= n; i++)
            m *= i;

        return m;
    }


    //组合  CnK
    private int Combination(int n, int k) {
        return Factorial(n) / (Factorial(k) * Factorial(n - k));
    }
    ///////////////////////////////////////////////////////////////////////////////


    public Bezier(int nCountKeyPoints, Point2d[] pptKey) {
        m_ptKey = null;
        Create(nCountKeyPoints, pptKey);
    }


    public Bezier(final Posture pstStart, final Posture pstEnd, float fLen1, float fLen2) {
        m_ptKey = null;
        Create(pstStart, pstEnd, fLen1, fLen2);
    }


    public Bezier() {
        m_ptKey = null;
        m_pt = null;
    }


    private void Clear() {
        if (m_ptKey != null) {
            m_ptKey = null;
        }
    }


    public boolean Create(int nCountKeyPoints, Point2d[] pptKey) {
        if (m_ptKey != null)
            m_ptKey = null;
        //		delete[]m_ptKey;

        m_bCurvature = true;

        m_nCountKeyPoints = nCountKeyPoints;


        m_ptKey = new Point2d[m_nCountKeyPoints];
        if (m_ptKey == null)
            return false;

        for (int i = 0; i < m_nCountKeyPoints; i++) {
            m_ptKey[i] = new Point2d();
            m_ptKey[i].x = pptKey[i].x;
            m_ptKey[i].y = pptKey[i].y;
        }

        m_nSampleCount = BEZIER_CURVE_DEFAULT_SEG_COUNT;
        float[] param = BezierOptParams.getInstance().GetAllParam();
        InitFindOpt(param);
        return CreateSamplePoints();
    }


    public boolean Create(final Posture pstStart, final Posture pstEnd, float fLen1, float fLen2) {
        if (m_ptKey != null)
            m_ptKey = null;
//				delete []m_ptKey;

        m_bCurvature = true;

        m_nCountKeyPoints = 4;
        m_ptKey = new Point2d[m_nCountKeyPoints];
        if (m_ptKey == null)
            return false;

        m_ptKey[0] = pstStart;
        m_ptKey[3] = pstEnd;

        Line ln1 = new Line(pstStart, fLen1);
        m_ptKey[1] = ln1.GetEndPoint();

        Angle ang = new Angle(0);
        ang.m_fRad = pstEnd.GetAngle().m_fRad + Line.PI;

        //	Line ln2= new Line(pstEnd, !pstEnd.GetAngle(),fLen2);
        Line ln2 = new Line(pstEnd, ang, fLen2);
        m_ptKey[2] = ln2.GetEndPoint();

//			m_nSampleCount = BEZIER_CURVE_DEFAULT_SEG_COUNT;


        return CreateSamplePoints();
    }


    public boolean Create(final Posture pstStart, final Posture pstEnd, float k) {
        if (m_ptKey != null) m_ptKey = null;
//				delete[]m_ptKey;

        m_bCurvature = true;

        m_nCountKeyPoints = 4;
        m_ptKey = new Point2d[m_nCountKeyPoints];
        if (m_ptKey == null)
            return false;


        Line lnTemp1 = new Line(pstStart, 1000.0f);
        Line lnTemp2 = new Line(pstEnd, 1000.0f);

        if (lnTemp1 == null || lnTemp2 == null) {
            return false;
        }

        Point2d pt = new Point2d();
        float fLen1, fLen2;


        //判断起点终点延长线有无交点
        if (!lnTemp1.Intersect(lnTemp2, pt, false, false, 1e-4f)) {
            //如果五交点
            Point2d fTemp = new Point2d();
            lnTemp1.DistanceToPoint(false, pstEnd, fTemp, pt);
            float fDist = pstStart.DistanceTo(pt);
            if (fDist < 0.05f)
                return false;

            fLen1 = fLen2 = fDist * 0.5f;
        } else {
            //如果有交点
            Angle angDiff = new Angle(0);
            angDiff.m_fRad = pstEnd.GetAngle().m_fRad - pstStart.GetAngle().m_fRad;
            float fPhi = angDiff.m_fRad;
            if (fPhi > Line.PI)
                fPhi = 2 * Line.PI - fPhi;

            if (fPhi > Line.PI / 2)
                fPhi = Line.PI - fPhi;

            fPhi /= 2;

            if (fPhi < Line.PI / 9) {
                float fDist = pstStart.DistanceTo(pstEnd);
                fLen1 = fLen2 = fDist / 2;
            } else {
                //为了控制曲线形状，将控制点与起点终点的距离减小（防止打卷）
                fLen1 = pstStart.DistanceTo(pt) * k;
                fLen2 = pstEnd.DistanceTo(pt) * k;
                fLen1 *= Math.abs(Math.tan(fPhi));
                fLen2 *= Math.abs(Math.tan(fPhi));
            }
        }

        if (fLen1 < 0.1f) {
            fLen1 = pstStart.DistanceTo(pstEnd) * k;
        }

        if (fLen2 < 0.1f) {
            fLen2 = pstStart.DistanceTo(pstEnd) * k;
        }


        m_ptKey[0] = pstStart;
        m_ptKey[3] = pstEnd;


        Line ln1 = new Line(pstStart, fLen1);
        m_ptKey[1] = ln1.GetEndPoint();


        Angle ang = new Angle(0);
        ang.m_fRad = pstEnd.GetAngle().m_fRad + Line.PI;

        //	Line ln2 = new Line(pstEnd, !pstEnd.GetAngle(), fLen2);
        Line ln2 = new Line(pstEnd, ang, fLen2);
        m_ptKey[2] = ln2.GetEndPoint();

//			m_nSampleCount = BEZIER_CURVE_DEFAULT_SEG_COUNT;

        float[] param = BezierOptParams.getInstance().GetAllParam();
        InitFindOpt(param);
        return CreateSamplePoints();
    }


    public boolean CreateSamplePoints() {

        if (m_pSamplePoints != null)
            m_pSamplePoints = null;

        m_pSamplePoints = new CurveSamplePoint[m_nSampleCount];

        m_fTotalLen = 0;
        for (int i = 0; i < m_nSampleCount; i++) {
            float t = (float) i / m_nSampleCount;
            SetCurT(t); //计算此时的t所对应的采样点的坐标、曲率

            float fSegLen = (float) (Math.sqrt(dx1 * dx1 + dy1 * dy1) * 1.0f / m_nSampleCount);    // 1. 求每个采样段的长度

            m_fTotalLen += fSegLen;  //2. 积分，求整个曲线的长度

            m_pSamplePoints[i] = new CurveSamplePoint();
            CurveSamplePoint sp = m_pSamplePoints[i];
            sp.t = t;
            sp.fProgress = m_fTotalLen;
            sp.fSegLen = fSegLen;
//				sp.GetPoint2dObject() = TrajFun();
            sp.x = TrajFun().x;
            sp.y = TrajFun().y;
            sp.fTangentAngle = TangentFun().m_fRad;
            sp.fCurvature = CurvatureFun();
        }

        return true;
    }



    public void SetCurT(float t) {

        int n = m_nCountKeyPoints - 1; // 从3开始
        m_pt = new Point2d();
        m_pt.x = m_pt.y = 0;


        /**
         * 三阶贝塞尔曲线
         * B(t) = P0 * (1-t)^3 + 3 * P1 * t * (1-t)^2 + 3 * P2 * t^2 * (1-t) + P3 * t^3, t ∈ [0,1]
         *
         * @param t  曲线长度比例
         * @param p0 起始点
         * @param p1 控制点1
         * @param p2 控制点2
         * @param p3 终止点
         * @return t对应的点
         */
        //求此时的t所对应的采样点坐标
        for (int i = 0; i <= n; i++) {
            int c = Combination(n, i);
            float p1 = FloatPowerInt(1 - t, n - i);
            float p2 = FloatPowerInt(t, i);
            float f = c * p1 * p2;
            m_pt.x += m_ptKey[i].x * f;
            m_pt.y += m_ptKey[i].y * f;
        }


        //一阶导数（对t求导）
        dx1 = dy1 = 0;
        for (int i = 0; i <= n; i++) {
            int c = Combination(n, i);
            float p1 = FloatPowerInt(1 - t, n - i - 1);
            float p2 = FloatPowerInt(t, i);
            float p3 = FloatPowerInt(t, i - 1);
            float p4 = FloatPowerInt(1 - t, n - i);

            float f = -(n - i) * p1 * p2 + i * p4 * p3;
            dx1 += c * m_ptKey[i].x * f;
            dy1 += c * m_ptKey[i].y * f;
        }


        //二阶导数（对t求导）
        dx2 = 6 * (m_ptKey[2].x - 2 * m_ptKey[1].x + m_ptKey[0].x) +
                6 * (m_ptKey[3].x - 3 * m_ptKey[2].x + 3 * m_ptKey[1].x - m_ptKey[0].x) * t;

        dy2 = 6 * (m_ptKey[2].y - 2 * m_ptKey[1].y + m_ptKey[0].y) +
                6 * (m_ptKey[3].y - 3 * m_ptKey[2].y + 3 * m_ptKey[1].y - m_ptKey[0].y) * t;


        float _dx2, _dy2;
        _dx2 = _dy2 = 0;
        for (int i = 0; i <= n; i++) {
            int c = Combination(n, i);
            float p1 = (n - i - 1 >= 0) ? FloatPowerInt(1 - t, n - i - 1) : 0;
            float p2 = FloatPowerInt(t, i);
            float p3 = FloatPowerInt(t, i - 1);
            float p4 = FloatPowerInt(1 - t, n - i);
            float p5 = (i - 2 >= 0) ? FloatPowerInt(t, i - 2) : 0;
            float p6 = (n - i - 2 >= 0) ? FloatPowerInt(1 - t, n - i - 2) : 0;

            float f = (n - i) * ((-(n - i - 1)) * p6 * p2 + p1 * i * p3) +
                    i * ((-n + i) * p1 * p3 + p4 * (i - 1) * p5);
            _dx2 += c * m_ptKey[i].x * f;
            _dy2 += c * m_ptKey[i].y * f;
        }



        float f = dx1 * dx1 + dy1 * dy1;
        f = (float) Math.sqrt(f * f * f);
        m_fCurvature = (dx1 * dy2 - dx2 * dy1) / f;  ////////计算每个点的曲率

        if (Math.abs(m_fCurvature) > 3) {
            m_bCurvature = false;
        }

        m_angTangent = new Angle(0);
        m_angTangent.m_fRad = (float) Math.atan2(dy1, dx1);
    }


    float GetTFromProgress(float fLen) {
        float t;

        if (fLen == 0)
            return 0;
        else if (fLen >= m_fTotalLen)
            return 1;

        return 1;
    }

    //
//   计算曲线外一点pt到曲线上最近距离的点
//
    public boolean GetClosestPoint(Point2d pt, Point2d pptClosest, float CurT) {
        //2019.10.29
        CreateSamplePoints();
        ///////////////
        // 先假定第一点为最近点
        int nClosest = 0;
        Point2d ptClosestSample = new Point2d();
        ptClosestSample = m_pSamplePoints[0].GetPoint2dObject();

        // 依次在各采样点中查找距离给定点pt最近的点
//        float fMinDist = ptClosestSample.DistanceTo(pt);
        float fMinDist = (float) Math.hypot(ptClosestSample.x - pt.x, ptClosestSample.y - pt.y);
        for (int i = 1; i < m_nSampleCount; i++) {
            // 取得一个采样点
            Point2d ptSample = m_pSamplePoints[i].GetPoint2dObject();

            // 计算采样点到给定点之间的距离
//            float fDist = ptSample.DistanceTo(pt);

            float fDist = (float) Math.hypot(ptSample.x - pt.x, ptSample.y - pt.y);

            // 更新最近点和最近距离
            if (fDist < fMinDist) {
                ptClosestSample.x = pt.x;
                ptClosestSample.y = pt.y;
                fMinDist = fDist;
                nClosest = i;
            }
        }

        // 下面通过插补方法，以更高的精度计算最近点
        Point2d pt1, pt2;
        float t1, t2;

        // 如果最近采样点是第一个采样点
        if (nClosest == 0) {
            pt1 = m_pSamplePoints[0];
            pt2 = m_pSamplePoints[1];

            t1 = m_pSamplePoints[0].t;
            t2 = m_pSamplePoints[1].t;
        }
        // 如果最近采样点是最后一个采样点
        else if (nClosest == m_nSampleCount - 1) {
            pt1 = m_pSamplePoints[m_nSampleCount - 1];
            pt2 = m_ptKey[m_nCountKeyPoints - 1];

            t1 = m_pSamplePoints[m_nSampleCount - 1].t;
            t2 = 1;
        }

        // 如果最近采样点不在曲线的两端
        else {
            // 求得曲线与当前最近采样点相邻的两个采样点及其对应的t值
            pt1 = m_pSamplePoints[nClosest - 1];
            pt2 = m_pSamplePoints[nClosest + 1];

            t1 = m_pSamplePoints[nClosest - 1].t;
            t2 = m_pSamplePoints[nClosest + 1].t;
        }
        //2019.10.24
        //如果两点距离过小，不能求解
        float fTotalLen = pt1.DistanceTo(pt2);

        //if (fTotalLen < MIN_LINE_LEN)
        if (fTotalLen < (1e-3F))
            return false;

        ////////////////////
        // 构造直线
        Line ln = new Line(pt1, pt2);

        // 计算当前姿态到上述直线的投影点
        Point2d ptFoot = new Point2d();

        //float fLambda = 0.0f;
        Point2d fLambda = new Point2d();
        // 计算指定点到短直线的投影点
        ln.DistanceToPoint(false, pt, fLambda, ptFoot);

        if (fLambda.x < 0) {
            if (nClosest == 0 && ptFoot.DistanceTo(m_ptKey[0]) < 0.03f) {
                if (pptClosest != null) {
                    pptClosest = m_ptKey[0];
                    CurT = 0;
                }
                return true;
            } else
                return false;
        } else if (fLambda.x > 1) {
            if (nClosest == m_nSampleCount - 1 && ptFoot.DistanceTo(m_ptKey[m_nCountKeyPoints - 1]) < 0.03f) {
                if (pptClosest != null) {
                    pptClosest = m_ptKey[m_nCountKeyPoints - 1];
                    CurT = 1;
                }

                return true;
            } else
                return false;
        } else {
            // 计算投影点到直线两端的距离
            float d1 = ptFoot.DistanceTo(pt1);
            float d2 = ptFoot.DistanceTo(pt2);

            // 得到(修正后的)最近点所对应的t
            float t = (d1 * t2 + d2 * t1) / (d1 + d2);

            // 计算得到修正后的最近点
            SetCurT(t);

            if (pptClosest != null) {
                pptClosest.x = ptFoot.x;
                pptClosest.y = ptFoot.y;
                CurT = t;
            }
            return true;
        }
    }

    public boolean ISInRect(double minx, double miny, double maxx, double maxy) {
        double dleft, dright, dtop, dbottom;
        dleft = m_ptKey[0].x;
        dright = m_ptKey[0].x;
        dtop = m_ptKey[0].y;
        dbottom = m_ptKey[0].y;
        for (int i = 1; i < 4; i++) {
            if (i == 1 || i == 2)  //不考虑控制点
            {
                continue;
            }

            if (dleft > m_ptKey[i].x)
                dleft = m_ptKey[i].x;

            if (dright < m_ptKey[i].x)
                dright = m_ptKey[i].x;

            if (dtop < m_ptKey[i].y)
                dtop = m_ptKey[i].y;

            if (dbottom > m_ptKey[i].y)
                dbottom = m_ptKey[i].y;

        }

        if (dleft > minx && dright < maxx && dtop < maxy && dbottom > miny) {
            return true;
        } else
            return false;
    }

    public void InitFindOpt(float[] param) {
        float tempMin = 1000000;
        float fMaxRudderSaltAng = param[0];
        int iRudderNum = 1;
        Vector<Point2d> cRudderSite = new Vector<>();
        Point2d pnt = new Point2d();
        pnt.x = param[1];
        pnt.y = param[2];
        cRudderSite.add(pnt);
        //???????????????????????
        float fRudderAng = param[4];
        //舵轮每秒最大打舵角度*0.05=AGV舵角单周期最大突变量
        float fMaxRudderSal = (float) (fRudderAng * 0.05);

        float fMaxLineRate = param[3];
        float fMaxAngRate = param[4];

//		if (fMaxRudderSaltAng > 70.0)
//		{
//			fMaxRudderSaltAng = 70.0f;
//		}
        for (int i = 0; i < iRudderNum; i++) {
            float temp = (float) (1 / (cRudderSite.get(i).y / Math.tan(fMaxRudderSaltAng * 0.01745329251994329575) + cRudderSite.get(i).x));
            if (tempMin > temp) {
                tempMin = temp;
            }
        }
        float fMaxCurvature = tempMin;
        float fMaxCurRate = 0.0f;
        if (fMaxRudderSal > 0.05 && fMaxRudderSal < 0.1) {
            fMaxCurRate = 0.05f;
        }
        if (fMaxRudderSal > 0.1) {
            fMaxCurRate = 0.1f;
        }

        Line lnS = new Line(m_ptKey[0], m_ptKey[1]);
        Line lnE = new Line(m_ptKey[m_nCountKeyPoints - 2], m_ptKey[m_nCountKeyPoints - 1]);

        if (lnS.m_angSlant == null || lnE.m_angSlant == null)
            return;

//	FindOptControlPoint findOptControlPoint;
        findOptControlPoint.SetPosturePoint(m_ptKey[0].x, m_ptKey[0].y, lnS.GetSlantAngle().m_fRad,
                m_ptKey[m_nCountKeyPoints - 1].x, m_ptKey[m_nCountKeyPoints - 1].y, lnE.GetSlantAngle().m_fRad);

        findOptControlPoint.SetAgvParamLimit(fMaxLineRate, fMaxAngRate, fMaxCurvature);//0228
        //findOptControlPoint.SetRudderSaltation(WDEG2RAD(10));//dRudderSaltation 舵轮最大打舵角度 单位度 角度
//		findOptControlPoint.SetAgvCurvaGradientMax(fMaxCurRate);
        findOptControlPoint.SetParam(param);


    }

    //
    // 曲线优化，生成满足约束的控制点
    //
    public boolean BezierOptic() {
// 起点终点位姿设置
        //将起点和第一个控制点连线
        Line lnS = new Line(m_ptKey[0], m_ptKey[1]);
        //将最后一个控制点和终点连线
        Line lnE = new Line(m_ptKey[m_nCountKeyPoints - 2], m_ptKey[m_nCountKeyPoints - 1]);
        //保存连线交点
        Point2d temppt = new Point2d();
        //fSdist:起点到交点的距离
        float fSdist;
        float ftdist = 0;
        //根据起点和终点，找到交点。
        //如果两条线是平行的（S型曲线），则返回值为false。即es为false
        boolean es = lnS.IntersectLineAtEx(lnE, temppt, ftdist);
        fSdist = temppt.DistanceTo(m_ptKey[0]);
        //fEdist：交点到终点的距离
        float fEdist = temppt.DistanceTo(m_ptKey[m_nCountKeyPoints - 1]);
        //fStoEDist：起点到终点的距离
        float fStoEDist = m_ptKey[0].DistanceTo(m_ptKey[m_nCountKeyPoints - 1]);
        boolean bTempOK = false;
        if (fSdist > (fStoEDist / 5) && fEdist > (fStoEDist / 5))
            bTempOK = true;
        //如果不是：S型曲线或者交点太远了的情况，则进入第一个函数。如果是，则进入第二个函数
        if (fSdist <= fStoEDist && fEdist <= fStoEDist && es == true && bTempOK) {
            findOptControlPoint.SetOptCtrltoSE(true, temppt.x, temppt.y, fSdist, fEdist); //控制点由交点向两端推进，true-启用, temppt-交点坐标，fSdist-起点到交点距离，fEdist-终点到交点距离
        } else
            findOptControlPoint.SetOptCtrltoSE(false, temppt.x, temppt.y, fSdist, fEdist);//控制点有两端向外推进，比例基数为起点到终点的距离
        ///////////////////////////////////////////////////////////////////////////////////////
        //端点切向相交处向两端方向寻找最优位置 两端dLamda依旧保持值相同 TestFor20200406
        boolean ret = findOptControlPoint.FindOptCtrlPtStEdSameLamda(); //寻找最有控制点起始函数

        if (ret) {
            for (int i = 0; i < findOptControlPoint.m_vecMiddleCtrlPt.size(); i++) {
                m_ptKey[i + 2].x = findOptControlPoint.m_vecMiddleCtrlPt.get(i).x;
                m_ptKey[i + 2].y = findOptControlPoint.m_vecMiddleCtrlPt.get(i).y;
            }
            m_ptKey[1].x = findOptControlPoint.cpStartCtrlPt.x;
            m_ptKey[1].y = findOptControlPoint.cpStartCtrlPt.y;
            m_ptKey[m_nCountKeyPoints - 2].x = findOptControlPoint.cpEndCtrlPt.x;
            m_ptKey[m_nCountKeyPoints - 2].y = findOptControlPoint.cpEndCtrlPt.y;
        }
        //20200210 贝塞尔合理性判断设置
//        SetLegitimate(ret);//20200210 设置贝塞尔合理性，将ret传给某个值（表示贝塞尔曲线是否合理）
        //会返回曲线是否合理
        return ret;
    }

//    public void SetLegitimate(boolean legitimate) {
//        isLegitimate = legitimate;
//    }

    public void Draw(CoordinateConversion ScrnRef, Canvas Grp, int cr, int nWidth, int nPointSize, boolean bShowKeyPoints) {

        Paint paint = new Paint();
        paint.setAntiAlias(true);



        paint.setColor(cr);

        paint.setStyle(Paint.Style.STROKE);

        paint.setStrokeWidth(nWidth);

//
        Path path = new Path();
        PointF mStart = ScrnRef.worldToScreen(m_ptKey[0].x, m_ptKey[0].y);
        PointF mControl1 = ScrnRef.worldToScreen(m_ptKey[1].x, m_ptKey[1].y);
        PointF mControl2 = ScrnRef.worldToScreen(m_ptKey[2].x, m_ptKey[2].y);
        PointF mEnd = ScrnRef.worldToScreen(m_ptKey[3].x, m_ptKey[3].y);
//        int mStartX = ScrnRef.GetWindowPoint(new Point2d(, )).x;

        path.moveTo(mStart.x, mStart.y);
        path.cubicTo(mControl1.x, mControl1.y, mControl2.x, mControl2.y, mEnd.x, mEnd.y);
        Grp.drawPath(path, paint);


        // 绘制采样点
//		CreateSamplePoints();
//		for (int i = 0; i < m_nSampleCount; i++) {
//			Point2d pnt = new Point2d();
//			pnt.x = m_pSamplePoints[i].x;
//			pnt.y = m_pSamplePoints[i].y;
//			pnt.Draw(ScrnRef, Grp, Color.RED, 1);
//		}
    }

    //
    //  画出控制点
    //
    public void DrawCtrlPoints(CoordinateConversion ScrnRef, Canvas Grp, Typeface pLogFont, int cr, int nWidth) {

        for (int i = 1; i < m_nCountKeyPoints - 1; i++) {
//			DrawCtrlID(i,ScrnRef, Grp,pLogFont, cr, nWidth);
            m_ptKey[i].Draw(ScrnRef, Grp, cr, nWidth);
        }
    }

    //
    // 画出控制点编号
    //
    public void DrawCtrlID(int nKeyID, CoordinateConversion ScrnRef, Canvas Grp, Typeface pLogFont, int cr, int nWidth) {
        if (nKeyID <= 0 || nKeyID >= m_nCountKeyPoints - 1)
            return;

        PointF pnt1 = ScrnRef.screenToWorld(m_ptKey[nKeyID].x, m_ptKey[nKeyID].y);
        String str;
        str = Integer.toString(nKeyID);

        Paint paint = new Paint();
        paint.setTypeface(pLogFont);
        paint.setTextSize(25); //设置画笔字体的大小
        Color clr = new Color();
        paint.setColor(clr.rgb(0, 255, 255));
        Grp.drawText(str, pnt1.x + 4, pnt1.y + 14, paint);
    }

    //
    // 临时得到采样点数据,后续删除
    //
    public Vector<Point2d> GetSimPoint() {
        Vector<Point2d> pnt = new Vector<Point2d>();
        for (int i = 0; i < m_nSampleCount; i++) {
            Point2d pntT = new Point2d();
            pntT.x = m_pSamplePoints[i].x;
            pntT.y = m_pSamplePoints[i].y;
            pnt.add(pntT);
        }
        return pnt;
    }
}
