package com.example.data.api

import android.content.Context
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import retrofit2.Call
import retrofit2.Response

object ApiClient {
    private lateinit var apiService: ApiService

    fun init(context: Context) {
        val logging = HttpLoggingInterceptor { println(it) }
        logging.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val baseUrl = BuildConfig.AGENTRA_BACKEND_URL.ifBlank { "http://10.0.2.2:8080" }

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    val service: ApiService
        get() = if (::apiService.isInitialized) apiService else throw IllegalStateException("ApiClient not initialized. Call ApiClient.init(context) first.")
}

/**
 * Suspend-aware wrapper around Retrofit Call.enqueue / Call.execute.
 */
suspend fun <T> Call<T>.awaitResponse(): Response<T> = suspendCoroutine { cont ->
    enqueue(object : retrofit2.Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            cont.resume(response)
        }
        override fun onFailure(call: Call<T>, t: Throwable) {
            val errorResponse = Response.error<T>(
                500,
                okhttp3.ResponseBody.create(null, t.message ?: "Network error")
            )
            cont.resume(errorResponse)
        }
    })
}
