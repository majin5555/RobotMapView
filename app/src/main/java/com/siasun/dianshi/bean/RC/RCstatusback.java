package com.siasun.dianshi.bean.RC;

import java.io.Serializable;


/**
 * 表示从RC接收到的反馈信息的结构体
 *
 * @author Jiang bin 2017-3-3
 */
public class RCstatusback implements Serializable {

    public byte robotstatus; //机器人状态

    public byte locationstatus; //定位状态

    public byte elecmachinestatus;   //电机状态

    public byte navistatus; //导航状态

    public byte roamstatus;   //漫游状态

    public byte avoidstatus;   //避障状态

    public byte followstatus; //跟随状态

    public byte showstatus; //演示状态

    public byte electricquantity;           //电量

    public byte systemmode; //系统模式

    public byte untouchbutton; //防碰条开关

    public byte undropbutton;    //防跌落开关

    public byte sonarbutton;           //声呐开关

    public byte laserbutton;            //激光避障开关

    public byte electricdetectionbutton;            //电量检测开关

    public byte serverconnectbutton;            //连接服务器开关


    public byte facedetectionbutton;            //人脸识别开关
    public byte bank_assist_facedetectionbutton; //银行助理人脸识别开关

    public double leftwheelspeed;   //左轮转速

    public double rightwheelspeed;   //右轮转速
}
