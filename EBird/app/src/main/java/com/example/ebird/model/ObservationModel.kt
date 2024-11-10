package com.example.ebird.model

data class ObservationModel(
    val speciesCode: String="",
    val comName: String="",
    val sciName: String="",
    val locId: String="",
    val locName: String="",
    val obsDt: String="",
    val howMany: Int=0,
    val lat: Double=0.0,
    val lng: Double=0.0,
    val obsValid: Boolean=false,
    val obsReviewed: Boolean=false,
    val locationPrivate: Boolean=false,
    val subId: String="",
    var isFav:Boolean=false
)