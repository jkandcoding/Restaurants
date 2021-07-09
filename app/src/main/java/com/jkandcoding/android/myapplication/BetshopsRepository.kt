package com.jkandcoding.android.myapplication

import com.jkandcoding.android.myapplication.network.BetshopResponse
import com.jkandcoding.android.myapplication.network.BetshopsApi
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BetshopsRepository @Inject constructor(
    private val betshopsApi: BetshopsApi
) {
    suspend fun getBetshops(latLonMapView: String): Response<BetshopResponse> {
        return betshopsApi.searchBetshops(latLonMapView)
    }
}