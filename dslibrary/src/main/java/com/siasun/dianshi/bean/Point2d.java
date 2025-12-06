package com.siasun.dianshi.bean;

//import java.awt.BasicStroke;
//import android.graphics.Canvas;
//import android.graphics.Point;//import android.graphics.Color;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.annotation.JSONType;
import com.google.gson.annotations.SerializedName;
import com.siasun.dianshi.utils.CoordinateConversion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;


@JSONType(orders = {"X", "Y"})
public class Point2d implements Serializable {

    @JSONField(serialize = false)
    public int id;
    @SerializedName("X")
    @JSONField(name = "X")
    public float x;
    @SerializedName("Y")
    @JSONField(name = "Y")
    public float y;

    @JSONField(serialize = false)
    public float a;
    @JSONField(serialize = false)
    public float r;


    // The constructor
    public Point2d(float fx, float fy) {
        int _id = 0;
        x = fx;
        y = fy;
        id = _id;
//        Point2d(fx, fy, _id);
    }

//    public void Point2d(float fx, float fy, int _id) {
//        id = _id;
//    }

    // Default constructor
    public Point2d() {
        x = 0;
        y = 0;
        id = 0;
    }

    public Point2d GetPoint2dObject() {
        return this;
    }


//    public void Set(float _x, float _y) {
//        x = _x;
//        y = _y;
//    }


//    public void SetPolar(float fAngle, float fRadius, int _id) {
//        id = _id;
//        r = fRadius;
//        a = fAngle;
//        x = (float) (fRadius * Math.cos(fAngle));
//        y = (float) (fRadius * Math.sin(fAngle));
//    }


//    public Point2d(final Point point) {
//        x = (float) point.x;
//        y = (float) point.y;
//        id = 0;
//    }


//    public void Move(float dx, float dy) {
//        x += dx;
//        y += dy;
//    }

//
//    public void Rotate(float fAng, float fCx, float fCy) {
//        float fX = (float) ((x - fCx) * Math.cos(fAng) - (y - fCy) * Math.sin(fAng) + fCx);
//        float fY = (float) ((x - fCx) * Math.sin(fAng) + (y - fCy) * Math.cos(fAng) + fCy);
//
//        x = fX;
//        y = fY;
//    }

//
//    /**
//     * 旋转
//     *
//     * @param fAng     弧度
//     * @param ptCenter 旋转中心
//     */
//    public void Rotate(float fAng, final Point2d ptCenter) {
//        float fX = (float) ((x - ptCenter.x) * Math.cos(fAng) - (y - ptCenter.y) * Math.sin(fAng) + ptCenter.x);
//        float fY = (float) ((x - ptCenter.x) * Math.sin(fAng) + (y - ptCenter.y) * Math.cos(fAng) + ptCenter.y);
//
//        x = fX;
//        y = fY;
//    }


//	public bool operator == (const CPoint2d& pt) const
//{
//	return (x == pt.x && y == pt.y);
//}
//

//	public bool  operator != (const CPoint2d& pt) const
//{
//	return (x != pt.x || y != pt.y);
//}
//

//void CPoint2d::operator += (const CPoint2d& pt)
//{
//	x += pt.x;
//	y += pt.y;
//}
//

//CPoint2d CPoint2d::operator +(const CPoint2d& pt) const
//{
//	CPoint2d ptNew(x, y);
//	ptNew += pt;
//	return ptNew;
//}
//

//void CPoint2d::operator -= (const CPoint2d& pt)
//{
//	x -= pt.x;
//	y -= pt.y;
//}
//

//CPoint2d CPoint2d::operator -(const CPoint2d& pt) const
//{
//	CPoint2d ptNew(x, y);
//	ptNew -= pt;
//
//	return ptNew;
//}


    public float DistanceTo(final Point2d pt) {
        return (float) Math.hypot(x - pt.x, y - pt.y);
    }


//    public float Distance2To(final Point2d pt2) {
//        float d_x = x - pt2.x;
//        float d_y = y - pt2.y;
//
//        return d_x * d_x + d_y * d_y;
//    }


//    public boolean IsEqualTo(final Point2d pt2, float limit) {
//        if (Distance2To(pt2) < (limit * limit))
//            return true;
//        else
//            return false;
//    }


//    public void UpdatePolar() {
//
//        r = (float) Math.sqrt(y * y + x * x);
//
//
//        if (r < 1E-7)
//            a = 0;
//        else {
//            float fAngle = (float) Math.atan2(y, x);
//            Angle ang = new Angle(fAngle);
//            a = ang.NormAngle(fAngle);
//        }
//    }


//    public void UpdateCartisian() {
//        x = (float) (r * Math.cos(a));
//        y = (float) (r * Math.sin(a));
//    }


//    public float AngleDistanceTo(final Point2d pt) {
//        Angle ang = new Angle(a - pt.a);
//        return (float) Math.abs(ang.NormAngle(a - pt.a));
//    }


//public  boolean Load(FILE* fp)
//{
//	if (fscanf(fp, "%d\t%f\t%f\t%f\t%f\n", &id, &x, &y, &r, &a) != 5)
//		return false;
//
//	return true;
//}
//

//public  boolean Save(FILE* fp)
//{
//	fprintf(fp, "%d\t%f\t%f\t%f\t%f\n", id, x, y, r, a);
//	return true;
//}


//public  void operator = (const CPoint& point)
//{
//	x = (float)point.x;
//	y = (float)point.y;
//}
//

//public  boolean operator == (const CPoint& point)
//{
//	return ((x == (float)point.x) && (y == (float)point.y));
//}


//public  boolean operator != (const CPoint& point)
//{
//	return (x != (float)point.x || y != (float)point.y);
//}

    //public void Dump()
//{
//	TRACE(_T("%d\t%f\t%f\t%f\t%f\n"), id, x, y, r, a);
//}
    public void Create(DataInputStream dis) {
//	    File file = new File(strFile);
        //   try {
//	        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
//       DataInputStream dis = new DataInputStream(bis);
        try {
            int ch1 = dis.read();
            int ch2 = dis.read();
            int ch3 = dis.read();
            int ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0)
                throw new EOFException();
            int tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.id = tempI;

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0)
                throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.x = Float.intBitsToFloat(tempI);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0)
                throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.y = Float.intBitsToFloat(tempI);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0)
                throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.a = Float.intBitsToFloat(tempI);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0)
                throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.r = Float.intBitsToFloat(tempI);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void Save(DataOutputStream dis) {
//	    File file = new File(strFile);
        //   try {
//	        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
//       DataInputStream dis = new DataInputStream(bis);
        try {
            int ch1 = this.id;
            int ch2 = this.id;
            int ch3 = this.id;
            int ch4 = this.id;
            ch1 = ch1 >> 24; //低8位
            ch2 = (ch2 & 0xff0000) >> 8; //低16位
            ch3 = (ch3 & 0xff00) << 8;
            ch4 = (ch4 & 0xff) << 24;
            int T = (ch1 + ch2 + ch3 + ch4);
            dis.writeInt(T);

            int Ix = Float.floatToIntBits(this.x);
            ch1 = Ix;
            ch2 = Ix;
            ch3 = Ix;
            ch4 = Ix;
            ch1 = ch1 >> 24; //低8位
            ch2 = (ch2 & 0xff0000) >> 8; //低16位
            ch3 = (ch3 & 0xff00) << 8;
            ch4 = (ch4 & 0xff) << 24;
            T = (ch1 + ch2 + ch3 + ch4);
            float fsT = Float.intBitsToFloat(T);
            dis.writeFloat(fsT);

            Ix = Float.floatToIntBits(this.y);
            ch1 = Ix;
            ch2 = Ix;
            ch3 = Ix;
            ch4 = Ix;
            ch1 = ch1 >> 24; //低8位
            ch2 = (ch2 & 0xff0000) >> 8; //低16位
            ch3 = (ch3 & 0xff00) << 8;
            ch4 = (ch4 & 0xff) << 24;
            T = (ch1 + ch2 + ch3 + ch4);
            fsT = Float.intBitsToFloat(T);
            dis.writeFloat(fsT);

            Ix = Float.floatToIntBits(this.a);
            ch1 = Ix;
            ch2 = Ix;
            ch3 = Ix;
            ch4 = Ix;
            ch1 = ch1 >> 24; //低8位
            ch2 = (ch2 & 0xff0000) >> 8; //低16位
            ch3 = (ch3 & 0xff00) << 8;
            ch4 = (ch4 & 0xff) << 24;
            T = (ch1 + ch2 + ch3 + ch4);
            fsT = Float.intBitsToFloat(T);
            dis.writeFloat(fsT);

            Ix = Float.floatToIntBits(this.r);
            ch1 = Ix;
            ch2 = Ix;
            ch3 = Ix;
            ch4 = Ix;
            ch1 = ch1 >> 24; //低8位
            ch2 = (ch2 & 0xff0000) >> 8; //低16位
            ch3 = (ch3 & 0xff00) << 8;
            ch4 = (ch4 & 0xff) << 24;
            T = (ch1 + ch2 + ch3 + ch4);
            fsT = Float.intBitsToFloat(T);
            dis.writeFloat(fsT);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Draw(CoordinateConversion ScrnRef, Canvas canvas, int color, int nPointSize) {

        if (nPointSize < 0) {
            nPointSize = (-1) * nPointSize;
        }


        PointF pnt1 = ScrnRef.worldToScreen(x, y);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStyle(Paint.Style.FILL);

        RectF ret = new RectF(pnt1.x - nPointSize, pnt1.y - nPointSize, pnt1.x + nPointSize, pnt1.y + nPointSize);
        canvas.drawOval(ret, paint);

    }

//    public void DrawCircle(ScreenReference ScrnRef, Canvas canvas, float nPointSize, int color) {
//        Point2d pt = new Point2d(x, y);
//        Point pnt1 = ScrnRef.GetWindowPoint(pt);
//
//        Paint paint = new Paint();
//        paint.setAntiAlias(true);
//        paint.setColor(color);
//        paint.setStyle(Paint.Style.FILL);
//
//        canvas.drawCircle(pnt1.x, pnt1.y, nPointSize, paint);
//    }


    @Override
    public String toString() {
        return "Point2d{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
