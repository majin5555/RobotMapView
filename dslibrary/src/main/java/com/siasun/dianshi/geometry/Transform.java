//package com.siasun.dianshi.geometry;
//
//import com.siasun.dianshi.bean.Point2d;
//import com.siasun.dianshi.bean.pp.Angle;
//import com.siasun.dianshi.bean.pp.Line;
//import com.siasun.dianshi.bean.pp.Posture;
//
//public class Transform extends Posture {
//
//    public Transform() {
//    }
//
//    public void Create(float _x, float _y, float _angle) {
//        super.Create(_x, _y, _angle);
//    }
//
//    public Transform(Point2d ptOrigin, Angle angSlant) {
//        super.SetPosture(ptOrigin, angSlant);
//    }
//
//    public void Init(Posture pstLocal) {
//        SetPosture(pstLocal.x, pstLocal.y, pstLocal.fThita);
//    }
//
//    public void Create(Line line1, Line line2) {
//
//        Transform trans1 = new Transform();
//        float angle = line1.GetSlantAngle().m_fRad - line2.GetSlantAngle().m_fRad;
//        trans1.Create(0, 0, angle);
//
//        Point2d pnt1 = GetWorldPoint(line2.m_ptStart);
//
//        float x = line1.m_ptStart.x - pnt1.x;
//        float y = line1.m_ptStart.y - pnt1.y;
//
//        Create(x, y, angle);
//    }
//
//    public Point2d GetWorldPoint(Point2d ptLocal) {
//        Point2d pt = new Point2d();
//        pt.x = x + ptLocal.x * (float) Math.cos(fThita) - ptLocal.y * (float) Math.sin(fThita);
//        pt.y = y + ptLocal.y * (float) Math.cos(fThita) + ptLocal.x * (float) Math.sin(fThita);
//
//        return pt;
//    }
//
//    public Point2d GetLocalPoint(Point2d ptWorld) {
//        Point2d pt = new Point2d();
//        float fDx = ptWorld.x - x;
//        float fDy = ptWorld.y - y;
//
//        pt.x = fDx * (float) Math.cos(fThita) + fDy * (float) Math.sin(fThita);
//        pt.y = fDy * (float) Math.cos(fThita) - fDx * (float) Math.sin(fThita);
//
//        return pt;
//    }
//
//    public Posture GetWorldPosture(Posture pstLocal) {
//        Posture pst = new Posture();
//
//        Angle ang = new Angle(0);
//        ang.m_fRad = pstLocal.fThita + fThita;
//        pst.SetPnt(GetWorldPoint(pstLocal));
//        pst.SetAngle(ang);
//        return pst;
//    }
//
//    public Posture GetLocalPosture(Posture pstWorld) {
//        Posture pst = new Posture();
//
//        Angle ang = new Angle(0);
//        ang.m_fRad = pstWorld.fThita - fThita;
//        pst.SetPnt(GetLocalPoint(pstWorld));
//        pst.SetAngle(ang);
//
//        return pst;
//    }
//
//}
