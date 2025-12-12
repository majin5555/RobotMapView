package com.siasun.dianshi.utils

import com.siasun.dianshi.bean.TranBytes
import com.siasun.dianshi.bean.pp.world.CLayer
import com.siasun.dianshi.io.FileIOUtil
import com.siasun.dianshi.io.WorldFileIO
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * World类，用于处理World文件数据
 */
class World {
    var cLayer: CLayer = CLayer()
    private val otherProperty = CharArray(32)

    /**
     * 读取路径  获取world_pad.dat 文件下的二进制数据文件
     *
     * @param path        文件所在目录路径
     * @param strFileName 文件名
     * @return 是否读取成功
     */
    fun readWorld(path: String, strFileName: String): Boolean {
        return try {
            // 创建文件输入流和数据输入流
            val fis = FileInputStream(path + File.separator + strFileName)
            val dis = DataInputStream(fis)
            // 读取World编辑器版本号
            val worldEditorVersion = WorldFileIO.readInt(dis)
            //            Log.d("readWorld", "World编辑器版本号：" + worldEditorVersion);
            // 读取两个时间戳（可能是创建时间和修改时间）
            val time1 = dis.readInt()
            //            Log.d("readWorld", "time1：" + time1);
            val time2 = dis.readInt()
            //            Log.d("readWorld", "time2：" + time2);
            // 读取地图版本号
            val mapVersion = WorldFileIO.readInt(dis)
            //            Log.d("readWorld", "mapVersion：" + mapVersion);
            // 读取项目名称长度
            val nameLength = WorldFileIO.readInt(dis)
            //            Log.d("readWorld", "nameLength：" + nameLength);
            // 读取其他属性（固定32个字符长度）
            for (cnt in 0..31) {
                val c = dis.read().toChar()
                otherProperty[cnt] = c
                //                Log.d("readWorld", "otherProperty[" + cnt + "]" + c);
            }
            //            Log.d("readWorld", "otherProperty：otherProperty.size" + this.otherProperty.length);
            // 跳过一个整数值（可能是保留字段或标记位）
            val i1 = WorldFileIO.readInt(dis)
            //            Log.d("readWorld", "跳过一个整数值（可能是保留字段或标记位）" + i1);
            // 读取图层数据，包括路径、区域等核心地图元素
            if (cLayer != null) cLayer!!.read(dis)
            //            Log.d("readWorld", "读取图层数据，包括路径、区域等核心地图元素 cLayer" + cLayer);
            // 跳过一个整数值（可能是图层数据结束标记或保留字段）
            val i2 = WorldFileIO.readInt(dis)
            //            Log.d("readWorld", "跳过一个整数值（可能是图层数据结束标记或保留字段）" + i2);
            // 读取驱动单元数量
            val nDriveUnitCount = WorldFileIO.readInt(dis)
            //            Log.d("readWorld", "读取驱动单元数量" + nDriveUnitCount);
            // fix 注释
//            for (int i = 0; i < nDriveUnitCount; ++i) {
//                this.UnitType = WorldFileIO.readInt(dis);           // 驱动单元类型
//                Log.d("readWorld", "驱动单元类型 UnitType" + UnitType);
//                this.drive_unit_x = WorldFileIO.readFloat(dis);     // 驱动单元X坐标
//                Log.d("readWorld", "drive_unit_x" + drive_unit_x);
//                this.drive_unit_y = WorldFileIO.readFloat(dis);     // 驱动单元Y坐标
//                Log.d("readWorld", "驱动单元Y坐标" + drive_unit_y);
//                this.fVelMax = WorldFileIO.readFloat(dis);          // 最大速度
//                Log.d("readWorld", "最大速度" + fVelMax);
//                this.fVelACC = WorldFileIO.readFloat(dis);          // 加速度
//                Log.d("readWorld", "加速度" + fVelACC);
//                this.fThetaDiffMax = WorldFileIO.readFloat(dis);    // 最大角度差
//                Log.d("readWorld", "最大角度差" + fThetaDiffMax);
//                this.fAngVelACC = WorldFileIO.readFloat(dis);       // 角加速度
//                Log.d("readWorld", "角加速度" + fAngVelACC);
//                this.fSteerAngle = WorldFileIO.readFloat(dis);      // 转向角度
//                Log.d("readWorld", "转向角度" + fSteerAngle);
//                // 读取用户自定义数据（10个float值）
//                Log.d("readWorld", "读取用户自定义数据（10个float值）" + fUserData.length);
//                for (int j = 0; j < 10; ++j) {
//                    this.fUserData[j] = WorldFileIO.readFloat(dis);
//                    Log.d("readWorld", "this.fUserData[" + j + "] " + this.fUserData[j]);
//                }
//            }

            // 关闭输入流
            dis.close()
            true
        } catch (var7: IOException) {
            var7.printStackTrace()
            false
        }
    }

    /**
     * 保存路径数据到world_pad.dat二进制文件
     *
     * @param strFilepath 文件保存目录路径
     * @param strFileName 保存的文件名
     * @return 是否保存成功
     */
    fun saveWorld(strFilepath: String, strFileName: String): Boolean {
        var dos: DataOutputStream? = null
        try {
            // 创建文件输出流和数据输出流，false表示覆盖现有文件
            val outputStream: OutputStream =
                FileOutputStream(strFilepath + File.separator + strFileName, false)
            dos = DataOutputStream(outputStream)

            // 创建字节转换工具，用于处理数据的字节序
            val tan = TranBytes()

            // 写入World编辑器版本号
            dos.writeInt(tan.tranInteger(-10005))
            // 写入时间戳
            dos.writeInt(tan.tranInteger(0))
            dos.writeInt(tan.tranInteger(0))
            // 写入地图版本号
            dos.writeInt(tan.tranInteger(0))
            // 写入项目名称长度
            WorldFileIO.writeInt(0, dos)

            // 写入项目名称
            dos.write("".toByteArray())

            // 写入其他属性（32个字符）
            val var4 = otherProperty
            val i = var4.size
            for (j in 0 until i) {
                val c = otherProperty[j]
                dos.write(c.code)
            }

            // 写入一个整数值1（可能是标记位或版本标识）
            dos.writeInt(tan.tranInteger(1))

            // 写入图层数据（包含路径、区域等核心地图元素）
            if (cLayer != null) cLayer!!.save(dos)

            // 写入一个整数值0（可能是图层数据结束标记或保留字段）
            val cnt = 0
            dos.writeInt(tan.tranInteger(cnt))

            // 写入驱动单元数量
            tan.writeInteger(dos, 0)

//            // 根据驱动单元数量写入每个驱动单元的配置  fix 注释  mj
//            for (i = 0; i < this.nDriveUnitCount; ++i) {
//                tan.writeInteger(dos, this.UnitType);           // 驱动单元类型
//                dos.writeFloat(tan.tranFloat(this.drive_unit_x));     // 驱动单元X坐标
//                dos.writeFloat(tan.tranFloat(this.drive_unit_y));     // 驱动单元Y坐标
//                dos.writeFloat(tan.tranFloat(this.fVelMax));          // 最大速度
//                dos.writeFloat(tan.tranFloat(this.fVelACC));          // 加速度
//                dos.writeFloat(tan.tranFloat(this.fThetaDiffMax));    // 最大角度差
//                dos.writeFloat(tan.tranFloat(this.fAngVelACC));       // 角加速度
//                dos.writeFloat(tan.tranFloat(this.fSteerAngle));      // 转向角度
//
//                // 写入用户自定义数据（10个float值）
//                for (int j = 0; j < 10; ++j) {
//                    dos.writeFloat(tan.tranFloat(this.fUserData[j]));
//                }
//            }
            // 刷新输出流，确保所有数据写入文件
            dos.flush()
            return true
        } catch (var16: IOException) {
            var16.printStackTrace()
        } finally {
            // 关闭输出流
            if (dos != null) {
                try {
                    dos.close()
                    // 同步文件到磁盘，确保数据持久化
                    FileIOUtil.fileSync()
                } catch (var15: IOException) {
                    var15.printStackTrace()
                }
            }
        }
        return false
    }
}
