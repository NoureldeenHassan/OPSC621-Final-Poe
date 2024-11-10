package com.example.ebird.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ebird.MyApplication
import com.example.ebird.api.NetworkResult
import com.example.ebird.api.RetrofitInstance
import com.example.ebird.model.ObservationModel
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BirdObservationViewModel : ViewModel() {
    private val _observations = MutableLiveData<NetworkResult<List<ObservationModel>>>()
    val observations: LiveData<NetworkResult<List<ObservationModel>>> get() = _observations

    init {
        // Observe birdApiKey and trigger API call when it's available
        viewModelScope.launch {
            MyApplication.birdApiKey.collect { apiKey ->
                apiKey?.let {
                    fetchObservations(it) // Fetch observations once the API key is available
                }
            }
        }
    }
    private fun fetchObservations(apiKey: String) {
        _observations.value = NetworkResult.Loading()  // Set loading state

        RetrofitInstance.api.getRecentObservations(apiKey).enqueue(object : Callback<List<ObservationModel>> {
            override fun onResponse(call: Call<List<ObservationModel>>, response: Response<List<ObservationModel>>) {
                if (response.isSuccessful && response.body() != null) {
                    val observations = response.body()!!
                    // Limit to the first 100 items
                    _observations.value = NetworkResult.Success(observations.take(100))
                } else {
                    _observations.value = NetworkResult.Error("Failed to load data")
                }
            }
            override fun onFailure(call: Call<List<ObservationModel>>, t: Throwable) {
                _observations.value = NetworkResult.Error("Network error: ${t.message}")
            }
        })
    }
}