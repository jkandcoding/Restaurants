package com.jkandcoding.android.myapplication.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jkandcoding.android.myapplication.BetshopsRepository
import com.jkandcoding.android.myapplication.network.BetshopResponse
import com.jkandcoding.android.myapplication.other.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class BetshopsViewModel @Inject constructor(
    private val repository: BetshopsRepository
) : ViewModel() {

    private lateinit var latLonMapView: String
    private val _res = MutableLiveData<Resource<BetshopResponse>>()

    val res: LiveData<Resource<BetshopResponse>>
        get() = _res

    private fun getBetshops(latLonMapView: String) = viewModelScope.launch {
        try {
            _res.postValue(Resource.loading(null))
            repository.getBetshops(latLonMapView).let {
                if (it.isSuccessful) {
                    _res.postValue(Resource.success(it.body()))
                } else {
                    _res.postValue(Resource.error(it.raw().message(), null))
                }
            }
        } catch (e: IOException) {
            _res.postValue(Resource.error(e.localizedMessage, null))
            e.printStackTrace()
        }
    }

    fun setLanLonMapView(latLonMapViewFromActivity: String) {
        this.latLonMapView = latLonMapViewFromActivity
        getBetshops(latLonMapView)
    }

}
