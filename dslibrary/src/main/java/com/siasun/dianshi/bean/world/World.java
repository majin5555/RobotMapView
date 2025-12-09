package com.siasun.dianshi.bean.world;

import com.siasun.dianshi.bean.TranBytes;
import com.siasun.dianshi.io.FileIOUtil;
import com.siasun.dianshi.io.WorldFileIO;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class World extends NodeBase {
    public CLayer m_layers = new CLayer();
    private int time1;
    private int time2;
    private int mapVersion;
    private StringBuffer projectName;
    private final char[] otherProperty = new char[32];
    private int nameLength;
    private int worldEditorVersion = -10005;
    private int nDriveUnitCount = 1;
    private float drive_unit_x = 0.0F;
    private float drive_unit_y = 0.0F;
    private float fVelMax = 1.0F;
    private float fVelACC = 0.2F;
    private float fThetaDiffMax = 1.0F;
    private float fAngVelACC = 1.0F;
    private float fSteerAngle = 80.0F;
    private final float[] fUserData = new float[10];
    private int UnitType = 5;

    public World() {
    }

    /**
     * 读取路径  获取world_pad.dat 文件下的二进制数据文件
     *
     * @param path
     * @param strFileName
     * @return
     */
    public boolean readWorld(String path, String strFileName) {
        try {
            FileInputStream fis = new FileInputStream(path + File.separator + strFileName);
            DataInputStream dis = new DataInputStream(fis);

            //固定格式 头 start
            this.worldEditorVersion = WorldFileIO.readInt(dis);
            this.time1 = dis.readInt();
            this.time2 = dis.readInt();
            this.mapVersion = WorldFileIO.readInt(dis);
            this.nameLength = WorldFileIO.readInt(dis);
            this.projectName = new StringBuffer();

            for (int cnt = 0; cnt < this.nameLength; ++cnt) {
                this.projectName.append((char) dis.read());
            }

            for (int cnt = 0; cnt < 32; ++cnt) {
                this.otherProperty[cnt] = (char) dis.read();
            }

            WorldFileIO.readInt(dis);
            //固定格式 头 end

            //m_layers 只有1层
            m_layers.create(dis);

            //固定格式 尾 start
            WorldFileIO.readInt(dis);
            this.nDriveUnitCount = WorldFileIO.readInt(dis);
            for (int i = 0; i < this.nDriveUnitCount; ++i) {
                this.UnitType = WorldFileIO.readInt(dis);
                this.drive_unit_x = WorldFileIO.readFloat(dis);
                this.drive_unit_y = WorldFileIO.readFloat(dis);
                this.fVelMax = WorldFileIO.readFloat(dis);
                this.fVelACC = WorldFileIO.readFloat(dis);
                this.fThetaDiffMax = WorldFileIO.readFloat(dis);
                this.fAngVelACC = WorldFileIO.readFloat(dis);
                this.fSteerAngle = WorldFileIO.readFloat(dis);

                for (int j = 0; j < 10; ++j) {
                    this.fUserData[j] = WorldFileIO.readFloat(dis);
                }
                //固定格式 尾 end
            }
            dis.close();
            return true;
        } catch (IOException var7) {
            var7.printStackTrace();
            return false;
        }
    }

    /**
     * 保存路径
     *
     * @param strFilepath
     * @return
     */
    public boolean saveWorld(String strFilepath, String strFileName) {
        DataOutputStream dos = null;

        try {
            OutputStream outputStream = new FileOutputStream(strFilepath + File.separator + strFileName, false);
            dos = new DataOutputStream(outputStream);
            TranBytes tan = new TranBytes();
            //固定格式 头 start
            dos.writeInt(tan.tranInteger(this.worldEditorVersion));
            dos.writeInt(tan.tranInteger(this.time1));
            dos.writeInt(tan.tranInteger(this.time2));
            dos.writeInt(tan.tranInteger(this.mapVersion));
            WorldFileIO.writeInt(this.nameLength, dos);
            if (this.projectName == null) {
                this.projectName = new StringBuffer("");
            }

            dos.write(this.projectName.toString().getBytes());
            char[] var4 = this.otherProperty;
            int i = var4.length;
            for (int j = 0; j < i; ++j) {
                char c = var4[j];
                dos.write(c);
            }
//            Params.SUM_LAYER_NUM = this.m_layers.size();
            dos.writeInt(tan.tranInteger(1));
            //固定格式 头 end

//            for (i = 0; i < Params.SUM_LAYER_NUM; ++i) {
//                (this.m_layers.get(i)).Save(dos);
//            }
            m_layers.Save(dos);

            //固定格式 尾 start
            int cnt = 0;
            dos.writeInt(tan.tranInteger(cnt));
            tan.writeInteger(dos, this.nDriveUnitCount);
            for (i = 0; i < this.nDriveUnitCount; ++i) {
                tan.writeInteger(dos, this.UnitType);
                dos.writeFloat(tan.tranFloat(this.drive_unit_x));
                dos.writeFloat(tan.tranFloat(this.drive_unit_y));
                dos.writeFloat(tan.tranFloat(this.fVelMax));
                dos.writeFloat(tan.tranFloat(this.fVelACC));
                dos.writeFloat(tan.tranFloat(this.fThetaDiffMax));
                dos.writeFloat(tan.tranFloat(this.fAngVelACC));
                dos.writeFloat(tan.tranFloat(this.fSteerAngle));
                for (int j = 0; j < 10; ++j) {
                    dos.writeFloat(tan.tranFloat(this.fUserData[j]));
                }
                //固定格式 尾 end
            }

            dos.flush();
            return true;
        } catch (IOException var16) {
            var16.printStackTrace();
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                    FileIOUtil.fileSync();
                } catch (IOException var15) {
                    var15.printStackTrace();
                }
            }
        }
        return false;
    }
}


