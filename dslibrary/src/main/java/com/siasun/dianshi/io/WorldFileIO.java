package com.siasun.dianshi.io;

import com.siasun.dianshi.bean.TranBytes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;


/**
 * world_pad.dat文件读写工具类
 * 提供从二进制文件中读取和写入不同类型数据的方法，处理字节序转换
 * Created by changjian.song on 2023/3/29 at 18:22
 */
public class WorldFileIO {
    /**
     * 从数据流中以小端字节序读取int类型数据
     *
     * @param dis 数据输入流
     * @return 读取的int值
     * @throws IOException 文件读取异常
     */
    public static int readInt(DataInputStream dis) throws IOException {
        // 依次读取4个字节（小端字节序）
        int ch1 = dis.read();
        int ch2 = dis.read();
        int ch3 = dis.read();
        int ch4 = dis.read();
        
        // 检查是否到达文件末尾
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
            
        // 将小端字节序转换为int值
        return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1));
    }

    /**
     * 从数据流中以小端字节序读取float类型数据
     *
     * @param dis 数据输入流
     * @return 读取的float值
     * @throws IOException 文件读取异常
     */
    public static float readFloat(DataInputStream dis) throws IOException {
        // 依次读取4个字节（小端字节序）
        int ch1 = dis.read();
        int ch2 = dis.read();
        int ch3 = dis.read();
        int ch4 = dis.read();
        
        // 检查是否到达文件末尾
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
            
        // 将小端字节序转换为int值
        int tempI = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1));
        
        // 将int转换为float
        return Float.intBitsToFloat(tempI);
    }

    /**
     * 从数据流中以小端字节序读取short类型数据
     *
     * @param dis 数据输入流
     * @return 读取的short值
     * @throws IOException 文件读取异常
     */
    public static short readShort(DataInputStream dis) throws IOException {
        // 依次读取2个字节（小端字节序）
        int ch1 = dis.read();
        int ch2 = dis.read();
        
        // 检查是否到达文件末尾
        if ((ch1 | ch2) < 0)
            throw new EOFException();
            
        // 将小端字节序转换为short值
        return (short) ((ch2 << 8) + (ch1 << 0));
    }

    /**
     * 以小端字节序将int类型数据写入数据流
     *
     * @param num 要写入的int值
     * @param dos 数据输出流
     */
    public static void writeInt(int num, DataOutputStream dos) {
        // 创建字节序转换工具
        TranBytes tan = new TranBytes();
        try {
            // 转换为小端字节序并写入
            dos.writeInt(tan.tranInteger(num));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 以小端字节序将float类型数据写入数据流
     *
     * @param num 要写入的float值
     * @param dos 数据输出流
     */
    public static void writeFloat(float num, DataOutputStream dos) {
        // 创建字节序转换工具
        TranBytes tan = new TranBytes();
        try {
            // 转换为小端字节序并写入
            dos.writeFloat(tan.tranFloat(num));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 