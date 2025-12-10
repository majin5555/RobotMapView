package com.siasun.dianshi.bean.world;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;

import com.siasun.dianshi.bean.Point2d;
import com.siasun.dianshi.bean.TranBytes;
import com.siasun.dianshi.utils.CoordinateConversion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;


public class Node extends Point2d {
    //   "Node"��Ķ���.

    public int m_uId;             // 节点ID
    public short m_uType;           // �ڵ�������
    public short m_uExtType;        // �ڵ����չ������
    public short m_uExtType2;        // �ڵ����չ������
    public float m_fHeading;        // �ڵ㴦��ͷ����
    public RfId m_Tag;             // ���ӱ�ǩ��Ϣ
    public float m_fChkMarkDist;    // ���ҵر����
    public float m_fChkMarkVel;     // �ҵر��ٶ�
    public float m_fMarkWidth;      // �ر���Ч���

    public float m_fOffset1;
    public float m_fOffset2;

    public float m_fFwdMarkOffset;
    public float m_fBwdMarkOffset;

    public short m_uLayerID;
    public short m_uStationType;  //站类型 临时0 stop1 charg2 act3
    public short m_uStationTempId;
    public short m_uStationId;

    public short m_uCarrierType = 255;  //车辆类型，默认：255
    public short m_uOnLine = 1;         //允许上线，1：允许上线，0：不允许；默认：1

    public Color m_clr;    //��ͼ��ɫ 1119
    public Color m_oldclr;    //��ѡ���߶�ʱ����Ҫ����ԭʼ��ɫ 1119

    private final int offset = 20;


//	public:
    // The constructor
    //Node(short uId, Point2d& pt, short uType = MARK_NODE,	short uExtType = 0,
    //	float fHeading = 0.0f, UCHAR* pTag = NULL);

//		public	Node(short uId, Point2d pt, short uType = MARK_NODE,	short uExtType = 0,	
//			float fHeading = 0.0f, UCHAR* pTag = NULL, float ChkMarkDist = 0.45f, float ChkMarkVel = 0.1f,
//			float MarkWidth = 0.0f, float FwdMarkOffset = 0.0f, float BwdMarkOffset = 0.0f, float Offset1 = 0.0f,
//			float Offset2 = 0.0f);

    // The default constructor
    public Node() {
        m_uId = 0;
        m_uLayerID = 0;
        m_uType = 1;
        m_uExtType = 0;
        m_fHeading = 0.0f;
        m_uExtType2 = 0;
        m_Tag = new RfId();

        m_fChkMarkDist = 0.45f;
        m_fChkMarkVel = 0.1f;
        m_fMarkWidth = 0.0f;

        m_fFwdMarkOffset = 0.0f;
        m_fBwdMarkOffset = 0.0f;

        m_fOffset1 = 0.0f;
        m_fOffset2 = 0.0f;

        //SetColor(RGB(255,255,255)); //1119
    }

//		// Check if a mark is available at this node
//		boolean IsMarkNode();
//
//		// Check if a temporary mark is available at this node
//		short IsTempMarkNode();
//
//		// Check if this node is a conveyor station
//		boolean IsConveyorNode();
//
//		// Check if this node is a spin turn node
//		boolean IsSpinTurnNode();
//
//		// Check if this node is a charger station
//		boolean IsChargerNode();
//
//		//  Check if a RFID mark is available at this node
//		boolean IsRFIDMarkNode();
//
//		boolean IsComNode();

    // Get the tag ID of the node
//		CRfId GetTag() {return m_Tag;}

    // Set the tag ID for the node
//		public void SetTag(CRfId& Tag) {m_Tag = Tag;}
//
//		// Verify tag ID
//		boolean VerifyTag(CRfId& Tag) {return m_Tag == Tag;}
//
//	   // overloaded operator "="
//	   public void operator = (const Node& nd);
//
//	   // overloaded operator "=="
//		boolean operator ==(const Node& nd) const;
//
//		// overloaded operator "!="
//		boolean operator !=(const Node& nd) const;
//
//		// overloaded operator "=="
//		boolean operator == (short uNodeId);
//
//		// overloaded operator "!="
//		boolean operator != (short uNodeId);
//
//		// Create the node data from a text file
//		boolean Create(FILE *StreamIn);
//
//		// Save the node data to a text file
//		boolean Save(FILE *StreamOut);
//
//		public void MoveCircle(Point2d ptCenter, float fAngle); //��ת
//		public void Move(Point2d ptMove);                       //�ƶ�
//
//		public void SetColor(Color clr) { m_clr = clr; }		//1119
//		Color GetColor(){return m_clr;}					//1119
//		public void SetOldColor(Color clr){m_oldclr = clr;}		//1119
//		Color GetOldColor(){return m_oldclr;}			//1119
//
//	#ifdef _MFC_VER
//		// Archive I/O routine


    public Node(int uId, Point2d pt) {
        x = pt.x;
        y = pt.y;
        m_uId = uId;
        m_uType = 1;
        m_uExtType = 0;
        m_uExtType2 = 0;
        m_fHeading = 0.0f;
        m_Tag = new RfId();
        m_Tag.Init(null);
    }

    public Node(int uId, Point2d pt, Short uLayerID) {
//		(Point2d)this = Point2d(pt);

        x = pt.x;
        y = pt.y;
        m_uId = uId;
        m_uLayerID = uLayerID;
        m_uType = 1;
        m_uExtType = 0;
        m_uExtType2 = 0;
        m_fHeading = 0.0f;
        m_Tag = new RfId();
        m_Tag.Init(null);
//		m_uType = uType;
//		m_uExtType = uExtType;
//		m_fHeading = fHeading;
        //	m_Tag.Init(pTag);
    }

    //
    //   IsMarkNode: Check if a mark is available at this node.
    //
//    boolean IsMarkNode() {
//        return ((m_uType & MARK_NODE) != 0);
//    }

    //
    //   Check if a temporary mark is available at this node.
    //   Return:
//		     0 - No temporary mark
//		     1 - Temporary mark #1
//		     2 - Temporary mark #2
    //
//	short IsTempMarkNode()
//	{
//		short uMask = (short)(BIT(4)|BIT(5));
//		return ((m_uType & uMask) >> 4); 
//	}
//
//	//
//	//   IsConveyorNode: Check whether the node is a conveyor node.
//	//
//	boolean IsConveyorNode()
//	{
//		return ((m_uType & CONVEYOR_NODE) != 0);
//	}
//
//	//
//	//   IsSpinTurnNode: Check if this is a spin-turn node.
//	//
//	boolean IsSpinTurnNode()
//	{
//		return ((m_uType & SPINTURN_NODE) != 0);
//	}
//
//	//
//	//   IsChargerNode: Check if this node is a charger station.
//	//
//	boolean IsChargerNode()
//	{
//		return ((m_uType & CHARGER_NODE) != 0);
//	}
//
//	//
//	//   overloaded operator "=".
//	//
//	public void operator = (const Node& nd)
//	{
//		m_uId = nd.m_uId;
//		m_uType = nd.m_uType;
//		m_uExtType = nd.m_uExtType;
//		m_fHeading = nd.m_fHeading;
//		m_Tag = nd.m_Tag;
//		GetPoint2dObject() = nd;
//	}
//
//	//
//	//   IsRFIDMarkNode: Check if a RFID mark is available at this node.
//	//
//	boolean IsRFIDMarkNode()
//	{
//		return ((m_uType & RFID_MARK_NODE) != 0);
//	}
//
//	boolean IsComNode()
//	{
//		return ((m_uType & COM_NODE) != 0);
//	}
//
//	//
//	//   operator "==": Check if 2 nodes are identical.
//	//
//	boolean operator ==(const Node& nd) const
//	{
//		// If their ID are equal, they are identical
//		return (m_uId == nd.m_uId);
//	}
//
//	//
//	//   operator "!=": Check if 2 nodes are not identical.
//	//
//	boolean operator != (const Node& nd) const
//	{
//		// If their ID are different, they are not identical
//		return (m_uId != nd.m_uId);
//	}
//
//	//
//	//   operator "==": Check if 2 nodes are identical.
//	//
//	boolean operator ==(short uNodeId)
//	{
//		return (m_uId == uNodeId);
//	}
//
//	//
//	//   operator "!=": Check if 2 nodes are not identical.
//	//
//	boolean operator !=(short uNodeId)
//	{
//		return (m_uId != uNodeId);
//	}
//
//	//
////		    Create the node data from a text file.
//	//
//	boolean Create(FILE *StreamIn)
//	{
//		if (fscanf(StreamIn, "%u,\t%2u", &m_uId, &m_uType) == EOF)
//			return false;
//
//		// �������һ������λ����Ϣ�ڵ㡱,���ٶ�ȡ�����ֶ�����
//		if (m_uType & PNT_UNKNOWN_NODE)
//		{
//			x = y = 0;
//			m_uExtType = 0;
//			m_fHeading = 0;
//			fscanf(StreamIn, "\n");
//			return true;
//		}
//
//		else if (fscanf(StreamIn, ",\t%f,\t%f,\t%u,\t%f\t",
//							&x,
//							&y
//
//	#ifdef WORLD_FORMAT_VER2_0
//							,&m_uExtType
//							,&m_fHeading
//	#endif
//
//							) == EOF)
//	      return false;
//
//
//	#ifdef WORLD_FORMAT_VER2_1
//	   return m_Tag.Create(StreamIn);
//	#endif
//	}
//
////	//
//////		    Save: Save node data to a text file.
////	//
////	boolean Save(FILE *StreamOut)
////	{
////		if (fprintf(StreamOut, "%u,\t%2u", m_uId, m_uType) == EOF)
////			return false;
////
////		if (m_uType & PNT_UNKNOWN_NODE)
////		{
////			fprintf(StreamOut, "\n");
////			return true;
////		}
////		else if (fprintf(StreamOut, ",\t%f,\t%f,\t%u,\t%f\t",
////							 x,
////							 y
////
////	#ifdef WORLD_FORMAT_VER2_0
////							 ,m_uExtType
////							 ,m_fHeading
////	#endif
////
////							 ) == EOF)
////	      return false;
////
////	#ifdef WORLD_FORMAT_VER2_1
////	   return m_Tag.Save(StreamOut);
////	#endif
////	}
//

    public void Create(DataInputStream dis) {
        try {
            // ����ʱ��Ӧ����д���˳��
            int ch1 = dis.read();
            int ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uId = ((ch2 << 8) + (ch1 << 0));
            //   this.m_uId =  (short)((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uType = (short) ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            int ch3 = dis.read();
            int ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            int tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.x = Float.intBitsToFloat(tempI);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.y = Float.intBitsToFloat(tempI);

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uExtType = (short) ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.m_fHeading = Float.intBitsToFloat(tempI);

            this.m_Tag.Create(dis);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.m_fChkMarkDist = Float.intBitsToFloat(tempI);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.m_fChkMarkVel = Float.intBitsToFloat(tempI);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.m_fMarkWidth = Float.intBitsToFloat(tempI);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.m_fOffset1 = Float.intBitsToFloat(tempI);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.m_fOffset2 = Float.intBitsToFloat(tempI);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.m_fFwdMarkOffset = Float.intBitsToFloat(tempI);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.m_fBwdMarkOffset = Float.intBitsToFloat(tempI);

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uLayerID = (short) ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uStationType = (short) ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uStationTempId = (short) ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uStationId = (short) ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            m_uCarrierType = (short) ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            m_uOnLine = (short) ((ch2 << 8) + (ch1 << 0));


            //     dis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void Save(DataOutputStream dis) {
        try {
            TranBytes tan = new TranBytes();
            int ch1 = this.m_uId;
            int ch2 = this.m_uId;
            tan.writeShort(dis, this.m_uId);

            ch1 = this.m_uType;
            ch2 = this.m_uType;
            tan.writeShort(dis, this.m_uType);


            int Ix = Float.floatToIntBits(this.x);
            tan.writeInteger(dis, Ix);

            int Iy = Float.floatToIntBits(this.y);
            tan.writeInteger(dis, Iy);

            ch1 = this.m_uExtType;
            ch2 = this.m_uExtType;
            tan.writeShort(dis, this.m_uExtType);

            Iy = Float.floatToIntBits(this.m_fHeading);
            tan.writeInteger(dis, Iy);
            //	dis.writeFloat(this.m_fHeading);
            m_Tag.Save(dis);

            Iy = Float.floatToIntBits(this.m_fChkMarkDist);
            tan.writeInteger(dis, Iy);

            Iy = Float.floatToIntBits(this.m_fChkMarkVel);
            tan.writeInteger(dis, Iy);

            Iy = Float.floatToIntBits(this.m_fMarkWidth);
            tan.writeInteger(dis, Iy);

            Iy = Float.floatToIntBits(this.m_fOffset1);
            tan.writeInteger(dis, Iy);

            Iy = Float.floatToIntBits(this.m_fOffset2);
            tan.writeInteger(dis, Iy);

            Iy = Float.floatToIntBits(this.m_fFwdMarkOffset);
            tan.writeInteger(dis, Iy);

            Iy = Float.floatToIntBits(this.m_fBwdMarkOffset);
            tan.writeInteger(dis, Iy);

            //	TranBytes tan = new TranBytes();
            dis.writeShort(tan.TranShort(m_uLayerID));
            dis.writeShort(tan.TranShort(m_uStationType));
            dis.writeShort(tan.TranShort(m_uStationTempId));
            dis.writeShort(tan.TranShort(m_uStationId));

            dis.writeShort(tan.TranShort(m_uCarrierType));
            dis.writeShort(tan.TranShort(m_uOnLine));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean PointHitTest(Point point, CoordinateConversion ScrnRef) {
        Point2d ptNode = GetPoint2dObject();
        PointF pntTemp = ScrnRef.worldToScreen(ptNode.x, ptNode.y);
        PointF p11 = new PointF();
        PointF p12 = new PointF();
        p11.x = (pntTemp.x - offset);
        p11.y = (pntTemp.y - offset);
        p12.x = (pntTemp.x + offset);
        p12.y = (pntTemp.y + offset);
        if (point.x >= p11.x && point.x <= p12.x && point.y >= p11.y && point.y <= p12.y) {
            return true;
        }
        return false;
    }

    public void Draw(CoordinateConversion ScrnRef, Canvas Grp, int color) {
        try {
            PointF pnt1 = ScrnRef.worldToScreen(GetPoint2dObject().x, GetPoint2dObject().y);
            int width = Grp.getWidth();
            int Height = Grp.getHeight();
            if ((pnt1.x < 0 || pnt1.x > width) && (pnt1.y < 0 || pnt1.y > Height)) {
                return;
            }
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
            //	RectF ret = new RectF(pnt1.x-2, pnt1.y-2, 4, 4);
            RectF ret = new RectF(pnt1.x - 5, pnt1.y - 5, pnt1.x + 5, pnt1.y + 5);
            Grp.drawOval(ret, paint);

            if (m_uType == 4) {
                paint.setColor(Color.RED);
                Grp.drawOval(ret, paint);
            }
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }
}
