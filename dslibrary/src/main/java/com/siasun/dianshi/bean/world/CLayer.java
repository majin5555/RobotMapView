package com.siasun.dianshi.bean.world;


import static java.lang.Math.abs;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;

import com.siasun.dianshi.attr.NodeBaseAttr;
import com.siasun.dianshi.attr.PathBaseAttr;
import com.siasun.dianshi.bean.Point2d;
import com.siasun.dianshi.bean.TranBytes;
import com.siasun.dianshi.bean.pp.Angle;
import com.siasun.dianshi.bean.pp.Bezier;
import com.siasun.dianshi.bean.pp.Line;
import com.siasun.dianshi.bean.pp.Posture;
import com.siasun.dianshi.io.WorldFileIO;
import com.siasun.dianshi.utils.CoordinateConversion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;


public class CLayer extends NodeBase {
    // 存第几层 默认是0
    public int m_iLayerIdx;
    public PathBase m_PathBase;

    float[] m_fVeloLimit = new float[2];     //正逆限速
    short m_uGuideType; //导航类型

    //调用public CLayer() {}构造时，调用顺序为：1.调用父类的无参构造函数 2.父类成员变量赋值 3.调用子类构造函数 4.子类成员变量赋值
    public CLayer() {
    }


    //清扫使用,生成弓字形
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

        LinePath pPath = new LinePath((short) nNextID, (short) nStartNodeId, (short) nEndNodeId, m_fVeloLimit, m_uGuideType, (short) 0, (short) 0, (short) 0, m_PathBase.m_MyNode);
        if (pPath == null) return false;

        return m_PathBase.AddPath(pPath);
    }

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

        GenericPath pPath = new GenericPath(nNextID, nStartNodeId, nEndNodeId, pstStart, pstEnd, fLen1, fLen2, m_fVeloLimit, m_uGuideType, (short) 0, (short) 0, (short) 3, m_PathBase.m_MyNode);


        if (pPath == null) return false;

//        boolean res = pPath.isM_bCurvature() && m_PathBase.AddPath(pPath);
//        return res;

        return m_PathBase.AddPath(pPath);
    }

    //pp示教生成曲线
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

        GenericPath pPath = new GenericPath(nNextID, nStartNodeId, nEndNodeId, pstStart, pstEnd, pptCtrl, m_fVeloLimit, m_uGuideType, (short) 0, (short) 0, (short) 3, m_PathBase.m_MyNode, pathParam);
        if (pPath == null) return false;

        return m_PathBase.AddPath(pPath);
    }

    //
//   GetAllNeighborNodes: Get (the ID of) a neighbor node of the specified node.
//   Return: The number of neighboring nodes found.
//
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

    //修改位姿
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
     * 修改NodeID所对应的节点坐标
     *
     * @param pnt          待写入NodeID中的坐标值
     * @param NodeID       待修改的节点Id
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

    //
    //   取得在指定节点的车身姿态(可以有多个)，其实这个方法就相当于给一个中间路段的nodeId，求这个node的角度
    //   获取与nNodeId节点相邻节点的角度，保存在pAngles中，如果有相同角度或者差Pi，只保存一个
    //
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

    public void Draw(CoordinateConversion ScrnRef, Canvas canvas) {
        if (m_PathBase != null && m_PathBase.m_MyNode != null) {
            m_PathBase.Draw(ScrnRef, canvas, Color.BLACK);
        }
    }

    public void DeleteNode(int NodeId) {
        Vector<Integer> vPathID = new Vector();
        for (int i = 0; i < m_PathBase.m_uCount; i++) {
            Path pPath = m_PathBase.m_pPathIdx[i].m_ptr;
            if (pPath.m_uStartNode == NodeId || pPath.m_uEndNode == NodeId)
                vPathID.add(i);
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
            romoveID = -1;
            m_PathBase.RemovePath(DeletePathID.get(i));
            romoveID = DeletePathID.get(i);
        }
//	m_nCurPathId = -1;
        DeletePathID.clear();
    }


    //现阶段判断只有一条线，或者两条线包含该节点
    //TaskNodeId：NodeId节点所在的任务，如果包含该点的有连个路段，要自动连接以前的两个路段
//    public void DeleteNode(int NodeId, Vector<Integer> TaskNodeId) {
//        Vector<Integer> vPathID = new Vector();
//        //得到包含节点的路径
//        for (int i = 0; i < m_PathBase.m_uCount; i++) {
//            Path pPath = m_PathBase.m_pPathIdx[i].m_ptr;
//            if (pPath.m_uStartNode == NodeId || pPath.m_uEndNode == NodeId)
//                vPathID.add(i);           // 曲线的序号
//        }
//        //如果包含该节点的路径很多，需要增加线，将断开的位置自动连接上
//        int leftNode = -1;
//        int rightNode = -1;
//        for (int i = 0; i < TaskNodeId.size(); i++) //得到节点在任务列表里左右两边的点
//        {
//            if (TaskNodeId.get(i) == NodeId) {
//                if (i > 0) leftNode = TaskNodeId.get(i - 1);
//                if (i < TaskNodeId.size() - 1) rightNode = TaskNodeId.get(i + 1);
//                break;
//            }
//        }
//
//        //
//        //重新计算路径ID，只要包含在该任务中的路径需要删除
//        Vector<Integer> DeletePathID = new Vector();
//        for (int i = 0; i < vPathID.size(); i++) {
//            Path pPath = m_PathBase.m_pPathIdx[vPathID.get(i)].m_ptr;
//            //判断该节点是否为属于一条路径
//            if (leftNode == -1) {
//                if (pPath.m_uEndNode == rightNode || pPath.m_uStartNode == rightNode) {
//                    DeletePathID.add(vPathID.get(i));
//                }
//            } else if (rightNode == -1) {
//                if (pPath.m_uEndNode == leftNode || pPath.m_uStartNode == leftNode) {
//                    DeletePathID.add(vPathID.get(i));
//                }
//            } else {
//                if ((pPath.m_uEndNode == leftNode || pPath.m_uStartNode == leftNode) || (pPath.m_uEndNode == rightNode || pPath.m_uStartNode == rightNode)) {
//                    DeletePathID.add(vPathID.get(i));
//                }
//            }
//        }
//        //增加新线
//        if (leftNode != -1 && rightNode != -1) {
//            // 整个曲线已确定
//            Posture m_pstStart = new Posture();
//            Posture m_pstEnd = new Posture();
//            Bezier m_Bezier = new Bezier();
//            Node nodeS = new Node();
//            Node nodeE = new Node();
//            nodeS = GetNode(leftNode);
//            nodeE = GetNode(rightNode);
//            Angle[] Angles = new Angle[4];
//            int nHeadingAngleCount = GetNodeHeadingAngle(leftNode, Angles, 4);
//            //	if (nHeadingAngleCount == 1)
//            if (nHeadingAngleCount > 0) {
//                m_pstStart.Create(nodeS.GetPoint2dObject(), Angles[0]);
//            }
//
//            int nCount = GetNodeHeadingAngle(rightNode, Angles, 4);
//            //	if (nCount == 1) {
//            if (nCount > 0) {
//                m_pstEnd.Create(nodeE.GetPoint2dObject(), Angles[0]);
//            }
//            boolean flag = m_Bezier.Create(m_pstStart, m_pstEnd, BEZIER_K);
//            if (flag == true) {
//                float fDist1 = m_Bezier.m_ptKey[0].DistanceTo(m_Bezier.m_ptKey[1]);
//                float fDist2 = m_Bezier.m_ptKey[2].DistanceTo(m_Bezier.m_ptKey[3]);
//                AddGenericPath(m_pstStart, m_pstEnd, fDist1, fDist2, leftNode, rightNode);
//            } else {
//                //Toast.makeText();
//            }
//
//        }
//        //选择删除
//        int romoveID = -1;
//        for (int i = 0; i < DeletePathID.size(); i++) {
//            for (int j = i; j < DeletePathID.size(); j++) {
//                if (romoveID != -1 && DeletePathID.get(j) > romoveID) {
//                    DeletePathID.set(j, DeletePathID.get(j) - 1);
//                }
//            }
//            romoveID = -1;
//            m_PathBase.RemovePath(DeletePathID.get(i));
//            romoveID = DeletePathID.get(i);
//        }
////	m_nCurPathId = -1;
//        DeletePathID.clear();
//    }
//
//    public float CalculateSplitRatioB(Point2d pnt, Bezier Curve) {
//        if (Curve.m_nCountKeyPoints > 4)   //分割紧限于三阶beizier
//            return -1;
//
//        Point2d pntB = new Point2d();
//        float t;
//        float lenMin = 10;
//        float ratio = -1;
//        //用遍历求t，这种方法不好
//        for (t = 0; t <= 1; t = t + 0.02f) {
//            Curve.SetCurT(t);
//            pntB = Curve.m_pt;
//
//            float lenT = (float) Math.sqrt((pntB.x - pnt.x) * (pntB.x - pnt.x) + (pntB.y - pnt.y) * (pntB.y - pnt.y));
//            if (lenMin > lenT) {
//                lenMin = lenT;
//                ratio = t;
//            } else continue;
//        }
//        return ratio;
//    }
//
//    public void CalculateNewSplitPath(float e, Bezier Curve, Bezier CurveNew) {
//        //if(Curve.m_nCountKeyPoints>4)   //分割紧限于三阶beizier
//        //	return NULL;
//
//        Curve.SetCurT(e);
//        Point2d NewP = Curve.m_pt;
//        //修正原有的曲线的控制点
//        Point2d orgK1 = new Point2d();
//        Point2d orgK2 = new Point2d();
//        orgK1.x = Curve.m_ptKey[0].x * (1 - e) + Curve.m_ptKey[1].x * e;
//        orgK1.y = Curve.m_ptKey[0].y * (1 - e) + Curve.m_ptKey[1].y * e;
//
//        orgK2.x = Curve.m_ptKey[0].x * (1 - e) * (1 - e) + Curve.m_ptKey[1].x * e * 2 * (1 - e) + Curve.m_ptKey[2].x * e * e;
//        orgK2.y = Curve.m_ptKey[0].y * (1 - e) * (1 - e) + Curve.m_ptKey[1].y * e * 2 * (1 - e) + Curve.m_ptKey[2].y * e * e;
//
//        //新曲线的两个控制点
//        Point2d newK1 = new Point2d();
//        Point2d newK2 = new Point2d();
//        newK1.x = Curve.m_ptKey[1].x * (1 - e) * (1 - e) + Curve.m_ptKey[2].x * e * 2 * (1 - e) + Curve.m_ptKey[3].x * e * e;
//        newK1.y = Curve.m_ptKey[1].y * (1 - e) * (1 - e) + Curve.m_ptKey[2].y * e * 2 * (1 - e) + Curve.m_ptKey[3].y * e * e;
//
//        newK2.x = Curve.m_ptKey[2].x * (1 - e) + Curve.m_ptKey[3].x * e;
//        newK2.y = Curve.m_ptKey[2].y * (1 - e) + Curve.m_ptKey[3].y * e;
//
//        //生成新的曲线
//        //CBezier bezierNew;
//        int nCountKeyPoints = 4;
//        Point2d[] m_ptKey = new Point2d[4];
//        for (int i = 0; i < 4; i++) {
//            m_ptKey[i] = new Point2d();
//        }
//        m_ptKey[0] = NewP;
//        m_ptKey[1] = newK1;
//        m_ptKey[2] = newK2;
//        m_ptKey[3] = Curve.m_ptKey[3];
//        CurveNew.Create(nCountKeyPoints, m_ptKey);
//
//        //修正原有的曲线参数
//        Curve.m_ptKey[1] = orgK1;
//        Curve.m_ptKey[2] = orgK2;
//        Curve.m_ptKey[3] = NewP;
//    }
//
//    public boolean GetLineFootPt(Point2d pnt, Point2d StartPnt, Point2d EndPnt) {
//        Point2d ptFoot = new Point2d();
//        Point2d fLambda = new Point2d();
//        Angle ang = new Angle(StartPnt, EndPnt);
//        Line ln2 = new Line(StartPnt, ang, 10000.0f);
//        ln2.DistanceToPoint(false, pnt, fLambda, ptFoot);
//        pnt.x = ptFoot.x;
//        pnt.y = ptFoot.y;
//        float Len1 = (float) Math.sqrt((StartPnt.x - EndPnt.x) * (StartPnt.x - EndPnt.x) + (StartPnt.y - EndPnt.y) * (StartPnt.y - EndPnt.y));
//        float Len2 = (float) Math.sqrt((StartPnt.x - pnt.x) * (StartPnt.x - pnt.x) + (StartPnt.y - pnt.y) * (StartPnt.y - pnt.y));
//        float Len3 = (float) Math.sqrt((EndPnt.x - pnt.x) * (EndPnt.x - pnt.x) + (EndPnt.y - pnt.y) * (EndPnt.y - pnt.y));
//        if (Len3 + Len2 > (Len1 + 0.1)) {
//            return false;
//        } else {
//            return true;
//        }
//    }

    //判断新增的点在哪条路段中间
    //TaskNodeId：NodeId节点所在的任务，如果包含该点的有两个路段，要自动连接以前的两个路段
//    public boolean AddNode(Point2d pnt, CoordinateConversion ScrnRef) {
////        Point point = ScrnRef.GetWindowPoint(pnt);
//        PointF screen = ScrnRef.worldToScreen(pnt.x, pnt.y);
//        //找到新增的节点所在位置
//        int[] PathId = m_PathBase.PointHitPath(new Point((int) screen.x, (int) screen.y), ScrnRef, -1, -1);
//        int deleteId = PathId[0];
//        if (deleteId < 0) return false;
//        Path pPath = m_PathBase.m_pPathIdx[deleteId].m_ptr;
//        if (pPath.m_uType == 10) {
//            if (((GenericPath) pPath).m_Curve.m_nCountKeyPoints > 4) return false;
//        }
//        if (pPath.m_uType == 10) {
//            // PathBaseAttr PathA = GetPathAttr(pPath.m_uId);
//            PathBaseAttr PathA = GetPathAttr(deleteId);
//            Bezier bezierT = ((GenericPath) pPath).m_Curve;
//            float fRatio = CalculateSplitRatioB(pnt, bezierT);
//            if (fRatio < 0.0f || fRatio > 1.0f) {
//                return false;
//            }
//            Point2d[] keyT = new Point2d[4];
//            for (int i = 0; i < 4; i++) {
//                keyT[i] = new Point2d();
//                keyT[i].x = 0;
//                keyT[i].y = 0;
//            }
//            Bezier bezierN = new Bezier();
//            bezierN.Create(4, keyT);
//            CalculateNewSplitPath(fRatio, ((GenericPath) pPath).m_Curve, bezierN);
//            int nNewNodeID = -1;
//            //修改原有的曲线路径的节点号
//            //int nNewNodeID  = m_AgvWorld->m_pPathIdx[m_AgvWorld->CPathBase::m_uCount-1].m_ptr->m_uStartNode;
//            Angle angSN = new Angle(((GenericPath) pPath).m_Curve.m_ptKey[0], ((GenericPath) pPath).m_Curve.m_ptKey[1]);
//            Angle angEN = new Angle(((GenericPath) pPath).m_Curve.m_ptKey[2], ((GenericPath) pPath).m_Curve.m_ptKey[3]);
//            Posture pstStartN = new Posture(((GenericPath) pPath).m_Curve.m_ptKey[0], angSN);
//            Posture pstEndN = new Posture(((GenericPath) pPath).m_Curve.m_ptKey[3], angEN);
//            float fLen1 = ((GenericPath) pPath).m_Curve.m_ptKey[0].DistanceTo(((GenericPath) pPath).m_Curve.m_ptKey[1]);
//            float fLen2 = ((GenericPath) pPath).m_Curve.m_ptKey[2].DistanceTo(((GenericPath) pPath).m_Curve.m_ptKey[3]);
//            int strNodeID = pPath.m_uStartNode;
//            //         int i = ((GenericPath)pPath).GetTangency();
//            if (((GenericPath) pPath).GetTangency() == false)    //相切
//            {
//                AddGenericPath(pstStartN, pstEndN, fLen1, fLen2, strNodeID, nNewNodeID);
//                //TRACE("11111%f,%f\r\n",fLen1,fLen2);
//            }
//            if (((GenericPath) pPath).GetTangency() == true)    //侧移
//            {
//                //               AddSideGenericPath(pstStartN, pstEndN, fLen1, fLen2, strNodeID,nNewNodeID);
//            }
//            int pathMID = pPath.m_uId;
//            //	m_AgvWorld->RemovePath(PathID);
//            Path pPathNew = m_PathBase.m_pPathIdx[m_PathBase.m_uCount - 1].m_ptr;
//            nNewNodeID = pPathNew.m_uEndNode;
//            //增加新的曲线路径
//            Angle angS = new Angle(bezierN.m_ptKey[0], bezierN.m_ptKey[1]);
//            Angle angE = new Angle(bezierN.m_ptKey[2], bezierN.m_ptKey[3]);
//            Posture pstStart = new Posture(bezierN.m_ptKey[0], angS);
//            Posture pstEnd = new Posture(bezierN.m_ptKey[3], angE);
//            float fDist1 = bezierN.m_ptKey[0].DistanceTo(bezierN.m_ptKey[1]);
//            float fDist2 = bezierN.m_ptKey[2].DistanceTo(bezierN.m_ptKey[3]);
//            //	int nNewNodeID = -1;
//            //          int ii = ((GenericPath)pPath).GetTangency();
//            int endNodeID = pPath.m_uEndNode;
//            if (((GenericPath) pPath).GetTangency() == false)    //相切
//            {
//                AddGenericPath(pstStart, pstEnd, fDist1, fDist2, nNewNodeID, endNodeID);
//                //TRACE("22222%f,%f\r\n",fDist1,fDist2);
//            }
//            if (((GenericPath) pPath).GetTangency() == true)    //侧移
//            {
//                //      m_PathBase.AddSideGenericPath(pstStart, pstEnd, fDist1, fDist2, nNewNodeID,endNodeID);
//            }
//            //保持原有路径中节点属性不变 //2019.11.8
//            //	PathBaseAttr  PathA = GetPathAttr(pPath->m_uId);
//            PathA.m_uId = pPathNew.m_uId;
//            ModifyPathAttr(PathA, pPathNew.m_uId, false);
//
//            NodeBaseAttr NodeSA = GetNodeAttr(pPath.m_uStartNode);
//            NodeSA.m_uId = pPathNew.GetStartNode().m_uId;
//            Point2d TempSA = pPathNew.GetStartPnt();
//            NodeSA.x = TempSA.x;
//            NodeSA.y = TempSA.y;
//            modifyNodeAttr(NodeSA, pPathNew.m_uStartNode);
//
//            NodeBaseAttr NodeEA = GetNodeAttr(pPath.m_uEndNode);
//            NodeEA.m_uId = pPathNew.GetEndNode().m_uId;
//            Point2d TempEA = pPathNew.GetEndPnt();
//            NodeEA.x = TempEA.x;
//            NodeEA.y = TempEA.y;
//            modifyNodeAttr(NodeEA, pPathNew.m_uEndNode);
//            ///////////////////////////////
//
//
//            Path pPathNew2 = m_PathBase.m_pPathIdx[m_PathBase.m_uCount - 1].m_ptr;
//
//            ////修改新路径、节点属性 //2019.11.8
//            //PathA = GetPathAttr(pPath->m_uId);
//            PathA.m_uId = pPathNew2.m_uId;
//            ModifyPathAttr(PathA, pPathNew2.m_uId, false);
//
//            NodeSA = GetNodeAttr(pPath.m_uEndNode);
//            NodeSA.m_uId = pPathNew2.GetStartNode().m_uId;
//            TempSA = pPathNew2.GetStartPnt();
//            NodeSA.x = TempSA.x;
//            NodeSA.y = TempSA.y;
//            modifyNodeAttr(NodeSA, pPathNew2.m_uStartNode);
//
////            pPathNew.SetColor(pPath.GetColor());//1120
////            pPathNew2.SetColor(pPath->GetColor());//1120
//            ///////////////////////////////
//
//            m_PathBase.RemovePath(deleteId);
//
//            //TRACE("RemovePath");
//            pPathNew.m_uId = pathMID;
//            pPathNew2.m_uId = pPathNew2.m_uId - 1;
//        }
//        if (pPath.m_uType == 0) {
//            //TRACE("CyzTest0510\r\n");
//            Point2d ptFoot = new Point2d();
//
//            //CAngle ang(pPath->GetStartNode().GetPoint2dObject(), pPath->GetEndNode().GetPoint2dObject());
//            //CLine ln2(pPath->GetStartNode().GetPoint2dObject(), ang, 10000.0f);
//            //ln2.DistanceToPoint(false, pt, NULL, &ptFoot);
//            boolean ret = GetLineFootPt(pnt, pPath.GetStartNode().GetPoint2dObject(), pPath.GetEndNode().GetPoint2dObject());
//            if (!ret) {
////			AfxMessageBox("当前点投影不在该路径内，不能分割直线。");
//                return false;
//            }
//            Node nodeNew = m_PathBase.m_MyNode.AddNode(pnt, (short) m_iLayerIdx);
//            int startId = nodeNew.m_uId;
//            int endId = pPath.m_uEndNode;
//
//            AddLinePath(pnt, pPath.GetEndNode().GetPoint2dObject(), startId, endId);
//            Path pPathT = m_PathBase.m_pPathIdx[m_PathBase.m_uCount - 1].m_ptr;
//            //2019.11.8
//            //相应的新路径和节点属性也要复制，新节点的属性和终点的属性一致
//            //         PathBaseAttr PathA = GetPathAttr(pPath.m_uId);
//            PathBaseAttr PathA = GetPathAttr(deleteId);
//            int tempId = PathA.m_uEndNode;
//            PathA.m_uEndNode = startId;
//            ModifyPathAttr(PathA, pPath.m_uId, true);
//            PathA.m_uId = pPathT.m_uId;
//            PathA.m_uStartNode = startId;
//            PathA.m_uEndNode = tempId;
//            ModifyPathAttr(PathA, pPathT.m_uId, true);
//
//            NodeBaseAttr NodeSA = GetNodeAttr(pPath.m_uEndNode);
//            NodeSA.m_uId = pPathT.GetStartNode().m_uId;
//            NodeSA.x = pPathT.GetStartPnt().x;
//            NodeSA.y = pPathT.GetStartPnt().y;
//            modifyNodeAttr(NodeSA, pPathT.m_uStartNode);
//
//            /////////////////////////////////
//            pPath.m_uEndNode = nodeNew.m_uId;
//        }
//
//        return true;
//    }

//    public boolean AddNode(Point2d pnt,ScreenReference ScrnRef) {
//        int PosS = -1;
//        int PosE = -1;
//
//        Point point = ScrnRef.GetWindowPoint(pnt);
//        //找到新增的节点所在位置
//        int[] PathId = m_PathBase.PointHitPath(point, ScrnRef, -1, -1);
//        int deleteId = PathId[0];
//        if (deleteId<0)
//            return false;
//
//        PosS = m_PathBase.m_pPathIdx[deleteId].m_ptr.m_uStartNode;
//        PosE = m_PathBase.m_pPathIdx[deleteId].m_ptr.m_uEndNode;
//        Posture pst = new Posture(); //默认角度先为0
//        pst.x = pnt.x;
//        pst.y = pnt.y;
//
//        //增加两段新的线
//        // 整个曲线已确定
//
//        int StartNodeId = -1;
//        int EndNodeId = -1;
//        for (int i = 0; i < 2; i++) {
//            Posture m_pstStart = new Posture();
//            Posture m_pstEnd = new Posture();
//            Bezier m_Bezier = new Bezier();
//            Node nodeS = new Node();
//            Node nodeE = new Node();
//
//            if (i == 0) {
//                nodeS = GetNode(PosS);
//                StartNodeId = PosS;
//                Angle[] Angles = new Angle[4];
//                int nHeadingAngleCount = GetNodeHeadingAngle(PosS, Angles, 4);
//                //	if (nHeadingAngleCount == 1) {
//                if (nHeadingAngleCount > 0) {
//                    m_pstStart.Create(nodeS.GetPoint2dObject(), Angles[0]);
//                }
//                m_pstEnd = pst;
//            }
//
//            if (i == 1) {
//                nodeE = GetNode(PosE);
//                EndNodeId = PosE;
//                Angle[] Angles = new Angle[4];
//                int nCount = GetNodeHeadingAngle(PosE, Angles, 4);
//                //	if (nCount == 1) {
//                if (nCount > 0) {
//                    m_pstEnd.Create(nodeE.GetPoint2dObject(), Angles[0]);
//                }
//                m_pstStart = pst;
//            }
//
//            boolean flag = m_Bezier.Create(m_pstStart, m_pstEnd, 0.7f);
//            if (flag == true) {
//                float fDist1 = -1;
//                float fDist2 = -1;
//                try {
//                    fDist1 = m_Bezier.m_ptKey[0].DistanceTo(m_Bezier.m_ptKey[1]);
//                    fDist2 = m_Bezier.m_ptKey[2].DistanceTo(m_Bezier.m_ptKey[3]);
//                } catch (Exception ex) {
//                    System.out.println(ex.toString());
//                    return false;
//                }
//
//                AddGenericPath(m_pstStart, m_pstEnd, fDist1, fDist2, StartNodeId, EndNodeId);
//                StartNodeId = m_PathBase.m_pPathIdx[m_PathBase.m_uCount - 1].m_ptr.m_uEndNode; //为第二次增加路径准备
//            }
//        }
//
//        //删除原来的线
//        if (deleteId != -1) {
//            m_PathBase.RemovePath(deleteId);
//        }
//        return true;
//    }


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
/*if (m_nCurPathId >= 0)
	m_vCurSelPathIndex.push_back(m_nCurPathId);*/
        //块选择删除
        int romoveID = -1;
        for (int i = 0; i < vPathID.size(); i++) {
            for (int j = i; j < vPathID.size(); j++) {
                if (romoveID != -1 && vPathID.get(j) > romoveID) {
                    vPathID.setElementAt(vPathID.get(j) - 1, j);
                    //	vPathID.get(j) = vPathID.get(j) - 1;
                }
            }
            romoveID = -1;
            Vector<Integer> NodeT = m_PathBase.RemovePath(vPathID.get(i));
            for (int k = 0; k < NodeT.size(); k++) {
                NodeID.add(NodeT.get(k));
            }
            romoveID = vPathID.get(i);
        }
        vPathID.clear();
        return NodeID;
    }

    public NodeBaseAttr GetNodeAttr(int NodeID) {
        NodeBaseAttr nodeAttr = new NodeBaseAttr();
        Node pNode = m_PathBase.m_MyNode.GetNode(NodeID);
        if (pNode == null) {
            nodeAttr.m_uId = -1;
            return nodeAttr;
        }
        nodeAttr.m_uId = pNode.m_uId;
        nodeAttr.m_uType = pNode.m_uType;
        nodeAttr.x = pNode.GetPoint2dObject().x;
        nodeAttr.y = pNode.GetPoint2dObject().y;
        nodeAttr.m_uExtType = pNode.m_uExtType;
        nodeAttr.m_fHeading = pNode.m_fHeading;
        nodeAttr.m_Tag = pNode.m_Tag;
        nodeAttr.m_fChkMarkDist = pNode.m_fChkMarkDist;
        nodeAttr.m_fChkMarkVel = pNode.m_fChkMarkVel;
        nodeAttr.m_fMarkWidth = pNode.m_fMarkWidth;

        nodeAttr.m_fOffset1 = pNode.m_fOffset1;
        nodeAttr.m_fOffset2 = pNode.m_fOffset2;

        nodeAttr.m_fFwdMarkOffset = pNode.m_fFwdMarkOffset;
//        nodeAttr.m_fBwdMarkOffset = pNode.m_fBwdMarkOffset;
        nodeAttr.m_fBwdMarkOffset = pNode.m_fBwdMarkOffset;

//        nodeAttr.m_uLayerId = pNode.m_uLayerID;
        //nodeAttr.m_uCrossType = pNode.m_uCrossType;	//LS	穿越类型，未使用
        nodeAttr.m_uStationType = pNode.m_uStationType;
        nodeAttr.m_uStationId = pNode.m_uStationId;
        nodeAttr.m_uStationTempId = pNode.m_uStationTempId;

        nodeAttr.m_uLayerId = (short) m_iLayerIdx;

        nodeAttr.m_uCarrierType = pNode.m_uCarrierType;  //车辆类型
        nodeAttr.m_uOnLine = pNode.m_uOnLine;            //允许上线
        return nodeAttr;
    }

    public PathBaseAttr GetPathAttr(int PathID) {
        PathBaseAttr PathA = new PathBaseAttr();
        Path pPath;

        pPath = m_PathBase.m_pPathIdx[PathID].m_ptr;


        PathA.m_uId = pPath.m_uId;
        PathA.m_uLayerId = pPath.m_LayerID;
//	ModifyPathID(pPath->m_uId, PathA.->m_uId);
        PathA.m_uType = pPath.m_uType;
        PathA.m_uExtType = pPath.m_uExtType;
        PathA.m_uStartNode = pPath.m_uStartNode;
        PathA.m_uEndNode = pPath.m_uEndNode;
        PathA.m_fVeloLimit[0] = pPath.m_fVeloLimit[0];
        PathA.m_fVeloLimit[1] = pPath.m_fVeloLimit[1];
        PathA.m_uGuideType = pPath.m_uGuideType;
        PathA.m_uFwdRotoScannerObstacle = pPath.m_uFwdRotoScannerObstacle;
        PathA.m_uFwdObdetectorObstacle = pPath.m_uFwdObdetectorObstacle;
        PathA.m_uBwdRotoScannerObstacle = pPath.m_uBwdRotoScannerObstacle;
        PathA.m_uBwdObdetectorObstacle = pPath.m_uBwdObdetectorObstacle;
        PathA.m_uPathHeading = pPath.m_uPathHeading;
        PathA.m_fSize = pPath.m_fSize;
        PathA.m_fNavParam = pPath.m_fNavParam;
        PathA.m_uMoveHeading = pPath.m_uMoveHeading;
//	PathA.m_fAngle = pPath->m_fAngle;

        if (pPath.m_uType == 10) {
            PathA.m_nCountKeyPoints = ((GenericPath) pPath).m_Curve.m_nCountKeyPoints;
            PathA.m_ptKey = ((GenericPath) pPath).m_Curve.m_ptKey;
            short count = 0;
            //      ((GenericPath)pPath).m_Curve.GetSamplePoints(m_fSampleLen,count);
            PathA.m_SamCount = count;
            //      PathA.m_ptSam=((GenericPath)pPath).m_Curve.pntSam;
            Point2d ptStart = ((GenericPath) pPath).m_Curve.m_ptKey[0];
            Point2d ptEnd = ((GenericPath) pPath).m_Curve.m_ptKey[1];
            Line ln = new Line(ptStart, ptEnd);
            Angle angS = ln.GetSlantAngle();
            PathA.m_fPathStartHeading = angS.m_fRad * 180 / Math.PI;    //曲线路段起点方向角

            Point2d ptStart1 = ((GenericPath) pPath).m_Curve.m_ptKey[PathA.m_nCountKeyPoints - 2];
            //CPoint2d ptEnd1 = ((CGenericPath*)pPath)->GetEndPnt();
            Point2d ptEnd1 = ((GenericPath) pPath).m_Curve.m_ptKey[PathA.m_nCountKeyPoints - 1];

            Line ln1 = new Line(ptStart1, ptEnd1);
            Angle angE = ln1.GetSlantAngle();

            PathA.m_fPathEndHeading = angE.m_fRad * 180 / Math.PI;        //曲线路段终点方向角

            //路段限制参数（轮最大线速度，车最大角速度，最大角加速度，舵角最大变化量，） 20200331
            PathA.m_fVelMax = ((GenericPath) pPath).m_fVelMax;                //最弱的轮的最大线速度（m/s）
            PathA.m_fThitaDiffMax = ((GenericPath) pPath).m_fThitaDiffMax;    //最弱的舵的最大角速度(rad/s)

            PathA.m_fAngVelMax = ((GenericPath) pPath).m_fAngVelMax;            //运动中心最大角速度（rad/s）
            PathA.m_fAngVelACC = ((GenericPath) pPath).m_fAngVelACC;            //运动中心最大角加速度(rad/s/s)
            PathA.m_fVelACC = ((GenericPath) pPath).m_fVelACC;                //运动中心最大线加速度(m/s/s)

            PathA.m_fLenForAngJump = ((GenericPath) pPath).m_fLenForAngJump;                        //路段端点的打舵距离(m)
            PathA.m_fThitaDiffMaxForStAndEd = ((GenericPath) pPath).m_fThitaDiffMaxForStAndEd;    //最弱的舵在路段端点的最大角速度(rad/s)

            //        PathA.m_fLamdaStart = ((GenericPath)pPath).GetBezierLamdaStart();//20200415
            //        PathA.m_fLamdaEnd = ((GenericPath)pPath).GetBezierLamdaEnd();//20200415
        } else {
            PathA.m_SamCount = 0;
            PathA.m_ptSam = null;
            PathA.m_nCountKeyPoints = 0;
            PathA.m_ptKey = null;

            Point2d ptStart = pPath.GetStartPnt();
            Point2d ptEnd = pPath.GetEndPnt();
            Line ln = new Line(ptStart, ptEnd);
            Angle ang = ln.GetSlantAngle();
            PathA.m_fangle = ang.m_fRad * 180 / Math.PI;               //直线路段的角度
            //康凯要求给直线的起点和终点角度也赋值
            PathA.m_fPathStartHeading = PathA.m_fangle;
            PathA.m_fPathEndHeading = PathA.m_fangle;
//20200411
            //路段限制参数（轮最大线速度，车最大角速度，最大角加速度，舵角最大变化量，） 20200331
            PathA.m_fVelMax = 0;                //最弱的轮的最大线速度（m/s）
            PathA.m_fThitaDiffMax = 0;    //最弱的舵的最大角速度(rad/s)

            PathA.m_fAngVelMax = 0;            //运动中心最大角速度（rad/s）
            PathA.m_fAngVelACC = 0;            //运动中心最大角加速度(rad/s/s)
            PathA.m_fVelACC = 0;                //运动中心最大线加速度(m/s/s)

            PathA.m_fLenForAngJump = 0;                        //路段端点的打舵距离(m)
            PathA.m_fThitaDiffMaxForStAndEd = 0;    //最弱的舵在路段端点的最大角速度(rad/s)

            PathA.m_fLamdaStart = 0; //20200415
            PathA.m_fLamdaEnd = 0; //20200415

        }

        //2019.11.8
        if (pPath.m_uType == 0)  // LINE_TYPE
        {
		/*pPath->m_uType = SIDE_TYPE;
		((CSidePath*)pPath)->m_angHeading.m_fRad = PathA.m_angShiftHeading/180*PI; */
            PathA.m_uShift = 0;
        }
        if (pPath.m_uType == 4)  // SIDE_TYPE
        {
            PathA.m_uShift = 1;
            PathA.m_angShiftHeading = ((SidePath) pPath).m_angHeading.m_fRad;

        }
        if (pPath.m_uType == 10) {
            if (!((GenericPath) pPath).GetTangency())//不相切
            {
                PathA.m_uShift = 0;
                //             PathA.m_angShiftHeading = ((SidePath)pPath).m_angShiftHeading.m_fRad;
            } else {
                PathA.m_uShift = 1;
                //             PathA.m_angShiftHeading= ((SidePath)pPath).m_angShiftHeading.m_fRad;
            }
        }

        PathA.m_uCarrierType = pPath.m_uCarrierType;  //车辆类型
        PathA.m_uOnLine = pPath.m_uOnLine;            //允许上线
        return PathA;
    }

    //修改节点属性
    public void modifyNodeAttr(NodeBaseAttr nodeAttr, int NodeID) {
        Node pNode = m_PathBase.m_MyNode.GetNode(NodeID);

        pNode.m_uType = nodeAttr.m_uType;

        Point2d m_pnt = new Point2d();
        m_pnt.x = (float) nodeAttr.x;
        m_pnt.y = (float) nodeAttr.y;

        //修改节点坐标
        ModifyNodeCoord(m_pnt, NodeID, -1);
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

        pNode.m_uLayerID = nodeAttr.m_uLayerId;
        //pNode->m_uCrossType = nodeAttr.m_uCrossType;	//LS	穿越类型，未使用
        pNode.m_uStationType = nodeAttr.m_uStationType;
        pNode.m_uStationTempId = nodeAttr.m_uStationTempId;
        pNode.m_uStationId = nodeAttr.m_uStationId;

        pNode.m_uCarrierType = nodeAttr.m_uCarrierType;  //车辆类型
        pNode.m_uOnLine = nodeAttr.m_uOnLine;            //允许上线


//        if (nodeAttr.m_uId != pNode->m_uId)
//        {
//            ModifyNodeID(pNode->m_uId, nodeAttr.m_uId);
//        }

    }

    //修改路段属性
    public void ModifyPathAttr(PathBaseAttr PathAttr, int PathID, boolean bChangeId) {
        Path pPath = null;
        for (int i = 0; i < m_PathBase.m_uCount; i++) {
            pPath = m_PathBase.m_pPathIdx[i].m_ptr;
            if (pPath.m_uId == PathID) {
                break;
            }
        }

        pPath.m_uExtType = PathAttr.m_uExtType;
        //1106设置路段属性使用默认节点编号
        if (bChangeId) {
            pPath.m_uStartNode = PathAttr.m_uStartNode;
            pPath.m_uEndNode = PathAttr.m_uEndNode;
        }

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
//        if (pPath.m_uId != PathAttr.m_uId)
//            ModifyPathID(pPath.m_uId, PathA.m_uId);

        if (pPath.m_uType == 10) {
            //路段限制参数（轮最大线速度，车最大角速度，最大角加速度，舵角最大变化量，） 20200331
            ((GenericPath) pPath).m_fVelMax = PathAttr.m_fVelMax;                //最弱的轮的最大线速度（m/s）
            ((GenericPath) pPath).m_fThitaDiffMax = PathAttr.m_fThitaDiffMax;    //最弱的舵的最大角速度(rad/s)

            ((GenericPath) pPath).m_fAngVelMax = PathAttr.m_fAngVelMax;            //运动中心最大角速度（rad/s）
            ((GenericPath) pPath).m_fAngVelACC = PathAttr.m_fAngVelACC;            //运动中心最大角加速度(rad/s/s)
            ((GenericPath) pPath).m_fVelACC = PathAttr.m_fVelACC;                //运动中心最大线加速度(m/s/s)

            ((GenericPath) pPath).m_fLenForAngJump = PathAttr.m_fLenForAngJump;                        //路段端点的打舵距离(m)
            ((GenericPath) pPath).m_fThitaDiffMaxForStAndEd = PathAttr.m_fThitaDiffMaxForStAndEd;    //最弱的舵在路段端点的最大角速度(rad/s)

            //((CGenericPath*)pPath)->m_Curve.SetLineAcc(PathA.m_fVelACC);
            //((CGenericPath*)pPath)->m_Curve.SetAngAcc(PathA.m_fAngVelACC);

            //((CGenericPath*)pPath)->SetTangency(false); //非相切
            //((CGenericPath*)pPath)->SetTangency(true);//1118 平移true
//            if (((GenericPath)pPath).GetTangency() == 1)		//1 侧移
//            {
//                pPath.m_angShiftHeading.m_fRad =PathAttr.m_angShiftHeading;
//            }
        }
//        if (pPath.m_uType == SIDE_TYPE)
//        {
//            ((SidePath)pPath).m_angHeading.m_fRad = PathAttr.m_angShiftHeading;
//        }

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
     * 读取
     *
     * @param dis
     */
    public void create(DataInputStream dis) {
        try {
            this.m_iLayerIdx = WorldFileIO.readInt(dis);

            m_PathBase = new PathBase();
            m_PathBase.m_MyNode = GetNodeBaseObject(); //此处GetNodeBaseObject()返回的是CLayer的对象
            m_PathBase.m_MyNode.CreateParm(dis);

            m_PathBase.Create(dis);


            Point2d m_lenth = new Point2d();
            Point2d m_startNode = new Point2d();

            float m_radio = WorldFileIO.readFloat(dis);
            m_lenth.Create(dis);
            m_startNode.Create(dis);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存
     *
     * @param dos
     */
    public void Save(DataOutputStream dos) {
        try {
            TranBytes tan = new TranBytes();
            dos.writeInt(tan.tranInteger(m_iLayerIdx));

            if (m_PathBase != null) {
                if (m_PathBase.m_MyNode == null) {
                    m_PathBase.m_MyNode = new NodeBase();
                }
                m_PathBase.m_MyNode = GetNodeBaseObject();

                m_PathBase.m_MyNode.SaveParm(dos);
                m_PathBase.Save(dos);
                // 占位用
                float m_radio = 2.0f;
                Point2d m_lenth = new Point2d();
                Point2d m_startNode = new Point2d();
                dos.writeFloat(tan.tranFloat(m_radio));
                m_lenth.Save(dos);
                m_startNode.Save(dos);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
