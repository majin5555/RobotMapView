package com.siasun.dianshi.bean.world;

import android.graphics.Canvas;
import android.graphics.Point;

import com.siasun.dianshi.utils.CoordinateConversion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;


public class PathBase {

    // Clean up the memory occupied by the path data base
    public PathIndex[] m_pPathIdx;        // Head pointer to the indexes
    public short m_uCount;               // Total number of paths
    public NodeBase m_MyNode;
    /////////////////////////////////////////////////////////////////////////////
    //   Implementation of class "PathBase".

    public PathBase() {
        m_uCount = 0;
        m_pPathIdx = null;
    }

    private void CleanUp() {
        for (short i = 0; i < m_uCount; i++) {
            if (m_pPathIdx[i].m_ptr != null)
                m_pPathIdx[i].m_ptr = null;
        }

        if (m_pPathIdx != null)
            m_pPathIdx = null;

        m_uCount = 0;
        m_pPathIdx = null;
    }

    PathBase GetPathBaseObject() {
        return this;
    }

    public Path GetPathPointer(short uPath) {
        for (short i = 0; i < m_uCount; i++) {
            Path pPath = m_pPathIdx[i].m_ptr;

            if (pPath.m_uId == uPath)
                return pPath;
        }

        return null;
    }

    public Path GetPathPointer(int uNode1, int uNode2) {
        for (short i = 0; i < m_uCount; i++) {
            Path pPath = m_pPathIdx[i].m_ptr;

            int uStartNode = pPath.m_uStartNode;
            int uEndNode = pPath.m_uEndNode;

            if ((uNode1 == uStartNode && uNode2 == uEndNode) ||
                    (uNode1 == uEndNode && uNode2 == uStartNode))
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

            if (nPathType >= 0 && pPath.m_uType != nPathType)
                continue;

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

    public void Create(DataInputStream dis) {
        try {
            int ch1 = dis.read();
            int ch2 = dis.read();
            if ((ch1 | ch2) < 0) {
                return;//throw new EOFException();
            }

            this.m_uCount = (short) ((ch2 << 8) + (ch1 << 0));// Init the count of paths

            // Allocate memory for the path indexes
            this.m_pPathIdx = new PathIndex[m_uCount];
            for (int i = 0; i < m_uCount; i++) {
                m_pPathIdx[i] = new PathIndex();
            }

            //assert (this.m_pPathIdx != null);

            for (short i = 0; i < this.m_uCount; i++) {
                Path pPath = null;
                short uType;
                ch1 = dis.read();
                ch2 = dis.read();
                if ((ch1 | ch2) < 0) {
                    throw new EOFException();
                }

                uType = (short) ((ch2 << 8) + (ch1 << 0));
                switch (uType) {
                    case 0:
//                        pPath = new LinePath();
                        break;
                    case 6:
//                        pPath = new ArcPath();
                        break;
                    case 1:
//                        pPath = new SppPath();
                        break;
                    case 2:
//                        pPath = new SplinePath();
                        break;
                    case 4:
//                        pPath = new SidePath();
                        break;
                    case 3:
//                        pPath = new ScpPath();
                        break;
                    case 5:
//                        pPath = new LazySPath();
                        break;
                    case 10:
                        pPath = new GenericPath();
                        break;
                    case 100:
                        //pPath = new CUnknownPath;
                        break;
                    default:
                        //assert (false);
                        break;
                }

                //assert (pPath != null);
                pPath.m_pNodeBase = m_MyNode;
                if (!pPath.Create(dis)) {
                    //assert (false);
                }

                pPath.m_uType = uType;

                m_pPathIdx[i].m_ptr = pPath;
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

            if (uNode == uStartNode)
                nCount++;
            //return uEndNode;

            if (uNode == uEndNode)
                nCount++;
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
            if (pPath.m_uId > nNextID)
                nNextID = pPath.m_uId;
        }
        return (nNextID + 1);
    }

    //
    //   Add a node to the nodes data base.
    //
    public boolean AddPath(Path pPath) {
        // Allocate memory for the path indexes
        PathIndex[] pTemp = new PathIndex[m_uCount + 1];
        if (pTemp == null)
            return false;

        for (short i = 0; i < m_uCount + 1; i++)
            pTemp[i] = new PathIndex();

        for (short i = 0; i < m_uCount; i++)
            pTemp[i] = m_pPathIdx[i];

        pTemp[m_uCount++].m_ptr = pPath;

        //free(m_pPathIdx);
        m_pPathIdx = pTemp;

        if (pPath instanceof GenericPath) {
            return ((GenericPath) pPath).isM_bCurvature();
        }

        return true;
    }

    //返回删除的节点ID
    public Vector<Integer> RemovePath(int uId) {
        Vector<Integer> Node = new Vector();
        short i;

        if (uId >= m_uCount)
            return Node;

        // Allocate memory for the path indexes
        PathIndex[] pTemp = new PathIndex[m_uCount - 1];
        if (pTemp == null)
            return Node;

        for (i = 0; i < m_uCount; i++)
            if (i != uId)
                pTemp[i] = m_pPathIdx[i];
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

    public float Cost(Node pNode1, Node pNode2) {
        Path pPath = GetPathPointer(pNode1.m_uId, pNode2.m_uId);
        if (pPath != null) {
            return pPath.m_fSize;
        } else {
            return 1000000;
        }
    }

    public float Cost(int uNode1, int uNode2) {
        Path pPath = GetPathPointer(uNode1, uNode1);
        if (pPath != null) {
            return pPath.m_fSize;
        } else {
            return 1000000;
        }
    }


    public void Draw(CoordinateConversion scrRef, Canvas Grp, int crPath) {
        for (short i = 0; i < m_uCount; i++) {
            if (m_pPathIdx != null && m_pPathIdx[i].m_ptr != null)
                m_pPathIdx[i].m_ptr.Draw(scrRef, Grp, crPath, 3);
        }
    }

    public void DrawPathID(CoordinateConversion ScrnRef, Canvas Grp) {
        for (int i = 0; i < m_uCount; i++) {
            m_pPathIdx[i].m_ptr.DrawID(ScrnRef, Grp);
        }
    }
}
