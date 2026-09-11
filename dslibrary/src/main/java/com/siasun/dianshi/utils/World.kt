package com.siasun.dianshi.utils

import com.siasun.dianshi.bean.TranBytes
import com.siasun.dianshi.bean.pp.world.CLayer
import com.siasun.dianshi.utils.io.FileIOUtil
import com.siasun.dianshi.utils.io.WorldFileIO
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            // 与 saveWorld 写入保持一致：使用小端字节序读取
            val time1 = WorldFileIO.readInt(dis)
            //            Log.d("readWorld", "time1：" + time1);
            val time2 = WorldFileIO.readInt(dis)
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
     * 采用"备份 + 原子写"策略：
     * 1. 写操作前，将现有 world_pad.dat 备份为带时间戳的 world_pad_bak_<时间戳>.dat（用于事故找回）
     * 2. 数据写入固定缓冲文件 world_pad.tmp，flush + force 落盘后 rename 替换正式文件（原子替换，杜绝半截文件）
     * 3. 时间戳备份按地图保留最近 5 份，超出删除最旧
     *
     * @param strFilepath 文件保存目录路径
     * @param strFileName 保存的文件名
     * @return 是否保存成功
     */
    fun saveWorld(strFilepath: String, strFileName: String): Boolean {
        var dos: DataOutputStream? = null
        val targetFile = File(strFilepath + File.separator + strFileName)
        val tmpFile = File(strFilepath + File.separator + "$strFileName.tmp")
        try {
            // ---------- ① 写前备份：保留最近 10 份时间戳备份 ----------
            backupWorldFile(strFilepath, strFileName)

            // ---------- ② 原子写：固定缓冲文件 ----------
            val outputStream: OutputStream = FileOutputStream(tmpFile, false)
            dos = DataOutputStream(outputStream)

            // 创建字节转换工具，用于处理数据的字节序
            val tan = TranBytes()

            // 写入World编辑器版本号
            dos.writeInt(tan.tranInteger(-10005))
            // 写入时间戳
            dos.writeInt(tan.tranInteger(System.currentTimeMillis().toInt()))
            dos.writeInt(tan.tranInteger(System.currentTimeMillis().toInt()))
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

            // ---------- ③ 落盘 ----------
            dos.flush()
            // DataOutputStream 不支持直接 force，将其 flush 到底层 FileOutputStream 后 force
            outputStream.flush()
            if (outputStream is FileOutputStream) {
                outputStream.channel?.force(true)
            }
            dos.close()
            dos = null

            // ---------- ④ 原子替换：tmp → 正式文件 ----------
            // Android(Linux) 上 rename 对已存在目标是原子覆盖，不做 delete，避免产生"文件短暂不存在"窗口
            if (!tmpFile.renameTo(targetFile)) {
                // rename 失败则回退为直接拷贝
                tmpFile.copyTo(targetFile, overwrite = true)
                tmpFile.delete()
            }

            // 同步文件到磁盘，确保数据持久化
            FileIOUtil.fileSync()

            // ---------- ⑤ 保留最近 10 份时间戳备份，删除最旧 ----------
            trimBackupFiles(strFilepath, strFileName)

            return true
        } catch (e: IOException) {
            e.printStackTrace()
            // 写失败：删除缓冲文件，保留时间戳备份以便找回
            try {
                if (tmpFile.exists()) tmpFile.delete()
            } catch (ignored: Exception) {
            }
        } finally {
            if (dos != null) {
                try {
                    dos.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return false
    }

    /**
     * 写操作前备份当前 world_pad.dat 为带时间戳的备份文件。
     *
     * 命名：world_pad_bak_yyyyMMdd_HHmmss_SSS.dat
     * - 与原文件同目录，实现按地图隔离
     * - 含毫秒确保唯一，定宽前缀确保可按文件名排序
     *
     * 若原文件不存在（首次创建）则跳过备份。
     * 备份失败仅记录，不影响主保存流程。
     */
    private fun backupWorldFile(strFilepath: String, strFileName: String) {
        try {
            val src = File(strFilepath + File.separator + strFileName)
            if (!src.exists()) return

            val ts = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
            val backupName = "${strFileName.replace(".dat", "")}_bak_$ts.dat"
            val backupFile = File(strFilepath + File.separator + backupName)
            src.copyTo(backupFile, overwrite = false)
        } catch (e: Exception) {
            // 备份是附加保护，失败不阻断保存
            e.printStackTrace()
        }
    }

    /**
     * 保留最近 MAX_BACKUP_COUNT 份时间戳备份，删除最旧的。
     * 只操作 world_pad_bak_*.dat，绝不误删正式文件或 .tmp 缓冲。
     */
    private fun trimBackupFiles(strFilepath: String, strFileName: String) {
        try {
            val dir = File(strFilepath)
            if (!dir.exists() || !dir.isDirectory) return

            val prefix = strFileName.replace(".dat", "") + "_bak_"
            val backups = dir.listFiles { _, name ->
                name.startsWith(prefix) && name.endsWith(".dat")
            }?.toMutableList() ?: return

            // 按文件名（时间戳）升序排序，最小的最先（最旧）
            backups.sortBy { it.name }

            while (backups.size > MAX_BACKUP_COUNT) {
                val oldest = backups.removeAt(0)
                try {
                    oldest.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        /** 每个地图最多保留的时间戳备份份数 */
        private const val MAX_BACKUP_COUNT = 5
    }
}
