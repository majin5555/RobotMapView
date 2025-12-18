package com.siasun.dianshi.bean.pp.world;

import android.graphics.Point;
import android.util.Log;


import com.siasun.dianshi.bean.Point2d;
import com.siasun.dianshi.utils.CoordinateConversion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * 节点基础类
 * 管理地图中的节点集合，提供节点的增删改查、文件读写等功能
 * World类的父类，是world_pad.dat文件中节点数据的核心处理类
 */
public class NodeBase {


    public short m_uCount;       // 节点集合中的节点数量
    public Node[] m_paNode;       // 节点数组指针


    /**
     * 获取指定索引位置的节点ID
     *
     * @param uIndex 节点索引
     * @return 节点ID
     */
    int GetNodeID(short uIndex) {
        return m_paNode[uIndex].m_uId;
    }

    /**
     * 获取节点集合中的节点数量
     *
     * @return 节点数量
     */
    short GetNodeCount() {
        return m_uCount;
    }


    /**
     * 根据节点ID删除节点
     *
     * @param uId 要删除的节点ID
     * @return 操作结果
     * - 0: 删除成功
     * -1: 内存分配失败
     * -2: 未找到指定ID的节点
     */
    public short RemoveNode(int uId) {
        short i;

        // 检查节点是否存在
        if (GetNode(uId) == null) return -2;

        // 分配新的节点数组
        Node[] pTemp = new Node[m_uCount - 1];
        if (pTemp == null) return -1;

        // 从旧数据库复制数据，跳过要删除的节点
        for (i = 0; i < m_uCount; i++)
            if (m_paNode[i].m_uId != uId) pTemp[i] = m_paNode[i];
            else break;

        // 复制剩余节点
        for (; i < m_uCount - 1; i++)
            pTemp[i] = m_paNode[i + 1];

        short uCount = m_uCount;
        // 清空当前节点集合
        Clear();
        // 使用新节点数组替换旧数组
        m_paNode = pTemp;
        m_uCount = uCount;
        m_uCount--;
        return 0;
    }


    /**
     * 默认构造方法
     * 初始化空的节点集合
     */
    public NodeBase() {
        m_uCount = 0;
        m_paNode = null;
    }

    /**
     * 构造方法
     * 初始化指定数量的节点集合
     *
     * @param count 节点数量
     */
    public NodeBase(short count) {
        this.m_uCount = count;
    }

    /**
     * 清空节点集合
     * 释放节点数组内存并重置节点数量
     */
    void Clear() {
        if (m_paNode != null) {
            m_uCount = 0;
            m_paNode = null;
        }
    }

    /**
     * 根据节点ID获取指定节点对象
     *
     * @param uId 节点ID
     * @return 节点对象，如果未找到则返回null
     */
    public Node GetNode(int uId) {
        // 确保节点集合已创建
        if (m_paNode == null) return null;

        // 遍历查找指定ID的节点
        for (int i = 0; i < m_uCount; i++)
            if (m_paNode[i].m_uId == uId) return m_paNode[i];

        return null;
    }

    /**
     * 检查指定代码是否为有效的节点ID
     *
     * @param uCode 要检查的代码
     * @return 如果是有效节点ID则返回true，否则返回false
     */
    boolean IsNode(int uCode) {
        return (GetNode(uCode) != null);
    }

    // Get the X coordinate of the left-most point
    public float LeftMost() {
        if (m_uCount == 0) return 0.0f;

        float fMost = m_paNode[0].x;
        for (short i = 0; i < m_uCount; i++) {
            if (fMost > m_paNode[i].x) fMost = m_paNode[i].x;
        }

        return fMost;
    }

    // Get the Y coordinate of the top-most point
    public float TopMost() {
        if (m_uCount == 0) return 0.0f;

        float fMost = m_paNode[0].y;
        for (short i = 0; i < m_uCount; i++) {
            if (fMost < m_paNode[i].y) fMost = m_paNode[i].y;
        }

        return fMost;
    }

    // Get the X coordinate of the right-most point
    public float RightMost() {
        if (m_uCount == 0) return 0.0f;

        float fMost = m_paNode[0].x;
        for (short i = 0; i < m_uCount; i++) {
            if (fMost < m_paNode[i].x) fMost = m_paNode[i].x;
        }

        return fMost;
    }

    // Get the Y coordinate of the bottom-most point
    public float BottomMost() {
        if (m_uCount == 0) return 0.0f;

        float fMost = m_paNode[0].y;
        for (short i = 0; i < m_uCount; i++) {
            if (fMost > m_paNode[i].y) fMost = m_paNode[i].y;
        }

        return fMost;
    }

    //
    //   Get the width of the map area.
    //
    public float Width() {
        return RightMost() - LeftMost();
    }

    //
    //   Get the height of the map area.
    //
    public float Height() {
        return TopMost() - BottomMost();
    }

    //
    //   Get the coordinates of the center point.
    //
    public Point2d Center() {
        Point2d pt = new Point2d();
        pt.x = (RightMost() + LeftMost()) / 2;
        pt.y = (TopMost() + BottomMost()) / 2;
        return pt;
    }

    public int NextID() {
        int nNextID = 0;
        for (int i = 0; i < m_uCount; i++) {
            if (m_paNode[i].m_uId > nNextID) nNextID = m_paNode[i].m_uId;
        }

        return nNextID + 1;
    }


    public int PointHitNodeTest(Point pnt, CoordinateConversion ScrnRef) {
        for (int i = 0; i < m_uCount; i++) {
            if (m_paNode[i].PointHitTest(pnt, ScrnRef)) return m_paNode[i].m_uId;
        }
        return -1;
    }


    public short AddNode(Node nd) {
        if (GetNode(nd.m_uId) != null) return -2;

        Node[] pTemp = new Node[m_uCount + 1];
        if (pTemp == null) return -1;

        for (short i = 0; i < m_uCount; i++)
            pTemp[i] = m_paNode[i];
        pTemp[m_uCount] = nd;
        short uCount = (short) (m_uCount + 1);

        Clear();
        m_paNode = pTemp;
        m_uCount = uCount;

        return 0;
    }

    //
    //   ��ָ��λ�ü���һ���½ڵ㡣
    //
    Node AddNode(Point2d pt) {
        int nNewID = NextID();

        Node NewNode = new Node(nNewID, pt);
        if (AddNode(NewNode) < 0) return null;
        //	return false;

        return GetNode(nNewID);
    }


    /**
     * 从数据流中读取节点数据
     * 在world_pad.dat文件读取过程中被World类调用，用于加载节点集合
     *
     * @param dis 数据输入流
     */
    public void CreateParm(DataInputStream dis) {
        try {
            // 读取节点数量（小端字节序）
            int ch1 = dis.read();
//            Log.d("readWorld", "读取节点数量 ch1 " + ch1);
            int ch2 = dis.read();
//            Log.d("readWorld", "CreateParm ch2 " + ch2);

            // 检查是否到达文件末尾
            if ((ch1 | ch2) < 0) return;

            // 将小端字节序转换为节点数量
            this.m_uCount = (short) ((ch2 << 8) + (ch1 << 0));
            Log.d("readWorld", "节点集合中的节点数量 " + m_uCount);
            
            // 确保节点数量为正数，避免创建负长度数组
            if (this.m_uCount > 0) {
                // 为节点数组分配内存
                this.m_paNode = new Node[this.m_uCount];
//                Log.d("readWorld", "节点数组指针 " + m_paNode);
                Log.d("readWorld", "节点数组指针长度 " + m_paNode.length);
                // 逐个读取节点数据
                for (int i = 0; i < this.m_uCount; i++) {
                    this.m_paNode[i] = new Node();
                    this.m_paNode[i].read(dis);
                    Log.d("readWorld", "逐个读取节点数据 m_paNode[" + i + "] " + this.m_paNode[i]);
                }
            } else {
                this.m_uCount = 0;
                this.m_paNode = null;
            }
        } catch (IOException e) {
            Log.e("readWorld", "节点NodeBase错误" + e);
            e.printStackTrace();
        }
    }// readData

    /**
     * 将节点数据保存到数据流
     * 在world_pad.dat文件保存过程中被World类调用，用于存储节点集合
     *
     * @param dis 数据输出流
     */
    public void SaveParm(DataOutputStream dis) {
        try {
            // 准备节点数量的小端字节序数据
            int ch1 = this.m_uCount;
            int ch2 = this.m_uCount;
            int a1 = (ch1 & 0xff) << 8;
            int a2 = (ch2 >> 8);
            int a3 = a1 + a2;
            short sT = (short) ((ch2 >> 8) + (ch1 & 0xff) << 8);
            sT = (short) a3;
            dis.write((ch1 & 0xff));
            dis.write((ch2 >> 8));

            // 逐个保存节点数据
            for (int i = 0; i < this.m_uCount; i++) {
                this.m_paNode[i].Save(dis);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }// readData


//    public void Draw(CoordinateConversion ScrnRef, Canvas Grp, Paint mPaint) {
//        for (short i = 0; i < m_uCount; i++) {
//            Node Node = m_paNode[i];
//            Node.Draw(ScrnRef, Grp, mPaint);
//        }
//    }
}
