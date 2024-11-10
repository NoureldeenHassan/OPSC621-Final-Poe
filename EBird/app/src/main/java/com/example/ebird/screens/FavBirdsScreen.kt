package com.example.ebird.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ebird.model.ObservationModel
import com.example.ebird.navigation.NavigationItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson

@Composable
fun FavBirdsScreen(navController: NavController){
    val context = LocalContext.current
    val birdsListFirebase = remember { mutableStateListOf<ObservationModel>() }


    LaunchedEffect(Unit) {
        fetchFavoritesFromFirebase(birdsListFirebase)
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
                            if (!isFavourite.value) {
                                birdsListFirebase.remove(currentItem)
                            }
                        }, interactionSource = remember { MutableInteractionSource() }))
            }
        }
    }
}

fun fetchFavoritesFromFirebase(birdsListFirebase: MutableList<ObservationModel>) {
    val database = FirebaseDatabase.getInstance()
    val observationsRef = database.getReference("observations")

    // Query only items where isFav is true
    observationsRef.orderByChild("fav").equalTo(true).addListenerForSingleValueEvent(object :
        ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            birdsListFirebase.clear() // Clear the list to avoid duplication
            for (childSnapshot in snapshot.children) {
                val observation = childSnapshot.getValue(ObservationModel::class.java)
                observation?.let { birdsListFirebase.add(it) }
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("Firebase", "Error fetching favorite observations: ${error.message}")
        }
    })
}
