package iss.nus.edu.sg.webviews.binitrightmobileapp.network

//object RetrofitClient {
//
//    private const val BASE_URL = "http://10.0.2.2:8080/"
//    private lateinit var api: ApiService
//
//    fun init(context: Context) {
//        val logging = HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BODY
//        }
//
//        val client = OkHttpClient.Builder()
//            .addInterceptor(logging)
//            .addInterceptor(AuthInterceptor(context.applicationContext))
//            .connectTimeout(30, TimeUnit.SECONDS)
//            .readTimeout(30, TimeUnit.SECONDS)
//            .build()
//
//        api = Retrofit.Builder()
//            .baseUrl(BuildConfig.BASE_URL)
//            .client(client)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//            .create(ApiService::class.java)
//    }
//
//    fun apiService(): ApiService = api
//}

import android.content.Context
import com.google.gson.GsonBuilder
import iss.nus.edu.sg.webviews.binitrightmobileapp.ApiService
import iss.nus.edu.sg.webviews.binitrightmobileapp.Model.AuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"

    private lateinit var apiServiceInstance: ApiService

    fun init(context: Context) {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val gson = GsonBuilder()
            .setLenient()
            .create()

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(AuthInterceptor(context.applicationContext))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        apiServiceInstance = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    val instance: ApiService
        get() = apiServiceInstance

    // Add this for backward compatibility
    fun apiService(): ApiService = apiServiceInstance
}
