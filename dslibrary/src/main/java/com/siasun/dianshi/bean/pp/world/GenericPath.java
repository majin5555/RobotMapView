package com.siasun.dianshi.bean.pp.world;

import static java.lang.Math.abs;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;

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
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * GenericPath类：通用路径类，继承自Path抽象类
 * 用于表示机器人的通用路径，基于贝塞尔曲线实现
 * 支持路径的创建、绘制、数据读写等功能
 */
public class GenericPath extends Path {
    /**
     * 路径起始节点处的航向角
     */
    public Angle m_angStartHeading;
    /**
     * 路径结束节点处的航向角
     */
    public Angle m_angEndHeading;
    /**
     * 控制点个数，通常为2个
     */
    public int m_nCountCtrlPoints;
    /**
     * 控制点数组，用于描述贝塞尔曲线的形状
     */
    public Point2d[] m_pptCtrl;

    /**
     * 路径起始状态，包含位置和角度信息
     */
    public Posture m_pstStart;
    /**
     * 路径结束状态，包含位置和角度信息
     */
    public Posture m_pstEnd;
    /**
     * 相切标志：是否采用相切平移模式，运动模式 0:相切 1:平移
     */
    public boolean m_bTangency;
    /**
     * 运行功能：运动模式 0:平移 1:相切 2:自旋
     */
    public short m_uRunFunction;

    /**
     * 贝塞尔曲线对象，用于路径的几何表示
     */
    public Bezier m_Curve;
    // 曲线约束条件参数
    /**
     * 最大速度
     */
    public float m_fVelMax;
    /**
     * 最大角度差
     */
    public float m_fThitaDiffMax;
    /**
     * 最大角速度
     */
    public float m_fAngVelMax;
    /**
     * 角加速度
     */
    public float m_fAngVelACC;
    /**
     * 线加速度
     */
    public float m_fVelACC;
    /**
     * 角度跳跃的长度阈值
     */
    public float m_fLenForAngJump;
    /**
     * 起始和结束点的最大角度差
     */
    public float m_fThitaDiffMaxForStAndEd;
    /**
     * 字节序转换对象，用于文件读写时的字节序处理
     */
    TranBytes m_TranFloat = new TranBytes();

    /**
     * 路径点击检测的偏移量（屏幕坐标）
     */
    private final int pathOffset = 20;
    /**
     * 节点点击检测的偏移量（屏幕坐标）
     */
    private final int nodeOffset = 20;

    /**
     * 贝塞尔曲线的K值，用于曲线形状控制
     */
    public static float BEZIER_K = 0.95f;
    /**
     * 入弯速度
     */
    private float m_fTurnVel1 = 1.0f;
    /**
     * 出弯速度
     */
    private float m_fTurnVel2 = 1.0f;
    /**
     * 驱动单元数量
     */
    private int nDriveUnitCount = 1;
    /**
     * 驱动单元安装位置X坐标
     */
    private float drive_unit_x = 0.f;
    /**
     * 驱动单元安装位置Y坐标
     */
    private float drive_unit_y = 0.f;
    /**
     * 轮最大速度
     */
    private float fVelMax = 1.f;
    /**
     * 轮最大加速度
     */
    private float fVelACC = 0.2f;
    /**
     * 舵最大速度
     */
    private float fThitaDiffMax = 1.f;
    /**
     * 舵最大加速度
     */
    private float fAngVelACC = 1.f;
    /**
     * 最大打舵角度
     */
    private float fSteerAngle = 80.f;
    /**
     * 用户自定义数据数组
     */
    private float[] fUserData = new float[10];
    /**
     * 驱动单元类型，5表示双轮差动
     */
    private int UnitType = 5;

    /**
     * 曲线曲率是否满足要求的标志
     */
//    public boolean m_bCurvature;
    /**
     * 日志标签
     */
    private static String TAG = GenericPath.class.getSimpleName();


//////////////////////////////////////////////////////////////////////////////
// GenericPath类的实现部分

    /**
     * 构造方法：创建通用路径对象
     *
     * @param uId        路径ID
     * @param nStartNode 起始节点ID
     * @param nEndNode   结束节点ID
     * @param pstStart   起始姿态
     * @param pstEnd     结束姿态
     * @param fLen1      起始节点到第一个控制点的长度
     * @param fLen2      结束节点到第二个控制点的长度
     * @param fVeloLimit 速度限制数组
     * @param nGuideType 引导类型
     * @param uObstacle  障碍物类型
     * @param uDir       方向
     * @param uExtType   扩展类型
     * @param nodeBase   节点管理对象
     */
    public GenericPath(int uId, int nStartNode, int nEndNode, Posture pstStart, Posture pstEnd, float fLen1, float fLen2, float[] fVeloLimit, short nGuideType, short uObstacle, short uDir, short uExtType, NodeBase nodeBase) {
        short type = 10;
        m_fVelMax = 0.7f;
        m_fThitaDiffMax = 0.6f;
        m_fAngVelMax = 0.3f;
        m_fAngVelACC = 0.1f;
        m_fVelACC = 0.5f;
        m_fLenForAngJump = 0.1f;
        m_fThitaDiffMaxForStAndEd = 0.6f;
//        m_bCurvature = true;

        super.Create(uId, nStartNode, nEndNode, fVeloLimit, type, nGuideType, 0, uExtType, nodeBase);
        Create(pstStart, pstEnd, fLen1, fLen2);
    }

    /**
     * 构造方法：使用控制点创建通用路径对象
     *
     * @param uId        路径ID
     * @param nStartNode 起始节点ID
     * @param nEndNode   结束节点ID
     * @param pstStart   起始姿态
     * @param pstEnd     结束姿态
     * @param pptCtrl    控制点数组
     * @param fVeloLimit 速度限制数组
     * @param nGuideType 引导类型
     * @param uObstacle  障碍物类型
     * @param uDir       方向
     * @param uExtType   扩展类型
     * @param nodeBase   节点管理对象
     * @param pathParam  路径参数
     */
    public GenericPath(int uId, int nStartNode, int nEndNode, Posture pstStart, Posture pstEnd, Point2d[] pptCtrl /*float fLen1, float fLen2*/, float[] fVeloLimit, short nGuideType, short uObstacle, short uDir, short uExtType, NodeBase nodeBase, short pathParam) {
        short type = 10;
        m_fVelMax = 0.7f;
        m_fThitaDiffMax = 0.6f;
        m_fAngVelMax = 0.3f;
        m_fAngVelACC = 0.1f;
        m_fVelACC = 0.5f;
        m_fLenForAngJump = 0.1f;
        m_fThitaDiffMaxForStAndEd = 0.6f;
//        m_bCurvature = true;

        super.CreatePP(uId, nStartNode, nEndNode, fVeloLimit, type, nGuideType, 0, uExtType, nodeBase, pathParam);
//        Create(pstStart, pstEnd, fLen1, fLen2);
        Create(pstStart, pstEnd, 2, pptCtrl);
    }


    /**
     * 默认构造方法：创建空的通用路径对象
     */
    public GenericPath() {
        m_pstStart = new Posture();
        m_pstEnd = new Posture();
        m_nCountCtrlPoints = 2;
        m_pptCtrl = null;

        m_fVelMax = 0.7f;
        m_fThitaDiffMax = 0.6f;
        m_fAngVelMax = 0.3f;
        m_fAngVelACC = 0.1f;
        m_fVelACC = 0.5f;
        m_fLenForAngJump = 0.1f;
        m_fThitaDiffMaxForStAndEd = 0.6f;
//        m_bCurvature = true;
    }

    /**
     * 获取相切标志
     *
     * @return 是否相切
     */
    public boolean GetTangency() {
        return m_bTangency;
    }


    /**
     * 点命中测试：判断屏幕点是否命中路径或其关键点
     *
     * @param pnt     屏幕坐标点
     * @param ScrnRef 坐标转换对象
     * @return -1：未命中；0：命中路径；>0：命中第n个关键点
     */
    @Override
    int PointHitTest(Point pnt, CoordinateConversion ScrnRef) {
        // 判断屏幕点是否落在某个关键点处
        for (int i = 1; i < m_Curve.m_nCountKeyPoints - 1; i++) {
//            Point pntKey = ScrnRef.GetWindowPoint(m_Curve.m_ptKey[i]);
            PointF pntKey = ScrnRef.worldToScreen(m_Curve.m_ptKey[i].x, m_Curve.m_ptKey[i].y);
            int offSet = 20;
            Rect r = new Rect((int) (pntKey.x - nodeOffset), (int) (pntKey.y - nodeOffset), (int) (pntKey.x + nodeOffset), (int) (pntKey.y + nodeOffset)); // Construct an emtpy rectangle

            if (pnt.x >= r.left && pnt.x <= (r.left + r.width()) && pnt.y < r.bottom && pnt.y >= (r.bottom - r.height()))
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
            if (nDist <= pathOffset) return 0;
        }

        return -1;
    }


    /**
     * 创建路径：使用控制点创建路径
     *
     * @param pstStart         起始姿态
     * @param pstEnd           结束姿态
     * @param nCountCtrlPoints 控制点数量
     * @param pptCtrl          控制点数组
     * @return 创建是否成功
     */
    private boolean Create(Posture pstStart, Posture pstEnd, int nCountCtrlPoints, Point2d[] pptCtrl) {
        m_pstStart = pstStart;
        m_pstEnd = pstEnd;
        m_nCountCtrlPoints = nCountCtrlPoints;

        // Ϊ�ؼ������ռ�
        m_pptCtrl = new Point2d[m_nCountCtrlPoints];
        if (m_pptCtrl == null) return false;

        // ���ƿ��Ƶ�����
        for (int i = 0; i < m_nCountCtrlPoints; i++)
            m_pptCtrl[i] = pptCtrl[i];

        // ��ʼ��·��
        return Init();
    }


    /**
     * 创建路径：使用长度参数创建路径
     *
     * @param pstStart 起始姿态
     * @param pstEnd   结束姿态
     * @param fLen1    起始节点到第一个控制点的长度
     * @param fLen2    结束节点到第二个控制点的长度
     * @return 创建是否成功
     */
    private boolean Create(Posture pstStart, Posture pstEnd, float fLen1, float fLen2) {
        m_pstStart = pstStart;
        m_pstEnd = pstEnd;
        m_nCountCtrlPoints = 2;

        // Ϊ�ؼ������ռ�
        m_pptCtrl = new Point2d[m_nCountCtrlPoints];
        if (m_pptCtrl == null) return false;

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

    /**
     * 初始化路径：根据提供的关键点位置初始化路径
     *
     * @return 初始化是否成功
     */
    public boolean Init() {
        Point2d[] pptKey = new Point2d[m_nCountCtrlPoints + 2];

        pptKey[0] = new Point2d();
        pptKey[0].x = m_pstStart.x;
        pptKey[0].y = m_pstStart.y;
        pptKey[m_nCountCtrlPoints + 1] = new Point2d();
        pptKey[m_nCountCtrlPoints + 1].x = m_pstEnd.x;
        pptKey[m_nCountCtrlPoints + 1].y = m_pstEnd.y;

        for (int i = 0; i < m_nCountCtrlPoints; i++) {
            if (pptKey[i + 1] == null) {
                pptKey[i + 1] = new Point2d();
            }
            pptKey[i + 1].x = m_pptCtrl[i].x;
            pptKey[i + 1].y = m_pptCtrl[i].y;
        }

        m_Curve = new Bezier();
        m_Curve.Create(m_nCountCtrlPoints + 2, pptKey);

        m_angStartHeading = m_pstStart.GetAngle();
        m_angEndHeading = m_pstEnd.GetAngle();
        m_fSize = m_Curve.m_fTotalLen;
        return true;
    }

    /**
     * 获取车辆在指定节点处的航向角
     *
     * @param nd 节点对象
     * @return 航向角对象
     */
    @Override
    public Angle GetHeading(Node nd) {
        if (nd.m_uId == m_uStartNode) return m_angStartHeading;
        else return m_angEndHeading;
    }


    /**
     * 从数据流创建路径对象
     *
     * @param dis 数据输入流
     * @return 创建是否成功
     */
    public boolean Create(DataInputStream dis) {
        short uDir;            // Positive input/negative input
        Point2d[] CtrlPnt = new Point2d[2];
        CtrlPnt[0] = new Point2d();
        CtrlPnt[1] = new Point2d();

        boolean bTangency = false;

        //读取path基本数据类型
        if (!super.Create(dis)) return false;
        try {
            //ar >> m_uFwdRotoScannerObstacle >> m_uFwdObdetectorObstacle >> m_uBwdRotoScannerObstacle >> m_uBwdObdetectorObstacle >> uDir;
            int ch1 = dis.read();
            int ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uFwdRotoScannerObstacle = ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uFwdObdetectorObstacle = ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uBwdRotoScannerObstacle = ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uBwdObdetectorObstacle = ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            uDir = (short) ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            int ch3 = dis.read();
            int ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            int tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            float angShiftHeading = Float.intBitsToFloat(tempI);

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uRunFunction = (short) ((ch2 << 8) + (ch1 << 0));


            m_uPathHeading = uDir;
//	#if 1
            // ����ؼ�������
            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            this.m_nCountCtrlPoints = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
//	#else
//		m_nCountCtrlPoints = 2;
//	#endif

            if (m_pptCtrl != null) m_pptCtrl = null;
//			delete []m_pptCtrl;

            // 确保控制点数量为正数，避免创建负长度数组
            if (this.m_nCountCtrlPoints > 0) {
                m_pptCtrl = new Point2d[m_nCountCtrlPoints];
                for (int i = 0; i < m_nCountCtrlPoints; i++) {
                    m_pptCtrl[i] = new Point2d();
                }

                for (int i = 0; i < m_nCountCtrlPoints; i++) {
                    ch1 = dis.read();
                    ch2 = dis.read();
                    ch3 = dis.read();
                    ch4 = dis.read();
                    if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
                    tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
                    m_pptCtrl[i].x = Float.intBitsToFloat(tempI);

                    ch1 = dis.read();
                    ch2 = dis.read();
                    ch3 = dis.read();
                    ch4 = dis.read();
                    if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
                    tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
                    m_pptCtrl[i].y = Float.intBitsToFloat(tempI);
                }
            } else {
                this.m_nCountCtrlPoints = 0;
                this.m_pptCtrl = null;
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
        ptStart.x = GetStartNode().x;
        ptStart.y = GetStartNode().y;
        ptEnd.x = GetEndNode().x;
        ptEnd.y = GetEndNode().y;

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

    /**
     * 将路径对象保存到数据流
     *
     * @param dos 数据输出流
     * @return 保存是否成功
     */
    public boolean Save(DataOutputStream dos) {
        short uDir;            // Positive input/negative input
        short uTemp;
        Point2d[] CtrlPnt = new Point2d[2];
        CtrlPnt[0] = new Point2d();
        CtrlPnt[1] = new Point2d();
        float angShiftHeading = 0.0f;
        boolean bTangency = false;

        if (!super.Save(dos)) return false;
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

    /**
     * 根据曲线修改路径参数：当曲线发生变化时更新路径的相关参数
     */
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

    /**
     * 获取曲线曲率是否满足要求的标志
     *
     * @return 曲率是否满足要求
     */
//    public boolean isM_bCurvature() {
//        return m_bCurvature;
//    }

    /**
     * 判断路径是否在指定矩形范围内
     *
     * @param minx 矩形最小x坐标
     * @param miny 矩形最小y坐标
     * @param maxx 矩形最大x坐标
     * @param maxy 矩形最大y坐标
     * @return 是否在矩形范围内
     */
    @Override
    public boolean ISInRect(double minx, double miny, double maxx, double maxy) {
        return m_Curve.ISInRect(minx, miny, maxx, maxy);
    }


    /**
     * 绘制路径
     *
     * @param ScrnRef 坐标转换对象
     */
    android.graphics.Path mBezirPath = new android.graphics.Path();

    public void Draw(CoordinateConversion conversion, Canvas canvas, int color, Paint paint) {
        if (m_Curve != null && m_Curve.m_ptKey != null) {
            PointF mStart = conversion.worldToScreen(m_Curve.m_ptKey[0].x, m_Curve.m_ptKey[0].y);
            PointF mControl1 = conversion.worldToScreen(m_Curve.m_ptKey[1].x, m_Curve.m_ptKey[1].y);
            PointF mControl2 = conversion.worldToScreen(m_Curve.m_ptKey[2].x, m_Curve.m_ptKey[2].y);
            PointF mEnd = conversion.worldToScreen(m_Curve.m_ptKey[3].x, m_Curve.m_ptKey[3].y);

            // 重置路径，避免重复绘制
            mBezirPath.reset();
            paint.setColor(color);

            // 检查起点和终点是否相同
            if (mStart.equals(mEnd)) {
                // 起点和终点相同，绘制一个小圆点
                float radius = 3f;
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(mStart.x, mStart.y, radius, paint);
            } else {
                // 正常绘制贝塞尔曲线
                mBezirPath.moveTo(mStart.x, mStart.y);
                mBezirPath.cubicTo(mControl1.x, mControl1.y, mControl2.x, mControl2.y, mEnd.x, mEnd.y);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(mBezirPath, paint);
            }
        }
    }


    /**
     * 绘制路径的控制点
     *
     * @param ScrnRef    坐标转换对象
     * @param Grp        画布对象
     * @param cr         控制点颜色
     * @param nPointSize 控制点大小
     */
    public void DrawCtrlPoints(CoordinateConversion ScrnRef, Canvas Grp, int cr, int nPointSize, Paint paint) {
        m_Curve.DrawCtrlPoints(ScrnRef, Grp, cr, nPointSize, paint);
    }

    /**
     * 绘制路径ID
     *
     * @param ScrnRef 坐标转换对象
     * @param Grp     画布对象
     */
    @Override
    public void DrawID(CoordinateConversion ScrnRef, Canvas Grp, int color, Paint paint) {
        m_Curve.SetCurT(0.8f);
        Point2d pntT = m_Curve.TrajFun();
        PointF pnt1 = ScrnRef.worldToScreen(pntT.x, pntT.y);
        String str = String.valueOf(m_uId);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        Grp.drawText(str, pnt1.x, pnt1.y, paint);
    }
}
