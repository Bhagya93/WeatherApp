package com.example.weatherapp.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.weatherapp.model.WeatherResponse
import com.example.weatherapp.retrofit.WeatherAPIService
import com.example.weatherapp.utils.Constants
import javax.inject.Inject

class WeatherRepository @Inject constructor(private val weatherAPIService: WeatherAPIService) {

    private val _weatherData = MutableLiveData<WeatherResponse>()
    val weatherData: LiveData<WeatherResponse>
        get() = _weatherData

    suspend fun getWeatherDetails(city:String) {
        val result = weatherAPIService.getWeatherDetails(city, Constants.API_KEY)
            _weatherData.postValue(result.body())
    }
}