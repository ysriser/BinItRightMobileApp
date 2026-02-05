package iss.nus.edu.sg.webviews.binitrightmobileapp.network

import android.content.Context
import com.google.gson.GsonBuilder
import iss.nus.edu.sg.webviews.binitrightmobileapp.ApiService
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.AuthInterceptor
import iss.nus.edu.sg.webviews.binitrightmobileapp.BuildConfig
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.AuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var api: ApiService? = null
    private const val BASE_URL = "http://10.0.2.2:8080/"

    fun init(context: Context) {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        val gson = GsonBuilder()
            .setLenient()
            .create()

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(AuthInterceptor(context))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
        api = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        api = retrofit.create(ApiService::class.java)
    }

    fun apiService(): ApiService {
        return api ?: throw IllegalStateException("RetrofitClient not initialized. Call init() first.")
    }

    val instance: ApiService
        get() = apiService()
}
