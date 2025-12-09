package com.siasun.dianshi.bean;

import java.io.DataOutputStream;
import java.io.IOException;

public class TranBytes {
    //实现数据大端转小端
    public TranBytes() {

    }

    public Short TranShort(Short value) {
        int ch1 = value;
        int ch2 = value;
        short sT = (short) ((ch2 >> 8) + (ch1 & 0xff) << 8);
        return sT;
    }

    public int tranInteger(int value) {
        int Ix = value;
        int ch1 = Ix;
        int ch2 = Ix;
        int ch3 = Ix;
        int ch4 = Ix;
        ch1 = ch1 >> 24; //低8位
        ch2 = (ch2 & 0xff0000) >> 8; //低16位
        ch3 = (ch3 & 0xff00) << 8;
        ch4 = (ch4 & 0xff) << 24;
        if (ch1 < 0) {
            ch1 = (ch1 & 0xff);
        }
        int T = (ch1 + ch2 + ch3 + ch4);
        return T;
    }

    public Float tranFloat(Float value) {
        int Ix = Float.floatToIntBits(value);
        int ch1 = Ix;
        int ch2 = Ix;
        int ch3 = Ix;
        int ch4 = Ix;
        ch1 = ch1 >> 24; //低8位
        ch2 = (ch2 & 0xff0000) >> 8; //低16位
        ch3 = (ch3 & 0xff00) << 8;
        ch4 = (ch4 & 0xff) << 24;
        int T = (ch1 + ch2 + ch3 + ch4);
        float fsT = Float.intBitsToFloat(T);
        return fsT;
    }

    public void writeInteger(DataOutputStream dis, int Data) throws IOException {
        int ch1 = Data;
        int ch2 = Data;
        int ch3 = Data;
        int ch4 = Data;
        ch1 = ch1 & 0xff; //低8位
        ch2 = (ch2 & 0xff00) >> 8; //低16位
        ch3 = (ch3 & 0xff0000) >> 16;
        ch4 = (ch4 & 0xff000000) >> 24;
        if (ch1 < 0) {
            ch1 = (ch1 & 0xff);
        }
        if (ch2 < 0) {
            ch2 = (ch2 & 0xff);
        }
        if (ch3 < 0) {
            ch3 = (ch3 & 0xff);
        }
        if (ch4 < 0) {
            ch4 = (ch4 & 0xff);
        }
        dis.write(ch1);
        dis.write(ch2);
        dis.write(ch3);
        dis.write(ch4);
    }

    public void writeShort(DataOutputStream dis, int Data) throws IOException {
        int ch1 = Data;
        int ch2 = Data;

        ch1 = ch1 & 0xff; //低8位
        ch2 = (ch2 & 0xff00) >> 8; //低16位
        if (ch1 < 0) {
            ch1 = (ch1 & 0xff);
        }
        if (ch2 < 0) {
            ch2 = (ch2 & 0xff);
        }
        dis.write(ch1);
        dis.write(ch2);
    }
}
