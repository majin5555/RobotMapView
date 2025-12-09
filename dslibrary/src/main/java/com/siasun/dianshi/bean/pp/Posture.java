package com.siasun.dianshi.bean.pp;


import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;

import com.siasun.dianshi.bean.Point2d;
import com.siasun.dianshi.utils.CoordinateConversion;


public class Posture extends Point2d {

    public float fThita;      // The direction angle

    // The constructors
    public Posture(float fX, float fY, float fAngle) {
        x = fX;
        y = fY;
        fThita = fAngle;
    }

    public Posture(float fX, float fY, Angle Angle) {
        x = fX;
        y = fY;
        fThita = Angle.m_fRad;
    }

    public Posture(Point2d pt, Angle Angle) {
        x = pt.x;
        y = pt.y;
        fThita = Angle.m_fRad;
    }

    // Default constructor
    public Posture() {
        x = y = 0;
        fThita = 0;
    }

    public void Create(float _x, float _y, float _thita) {
        x = _x;
        y = _y;
        fThita = _thita;
    }

    public void Create(final Point2d pt, final Angle ang) {
        x = pt.x;
        y = pt.y;
        fThita = ang.m_fRad;
    }

    public void SetPnt(final Point2d pt) {
        x = pt.x;
        y = pt.y;
    }

    void SetPnt(float fX, float fY) {
        x = fX;
        y = fY;
    }

    public void SetAngle(final Angle ang) {
        fThita = ang.m_fRad;
    }



    protected void SetPosture(final Point2d pt, final Angle ang) {
        SetPnt(pt);
        SetAngle(ang);
    }


    protected void SetPosture(float fX, float fY, float fAngle) {
        x = fX;
        y = fY;
        fThita = Angle.NormAngle(fAngle);
    }

    public Angle GetAngle() {
        Angle ang = new Angle(fThita);
        return ang;
    }

    public boolean PointHitTest(Point pnt, CoordinateConversion ScrnRef) {
        Point2d ptNode = new Point2d();
        ptNode = GetPoint2dObject();
//        Point pntNode = ScrnRef.GetWindowPoint(ptNode);
        PointF pntNode = ScrnRef.worldToScreen(ptNode.x, ptNode.y);
        //	SIZE sizeZero = {0,0};
        RectF rec = new RectF(pntNode.x - 30, pntNode.y - 30, pntNode.x + 30, pntNode.y + 30);
        if (rec.contains(pnt.x, pnt.y))
            return true;
        else
            return false;
    }

    @Override
    public String toString() {
        return "Posture{" +
                "fThita=" + fThita +
                ", id=" + id +
                ", x=" + x +
                ", y=" + y +
                ", a=" + a +
                ", r=" + r +
                '}';
    }
}
