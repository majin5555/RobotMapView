package com.siasun.dianshi.bean.pp.world;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;

import com.siasun.dianshi.bean.Point2d;
import com.siasun.dianshi.bean.TranBytes;
import com.siasun.dianshi.bean.pp.Angle;
import com.siasun.dianshi.utils.CoordinateConversion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * Path类：路径抽象基类，定义了路径的基本属性和行为
 * 所有具体路径类型（如直线、曲线等）都继承自此类
 */
public abstract class Path {
    /**
     * 航向规则枚举：定义路径的航向方向
     * POSITIVE_HEADING：正向航向
     * NEGATIVE_HEADING：负向航向
     */
    public enum THeadingRule {POSITIVE_HEADING, NEGATIVE_HEADING}


    public int m_uId;                  // 路径ID号，从1开始
    public short m_uType;              // 路径拓扑类型
    public short m_uExtType;           // 扩展类型
    public int m_uStartNode;           // 起始节点ID
    public int m_uEndNode;             // 结束节点ID
    public float m_fSize;              // 路径长度
    public float[] m_fVeloLimit;       // 路径速度限制（[0]：正向速度，[1]：反向速度）
    public short m_uGuideType;         // 引导类型

    public float m_fNavParam;          // 导航参数
    public short m_uObstacle;          // 障碍物基本检测数据
    public int m_uFwdRotoScannerObstacle;     // 前进方向旋转扫描仪障碍物数据
    public int m_uFwdObdetectorObstacle;       // 前进方向障碍物检测器数据
    public int m_uBwdRotoScannerObstacle;     // 后退方向旋转扫描仪障碍物数据
    public int m_uBwdObdetectorObstacle;       // 后退方向障碍物检测器数据

    public float[] m_fTimeValue;       // 时间值
    public short m_uMoveHeading = 1;   // 行走方向：1-起点→终点；2-终点→起点；3-双向通行；4-禁止通行
    public short m_uPathHeading;       // 路径航向方向
    public short m_LayerID;            // 所在图层ID

    public short m_uCarrierType = 255; // 车辆类型，默认：255
    public short m_uOnLine = 1;        // 允许上线：1-允许上线，0-不允许；默认：1

    public int m_clr = 255;            // 绘图颜色

    public NodeBase m_pNodeBase;       // 关联的节点管理对象

    /**
     * 构造方法：初始化路径对象的基本属性
     */
    public Path() {
        m_uObstacle = 0;               // 初始化障碍物数据
        m_fVeloLimit = new float[2];   // 初始化速度限制数组
        m_fTimeValue = new float[2];   // 初始化时间值数组
    }

    /**
     * 抽象方法：绘制路径
     *
     * @param ScrnRef 坐标转换对象
     * @param Grp     画布对象
     */
    abstract public void Draw(CoordinateConversion ScrnRef, Canvas Grp, int color, Paint paint);

    /**
     * 抽象方法：绘制路径ID
     *
     * @param ScrnRef 坐标转换对象
     * @param Grp     画布对象
     */
    abstract public void DrawID(CoordinateConversion ScrnRef, Canvas Grp, int color, Paint paint);

    /**
     * 抽象方法：获取节点在路径上的航向角
     *
     * @param nd 节点对象
     * @return 航向角对象
     */
    abstract public Angle GetHeading(Node nd);

    /**
     * 抽象方法：判断路径是否在指定矩形范围内
     *
     * @param minx 矩形最小x坐标
     * @param miny 矩形最小y坐标
     * @param maxx 矩形最大x坐标
     * @param maxy 矩形最大y坐标
     * @return 是否在矩形范围内
     */
    abstract public boolean ISInRect(double minx, double miny, double maxx, double maxy);

    /**
     * 抽象方法：判断点是否命中路径（用于交互操作）
     *
     * @param pnt     屏幕坐标点
     * @param ScrnRef 坐标转换对象
     * @return 命中结果（0-未命中，非0-命中）
     */
    abstract int PointHitTest(Point pnt, CoordinateConversion ScrnRef);

    /**
     * 创建路径对象
     *
     * @param uId        路径ID
     * @param uStartNode 起始节点ID
     * @param uEndNode   结束节点ID
     * @param fVeloLimit 速度限制数组
     * @param uType      路径类型
     * @param uGuideType 引导类型
     * @param fNavParam  导航参数
     * @param uExtType   扩展类型
     * @param nodeBase   节点管理对象
     */
    public void Create(int uId, int uStartNode, int uEndNode, float[] fVeloLimit, short uType, short uGuideType, float fNavParam, short uExtType, NodeBase nodeBase) {

        m_uId = uId;
        m_uType = uType;
        m_uExtType = uExtType;
        m_uStartNode = uStartNode;
        m_uEndNode = uEndNode;
        m_fVeloLimit[0] = fVeloLimit[0];  // 设置正向速度限制
        m_fVeloLimit[1] = fVeloLimit[1];  // 设置反向速度限制
        m_uGuideType = uGuideType;
        m_fNavParam = fNavParam;
        m_uObstacle = 0;                  // 初始化障碍物数据
        m_uPathHeading = (short) (THeadingRule.POSITIVE_HEADING.ordinal());  // 默认正向航向

        m_uFwdRotoScannerObstacle = 0;     // 初始化前进方向旋转扫描仪障碍物数据
        m_uFwdObdetectorObstacle = 65535;  // 初始化前进方向障碍物检测器数据
        m_uBwdRotoScannerObstacle = 0;     // 初始化后退方向旋转扫描仪障碍物数据
        m_uBwdObdetectorObstacle = 65535;  // 初始化后退方向障碍物检测器数据
        m_uMoveHeading = (short) 3;        // 默认双向通行
        m_pNodeBase = nodeBase;            // 设置关联的节点管理对象
    }

    /**
     * 创建示教路径（PP模式）
     *
     * @param uId        路径ID
     * @param uStartNode 起始节点ID
     * @param uEndNode   结束节点ID
     * @param fVeloLimit 速度限制数组
     * @param uType      路径类型
     * @param uGuideType 引导类型
     * @param fNavParam  导航参数
     * @param uExtType   扩展类型
     * @param nodeBase   节点管理对象
     * @param pathParam  路径参数（包含行走方向等信息）
     */
    public void CreatePP(int uId, int uStartNode, int uEndNode, float[] fVeloLimit, short uType, short uGuideType, float fNavParam, short uExtType, NodeBase nodeBase, short pathParam) {
        // 确保节点数据库已设置
        //assert m_pNodeBase != null?true:false;

        m_uId = uId;
        m_uType = uType;
        m_uExtType = uExtType;
        m_uStartNode = uStartNode;
        m_uEndNode = uEndNode;
        m_fVeloLimit[0] = fVeloLimit[0];  // 设置正向速度限制
        m_fVeloLimit[1] = fVeloLimit[1];  // 设置反向速度限制
        m_uGuideType = uGuideType;
        m_fNavParam = fNavParam;
        m_uObstacle = 0;                  // 初始化障碍物数据
        m_uPathHeading = (short) (THeadingRule.POSITIVE_HEADING.ordinal());  // 默认正向航向

        m_uFwdRotoScannerObstacle = 0;     // 初始化前进方向旋转扫描仪障碍物数据
        m_uFwdObdetectorObstacle = 65535;  // 初始化前进方向障碍物检测器数据
        m_uBwdRotoScannerObstacle = 0;     // 初始化后退方向旋转扫描仪障碍物数据
        m_uBwdObdetectorObstacle = 65535;  // 初始化后退方向障碍物检测器数据
        m_uMoveHeading = pathParam;        // 设置行走方向参数
        m_pNodeBase = nodeBase;            // 设置关联的节点管理对象
    }

    /**
     * 获取起始节点对象
     *
     * @return 起始节点对象
     */
    public Node GetStartNode() {
        return m_pNodeBase.GetNode(m_uStartNode);
    }

    /**
     * 获取结束节点对象
     *
     * @return 结束节点对象
     */
    public Node GetEndNode() {
        return m_pNodeBase.GetNode(m_uEndNode);
    }

    /**
     * 获取起始节点的世界坐标
     *
     * @return 起始节点的世界坐标点
     */
    public Point2d GetStartPnt() {
        Node nd = GetStartNode();
        return nd.GetPoint2dObject();
    }

    /**
     * 获取结束节点的世界坐标
     *
     * @return 结束节点的世界坐标点
     */
    public Point2d GetEndPnt() {
        Node nd = GetEndNode();
        return nd.GetPoint2dObject();
    }


    /**
     * 从数据流中创建路径对象
     *
     * @param dis 数据输入流
     * @return 是否创建成功
     */
    public boolean Create(DataInputStream dis) {

        try {
            // 注意：读取顺序必须与写入顺序一致
            int ch1 = dis.read();
            int ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uId = ((ch2 << 8) + (ch1));  // 读取路径ID
//            Log.d("readWorld", "路径ID号，从1开始 m_uId" + m_uId);

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uStartNode = ((ch2 << 8) + (ch1));  // 读取起始节点ID
//            Log.d("readWorld", "起始节点ID， m_uStartNode" + m_uStartNode);

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uEndNode = ((ch2 << 8) + (ch1));  // 读取结束节点ID
//            Log.d("readWorld", "结束节点ID， m_uEndNode" + m_uEndNode);

            ch1 = dis.read();
            ch2 = dis.read();
            int ch3 = dis.read();
            int ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            int tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1));
            this.m_fVeloLimit[0] = Float.intBitsToFloat(tempI);  // 读取正向速度限制
//            Log.d("readWorld", "路径速度限制（[0]：正向速度，[1]：反向速度），     this.m_fVeloLimit[0]" + this.m_fVeloLimit[0]);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1));
            this.m_fVeloLimit[1] = Float.intBitsToFloat(tempI);  // 读取反向速度限制
//            Log.d("readWorld", "路径速度限制（[0]：正向速度，[1]：反向速度），     this.m_fVeloLimit[1]" + this.m_fVeloLimit[1]);

//		#endif
            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uGuideType = (short) ((ch2 << 8) + (ch1));  // 读取引导类型
//            Log.d("readWorld", "读取引导类型 m_uGuideType" + m_uGuideType);

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            this.m_uExtType = (short) ((ch2 << 8) + (ch1));  // 读取扩展类型
//            Log.d("readWorld", "扩展类型 m_uExtType" + m_uExtType);


            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1));
            this.m_fNavParam = Float.intBitsToFloat(tempI);  // 读取导航参数
//            Log.d("readWorld", "读取导航参数 m_fNavParam" + m_fNavParam);

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            m_LayerID = (short) ((ch2 << 8) + (ch1));  // 读取图层ID
//            Log.d("readWorld", "读取图层ID m_LayerID" + m_LayerID);

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            m_uMoveHeading = (short) ((ch2 << 8) + (ch1));  // 读取行走方向
//            Log.d("readWorld", "读取行走方向 m_uMoveHeading" + m_uMoveHeading);

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            m_uCarrierType = (short) ((ch2 << 8) + (ch1));  // 读取车辆类型
//            Log.d("readWorld", "读取车辆类型 m_uCarrierType" + m_uCarrierType);

            ch1 = dis.read();
            ch2 = dis.read();
            if ((ch1 | ch2) < 0) throw new EOFException();
            m_uOnLine = (short) ((ch2 << 8) + (ch1 << 0));  // 读取上线状态
//            Log.d("readWorld", "读取上线状态 m_uOnLine" + m_uOnLine);

            ch1 = dis.read();
            ch2 = dis.read();
            ch3 = dis.read();
            ch4 = dis.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
            tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1));
            m_clr = tempI;  // 读取绘图颜色
//            Log.d("readWorld", "读取绘图颜色 m_clr" + m_clr);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 成功，返回true
        return true;
    }

    /**
     * 将路径对象保存到数据流中
     *
     * @param dis 数据输出流
     * @return 是否保存成功
     */
    public boolean Save(DataOutputStream dis) {

        // 保存路径数据
        try {
            TranBytes tan = new TranBytes();  // 创建字节转换对象（用于处理字节序）

            // 写入路径ID
            tan.writeShort(dis, this.m_uId);

            // 写入起始节点ID
            tan.writeShort(dis, this.m_uStartNode);

            // 写入结束节点ID
            tan.writeShort(dis, this.m_uEndNode);

            // 写入正向速度限制
            int Ix = Float.floatToIntBits(this.m_fVeloLimit[0]);
            tan.writeInteger(dis, Ix);

            // 写入反向速度限制
            Ix = Float.floatToIntBits(this.m_fVeloLimit[1]);
            tan.writeInteger(dis, Ix);

            // 写入引导类型
            tan.writeShort(dis, this.m_uGuideType);

            // 写入扩展类型
            tan.writeShort(dis, this.m_uExtType);

            // 写入导航参数
            Ix = Float.floatToIntBits(this.m_fNavParam);
            tan.writeInteger(dis, Ix);

            // 写入图层ID、行走方向、车辆类型、上线状态
            dis.writeShort(tan.TranShort(m_LayerID));
            dis.writeShort(tan.TranShort(m_uMoveHeading));
            dis.writeShort(tan.TranShort(m_uCarrierType));
            dis.writeShort(tan.TranShort(m_uOnLine));

            // 写入绘图颜色
            dis.writeInt(tan.tranInteger(m_clr));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 成功，返回true
        return true;
    }

}
