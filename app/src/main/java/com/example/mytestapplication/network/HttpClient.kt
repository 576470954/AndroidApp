package com.example.mytestapplication.network

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object HttpClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * 通用的 HTTP POST 请求处理函数
     * @param url 请求地址
     * @param body 请求对象（会被转为 JSON）
     * @param responseClass 返回结果的类型类
     */
    suspend fun <T> post(url: String, body: Any, responseClass: Class<T>): T? = withContext(Dispatchers.IO) {
        val jsonBody = gson.toJson(body)
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseString = response.body?.string()
                if (response.isSuccessful && responseString != null) {
                    gson.fromJson(responseString, responseClass)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 通用的 HTTP GET 请求
     */
    suspend fun <T> get(url: String, responseClass: Class<T>): T? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseString = response.body?.string()
                if (response.isSuccessful && responseString != null) {
                    gson.fromJson(responseString, responseClass)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
