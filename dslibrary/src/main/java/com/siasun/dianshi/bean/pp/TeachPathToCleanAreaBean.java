package com.siasun.dianshi.bean.pp;

import com.siasun.dianshi.bean.Point2d;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by changjian.song on 2023/3/24 at 16:47
 *
 * @description ：
 */
public class TeachPathToCleanAreaBean {
    private String areaName;
    private int areaId;
    private List<Point2d> pathList = new ArrayList<>();

    public TeachPathToCleanAreaBean() {
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public int getAreaId() {
        return areaId;
    }

    public void setAreaId(int areaId) {
        this.areaId = areaId;
    }

    public List<Point2d> getPathList() {
        return pathList;
    }

    public void setPathList(List<Point2d> pathList) {
        this.pathList = pathList;
    }

    @Override
    public String toString() {
        return "TeachPathToCleanAreaBean{" +
                "areaName='" + areaName + '\'' +
                ", areaId=" + areaId +
                ", pathList=" + pathList +
                '}';
    }
}