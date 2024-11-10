package com.example.ebird.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import com.example.ebird.model.ObservationModel
import com.example.ebird.utils.SharedPrefManager
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun CurrentLocationMapView(modifier: Modifier = Modifier, observationModel: ObservationModel) {
    val context = LocalContext.current
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val destinationLocation = LatLng(observationModel.lat, observationModel.lng)
    Log.d("DirectionAPI", "Model: $observationModel")

    // Request location permissions and start real-time location updates if granted
    RequestLocationPermissions {
        startLocationUpdates(context) { location ->
            location?.let {
                userLocation = LatLng(it.latitude, it.longitude)
            }
        }
    }

    // Store route in state and update it when userLocation changes
    var route by remember { mutableStateOf(emptyList<LatLng>()) }

    LaunchedEffect(userLocation) {
        userLocation?.let {
            route = getDirections(context, it, destinationLocation)
        }
    }

    // Display the GoogleMapView if userLocation is available
    userLocation?.let { currentLocation ->
        GoogleMapView(
            modifier = modifier,
            location = currentLocation,
            route = route,
            isKilometers = SharedPrefManager(context).getSwitchStatus()
        )
    }
}

@SuppressLint("MissingPermission")
fun startLocationUpdates(context: Context, onLocationReceived: (Location?) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val locationRequest = LocationRequest.create().apply {
        interval = 5000 // Update every 5 seconds
        fastestInterval = 2000 // Fastest update every 2 seconds
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            p0.lastLocation?.let { onLocationReceived(it) }
        }
    }

    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
}

@Composable
fun RequestLocationPermissions(onGranted: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onGranted()
        } else {
            Toast.makeText(
                context,
                "Location permission is required to access your location.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

@Composable
fun GoogleMapView(
    modifier: Modifier = Modifier,
    location: LatLng,
    zoomLevel: Float = 15f,
    route: List<LatLng>,
    isKilometers: Boolean = false // Flag to determine if the distance should be in miles or kilometers
) {
    // Calculate the distance between current location and the destination
    val distance = remember(location) {
        if (isKilometers) calculateDistance(location, route.lastOrNull())
        else  calculateDistanceInMiles(location, route.lastOrNull())
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(location, zoomLevel)
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = rememberMarkerState(position = location),
                title = "You are here"
            )

            // Draw route polyline if available
            if (route.isNotEmpty()) {
                Polyline(
                    points = route,
                    color = Color.Blue,
                    width = 5f
                )
            }
            Log.d("DirectionApi", "Route: $route")
        }

        // Display the distance
        if (route.isNotEmpty()) {
            Text(
                text = if (!isKilometers)
                    "Distance: ${String.format("%.2f", distance)} miles"
                else
                    "Distance: ${String.format("%.2f", distance)} km",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
                    .padding(8.dp)
            )
        }

        // Add a FloatingActionButton for moving to the current location
        FloatingActionButton(
            onClick = {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(location, zoomLevel)
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp, bottom = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Current Location"
            )
        }
    }
}

// Function to calculate the distance between two LatLng points in kilometers
fun calculateDistance(start: LatLng, end: LatLng?): Double {
    if (end == null) return 0.0

    val startLat = Math.toRadians(start.latitude)
    val startLng = Math.toRadians(start.longitude)
    val endLat = Math.toRadians(end.latitude)
    val endLng = Math.toRadians(end.longitude)

    // Haversine formula
    val dLat = endLat - startLat
    val dLng = endLng - startLng
    val a = sin(dLat / 2).pow(2) + cos(startLat) * cos(endLat) * sin(dLng / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    val radius = 6371 // Earth's radius in kilometers

    return radius * c
}

// Function to calculate the distance between two LatLng points in miles
fun calculateDistanceInMiles(start: LatLng, end: LatLng?): Double {
    if (end == null) return 0.0

    val startLat = Math.toRadians(start.latitude)
    val startLng = Math.toRadians(start.longitude)
    val endLat = Math.toRadians(end.latitude)
    val endLng = Math.toRadians(end.longitude)

    // Haversine formula
    val dLat = endLat - startLat
    val dLng = endLng - startLng
    val a = Math.sin(dLat / 2).pow(2) + Math.cos(startLat) * Math.cos(endLat) * Math.sin(dLng / 2).pow(2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    val radius = 3958.8 // Earth's radius in miles

    return radius * c
}




suspend fun getDirections(context: Context, origin: LatLng, destination: LatLng): List<LatLng> {
    val apiKey = "AIzaSyBENIuQHml2SQrTiQIOFiuMDrJKd81l1nw"
    val url =
        "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&key=$apiKey"

    val response = withContext(Dispatchers.IO) {
        URL(url).readText()
    }

    Log.d("DirectionsAPI", "Response: $response")

    val jsonResponse = JSONObject(response)
    val routes = jsonResponse.optJSONArray("routes")

    if (routes != null && routes.length() > 0) {
        val overviewPolyline =
            routes.getJSONObject(0).getJSONObject("overview_polyline").getString("points")
        val decodedPath = PolyUtil.decode(overviewPolyline)

        Log.d("DirectionsAPI", "Decoded path: $decodedPath")
        return decodedPath
    } else {
        Log.e("DirectionsAPI", "No routes found in the response")
    }

    return emptyList()
}