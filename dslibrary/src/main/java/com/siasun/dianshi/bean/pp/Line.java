package com.siasun.dianshi.bean.pp;


import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

import com.siasun.dianshi.bean.Point2d;
import com.siasun.dianshi.utils.CoordinateConversion;

import java.io.Serializable;


public class Line implements Serializable {

    public static final float PI = ((float) 3.14159265);
    public int m_nId;            // ID��
    public Point2d m_ptStart;        // The start point
    public Point2d m_ptEnd;          // The end point
    public Angle m_angSlant;       // Slant angle
    public float m_fTotalLen;      // The length of the line

    public final float MIN_LINE_LEN = 1e-6F;

    public Line() {
        m_ptStart = new Point2d();
        m_ptEnd = new Point2d();
        m_angSlant = new Angle(0);
    }


    public Line(final Point2d ptStart, final Point2d ptEnd) {
        m_ptStart = new Point2d();
        m_ptEnd = new Point2d();
        Create(ptStart, ptEnd);
    }

    public Line(final Point2d ptStart, final Angle angSlant, float fTotalLen) {
        m_ptStart = new Point2d();
        m_ptEnd = new Point2d();
        Create(ptStart, angSlant, fTotalLen);
    }


    public Line(final Posture pstStart, float fTotalLen) {
        m_ptStart = new Point2d();
        m_ptEnd = new Point2d();
        Create(pstStart, pstStart.GetAngle(), fTotalLen);
    }


    public boolean Create(final Point2d ptStart, final Point2d ptEnd) {
        // ����ֱ�߳���
        float fTotalLen = ptStart.DistanceTo(ptEnd);

        // ���ֱ��̫�̣�����false
        if (fTotalLen < MIN_LINE_LEN) return false;

        // �������/�յ�/����
        m_ptStart = ptStart;
        m_ptEnd = ptEnd;
        m_fTotalLen = fTotalLen;
        m_nId = 0;

        m_angSlant = new Angle(0);
        // ����ֱ�ߵ����
        m_angSlant.m_fRad = (float) (Math.atan2(m_ptEnd.y - m_ptStart.y, m_ptEnd.x - m_ptStart.x));

        return true;
    }

    //ȡ��ֱ�ߵ���б��
    public Angle GetSlantAngle() {
        return m_angSlant;
    }

    public boolean Create(final Point2d ptStart, final Angle angSlant, float fTotalLen) {
        // ֱ�߲��ܹ���
        if (fTotalLen < MIN_LINE_LEN) return false;

        // �����յ�����
        Point2d ptEnd = new Point2d();
        ptEnd.x = (float) (ptStart.x + fTotalLen * Math.cos(angSlant.m_fRad));
        ptEnd.y = (float) (ptStart.y + fTotalLen * Math.sin(angSlant.m_fRad));

        // ������������ֱ�߶�
        return Create(ptStart, ptEnd);
    }


    // ������ֹ��
    public Point2d GetEndPoint() {
        return m_ptEnd;
    }


    //ȡ��ֱ�߶εĳ���
    public float Length() {
        return m_fTotalLen;
    }


    public Point2d TrajFun(float fCurLen) {
        Point2d pt = new Point2d();
        pt.x = m_ptStart.x;
        pt.y = m_ptStart.y;
        pt.x += fCurLen * Math.cos(m_angSlant.m_fRad);
        pt.y += fCurLen * Math.sin(m_angSlant.m_fRad);

        return pt;
    }


    public boolean Intersect(final Line line2, Point2d pnt, boolean onSegment1, boolean onSegment2, float fSmallGate) {
        float l1dx, l1dy, l2dx, l2dy, det, ldx1, ldy1, lambda1, lambda2;

        l1dx = m_ptEnd.x - m_ptStart.x;
        l1dy = m_ptEnd.y - m_ptStart.y;
        l2dx = line2.m_ptEnd.x - line2.m_ptStart.x;
        l2dy = line2.m_ptEnd.y - line2.m_ptStart.y;
        det = l1dy * l2dx - l1dx * l2dy;

        if (Math.abs(det) < fSmallGate) return false;

        ldx1 = m_ptStart.x - line2.m_ptStart.x;
        ldy1 = m_ptStart.y - line2.m_ptStart.y;
        lambda1 = (ldx1 * l2dy - ldy1 * l2dx) / (l1dy * l2dx - l1dx * l2dy);
        lambda2 = (ldx1 * l1dy - ldy1 * l1dx) / (l1dy * l2dx - l1dx * l2dy);

        pnt.x = m_ptStart.x + l1dx * lambda1;

        pnt.y = m_ptStart.y + l1dy * lambda1;

        if (onSegment1 != false) onSegment1 = (lambda1 >= 0.0 && lambda1 <= 1.0);

        if (onSegment2 != false) onSegment2 = (lambda2 >= 0.0 && lambda2 <= 1.0);

        return true;
    }

    //判断投影点是否在线段内
    public boolean FootOnSegment(Point2d pnt) {
        float minX = Math.min(m_ptStart.x, m_ptEnd.x);
        float maxX = Math.max(m_ptStart.x, m_ptEnd.x);
        float minY = Math.min(m_ptStart.y, m_ptEnd.y);
        float maxY = Math.max(m_ptStart.y, m_ptEnd.y);
        if (minX <= pnt.x && pnt.x <= maxX && minY <= pnt.y && pnt.y <= maxY) {
            return true;
        } else return false;
    }


    //
    //�����ֱ�ߵ�ָ����ľ��롣
    //
    public float DistanceToPoint(boolean bIsSegment, final Point2d pt, Point2d pLambda, Point2d pFootPoint) {
        float dx = m_ptEnd.x - m_ptStart.x;
        float dy = m_ptEnd.y - m_ptStart.y;
        float d2 = dx * dx + dy * dy;

        float lambda = ((pt.y - m_ptStart.y) * dy + (pt.x - m_ptStart.x) * dx) / d2;

        //	if (pLambda != null)
        //	if (pLambda != 0.0f)
        pLambda.x = lambda;

        if (bIsSegment) {
            /* make sure point is on line (lambda <- [0..1]) */
            if (lambda < 0) lambda = 0;
            else if (lambda > 1) lambda = 1.0f;
        }

        float x = m_ptStart.x + lambda * dx;
        float y = m_ptStart.y + lambda * dy;

        if (pFootPoint != null) {
            pFootPoint.x = x;
            pFootPoint.y = y;
        }

        return (float) Math.hypot(pt.x - x, pt.y - y);
    }

    public boolean IsParallelTo(Line Line, float fMaxAngDiff) {
        if (fMaxAngDiff == 0) {
            fMaxAngDiff = Angle.m_fReso;
        }
        //		fMaxAngDiff = Angle::m_fReso;

        Angle ang1 = Line.GetSlantAngle();
        Angle m_angSlantT = new Angle((float) (m_angSlant.m_fRad + Math.PI));
        if (m_angSlant.ApproxEqualTo(ang1, fMaxAngDiff) || (m_angSlantT).ApproxEqualTo(ang1, fMaxAngDiff))
            return true;
        else return false;
    }

    //
//   判断一个点是否在此直线(段)上。
// !!!!!!!!! 下面的计算可能有问题，当ln1, ln2非常短时，可能会出现错误结果!!!!
//
    public boolean ContainPoint(Point2d pt, boolean bExtend) {
        //if (pt == m_ptStart || pt == m_ptEnd)
        //	return true;
        if ((Math.abs(pt.x - m_ptStart.x) < 0.001 && Math.abs(pt.y - m_ptStart.y) < 0.001) || (Math.abs(pt.x - m_ptEnd.x) < 0.001 && Math.abs(pt.y - m_ptEnd.y) < 0.001))
            return true;

        // !!!!!!!!! 下面的计算可能有问题，当ln1, ln2非常短时，可能会出现错误结果!!!!
        Line ln1 = new Line(pt, m_ptStart);
        Line ln2 = new Line(pt, m_ptEnd);

        // bExtend: 允许点落在线段的两端延长线上
        if (bExtend) {
            Angle m_angSlantT = new Angle((float) (ln1.GetSlantAngle().m_fRad + Math.PI));
            //	if (ln1.GetSlantAngle() == m_angSlant || !ln1.GetSlantAngle() == m_angSlant)
            //	if (ln1.GetSlantAngle() == m_angSlant || m_angSlantT == m_angSlant)
            float fTemp = (float) Math.abs(ln1.GetSlantAngle().m_fRad - m_angSlant.m_fRad);
            boolean ret1 = false;
            boolean ret2 = false;
            if (fTemp < Angle.m_fReso || 2 * PI - fTemp < Angle.m_fReso) ;
            {
                ret1 = true;
            }

            fTemp = (float) Math.abs(m_angSlantT.m_fRad - m_angSlant.m_fRad);
            if (fTemp < Angle.m_fReso || 2 * PI - fTemp < Angle.m_fReso) ;
            {
                ret2 = true;
            }

            if (ret1 || ret2) return true;
        }

        // 只允许点落在线段以内
        else {
            Angle m_angSlantT = new Angle((float) (m_angSlant.m_fRad + Math.PI));
            //	if ((ln1.GetSlantAngle() == m_angSlant) && (ln2.GetSlantAngle() == !m_angSlant))
            //	if ((ln1.GetSlantAngle() == m_angSlant) && (ln2.GetSlantAngle() == m_angSlantT))
            float fTemp = (float) Math.abs(ln1.GetSlantAngle().m_fRad - m_angSlant.m_fRad);
            boolean ret1 = false;
            boolean ret2 = false;
            if (fTemp < Angle.m_fReso || 2 * PI - fTemp < Angle.m_fReso) ;
            {
                ret1 = true;
            }

            fTemp = (float) Math.abs(ln2.GetSlantAngle().m_fRad - m_angSlantT.m_fRad);
            if (fTemp < Angle.m_fReso || 2 * PI - fTemp < Angle.m_fReso) ;
            {
                ret2 = true;
            }

            if (ret1 || ret2) return true;
            //	else if ((ln2.GetSlantAngle() == m_angSlant) && (ln1.GetSlantAngle() == !m_angSlant))
            //	else if ((ln2.GetSlantAngle() == m_angSlant) && (ln1.GetSlantAngle() == m_angSlantT))
            fTemp = (float) Math.abs(ln2.GetSlantAngle().m_fRad - m_angSlant.m_fRad);
            ret1 = false;
            ret2 = false;
            if (fTemp < Angle.m_fReso || 2 * PI - fTemp < Angle.m_fReso) ;
            {
                ret1 = true;
            }

            fTemp = (float) Math.abs(ln1.GetSlantAngle().m_fRad - m_angSlantT.m_fRad);
            if (fTemp < Angle.m_fReso || 2 * PI - fTemp < Angle.m_fReso) ;
            {
                ret2 = true;
            }

            if (ret1 || ret2) return true;
        }

        return false;
    }

    // 取得两条直线的交点。允许在延长线上 20200406
    public boolean IntersectLineAtEx(Line Line, Point2d pt, float fDist) {
        Point2d pt1 = new Point2d();
        float k1, k2;
        //判断两直线是否平行
        if (IsParallelTo(Line, 0)) return false;
        // 情形1：第一条直线平行于Y轴
        if (Math.abs(m_ptEnd.x - m_ptStart.x) < MIN_LINE_LEN) {
            if (Math.abs(Line.m_ptEnd.x - Line.m_ptStart.x) < MIN_LINE_LEN) {
                //ASSERT(false);               // 两线均平行于Y轴，不可能在此出现
            } else {
                k2 = (Line.m_ptEnd.y - Line.m_ptStart.y) / (Line.m_ptEnd.x - Line.m_ptStart.x);
                pt1.x = m_ptEnd.x;
                pt1.y = Line.m_ptStart.y + (pt1.x - Line.m_ptStart.x) * k2;
            }
        }
        // 情形2：第二条直线平行于Y轴
        else if (Math.abs(Line.m_ptEnd.x - Line.m_ptStart.x) < MIN_LINE_LEN) {
            if (Math.abs(m_ptEnd.x - m_ptStart.x) < MIN_LINE_LEN) {
                //ASSERT(false);               // 两线均平行于Y轴，不可能在此出现
            } else {
                k1 = (m_ptEnd.y - m_ptStart.y) / (m_ptEnd.x - m_ptStart.x);
                pt1.x = Line.m_ptEnd.x;
                pt1.y = m_ptStart.y + (pt1.x - m_ptStart.x) * k1;
            }
        }
        // 情形3：两条直线均不平行于Y轴
        else {
            k1 = (m_ptEnd.y - m_ptStart.y) / (m_ptEnd.x - m_ptStart.x);
            k2 = (Line.m_ptEnd.y - Line.m_ptStart.y) / (Line.m_ptEnd.x - Line.m_ptStart.x);

            pt1.x = (Line.m_ptStart.y - m_ptStart.y + k1 * m_ptStart.x - k2 * Line.m_ptStart.x) / (k1 - k2);
            pt1.y = m_ptStart.y + (pt1.x - m_ptStart.x) * k1;
        }
        // 交点需要处于两条线段以内
        if (!ContainPoint(pt1, true) || !Line.ContainPoint(pt1, true)) return false;
        pt.x = pt1.x;
        pt.y = pt1.y;
        fDist = pt1.DistanceTo(m_ptStart);
        return true;
    }

    //
//����Ļ�ϻ��ƴ�ֱ�ߡ�
//
    public void Draw(CoordinateConversion ScrnRef, Canvas Grp, int color, int nWidth, int nPointSize, boolean bBigVertex, Paint paint) {
//        Paint paint = new Paint();
        paint.setAntiAlias(true);
        /*����paint����ɫ*/
        paint.setColor(color);
        /*����paint�� style ΪSTROKE������*/
        paint.setStyle(Paint.Style.STROKE);
        /*����paint�������*/
        paint.setStrokeWidth(nWidth);


        PointF pnt1 = ScrnRef.worldToScreen(m_ptStart.x, m_ptStart.y);
        PointF pnt2 = ScrnRef.worldToScreen(m_ptEnd.x, m_ptEnd.y);

        Grp.drawLine(pnt1.x, pnt1.y, pnt2.x, pnt2.y, paint);

        if (bBigVertex) {
            m_ptStart.Draw(ScrnRef, Grp, color, nPointSize, paint);
            m_ptEnd.Draw(ScrnRef, Grp, color, nPointSize, paint);
        }
    }

    @Override
    public String toString() {
        return "Line{" + "m_ptStart=" + m_ptStart + ", m_ptEnd=" + m_ptEnd + '}';
    }
}
