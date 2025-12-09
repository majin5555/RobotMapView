package com.siasun.dianshi.bean.world;


import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;

import com.siasun.dianshi.bean.Point2d;
import com.siasun.dianshi.bean.pp.Angle;
import com.siasun.dianshi.bean.pp.Line;
import com.siasun.dianshi.utils.CoordinateConversion;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;


public class SidePath extends Path {
    Angle m_angHeading;

    public SidePath() {

    }

    float SizeFun() {
        Point2d ptStart = new Point2d();
        Point2d ptEnd = new Point2d();
        ptStart = GetStartPnt();
        ptEnd = GetEndPnt();
        Line ln = new Line(ptStart, ptEnd);
        return ln.Length();
    }

    //
//   GetHeading: Get the vehicle's heading angle at the specified node.
//
    public Angle GetHeading(Node nd) {
        // The vehicle's heading is the same everywhere on the path
        return m_angHeading;
    }

    @Override
    int PointHitTest(Point pnt, CoordinateConversion ScrnRef) {
        return 0;
    }

    public boolean Create(DataInputStream dis) {
        float fHeading;
//		unsigned int uObstacle = 0xFFFF;
//		USHORT uDir;
        short uFwdRotoScannerObstacle = (short) 65535;
        short uFwdObdetectorObstacle = (short) 65535;
        short uBwdRotoScannerObstacle = (short) 65535;
        short uBwdObdetectorObstacle = (short) 65535;
        float fDir = 0.0f;

        if (!super.Create(dis))
            return false;
        try {
            int ch1 = dis.read();
            int ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            uFwdRotoScannerObstacle = (short) ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            uFwdObdetectorObstacle = (short) ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            uBwdRotoScannerObstacle = (short) ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            uBwdObdetectorObstacle = (short) ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            int ch3 = dis.read();
            int ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0)
                throw new EOFException();
            int tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            fDir = Float.intBitsToFloat(tempI);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        m_uFwdRotoScannerObstacle = uFwdRotoScannerObstacle;
        m_uFwdObdetectorObstacle = uFwdObdetectorObstacle;
        m_uBwdRotoScannerObstacle = uBwdRotoScannerObstacle;
        m_uBwdObdetectorObstacle = uBwdObdetectorObstacle;


        fHeading = fDir;
        m_angHeading = new Angle(fHeading);
        m_fSize = SizeFun();
        return true;
    }

    @Override
    public boolean ISInRect(double minx, double miny, double maxx, double maxy) {
        return false;
    }

    public void Draw(CoordinateConversion ScrnRef, Canvas Grp, int color, int nWidth) {
        Point2d start = GetStartPnt();
        PointF pnt1 = ScrnRef.worldToScreen(start.x, start.y);
        Point2d end = GetEndPnt();
        PointF pnt2 = ScrnRef.worldToScreen(end.x, end.y);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        /*����paint����ɫ*/
        paint.setColor(color);
        /*����paint�� style ΪSTROKE������*/
        paint.setStyle(Paint.Style.STROKE);
        /*����paint�������*/
        paint.setStrokeWidth(nWidth);
        Grp.drawLine(pnt1.x, pnt1.y, pnt2.x, pnt2.y, paint);
    }

    @Override
    public void DrawID(CoordinateConversion ScrnRef, Canvas Grp) {

    }


}
