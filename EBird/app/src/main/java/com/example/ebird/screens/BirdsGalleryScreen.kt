package com.example.ebird.screens

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import com.example.ebird.model.BirdImageModel
import com.example.ebird.model.ObservationModel
import com.example.ebird.navigation.NavigationItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import java.util.Locale

@Composable
fun BirdsGalleryScreen(navController: NavController) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageName by remember { mutableStateOf("") }
    var imageLocation by remember { mutableStateOf("") }
    var birdImages by remember { mutableStateOf(listOf<BirdImageModel>()) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // Load images from Firebase Realtime Database on start
    LaunchedEffect(Unit) {
        fetchImagesFromFirebaseDatabase { fetchedImages ->
            birdImages = fetchedImages
        }
    }
val context= LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // LazyColumn to display bird images with name and location
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(birdImages.size) { item ->
                val birdImage = birdImages[item]
                val latLng= getLatLngFromAddress(context,birdImage.location)
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.inversePrimary,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(indication = null, onClick = {
                            val observationModel = latLng?.second?.let {
                                ObservationModel(
                                    sciName = birdImage.name,
                                    locName = birdImage.location,
                                    lat = latLng.first,
                                    lng = it
                                )
                            }
                            val selectedBirdJsonString = Gson().toJson(observationModel)

                            navController.navigate("${NavigationItem.GoogleMap.route}?selectedBird=${selectedBirdJsonString}")
                        },
                            interactionSource = remember { MutableInteractionSource() })
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(birdImage.uri),
                        contentDescription = birdImage.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.FillBounds
                    )
                    Text(
                        text = birdImage.name,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    Text(
                        text = birdImage.location,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 16.dp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button to select an image from the gallery
        Button(onClick = { galleryLauncher.launch("image/*") }) {
            Text(text = "Select Image from Gallery")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Display text fields for name and location if an image is selected
        selectedImageUri?.let {
            BasicTextField(
                value = imageName,
                onValueChange = { imageName = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(
                        Modifier.fillMaxWidth()
                    ) {
                        if (imageName.isEmpty()) Text("Enter image name")
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            BasicTextField(
                value = imageLocation,
                onValueChange = { imageLocation = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(
                        Modifier.fillMaxWidth()
                    ) {
                        if (imageLocation.isEmpty()) Text("Enter image location")
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Button to save the image along with name and location
            Button(
                onClick = {
                    if (imageName.isNotBlank() && imageLocation.isNotBlank()) {
                        saveImageToFirebase(selectedImageUri!!, imageName, imageLocation)
                        imageName = ""
                        imageLocation = ""
                        selectedImageUri = null
                    }
                }
            ) {
                Text("Save Image")
            }
        }
    }
}

// Function to upload image to Firebase Storage and save its info to Firebase Realtime Database
private fun saveImageToFirebase(imageUri: Uri, name: String, location: String) {
    val storageRef =
        FirebaseStorage.getInstance().reference.child("bird_images/${imageUri.lastPathSegment}")
    val databaseRef = FirebaseDatabase.getInstance().getReference("bird_images")

    storageRef.putFile(imageUri).addOnSuccessListener {
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            val birdImage = BirdImageModel(uri = uri.toString(), name = name, location = location)
            val newImageRef = databaseRef.push() // Automatically creates a new ID
            newImageRef.setValue(birdImage)
        }
    }
}

// Function to fetch images from Firebase Realtime Database
private fun fetchImagesFromFirebaseDatabase(onImagesFetched: (List<BirdImageModel>) -> Unit) {
    val databaseRef = FirebaseDatabase.getInstance().getReference("bird_images")

    databaseRef.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val imagesList = mutableListOf<BirdImageModel>()
            for (data in snapshot.children) {
                val birdImage = data.getValue(BirdImageModel::class.java)
                birdImage?.let { imagesList.add(it) }
            }
            onImagesFetched(imagesList)
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("FirebaseDatabase", "Error fetching images: ${error.message}")
        }
    })
}

fun getLatLngFromAddress(context: Context, address: String): Pair<Double, Double>? {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocationName(address, 1)
        if (addresses?.isNotEmpty() == true) {
            val location = addresses[0]
            Pair(location.latitude, location.longitude)
        } else {
            null // No address found
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null // In case of an error
    }
}
