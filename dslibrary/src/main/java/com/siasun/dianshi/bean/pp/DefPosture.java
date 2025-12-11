package com.siasun.dianshi.bean.pp;

import static java.lang.Math.PI;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;

import com.siasun.dianshi.utils.CoordinateConversion;

import java.util.Vector;


public class DefPosture {
    public final int POSTURE_ARROW_LEN = 2;
    //用户定义关键位姿
    public Vector<Posture> m_PstV = new Vector();//装载的坐标是世界坐标
    public int m_SelectPstID = -1;   //以Vector的序号为准

    public DefPosture() {

    }

    public void AddPst(Posture pst) {
        m_PstV.add(pst);
        m_SelectPstID = -1;
    }

    //删除选择节点
    public void DeletePst() {
        if (m_SelectPstID != -1) {
            m_PstV.remove(m_SelectPstID);
        }
    }

    public Posture GetPst() {
        if (m_SelectPstID != -1) {
            Posture pst = new Posture();
            pst.x = m_PstV.get(m_SelectPstID).x;
            pst.y = m_PstV.get(m_SelectPstID).y;
            pst.fThita = m_PstV.get(m_SelectPstID).fThita;
            return pst;
        }
        return null;
    }


    //判断当前点是否选择关键位姿
    public int PointHitPst(CoordinateConversion mScr, Point pnt) {
        float x, y;
        x = pnt.x;
        y = pnt.y;
        int index = -1;
        for (int i = 0; i < m_PstV.size(); i++) {
            Point pt = new Point();
            pt.x = (int) x;
            pt.y = (int) y;
            if (m_PstV.get(i).PointHitTest(pt, mScr)) {
                index = i;
                break;
            }
        }
        m_SelectPstID = index;
        return index;
    }

    //判断点是否在选择位姿的箭头上
    public Boolean CheckPointOnPostureArrowTip(CoordinateConversion mScr, Point point) {
        Boolean isOnArrow = false;
        if (m_SelectPstID == -1 || m_PstV == null) {
            return isOnArrow;
        }

        //此处先保护一下，崩溃原因未知
        if (m_SelectPstID > m_PstV.size() - 1) {
            return isOnArrow;
        }

        Posture pst = m_PstV.get(m_SelectPstID);
        Line ln = new Line(pst, POSTURE_ARROW_LEN);
//        Point pointTip = mScr.GetWindowPoint(ln.m_ptEnd);

        PointF pointTip = mScr.worldToScreen(ln.m_ptEnd.x, ln.m_ptEnd.y);
        if (Math.abs(pointTip.x - point.x) < 20 && Math.abs(pointTip.y - point.y) < 20) {
            isOnArrow = true;
        }
        return isOnArrow;
    }

    //清除
    public void Clear() {
        m_PstV.clear();
    }

    public void Draw(CoordinateConversion mScr, Canvas Grp, Paint paint) {
        paint.setColor(Color.GREEN);//笔的颜色
        paint.setStyle(Paint.Style.FILL);//实心线
        for (int i = 0; i < m_PstV.size(); i++) {

            PointF pointT = mScr.worldToScreen(m_PstV.get(i).x, m_PstV.get(i).y);
            RectF ret = new RectF(pointT.x - 10, pointT.y - 10, pointT.x + 10, pointT.y + 10);
            if (i == m_SelectPstID) {
                paint.setColor(Color.RED);//笔的颜色
            } else {
                paint.setColor(Color.GREEN);//笔的颜色
            }
            Grp.drawOval(ret, paint);

            float fArrowLen = POSTURE_ARROW_LEN;
            Line ln = new Line(m_PstV.get(i), fArrowLen);
            Angle ang1 = new Angle(0.0f);
            ang1.m_fRad = (float) (PI + ln.m_angSlant.m_fRad);
            Angle ang2 = new Angle(0.0f);
            ang2.m_fRad = (float) (ang1.m_fRad + PI / 18);
            Angle ang3 = new Angle(0.0f);
            ang3.m_fRad = (float) (ang1.m_fRad - PI / 18);
            Line ln2 = new Line(ln.m_ptEnd, ang2, fArrowLen / 3);
            Line ln3 = new Line(ln.m_ptEnd, ang3, fArrowLen / 3);
            ln.Draw(mScr, Grp, Color.BLACK, 3, 2, false, paint);
            ln2.Draw(mScr, Grp, Color.BLACK, 3, 2, false, paint);
            ln3.Draw(mScr, Grp, Color.BLACK, 3, 2, false, paint);
        }
    }

}
