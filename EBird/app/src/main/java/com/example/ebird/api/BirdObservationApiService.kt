package com.example.ebird.api

import com.example.ebird.model.ObservationModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header

interface BirdObservationApiService {
    @GET("data/obs/ZA/recent")
    fun getRecentObservations(
        @Header("X-eBirdApiToken") apiKey: String
    ): Call<List<ObservationModel>>
}