package cn.latlaser.coordinate.network

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val exception: Exception? = null) : NetworkResult<Nothing>()
}

object HttpClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS) // 缩短连接超时以便快速反馈
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun <T> post(url: String, body: Any, responseClass: Class<T>): NetworkResult<T> = withContext(Dispatchers.IO) {
        val jsonBody = gson.toJson(body)
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseString = response.body?.string()
                if (response.isSuccessful && responseString != null) {
                    NetworkResult.Success(gson.fromJson(responseString, responseClass))
                } else {
                    NetworkResult.Error("HTTP错误码: ${response.code}, 信息: ${response.message}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            NetworkResult.Error("网络异常: ${e.localizedMessage ?: e.message ?: "未知错误"}", e)
        }
    }

    suspend fun <T> get(url: String, responseClass: Class<T>): NetworkResult<T> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseString = response.body?.string()
                if (response.isSuccessful && responseString != null) {
                    NetworkResult.Success(gson.fromJson(responseString, responseClass))
                } else {
                    NetworkResult.Error("HTTP错误码: ${response.code}, 信息: ${response.message}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            NetworkResult.Error("网络异常: ${e.localizedMessage ?: e.message ?: "未知错误"}", e)
        }
    }
}
