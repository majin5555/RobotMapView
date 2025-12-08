package com.siasun.dianshi.bean.pp;

import com.siasun.dianshi.bean.Point2d;

public class CurveSamplePoint extends Point2d {

    public float t;
    public float fProgress;
    public float fSegLen;
    public float fTangentAngle;
    public float fCurvature;  //曲率
    public float[] fUserData;

    public CurveSamplePoint() {
        fUserData = new float[10];
        t = 0;
        fProgress = 0;
        fSegLen = 0;
        fTangentAngle = 0;
        fCurvature = 0;

        for (int i = 0; i < 10; i++)
            fUserData[i] = 0;
    }
}
