package com.example.ebird.screens

import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ebird.api.NetworkResult
import com.example.ebird.model.ObservationModel
import com.example.ebird.navigation.NavigationItem
import com.example.ebird.viewmodel.BirdObservationViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@Composable
fun BirdsListScreen(
    navController: NavController,
    birdObservationViewModel: BirdObservationViewModel
) {
    val birdsListState by birdObservationViewModel.observations.observeAsState()
    val context = LocalContext.current
    val birdsListFirebase = remember { mutableStateListOf<ObservationModel>() }


LaunchedEffect(Unit) {
    fetchDataFromFirebase(birdsListFirebase)
}
    // Display loading indicator while data is loading
    when (val birdsList = birdsListState) {
        is NetworkResult.Loading -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                CircularProgressIndicator()
            }
        }

        is NetworkResult.Error -> {
            Toast.makeText(
                context,
                birdsList.message ?: "Something went wrong",
                Toast.LENGTH_SHORT
            ).show()
        }

        is NetworkResult.Success -> {
            val birds = birdsList.data ?: emptyList()
            saveObservationsToFirebase(birds)

        }
        null -> {

        }
    }

    LazyColumn {
        items(birdsListFirebase.size) { index ->
            val currentItem = birdsListFirebase[index]
            val isFavourite = remember{ mutableStateOf(currentItem.isFav) }
            Column(
                modifier = Modifier
                    .padding(10.dp)
                    .background(
                        color = MaterialTheme.colorScheme.inversePrimary,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(10.dp)
                    .clickable(indication = null, onClick = {
                        val selectedBirdJsonString = Gson().toJson(currentItem)

                        navController.navigate("${NavigationItem.GoogleMap.route}?selectedBird=${selectedBirdJsonString}")
                    }, interactionSource = remember { MutableInteractionSource() })
            ) {
                Text(
                    text = "Name: ${currentItem.sciName}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                )
                Text(
                    text = "Location name: ${currentItem.locName}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                )
                Row(modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)) {
                    Text(
                        text = "Date: ${currentItem.obsDt}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "How many: ${currentItem.howMany}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "Latitude: ${currentItem.lat}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Longitude: ${currentItem.lng}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                )
                Text(if(!isFavourite.value) "Add to fav" else "Remove from fav", textDecoration = TextDecoration.Underline,
                    fontSize = 18.sp,   modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                        .clickable(indication = null, onClick = {
                            val newFavStatus = !currentItem.isFav
                            isFavourite.value=newFavStatus

                            currentItem.isFav = newFavStatus
                            toggleFavStatusInFirebase(currentItem.subId, newFavStatus)
                        }, interactionSource = remember { MutableInteractionSource() }))
            }
        }
    }
}
fun saveObservationsToFirebase(observations: List<ObservationModel>) {
    val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    val observationsRef = database.child("observations")

    // Step 1: Retrieve existing subIds from Firebase
    observationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            // Step 2: Get existing subIds in a Set
            val existingSubIds = snapshot.children.mapNotNull { it.key }.toSet()

            // Step 3: Filter out observations that are already in the database
            val newObservations = observations.filter { it.subId !in existingSubIds }

            // Step 4: Save each new observation to Firebase
            newObservations.forEach { observation ->
                observationsRef.child(observation.subId).setValue(observation)
                    .addOnSuccessListener {
                        Log.d("Firebase", "Observation ${observation.subId} saved successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firebase", "Failed to save observation ${observation.subId}", e)
                    }
            }

            Log.d("Firebase", "Total new observations saved: ${newObservations.size}")
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("Firebase", "Error fetching data", error.toException())
        }
    })
}
fun toggleFavStatusInFirebase(subId: String, isFav: Boolean) {
    val database = FirebaseDatabase.getInstance()
    val observationRef = database.getReference("observations/$subId")

    observationRef.child("fav").setValue(isFav).addOnCompleteListener { task ->
        if (task.isSuccessful) {
            Log.d("Firebase", "isFav status updated successfully.")
        } else {
            Log.e("Firebase", "Error updating isFav status: ${task.exception?.message}")
        }
    }
}


fun fetchDataFromFirebase(birdsListFirebase: SnapshotStateList<ObservationModel>) {
    val database = FirebaseDatabase.getInstance()
    val observationsRef = database.getReference("observations")

    observationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            birdsListFirebase.clear()
            snapshot.children.forEach { childSnapshot ->
                val observation = childSnapshot.getValue(ObservationModel::class.java)
                observation?.let { birdsListFirebase.add(it) }
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("Firebase", "Error fetching data: ${error.message}")
        }
    })
}

