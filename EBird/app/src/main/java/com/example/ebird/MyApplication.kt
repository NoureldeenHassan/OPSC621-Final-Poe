package com.example.ebird

import android.app.Application
import com.example.ebird.api.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MyApplication : Application() {

    companion object {
        lateinit var auth: FirebaseAuth
        private val _birdApiKey = MutableStateFlow<String?>(null) // Mutable state to hold the API key
        val birdApiKey: StateFlow<String?> get() = _birdApiKey // Immutable state to expose the API key
    }

    override fun onCreate() {
        super.onCreate()
        auth = Firebase.auth
        fetchApiKeyAndObservations()
    }

    private fun fetchApiKeyAndObservations() {
        // Fetch the API key from Firebase Realtime Database
        val database = FirebaseDatabase.getInstance().getReference("birdObservationApiKey")

        // This runs in the background, so we launch it in a coroutine
        MainScope().launch(Dispatchers.IO) {
            database.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // If the API key exists in the database, update the StateFlow
                    snapshot.getValue(String::class.java)?.let {
                        _birdApiKey.value = it
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle cancellation or error if necessary
                }
            })
        }
    }
}