package com.example.tweety2


import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class User(
    val id: String,
    val username: String,
    val email: String,
    val created_at: String?,
)

@JsonClass(generateAdapter = true)
data class RegisterRequest (
    val username: String,
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginRequest (
    val usernameOrEmail: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse (
    val token: String,
    val user: User
)



@JsonClass(generateAdapter = true)
data class GetEventsRequest (
    val events: List<Event>
)

@JsonClass(generateAdapter = true)
data class EventRequest (
    val stopId: String,
    val type: String,
    val description: String
)

@JsonClass(generateAdapter = true)
data class EventResponse (
    val id: String,
    val stopId: String,
    val type: String,
    val description: String,
    val timestamp: String,
    val created_by: String,
    val likes: Int,
    val dislikes: Int

)



@JsonClass(generateAdapter = true)
data class PoznanResponse (
    val id: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val zone: String,
    val route_type: String,
    val headsigns: String,
    val stop_name: String
)
@JsonClass(generateAdapter = true)
data class LikeDislikeResponse(
    val message: String,
    val likes: Int,
    val dislikes: Int
)

data class Event (
    val id: String,
    val stopId: String,
    val stopName: String,
    val type: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int,
    val dislikes: Int
)



interface ApiService {
    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<User>

    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("auth/logout")
    fun logout(@Header("Authorization") token: String,): Call<Void>

    @GET("auth/me")
    fun me(@Header("Authorization") token: String,): Call<User>

    @GET("events")
    fun getEvents(): Call<GetEventsRequest>


    @GET("events")
    fun getFilteredEvents(
        @Query("type") type: String?,
        @Query("timestamp") timestamp: String?
    ): Call<GetEventsRequest>

    @GET("events/{id}")
    fun getEvent(@Path("id") id: String): Call<EventResponse>

    @POST("events")
    fun createEvent(
        @Header("Authorization") token: String,
        @Body request: EventRequest
    ): Call<EventResponse>

    @GET("poznan")
    fun getStopLocation(): Call<List<PoznanResponse>>
    @POST("events/{id}/like")
    fun likeEvent(
        @Header("Authorization") token: String,
        @Path("id") eventId: String
    ): Call<LikeDislikeResponse>

    @POST("events/{id}/dislike")
    fun dislikeEvent(
        @Header("Authorization") token: String,
        @Path("id") eventId: String
    ): Call<LikeDislikeResponse>
}


object RetrofitClient{
    private const val BASE_URL = "http://87.205.0.172/chpq2/v1/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}