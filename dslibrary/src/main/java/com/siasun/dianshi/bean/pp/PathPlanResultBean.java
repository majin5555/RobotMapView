package com.siasun.dianshi.bean.pp;

import com.siasun.dianshi.bean.LineNew;

import java.util.Vector;


/**
 * Created by changjian.song on 2022/10/25 at 13:59
 *
 * @description ：路径规划结构体
 */
public class PathPlanResultBean {
    public int m_iPathPlanType;
    public boolean m_bIsPlanOk;
    public int m_iPathPlanId = -1;
    public Vector<LineNew> m_vecLineOfPathPlan = new Vector<>();
    public Vector<Bezier>m_vecBezierOfPathPlan = new Vector<>();
    public float[] startPoint = new float[3];
    public float[] endPoint = new float[3];



    @Override
    public String toString() {
        return "PathPlanResultBean{" +
                "m_iPathPlanType=" + m_iPathPlanType +
                ", m_bIsPlanOk=" + m_bIsPlanOk +
                ", m_vecLineOfPathPlan=" + m_vecLineOfPathPlan +
                ", m_vecBezierOfPathPlan=" + m_vecBezierOfPathPlan +
                '}';
    }
}