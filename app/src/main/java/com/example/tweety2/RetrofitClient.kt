package com.example.tweety2


import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface PoznanApiService{
    @GET("mim/plan/map_service.html")
    suspend fun getTransportStops(
        @Query("mtype") mtype: String = "pub_transport",
        @Query("co") co: String = "cluster"
    ): String // API zwraca HTML/JSON, potrzebna będzie analiza odpowiedzi
}

object RetrofitClient{
    private const val BASE_URL = "https://www.poznan.pl/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: PoznanApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PoznanApiService::class.java)
    }
}