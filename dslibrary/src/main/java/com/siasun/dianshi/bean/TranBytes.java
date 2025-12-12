package com.siasun.dianshi.bean;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 字节序转换工具类
 * 用于实现数据的大端字节序与小端字节序之间的转换
 * 在world_pad.dat文件读写中用于处理不同系统间的数据兼容性问题
 */
public class TranBytes {
    /**
     * 构造方法
     * 初始化字节序转换工具
     */
    public TranBytes() {
    }

    /**
     * 将short类型数据从大端转换为小端字节序
     *
     * @param value 原始大端字节序的short值
     * @return 转换为小端字节序的short值
     */
    public Short TranShort(Short value) {
        int ch1 = value;
        int ch2 = value;
        // 实现16位数据的字节序交换
        short sT = (short) ((ch2 >> 8) + (ch1 & 0xff) << 8);
        return sT;
    }

    /**
     * 将int类型数据从大端转换为小端字节序
     *
     * @param value 原始大端字节序的int值
     * @return 转换为小端字节序的int值
     */
    public int tranInteger(int value) {
        int Ix = value;
        int ch1 = Ix;
        int ch2 = Ix;
        int ch3 = Ix;
        int ch4 = Ix;
        
        // 分离各个字节位并重新排序
        ch1 = ch1 >> 24; // 最高8位移到最低位
        ch2 = (ch2 & 0xff0000) >> 8; // 中间高8位移到中低8位
        ch3 = (ch3 & 0xff00) << 8; // 中间低8位移到中高8位
        ch4 = (ch4 & 0xff) << 24; // 最低8位移到最高位
        
        // 处理负数情况
        if (ch1 < 0) {
            ch1 = (ch1 & 0xff);
        }
        
        // 合并重新排序后的字节
        int T = (ch1 + ch2 + ch3 + ch4);
        return T;
    }

    /**
     * 将float类型数据从大端转换为小端字节序
     *
     * @param value 原始大端字节序的float值
     * @return 转换为小端字节序的float值
     */
    public Float tranFloat(Float value) {
        // 将float转换为对应的int表示
        int Ix = Float.floatToIntBits(value);
        int ch1 = Ix;
        int ch2 = Ix;
        int ch3 = Ix;
        int ch4 = Ix;
        
        // 分离各个字节位并重新排序
        ch1 = ch1 >> 24; // 最高8位移到最低位
        ch2 = (ch2 & 0xff0000) >> 8; // 中间高8位移到中低8位
        ch3 = (ch3 & 0xff00) << 8; // 中间低8位移到中高8位
        ch4 = (ch4 & 0xff) << 24; // 最低8位移到最高位
        
        // 合并重新排序后的字节
        int T = (ch1 + ch2 + ch3 + ch4);
        
        // 将int转换回float
        float fsT = Float.intBitsToFloat(T);
        return fsT;
    }

    /**
     * 将int类型数据以小端字节序写入数据流
     *
     * @param dis  数据输出流
     * @param Data 要写入的int数据
     * @throws IOException 写入异常
     */
    public void writeInteger(DataOutputStream dis, int Data) throws IOException {
        int ch1 = Data;
        int ch2 = Data;
        int ch3 = Data;
        int ch4 = Data;
        
        // 分离各个字节位
        ch1 = ch1 & 0xff; // 最低8位
        ch2 = (ch2 & 0xff00) >> 8; // 中低8位
        ch3 = (ch3 & 0xff0000) >> 16; // 中高8位
        ch4 = (ch4 & 0xff000000) >> 24; // 最高8位
        
        // 处理负数情况
        if (ch1 < 0) ch1 = (ch1 & 0xff);
        if (ch2 < 0) ch2 = (ch2 & 0xff);
        if (ch3 < 0) ch3 = (ch3 & 0xff);
        if (ch4 < 0) ch4 = (ch4 & 0xff);
        
        // 按小端字节序写入
        dis.write(ch1);
        dis.write(ch2);
        dis.write(ch3);
        dis.write(ch4);
    }

    /**
     * 将short类型数据以小端字节序写入数据流
     *
     * @param dis  数据输出流
     * @param Data 要写入的short数据
     * @throws IOException 写入异常
     */
    public void writeShort(DataOutputStream dis, int Data) throws IOException {
        int ch1 = Data;
        int ch2 = Data;
        
        // 分离各个字节位
        ch1 = ch1 & 0xff; // 最低8位
        ch2 = (ch2 & 0xff00) >> 8; // 最高8位
        
        // 处理负数情况
        if (ch1 < 0) ch1 = (ch1 & 0xff);
        if (ch2 < 0) ch2 = (ch2 & 0xff);
        
        // 按小端字节序写入
        dis.write(ch1);
        dis.write(ch2);
    }
}
