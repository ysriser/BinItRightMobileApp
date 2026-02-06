package iss.nus.edu.sg.webviews.binitrightmobileapp.network

import android.content.Context
import com.google.gson.GsonBuilder
import iss.nus.edu.sg.webviews.binitrightmobileapp.ApiService
import iss.nus.edu.sg.webviews.binitrightmobileapp.BuildConfig
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.AuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private lateinit var api: ApiService


    private val BASE_URL: String = "http://10.0.2.2:8080/"

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
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        api = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    fun apiService(): ApiService = api
}
