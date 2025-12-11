package com.siasun.dianshi.bean.pp.world;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;

import com.siasun.dianshi.bean.Point2d;
import com.siasun.dianshi.bean.TranBytes;
import com.siasun.dianshi.bean.pp.Angle;
import com.siasun.dianshi.bean.pp.Line;
import com.siasun.dianshi.utils.CoordinateConversion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;


public class LinePath extends Path {


    protected Angle m_angHeading;     // The vehicle's heading at the start/end nodes

    private static final int offset = 20;

    public void SwabWord(short uWord) {
    }

    public LinePath() {
    }

    public LinePath(int uId, int uStartNode, int uEndNode, float[] fVeloLimit, short nGuideType, short uObstacle, short uDir, short uExtType, NodeBase nodeBase) {
        short type = 0;
        //	super.Create((short)uId,(short)nStartNode,(short)nEndNode, fVeloLimit,type,nGuideType,0,uExtType);
        super.Create(uId, uStartNode, uEndNode, fVeloLimit, type, nGuideType, 0, uExtType, nodeBase);
        m_uObstacle = uObstacle;

        //	if (uDir == NEGATIVE_HEADING)
        if (uDir == 1) {
            int uTemp = m_uStartNode;
            m_uStartNode = m_uEndNode;
            m_uEndNode = uTemp;
            SwabWord(m_uObstacle);
        }

        Setup();
    }


    public void Setup() {
        // Do initializations
        // Get the path's start/end points
        Point2d ptStart = new Point2d();
        Point2d ptEnd = new Point2d();
        ptStart = GetStartPnt();
        ptEnd = GetEndPnt();

        // Construct a line
        Line ln = new Line(ptStart, ptEnd);

        // The start and end heading angle are the same
        m_angHeading = ln.GetSlantAngle();
        m_fSize = ln.Length();
    }


    @Override
    public Angle GetHeading(Node node) {
        return m_angHeading;
    }

    @Override
    public boolean ISInRect(double minx, double miny, double maxx, double maxy) {
        Point2d ptStart = GetStartPnt();
        Point2d ptEnd = GetEndPnt();
        double dleft, dright, dtop, dbottom;
        dleft = Math.min(ptStart.x, ptEnd.x);

        dright = Math.max(ptStart.x, ptEnd.x);

        dtop = Math.max(ptStart.y, ptEnd.y);

        dbottom = Math.min(ptStart.y, ptEnd.y);

        if (dleft > minx && dright < maxx && dtop < maxy && dbottom > miny) {
            return true;
        } else return false;
    }
//
//Make a trajectory from the path.
//
//CTraj* MakeTraj()
//{
//CLineTraj* pLineTraj = new CLineTraj;
//
//Point2d& ptStart = GetStartPnt();
//Point2d& ptEnd = GetEndPnt();
//pLineTraj->CreateTraj(ptStart, ptEnd, FORWARD);
//return pLineTraj;
//}	

///////////////20191014


//BOOL GetSweepingRegion(CVehicleContour &Vehicle, CSweepingArea &SweepingRegion)
//{
//double dContour = (double)sqrt((Vehicle.m_rgn0.m_pVertex[0].x-Vehicle.m_rgn0.m_pVertex[1].x)*(Vehicle.m_rgn0.m_pVertex[0].x-Vehicle.m_rgn0.m_pVertex[1].x)+(Vehicle.m_rgn0.m_pVertex[0].y-Vehicle.m_rgn0.m_pVertex[1].y)*(Vehicle.m_rgn0.m_pVertex[0].y-Vehicle.m_rgn0.m_pVertex[1].y));
//double fLen = m_fSize;
//
//int nCount = (int)(fLen / dContour);
//SweepingRegion.SetSize(nCount+2);   //������������������Ŀ
////CTraj* pTraj = pPath->MakeTraj();
//CLineTraj* pTraj = (CLineTraj*)MakeTraj();//�����켣
//
//for (int i = 0; i <= nCount+1; i++)
//{
//pTraj->SetProgress(0.0f, (float)i*dContour);//���ý��ȱ���
//CPosture pst = pTraj->PostureFun();
//
//Vehicle.SetPosture(pst);
//SweepingRegion.AddContour(Vehicle);
//}
//delete pTraj;
//pTraj = NULL;
//return TRUE;
//}
//BOOL GetSweepingStartRegion(CVehicleContour &Vehicle, CSweepingArea &SweepingRegion)
//{
//double fLen = m_fSize;
//int nCount = 1/*(int)(fLen / 0.05f)*/;
//SweepingRegion.SetSize(nCount);
//
//CLineTraj* pTraj = (CLineTraj*)MakeTraj();
//
//for (int i = 0; i < nCount; i++)
//{
//pTraj->SetProgress(0.0f, (float)0);
//CPosture pst = pTraj->PostureFun();
//
//Vehicle.SetPosture(pst);
//SweepingRegion.AddContour(Vehicle);
//}
//
//delete pTraj;
//pTraj = NULL;
//return TRUE;
//}
//
//

    //return true;
//}
//BOOL ISInRect(double minx, double miny, double maxx, double maxy)
//{
//Point2d& ptStart = GetStartPnt();
//Point2d& ptEnd = GetEndPnt();
//double dleft, dright, dtop, dbottom;
//dleft = min(ptStart.x, ptEnd.x);
//
//dright = max(ptStart.x, ptEnd.x);
//
//dtop = max(ptStart.y, ptEnd.y);
//
//dbottom = min(ptStart.y, ptEnd.y);
//
//if (dleft>minx && dright<maxx && dtop<maxy && dbottom>miny)
//{
//return TRUE;
//}
//else
//return FALSE;
//}
//
//   判断给定的屏幕点是否落在路径上。
//   返回值：
//     -1:  未落在路径上
//     0:   落在路径上
//
    @Override
    int PointHitTest(Point pnt, CoordinateConversion ScrnRef) {
        // 取得对应的世界点坐标
//        Point2d pt = ScrnRef.GetWorldPoint(pnt);
        PointF pt = ScrnRef.screenToWorld((float) pnt.x, (float) pnt.y);
        Point2d ptStart = GetStartPnt();
        Point2d ptEnd = GetEndPnt();

        // 构造直线
        Line ln = new Line(ptStart, ptEnd);

        //float fLambda = 0.0F;
        Point2d fLambda = new Point2d();
        Point2d ptFoot = new Point2d();

        // 计算点到此直线的最短距离
        float fDist = ln.DistanceToPoint(false, new Point2d(pt.x, pt.y), fLambda, ptFoot);
        if (fLambda.x >= 0 && fLambda.x <= 1) {
            // 换算到屏幕窗口距离
            int nDist = (int) (fDist * ScrnRef.scale);

            // 如果屏幕窗口距离小于3，认为鼠标触碰到路径
            if (nDist <= offset) return 0;               // 落入
        }
        return -1;          // 落出
    }

    @Override
    public boolean Create(DataInputStream dis) {
        short uDir = 0;            // Positive input/negative input

        // Init the fields in the base class
//	if (!Path.Create(dis))
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        m_uPathHeading = uDir;
//	if (uDir == NEGATIVE_HEADING)
        if (uDir == 1) {
            int uTemp = m_uStartNode;
            m_uStartNode = m_uEndNode;
            m_uEndNode = uTemp;

            SwabWord(m_uObstacle);
        }

        // Find the heading angles at the 2 nodes
        Setup();

        return true;
    }

//	@Override
//	public boolean Create(DataInputStream dis) {
//		short uDir = 0;            // Positive input/negative input
//
//		// Init the fields in the base class
////	if (!Path.Create(dis))
//		if (!super.Create(dis))
//			return false;
//		try {
//			int ch1 = dis.read();
//			int ch2 = dis.read();
//			if ((ch1 | ch2) < 0)
//				throw new EOFException();
//			m_uPathHeading = (short) ((ch2 << 8) + (ch1 << 0));
//
////            ch1 = dis.read();
////            ch2 = dis.read();
////            if ((ch1 | ch2) < 0)
////                throw new EOFException();
////            m_uMoveHeading = (short) ((ch2 << 8) + (ch1 << 0));
//
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
////	if (uDir == NEGATIVE_HEADING)
//		if (m_uPathHeading == 1) {
//			short uTemp = m_uStartNode;
//			m_uStartNode = m_uEndNode;
//			m_uEndNode = uTemp;
//
//			SwabWord(m_uObstacle);
//		}
//
//		// Find the heading angles at the 2 nodes
//		Setup();
//
//		return true;
//	}

    @Override
    public boolean Save(DataOutputStream dis) {
        short uDir = 0;            // Positive input/negative input

        // Init the fields in the base class
//	if (!Path.Create(dis))
        if (!super.Save(dis)) return false;
        try {
            TranBytes tan = new TranBytes();
            int ch1;
            int ch2;
            tan.writeShort(dis, this.m_uFwdRotoScannerObstacle);
            tan.writeShort(dis, this.m_uFwdObdetectorObstacle);
            tan.writeShort(dis, this.m_uBwdRotoScannerObstacle);
            tan.writeShort(dis, this.m_uBwdObdetectorObstacle);

            ch1 = this.m_uPathHeading;
            ch2 = this.m_uPathHeading;
            short sT = (short) ((ch2 >> 8) + (ch1 & 0xff) << 8);
            dis.writeShort(sT);
//			dis.writeShort(this.m_uPathHeading);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void Draw(CoordinateConversion ScrnRef, Canvas Grp, Paint paint) {
        Point2d start = GetStartPnt();
        PointF pnt1 = ScrnRef.worldToScreen(start.x, start.y);
        Point2d end = GetEndPnt();
        PointF pnt2 = ScrnRef.worldToScreen(end.x, end.y);

        int width = Grp.getWidth();
        int Height = Grp.getHeight();
        if ((pnt1.x < 0 || pnt1.x > width) && (pnt1.y < 0 || pnt1.y > Height) && (pnt2.x < 0 || pnt2.x > width) && (pnt2.y < 0 || pnt2.y > Height)) {
            return;
        }

        Grp.drawLine(pnt1.x, pnt1.y, pnt2.x, pnt2.y, paint);
        if (m_uExtType == 7) {
            paint.setColor(Color.RED);

            Grp.drawLine(pnt1.x, pnt1.y, pnt2.x, pnt2.y, paint);
        }
    }

    @Override
    public void DrawID(CoordinateConversion scrnRef, Canvas Grp, Paint paint) {
        Point pnt1 = new Point();

        PointF start = scrnRef.worldToScreen(GetStartNode().GetPoint2dObject().x, GetStartNode().GetPoint2dObject().y);
        PointF end = scrnRef.worldToScreen(GetEndNode().GetPoint2dObject().x, GetEndNode().GetPoint2dObject().y);
        pnt1.x = (int) (start.x + end.x);
        pnt1.y = (int) (start.y + end.y);

        pnt1.x = pnt1.x / 2;
        pnt1.y = pnt1.y / 2;

        String str = String.valueOf(m_uId);
        paint.setColor(Color.BLUE);
        Grp.drawText(str, pnt1.x + 4 * scrnRef.scale, pnt1.y + 4 * scrnRef.scale, paint);
    }


}
