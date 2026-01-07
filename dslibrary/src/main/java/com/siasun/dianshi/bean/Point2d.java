package com.siasun.dianshi.bean;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.Log;

import com.siasun.dianshi.utils.CoordinateConversion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;


public class Point2d implements Serializable {

    public int id;
    public float x;
    public float y;

    public float a;
    public float r;


    // The constructor
    public Point2d(float fx, float fy) {
        x = fx;
        y = fy;
        id = 0;
    }


    // Default constructor
    public Point2d() {
        x = 0;
        y = 0;
        id = 0;
    }

    public Point2d GetPoint2dObject() {
        return this;
    }


    public float DistanceTo(final Point2d pt) {
        return (float) Math.hypot(x - pt.x, y - pt.y);
    }


    /**
     * 读取
     *
     * @param dis
     */
    public void read(DataInputStream dis) {
        try {
            int ch1 = dis.read();
            int ch2 = dis.read();
            int ch3 = dis.read();
            int ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            int tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.id = tempI;
//            Log.d("readWorld", "Point2d id  " + id);
            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.x = Float.intBitsToFloat(tempI);
//            Log.d("readWorld", "Point2d x  " + x);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.y = Float.intBitsToFloat(tempI);
//            Log.d("readWorld", "Point2d y  " + y);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.a = Float.intBitsToFloat(tempI);
//            Log.d("readWorld", "Point2d a  " + a);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.r = Float.intBitsToFloat(tempI);
//            Log.d("readWorld", "Point2d r  " + r);

        } catch (IOException e) {
            Log.e("readWorld", "读取Point2d异常 r  " + e);
            e.printStackTrace();
        }
    }


    public void Save(DataOutputStream dis) {
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

        } catch (Exception e) {
            Log.e("readWorld", "保存Point2d异常 r  " + e);
            e.printStackTrace();
        }
    }

    /**
     * 绘制控制器点
     *
     * @param ScrnRef
     * @param canvas
     * @param color
     * @param nPointSize
     * @param paint
     */
    public void Draw(CoordinateConversion ScrnRef, Canvas canvas, int color, int nPointSize, Paint paint) {
        PointF pnt1 = ScrnRef.worldToScreen(x, y);
        paint.setColor(color);
        canvas.drawCircle(pnt1.x, pnt1.y, nPointSize, paint);
    }

    @Override
    public String toString() {
        return "Point2d{" + "x=" + x + ", y=" + y + '}';
    }
}
