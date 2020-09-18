package com.example.covid_tracker

import retrofit2.Call
import retrofit2.http.GET

interface CovidService
{
    @GET("us/daily.json")
    fun getnationaldata(): Call<List<CovidData>>

    @GET("states/daily.json")
    fun getStateData():Call<List<CovidData>>
}