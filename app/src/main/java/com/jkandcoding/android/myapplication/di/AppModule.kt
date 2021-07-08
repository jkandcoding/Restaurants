package com.jkandcoding.android.myapplication.di

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.ContextCompat
import com.jkandcoding.android.myapplication.network.BetshopsApi
import com.jkandcoding.android.myapplication.network.ConnectivityCheckingInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)

//object ConnectivityManager {
//    @Provides
//    fun provideConnectivityManager(
//        @ApplicationContext context: Context
//    ) = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//}

object AppModule {

    @Provides
    @Singleton
    fun provideRetrofit(connectivityManager: ConnectivityManager): Retrofit =
        Retrofit.Builder()
            .baseUrl(BetshopsApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder()
                .addInterceptor(ConnectivityCheckingInterceptor(connectivityManager))
                .build())
            .build()

    @Provides
    @Singleton
    fun provideBetshopsApi(retrofit: Retrofit): BetshopsApi =
        retrofit.create(BetshopsApi::class.java)

    @Provides
    fun provideConnectivityManager(
        @ApplicationContext context: Context
    ) = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
}