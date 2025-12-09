package com.siasun.dianshi.io;

import com.siasun.dianshi.bean.TranBytes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;


/**
 * Created by changjian.song on 2023/3/29 at 18:22
 *
 * @description ：读写world_pad.dat工具类
 */
public class WorldFileIO {
    public static int readInt(DataInputStream dis) throws IOException {
        int ch1 = dis.read();
        int ch2 = dis.read();
        int ch3 = dis.read();
        int ch4 = dis.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1));
    }

    public static float readFloat(DataInputStream dis) throws IOException {
        int ch1 = dis.read();
        int ch2 = dis.read();
        int ch3 = dis.read();
        int ch4 = dis.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        int tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1));
        return Float.intBitsToFloat(tempI);
    }

    public static short readShort(DataInputStream dis) throws IOException{
        int ch1 = dis.read();
        int ch2 = dis.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (short) ((ch2 << 8) + (ch1 << 0));
    }

    public static void writeInt(int num, DataOutputStream dos) {
        TranBytes tan = new TranBytes();
        try {
            dos.writeInt(tan.tranInteger(num));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeFloat(float num, DataOutputStream dos) {
        TranBytes tan = new TranBytes();
        try {
            dos.writeFloat(tan.tranFloat(num));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 