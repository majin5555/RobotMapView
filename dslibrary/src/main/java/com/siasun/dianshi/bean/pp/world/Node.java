package com.siasun.dianshi.bean.pp.world;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.Log;

import com.siasun.dianshi.bean.Point2d;
import com.siasun.dianshi.bean.TranBytes;
import com.siasun.dianshi.utils.CoordinateConversion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;

//   "Node"节点类.
public class Node extends Point2d {

    public int m_uId;             // 节点ID
    public short m_uType;         // 节点类型
    public short m_uExtType;      // 节点扩展类型
    public short m_uExtType2;     // 节点扩展类型2
    public float m_fHeading;      // 节点方向（航向角）
    public RfId m_Tag;            // 关联标签信息
    public float m_fChkMarkDist;  // 检测标记距离
    public float m_fChkMarkVel;   // 检测标记速度
    public float m_fMarkWidth;    // 标记有效宽度

    public float m_fOffset1;      // 偏移量1
    public float m_fOffset2;      // 偏移量2

    public float m_fFwdMarkOffset;// 前进标记偏移
    public float m_fBwdMarkOffset;// 后退标记偏移

    public short m_uLayerID;      // 图层ID
    public short m_uStationType;  // 站类型 临时0 stop1 charg2 act3
    public short m_uStationTempId;// 临时站ID
    public short m_uStationId;    // 站ID

    public short m_uCarrierType = 255;  // 车辆类型，默认：255
    public short m_uOnLine = 1;         // 允许上线，1：允许上线，0：不允许；默认：1


    private final int offset = 20; // 偏移量常量


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

    }


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

    /**
     * 节点读取
     *
     * @param dis
     */
    public void read(DataInputStream dis) {
        try {
            int ch1 = dis.read();
            int ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uId = ((ch2 << 8) + (ch1 << 0));
            Log.w("readWorld", "节点ID  m_uId " + this.m_uId);


            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uType = (short) ((ch2 << 8) + (ch1 << 0));
            Log.d("readWorld", "节点类型  m_uType " + this.m_uType);

            ch1 = dis.read();
            ch2 = dis.read();
            int ch3 = dis.read();
            int ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            int tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.x = Float.intBitsToFloat(tempI);
            Log.d("readWorld", "this.x  m_uType " + this.x);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.y = Float.intBitsToFloat(tempI);
            Log.d("readWorld", "this.y  m_uType " + this.y);

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uExtType = (short) ((ch2 << 8) + (ch1 << 0));
//            Log.d("readWorld", "节点扩展类型  m_uExtType " + this.m_uExtType);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.m_fHeading = Float.intBitsToFloat(tempI);
//            Log.d("readWorld", "节点方向（航向角） m_fHeading " + this.m_fHeading);

            this.m_Tag.read(dis);
//            Log.d("readWorld", "关联标签信息 m_Tag " + this.m_Tag);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.m_fChkMarkDist = Float.intBitsToFloat(tempI);
//            Log.d("readWorld", "检测标记距离 m_fChkMarkDist " + this.m_fChkMarkDist);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.m_fChkMarkVel = Float.intBitsToFloat(tempI);
//            Log.d("readWorld", "检测标记速度 m_fChkMarkVel " + this.m_fChkMarkVel);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.m_fMarkWidth = Float.intBitsToFloat(tempI);
//            Log.d("readWorld", "标记有效宽度 m_fMarkWidth " + this.m_fMarkWidth);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.m_fOffset1 = Float.intBitsToFloat(tempI);
//            Log.d("readWorld", "m_fOffset1 m_fOffset1 " + this.m_fOffset1);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.m_fOffset2 = Float.intBitsToFloat(tempI);
//            Log.d("readWorld", "偏移量2 m_fOffset2 " + this.m_fOffset2);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.m_fFwdMarkOffset = Float.intBitsToFloat(tempI);
//            Log.d("readWorld", "前进标记偏移 m_fFwdMarkOffset " + this.m_fFwdMarkOffset);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
            this.m_fBwdMarkOffset = Float.intBitsToFloat(tempI);
//            Log.d("readWorld", "后退标记偏移 m_fBwdMarkOffset " + this.m_fBwdMarkOffset);

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uLayerID = (short) ((ch2 << 8) + (ch1 << 0));

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uStationType = (short) ((ch2 << 8) + (ch1 << 0));
//            Log.d("readWorld", "站类型 临时0 stop1 charg2 act3 ---->m_uStationType " + this.m_uStationType);

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uStationTempId = (short) ((ch2 << 8) + (ch1 << 0));
//            Log.d("readWorld", "临时站ID m_uStationTempId " + this.m_uStationTempId);

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uStationId = (short) ((ch2 << 8) + (ch1 << 0));
//            Log.d("readWorld", "站ID m_uStationId " + this.m_uStationId);

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            m_uCarrierType = (short) ((ch2 << 8) + (ch1 << 0));
//            Log.d("readWorld", "车辆类型，默认：255 " + this.m_uCarrierType);

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            m_uOnLine = (short) ((ch2 << 8) + (ch1 << 0));
//            Log.d("readWorld", "允许上线，1：允许上线，0：不允许；默认：1  -->m_uOnLine " + this.m_uOnLine);

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
            m_Tag.save(dis);

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

    /**
     * 绘制节点
     *
     * @param conversion 坐标转换工具
     * @param canvas     画布
     * @param mPaint     画笔
     */
    /**
     * 绘制节点及其编号
     *
     * @param conversion 坐标转换工具
     * @param canvas     画布
     * @param mPaint     画笔
     * @param type       1 开始节点 2结束节点
     */
    public void Draw(CoordinateConversion conversion, Canvas canvas, Paint mPaint, Integer type) {
        try {
            PointF screenPoint = conversion.worldToScreen(GetPoint2dObject().x, GetPoint2dObject().y);
            int canvasWidth = canvas.getWidth();
            int canvasHeight = canvas.getHeight();

            // 检查节点是否在屏幕范围内
            if ((screenPoint.x < 0 || screenPoint.x > canvasWidth) && (screenPoint.y < 0 || screenPoint.y > canvasHeight))
                return;

            if (type == 1) {
                // 绘制节点（点）
                mPaint.setColor(Color.BLUE);
            } else {
                // 绘制节点（点）
                mPaint.setColor(Color.RED);
            }
            mPaint.setStyle(Paint.Style.FILL);

            if (type == 1) {
                canvas.drawCircle(screenPoint.x, screenPoint.y, 5, mPaint);
            } else {
                canvas.drawCircle(screenPoint.x, screenPoint.y + 10, 5, mPaint);
            }

            // 绘制节点编号
            mPaint.setTextSize(12f);
            if (type == 1) {
                canvas.drawText(String.valueOf(m_uId), screenPoint.x, screenPoint.y, mPaint);
            } else {
                canvas.drawText(String.valueOf(m_uId), screenPoint.x, screenPoint.y + 10, mPaint);
            }


        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    /**
     * 重写父类Point2d的Draw方法，添加节点编号绘制功能
     */
    @Override
    public void Draw(CoordinateConversion ScrnRef, Canvas canvas, int color, int nPointSize, Paint paint) {
        // 调用父类方法绘制节点
        super.Draw(ScrnRef, canvas, color, nPointSize, paint);
        
        try {
            PointF screenPoint = ScrnRef.worldToScreen(GetPoint2dObject().x, GetPoint2dObject().y);
            
            // 保存当前画笔状态
            Paint.Style originalStyle = paint.getStyle();
            float originalTextSize = paint.getTextSize();
            int originalColor = paint.getColor();
            
            // 设置文本绘制参数
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(14f);
            paint.setColor(Color.BLACK);
            
            // 绘制节点编号，偏移量以避免覆盖节点
            canvas.drawText(String.valueOf(m_uId), screenPoint.x + nPointSize + 2, screenPoint.y + nPointSize + 2, paint);
            
            // 恢复画笔状态
            paint.setStyle(originalStyle);
            paint.setTextSize(originalTextSize);
            paint.setColor(originalColor);
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

//    /**
//     * 兼容旧代码的DrawID方法，内部调用Draw方法
//     */
//    public void DrawID(CoordinateConversion conversion, Canvas canvas, Paint mPaint) {
//        Draw(conversion, canvas, mPaint);
//    }
}
