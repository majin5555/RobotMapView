package com.siasun.dianshi.bean.createMap2d;

/**
 * Created by wangziwei on 2019/1/9.
 * 常量数据
 */

public class MapEditorConstants {
    //地图元数据字节数
    public static final int META_DATA_LEN = 28;
    //地图每个像素4字节
    public static final int MAP_PIXEL_SIZE = 4;

    //地图块宽高
    public static final int TILE_WIDTH  = 256;
    public static final int TILE_HEIGHT = 256;

    public static final int MAP_EDITOR_MOVE = 1;
    public static final int MAP_EDITOR_DRAW_LINE = 2;
    public static final int MAP_EDITOR_DRAW_RECT = 3;
    public static final int MAP_EDITOR_DRAW_POLY = 4;
    public static final int MAP_EDITOR_DRAW_CIRCLE = 5;
    public static final int MAP_EDITOR_DRAW_TURN_POINT = 6;
    public static final int MAP_EDITOR_DRAW_TARGET_POINT = 7;
    public static final int MAP_EDITOR_DRAW_PASS_POINT = 8;
    public static final int MAP_EDITOR_ERASE = 128;

    public static final int MAX_DRAW_HISTORY_COUNT = 10;
    public static final int ERASE_SHOW_OFFSET = 30;
    public static final int ERASE_SHOW_WIDTH = 20;
}
