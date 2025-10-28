package com.siasun.dianshi.network.download

import com.siasun.dianshi.framework.log.LogUtil
import com.pnc.core.network.api.ApiInterface
import com.pnc.core.network.constant.BASE_URL
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream


/**
 * 文件下载
 */

class DownLoad {
    private val client = OkHttpClient.Builder().addInterceptor { chain ->
        val request =
            chain.request().newBuilder()
                .addHeader("Content-Type", "application/octet-stream")
                .addHeader("Accept", "*/*")
                .addHeader("Connection", "keep-alive")
                .build()
        chain.proceed(request)
    }.build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder().client(client).baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()).build()
    }

    private val fileDownloadService: ApiInterface = retrofit.create(ApiInterface::class.java)

    @OptIn(DelicateCoroutinesApi::class)
    fun downloadFile(
        url: String,
        outputFile: File,
        onStart: () -> Unit,
        onProgressUpdate: (progress: Int) -> Unit,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        fileDownloadService.downloadFile(url).enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>
            ) {
                if (response.isSuccessful) {

                    response.body()?.let { responseBody ->
                        GlobalScope.launch(Dispatchers.IO) {
                            var inputStream: InputStream? = null
                            var outputStream: OutputStream? = null
                            try {
                                onStart()
                                val fileSize = responseBody.contentLength()
                                LogUtil.d("获取压缩文件总长度 fileSize $fileSize")
                                inputStream = responseBody.byteStream()
                                outputStream = FileOutputStream(outputFile)

                                val data = ByteArray(4096)
                                var totalBytesRead: Long = 0
                                var bytesRead: Int

                                while (inputStream.read(data).also { bytesRead = it } != -1) {
                                    outputStream.write(data, 0, bytesRead)
                                    totalBytesRead += bytesRead
                                    // 更新进度
                                    val progress = (totalBytesRead * 100 / fileSize).toInt()
                                    onProgressUpdate(progress)
                                }
                                outputStream.flush()
                                onSuccess()
                            } catch (ex: Exception) {
                                LogUtil.e(" 下载文件异常 ex${ex}")
                            } finally {
                                inputStream?.close()
                                outputStream?.close()
                            }
                        }

                    } ?: onFailure(Exception("Empty response body"))
                } else {
                    onFailure(Exception("Failed to download file: ${response.code()}"))
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                onFailure(t)
            }
        })
    }
}