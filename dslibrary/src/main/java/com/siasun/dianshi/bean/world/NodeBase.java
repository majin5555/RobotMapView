package com.siasun.dianshi.bean.world;

import android.graphics.Canvas;
import android.graphics.Point;


import com.siasun.dianshi.bean.Point2d;
import com.siasun.dianshi.utils.CoordinateConversion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


public class NodeBase {


    public short m_uCount;       // The number of nodes in the node base
    public Node[] m_paNode;       // The pointer to the nodes array


    int GetNodeID(short uIndex) {
        return m_paNode[uIndex].m_uId;
    }

    // Get the number of nodes in the node base
    short GetNodeCount() {
        return m_uCount;
    }

    // Get the specified node object
    //Node[] GetNode(short uId);

    // Check if the code is a valid node ID
//	boolean IsNode(short uCode);

    //	// Get the pointer to the node data base object
    NodeBase GetNodeBaseObject() {
        return this;
    }

    public short RemoveNode(int uId) {
        short i;

        if (GetNode(uId) == null)
            return -2;

        // Allocate for the new database
        Node[] pTemp = new Node[m_uCount - 1];
        if (pTemp == null)
            return -1;

        // Copy data from the old database
        for (i = 0; i < m_uCount; i++)
            if (m_paNode[i].m_uId != uId)
                pTemp[i] = m_paNode[i];
            else
                break;

        for (; i < m_uCount - 1; i++)
            pTemp[i] = m_paNode[i + 1];


        short uCount = m_uCount;//zhaoqian
        Clear();
        m_paNode = pTemp;
        m_uCount = uCount;//zhaoqian
        m_uCount--;
        return 0;
    }
//
//
//	// Create the node base from a text file
//	boolean Create(FILE *StreamIn);
//
//	// Save the node base to a text file
//	boolean Save(FILE *StreamOut);


    //////////////////////////////////////////////////////////////////////////////
    //   Implementation of class "NodeBase".                                   *

    //
    //   The constructor of class "NodeBase".
    //
    public NodeBase() {
        m_uCount = 0;
        m_paNode = null;
    }

    public NodeBase(short count) {
        this.m_uCount = count;
    }

    //
    //   The destructor of class "NodeBase".
    //


    void Clear() {
        if (m_paNode != null) {
//			delete m_paNode;
            m_uCount = 0;
            m_paNode = null;
        }
    }

    //
//	    Get the specified node object.
    //
    public Node GetNode(int uId) {
        // Make sure the node base was created
        if (m_paNode == null)
            return null;

        for (int i = 0; i < m_uCount; i++)
            if (m_paNode[i].m_uId == uId)
                return m_paNode[i];

        return null;
    }

    //
    //   Check if a code is a valid node ID.
    //
    boolean IsNode(int uCode) {
        return (GetNode(uCode) != null);
    }

    // Get the X coordinate of the left-most point
    public float LeftMost() {
        if (m_uCount == 0)
            return 0.0f;

        float fMost = m_paNode[0].x;
        for (short i = 0; i < m_uCount; i++) {
            if (fMost > m_paNode[i].x)
                fMost = m_paNode[i].x;
        }

        return fMost;
    }

    // Get the Y coordinate of the top-most point
    public float TopMost() {
        if (m_uCount == 0)
            return 0.0f;

        float fMost = m_paNode[0].y;
        for (short i = 0; i < m_uCount; i++) {
            if (fMost < m_paNode[i].y)
                fMost = m_paNode[i].y;
        }

        return fMost;
    }

    // Get the X coordinate of the right-most point
    public float RightMost() {
        if (m_uCount == 0)
            return 0.0f;

        float fMost = m_paNode[0].x;
        for (short i = 0; i < m_uCount; i++) {
            if (fMost < m_paNode[i].x)
                fMost = m_paNode[i].x;
        }

        return fMost;
    }

    // Get the Y coordinate of the bottom-most point
    public float BottomMost() {
        if (m_uCount == 0)
            return 0.0f;

        float fMost = m_paNode[0].y;
        for (short i = 0; i < m_uCount; i++) {
            if (fMost > m_paNode[i].y)
                fMost = m_paNode[i].y;
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
            if (m_paNode[i].m_uId > nNextID)
                nNextID = m_paNode[i].m_uId;
        }

        return nNextID + 1;
    }

//	//
//	//   Verify the node tag.
//	//
//	boolean VerifyNodeTag(Node& nd, CRfId& Tag)
//	{
//		return nd.VerifyTag(Tag);
//	}

//	//
//	//   Find the node with the specified tag ID.
//	//
//	Node* FindNodeByTag(CRfId& Tag)
//	{
//		// Make sure the node base was created
//		if (m_paNode == null)
//			return null;
//
//		for (short i = 0; i < m_uCount; i++)
//			if (m_paNode[i].VerifyTag(Tag))
//				return &m_paNode[i];
//
//		return null;
//	}

    //	//
//	//   ����һ�����ڵ��Ƿ�������һ���ڵ��ϡ�
//	//   ����ֵ��
////	     -1: û���䵽�κ�һ���ڵ���
////	     >=0: �䵽�ڵ��Id��
//	//
    public int PointHitNodeTest(Point pnt, CoordinateConversion ScrnRef) {
        for (int i = 0; i < m_uCount; i++) {
            if (m_paNode[i].PointHitTest(pnt, ScrnRef))
                return m_paNode[i].m_uId;
        }
        return -1;
    }


    //
    //   Create nodes bank data from a text file.
    //
//	boolean Create(FILE *StreamIn)
//	{
//		// Blank the node base if neccessary
//		Clear();
//
//		// Load the total number of nodes
//		if (fscanf(StreamIn, "%u\n", &m_uCount) == EOF)
//			return FALSE;
//
//		// Allocate memory for the nodes
//		m_paNode = new Node[m_uCount];
//
//		// Make sure the memory allocation is successful
//		if (m_paNode == null)
//			return FALSE;
//
//		// Load the nodes data one by one
//		for (short i = 0; i < m_uCount; i++)
//			if (!m_paNode[i].Create(StreamIn))
//				return FALSE;
//
//		return TRUE;
//	}
//
//	//
//	//   Save nodes bank data to a stream.
//	//
//	boolean Save(FILE *StreamOut)
//	{
//		// Save the total number of nodes
//		if (fprintf(StreamOut, "%u\n", m_uCount) == EOF)
//			return FALSE;
//
//	   // Save the nodes data one by one
//		for (short i = 0; i < m_uCount; i++)
//			if (!m_paNode[i].Save(StreamOut))
//				return FALSE;
//
//		return TRUE;
//	}

    //
    //   Add a node to the nodes data base.
    //   Return:
//	     0 - Success
//	    -1 - Out of memory
//	    -2 - A node with the same ID already exist in the data base
    //
    public short AddNode(Node nd) {
        if (GetNode(nd.m_uId) != null)
            return -2;

        Node[] pTemp = new Node[m_uCount + 1];
        if (pTemp == null)
            return -1;

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
        if (AddNode(NewNode) < 0)
            return null;
        //	return false;

        return GetNode(nNewID);
    }

    //
//   在指定位置加入一个新节点。
//
//    public Node AddNode(Point2d pt, short uLayerId) {
//        int nNewID = NextID();
//        Node NewNode = new Node(nNewID, pt, uLayerId);
//        if (AddNode(NewNode) < 0)
//            return null;
//
//        return GetNode(nNewID);
//    }

    //
    //   Delete a node from the nodes data base.
    //   Return:
//	     0 - Success
//	    -1 - Out of memory
//	    -2 - Such node does not exist in the data base
    //
//	SHORT RemoveNode(short uId)
//	{
//		short i;
//
//		if (GetNode(uId) == null)
//			return -2;
//
//		// Allocate for the new database
//		Node* pTemp = new Node[m_uCount-1];
//		if (pTemp == null)
//			return -1;
//
//		// Copy data from the old database
//		for (i = 0; i < m_uCount; i++)
//			if (m_paNode[i].m_uId != uId)
//				pTemp[i] = m_paNode[i];
//			else
//				break;
//				
//		for ( ; i < m_uCount-1; i++)
//			pTemp[i] = m_paNode[i+1];
//
//		Clear();
//		m_paNode = pTemp;
//		m_uCount--;
//
//		return 0;
//	}

    //
    //   Modify the ID of a node.
    //   Return:
//	      0 - Success
//	     -1 - The Specified old node does not exist in the database
//	     -2 - The specified new ID already exists in the database
    //
//	SHORT ModifyNodeID(short uOldId, short uNewId)
//	{
//		Node* pNode = GetNode(uOldId);
//		if (pNode == null)
//			return -1;
//
//		if (GetNode(uNewId) != null)
//			return -2;
//
//		pNode->m_uId = uNewId;
//		return 0;
//	}

//	//
//	//   Modify the type of a node.
//	//
//	boolean ModifyNodeType(short uId, short uNewType)
//	{
//		Node* pNode = GetNode(uId);
//		if (pNode == null)
//			return FALSE;
//
//		pNode->m_uType = uNewType;
//		return TRUE;
//	}
//
//	//
//	//   Modify the extended type of a node.
//	//
//	boolean ModifyNodeExtType(short uId, SHORT uNewExtType)
//	{
//		Node* pNode = GetNode(uId);
//		if (pNode == null)
//			return FALSE;
//
//		pNode->m_uExtType = uNewExtType;
//		return TRUE;
//	}
//
//	//
//	//   Modify the location of a node.
//	//
//	boolean ModifyNodePoint(short uId, float x, float y)
//	{
//		Node* pNode = GetNode(uId);
//		if (pNode == null)
//			return FALSE;
//
//		pNode->x = x;
//		pNode->y = y;
//		return TRUE;
//	}


////	#ifdef _MFC_VER
//	public void Create(ObjectInputStream ois) throws ClassNotFoundException 
//	 {
////	    File file = new File(strFile);
//	    try {
////	        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
//	//
////	        DataInputStream dis = new DataInputStream(bis);
//
//	        // ����ʱ��Ӧ����д���˳��
//
//			// Load the total number of nodes
//			this.m_uCount =(short) ois.readObject();
//
//			// Allocate memory for the nodes
//			this.m_paNode = new Node[this.m_uCount];
//
//			// Make sure the memory allocation is successful
//		//	assert(Obj.m_paNode != null);
//
//			// Load the nodes data one by one
////			for (short i = 0; i < this.m_uCount; i++)
////				m_paNode[i].Create(dis);
//	////
////	        int num = dis.readInt();
////	        byte b = dis.readByte();
////	        String str = dis.readUTF();
//	//
////	        System.out.println(num + "," + b + "," + str);
//	        ois.close();
//	    } catch (FileNotFoundException e) {
//	        e.printStackTrace();
//	    } catch (IOException e) {
//	        e.printStackTrace();
//	    }
//	}// readData

    public void CreateParm(DataInputStream dis) {
        try {
            // ����ʱ��Ӧ����д���˳��
            // Load the total number of nodes
            int ch1 = dis.read();
            int ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                return; //                throw new EOFException();

            this.m_uCount = (short) ((ch2 << 8) + (ch1 << 0));

            // Load the nodes data one by one
            this.m_paNode = new Node[this.m_uCount];
            for (int i = 0; i < this.m_uCount; i++) {
                this.m_paNode[i] = new Node();
                this.m_paNode[i].Create(dis);
            }
//        dis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }// readData

    public void SaveParm(DataOutputStream dis) {
        try {

//			int ch1 = dis.read();
//			int ch2 = dis.read();
//			if ((ch1 | ch2) < 0)
//				throw new EOFException();
//			this.m_uCount =  (short)((ch2 << 8) + (ch1 << 0));

            int ch1 = this.m_uCount;
            int ch2 = this.m_uCount;
            int a1 = (ch1 & 0xff) << 8;
            int a2 = (ch2 >> 8);
            int a3 = a1 + a2;
            short sT = (short) ((ch2 >> 8) + (ch1 & 0xff) << 8);
            sT = (short) a3;
//			dis.writeShort(a3);
            dis.write((ch1 & 0xff));
            dis.write((ch2 >> 8));

            //	dis.writeShort(this.m_uCount);

            // Load the nodes data one by one
//            LogUtil.INSTANCE.i("存储节点个数：" + this.m_uCount, null, "pp");
            for (int i = 0; i < this.m_uCount; i++) {
//                LogUtil.INSTANCE.i("存储节点ID：" + this.m_paNode[i].m_uId, null, "pp");
                this.m_paNode[i].Save(dis);
            }
//        dis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }// readData


    public void DrawNodeID(CoordinateConversion ScrnRef, Canvas Grp) {
        for (short i = 0; i < m_uCount; i++) {
            Node Node = m_paNode[i];
            Node.DrawID(ScrnRef, Grp);
        }
    }


    //packageWorld;
//
//importWorld.Node;
//import Geometry.Point2d;
//import Geometry.ScreenReference;
//
//import android.graphics.Color;
//import android.graphics.Canvas;
//import android.graphics.Point;//import java.awt.Font;
//import java.io.File;
//import java.io.BufferedInputStream;
//import java.io.DataInputStream;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//
//public class NodeBase {
//
//
//	public	short m_uCount;       // The number of nodes in the node base
//	public	Node[]  m_paNode;       // The pointer to the nodes array
//
//
//	short GetNodeID(short uIndex) {return m_paNode[uIndex].m_uId;}
//
//	// Get the number of nodes in the node base
//    short GetNodeCount() {return m_uCount;}
//
//   // Get the specified node object
//	//Node[] GetNode(short uId);
//
//	// Check if the code is a valid node ID
////	boolean IsNode(short uCode);
//
////	// Get the pointer to the node data base object
// 	NodeBase   GetNodeBaseObject() {return  this;}
////
////
////	// Create the node base from a text file
////	boolean Create(FILE *StreamIn);
////
////	// Save the node base to a text file
////	boolean Save(FILE *StreamOut);
//
//
//	
//
//
//
//	//////////////////////////////////////////////////////////////////////////////
//	//   Implementation of class "NodeBase".                                   *
//
//	//
//	//   The constructor of class "NodeBase".
//	//
//	public NodeBase()
//	{
//		m_uCount = 0;
//		m_paNode = null;
//	}
//
//	//
//	//   The destructor of class "NodeBase".
//	//
//	
//
//	void Clear()
//	{
//		if (m_paNode != null)
//		{
////			delete m_paNode;
//			m_uCount = 0;
//			m_paNode = null;
//		}
//	}
//
//	//
////	    Get the specified node object.
//	//
//	Node GetNode(short uId)
//	{
//		// Make sure the node base was created
//		if (m_paNode == null)
//			return null;
//
//		for (int i = 0; i < m_uCount; i++)
//			if (m_paNode[i].m_uId == uId)
//				return m_paNode[i];
//
//		return null;
//	}
//
//	//
//	//   Check if a code is a valid node ID.
//	//
//	boolean IsNode(short uCode)
//	{
//		return (GetNode(uCode) != null);
//	}
//
//	// Get the X coordinate of the left-most point
//	public float LeftMost()
//	{
//		if (m_uCount == 0)
//			return 0.0f;
//
//		float fMost = m_paNode[0].x;
//		for (short i = 0; i < m_uCount; i++)
//		{
//			if (fMost > m_paNode[i].x)
//				fMost = m_paNode[i].x;
//		}
//
//		return fMost;
//	}
//
//	// Get the Y coordinate of the top-most point
//	float TopMost()
//	{
//		if (m_uCount == 0)
//			return 0.0f;
//
//		float fMost = m_paNode[0].y;
//		for (short i = 0; i < m_uCount; i++)
//		{
//			if (fMost < m_paNode[i].y)
//				fMost = m_paNode[i].y;
//		}
//
//		return fMost;
//	}
//
//	// Get the X coordinate of the right-most point
//	float RightMost()
//	{
//		if (m_uCount == 0)
//			return 0.0f;
//
//		float fMost = m_paNode[0].x;
//		for (short i = 0; i < m_uCount; i++)
//		{
//			if (fMost < m_paNode[i].x)
//				fMost = m_paNode[i].x;
//		}
//
//		return fMost;
//	}
//
//	// Get the Y coordinate of the bottom-most point
//	float BottomMost()
//	{
//		if (m_uCount == 0)
//			return 0.0f;
//
//		float fMost = m_paNode[0].y;
//		for (short i = 0; i < m_uCount; i++)
//		{
//			if (fMost > m_paNode[i].y)
//				fMost = m_paNode[i].y;
//		}
//
//		return fMost;
//	}
//
//	//
//	//   Get the width of the map area.
//	//
//	float Width()
//	{
//		return RightMost() - LeftMost();
//	}
//
//	//
//	//   Get the height of the map area.
//	//
//	float Height()
//	{
//		return TopMost() - BottomMost();
//	}
//
//	//
//	//   Get the coordinates of the center point.
//	//
//	Point2d Center()
//	{
//		Point2d pt = new Point2d();
//		pt.x = (RightMost() + LeftMost()) / 2;
//		pt.y = (TopMost() + BottomMost()) / 2;
//		return pt;
//	}
//
//	private int nextID()
//	{
//		int nNextID = 0;
//		for (int i = 0; i < m_uCount; i++)
//		{
//			if (m_paNode[i].m_uId > nNextID)
//				nNextID = m_paNode[i].m_uId;
//		}
//
//		return nNextID + 1;
//	}
//
////	//
////	//   Verify the node tag.
////	//
////	boolean VerifyNodeTag(Node& nd, CRfId& Tag)
////	{
////		return nd.VerifyTag(Tag);
////	}
//
////	//
////	//   Find the node with the specified tag ID.
////	//
////	Node* FindNodeByTag(CRfId& Tag)
////	{
////		// Make sure the node base was created
////		if (m_paNode == null)
////			return null;
////
////		for (short i = 0; i < m_uCount; i++)
////			if (m_paNode[i].VerifyTag(Tag))
////				return &m_paNode[i];
////
////		return null;
////	}
//
////	//
////	//   ����һ�����ڵ��Ƿ�������һ���ڵ��ϡ�
////	//   ����ֵ��
//////	     -1: û���䵽�κ�һ���ڵ���
//////	     >=0: �䵽�ڵ��Id��
////	//
////	int PointHitNodeTest(CPoint& pnt, ScreenReference& ScrnRef)
////	{
////		for (int i = 0; i < m_uCount; i++)
////		{
////			if (m_paNode[i].PointHitTest(pnt, ScrnRef))
////				return m_paNode[i].m_uId;
////		}
////		return -1;
////	}
//
//	//
//	//   Create nodes bank data from a text file.
//	//
////	boolean Create(FILE *StreamIn)
////	{
////		// Blank the node base if neccessary
////		Clear();
////
////		// Load the total number of nodes
////		if (fscanf(StreamIn, "%u\n", &m_uCount) == EOF)
////			return FALSE;
////
////		// Allocate memory for the nodes
////		m_paNode = new Node[m_uCount];
////
////		// Make sure the memory allocation is successful
////		if (m_paNode == null)
////			return FALSE;
////
////		// Load the nodes data one by one
////		for (short i = 0; i < m_uCount; i++)
////			if (!m_paNode[i].Create(StreamIn))
////				return FALSE;
////
////		return TRUE;
////	}
////
////	//
////	//   Save nodes bank data to a stream.
////	//
////	boolean Save(FILE *StreamOut)
////	{
////		// Save the total number of nodes
////		if (fprintf(StreamOut, "%u\n", m_uCount) == EOF)
////			return FALSE;
////
////	   // Save the nodes data one by one
////		for (short i = 0; i < m_uCount; i++)
////			if (!m_paNode[i].Save(StreamOut))
////				return FALSE;
////
////		return TRUE;
////	}
//
//	//
//	//   Add a node to the nodes data base.
//	//   Return:
////	     0 - Success
////	    -1 - Out of memory
////	    -2 - A node with the same ID already exist in the data base
//	//
//	public short  AddNode(Node nd)
//	{
//		if (GetNode(nd.m_uId) != null)
//			return -2;
//
//		Node[] pTemp = new Node[m_uCount+1];
//		if (pTemp == null)
//			return -1;
//
//		for (short i = 0; i < m_uCount; i++)
//			pTemp[i] = m_paNode[i];
//		pTemp[m_uCount] = nd;
//		short uCount = (short) (m_uCount+1);
//
//		Clear();
//		m_paNode = pTemp;
//		m_uCount = uCount;
//
//		return 0;
//	}
//
//	//
//	//   ��ָ��λ�ü���һ���½ڵ㡣
//	//
//	Node AddNode(Point2d pt)
//	{
//		int nNewID = nextID();
//		
//		Node NewNode = new Node((short)nNewID, pt);
//		if (AddNode(NewNode) < 0)
//			return null;
//		//	return false;
//
//		return GetNode((short)nNewID);
//	}
//
//	//
//	//   Delete a node from the nodes data base.
//	//   Return:
////	     0 - Success
////	    -1 - Out of memory
////	    -2 - Such node does not exist in the data base
//	//
////	SHORT RemoveNode(short uId)
////	{
////		short i;
////
////		if (GetNode(uId) == null)
////			return -2;
////
////		// Allocate for the new database
////		Node* pTemp = new Node[m_uCount-1];
////		if (pTemp == null)
////			return -1;
////
////		// Copy data from the old database
////		for (i = 0; i < m_uCount; i++)
////			if (m_paNode[i].m_uId != uId)
////				pTemp[i] = m_paNode[i];
////			else
////				break;
////				
////		for ( ; i < m_uCount-1; i++)
////			pTemp[i] = m_paNode[i+1];
////
////		Clear();
////		m_paNode = pTemp;
////		m_uCount--;
////
////		return 0;
////	}
//
//	//
//	//   Modify the ID of a node.
//	//   Return:
////	      0 - Success
////	     -1 - The Specified old node does not exist in the database
////	     -2 - The specified new ID already exists in the database
//	//
////	SHORT ModifyNodeID(short uOldId, short uNewId)
////	{
////		Node* pNode = GetNode(uOldId);
////		if (pNode == null)
////			return -1;
////
////		if (GetNode(uNewId) != null)
////			return -2;
////
////		pNode->m_uId = uNewId;
////		return 0;
////	}
//
////	//
////	//   Modify the type of a node.
////	//
////	boolean ModifyNodeType(short uId, short uNewType)
////	{
////		Node* pNode = GetNode(uId);
////		if (pNode == null)
////			return FALSE;
////
////		pNode->m_uType = uNewType;
////		return TRUE;
////	}
////
////	//
////	//   Modify the extended type of a node.
////	//
////	boolean ModifyNodeExtType(short uId, SHORT uNewExtType)
////	{
////		Node* pNode = GetNode(uId);
////		if (pNode == null)
////			return FALSE;
////
////		pNode->m_uExtType = uNewExtType;
////		return TRUE;
////	}
////
////	//
////	//   Modify the location of a node.
////	//
////	boolean ModifyNodePoint(short uId, float x, float y)
////	{
////		Node* pNode = GetNode(uId);
////		if (pNode == null)
////			return FALSE;
////
////		pNode->x = x;
////		pNode->y = y;
////		return TRUE;
////	}
//
//
////	#ifdef _MFC_VER
//	
// public void Create( DataInputStream dis) 
// {
////    File file = new File(strFile);
//    try {
////        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
////
////        DataInputStream dis = new DataInputStream(bis);
//
//        // ����ʱ��Ӧ����д���˳��
//
//		// Load the total number of nodes
//		this.m_uCount = dis.readShort();
//
//		// Allocate memory for the nodes
//		this.m_paNode = new Node[this.m_uCount];
//
//		// Make sure the memory allocation is successful
//	//	assert(Obj.m_paNode != null);
//
//		// Load the nodes data one by one
//		for (short i = 0; i < this.m_uCount; i++)
//			m_paNode[i].Create(dis);
//////
////        int num = dis.readInt();
////        byte b = dis.readByte();
////        String str = dis.readUTF();
////
////        System.out.println(num + "," + b + "," + str);
//        dis.close();
//    } catch (FileNotFoundException e) {
//        e.printStackTrace();
//    } catch (IOException e) {
//        e.printStackTrace();
//    }
//}// readData
//
////	CArchive& operator >> (CArchive& ar, NodeBase& Obj)
////	{
////		// Blank the node base if neccessary
////		Obj.Clear();
////
////		// Load the total number of nodes
////		ar >> Obj.m_uCount;
////
////		// Allocate memory for the nodes
////		Obj.m_paNode = new Node[Obj.m_uCount];
////
////		// Make sure the memory allocation is successful
////		assert(Obj.m_paNode != null);
////
////		// Load the nodes data one by one
////		for (short i = 0; i < Obj.m_uCount; i++)
////			ar >> Obj.m_paNode[i];
////
////		return ar;
////	}
//
////	CArchive& operator << (CArchive& ar, NodeBase& Obj)
////	{
////		// Load the total number of nodes
////		ar << Obj.m_uCount;
////
////		// Load the nodes data one by one
////		for (short i = 0; i < Obj.m_uCount; i++)
////			ar << Obj.m_paNode[i];
////
////		return ar;
////	}
//
    public void Draw(CoordinateConversion ScrnRef, Canvas Grp, int cr) {
        for (short i = 0; i < m_uCount; i++) {
            Node Node = m_paNode[i];
            Node.Draw(ScrnRef, Grp, cr);
        }
    }
//
//	public void DrawNodeID(ScreenReference ScrnRef,  Canvas  Grp, Typeface pLogFont)
//	{
//		for (short i = 0; i < m_uCount; i++)
//		{
//			Node  Node = m_paNode[i];
//			Node.DrawID(ScrnRef, Grp, pLogFont);
//		}
//	}
//
}
