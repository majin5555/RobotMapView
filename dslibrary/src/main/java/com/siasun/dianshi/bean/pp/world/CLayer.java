package com.siasun.dianshi.bean.pp.world;


import static java.lang.Math.abs;

import android.graphics.Point;
import android.util.Log;

import com.siasun.dianshi.bean.Point2d;
import com.siasun.dianshi.bean.TranBytes;
import com.siasun.dianshi.bean.pp.Angle;
import com.siasun.dianshi.bean.pp.Bezier;
import com.siasun.dianshi.bean.pp.Line;
import com.siasun.dianshi.bean.pp.Posture;
import com.siasun.dianshi.utils.io.WorldFileIO;
import com.siasun.dianshi.utils.CoordinateConversion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;


/**
 * 地图图层类，用于管理地图中的路径、节点等元素
 * 继承自NodeBase类，实现了图层数据的读写和绘制功能
 */
public class CLayer extends NodeBase {

    /**
     * 路径数据库对象，用于管理图层中的所有路径
     */
    public PathBase m_PathBase = new PathBase();

    public CLayer() {
        m_PathBase.m_MyNode = this;
    }


    /**
     * 清扫使用，生成弓字形路径
     * 添加直线路径到图层
     *
     * @param ptStart      路径起点坐标
     * @param ptEnd        路径终点坐标
     * @param nStartNodeId 起点节点ID，-1表示创建新节点
     * @param nEndNodeId   终点节点ID，-1表示创建新节点
     * @param speed        速度参数
     * @param guidFunction 引导功能类型
     * @return 是否成功添加路径
     */
    public boolean AddLinePath(Point2d ptStart, Point2d ptEnd, int nStartNodeId, int nEndNodeId, float[] speed, short guidFunction) {

        Node pStartNode = new Node();
        Node pEndNode = new Node();
        if (nStartNodeId < 0) {
            pStartNode = m_PathBase.m_MyNode.AddNode(ptStart);
        } else {
            pStartNode = m_PathBase.m_MyNode.GetNode(nStartNodeId);

        }
        if (pStartNode == null) return false;

        nStartNodeId = pStartNode.m_uId;

        if (nEndNodeId < 0) {
            pEndNode = m_PathBase.m_MyNode.AddNode(ptEnd);
        } else {
            pEndNode = m_PathBase.m_MyNode.GetNode(nEndNodeId);
        }

        if (pEndNode == null) return false;

        nEndNodeId = pEndNode.m_uId;

        int nNextID = m_PathBase.NextID();

        LinePath pPath = new LinePath((short) nNextID, nStartNodeId, nEndNodeId, speed, guidFunction, (short) 0, (short) 0, (short) 3, m_PathBase.m_MyNode);

        return m_PathBase.AddPath(pPath);
    }

    @Deprecated
    public boolean AddLinePath(Point2d ptStart, Point2d ptEnd, int nStartNodeId, int nEndNodeId) {

        Node pStartNode = new Node();
        Node pEndNode = new Node();
        if (nStartNodeId < 0) {
            // �ڵ�ͼ�м����½ڵ�
            pStartNode = AddNode(ptStart);
            if (pStartNode == null) return false;
        } else {
            // ���ݽڵ��ȡ�ýڵ�ָ��
            pStartNode = GetNode(nStartNodeId);

            // ����ڵ㲻���ڣ�����false
            if (pStartNode == null) return false;
        }
        nStartNodeId = pStartNode.m_uId;


        // ����յ�ָ��δ�ṩ��˵���յ���½ڵ�
        if (nEndNodeId < 0) {
            // �ڵ�ͼ�м����½ڵ�
            pEndNode = AddNode(ptEnd);
            // ����ڵ㲻���ڣ�����false
            if (pEndNode == null) return false;
        } else {
            // ���ݽڵ��ȡ�ýڵ�ָ��
            pEndNode = GetNode(nEndNodeId);

            // ����ڵ㲻���ڣ�����false
            if (pEndNode == null) return false;
        }
        nEndNodeId = pEndNode.m_uId;

        // �����·��
        int nNextID = m_PathBase.NextID();

        LinePath pPath = new LinePath((short) nNextID, (short) nStartNodeId, (short) nEndNodeId, new float[2], (short) 00, (short) 0, (short) 0, (short) 0, m_PathBase.m_MyNode);
        if (pPath == null) return false;

        return m_PathBase.AddPath(pPath);
    }

    /**
     * 创建一个新节点
     *
     * @param pt 节点位置坐标
     * @return 创建的节点对象
     */
    public Node CreateNode(Point2d pt) {
        // 获取下一个可用的节点ID
        int nNewID = NextID();

        // 创建新节点
        Node newNode = new Node(nNewID, pt);

        // 设置节点的默认属性
        newNode.m_uType = 1; // 默认节点类型
        newNode.m_uExtType = 0; // 默认扩展类型
        newNode.m_uExtType2 = 0; // 默认扩展类型2
        newNode.m_fHeading = 0.0f; // 默认航向角
        newNode.m_Tag = new RfId(); // 创建新的标签对象
        newNode.m_Tag.Init(null); // 初始化标签

        // 设置标记相关属性
        newNode.m_fChkMarkDist = 0.45f; // 检测标记距离
        newNode.m_fChkMarkVel = 0.1f; // 检测标记速度
        newNode.m_fMarkWidth = 0.0f; // 标记有效宽度

        // 设置偏移量
        newNode.m_fOffset1 = 0.0f;
        newNode.m_fOffset2 = 0.0f;

        // 设置标记偏移
        newNode.m_fFwdMarkOffset = 0.0f;
        newNode.m_fBwdMarkOffset = 0.0f;

        // 设置图层和站类型
        newNode.m_uLayerID = 0;
        newNode.m_uStationType = 0; // 0:临时站
        newNode.m_uStationTempId = 0;
        newNode.m_uStationId = 0;

        // 设置车辆类型和上线状态
        newNode.m_uCarrierType = 255; // 默认车辆类型
        newNode.m_uOnLine = 1; // 允许上线

        // 将节点添加到节点集合中
        if (AddNode(newNode) < 0) {
            return null;
        }

        // 返回创建的节点
        return GetNode(nNewID);
    }

    /**
     * 创建一条连接两个节点的路径
     *
     * @param startNode 起始节点
     * @param endNode   结束节点
     * @return 创建的路径对象
     */
    public Path CreatePPLine(Node startNode, Node endNode) {
        if (startNode == null || endNode == null) {
            return null;
        }

        // 获取路径的下一个ID
        int nNextID = m_PathBase.NextID();

        // 创建起点和终点的姿态对象
        Posture pstStart = new Posture();
        pstStart.x = startNode.x;
        pstStart.y = startNode.y;
        pstStart.SetAngle(new Angle(0)); // 初始角度为0

        Posture pstEnd = new Posture();
        pstEnd.x = endNode.x;
        pstEnd.y = endNode.y;
        pstEnd.SetAngle(new Angle(0)); // 初始角度为0

        // 创建曲线路径，使用默认的控制点距离
        float defaultControlPointDistance = 0.5f; // 可以根据实际需求调整

        GenericPath pPath = new GenericPath(nNextID, startNode.m_uId, endNode.m_uId,
                pstStart, pstEnd, defaultControlPointDistance, defaultControlPointDistance,
                new float[2], (short) 0, (short) 0, (short) 0, (short) 3, m_PathBase.m_MyNode);

        // 将路径添加到路径数据库中
        if (m_PathBase.AddPath(pPath)) {
            return pPath;
        }

        return null;
    }

    /**
     * 创建路径的通用方法
     *
     * @param startPoint           起点坐标
     * @param endPoint             终点坐标
     * @param startNodeId          起点节点ID，-1表示创建新节点
     * @param endNodeId            终点节点ID，-1表示创建新节点
     * @param pathType             路径类型（0表示直线，10表示曲线）
     * @param speed                速度参数
     * @param guidFunction         引导功能类型
     * @param controlPointDistance 控制点距离（仅用于曲线类型）
     * @return 创建的路径对象，失败返回null
     */
    public Path CreatePath(Point2d startPoint, Point2d endPoint, int startNodeId, int endNodeId,
                           int pathType, float[] speed, short guidFunction, float controlPointDistance) {
        Node startNode;
        Node endNode;

        // 处理起点节点
        if (startNodeId < 0) {
            // 创建新节点
            startNode = CreateNode(startPoint);
            if (startNode == null) {
                return null;
            }
        } else {
            // 使用现有节点
            startNode = GetNode(startNodeId);
            if (startNode == null) {
                return null;
            }
        }

        // 处理终点节点
        if (endNodeId < 0) {
            // 创建新节点
            endNode = CreateNode(endPoint);
            if (endNode == null) {
                return null;
            }
        } else {
            // 使用现有节点
            endNode = GetNode(endNodeId);
            if (endNode == null) {
                return null;
            }
        }

        // 获取路径的下一个ID
        int nextPathId = m_PathBase.NextID();

        // 创建起点和终点的姿态对象
        Posture pstStart = new Posture();
        pstStart.x = startNode.x;
        pstStart.y = startNode.y;
        pstStart.SetAngle(new Angle(0)); // 初始角度为0

        Posture pstEnd = new Posture();
        pstEnd.x = endNode.x;
        pstEnd.y = endNode.y;
        pstEnd.SetAngle(new Angle(0)); // 初始角度为0

        Path createdPath;

        // 根据路径类型创建不同类型的路径
        if (pathType == 0) {
            // 创建直线路径
            createdPath = new LinePath(nextPathId, startNode.m_uId, endNode.m_uId,
                    speed, guidFunction, (short) 0, (short) 0, (short) 3, m_PathBase.m_MyNode);
        } else if (pathType == 10) {
            // 创建曲线路径
            if (controlPointDistance <= 0) {
                controlPointDistance = 0.5f; // 默认控制点距离
            }

            createdPath = new GenericPath(nextPathId, startNode.m_uId, endNode.m_uId,
                    pstStart, pstEnd, controlPointDistance, controlPointDistance,
                    speed, guidFunction, (short) 0, (short) 0, (short) 3, m_PathBase.m_MyNode);
        } else {
            // 不支持的路径类型
            return null;
        }

        // 将路径添加到路径数据库中
        if (createdPath != null && m_PathBase.AddPath(createdPath)) {
            return createdPath;
        }

        return null;
    }

    /**
     * 添加曲线路径到图层
     *
     * @param pstStart     起点姿态（包含位置和角度）
     * @param pstEnd       终点姿态（包含位置和角度）
     * @param fLen1        起点到第一个控制点的距离
     * @param fLen2        终点到第二个控制点的距离
     * @param nStartNodeId 起点节点ID，-1表示创建新节点
     * @param nEndNodeId   终点节点ID，-1表示创建新节点
     * @return 是否成功添加路径
     */
    public boolean AddGenericPath(Posture pstStart, Posture pstEnd, float fLen1, float fLen2, int nStartNodeId, int nEndNodeId) {
        Node pStartNode = new Node();
        Node pEndNode = new Node();

        if (nStartNodeId < 0) {
            // �ڵ�ͼ�м����½ڵ�
            pStartNode = AddNode(pstStart.GetPoint2dObject());
            if (pStartNode == null) return false;
        } else {
            // ���ݽڵ��ȡ�ýڵ�ָ��
            pStartNode = GetNode(nStartNodeId);

            // ����ڵ㲻���ڣ�����false
            if (pStartNode == null) return false;
        }
        nStartNodeId = pStartNode.m_uId;


        // ����յ�ָ��δ�ṩ��˵���յ���½ڵ�
        if (nEndNodeId < 0) {
            // �ڵ�ͼ�м����½ڵ�
            pEndNode = AddNode(pstEnd.GetPoint2dObject());
            // ����ڵ㲻���ڣ�����false
            if (pEndNode == null) return false;
        } else {
            // ���ݽڵ��ȡ�ýڵ�ָ��
            pEndNode = GetNode(nEndNodeId);

            // ����ڵ㲻���ڣ�����false
            if (pEndNode == null) return false;
        }
        nEndNodeId = pEndNode.m_uId;

        // �����·��
        int nNextID = m_PathBase.NextID();

        GenericPath pPath = new GenericPath(nNextID, nStartNodeId, nEndNodeId, pstStart, pstEnd, fLen1, fLen2, new float[2], (short) 0, (short) 0, (short) 0, (short) 3, m_PathBase.m_MyNode);


        return m_PathBase.AddPath(pPath);
    }

    /**
     * PP示教生成曲线
     * 通过示教点和控制点添加曲线路径
     *
     * @param pstStart     起点姿态（包含位置和角度）
     * @param pstEnd       终点姿态（包含位置和角度）
     * @param pptCtrl      控制点数组
     * @param nStartNodeId 起点节点ID，-1表示创建新节点
     * @param nEndNodeId   终点节点ID，-1表示创建新节点
     * @param pathParam    路径参数
     * @return 是否成功添加路径
     */
    public boolean AddGenericPath_PPteach(Posture pstStart, Posture pstEnd, Point2d[] pptCtrl, int nStartNodeId, int nEndNodeId, short pathParam) {
        Node pStartNode = new Node();
        Node pEndNode = new Node();

        if (nStartNodeId < 0) {
            // �ڵ�ͼ�м����½ڵ�
            pStartNode = AddNode(pstStart.GetPoint2dObject());
            if (pStartNode == null) return false;
        } else {
            // ���ݽڵ��ȡ�ýڵ�ָ��
            pStartNode = GetNode(nStartNodeId);

            // ����ڵ㲻���ڣ�����false
            if (pStartNode == null) return false;
        }
        nStartNodeId = pStartNode.m_uId;


        // ����յ�ָ��δ�ṩ��˵���յ���½ڵ�
        if (nEndNodeId < 0) {
            // �ڵ�ͼ�м����½ڵ�
            pEndNode = AddNode(pstEnd.GetPoint2dObject());
            // ����ڵ㲻���ڣ�����false
            if (pEndNode == null) return false;
        } else {
            // ���ݽڵ��ȡ�ýڵ�ָ��
            pEndNode = GetNode(nEndNodeId);

            // ����ڵ㲻���ڣ�����false
            if (pEndNode == null) return false;
        }
        nEndNodeId = pEndNode.m_uId;

        // �����·��
        int nNextID = m_PathBase.NextID();

        GenericPath pPath = new GenericPath(nNextID, nStartNodeId, nEndNodeId, pstStart, pstEnd, pptCtrl, new float[2], (short) 0, (short) 0, (short) 0, (short) 3, m_PathBase.m_MyNode, pathParam);

        return m_PathBase.AddPath(pPath);
    }

    /**
     * 获取指定节点的所有邻居节点ID
     *
     * @param uNode   要查询的节点ID
     * @param pBuf    存储邻居节点ID的缓冲区
     * @param uBufLen 缓冲区长度
     * @return 找到的邻居节点数量
     */
    public short GetAllNeighborNodes(int uNode, int[] pBuf, int uBufLen) {
        short uFound = 0;
        for (short i = 0; i < m_PathBase.m_uCount; i++) {
            Path pPath = m_PathBase.m_pPathIdx[i].m_ptr;

            int uStartNode = pPath.m_uStartNode;
            int uEndNode = pPath.m_uEndNode;

            if (uNode == uStartNode) pBuf[uFound++] = uEndNode;

            else if (uNode == uEndNode) pBuf[uFound++] = uStartNode;

            if (uFound == uBufLen) break;
        }

        return uFound;
    }

    /**
     * 获取包含指定节点的所有路径ID
     *
     * @param NodeID 要查询的节点ID
     * @return 包含该节点的所有路径ID的向量
     */
    public Vector<Integer> GetPathIncludeNode(int NodeID) {
        Vector<Integer> vPathID = new Vector();
        for (int i = 0; i < m_PathBase.m_uCount; i++) {
            Path path = m_PathBase.m_pPathIdx[i].m_ptr;
            if (path.m_uStartNode == NodeID || path.m_uEndNode == NodeID) {
                vPathID.add(i);
            }
        }
        return vPathID;
    }

    /**
     * 修改节点角度并更新曲线控制点
     *
     * @param pnt    新的位置点
     * @param NodeID 要修改的节点ID
     */
    public void ModifyNodeAng(Point2d pnt, int NodeID) {
        //如果方向有很多个，不允许移动节点
        Angle[] Angles = new Angle[4];
        int nHeadingAngleCount = GetNodeHeadingAngle(NodeID, Angles, 4);
        if (nHeadingAngleCount > 1) {
            //		return;
        }
        //得到和曲线起点相关联的曲线
        Vector<Integer> vPathID = new Vector();
        vPathID = GetPathIncludeNode(NodeID);
        if (vPathID == null) return;
        //如果包含的路径中含有直线，不允许移动
        for (int i = 0; i < vPathID.size(); i++) {
            Path pPathT = m_PathBase.m_pPathIdx[vPathID.get(i)].m_ptr;
            if (pPathT.m_uType == 0) {
                return;
            }
        }
        //如果全部是曲线，为了保证相切性，所有的曲线控制点都需要相应的做旋转变换
        Point2d ptS = new Point2d();
        Node node = GetNode(NodeID);
        if (node == null) {
            return;
        }
        ptS.x = node.x;
        ptS.y = node.y;
        Angle angT = new Angle(0.0f);
        //更新控制点的位置
        for (int i = 0; i < vPathID.size(); i++) {
            Angle angLen = new Angle(0.0f);
            GenericPath pPathT = (GenericPath) (m_PathBase.m_pPathIdx[vPathID.get(i)].m_ptr);
            if (pPathT.m_uStartNode == NodeID) {
                float fTotalLen = (float) Math.sqrt((pPathT.m_Curve.m_ptKey[0].x - pPathT.m_Curve.m_ptKey[1].x) * (pPathT.m_Curve.m_ptKey[0].x - pPathT.m_Curve.m_ptKey[1].x) + (pPathT.m_Curve.m_ptKey[0].y - pPathT.m_Curve.m_ptKey[1].y) * (pPathT.m_Curve.m_ptKey[0].y - pPathT.m_Curve.m_ptKey[1].y));
                Line ln1 = new Line(ptS, pnt);
                angT.m_fRad = ln1.m_angSlant.m_fRad;
//				if(pPathT.GetHeading(pNode) == pPath.GetHeading(pNode))
//				{
//					angLen.m_fRad = (float) (angT.m_fRad + Math.PI);
//				}
//				else
                angLen.m_fRad = angT.m_fRad;
                Line ln = new Line(ptS, angLen, fTotalLen);
                pPathT.m_Curve.m_ptKey[1].x = ln.m_ptEnd.x;
                pPathT.m_Curve.m_ptKey[1].y = ln.m_ptEnd.y;
                pPathT.ModifyParmByCurve();
            }
            if (pPathT.m_uEndNode == NodeID) {
                int nKeyCount = pPathT.m_Curve.m_nCountKeyPoints - 1;
                float fTotalLen = (float) Math.sqrt((pPathT.m_Curve.m_ptKey[nKeyCount].x - pPathT.m_Curve.m_ptKey[nKeyCount - 1].x) * (pPathT.m_Curve.m_ptKey[nKeyCount].x - pPathT.m_Curve.m_ptKey[nKeyCount - 1].x) + (pPathT.m_Curve.m_ptKey[nKeyCount].y - pPathT.m_Curve.m_ptKey[nKeyCount - 1].y) * (pPathT.m_Curve.m_ptKey[nKeyCount].y - pPathT.m_Curve.m_ptKey[nKeyCount - 1].y));
                Line ln1 = new Line(ptS, pnt);
                angT.m_fRad = ln1.m_angSlant.m_fRad;
//				if(pPathT.GetHeading(pNode) == pPath.GetHeading(pNode))
//				{
//					angLen.m_fRad = angT.m_fRad;
//				}
//				else
                angLen.m_fRad = (float) (angT.m_fRad + Math.PI);
                Line ln = new Line(ptS, angLen, fTotalLen);
                pPathT.m_Curve.m_ptKey[nKeyCount - 1].x = ln.m_ptEnd.x;
                pPathT.m_Curve.m_ptKey[nKeyCount - 1].y = ln.m_ptEnd.y;
                pPathT.ModifyParmByCurve();
            }
        }
    }

    /**
     * 修改指定节点的坐标
     * 如果是曲线节点，还会更新对应的曲线控制点
     *
     * @param pnt          新的坐标值
     * @param NodeID       待修改的节点ID
     * @param TargetNodeId 待合并的节点号，-1表示不进行合并节点
     */
    public void ModifyNodeCoord(Point2d pnt, int NodeID, int TargetNodeId) {
        Node pNode = GetNode(NodeID);
        if (pNode == null) {
            return;
        }

        //修改节点坐标
        pNode.x = pnt.x;
        pNode.y = pnt.y;
        int oldNodeId = NodeID;        //记录移动节点ID


        // 判断当前的位置是否和现有节点重合
        // m_nCurNodeId = layer.PointHitNodeTest(pnt, ScrnRef);

        //如果NodeID所对应的路段是曲线，那么需要修改对应的控制点的坐标
        for (int i = 0; i < m_PathBase.m_uCount; i++) {
            Path path = m_PathBase.m_pPathIdx[i].m_ptr;
            int Type = path.m_uType;
            if (Type == 10 && path.m_uStartNode == NodeID) {
                if (TargetNodeId != -1) {
                    NodeID = TargetNodeId;
                    pNode = GetNode(TargetNodeId);
                }
                Bezier Curve = new Bezier();
                Curve = ((GenericPath) path).m_Curve;
                if (TargetNodeId == -1) {
                    Curve.m_ptKey[1].x = Curve.m_ptKey[1].x + (pNode.x - Curve.m_ptKey[0].x);
                    Curve.m_ptKey[1].y = Curve.m_ptKey[1].y + (pNode.y - Curve.m_ptKey[0].y);
                } else {
                    // 为了相切，需要修改控制点的坐标
                    Line line1 = new Line(Curve.m_ptKey[0], Curve.m_ptKey[1]);
                    float Len = line1.Length();
                    Angle[] Angles = new Angle[4];
                    int nCount = GetNodeHeadingAngle(NodeID, Angles, 4);
                    Line line = new Line(pNode, Angles[0], Len);
                    Curve.m_ptKey[1].x = line.m_ptEnd.x;
                    Curve.m_ptKey[1].y = line.m_ptEnd.y;
                }

                //	Curve.m_ptKey[0] = m_AgvWorld->GetNode(NodeID)->GetPoint2dObject();
                if (TargetNodeId == -1) {
                    Curve.m_ptKey[0] = pnt;
                } else {
                    Curve.m_ptKey[0].x = pNode.x;
                    Curve.m_ptKey[0].y = pNode.y;
                    path.m_uStartNode = TargetNodeId;
                }
                ((GenericPath) path).ModifyParmByCurve(); //给贝塞尔曲线参数赋值，用于保存地图
                path.m_fSize = ((GenericPath) path).m_Curve.m_fTotalLen;///////////////////////////？？
            }

            if (Type == 10 && path.m_uEndNode == NodeID) {
                if (TargetNodeId != -1) {
                    NodeID = TargetNodeId;
                    pNode = GetNode(TargetNodeId);
                }
                Bezier curve = new Bezier();
                curve = ((GenericPath) path).m_Curve;
                if (TargetNodeId == -1) {
                    curve.m_ptKey[curve.m_nCountKeyPoints - 2].x = curve.m_ptKey[curve.m_nCountKeyPoints - 2].x + (pNode.x - curve.m_ptKey[curve.m_nCountKeyPoints - 1].x);
                    curve.m_ptKey[curve.m_nCountKeyPoints - 2].y = curve.m_ptKey[curve.m_nCountKeyPoints - 2].y + (pNode.y - curve.m_ptKey[curve.m_nCountKeyPoints - 1].y);
                } else {
                    // 为了相切，需要修改控制点的坐标
                    Line line1 = new Line(curve.m_ptKey[curve.m_nCountKeyPoints - 2], curve.m_ptKey[curve.m_nCountKeyPoints - 1]);
                    float Len = line1.Length();
                    Angle[] Angles = new Angle[4];
                    int nCount = GetNodeHeadingAngle(NodeID, Angles, 4);
                    Line line = new Line(pNode, Angles[0], Len);
                    curve.m_ptKey[curve.m_nCountKeyPoints - 2].x = line.m_ptEnd.x;
                    curve.m_ptKey[curve.m_nCountKeyPoints - 2].y = line.m_ptEnd.y;
                }
                //	curve.m_ptKey[curve.m_nCountKeyPoints - 1] = m_AgvWorld->GetNode(NodeID)->GetPoint2dObject();
                if (TargetNodeId == -1) {
                    curve.m_ptKey[curve.m_nCountKeyPoints - 1] = pnt;
                } else {
                    curve.m_ptKey[curve.m_nCountKeyPoints - 1].x = pNode.x;
                    curve.m_ptKey[curve.m_nCountKeyPoints - 1].y = pNode.y;
                    path.m_uEndNode = TargetNodeId;
                }

                ((GenericPath) path).ModifyParmByCurve();
                path.m_fSize = ((GenericPath) path).m_Curve.m_fTotalLen;
            }

            //如果NodeID对应的路段是直线，
            if (path.m_uType == 0 && (path.m_uStartNode == NodeID || path.m_uEndNode == NodeID)) {
//                Point2d ptStart = path.GetStartPnt();
//                Point2d ptEnd = path.GetEndPnt();

                // Construct a line
//                Line ln = new Line(ptStart, ptEnd);
                if (TargetNodeId == -1) {
                } else {
                    if (path.m_uStartNode == NodeID) {
                        path.m_uStartNode = TargetNodeId;
                    } else {
                        path.m_uEndNode = TargetNodeId;
                    }
                }
                // The start and end heading angle are the same
                ((LinePath) path).Setup();
            }
        }

        // 合并后，需要把移动点删除
        int PathId = -1;
        for (int i = 0; i < m_PathBase.m_uCount; i++) {
            if (m_PathBase.m_pPathIdx[i].m_ptr.m_uStartNode == oldNodeId || m_PathBase.m_pPathIdx[i].m_ptr.m_uEndNode == oldNodeId) {

                PathId = i;
            }
        }

        if (PathId == -1) {
            RemoveNode(oldNodeId);
        }
    }

    /**
     * 检测屏幕点是否命中图层中的任意节点
     *
     * @param pnt       屏幕坐标点
     * @param ScrnRef   坐标转换对象
     * @param withoutId 排除的节点ID，用于避免检测到特定节点
     * @return 命中的节点ID，-1表示未命中
     */
    public int PointHitNodeTest(Point pnt, CoordinateConversion ScrnRef, int withoutId) {
        for (int i = 0; i < m_uCount; i++) {
            if (m_paNode[i].PointHitTest(pnt, ScrnRef)) {
                if (m_paNode[i].m_uId == withoutId) {
                    continue;
                } else {
                    return m_paNode[i].m_uId;
                }
            }
        }
        return -1;
    }

    /**
     * 获取指定节点的车身姿态角度（可以有多个）
     * 获取与nNodeId节点相邻节点的角度，保存在pAngles中，如果有相同角度或者差Pi，只保存一个
     *
     * @param nNodeId 要查询的节点ID
     * @param pAngles 存储角度的缓冲区
     * @param nMaxNum 缓冲区最大容量
     * @return 实际获取的角度数量
     */
    public int GetNodeHeadingAngle(int nNodeId, Angle[] pAngles, int nMaxNum) {
        int[] pBuf = new int[nMaxNum];

        // 取得与该节点所有相邻节点的个数
        int nCount = GetAllNeighborNodes(nNodeId, pBuf, (short) nMaxNum);
        Node pNode = GetNode(nNodeId);

        // 依次判断各相邻路段的终止姿态
        for (int i = 0; i < nCount; i++) {
            Path pPath = m_PathBase.GetPathPointer(nNodeId, pBuf[i]);
            if (pPath == null) return 0;

            // 取得该路径的终止姿态角
            pAngles[i] = pPath.GetHeading(pNode);
        }

        // 分配删除标志
        boolean[] bDelete = new boolean[nCount];
        for (int i = 0; i < nCount; i++)
            bDelete[i] = false;

        // 核对是否有相同的角，如有，则删除后面一个
        for (int i = 0; i < nCount; i++) {
            if (bDelete[i]) continue;

            for (int j = i + 1; j < nCount; j++) {
                if (bDelete[j]) continue;

                if (abs(pAngles[i].m_fRad - pAngles[j].m_fRad) < 0.0001f || abs(pAngles[i].m_fRad - pAngles[j].m_fRad) == Math.PI)
                    bDelete[j] = true;
            }
        }

        // 删除标明为“bDelete”的项
        int nNewCount = 0;
        for (int i = 0; i < nCount; i++) {
            if (!bDelete[i]) {
                pAngles[nNewCount++] = pAngles[i];
            }
        }

        return nNewCount;
    }

    /**
     * 绘制图层中的所有路段
     *
     * @param ScrnRef 坐标转换对象，用于将世界坐标转换为屏幕坐标
     * @param canvas  画布对象
     * @param paint   画笔对象
     */
//    public void Draw(Canvas canvas, CoordinateConversion ScrnRef, Paint paint) {
//        if (m_PathBase != null && m_PathBase.m_MyNode != null) {
//            m_PathBase.Draw(ScrnRef, canvas, paint);
//        }
//    }

    /**
     * 删除指定节点及其关联的所有路径
     *
     * @param NodeId 要删除的节点ID
     */
    public void DeleteNode(int NodeId) {
        Vector<Integer> vPathID = new Vector();
        for (int i = 0; i < m_PathBase.m_uCount; i++) {
            Path pPath = m_PathBase.m_pPathIdx[i].m_ptr;
            if (pPath.m_uStartNode == NodeId || pPath.m_uEndNode == NodeId) vPathID.add(i);
        }
        int leftNode = -1;
        int rightNode = -1;
        // 取得与该节点所有相邻节点的编号
        int[] pBuf = new int[4];
        int nCount = GetAllNeighborNodes(NodeId, pBuf, (short) 4);
        if (nCount == 1) leftNode = pBuf[0];
        if (nCount > 1) {

            Vector<Integer> NodeID = new Vector();
            NodeID.add(NodeId);
            NodeID.add(pBuf[0]);
            Vector<Integer> vPath = GetPathByNodeId(NodeID);
            if (vPath.size() < 0) return;
            if (m_PathBase.m_pPathIdx[vPath.get(0)].m_ptr.m_uStartNode == pBuf[0]) {
                leftNode = pBuf[0];
                rightNode = pBuf[1];
            } else {
                leftNode = pBuf[1];
                rightNode = pBuf[0];
            }
        }
        //
        //重新计算路径ID，只要包含在该任务中的路径需要删除
        Vector<Integer> DeletePathID = new Vector();
        for (int i = 0; i < vPathID.size(); i++) {
            Path pPath = m_PathBase.m_pPathIdx[vPathID.get(i)].m_ptr;
            //判断该节点是否为属于一条路径
            if (leftNode == -1) {
                if (pPath.m_uEndNode == rightNode || pPath.m_uStartNode == rightNode) {
                    DeletePathID.add(vPathID.get(i));
                }
            } else if (rightNode == -1) {
                if (pPath.m_uEndNode == leftNode || pPath.m_uStartNode == leftNode) {
                    DeletePathID.add(vPathID.get(i));
                }
            } else {
                if ((pPath.m_uEndNode == leftNode || pPath.m_uStartNode == leftNode) || (pPath.m_uEndNode == rightNode || pPath.m_uStartNode == rightNode)) {
                    DeletePathID.add(vPathID.get(i));
                }
            }
        }
        //增加新线
        if (leftNode != -1 && rightNode != -1) {
            // 整个曲线已确定
            Posture m_pstStart = new Posture();
            Posture m_pstEnd = new Posture();
            Bezier m_Bezier = new Bezier();
            Node nodeS = new Node();
            Node nodeE = new Node();
            nodeS = GetNode(leftNode);
            nodeE = GetNode(rightNode);
            Angle[] Angles = new Angle[4];
            int nHeadingAngleCount = GetNodeHeadingAngle(leftNode, Angles, 4);
            //	if (nHeadingAngleCount == 1)
            if (nHeadingAngleCount > 0) {
                m_pstStart.Create(nodeS.GetPoint2dObject(), Angles[0]);
            }

            nCount = GetNodeHeadingAngle(rightNode, Angles, 4);
            //	if (nCount == 1) {
            if (nCount > 0) {
                m_pstEnd.Create(nodeE.GetPoint2dObject(), Angles[0]);
            }
            boolean flag = m_Bezier.Create(m_pstStart, m_pstEnd, 0.95f);
            m_Bezier.BezierOptic();
            if (flag == true) {
                float fDist1 = m_Bezier.m_ptKey[0].DistanceTo(m_Bezier.m_ptKey[1]);
                float fDist2 = m_Bezier.m_ptKey[2].DistanceTo(m_Bezier.m_ptKey[3]);
                AddGenericPath(m_pstStart, m_pstEnd, fDist1, fDist2, leftNode, rightNode);
            } else {
                //Toast.makeText();
            }

        }
        //选择删除
        int romoveID = -1;
        for (int i = 0; i < DeletePathID.size(); i++) {
            for (int j = i; j < DeletePathID.size(); j++) {
                if (romoveID != -1 && DeletePathID.get(j) > romoveID) {
                    DeletePathID.set(j, DeletePathID.get(j) - 1);
                }
            }
            m_PathBase.RemovePath(DeletePathID.get(i));
            romoveID = DeletePathID.get(i);
        }
        DeletePathID.clear();
    }


    //由任务中的节点号得到路径序号
    public Vector<Integer> GetPathByNodeId(Vector<Integer> nodeId) {
        Vector<Integer> pathId = new Vector();
        // 依次对每个路径进行判断
        for (int j = 0; j < nodeId.size() - 1; j++) {
            int startID = nodeId.get(j);
            int endID = nodeId.get(j + 1);
            for (int i = 0; i < m_PathBase.m_uCount; i++) {
                Path pPath = m_PathBase.m_pPathIdx[i].m_ptr;
                if ((pPath.m_uStartNode == startID && pPath.m_uEndNode == endID) || (pPath.m_uEndNode == startID && pPath.m_uStartNode == endID)) {
                    pathId.add(i);           // 曲线的序号
                }
            }
        }
        return pathId;
    }

    public boolean ISInRect(int pathIndex, double minx, double miny, double maxx, double maxy) {
        if (pathIndex > (m_PathBase.m_uCount - 1)) return false;
        return m_PathBase.ISInRect(pathIndex, minx, miny, maxx, maxy);
    }

    //返回删除的节点ID
    public Vector<Integer> DelPath(Vector<Integer> vPathID) {
        Vector<Integer> NodeID = new Vector();
        //单条线删除

        //块选择删除
        int romoveID = -1;
        for (int i = 0; i < vPathID.size(); i++) {
            for (int j = i; j < vPathID.size(); j++) {
                if (romoveID != -1 && vPathID.get(j) > romoveID) {
                    vPathID.setElementAt(vPathID.get(j) - 1, j);
                    //	vPathID.get(j) = vPathID.get(j) - 1;
                }
            }
            Vector<Integer> NodeT = m_PathBase.RemovePath(vPathID.get(i));
            for (int k = 0; k < NodeT.size(); k++) {
                NodeID.add(NodeT.get(k));
            }
            romoveID = vPathID.get(i);
        }
        vPathID.clear();
        return NodeID;
    }

    /**
     * 修改指定节点的属性
     *
     * @param nodeAttr 包含新属性的节点属性对象
     */
    public void updateNodeAttr(Node nodeAttr) {
        Node pNode = m_PathBase.m_MyNode.GetNode(nodeAttr.m_uId);

        pNode.m_uType = nodeAttr.m_uType;

        Point2d m_pnt = new Point2d();
        m_pnt.x = (float) nodeAttr.x;
        m_pnt.y = (float) nodeAttr.y;

        //修改节点坐标
        ModifyNodeCoord(m_pnt, nodeAttr.m_uId, -1);
        pNode.m_uExtType = nodeAttr.m_uExtType;
        pNode.m_fHeading = nodeAttr.m_fHeading;
        pNode.m_Tag = nodeAttr.m_Tag;
        pNode.m_fChkMarkDist = nodeAttr.m_fChkMarkDist;
        pNode.m_fChkMarkVel = nodeAttr.m_fChkMarkVel;
        pNode.m_fMarkWidth = nodeAttr.m_fMarkWidth;

        pNode.m_fOffset1 = nodeAttr.m_fOffset1;
        pNode.m_fOffset2 = nodeAttr.m_fOffset2;

        pNode.m_fFwdMarkOffset = nodeAttr.m_fFwdMarkOffset;
        pNode.m_fBwdMarkOffset = nodeAttr.m_fBwdMarkOffset;

        pNode.m_uLayerID = nodeAttr.m_uLayerID;
        //pNode->m_uCrossType = nodeAttr.m_uCrossType;	//LS	穿越类型，未使用
        pNode.m_uStationType = nodeAttr.m_uStationType;
        pNode.m_uStationTempId = nodeAttr.m_uStationTempId;
        pNode.m_uStationId = nodeAttr.m_uStationId;

        pNode.m_uCarrierType = nodeAttr.m_uCarrierType;  //车辆类型
        pNode.m_uOnLine = nodeAttr.m_uOnLine;            //允许上线
    }

    /**
     * 修改指定路径的属性
     *
     * @param PathAttr 包含新属性的路径属性对象
     */
    public void updatePathAttr(Path PathAttr) {
        Path pPath = null;
        for (int i = 0; i < m_PathBase.m_uCount; i++) {
            pPath = m_PathBase.m_pPathIdx[i].m_ptr;
            if (pPath.m_uId == PathAttr.m_uId) {
                break;
            }
        }

        assert pPath != null;
        pPath.m_uExtType = PathAttr.m_uExtType;

        pPath.m_fVeloLimit[0] = PathAttr.m_fVeloLimit[0];
        pPath.m_fVeloLimit[1] = PathAttr.m_fVeloLimit[1];
        pPath.m_uGuideType = PathAttr.m_uGuideType;
        pPath.m_uMoveHeading = PathAttr.m_uMoveHeading;
        pPath.m_uFwdRotoScannerObstacle = PathAttr.m_uFwdRotoScannerObstacle;
        pPath.m_uFwdObdetectorObstacle = PathAttr.m_uFwdObdetectorObstacle;
        pPath.m_uBwdRotoScannerObstacle = PathAttr.m_uBwdRotoScannerObstacle;
        pPath.m_uBwdObdetectorObstacle = PathAttr.m_uBwdObdetectorObstacle;
        pPath.m_uPathHeading = PathAttr.m_uPathHeading;
        pPath.m_fNavParam = PathAttr.m_fNavParam;

        pPath.m_uCarrierType = PathAttr.m_uCarrierType;  //车辆类型
        pPath.m_uOnLine = PathAttr.m_uOnLine;            //允许上线
    }

    /*********************
     功能：
     拉伸曲线路径控制点。
     参数说明：
     PathID：路径ID。
     point：当前点的位置。
     ***********************/
    public void DragPath(int KeyID, int PathID, Point2d pt, int nMoveMode) {
        Point2d ptFoot = new Point2d();
        GenericPath pPath = (GenericPath) (m_PathBase.m_pPathIdx[PathID].m_ptr);
        Bezier Curve = pPath.m_Curve;


        Point2d lambda = new Point2d();
        // 计算此点到路径方向直线的投影点
        if (nMoveMode == 0) {
            if (KeyID == 1) {
                Line ln = new Line(Curve.m_ptKey[0], Curve.m_ptKey[1]);
                ln.DistanceToPoint(false, pt, lambda, ptFoot);
                if (abs((ptFoot.x - Curve.m_ptKey[0].x) * (ptFoot.x - Curve.m_ptKey[0].x) + (ptFoot.y - Curve.m_ptKey[0].y) * (ptFoot.y - Curve.m_ptKey[0].y)) < 0.001 || abs((ptFoot.x - Curve.m_ptKey[Curve.m_nCountKeyPoints - 1].x) * (ptFoot.x - Curve.m_ptKey[Curve.m_nCountKeyPoints - 1].x) + (ptFoot.y - Curve.m_ptKey[Curve.m_nCountKeyPoints - 1].y) * (ptFoot.y - Curve.m_ptKey[Curve.m_nCountKeyPoints - 1].y)) < 0.001) {
                    return;
                }
                ////////////////////////////////控制点不允许改变方向
                Point2d pStart = Curve.m_ptKey[0];//pPath->GetStartNode().GetPoint2dObject();
                Point2d pEnd = Curve.m_ptKey[1];//pPath->GetEndNode().GetPoint2dObject();
                //CLine ln(pStart, pEnd);
                //ln.DistanceToPoint(false, pt, NULL, &ptFoot);
                Line Templn = new Line(pStart, ptFoot);
                // 计算直线长度
                float fTotalLen = ptFoot.DistanceTo(pStart);
                // 如果直线太短，返回false
                if (fTotalLen < 1e-3F) return;
                if (abs(ln.m_angSlant.m_fRad - Templn.m_angSlant.m_fRad) > Math.PI / 2) {
                    return;
                }
                /////////////////////////////////
                pt = ptFoot;
            } else if (KeyID == Curve.m_nCountKeyPoints - 2) {
                Line ln = new Line(Curve.m_ptKey[Curve.m_nCountKeyPoints - 2], Curve.m_ptKey[Curve.m_nCountKeyPoints - 1]);
                ln.DistanceToPoint(false, pt, lambda, ptFoot);
                if (abs((ptFoot.x - Curve.m_ptKey[0].x) * (ptFoot.x - Curve.m_ptKey[0].x) + (ptFoot.y - Curve.m_ptKey[0].y) * (ptFoot.y - Curve.m_ptKey[0].y)) < 0.001 || abs((ptFoot.x - Curve.m_ptKey[Curve.m_nCountKeyPoints - 1].x) * (ptFoot.x - Curve.m_ptKey[Curve.m_nCountKeyPoints - 1].x) + (ptFoot.y - Curve.m_ptKey[Curve.m_nCountKeyPoints - 1].y) * (ptFoot.y - Curve.m_ptKey[Curve.m_nCountKeyPoints - 1].y)) < 0.001) {
                    return;
                }
                ////////////////////////////////控制点不允许改变方向
                Point2d pStart = Curve.m_ptKey[Curve.m_nCountKeyPoints - 2];//pPath->GetStartNode().GetPoint2dObject();
                Point2d pEnd = Curve.m_ptKey[Curve.m_nCountKeyPoints - 1];//pPath->GetEndNode().GetPoint2dObject();
                //CLine ln(pStart, pEnd);
                //ln.DistanceToPoint(false, pt, NULL, &ptFoot);
                Line Templn = new Line(ptFoot, pEnd);
                // 计算直线长度
                float fTotalLen = ptFoot.DistanceTo(pEnd);
                // 如果直线太短，返回false
                if (fTotalLen < 1e-3F) return;
                if (abs(ln.m_angSlant.m_fRad - Templn.m_angSlant.m_fRad) > Math.PI / 2) {
                    return;
                }
                /////////////////////////////////
                pt = ptFoot;
            }
        }
        Curve.m_ptKey[KeyID] = pt;
        Curve.CreateSamplePoints();
        pPath.ModifyParmByCurve();
    }


/****************************************************************
 **************************************************************/

    /**
     * 从数据流中读取图层数据
     *
     * @param dis 输入数据流
     */
    public void read(DataInputStream dis) {
        try {
            //占位用
            WorldFileIO.readInt(dis);

            m_PathBase.m_MyNode.CreateParm(dis);
            m_PathBase.read(dis);

            Point2d m_lenth = new Point2d();
            Point2d m_startNode = new Point2d();

            //占位读取
            WorldFileIO.readFloat(dis);
            m_lenth.read(dis);
            m_startNode.read(dis);

        } catch (IOException e) {
            Log.e("readWorld", "读取CLayer异常 r  " + e);
            e.printStackTrace();
        }

    }

    /**
     * 将图层数据保存到数据流中
     *
     * @param dos 输出数据流
     */
    public void save(DataOutputStream dos) {
        try {
            //占位用
            TranBytes tan = new TranBytes();
            dos.writeInt(tan.tranInteger(0));

            m_PathBase.m_MyNode.SaveParm(dos);
            m_PathBase.Save(dos);

            //占位保存
            Point2d m_lenth = new Point2d();
            Point2d m_startNode = new Point2d();
            dos.writeFloat(tan.tranFloat(2.0f));
            m_lenth.Save(dos);
            m_startNode.Save(dos);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
