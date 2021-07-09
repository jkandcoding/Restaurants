package com.jkandcoding.android.myapplication.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface BetshopsApi {

    companion object {
        const val BASE_URL = "https://interview.superology.dev"
    }

    @GET("/betshops")
    suspend fun searchBetshops(
        @Query("boundingBox", encoded = true) boundingBox: String
    ): Response<BetshopResponse>


}