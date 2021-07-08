package com.jkandcoding.android.myapplication

import android.util.Log
import com.jkandcoding.android.myapplication.network.BetshopResponse
import com.jkandcoding.android.myapplication.network.BetshopsApi
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BetshopsRepository @Inject constructor(
    private val betshopsApi: BetshopsApi
) {
   // suspend fun getBetshops(latLonMapView: String): Response<BetshopsResponse> {
   // suspend fun getBetshops(latLonMapView: String): Response<List<BetshopData>> {
    suspend fun getBetshops(latLonMapView: String): Response<BetshopResponse> {
        Log.d("hghgh", "repository: boundingBox: " + latLonMapView)
      //  Log.d("hghgh", "repository -> response: " + betshopsApi.searchBetshops(latLonMapView).toString())
       return betshopsApi.searchBetshops(latLonMapView)
    }
}