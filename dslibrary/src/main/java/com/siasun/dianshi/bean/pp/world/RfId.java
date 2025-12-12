package com.siasun.dianshi.bean.pp.world;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

public class RfId {
    public final int RFID_CODE_LEN = 6;

    public char[] m_uchCode = new char[RFID_CODE_LEN];

    public RfId() {
        for (int i = 0; i < RFID_CODE_LEN; i++)
            m_uchCode[i] = 0;
    }


    public void Init(char[] pBuf) {
        if (pBuf == null) {
            for (int i = 0; i < RFID_CODE_LEN; i++)
                m_uchCode[i] = 0;
        } else m_uchCode = Arrays.copyOf(pBuf, RFID_CODE_LEN);
    }


    /**
     * 读取
     *
     * @param dis
     */
    public void read(DataInputStream dis) {
        try {
//            Log.d("readWorld", "RFID_CODE_LEN " + RFID_CODE_LEN);
            for (int i = 0; i < RFID_CODE_LEN; i++) {
                int ch1 = dis.read();
                this.m_uchCode[i] = (char) ch1;
//                Log.d("readWorld", "m_uchCode[" + i + "] " + this.m_uchCode[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save(DataOutputStream dis) {
        try {
            for (int i = 0; i < RFID_CODE_LEN; i++) {
                byte b = (byte) (this.m_uchCode[i] & 0xff);
                dis.writeByte(b);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
