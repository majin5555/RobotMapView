package com.siasun.dianshi.utils;


import com.siasun.dianshi.bean.Point2d;

public class DragRect {
    //拖拽的起点
    private Point2d m_Start;
    //拖拽的终点
    private Point2d m_End;

    private final float LEN = 0.0f;

    public DragRect() {
        m_Start = new Point2d();
        m_End = new Point2d();
    }

    //得到起点
    public Point2d getDragStart() {
        return m_Start;
    }

    //得到终点
    public Point2d getDragEnd() {
        return m_End;
    }


    public float getLEN() {
        return LEN;
    }

    //定义起点
    public void difDragStart(Point2d pnt) {
        m_Start.x = pnt.x;
        m_Start.y = pnt.y;
    }

    //定义终点
    public void refreshEnd(Point2d pnt) {
        m_End.x = pnt.x;
        m_End.y = pnt.y;
    }

    //清除数据
    public void clearRect() {
        m_Start.x = LEN;
        m_Start.y = LEN;
        m_End.x = LEN;
        m_End.y = LEN;
    }

    //
    // 路径合理性判断
    //
    public boolean reasonable() {
        boolean ret = true;
        if (Math.abs(m_Start.x - m_End.x) < 0.05f && Math.abs(m_Start.x - m_End.x) < 0.05f) {
            ret = false;
        }
        return ret;
    }
}
