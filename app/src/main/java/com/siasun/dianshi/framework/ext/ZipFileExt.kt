package com.siasun.dianshi.framework.ext

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * @Author: CheFuX1n9
 * @Date: 2024/8/28 15:09
 * @Description: 文件压缩扩展
 */
fun zipFile(src: File): File {
    return zipFile(src, src.name)
}

fun zipFile(src: File, name: String): File {
    val dist = File(src.parentFile, "${name}.zip")
    zipFile(src, dist)
    return dist
}

fun zipFile(src: File, dist: File) {
    if (dist.exists()) {
        dist.delete()
    } else {
        dist.parentFile?.mkdirs()
    }
    val fos = FileOutputStream(dist)
    val zos = ZipOutputStream(fos)
    addEntry("", src, zos)
    zos.close()
}

private fun addEntry(dirs: String, src: File, zos: ZipOutputStream) {
    if (src.isDirectory) {
        val files = src.listFiles()
        if (files == null) {
            return
        } else if (files.isEmpty()) {
            zos.putNextEntry(
                ZipEntry(
                    String(
                        "${dirs}${src.name}/".toByteArray(Charsets.ISO_8859_1),
                        Charset.forName("GB2312")
                    )
                )
            )
            zos.flush()
            zos.closeEntry()
        } else {
            for (file in files) {
                addEntry("${dirs}${src.name}/", file, zos)
            }
        }
    } else {
        val buffer = ByteArray(1024)
        val fis = FileInputStream(src)
        zos.putNextEntry(
            ZipEntry(
                String(
                    "${dirs}${src.name}".toByteArray(Charsets.ISO_8859_1), Charset.forName("GB2312")
                )
            )
        )
        var len = fis.read(buffer)
        while (len > 0) {
            zos.write(buffer, 0, len)
            len = fis.read(buffer)
        }
        zos.flush()
        zos.closeEntry()
        fis.close()
    }
}