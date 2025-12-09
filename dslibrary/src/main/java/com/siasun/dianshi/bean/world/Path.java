package com.siasun.dianshi.bean.world;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;

import com.siasun.dianshi.bean.Point2d;
import com.siasun.dianshi.bean.TranBytes;
import com.siasun.dianshi.bean.pp.Angle;
import com.siasun.dianshi.utils.CoordinateConversion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;


public abstract class Path {
//  - PATH.CPP -
//
//Implementation of class "Path" - a class defining a generic path in
//AGVS map. It is the base class of other path classes.
//
//Author: Zhang Lei
//Date:   2000. 10. 28
//

//Defines null ID of nodes and paths

    static final short NULLID = (short) (0xFFFF);

    //Enumeration of standard path types
    public static enum TPathType {
        LINE_TYPE(0), SPP_TYPE(1), SPLINE_TYPE(2), SCP_TYPE(3), SIDE_TYPE(4),
        LAZY_S_TYPE(5), ARC_TYPE(6), SPP_SHIFT_TYPE(9), GENERIC_TYPE(10),
        UNKNOWN_PATH_TYPE(100);

        private int value;

        private TPathType(int value) {
            this.value = value;
        }

    }

    ;

////Enumeration of standard path types
//public enum TPathType {LINE_TYPE(0), SPP_TYPE(1), SPLINE_TYPE(2), SCP_TYPE(3), SIDE_TYPE(4),
//			    LAZY_S_TYPE(5), ARC_TYPE(6), SPP_SHIFT_TYPE(9), GENERIC_TYPE(10),
//			    UNKNOWN_PATH_TYPE(100);
//
//		private int value;
//		private TPathType(int value){
//		    this.value = value;
//		}
//};

    public enum THeadingRule {POSITIVE_HEADING, NEGATIVE_HEADING}

    ;

    //Enumeration of path guidance type
//enum TGuideType {NO_GUIDANCE = 0, TAPE_GUIDANCE = 2, LASER_GUIDANCE = 1, TAPE_LASER_GUIDANCE = 3};
    static final short NO_GUIDANCE = ((short) 0);
    static final short TAPE_GUIDANCE = ((short) 1);
    static final short LASER_GUIDANCE = ((short) 2);
    static final short TAPE_LASER_GUIDANCE = ((short) 3);
    static final short OD_LEFT_GUIDANCE = ((short) 4);
    static final short OD_RIGHT_GUIDANCE = ((short) 5);

    static final float COMMON_CARRIER_COST = (100.0f);

    //��֧���Ͷ���
    static final int NO_BRANCH = 0;                       // �޷�֧
    static final int LEFT_BRANCH = 1;                     // ���֧
    static final int RIGHT_BRANCH = 2;                    // �ҷ�֧
    static final int ERROR_BRANCH = 3;                    // ��֧����2��������

//////////////////////////////////////////////////////////////////////////////


//class OS_API Path{ static CNodeBase* m_pNodeBase; }


    //class Path


    public int m_uId;                  // Path ID number, 从1开始
    public short m_uType;                // Path topological type
    public short m_uExtType;             // Extended path type
    private short m_uExtType2;
    private short m_uExtBit;
    public int m_uStartNode;           // The ID of the start node
    public int m_uEndNode;             // The ID of the end node
    public float m_fSize;                // Size of the path
    public float[] m_fVeloLimit;        // The velocity limit on the path
    public short m_uGuideType;           // The guide method on this path

    public float m_fForDist;        // 前进距离
    public float m_fBackDist;       // 后退距离
    public float m_fLengthNoGuide;  // 盲走距离

    // 0 - Dead reckoning; D0 - magnetic; D1 - laser
    public float m_fNavParam;            // Navigation parameter
    public short m_uObstacle;            // Obstacle basic checking data
    public int m_uFwdRotoScannerObstacle;
    public int m_uFwdObdetectorObstacle;
    public int m_uBwdRotoScannerObstacle;
    public int m_uBwdObdetectorObstacle;

    public boolean m_bTimeOrVel;            // 0 - Time , 1 - Velocity
    public float[] m_fTimeValue;            // Time value
    Angle m_angShiftHeading; // ����ƽ�Ʒ����
    public short m_uMoveHeading = 1;      // 行走方向 1：起点 -> 终点;2：终点 -> 起点;3：双向通行;4：禁止通行
    public short m_uPathHeading;          //·�γ�ͷ���� ǰ ��
    public short m_LayerID;               //所在层的ID

    public short m_uCarrierType = 255;  //车辆类型，默认：255
    public short m_uOnLine = 1;         //允许上线，1：允许上线，0：不允许；默认：1

    public int m_clr = 255;              //绘图颜色

    //#ifdef _MFC_VER
//	Color m_clr;	//��ͼ��ɫ
    Color m_oldclr;    //��ѡ���߶�ʱ����Ҫ����ԭʼ��ɫ
//#endif

    //	public static CNodeBase m_pNodeBase; // Pointer to the nodes data base
//	public static NodeBase m_pNodeBase = null;   //后续应该注释掉，多层之后没意义了
    public NodeBase m_pNodeBase;

    //	abstract  void Draw(ScreenReference ScrnRef,  Canvas  Grp, Color crPath);
    // The default constructor
    public Path() {
        m_uObstacle = 0;
        m_fVeloLimit = new float[2];
        m_fTimeValue = new float[2];
    }

//	void Create(short uId, short uStartNode, short uEndNode, float fVeloLimit,
//			short uType, short uGuideType,float fNavParam, short uExtType);

    // Get the guide method on this path
    public short GuideType() {
        return m_uGuideType;
    }


//////////////////////////////////////////////////////////////////////////////
//Implementation of class "Path"

    Path(int uId, int uStartNode, int uEndNode, float[] fVeloLimit, short uType, short uGuideType, float fNavParam, short uExtType, NodeBase nodeBase) {
//	SetColor(RGB(255,255,255));
//	SetOldColor(RGB(255,255,255));
        Create(uId, uStartNode, uEndNode, fVeloLimit, uType, uGuideType, fNavParam, uExtType, nodeBase);
    }

    abstract public void Draw(CoordinateConversion ScrnRef, Canvas Grp, int cr, int nWidth);

    abstract public void DrawID(CoordinateConversion ScrnRef, Canvas Grp);

    abstract public Angle GetHeading(Node nd);

    abstract public boolean ISInRect(double minx, double miny, double maxx, double maxy);

    abstract int PointHitTest(Point pnt, CoordinateConversion ScrnRef);

    public void Create(int uId, int uStartNode, int uEndNode, float[] fVeloLimit, short uType, short uGuideType,
                       float fNavParam, short uExtType, NodeBase nodeBase) {
        // Make sure the nodes data base is already setup
        //assert m_pNodeBase != null?true:false;

        m_uId = uId;
        m_uType = uType;
        m_uExtType = uExtType;
        m_uStartNode = uStartNode;
        m_uEndNode = uEndNode;
        m_fVeloLimit[0] = fVeloLimit[0];
        m_fVeloLimit[1] = fVeloLimit[1];
        m_uGuideType = uGuideType;
        m_fNavParam = fNavParam;
        m_uObstacle = 0;
        m_uPathHeading = (short) (THeadingRule.POSITIVE_HEADING.ordinal());

        m_uFwdRotoScannerObstacle = 0;
        m_uFwdObdetectorObstacle = 65535;
        m_uBwdRotoScannerObstacle = 0;
        m_uBwdObdetectorObstacle = 65535;
        m_uMoveHeading = (short) 3;
        m_pNodeBase = nodeBase;
    }

    //pp创建示教路径
    public void CreatePP(int uId, int uStartNode, int uEndNode, float[] fVeloLimit, short uType, short uGuideType,
                         float fNavParam, short uExtType, NodeBase nodeBase, short pathParam) {
        // Make sure the nodes data base is already setup
        //assert m_pNodeBase != null?true:false;

        m_uId = uId;
        m_uType = uType;
        m_uExtType = uExtType;
        m_uStartNode = uStartNode;
        m_uEndNode = uEndNode;
        m_fVeloLimit[0] = fVeloLimit[0];
        m_fVeloLimit[1] = fVeloLimit[1];
        m_uGuideType = uGuideType;
        m_fNavParam = fNavParam;
        m_uObstacle = 0;
        m_uPathHeading = (short) (THeadingRule.POSITIVE_HEADING.ordinal());

        m_uFwdRotoScannerObstacle = 0;
        m_uFwdObdetectorObstacle = 65535;
        m_uBwdRotoScannerObstacle = 0;
        m_uBwdObdetectorObstacle = 65535;
        m_uMoveHeading = /*(short) Params.MOVE_DIRECTION;*/pathParam;
        m_pNodeBase = nodeBase;
    }

    //
//Get the pointer to the start node.
//
    public Node GetStartNode() {
        Node pNode = new Node();
        pNode = m_pNodeBase.GetNode(m_uStartNode);
        //assert pNode != null ?true:false;
        return pNode;
    }

    //
//Get the pointer to the end node.
//
    public Node GetEndNode() {
        Node pNode = new Node();
        pNode = m_pNodeBase.GetNode(m_uEndNode);
        //assert pNode != null?true:false;
        return pNode;
    }

    //
//Get the world point of the start node.
//
    public Point2d GetStartPnt() {
        Node nd = new Node();
        nd = GetStartNode();
        Point2d pt = new Point2d();
        pt = nd.GetPoint2dObject();
        return pt;
    }

    //
//Get the world point of the end node.
//
    public Point2d GetEndPnt() {
        Node nd = GetEndNode();
        return nd.GetPoint2dObject();
    }

//
//GetNeighborNode: Get (the ID of) a neighbor node of the specified node.
//
//public short GetNeighborNode(short uNode)
//{
//	for (short i = 0; i < m_uCount; i++)
//	{
//	Path* pPath = m_pPathIdx[i].m_ptr;
//
//	short uStartNode = pPath->m_uStartNode;
//	short uEndNode = pPath->m_uEndNode;
//
//	if (uNode == uStartNode)
//	return uEndNode;
//	
//	if (uNode == uEndNode)
//	return uStartNode;
//	}
//	
//	return NULLID;
//}


//
//Create the common data of a path (except "m_uType") from a text file.
//
//public boolean Create(FILE *StreamIn)
//{
//	int uId, uStartNode, uEndNode, uGuideType, uExtType;
//
//	if (fscanf(StreamIn, "%u,\t%u,\t%u,\t%f,\t%u",
//	&uId,
//	&uStartNode,
//	&uEndNode,
//	&m_fVeloLimit,
//	&uGuideType) == EOF)
//	return false;
//	m_uId = uId;
//	m_uStartNode = uStartNode;
//	m_uEndNode = uEndNode;
//	m_uGuideType = uGuideType;
//
//	#ifdef WORLD_FORMAT_VER2_0
//	if (fscanf(StreamIn, ",\t%u,\t%f", &uExtType, &m_fNavParam) == EOF)
//	return false;
//	m_uExtType = uExtType;
//	#endif
//
//	// Success, return true
//	return true;
//}

//
//Save: Save the common part of the path data to a text file.
//
//boolean Save(FILE *StreamOut)
//{
//	if (fprintf(StreamOut, "%u,\t%u,\t%u,\t%f,\t%u",
//	m_uId,
//	m_uStartNode,
//	m_uEndNode,
//	m_fVeloLimit,
//	m_uGuideType) == EOF)
//	return false;
//
//	#ifdef WORLD_FORMAT_VER2_0
//	if (fprintf(StreamOut, ",\t%u,\t%f", m_uExtType, m_fNavParam) == EOF)
//	return false;
//	#endif
//
//	// Success, return true
//	return true;
//}

    public boolean Create(DataInputStream dis) {
        short uGuideType;
        float fDummy;

        // Read the path data
        try {
            // ����ʱ��Ӧ����д���˳��
            int ch1 = dis.read();
            int ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            this.m_uId = ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            this.m_uStartNode = ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            this.m_uEndNode = ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            int ch3 = dis.read();
            int ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0)
                throw new EOFException();
            int tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.m_fVeloLimit[0] = Float.intBitsToFloat(tempI);

//		#ifdef WORLD_FORMAT_VER3_0
            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0)
                throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.m_fVeloLimit[1] = Float.intBitsToFloat(tempI);
//		#endif
            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            this.m_uGuideType = (short) ((ch2 << 8) + (ch1 << 0));
//		m_uGuideType = uGuideType;

//		#ifdef WORLD_FORMAT_VER2_0
            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            this.m_uExtType = (short) ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0)
                throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.m_fNavParam = Float.intBitsToFloat(tempI);

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            m_LayerID = (short) ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            m_uMoveHeading = (short) ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            m_uCarrierType = (short) ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            m_uOnLine = (short) ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0)
                throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            m_clr = tempI;

//		#endif
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Success, return true
        return true;
    }

    public boolean Save(DataOutputStream dis) {
        short uGuideType;
        float fDummy;

        // Read the path data
        try {
            TranBytes tan = new TranBytes();

            int ch1 = this.m_uId;
            int ch2 = this.m_uId;
            tan.writeShort(dis, this.m_uId);

            ch1 = this.m_uStartNode;
            ch2 = this.m_uStartNode;
            tan.writeShort(dis, this.m_uStartNode);

            ch1 = this.m_uEndNode;
            ch2 = this.m_uEndNode;
            tan.writeShort(dis, this.m_uEndNode);

            int Ix = Float.floatToIntBits(this.m_fVeloLimit[0]);
            tan.writeInteger(dis, Ix);

            Ix = Float.floatToIntBits(this.m_fVeloLimit[1]);
            tan.writeInteger(dis, Ix);

            ch1 = this.m_uGuideType;
            ch2 = this.m_uGuideType;
            tan.writeShort(dis, this.m_uGuideType);

            ch1 = this.m_uExtType;
            ch2 = this.m_uExtType;
            tan.writeShort(dis, this.m_uExtType);

            Ix = Float.floatToIntBits(this.m_fNavParam);
            tan.writeInteger(dis, Ix);

            dis.writeShort(tan.TranShort(m_LayerID));
            dis.writeShort(tan.TranShort(m_uMoveHeading));
            dis.writeShort(tan.TranShort(m_uCarrierType));
            dis.writeShort(tan.TranShort(m_uOnLine));

            dis.writeInt(tan.tranInteger(m_clr));
            //dis.flush();
//		#endif
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Success, return true
        return true;
    }

//public boolean Create(CArchive& ar)
//{
//	short uGuideType;
//	float fDummy;
//	
//	// Read the path data
//	ar >> m_uId
//	>> m_uStartNode
//	>> m_uEndNode
//	>> m_fVeloLimit[0]
//	#ifdef WORLD_FORMAT_VER3_0
//	>> m_fVeloLimit[1]
//	#endif
//	>> uGuideType;        
//	m_uGuideType = uGuideType;
//	
//	#ifdef WORLD_FORMAT_VER2_0
//	ar >> m_uExtType;
//	ar >> m_fNavParam;
//	#endif
//	
//	// Success, return true
//	return true;
//}

//boolean Save(CArchive& ar)
//{
//	//2019.11.8
//	m_uGuideType = m_uGuideType&0xFF;
//	// Save the path data
//	ar << m_uId
//	<< m_uStartNode
//	<< m_uEndNode
//	<< m_fVeloLimit[0]
//	#ifdef WORLD_FORMAT_VER3_0
//	<< m_fVeloLimit[1]
//	#endif
//	<< m_uGuideType;
//	
//	#ifdef WORLD_FORMAT_VER2_0
//	ar << m_uExtType;
//	ar << m_fNavParam;
//	#endif
//	
//	// Success, return true
//	return true;
//}

}
