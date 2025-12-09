package com.siasun.dianshi.geometry;

import androidx.constraintlayout.widget.ConstraintSet;

import com.siasun.dianshi.bean.Point2d;
import com.siasun.dianshi.bean.pp.Angle;
import com.siasun.dianshi.bean.pp.Line;

public class Scp {
    public Point2d m_ptStart;            // Start point
    public Point2d m_ptEnd;              // End point
    public Angle m_angLane;            // Slant angle of the lanes
    public Point2d m_pt;                 // Trajectory point
    public Point2d m_ptLocal;            // Local trajectory point
    public Angle m_angTangent;         // Tangent angle with respect to world frame
    public Angle m_angTangent0;        // Tangent angle with respect to the lane
    public float m_fCurvature;         // Curvature

    public float m_fXe;                // Xe
    public float m_fYe;                // Ye
    public Angle m_angShift;           // Curve's shifting angle (to the lane)
    public Transform m_Transform;      // Coordinate transformation object

    public Scp(Point2d ptStart, Point2d ptEnd, Angle angLane) {
        // Init the curve parameters
        m_ptStart = ptStart;
        m_ptEnd = ptEnd;
        m_angLane = angLane;

        // The local frame - origin at "m_ptStart", slant angle "m_angLane"
        m_Transform = new Transform(m_ptStart, m_angLane);

        // Get the coordinates of the end point in the local frame
        Point2d ptLocalEnd = m_Transform.GetLocalPoint(m_ptEnd);

        // Init "Xe" and "Ye" - "Xe" is the progress variable
        m_fXe = ptLocalEnd.x;
        m_fYe = ptLocalEnd.y;

        // Now, attempts to caculate the shifting angle
        Line ln = new Line(ptStart, ptEnd);
        m_angShift = new Angle(0);
        m_angShift.m_fRad = ln.GetSlantAngle().m_fRad - angLane.m_fRad;
    }

    //
    //   GetX: Get the X distance of the curve.
    //
    public float GetX() {
        return m_fXe;
    }

    //
    //   SetCurX: Set the current X distance to specify the current point.
    //
    public void SetCurX(float fX) {
        // Step 1: Initializations


//		#if defined _TRAJ_POINT_
        //   Powers of the rate "X/Xe"
        float fRate = fX / m_fXe;
        float fRate_3 = fRate * fRate * fRate;
        float fRate_4 = fRate_3 * fRate;
        float fRate_5 = fRate_4 * fRate;
//		#endif


        //   Powers of "X"
        float fX_2 = fX * fX;
        float fX_3 = fX_2 * fX;
        float fX_4 = fX_3 * fX;

        //   Powers of "Xe"
        float fXe_3 = (float) Math.pow(m_fXe, 3.0f);
        float fXe_4 = fXe_3 * m_fXe;
        float fXe_5 = fXe_4 * m_fXe;


//		#if defined _TRAJ_POINT_
        //  Caculate Y(X) at the current point (in local frame)
        float fY = m_fYe * (10 * fRate_3 - 15 * fRate_4 + 6 * fRate_5);
//		#endif


        //  Caculate the 1st derivative of Y(X) at the current point
        float fY1 = m_fYe * (30 * fX_2 / fXe_3 - 60 * fX_3 / fXe_4 + 30 * fX_4 / fXe_5);

        //  Caculate the 2nd derivative of Y(X) at the current point
        float fY2 = m_fYe * (60 * fX / fXe_3 - 180 * fX_2 / fXe_4 + 120 * fX_3 / fXe_5);

        //  Caculate the square of the 1st derivative of Y(X)
        float fY1_2 = fY1 * fY1;


//		#if defined _TRAJ_POINT_
        // Step 2: Obtain the coordinates of the current point
        m_ptLocal = new Point2d();
        m_ptLocal.x = fX;
        m_ptLocal.y = fY;

        m_pt = m_Transform.GetWorldPoint(m_ptLocal);
//		#endif


        // Step 3: Caculate the tangent angle at the current point
        m_angTangent0 = new Angle((float) Math.atan(fY1));
        m_angTangent = new Angle(0);
        m_angTangent.m_fRad = m_angLane.m_fRad + m_angTangent0.m_fRad;

        // Step 4: Caculate the curvature of the curve at the current point
        m_fCurvature = fY2 / (float) Math.pow(1 + fY1_2, 1.5f);
    }

    //
    //   TrajFun: The trajectory generation function.
    //
    public Point2d TrajFun() {
        return m_pt;
    }

    //
    //   TangentFun: The tangent angle generation function (IN LOCAL FRAME!).
    //
    public Angle TangentFun(boolean bWorldFrame) {
        if (bWorldFrame)
            return m_angTangent;
        else
            return m_angTangent0;
    }

    //
    // The curvature generation function
    //
    public float CurvatureFun() {
        return m_fCurvature;
    }

    //
    //   ShiftAngle: Get the curve's shift angle with respect to the lane.
    //
    public Angle ShiftAngle() {
        return m_angShift;
    }

    public float NewtonRoot(float fXk, float fY) {
        float fXk1 = fXk;

        do {
            fXk = fXk1;
            fXk1 = fXk - ScpFun(fXk, fY) / ScpFun_(fXk);
        } while (Math.abs(fXk1 - fXk) > 0.001f);
        return fXk1;
    }

    public float ScpFun(float fXk, float fY) {
        float k = fY / m_fYe;
        float t = fXk / m_fXe;
        float t3 = t * t * t;
        float t4 = t3 * t;
        float t5 = t4 * t;
        float fResult = 6 * t5 - 15 * t4 + 10 * t3 - k;
        return fResult;
    }

    public float ScpFun_(float fXk) {
        float t = fXk / m_fXe;
        float t2 = t * t;
        float t3 = t2 * t;
        float t4 = t3 * t;
        float fResult = 30 * t4 - 60 * t3 + 30 * t2;
        return fResult;
    }

    //
    //   Find the X coordinate of the reference point.
    //
//		public boolean FindRefX(Point2d pt, float fRefX, float fErrX)
//		{
//			Point2d ptLocal = m_Transform.GetLocalPoint(pt);
//
//			if (((m_fXe > 0) && (ptLocal.x < 0)) || ((m_fXe < 0) && (ptLocal.x > 0)))
//				fRefX = 0;
//			else if (((m_fXe > 0) && (ptLocal.x > m_fXe)) || ((m_fXe < 0) && (ptLocal.x < m_fXe)))
//				fRefX = m_fXe;
//			else if (((m_fYe > 0) && (ptLocal.y < 0)) || ((m_fYe < 0) && (ptLocal.y > 0)))
//				fRefX = 0;
//			else if (((m_fYe > 0) && (ptLocal.y > m_fYe)) || ((m_fYe < 0) && (ptLocal.y < m_fYe)))
//				fRefX = m_fXe;
//			else if (Math.abs(ptLocal.y) < 0.0001f)
//				fRefX = 0;
//			else if (Math.abs(ptLocal.y - m_fYe) < 0.0001f)
//				fRefX = m_fXe;
//			else
//				fRefX = NewtonRoot(ptLocal.x, ptLocal.y);
//
//			fErrX = ptLocal.x - fRefX;
//
//			return true;
//		}

    //
    //   Find the error in the Y direction.
    //
//		boolean FindErrY(Point2d pt, float fErrY)
//		{
//			Point2d pt1 = m_Transform.GetLocalPoint(pt);
//			float fAbsXe = (float)Math.abs(m_fXe);
//			pt1.x = Limit(pt1.x, fAbsXe);
//			SetCurX(pt1.x);
//			fErrY = pt1.y - m_ptLocal.y;
//			return true;
//		}

}
