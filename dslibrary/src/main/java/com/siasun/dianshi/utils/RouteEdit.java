package com.siasun.dianshi.utils;

import static java.lang.Math.PI;

import android.graphics.Point;
import android.graphics.PointF;


import com.siasun.dianshi.attr.PathBaseAttr;
import com.siasun.dianshi.bean.Point2d;
import com.siasun.dianshi.bean.pp.Angle;
import com.siasun.dianshi.bean.pp.Bezier;
import com.siasun.dianshi.bean.pp.DefPosture;
import com.siasun.dianshi.bean.pp.Line;
import com.siasun.dianshi.bean.pp.Posture;
import com.siasun.dianshi.bean.world.CLayer;
import com.siasun.dianshi.bean.world.GenericPath;
import com.siasun.dianshi.bean.world.Path;
import com.siasun.dianshi.bean.world.World;

import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * 路径编辑 （包含创建与编辑）
 */
public class RouteEdit {
    private int mCreateWorldStage = 0;        //定义路径处的阶段
    private final int WRD_DEF_NORMAL = 0;      //空闲
    private final int WRD_DRF_PST = 1;         //定义关键位姿（创建）
    private final int WRD_MOD_PST = 2;         //修改位姿
    private final int WRD_DELETE_PST = 3;      //删除位姿

    public DefPosture m_KeyPst = new DefPosture();
    Lock m_lock = new ReentrantLock();

    /**
     * 开始创建路径状态
     */
    public void startDefPostureState() {
        mCreateWorldStage = WRD_DRF_PST;
    }

    /**
     * 手动编辑
     */
    public void startModifyNode() {
        mEditWorldStage = WRD_MOD_NODE;
    }

    /**
     * 手动编辑状态
     *
     * @return
     */
    public boolean isStartModifyNodeState() {
        return mEditWorldStage == WRD_MOD_NODE;
    }


    /**
     * 属性修改
     */
    public void startModifAttr() {
        mEditWorldStage = WRD_MODIF_ATTR;
    }

    //返回节点编辑的空闲状态
    public void leaveNodeEdit() {
        mEditWorldStage = WRD_EDIT_NORMAL;
    }

    /**
     * 路径删除状态
     */
    public void deleteRouteState() {
        mEditWorldStage = WRD_MULTIDELETE_NODE;
    }

    /**
     * 是否是删除路径状态
     */
    public boolean isDeleteRouteState() {
        return mEditWorldStage == WRD_MULTIDELETE_NODE;
    }

    /**
     * 返回空闲状态
     */
    public void startLeaveNormalFunction() {
        mCreateWorldStage = WRD_DEF_NORMAL;
    }

    /****************************************************************
     *                       路径 手指按下                            *
     ***************************************************************/

    public void downActionAddRoute(CoordinateConversion mScrRef, PointF ptDown, World mWorld) {

        switch (mCreateWorldStage) {
            case WRD_DEF_NORMAL:
                break;
            case WRD_DRF_PST://添加点

// old
//                Posture pst = new Posture();
//                pst.x = mScrRef.GetWorldPoint(ptDown).x;
//                pst.y = mScrRef.GetWorldPoint(ptDown).y;
//                pst.fThita = 0;
//                m_KeyPst.AddPst(pst);
//                LogUtil.INSTANCE.w("添加前 手指下 m_KeyPs.size " + m_KeyPst.m_PstV.size());
                //屏幕坐标转世界坐标
                PointF world = mScrRef.screenToWorld(ptDown.x, ptDown.y);
                Posture pst = new Posture();
                pst.x = world.x;
                pst.y = world.y;
                pst.fThita = 0;
//                LogUtil.INSTANCE.i("手指下  " + pst);
                m_KeyPst.AddPst(pst);
//                LogUtil.INSTANCE.i("手指下 m_KeyPs " + m_KeyPst.m_PstV);
//                LogUtil.INSTANCE.w("添加后 手指下 m_KeyPs.size " + m_KeyPst.m_PstV.size());

                CreatePathByKey(mWorld, mScrRef);
                break;

            case WRD_MOD_PST: {
                PointF pointF = mScrRef.screenToWorld(ptDown.x, ptDown.y);
                GetSelectPstId(mScrRef, new Point((int) pointF.x, (int) pointF.y));  //得到选择的关键点
                GetSelectPst();      //得到选择的关键点的数据
            }
            break;
            case WRD_DELETE_PST: {
                PointF pointF = mScrRef.screenToWorld(ptDown.x, ptDown.y);
                GetSelectPstId(mScrRef, new Point((int) pointF.x, (int) pointF.y));  //得到选择的关键点
                DeletePst(); //删除关键点
            }
            break;

        }
    }


    /**
     * **************************************************************
     */
    //当前选择的节点
    public int mCurNodeId = -1;
    //选择控制点ID
    public int mCurKeyId = -1;

    //路段起点ID
    public int m_nStartNodeId = -1;
    //路段终点ID
    public int m_nEndNodeId = -1;

    private Posture m_pstStart = new Posture();
    private Posture m_pstEnd = new Posture();

    private Bezier m_Bezier = new Bezier(); // 当前的Bezier曲线段
    public final float BEZIER_K = 0.9f;

    // 需要用户设定的基本参数
    //(1)路段参数
    private float[] m_LimVal = new float[2];    // 路段限速[0]:正向限速 [1]:逆向限速
    private short m_GuideType = 2;              // 路段导航类型


    //根据关键位姿创建任务（会直接生成地图）
    public void CreatePathByKey(World world, CoordinateConversion scrRef) {
        if (m_KeyPst.m_PstV.size() != 2) return;
        //终点方向等于起点到终点的方向
        Line line = new Line(m_KeyPst.m_PstV.get(0), m_KeyPst.m_PstV.get(1));

//        float fLen = m_KeyPst.m_PstV.get(0).DistanceTo(m_KeyPst.m_PstV.get(1));
        //计算两点的距离
        float fLen = (float) Math.hypot(line.m_ptStart.x - line.m_ptEnd.x, line.m_ptStart.y - line.m_ptEnd.y);
        // 直线不存在,删除新增点（防止控制点离起始点过近）
        if (fLen < 1e-3F) {
            m_KeyPst.m_PstV.remove(1);
            return;
        }
        m_KeyPst.m_PstV.get(1).fThita = line.m_angSlant.m_fRad;

        /////////////
        Posture startPst = new Posture();
        Posture endPst = new Posture();

        for (int i = 0; i < m_KeyPst.m_PstV.size() - 1; i++) {
//            LogUtil.INSTANCE.e("i= " + i + "   " + m_KeyPst.m_PstV.get(i));

            if (i == 0) {
                m_nStartNodeId = -1;
                m_nEndNodeId = -1;
            }
            startPst = m_KeyPst.m_PstV.get(i);
            endPst = m_KeyPst.m_PstV.get(i + 1);
            //世界转屏幕坐标
            PointF screenStart = scrRef.worldToScreen(startPst.x, startPst.y);
            // 判断当前关键位姿和现有节点是否重合
            GetSelectNode(new Point((int) screenStart.x, (int) screenStart.y), scrRef, world);
            if (mCurNodeId != -1) {
                m_nStartNodeId = mCurNodeId;
                // 取得对应于nStartNodeId节点的所有姿态角
                Angle[] Angles = new Angle[4];

                int nHeadingAngleCount = world.m_layers.GetNodeHeadingAngle(m_nStartNodeId, Angles, 4);
                if (nHeadingAngleCount > 0) {
                    startPst.x = world.m_layers.m_PathBase.m_MyNode.GetNode(m_nStartNodeId).x;
                    startPst.y = world.m_layers.m_PathBase.m_MyNode.GetNode(m_nStartNodeId).y;
                    startPst.fThita = Angles[0].m_fRad;
                }
            }

            PointF screenEnd = scrRef.worldToScreen(endPst.x, endPst.y);
            GetSelectNode(new Point((int) screenEnd.x, (int) screenEnd.y), scrRef, world);
            if (mCurNodeId != -1) {
                m_nEndNodeId = mCurNodeId;
                // 取得对应于m_nEndNodeId节点的所有姿态角
                Angle[] Angles = new Angle[4];

                int nHeadingAngleCount = world.m_layers.GetNodeHeadingAngle(m_nEndNodeId, Angles, 4);
                if (nHeadingAngleCount > 0) {
                    endPst.x = world.m_layers.m_PathBase.m_MyNode.GetNode(m_nEndNodeId).x;
                    endPst.y = world.m_layers.m_PathBase.m_MyNode.GetNode(m_nEndNodeId).y;
                    endPst.fThita = Angles[0].m_fRad;
                }
            }
            mCurNodeId = -1;

            //如果是角度相同，有可能是直线
            if (startPst.fThita == endPst.fThita || Math.abs((startPst.fThita - endPst.fThita)) == PI) {
                Line ln = new Line(startPst.GetPoint2dObject(), endPst.GetPoint2dObject());
                float fTotalLen = startPst.GetPoint2dObject().DistanceTo(endPst.GetPoint2dObject());

                // 直线不存在
                if (fTotalLen < 1e-3F) {
                    m_KeyPst.m_PstV.remove(1);
                    return;
                }

                //直线
                if (ln.m_angSlant.m_fRad == startPst.fThita || Math.abs((ln.m_angSlant.m_fRad - startPst.fThita)) == PI) {

                    world.m_layers.AddLinePath(startPst, endPst, m_nStartNodeId, m_nEndNodeId, m_LimVal, m_GuideType);
                    m_nStartNodeId = world.m_layers.m_PathBase.m_pPathIdx[world.m_layers.m_PathBase.m_uCount - 1].m_ptr.m_uStartNode;
                    m_nEndNodeId = world.m_layers.m_PathBase.m_pPathIdx[world.m_layers.m_PathBase.m_uCount - 1].m_ptr.m_uEndNode;

                    m_nStartNodeId = m_nEndNodeId;
                    m_nEndNodeId = -1;
                } else {
                    // 整个曲线已确定
                    m_pstStart = startPst;
                    m_pstEnd = endPst;
                    boolean res = m_Bezier.Create(m_pstStart, m_pstEnd, BEZIER_K);
                    if (!res) {
                        //终点无效
                        m_KeyPst.m_PstV.remove(1);
                        return;
                    }

                    float fDist1 = m_Bezier.m_ptKey[0].DistanceTo(m_Bezier.m_ptKey[1]);
                    float fDist2 = m_Bezier.m_ptKey[2].DistanceTo(m_Bezier.m_ptKey[3]);

                    boolean isCurvature = world.m_layers.AddGenericPath(m_pstStart, m_pstEnd, fDist1, fDist2, m_nStartNodeId, m_nEndNodeId);
                    // mListener.showCurvature(isCurvature);
                    boolean ret = ((GenericPath) (world.m_layers.m_PathBase.m_pPathIdx[world.m_layers.m_PathBase.m_uCount - 1].m_ptr)).m_Curve.BezierOptic();
                    m_nStartNodeId = world.m_layers.m_PathBase.m_pPathIdx[world.m_layers.m_PathBase.m_uCount - 1].m_ptr.m_uStartNode;
                    m_nEndNodeId = world.m_layers.m_PathBase.m_pPathIdx[world.m_layers.m_PathBase.m_uCount - 1].m_ptr.m_uEndNode;

                    m_nStartNodeId = m_nEndNodeId;
                    m_nEndNodeId = -1;
                }
            } else { //如果角度不同一定是曲线

                // 整个曲线已确定
                m_pstStart = startPst;
                m_pstEnd = endPst;
                boolean res = m_Bezier.Create(m_pstStart, m_pstEnd, BEZIER_K);
                if (!res) {
                    //终点无效
                    m_KeyPst.m_PstV.remove(1);
                    return;
                }

                float fDist1 = m_Bezier.m_ptKey[0].DistanceTo(m_Bezier.m_ptKey[1]);
                float fDist2 = m_Bezier.m_ptKey[2].DistanceTo(m_Bezier.m_ptKey[3]);

                boolean isCurvature = world.m_layers.AddGenericPath(m_pstStart, m_pstEnd, fDist1, fDist2, m_nStartNodeId, m_nEndNodeId);
                //mListener.showCurvature(isCurvature);
                boolean ret = ((GenericPath) (world.m_layers.m_PathBase.m_pPathIdx[world.m_layers.m_PathBase.m_uCount - 1].m_ptr)).m_Curve.BezierOptic();
                m_nStartNodeId = world.m_layers.m_PathBase.m_pPathIdx[world.m_layers.m_PathBase.m_uCount - 1].m_ptr.m_uStartNode;
                m_nEndNodeId = world.m_layers.m_PathBase.m_pPathIdx[world.m_layers.m_PathBase.m_uCount - 1].m_ptr.m_uEndNode;

                m_nStartNodeId = m_nEndNodeId;
                m_nEndNodeId = -1;
            }

        }
        m_KeyPst.m_PstV.remove(0);
    }

    //得到当前选择的节点
    public int GetSelectNode(Point pnt, CoordinateConversion scrRef, World world) {
        mCurNodeId = world.m_layers.PointHitNodeTest(pnt, scrRef);
        return mCurNodeId;
    }

    //得到当前选择的位姿
    public void GetSelectPstId(CoordinateConversion scrRef, Point pnt) {
        m_KeyPst.PointHitPst(scrRef, pnt);
    }

    //得到选择的位姿
    public Posture GetSelectPst() {
        return m_KeyPst.GetPst();
    }

    //删除位姿
    public void DeletePst() {
        m_KeyPst.DeletePst();
    }


    /**************路径编辑***************/
    public int mEditWorldStage = 0;          //编辑路径处的阶段
    public final int WRD_EDIT_NORMAL = 0;     //空闲
    public final int WRD_ADD_NODE = 1;        //增加节点
    public final int WRD_DELETE_NODE = 2;     //删除节点
    public final int WRD_MOD_NODE = 3;        //调整节点
    public final int WRD_MULTIDELETE_NODE = 4;  //路径删除（批量删除）
    public final int WRD_MODIF_ATTR = 5;      //属性修改
    public final int WRD_MOD_KEY = 6;           // 移动控制点
    public final int WRD_ADD_REG_CON = 7;       // 区域连接，绘制两个点之间的线
    public final int WRD_TO_LINE = 8;       // 转为直线


    //修改节点
    public DefPosture m_ModNodePos = new DefPosture(); //选择的节点，对应的位姿
    public Boolean mModNodeAng = false;              //修改节点的切线方向

    public Vector<Integer> m_nCurPathIndex = new Vector();  //当前选择的路径(下标)

    public DragRect m_DragRect = new DragRect();

    public int m_RegConDownCount;   // 增加区域连接线过程中单击次数
    public Point2d m_RegConStart = new Point2d();// 区域连接线起点
    public Point2d m_RegConEnd = new Point2d();// 区域连接线终点

    /**
     * 编辑路径
     *
     * @param world
     * @param scrRef
     * @param ptDown 屏幕坐标
     */
    public void downActionForEditWorld(CoordinateConversion scrRef, PointF ptDown, World world) {
        PointF pointF = scrRef.screenToWorld(ptDown.x, ptDown.y);
        Point2d pnt = new Point2d(pointF.x, pointF.y);
        Point point = new Point((int) ptDown.x, (int) ptDown.y);
        switch (mEditWorldStage) {

            case WRD_MOD_NODE: {//修改节点
                //如果得到了选择节点的位姿，就不需要选择新的节点

                if (m_ModNodePos.CheckPointOnPostureArrowTip(scrRef, point)) {
                    mModNodeAng = true;
                } else {
                    GetSelectNode(point, scrRef, world);
                    mModNodeAng = false;
                }

                //选择路线，用于修改控制点
                int[] pathId = GetSelectSinglePath(point, scrRef, world);
                mCurKeyId = -1;
                m_nCurPathIndex.clear();

                //scj add 2022-08-09 实现点击已选择路段，取消选择
                if (pathId[0] >= 0) {
                    mCurKeyId = pathId[1];
                    m_nCurPathIndex.add(pathId[0]);
                }
            }
            break;
//            case WRD_DELETE_NODE: {
//                //删除节点
//                try {
//                    int iNodeId = GetSelectNode(scrRef.GetWindowPoint(pnt), scrRef, world);
//                    if (iNodeId != -1) {
//                        deleteNode(world);
//                    }
//                } catch (Exception ex) {
//                    System.out.println(ex.toString());
//                }
//            }
//            break;
            // 属性修改
            case WRD_MODIF_ATTR: {
                GetSelectNode(point, scrRef, world);
                if (mCurNodeId > 0) {
                    // 添加修改节点对话框
//                    slamMapView.getOnNodeAttrEditListener().invoke(world.m_layers.GetNodeAttr(mCurNodeId));
                    return;
                }

                m_nCurPathIndex.clear();
                int[] pathId = GetSelectSinglePath(point, scrRef, world);
                if (pathId[0] >= 0) {

                    m_nCurPathIndex.add(pathId[0]);
                    // 添加修改节点对话框
//                    slamMapView.getOnPathAttrEditListener().invoke(getPathAttr(pathId[0], world), pathId[0]);
                }
            }
            break;
            //路径删除
            case WRD_MULTIDELETE_NODE:
                //拖拽矩形起点
                m_DragRect.difDragStart(pnt);
                m_DragRect.refreshEnd(pnt);
                break;
            case WRD_MOD_KEY:
                //选择路线，用于修改控制点
                int[] pathId = GetSelectSinglePath(point, scrRef, world);
                mCurKeyId = -1;
                m_nCurPathIndex.clear();
                if (pathId[0] >= 0) {
                    mCurKeyId = pathId[1];
                    m_nCurPathIndex.add(pathId[0]);
                }
                // 停止移动控制点
                if (mCurKeyId < 0) mEditWorldStage = WRD_EDIT_NORMAL;

                break;
            //区域连接线
            case WRD_ADD_REG_CON: {
                // 增加区域连接线过程中单击次数
                m_RegConDownCount++;
                if (m_RegConDownCount == 1) m_RegConStart = pnt;

                if (m_RegConDownCount == 2) m_RegConEnd = pnt;

                if (m_RegConDownCount == 2) {
                    regionConnect(world, scrRef);
                    m_RegConDownCount = 0;
                }
            }
            break;
        }
    }

    public PathBaseAttr getPathAttr(int pathId, World world) {
        CLayer layer = world.m_layers;
        if (pathId >= 0) {
            return layer.GetPathAttr(pathId);
        }
        return null;
    }

    //得到当前单击选择的路径
    public int[] GetSelectSinglePath(Point pnt, CoordinateConversion scrRef, World world) {
        return world.m_layers.m_PathBase.PointHitPath(pnt, scrRef, -1, -1);
    }


    public void deleteNode(World world) {

        if (mCurNodeId != -1) {
            world.m_layers.DeleteNode(mCurNodeId);
        }
        mCurNodeId = -1;
        //更新得到选择的任务线
        m_nCurPathIndex.clear();
    }

    //
    // 区域连接功能，根据用户选取两个点，自动生成路线
    // 要求两个点必须和现有节点重合
    public void regionConnect(World world, CoordinateConversion scrRef) {
        Posture startPst = new Posture();
        Posture endPst = new Posture();
        CLayer layer = world.m_layers;

        m_nStartNodeId = -1;
        m_nEndNodeId = -1;

        startPst.x = m_RegConStart.x;
        startPst.y = m_RegConStart.y;
        endPst.x = m_RegConEnd.x;
        endPst.y = m_RegConEnd.y;

        float len = startPst.DistanceTo(endPst);
        if (len < 0.1f) return;
        PointF screenStart = scrRef.worldToScreen(startPst.x, startPst.y);
        // 判断当前关键位姿和现有节点是否重合
        GetSelectNode(new Point((int) screenStart.x, (int) screenStart.y), scrRef, world);
        if (mCurNodeId != -1) {
            m_nStartNodeId = mCurNodeId;
            // 取得对应于nStartNodeId节点的所有姿态角
            Angle[] Angles = new Angle[4];
            int nHeadingAngleCount = layer.GetNodeHeadingAngle(m_nStartNodeId, Angles, 4);
            if (nHeadingAngleCount == 1) {
                startPst.x = layer.m_PathBase.m_MyNode.GetNode(m_nStartNodeId).x;
                startPst.y = layer.m_PathBase.m_MyNode.GetNode(m_nStartNodeId).y;
                startPst.fThita = Angles[0].m_fRad;
            }
        }

        //如果有一个点没有和当前节点重合，则不生成曲线
        if (m_nStartNodeId == -1 && mCurNodeId == -1) {
            return;
        }
        PointF screenEnd = scrRef.worldToScreen(endPst.x, endPst.y);

        GetSelectNode(new Point((int) screenEnd.x, (int) screenEnd.y), scrRef, world);
        if (mCurNodeId != -1) {
            m_nEndNodeId = mCurNodeId;
            // 取得对应于m_nEndNodeId节点的所有姿态角
            Angle[] Angles = new Angle[4];
            int nHeadingAngleCount = layer.GetNodeHeadingAngle(m_nEndNodeId, Angles, 4);
            if (nHeadingAngleCount == 1) {
                endPst.x = layer.m_PathBase.m_MyNode.GetNode(m_nEndNodeId).x;
                endPst.y = layer.m_PathBase.m_MyNode.GetNode(m_nEndNodeId).y;
                if (Angles[0] == null) {
                    endPst.fThita = 0;
                    nHeadingAngleCount = layer.GetNodeHeadingAngle(m_nEndNodeId, Angles, 4);
                    endPst.fThita = 0;
                }
                endPst.fThita = Angles[0].m_fRad;
            }
        }
        mCurNodeId = -1;

        if (m_nStartNodeId == m_nEndNodeId) {
            return;
        }


        //如果是角度相同，有可能是直线
        if (startPst.fThita == endPst.fThita || Math.abs((startPst.fThita - startPst.fThita)) == PI) {
            Line ln = new Line(startPst.GetPoint2dObject(), endPst.GetPoint2dObject());
            //直线
            if (ln.m_angSlant.m_fRad == startPst.fThita || Math.abs((ln.m_angSlant.m_fRad - startPst.fThita)) == PI) {
                layer.AddLinePath(startPst, endPst, m_nStartNodeId, m_nEndNodeId, m_LimVal, m_GuideType);
            } else {
                // 整个曲线已确定
                m_pstStart = startPst;
                m_pstEnd = endPst;
                boolean result = m_Bezier.Create(m_pstStart, m_pstEnd, BEZIER_K);
                if (!result) {
                    return;
                }

                float fDist1 = m_Bezier.m_ptKey[0].DistanceTo(m_Bezier.m_ptKey[1]);
                float fDist2 = m_Bezier.m_ptKey[2].DistanceTo(m_Bezier.m_ptKey[3]);
                boolean isCurvature = layer.AddGenericPath(m_pstStart, m_pstEnd, fDist1, fDist2, m_nStartNodeId, m_nEndNodeId);
                // mListener.showCurvature(isCurvature);
                boolean ret = ((GenericPath) (layer.m_PathBase.m_pPathIdx[layer.m_PathBase.m_uCount - 1].m_ptr)).m_Curve.BezierOptic();
            }
        } else //如果角度不同一定是曲线
        {
            // 整个曲线已确定
            m_pstStart = startPst;
            m_pstEnd = endPst;
            boolean result = m_Bezier.Create(m_pstStart, m_pstEnd, BEZIER_K);
            if (!result) {
                return;
            }

            float fDist1 = m_Bezier.m_ptKey[0].DistanceTo(m_Bezier.m_ptKey[1]);
            float fDist2 = m_Bezier.m_ptKey[2].DistanceTo(m_Bezier.m_ptKey[3]);
            boolean isCurvature = layer.AddGenericPath(m_pstStart, m_pstEnd, fDist1, fDist2, m_nStartNodeId, m_nEndNodeId);
            // mListener.showCurvature(isCurvature);
            boolean ret = ((GenericPath) (layer.m_PathBase.m_pPathIdx[layer.m_PathBase.m_uCount - 1].m_ptr)).m_Curve.BezierOptic();
        }
        m_nStartNodeId = -1;
        m_nEndNodeId = -1;
        m_KeyPst.Clear();
    }


    /**
     * 路径编辑 移动
     */

    public void moveActionFoEditWorld(CoordinateConversion mScrRef, PointF down, World mWorld) {
        PointF worldDown = mScrRef.screenToWorld(down.x, down.y);
        Point2d pnt = new Point2d(worldDown.x, worldDown.y);
        switch (mEditWorldStage) {

            //移动选择的节点
            case WRD_MOD_NODE: { //手动编辑

                if (mModNodeAng) {
                    if (m_ModNodePos.m_PstV == null) return;

                    if (m_ModNodePos.m_PstV.size() != 1) {
                        return;
                    } else   //修改位姿的角度，进而修改路径
                    {
                        mWorld.m_layers.ModifyNodeAng(pnt, mCurNodeId);
                    }
                } else {
                    editNode(mWorld, pnt, -1);  //修改坐标
                }

                if (mCurKeyId > 0) {
                    mWorld.m_layers.DragPath(mCurKeyId - 1, m_nCurPathIndex.get(0), pnt, 0);
                }
            }
            break;
            case WRD_MULTIDELETE_NODE://路径删除
                m_DragRect.refreshEnd(pnt);  //拖拽矩形终点
                getSelectPath(mWorld);
                break;
        }
    }

    //得到块选的路径
    public void getSelectPath(World mWorld) {
        double minx, maxx, miny, maxy;
        Point2d ptS = m_DragRect.getDragStart();
        Point2d ptE = m_DragRect.getDragEnd();
        minx = Math.min(ptS.x, ptE.x);
        maxx = Math.max(ptS.x, ptE.x);
        miny = Math.min(ptS.y, ptE.y);
        maxy = Math.max(ptS.y, ptE.y);
        CLayer layer = mWorld.m_layers;
        for (int i = 0; i < layer.m_PathBase.m_uCount; i++) {
            boolean bInBox = layer.ISInRect(i, minx, miny, maxx, maxy);

            if (bInBox) {
                // 该路径不属于已选路径
                if (!m_nCurPathIndex.contains(i)) {
                    m_nCurPathIndex.add(i);
                }
            } else {
                // 该路径属于已选路径，为了让m_nCurPathIndex中只保留要删除路径的下标
                if (m_nCurPathIndex.contains(i)) {
                    for (int j = 0; j < m_nCurPathIndex.size(); j++) {
                        if (m_nCurPathIndex.get(j) == i) {
                            m_nCurPathIndex.remove(j);
                        }
                    }
                }
            }
        }
    }

    //修改位姿(相当于修改节点,目前只支持修改节点的X和Y坐标)
    //pnt:修改到的目标位置
    public void editNode(World mWorld, Point2d pnt, int TatgetNode) {
        if (mCurNodeId == -1) return;
        CLayer layer = mWorld.m_layers;
        layer.ModifyNodeCoord(pnt, mCurNodeId, TatgetNode);
    }

    private boolean isTeach = false;

    //根据关键位姿创建示教（会直接生成地图）
    public void CreateTeachPath(World mWorld, Point2d[] point2ds, DefPosture m_KeyPst, short pathParam) {

        if (!isTeach) {
            m_nStartNodeId = -1;
            m_nEndNodeId = -1;
        }
        isTeach = true;
        Posture startPst = m_KeyPst.m_PstV.get(0);
        Posture endPst = m_KeyPst.m_PstV.get(3);

        // 整个曲线已确定
        m_pstStart = startPst;
        m_pstEnd = endPst;

        CLayer layer = mWorld.m_layers;
        Point2d[] pptCtrl = new Point2d[2];
        pptCtrl[0] = point2ds[1];
        pptCtrl[1] = point2ds[2];
        layer.AddGenericPath_PPteach(m_pstStart, m_pstEnd, pptCtrl, m_nStartNodeId, m_nEndNodeId, pathParam);
        m_nStartNodeId = layer.m_PathBase.m_pPathIdx[layer.m_PathBase.m_uCount - 1].m_ptr.m_uStartNode;
        m_nEndNodeId = layer.m_PathBase.m_pPathIdx[layer.m_PathBase.m_uCount - 1].m_ptr.m_uEndNode;
        m_nStartNodeId = m_nEndNodeId;
        m_nEndNodeId = -1;
    }

    public void finishTeach() {
        m_nStartNodeId = -1;
        m_nEndNodeId = -1;
        isTeach = false;
    }


    /**
     * 转为直线
     */
    // 将单条选中Bezier转为LinePath
    public void bezierPathToLinePath(World mWorld) {
        mEditWorldStage = WRD_TO_LINE;
        CLayer mLayer = mWorld.m_layers;
        if (m_nCurPathIndex == null) return;
        if (m_nCurPathIndex.size() != 1) return;

        Path pPath = mLayer.m_PathBase.m_pPathIdx[m_nCurPathIndex.get(0)].m_ptr;
        if (pPath.m_uType == 10) {

            Point2d StartPnt = pPath.GetStartPnt();
            Point2d EndPnt = pPath.GetEndPnt();
            int StartNodeId = pPath.m_uStartNode;
            int EndNodeId = pPath.m_uEndNode;
            // 新增直线
            mLayer.AddLinePath(StartPnt, EndPnt, StartNodeId, EndNodeId, m_LimVal, m_GuideType);

            // 记录旧ID,删除曲线
            int oldId = pPath.m_uId;
            mLayer.m_PathBase.RemovePath(m_nCurPathIndex.get(0));
            // 修改新线ID
            mLayer.m_PathBase.m_pPathIdx[mLayer.m_PathBase.m_uCount - 1].m_ptr.m_uId = oldId;
        }
    }


    /**
     * 区域链接
     */
    public void startRegConnect() {
        mEditWorldStage = WRD_ADD_REG_CON;
        m_nCurPathIndex.clear();  //当前选择的路径
        mCurNodeId = -1;      //当前选择的节点
        mCurKeyId = -1;        //选择控制点ID
    }

    /**
     * 多条路径删除
     */
//    public void startMultiDeleteNodes(SLAMMapView slamMapView) {
//        mCurNodeId = -1;      //当前选择的节点
//        mCurKeyId = -1;        //选择控制点ID
//
//        // 区域尺寸有效，才显示对话框
//        if (m_nCurPathIndex != null) {
//            if (!m_nCurPathIndex.isEmpty()) {
//                slamMapView.getOnMultiPathDelete().invoke();
//            }
//        }
//    }

    /***********************************/
    //在进行模式切换的过程中，需要清除一下选择的元素
    public void clearAllSelect() {
        m_KeyPst.Clear();   //清除关键位姿
        m_nCurPathIndex.clear();  //清除选择的线
        mCurNodeId = -1;     //清除选择的点
        mCreateWorldStage = 0;
        mEditWorldStage = 0;
    }

    //块删除路径
    public void delSelectPath(World mWorld) {
        m_lock.lock();
        try {
            //删除路径
            CLayer layer = mWorld.m_layers;
            layer.DelPath(m_nCurPathIndex);
            m_nCurPathIndex.clear();
        } finally {
            m_lock.unlock();
        }

    }


    /**
     * 路径编辑  手指抬起
     *
     * @param upPoint
     * @param mScrRef
     * @param mWorld
     */
    public void upActionFoEditWorld(Point upPoint, CoordinateConversion mScrRef, World mWorld) {
        switch (mEditWorldStage) {
            case WRD_MOD_NODE:
                // 判断当前的位置是否和现有节点重合
                int CurNodeId = PointHitNodeTest(upPoint, mScrRef, mWorld);
                int curKeyId = mCurKeyId;
                // 移动节点时候才显示对话框
                if (CurNodeId != -1 && !mModNodeAng && curKeyId == -1) {
                    PointF pntT = mScrRef.screenToWorld((float) upPoint.x, (float) upPoint.y);
//                    Objects.requireNonNull(sLAMMapView.getNodeMergeListener()).invoke(pntT, CurNodeId);
                }

                break;
            case WRD_MULTIDELETE_NODE: { //路径删除
                // 区域尺寸有效，才显示对话框
                if (m_DragRect.reasonable()) {
//                    Objects.requireNonNull(sLAMMapView.getOnMultiPathDelete()).invoke();
                }
                // 手动编辑模式
                startModifyNode();
                break;
            }
            case WRD_TO_LINE://转为直线
                break;
        }
    }

    //
    // 判断当前位置是否与某节点重合
    //mCurNodeId: 不考虑的节点，在移动节点，节点合并的时候需要用
    public int PointHitNodeTest(Point pnt, CoordinateConversion mScrRef, World mWorld) {
        // 判断当前的位置是否和现有节点重合
        return mWorld.m_layers.PointHitNodeTest(pnt, mScrRef, mCurNodeId);
    }

//    public int createPathByLine(int id, Line line, World mWorld) {
//        int endNodeId = mWorld.m_layers.CreatePathByLine(id, line);
////        LogUtil.INSTANCE.d("WorldEdit createPathByLine endNodeId = " + endNodeId);
//        return endNodeId;
//    }

}


