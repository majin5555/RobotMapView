package com.siasun.dianshi.bean.world;

import android.annotation.SuppressLint;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

public class RfId {
    public static final int RFID_CODE_LEN = 6;

    public char[] m_uchCode = new char[RFID_CODE_LEN];

    public RfId() {
        for (int i = 0; i < RFID_CODE_LEN; i++)
            m_uchCode[i] = 0;
    }

    public RfId(char[] pBuf) {
        Init(pBuf);
    }


    public void Init(char[] pBuf) {
        if (pBuf == null) {
            for (int i = 0; i < RFID_CODE_LEN; i++)
                m_uchCode[i] = 0;
        } else m_uchCode = Arrays.copyOf(pBuf, RFID_CODE_LEN);
    }

    @SuppressLint("SuspiciousIndentation")
    public boolean Usable() {
        for (int i = 0; i < RFID_CODE_LEN; i++)
            if (m_uchCode[i] != 0) return true;

        return false;
    }

    public void Create(DataInputStream dis) {
        try {

            for (int i = 0; i < RFID_CODE_LEN; i++) {
                int ch1 = dis.read();

                this.m_uchCode[i] = (char) ch1;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Save(DataOutputStream dis) {
        try {

            for (int i = 0; i < RFID_CODE_LEN; i++) {
                byte b = (byte) (this.m_uchCode[i] & 0xff);
                dis.writeByte(b);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
