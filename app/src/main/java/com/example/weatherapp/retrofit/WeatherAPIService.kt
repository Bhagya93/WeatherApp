package com.example.weatherapp.retrofit

import com.example.weatherapp.model.WeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherAPIService {

    @GET("weather")
    suspend fun getWeatherDetails(
        @Query("q") q: String,
        @Query("appid") appid: String
    ): Response<WeatherResponse>
}