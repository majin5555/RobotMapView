package com.siasun.dianshi.bean.task;


import com.siasun.dianshi.bean.PointNew;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Vector;


/**
 * Created by changjian.song on 2021/12/9 at 16:44
 *
 * @description ：子区域 (已选区域的参数)
 */
public class Sub_Task implements Serializable {
    public Sub_Task() {
    }

    //子区域的名称，也就是已选区域中的子任务的名称
    public String sub_task_name;
    //子区域的循环次数，默认为1，不需要修改
    public int sub_cycle = 1;
    //区域id
    public int sub_region_id;
    //区域所在层号
    public int sub_region_layer;
    //区域起点
    public float[] areaStartPnt;
    //车体清扫模式
    public int cleanMode;
    // Pad清扫模式
    public int customCleanMode;
    //洒水量
    public int waterLevel = 35;
    //速度
    public float speed = 0.6f;
    //刷盘推杆下降高度
    public int brushHeight = 50;
    //0-从路径规划器申请  1-从PAD申请
    public int pathFrom = 0;
    //4-回字形  3-弓字型 6-混合型
    public int pathType = 4;
    //区域类型 外部控制台区域类型0   内部控制台区域为1
    public int areaType = 1;
    //洁尔量开关 0 close 1 open
    public byte cleanSolution = 0;
    //区域顶点
    public Vector<PointNew> areaVertexPnt;



    @Override
    public String toString() {
        return "Sub_Task{" +
                "sub_task_name='" + sub_task_name + '\'' +
                ", sub_cycle=" + sub_cycle +
                ", sub_region_id=" + sub_region_id +
                ", sub_region_layer=" + sub_region_layer +
                ", areaStartPnt=" + Arrays.toString(areaStartPnt) +
                ", cleanMode=" + cleanMode +
                ", customCleanMode=" + customCleanMode +
                ", waterLevel=" + waterLevel +
                ", speed=" + speed +
                ", brushHeight=" + brushHeight +
                ", pathFrom=" + pathFrom +
                ", pathType=" + pathType +
                ", areaType=" + areaType +
                ", cleanSolution=" + cleanSolution +
                ", areaVertexPnt=" + areaVertexPnt +
                '}';
    }
}