package com.siasun.dianshi.bean.world;

import static java.lang.Math.abs;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;

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
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;


public class GenericPath extends Path {
    protected Angle m_angStartHeading;     // ��������ʼ�ڵ㴦�ķ����
    protected Angle m_angEndHeading;       // ��������ֹ�ڵ㴦�ķ����
    protected int m_nCountCtrlPoints;   // 控制点个数--2
    protected Point2d[] m_pptCtrl;            // ָ�������и������Ƶ��ָ��(���������˽ڵ�)

    protected Posture m_pstStart;            // ��ʼ��̬
    protected Posture m_pstEnd;              // ��ֹ��̬
    protected boolean m_bTangency;           //���з�ʽΪ���л���ƽ��, �˶���ʽ 0:���� 1:ƽ��
    protected short m_uRunFunction;         //���з�ʽΪ���л���ƽ��, �˶���ʽ 0:ƽ�� 1:���� 2: ��ת

    public Bezier m_Curve;               // ���߶���
    // ��������ױ���������
    public float m_fVelMax;
    public float m_fThitaDiffMax;
    public float m_fAngVelMax;
    public float m_fAngVelACC;
    public float m_fVelACC;
    public float m_fLenForAngJump;
    public float m_fThitaDiffMaxForStAndEd;
    TranBytes m_TranFloat = new TranBytes();

    private final int pathOffset = 20;
    private final int nodeOffset = 20;



    public static float BEZIER_K = 0.95f;
    private float m_fTurnVel1 = 1.0f; //入弯速度
    private float m_fTurnVel2 = 1.0f; //出弯速度
    private int nDriveUnitCount = 1; //驱动单元数量
    private float drive_unit_x = 0.f;
    private float drive_unit_y = 0.f;
    private float fVelMax = 1.f; //轮最大速度
    private float fVelACC = 0.2f;
    private float fThitaDiffMax = 1.f;
    private float fAngVelACC = 1.f;
    private float fSteerAngle = 80.f;
    private float[] fUserData = new float[10];
    private int UnitType = 5; //驱动单元类型 5-双轮差动

    public boolean m_bCurvature; //曲线曲率是否满足
    private static String TAG = GenericPath.class.getSimpleName();


//////////////////////////////////////////////////////////////////////////////
//Implementation of class "GenericPath".


    public GenericPath(int uId, int nStartNode, int nEndNode, Posture pstStart, Posture pstEnd, float fLen1, float fLen2,
                       float[] fVeloLimit, short nGuideType, short uObstacle, short uDir, short uExtType, NodeBase nodeBase) {
        short type = 10;
        m_fVelMax = 0.7f;
        m_fThitaDiffMax = 0.6f;
        m_fAngVelMax = 0.3f;
        m_fAngVelACC = 0.1f;
        m_fVelACC = 0.5f;
        m_fLenForAngJump = 0.1f;
        m_fThitaDiffMaxForStAndEd = 0.6f;
        m_bCurvature = true;

        super.Create(uId, nStartNode, nEndNode, fVeloLimit, type, nGuideType, 0, uExtType, nodeBase);
        Create(pstStart, pstEnd, fLen1, fLen2);
    }

    public GenericPath(int uId, int nStartNode, int nEndNode, Posture pstStart, Posture pstEnd, Point2d[] pptCtrl /*float fLen1, float fLen2*/,
                       float[] fVeloLimit, short nGuideType, short uObstacle, short uDir, short uExtType, NodeBase nodeBase, short pathParam) {
        short type = 10;
        m_fVelMax = 0.7f;
        m_fThitaDiffMax = 0.6f;
        m_fAngVelMax = 0.3f;
        m_fAngVelACC = 0.1f;
        m_fVelACC = 0.5f;
        m_fLenForAngJump = 0.1f;
        m_fThitaDiffMaxForStAndEd = 0.6f;
        m_bCurvature = true;

        super.CreatePP(uId, nStartNode, nEndNode, fVeloLimit, type, nGuideType, 0, uExtType, nodeBase, pathParam);
//        Create(pstStart, pstEnd, fLen1, fLen2);
        Create(pstStart, pstEnd, 2, pptCtrl);
    }


    public GenericPath() {
        m_pstStart = new Posture();
        m_pstEnd = new Posture();
        m_nCountCtrlPoints = 0;
        m_pptCtrl = null;

        m_fVelMax = 0.7f;
        m_fThitaDiffMax = 0.6f;
        m_fAngVelMax = 0.3f;
        m_fAngVelACC = 0.1f;
        m_fVelACC = 0.5f;
        m_fLenForAngJump = 0.1f;
        m_fThitaDiffMaxForStAndEd = 0.6f;
        m_bCurvature = true;

    }

    public boolean GetTangency() {
        return m_bTangency;
    }


    @Override
    int PointHitTest(Point pnt, CoordinateConversion ScrnRef) {
        // 判断屏幕点是否落在某个关键点处
        for (int i = 1; i < m_Curve.m_nCountKeyPoints - 1; i++) {
//            Point pntKey = ScrnRef.GetWindowPoint(m_Curve.m_ptKey[i]);
            PointF pntKey = ScrnRef.worldToScreen(m_Curve.m_ptKey[i].x, m_Curve.m_ptKey[i].y);
            int offSet = 20;
            Rect r = new Rect((int) (pntKey.x - nodeOffset)
                    , (int) (pntKey.y - nodeOffset)
                    , (int) (pntKey.x + nodeOffset)
                    , (int) (pntKey.y + nodeOffset)); // Construct an emtpy rectangle

            if (pnt.x >= r.left && pnt.x <= (r.left + r.width()) &&
                    pnt.y < r.bottom && pnt.y >= (r.bottom - r.height()))
                return i + 1;
        }

        // 下面判断屏幕点是否落在曲线上
        Point2d ptClosest = new Point2d();
//        Point2d pt = ScrnRef.GetWorldPoint(pnt);
        PointF pt = ScrnRef.screenToWorld((float) pnt.x, (float) pnt.y);
        float CurT = 0.0f;
        // 计算此点在世界坐标系内到曲线的距离
        if (m_Curve.GetClosestPoint(new Point2d(pt.x, pt.y), ptClosest, CurT)) {
            // 计算点到曲线的最近距离
//            float fDist = pt.DistanceTo(ptClosest);

            float fDist = (float) Math.hypot(pt.x - ptClosest.x, pt.y - ptClosest.y);

            // 换算到屏幕窗口距离
            int nDist = (int) (fDist * ScrnRef.scale);
            //2019.10.24
            nDist = abs(nDist);
            ///////////////////
            // 如果屏幕窗口距离小于3，认为鼠标触碰到路径
            if (nDist <= pathOffset)
                return 0;
        }

        return -1;
    }


    private boolean Create(Posture pstStart, Posture pstEnd, int nCountCtrlPoints, Point2d[] pptCtrl) {
        m_pstStart = pstStart;
        m_pstEnd = pstEnd;
        m_nCountCtrlPoints = nCountCtrlPoints;

        // Ϊ�ؼ������ռ�
        m_pptCtrl = new Point2d[m_nCountCtrlPoints];
        if (m_pptCtrl == null)
            return false;

        // ���ƿ��Ƶ�����
        for (int i = 0; i < m_nCountCtrlPoints; i++)
            m_pptCtrl[i] = pptCtrl[i];

        // ��ʼ��·��
        return Init();
    }


    private boolean Create(Posture pstStart, Posture pstEnd, float fLen1, float fLen2) {
        m_pstStart = pstStart;
        m_pstEnd = pstEnd;
        m_nCountCtrlPoints = 2;

        // Ϊ�ؼ������ռ�
        m_pptCtrl = new Point2d[m_nCountCtrlPoints];
        if (m_pptCtrl == null)
            return false;

        Line ln1 = new Line(pstStart, fLen1);
        m_pptCtrl[0] = ln1.GetEndPoint();

        Angle ang = new Angle(0);
        ang.m_fRad = pstEnd.GetAngle().m_fRad + Line.PI;
        //Line ln2 = new Line(pstEnd, !pstEnd.GetAngle(), fLen2);
        Line ln2 = new Line(pstEnd, ang, fLen2);
        m_pptCtrl[1] = ln2.GetEndPoint();

        Init();

        return true;
    }


//m_uBwdRotoScannerObstacle = ArcPath.m_uBwdRotoScannerObstacle;
//m_uBwdObdetectorObstacle = ArcPath.m_uBwdObdetectorObstacle;
//}

    //
//�������ṩ�Ĺؼ���������λ�ó�ʼ����·����
//
    protected boolean Init() {
        // ��ʱΪ���йؼ������ռ䣬�Ա��������߶���
        Point2d[] pptKey = new Point2d[m_nCountCtrlPoints + 2];
        if (pptKey == null)
            return false;

        // ��һ�������һ���ؼ���ʵ������·���Ķ˽ڵ�
        pptKey[0] = new Point2d();
        pptKey[0].x = m_pstStart.x;
        pptKey[0].y = m_pstStart.y;
        pptKey[m_nCountCtrlPoints + 1] = new Point2d();
        pptKey[m_nCountCtrlPoints + 1].x = m_pstEnd.x;
        pptKey[m_nCountCtrlPoints + 1].y = m_pstEnd.y;

        // ���Ƴ���ʼ�ڵ㡢��ֹ�ڵ�֮��Ĺؼ���
        for (int i = 0; i < m_nCountCtrlPoints; i++) {
            if (pptKey[i + 1] == null) {
                pptKey[i + 1] = new Point2d();
            }
            pptKey[i + 1].x = m_pptCtrl[i].x;
            pptKey[i + 1].y = m_pptCtrl[i].y;
        }

        // �������߶���
        m_Curve = new Bezier();

//        for (int i = 0; i < pptKey.length; i++) {
//            Log.d("scj", "GenericPath  Init: m_Bezier.m_ptKey" + "[" + i + "]  = " + pptKey[i]);
//        }
        m_Curve.Create(m_nCountCtrlPoints + 2, pptKey);

        m_bCurvature = m_Curve.m_bCurvature;
//        Log.d(TAG, "Init: m_bCurvature = " + m_bCurvature);

        // �ͷ���ʱ�ռ�
//	delete []pptKey;
        pptKey = null;

        // ��ʼ����㡢�յ㴦�ķ����
        m_angStartHeading = m_pstStart.GetAngle();
        m_angEndHeading = m_pstEnd.GetAngle();
        m_fSize = m_Curve.m_fTotalLen;
        return true;
    }

    //
    //GetHeading: Get the vehicle's heading angle at the specified node.
    //
    @Override
    public Angle GetHeading(Node nd) {
        if (nd.m_uId == m_uStartNode)
            return m_angStartHeading;
        else
            return m_angEndHeading;
    }

    //
//Make a trajectory from the path.
//
//CTraj* MakeTraj()
//{
//#if 0
//CBezierTraj* pBezierTraj = new CBezierTraj;
//
//Point2d& ptStart = GetStartPnt();
//Point2d& ptEnd = GetEndPnt();
//pBezierTraj->CreateTraj(ptStart, ptEnd, m_CtrlPnt[1], ptEnd, FORWARD, m_TurnDir, m_bTangency, m_angShiftHeading);
//return pBezierTraj;
//#endif
//return null;
//}
//
//boolean Create(FILE *StreamIn)
//{
//return true;
//}
//
//boolean Save(FILE *StreamOut)
//{
//return true;
//}
    public boolean Create(DataInputStream dis) {
        short uDir;            // Positive input/negative input
        short uTemp;
        Point2d[] CtrlPnt = new Point2d[2];
        CtrlPnt[0] = new Point2d();
        CtrlPnt[1] = new Point2d();
        float angShiftHeading;
        boolean bTangency = false;

        //读取path基本数据类型
        if (!super.Create(dis))
            return false;
        try {
            //ar >> m_uFwdRotoScannerObstacle >> m_uFwdObdetectorObstacle >> m_uBwdRotoScannerObstacle >> m_uBwdObdetectorObstacle >> uDir;
            int ch1 = dis.read();
            int ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            this.m_uFwdRotoScannerObstacle = ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            this.m_uFwdObdetectorObstacle = ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            this.m_uBwdRotoScannerObstacle = ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            this.m_uBwdObdetectorObstacle = ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            uDir = (short) ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            int ch3 = dis.read();
            int ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0)
                throw new EOFException();
            int tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            angShiftHeading = Float.intBitsToFloat(tempI);

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            this.m_uRunFunction = (short) ((ch2 << 8) + (ch1 << 0));


            m_uPathHeading = uDir;
//	#if 1
            // ����ؼ�������
            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0)
                throw new EOFException();
            this.m_nCountCtrlPoints = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
//	#else
//		m_nCountCtrlPoints = 2;
//	#endif

            if (m_pptCtrl != null)
                m_pptCtrl = null;
//			delete []m_pptCtrl;

            m_pptCtrl = new Point2d[m_nCountCtrlPoints];
            for (int i = 0; i < m_nCountCtrlPoints; i++) {
                m_pptCtrl[i] = new Point2d();
            }

            for (int i = 0; i < m_nCountCtrlPoints; i++) {
                ch1 = dis.read();
                ch2 = dis.read();
                ch3 = dis.read();
                ch4 = dis.read();
                if ((ch1 | ch2 | ch3 | ch4) < 0)
                    throw new EOFException();
                tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
                m_pptCtrl[i].x = Float.intBitsToFloat(tempI);

                ch1 = dis.read();
                ch2 = dis.read();
                ch3 = dis.read();
                ch4 = dis.read();
                if ((ch1 | ch2 | ch3 | ch4) < 0)
                    throw new EOFException();
                tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
                m_pptCtrl[i].y = Float.intBitsToFloat(tempI);
            }

            //相切平移
            int ch = dis.read();
            if (ch < 0) {
                throw new EOFException();
            }


            //路段端点的打舵距离
            m_fLenForAngJump = WorldFileIO.readFloat(dis);

            //入弯速度
            m_fTurnVel1 = WorldFileIO.readFloat(dis);

            //出弯速度
            m_fTurnVel2 = WorldFileIO.readFloat(dis);

            //驱动单元数量
            nDriveUnitCount = WorldFileIO.readInt(dis);

            for (int i = 0; i < nDriveUnitCount; i++) {
                //驱动单元类型
                UnitType = WorldFileIO.readInt(dis);

                //驱动单元安装位置X
                drive_unit_x = WorldFileIO.readFloat(dis);

                //驱动单元安装位置Y
                drive_unit_y = WorldFileIO.readFloat(dis);

                //轮最大速度
                fVelMax = WorldFileIO.readFloat(dis);

                //轮最大加速度
                fVelACC = WorldFileIO.readFloat(dis);

                //舵最大速度
                fThitaDiffMax = WorldFileIO.readFloat(dis);

                //舵最大加速度
                fAngVelACC = WorldFileIO.readFloat(dis);

                //最大打舵角度
                fSteerAngle = WorldFileIO.readFloat(dis);

                //自定义数据
                for (int j = 0; j < 10; j++) {
                    fUserData[j] = WorldFileIO.readFloat(dis);
                }
            }

            /**  老版地图数据注释
             //20200528添加
             ch1 = dis.read();
             ch2 = dis.read();
             ch3 = dis.read();
             ch4 = dis.read();
             if ((ch1 | ch2 | ch3 | ch4) < 0)
             throw new EOFException();
             tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
             m_fVelMax = Float.intBitsToFloat(tempI);

             ch1 = dis.read();
             ch2 = dis.read();
             ch3 = dis.read();
             ch4 = dis.read();
             if ((ch1 | ch2 | ch3 | ch4) < 0)
             throw new EOFException();
             tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
             m_fThitaDiffMax = Float.intBitsToFloat(tempI);

             ch1 = dis.read();
             ch2 = dis.read();
             ch3 = dis.read();
             ch4 = dis.read();
             if ((ch1 | ch2 | ch3 | ch4) < 0)
             throw new EOFException();
             tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
             m_fAngVelMax = Float.intBitsToFloat(tempI);

             ch1 = dis.read();
             ch2 = dis.read();
             ch3 = dis.read();
             ch4 = dis.read();
             if ((ch1 | ch2 | ch3 | ch4) < 0)
             throw new EOFException();
             tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
             m_fAngVelACC = Float.intBitsToFloat(tempI);

             ch1 = dis.read();
             ch2 = dis.read();
             ch3 = dis.read();
             ch4 = dis.read();
             if ((ch1 | ch2 | ch3 | ch4) < 0)
             throw new EOFException();
             tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
             m_fVelACC = Float.intBitsToFloat(tempI);

             ch1 = dis.read();
             ch2 = dis.read();
             ch3 = dis.read();
             ch4 = dis.read();
             if ((ch1 | ch2 | ch3 | ch4) < 0)
             throw new EOFException();
             tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
             m_fThitaDiffMaxForStAndEd = Float.intBitsToFloat(tempI);
             */
            bTangency = (ch != 0);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        m_bTangency = !bTangency; //2019.11.19
        Point2d ptStart = new Point2d();
        Point2d ptEnd = new Point2d();
        ptStart.x = GetStartNode().GetPoint2dObject().x;
        ptStart.y = GetStartNode().GetPoint2dObject().y;
        ptEnd.x = GetEndNode().GetPoint2dObject().x;
        ptEnd.y = GetEndNode().GetPoint2dObject().y;

        m_pstStart.x = ptStart.x;
        m_pstStart.y = ptStart.y;
        m_pstEnd.x = ptEnd.x;
        m_pstEnd.y = ptEnd.y;

        Line ln1 = new Line(ptStart, m_pptCtrl[0]);
        Line ln2 = new Line(m_pptCtrl[m_nCountCtrlPoints - 1], ptEnd);

        m_pstStart.fThita = ln1.GetSlantAngle().m_fRad;
        m_pstEnd.fThita = ln2.GetSlantAngle().m_fRad;

        return Init();
    }

    public boolean Save(DataOutputStream dos) {
        short uDir;            // Positive input/negative input
        short uTemp;
        Point2d[] CtrlPnt = new Point2d[2];
        CtrlPnt[0] = new Point2d();
        CtrlPnt[1] = new Point2d();
        float angShiftHeading = 0.0f;
        boolean bTangency = false;

        if (!super.Save(dos))
            return false;
        try {
            TranBytes tan = new TranBytes();
            int ch1;
            int ch2;
            tan.writeShort(dos, this.m_uFwdRotoScannerObstacle);
            tan.writeShort(dos, this.m_uFwdObdetectorObstacle);
            tan.writeShort(dos, this.m_uBwdRotoScannerObstacle);
            tan.writeShort(dos, this.m_uBwdObdetectorObstacle);

            ch1 = this.m_uPathHeading;
            ch2 = this.m_uPathHeading;
            tan.writeShort(dos, this.m_uPathHeading);

            int Ix = Float.floatToIntBits(angShiftHeading);
            tan.writeInteger(dos, Ix);

            ch1 = this.m_uRunFunction;
            ch2 = this.m_uRunFunction;
            tan.writeShort(dos, this.m_uRunFunction);

            Ix = this.m_nCountCtrlPoints;
            tan.writeInteger(dos, Ix);


            for (int i = 0; i < m_nCountCtrlPoints; i++) {

                Ix = Float.floatToIntBits(this.m_pptCtrl[i].x);
                tan.writeInteger(dos, Ix);

                Ix = Float.floatToIntBits(this.m_pptCtrl[i].y);
                tan.writeInteger(dos, Ix);
            }
//			int ch = dis.read();
//			if (ch < 0)
//				throw new EOFException();
//			bTangency = (ch != 0);

            //保存相切平移
            dos.writeBoolean(!(this.m_bTangency));

            //路段端点打舵距离
            dos.writeFloat(m_TranFloat.tranFloat(m_fLenForAngJump));

            //入弯速度
            dos.writeFloat(m_TranFloat.tranFloat(m_fTurnVel1));

            //出弯速度
            dos.writeFloat(m_TranFloat.tranFloat(m_fTurnVel2));

            //驱动单元数量
            tan.writeInteger(dos, nDriveUnitCount);

            for (int i = 0; i < nDriveUnitCount; i++) {
                //驱动单元类型
                tan.writeInteger(dos, UnitType);

                //驱动单元安装位置X
                dos.writeFloat(m_TranFloat.tranFloat(drive_unit_x));

                //驱动单元安装位置Y
                dos.writeFloat(m_TranFloat.tranFloat(drive_unit_y));

                //轮最大速度
                dos.writeFloat(m_TranFloat.tranFloat(fVelMax));

                //轮最大加速度
                dos.writeFloat(m_TranFloat.tranFloat(fVelACC));

                //舵最大速度
                dos.writeFloat(m_TranFloat.tranFloat(fThitaDiffMax));

                //舵最大加速度
                dos.writeFloat(m_TranFloat.tranFloat(fAngVelACC));

                //最大打舵角度
                dos.writeFloat(m_TranFloat.tranFloat(fSteerAngle));

                //自定义数据
                for (int j = 0; j < 10; j++) {
                    dos.writeFloat(m_TranFloat.tranFloat(fUserData[j]));
                }
            }

            //20200528添加
//            dos.writeFloat(m_TranFloat.tranFloat(m_fVelMax));
//            dos.writeFloat(m_TranFloat.tranFloat(m_fThitaDiffMax));
//            dos.writeFloat(m_TranFloat.tranFloat(m_fAngVelMax));
//            dos.writeFloat(m_TranFloat.tranFloat(m_fAngVelACC));
//            dos.writeFloat(m_TranFloat.tranFloat(m_fVelACC));
//            dos.writeFloat(m_TranFloat.tranFloat(m_fLenForAngJump));
//            dos.writeFloat(m_TranFloat.tranFloat(m_fThitaDiffMaxForStAndEd));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void ModifyParmByCurve() {
        // 更新控制点
        for (int i = 0; i < m_nCountCtrlPoints; i++) {
            m_pptCtrl[i].x = m_Curve.m_ptKey[i + 1].x;
            m_pptCtrl[i].y = m_Curve.m_ptKey[i + 1].y;
        }

        //更新起点终点姿态
        m_pstStart.x = m_Curve.m_ptKey[0].x;
        m_pstStart.y = m_Curve.m_ptKey[0].y;
        m_pstEnd.x = m_Curve.m_ptKey[m_nCountCtrlPoints + 1].x;
        m_pstEnd.y = m_Curve.m_ptKey[m_nCountCtrlPoints + 1].y;
        Line ln1 = new Line(m_Curve.m_ptKey[0], m_Curve.m_ptKey[1]);
        m_pstStart.fThita = ln1.m_angSlant.m_fRad;
        Line ln2 = new Line(m_Curve.m_ptKey[m_nCountCtrlPoints], m_Curve.m_ptKey[m_nCountCtrlPoints + 1]);
        m_pstEnd.fThita = ln2.m_angSlant.m_fRad;
        // 初始化起点、终点处的方向角
        m_angStartHeading = m_pstStart.GetAngle();
        m_angEndHeading = m_pstEnd.GetAngle();
        //更新长度
        m_fSize = m_Curve.m_fTotalLen;  ///////////////////////////？？
    }

    public boolean isM_bCurvature() {
        return m_bCurvature;
    }

    @Override
    public boolean ISInRect(double minx, double miny, double maxx, double maxy) {
        return m_Curve.ISInRect(minx, miny, maxx, maxy);
    }


    public void Draw(CoordinateConversion ScrnRef, Canvas Grp, int cr, int nWidth) {
        m_Curve.Draw(ScrnRef, Grp, cr, nWidth, nWidth, false);
    }


    // 画出此路径曲线的控制点
    public void DrawCtrlPoints(CoordinateConversion ScrnRef, Canvas Grp, Typeface pLogFont, int cr, int nWidth) {
        m_Curve.DrawCtrlPoints(ScrnRef, Grp, pLogFont, cr, nWidth);
    }

    @Override
    public void DrawID(CoordinateConversion ScrnRef, Canvas Grp) {
        m_Curve.SetCurT(0.8f);
        Point2d pntT = m_Curve.TrajFun();
        PointF pnt1 = ScrnRef.worldToScreen(pntT.x, pntT.y);
        String str = String.valueOf(m_uId);
        int width = Grp.getWidth();
        int Height = Grp.getHeight();
        if ((pnt1.x < 0 || pnt1.x > width) && (pnt1.y < 0 || pnt1.y > Height)) {
            return;
        }
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);

        Grp.drawText(str, pnt1.x + 4, pnt1.y + 14, paint);
    }
}
