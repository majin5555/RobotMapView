package com.siasun.dianshi.bean.pp.world;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;

import com.siasun.dianshi.utils.CoordinateConversion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;


// PathBase类：路径数据库的基类，管理路径索引和节点关联
public class PathBase {

    // 路径索引数组：存储所有路径的索引信息，每个元素指向一条具体路径
    public PathIndex[] m_pPathIdx;        // 路径索引数组，存储所有路径的索引指针
    public short m_uCount;                // 路径总数
    public NodeBase m_MyNode;             // 关联的节点管理对象，包含此路径数据库中的所有节点
    /////////////////////////////////////////////////////////////////////////////
    //   PathBase类的实现部分

    public PathBase() {
        m_uCount = 0;
        m_pPathIdx = null;
    }

    private void CleanUp() {
        for (short i = 0; i < m_uCount; i++) {
            if (m_pPathIdx[i].m_ptr != null) m_pPathIdx[i].m_ptr = null;
        }

        if (m_pPathIdx != null) m_pPathIdx = null;

        m_uCount = 0;
        m_pPathIdx = null;
    }

    PathBase GetPathBaseObject() {
        return this;
    }

    public Path GetPathPointer(short uPath) {
        for (short i = 0; i < m_uCount; i++) {
            Path pPath = m_pPathIdx[i].m_ptr;

            if (pPath.m_uId == uPath) return pPath;
        }

        return null;
    }

    public Path GetPathPointer(int uNode1, int uNode2) {
        for (short i = 0; i < m_uCount; i++) {
            Path pPath = m_pPathIdx[i].m_ptr;

            int uStartNode = pPath.m_uStartNode;
            int uEndNode = pPath.m_uEndNode;

            if ((uNode1 == uStartNode && uNode2 == uEndNode) || (uNode1 == uEndNode && uNode2 == uStartNode))
                return pPath;
        }

        return null;
    }

    /**
     * @param pnt
     * @param ScrnRef
     * @param nHitType
     * @param nPathType
     * @return res[0]- 曲线序号  res[1]-曲线类型
     */
    public int[] PointHitPath(Point pnt, CoordinateConversion ScrnRef, int nHitType, int nPathType) {
        int[] ret = new int[2];
        ret[0] = -1;
        ret[1] = -1;

        for (int i = 0; i < m_uCount; i++) {
            Path pPath = m_pPathIdx[i].m_ptr;

            if (nPathType >= 0 && pPath.m_uType != nPathType) continue;

            int _nHitType = pPath.PointHitTest(pnt, ScrnRef);

            if (_nHitType >= 0) {
                nHitType = _nHitType;           // 0:曲线上， >0:控制点处
                ret[0] = i;
                ret[1] = nHitType;
                return ret;
            }
        }
        return ret;
        //	return -1;
    }

    /**
     * 数据流
     *
     * @param dis
     */
    public void read(DataInputStream dis) {
        try {
            int ch1 = dis.read();
            int ch2 = dis.read();
            if ((ch1 | ch2) < 0) {
                return;//throw new EOFException();
            }

            this.m_uCount = (short) ((ch2 << 8) + (ch1 << 0));// Init the count of paths
            Log.i("readWorld", "路径总数 m_uCount " + m_uCount);

            // 确保路径数量为正数，避免创建负长度数组
            if (this.m_uCount > 0) {
                // Allocate memory for the path indexes
                this.m_pPathIdx = new PathIndex[m_uCount];

                for (int i = 0; i < m_uCount; i++) {
                    m_pPathIdx[i] = new PathIndex();
//                Log.d("readWorld", "路径索引数组，存储所有路径的索引指针 m_pPathIdx[" + i + "] " + m_pPathIdx[i]);
                }

                    for (short i = 0; i < this.m_uCount; i++) {
                        Path pPath = null;
                        short uType;
                        ch1 = dis.read();
                        ch2 = dis.read();
                        if ((ch1 | ch2) < 0) {
                            throw new EOFException();
                        }

                        uType = (short) ((ch2 << 8) + (ch1 << 0));
                        Log.e("readWorld", "uType == " + uType);
                        switch (uType) {
                            case 0:
                                pPath = new LinePath();
                                break;
                            case 10:
                                pPath = new GenericPath();
                                break;
                        }
                        // 确保路径对象创建成功
                        pPath.m_pNodeBase = m_MyNode;
                        pPath.Create(dis);
                        pPath.m_uType = uType;
                        m_pPathIdx[i].m_ptr = pPath;
                    }
            } else {
                this.m_uCount = 0;
                this.m_pPathIdx = null;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void Save(DataOutputStream dis) {
        // Init the count of paths
        try {
            // Save the count of paths
            int ch1 = this.m_uCount;
            int ch2 = this.m_uCount;

            dis.write((ch1 & 0xff));
            dis.write((ch2 >> 8));

            for (short i = 0; i < this.m_uCount; i++) {
                Path pPath;
                pPath = this.m_pPathIdx[i].m_ptr;
                ch1 = pPath.m_uType;
                ch2 = pPath.m_uType;
                short sT = (short) ((ch2 >> 8) + (ch1 & 0xff) << 8);
                dis.writeShort(sT);
                if (!pPath.Save(dis)) {
                    //assert (false); 
                }
            }
            //dis.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public short GetNeighborNode(int uNode) {
        short nCount = 0;
        for (short i = 0; i < m_uCount; i++) {
            Path pPath = m_pPathIdx[i].m_ptr;

            int uStartNode = pPath.m_uStartNode;
            int uEndNode = pPath.m_uEndNode;

            if (uNode == uStartNode) nCount++;
            //return uEndNode;

            if (uNode == uEndNode) nCount++;
            //return uStartNode;
        }
        if (nCount == 1) {
            return -1;
        } else {
            return nCount;
        }
    }

    public boolean ISInRect(int pathID, double minx, double miny, double maxx, double maxy) {
        Path pPath = m_pPathIdx[pathID].m_ptr;
        return pPath.ISInRect(minx, miny, maxx, maxy);
    }


    public int NextID() {
        int nNextID = 0;
        for (int i = 0; i < m_uCount; i++) {
            Path pPath = m_pPathIdx[i].m_ptr;
            if (pPath.m_uId > nNextID) nNextID = pPath.m_uId;
        }
        return (nNextID + 1);
    }

    //
    //   Add a node to the nodes data base.
    //
    public boolean AddPath(Path pPath) {
        // Allocate memory for the path indexes
        PathIndex[] pTemp = new PathIndex[m_uCount + 1];
        if (pTemp == null) return false;

        for (short i = 0; i < m_uCount + 1; i++)
            pTemp[i] = new PathIndex();

        for (short i = 0; i < m_uCount; i++)
            pTemp[i] = m_pPathIdx[i];

        pTemp[m_uCount++].m_ptr = pPath;

        //free(m_pPathIdx);
        m_pPathIdx = pTemp;

//        if (pPath instanceof GenericPath) {
//            boolean mBCurvature = ((GenericPath) pPath).isM_bCurvature();
//            if (!mBCurvature) {
//                Log.d("AddPath", "mBCurvature " + mBCurvature);
//                Log.d("AddPath", "mBCurvature " + ((GenericPath) pPath).m_Curve);
//            }
//
//            return mBCurvature;
//        }

        return true;
    }

    //返回删除的节点ID
    public Vector<Integer> RemovePath(int uId) {
        Vector<Integer> Node = new Vector();
        short i;

        if (uId >= m_uCount) return Node;

        // Allocate memory for the path indexes
        PathIndex[] pTemp = new PathIndex[m_uCount - 1];
        if (pTemp == null) return Node;

        for (i = 0; i < m_uCount; i++)
            if (i != uId) pTemp[i] = m_pPathIdx[i];
            else {
                int uNode1 = m_pPathIdx[uId].m_ptr.m_uStartNode;
                int uNode2 = m_pPathIdx[uId].m_ptr.m_uEndNode;

                // 如果删除路径后它的节点变为孤立节点，则需要将节点也删除
                if (GetNeighborNode(uNode1) == -1) {
                    m_MyNode.RemoveNode(uNode1);
//					Node.add((int)uNode1);
                }
                if (GetNeighborNode(uNode2) == -1) {
                    m_MyNode.RemoveNode(uNode2);
//					Node.add((int)uNode2);
                }
                //delete m_pPathIdx[uId].m_ptr;
                m_pPathIdx[uId].m_ptr = null;
                break;
            }

        m_uCount--;
        for (int j = i; j < m_uCount; j++) {
            pTemp[j] = m_pPathIdx[j + 1];
        }

        m_pPathIdx = pTemp;

        return Node;
    }


    public void Draw(CoordinateConversion scrRef, Canvas Grp, Paint paint) {
        for (short i = 0; i < m_uCount; i++) {
            if (m_pPathIdx != null && m_pPathIdx[i].m_ptr != null)
                m_pPathIdx[i].m_ptr.Draw(scrRef, Grp, paint);
        }
    }

    /**
     * 绘制路线编号
     *
     * @param ScrnRef
     * @param Grp
     * @param paint
     */
//    public void DrawPathID(CoordinateConversion ScrnRef, Canvas Grp, Paint paint) {
//        for (int i = 0; i < m_uCount; i++) {
//            m_pPathIdx[i].m_ptr.DrawID(ScrnRef, Grp, paint);
//        }
//    }
}
