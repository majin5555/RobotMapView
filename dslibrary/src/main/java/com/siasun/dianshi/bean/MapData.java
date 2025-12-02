package com.siasun.dianshi.bean;

public final class MapData {
    public float resolution = 0.05f;
    public float originX;
    public float originY;
    public float width;
    public float height;


    @Override
    public String toString() {
        return "MapData{" +
                "resolution=" + resolution +
                ", originX=" + originX +
                ", originY=" + originY +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
