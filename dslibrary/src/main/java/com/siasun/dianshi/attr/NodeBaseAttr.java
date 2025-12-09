package com.siasun.dianshi.attr;


import com.siasun.dianshi.bean.world.RfId;

public class NodeBaseAttr {
    public int m_uId;               // 节点ID号
    public short m_uType;           // 节点类型码
    public double x;                 // 节点坐标x
    public double y;                 // 节点坐标x
    public short m_uExtType;        // 节点的扩展类型码
    public float  m_fHeading;        // 节点处车头方向
    public RfId m_Tag;             // 电子标签信息
    public float  m_fChkMarkDist;    // 查找地标距离
    public float  m_fChkMarkVel;     // 找地标速度
    public float  m_fMarkWidth;      // 地标有效宽度

    public float  m_fOffset1;
    public float  m_fOffset2;

    public float  m_fFwdMarkOffset;
    public float  m_fBwdMarkOffset;
    public short m_uLayerId ;
    public short m_uCrossType ;
    public short m_uStationType;
    public short m_uStationId;
    public short m_uStationTempId;
    public short m_uCarrierType;  //车辆类型
    public short m_uOnLine;       //允许上线
    public char[] buf = new char[100];

    public	NodeBaseAttr()
    {

    }
}
