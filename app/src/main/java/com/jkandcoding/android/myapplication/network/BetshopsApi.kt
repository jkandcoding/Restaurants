package com.jkandcoding.android.myapplication.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface BetshopsApi {

    companion object {
        const val BASE_URL = "https://interview.superology.dev"
      //  const val CLIENT_ID = BuildConfig.GOOGLE_MAPS_API_KEY
    }

    @GET("/betshops")
    suspend fun searchBetshops(
        @Query("boundingBox", encoded = true) boundingBox: String
// ): Response<List<BetshopData>>
 ): Response<BetshopResponse>


}