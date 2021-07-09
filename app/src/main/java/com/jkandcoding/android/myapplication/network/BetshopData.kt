package com.jkandcoding.android.myapplication.network

import com.google.android.gms.maps.model.LatLng

data class BetshopData(
    val name: String,
    val address: String,
    val city: String,
    val county: String,
    val location: BetshopLocation
) {
    data class BetshopLocation(
        val lng: Double,
        val lat: Double
    )

    fun getPosition(): LatLng {
        return LatLng(location.lat, location.lng)
    }

}
