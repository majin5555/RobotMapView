package com.siasun.dianshi.bean.world;
	
import android.annotation.SuppressLint;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
public class RfId {
	public static final int  RFID_CODE_LEN =  6;
	
	//////////////////////////////////////////////////////////////////////////////
	//Implementation of class "RfId".
	
	public char[] m_uchCode = new char[RFID_CODE_LEN];
	//Implementation of class "RfId".
	//
	//Author: Zhanglei
	//Date:   2004. 5. 25
	//
	
	
	//////////////////////////////////////////////////////////////////////////////
	//Implementation of class "RfId".
	
	public RfId() 
	{
		for(int i=0;i<RFID_CODE_LEN;i++)
		m_uchCode[i]=0;
	}
	
	public RfId(char[] pBuf) 
	{
		Init(pBuf);
	}
	
	//
	//Initialize the codes.
	//
	public void Init(char[] pBuf)
	{
	if (pBuf == null)
	{
		for(int i=0;i<RFID_CODE_LEN;i++)
			m_uchCode[i]=0;
	}
	else
		m_uchCode = Arrays.copyOf(pBuf, RFID_CODE_LEN);
	}
	
//	//
//	//Compare if 2 RF-IDs are equal.
//	//
//	boolean operator == (RfId& Obj)	
//	{
//	return (memcmp(m_uchCode, Obj.m_uchCode, RFID_CODE_LEN) == 0);
//	}
//	
//	//
//	//Compare if 2 RF-IDs are not equal.
//	//
//	boolean operator != (RfId& Obj) 
//	{
//	return (memcmp(m_uchCode, Obj.m_uchCode, RFID_CODE_LEN) != 0);
//	}
	
	//
	//Check if the RF-ID is usable.
	//
	public boolean Usable()
	{
		for (@SuppressLint("SuspiciousIndentation") int i = 0; i < RFID_CODE_LEN; i++)
		if (m_uchCode[i] != 0)
		return true;
		
		return false;
	}
	
	//
	//Create the node data from a text file.
	//
//	boolean Create(FILE *StreamIn)
//	{
//	for (int i = 0; i < RFID_CODE_LEN; i++)
//	{
//	if (fscanf(StreamIn, ", %X", &m_uchCode[i]) == EOF)
//	return false;
//	}
//	fscanf(StreamIn, "\n");
//	return true;
//	}
//	
//	//
//	//Save: Save node data to a text file.
//	//
//	boolean Save(FILE *StreamOut)
//	{
//	for (int i = 0; i < RFID_CODE_LEN; i++)
//	{
//	if (fprintf(StreamOut, ", %02X", m_uchCode[i]) == EOF)
//	return false;
//	}
//	fprintf(StreamOut, "\n");
//	
//	return true;
//	}
//	
	//
	//Archive I/O routine.
	//
	
public void Create( DataInputStream dis) 
 {
//		    File file = new File(strFile);
    try {
    	
		for (int i = 0; i < RFID_CODE_LEN; i++)
		{
			  int ch1 = dis.read();
//		        int ch2 = dis.read();
//		        if ((ch1 | ch2) < 0)
//		            throw new EOFException();
//		        this.m_uchCode[i] = (char)((ch2 << 8) + (ch1 << 0));
			  this.m_uchCode[i] = (char)ch1;
		}
      //  dis.close();
    } catch (FileNotFoundException e) {
        e.printStackTrace();
    } catch (IOException e) {
        e.printStackTrace();
    }
}

	public void Save(DataOutputStream dis)
	{
//		    File file = new File(strFile);
		try {

			for (int i = 0; i < RFID_CODE_LEN; i++)
			{
//				int ch1 = dis.read();
//				this.m_uchCode[i] = (char)ch1;
		//		int k =this.m_uchCode[i].getBytes("UTF-8").lenth();
				byte b= (byte)(this.m_uchCode[i]&0xff);
				dis.writeByte(b);
			}
			//  dis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

//	CArchive& operator >> (CArchive& ar, RfId& Obj)
//	{
//	for (int i = 0; i < RFID_CODE_LEN; i++)
//	ar >> Obj.m_uchCode[i];
//	return ar;
//	}
//	
//	CArchive& operator << (CArchive& ar, RfId& Obj)
//	{
//	for (int i = 0; i < RFID_CODE_LEN; i++)
//	ar << Obj.m_uchCode[i];
//	return ar;
//	}
	
}
