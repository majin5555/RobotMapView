package com.siasun.dianshi.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by changjian.song on 2023/3/27 at 14:36
 *
 * @description ：pad示教生成路径规划结构体
 */
public class PathPlanInfo implements Serializable {
    public List<Point2d> teachPointList = new ArrayList<>(); //狭窄区域示教点集合
    public List<Integer> pathType = new ArrayList<>(); //路段类型，0-直线， 1-贝塞尔
    public String areaId = "-2"; //该路段所属区域id

    public List<Point2d> getTeachPointList() {
        return teachPointList;
    }

    public void setTeachPointList(List<Point2d> teachPointList) {
        this.teachPointList = teachPointList;
    }

    public List<Integer> getPathType() {
        return pathType;
    }

    public void setPathType(List<Integer> pathType) {
        this.pathType = pathType;
    }

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    @Override
    public String toString() {
        return "PathPlanInfo{" +
                "pathType=" + pathType +
//                ", nodeLoc=" + nodeLoc +
                ", areaId='" + areaId + '\'' +
                '}';
    }
}