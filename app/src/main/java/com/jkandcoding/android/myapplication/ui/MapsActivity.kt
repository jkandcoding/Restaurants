package com.jkandcoding.android.myapplication.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.jkandcoding.android.myapplication.R
import com.jkandcoding.android.myapplication.databinding.ActivityMapsBinding
import com.jkandcoding.android.myapplication.map.PermissionUtils.PermissionUtils.isPermissionGranted
import com.jkandcoding.android.myapplication.map.PermissionUtils.PermissionUtils.requestPermission
import com.jkandcoding.android.myapplication.network.BetshopData
import com.jkandcoding.android.myapplication.other.Status
import dagger.hilt.android.AndroidEntryPoint
import java.time.*
import kotlin.random.Random


@AndroidEntryPoint
class MapsActivity : AppCompatActivity(), OnRequestPermissionsResultCallback, OnMapReadyCallback,
    GoogleMap.OnCameraIdleListener, GoogleMap.OnInfoWindowCloseListener,
    GoogleMap.OnMarkerClickListener {

    private val viewModel: BetshopsViewModel by viewModels()
    private lateinit var binding: ActivityMapsBinding

    private var permissionDenied = false
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // betshops
    // all betshops in Germany
    private var allBetshops: ArrayList<BetshopData> = arrayListOf()

    // betshops that currently have marker on the map (for whole Germany)
    private val shownBetshops: ArrayList<BetshopData> = arrayListOf()

    // betshops whose locations are inside current boundingBox; the list is set after OnCameraIdle()
    private var newList: ArrayList<BetshopData> = arrayListOf()

    // betshops which already have markers on current boundingBox
    private var markersOnCurrentScreen: ArrayList<BetshopData> = arrayListOf()

    // markers
    // list of all markers which are set on the map (for whole Germany)
    private var markersArray: ArrayList<Marker> = arrayListOf()

    // infoWindow
    private var infoWindow: ViewGroup? = null
    private var infoWindowButtonListenerRoute: OnInfoWindowElemTouchListener? = null
    private var infoWindowButtonListenerClose: OnInfoWindowElemTouchListener? = null

    private lateinit var btnRoute: ImageButton
    private lateinit var btnClose: ImageButton
    private lateinit var tvName: TextView
    private lateinit var tvAdress: TextView
    private lateinit var tvCity: TextView
    private lateinit var tvCounty: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvHours: TextView

    companion object {
        /**
         * Request code for location permission request.
         * @see .onRequestPermissionsResult
         */
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

        private const val DEFAULT_ZOOM = 15
        private const val GERMANY_BOUNDING_BOX = "55.099161,15.0419319,47.2701114,5.8663153"

        // Munich:
        private val DEFAULT_LOCATION = LatLng(48.137154, 11.576124)

        // for making the marker not clickable
        private const val USER_TAG = "userMarker"

        // numbers of markers showed on screen based on zoom level
        private const val zoom3AndLess = 2
        private const val zoom3_4 = 4
        private const val zoom4_6 = 7
        private const val zoom6_7 = 10
        private const val zoom7_14 = 15

        // java.time
        @RequiresApi(Build.VERSION_CODES.O)
        private val GERMAN_OFFSET = ZoneOffset.ofHours(2)

        @RequiresApi(Build.VERSION_CODES.O)
        private val START_TIME: OffsetTime = LocalTime.of(8, 0).atOffset(GERMAN_OFFSET)

        @RequiresApi(Build.VERSION_CODES.O)
        private val END_TIME: OffsetTime = LocalTime.of(16, 0).atOffset(GERMAN_OFFSET)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Construct a FusedLocationProviderClient - for getting user's location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        getBetshopsFromApi()

        binding.btnRetry.setOnClickListener {
            getBetshopsFromApi()
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("PotentialBehaviorOverride")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.uiSettings.isZoomControlsEnabled = true

        mMap.setOnCameraIdleListener(this)
        mMap.setOnMarkerClickListener(this)
        mMap.setOnInfoWindowCloseListener(this)

        initInfoWindowElements()
        setUserLocation()
        setListenerOnInfoWindowRouteBtn()
        setListenerOnInfoWindowCloseBtn()
        setCustomInfoWindowAdapter()
    }

    private fun initInfoWindowElements() {
        // MapWrapperLayout initialization
        // 39 - default marker height
        // 20 - offset between the default InfoWindow bottom edge and it's content bottom edge
        binding.mapFrameLayout.init(mMap, getPixelsFromDp(this, (39 + 20).toFloat()))

        infoWindow = layoutInflater.inflate(R.layout.info_window_layout, null) as ViewGroup?
        btnRoute = infoWindow?.findViewById(R.id.btn_route) as ImageButton
        btnClose = infoWindow?.findViewById(R.id.btn_close) as ImageButton
        tvName = infoWindow?.findViewById(R.id.tv_name)!!
        tvAdress = infoWindow?.findViewById(R.id.tv_adress)!!
        tvCity = infoWindow?.findViewById(R.id.tv_city)!!
        tvCounty = infoWindow?.findViewById(R.id.tv_county)!!
        tvPhone = infoWindow?.findViewById(R.id.tv_phone)!!
        tvHours = infoWindow?.findViewById(R.id.tv_hours)!!
    }

    @SuppressLint("PotentialBehaviorOverride")
    private fun setCustomInfoWindowAdapter() {
        mMap.setInfoWindowAdapter(object : InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? {
                return null
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun getInfoContents(marker: Marker): View {
                // Setting up the infoWindow with current's marker info
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_active))

                (infoWindowButtonListenerRoute as OnInfoWindowElemTouchListener).setMarker(marker)
                (infoWindowButtonListenerClose as OnInfoWindowElemTouchListener).setMarker(marker)

                val nowTimeWithoutOffset = OffsetTime.now(ZoneOffset.UTC)
                val germanNowTime = nowTimeWithoutOffset.withOffsetSameInstant(GERMAN_OFFSET)

                var betshopInfo: BetshopData? = null
                // getting betshop info by comparing currently pressed marker's location with all betshop's locations
                for (bs in allBetshops) {
                    if (marker.position.equals(bs.getPosition())) {
                        betshopInfo = bs
                    }
                }

                if (betshopInfo != null) {
                    tvName.text = betshopInfo.name.trim()
                    tvAdress.text = betshopInfo.address.trim()
                    tvCity.text = betshopInfo.city.trim()
                    tvCounty.text = betshopInfo.county.trim()

                    if (germanNowTime.isAfter(START_TIME) && germanNowTime.isBefore(END_TIME)) {
                        tvHours.text = getString(R.string.open_now, END_TIME.hour)
                        tvHours.setTextColor(
                            ContextCompat.getColor(
                                this@MapsActivity,
                                R.color.teal_200
                            )
                        )
                    } else {
                        tvHours.text = getString(R.string.opens_tomorrow, START_TIME.hour)
                    }
                }

                // We must call this to set the current marker and infoWindow references
                // to the MapWrapperLayout
                binding.mapFrameLayout.setMarkerWithInfoWindow(marker, infoWindow)
                return infoWindow!!
            }
        })
    }

    private fun setListenerOnInfoWindowCloseBtn() {
        infoWindowButtonListenerClose = object : OnInfoWindowElemTouchListener(
            btnClose,
            ContextCompat.getDrawable(this, R.drawable.close_black),
            ContextCompat.getDrawable(this, R.drawable.close_pressed_gray)
        ) {
            override fun onClickConfirmed(v: View?, marker: Marker?) {
                marker?.hideInfoWindow()
                marker?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_normal))
            }
        }
        btnClose.setOnTouchListener(infoWindowButtonListenerClose)
    }

    private fun setListenerOnInfoWindowRouteBtn() {
        this.infoWindowButtonListenerRoute = object : OnInfoWindowElemTouchListener(
            btnRoute,
            ContextCompat.getDrawable(this, R.drawable.route_dark),
            ContextCompat.getDrawable(this, R.drawable.route_pressed_light)
        ) {
            override fun onClickConfirmed(v: View?, marker: Marker?) {
                // Getting markers location and setting navigation intent
                val lat = marker?.position?.latitude
                val lon = marker?.position?.longitude

                val url = "waze://?ll=$lat, $lon&navigate=yes"
                val intentWaze = Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage("com.waze")

                val uriGoogle = "google.navigation:q=$lat,$lon"
                val intentGoogleNav = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(uriGoogle)
                ).setPackage("com.google.android.apps.maps")

                val title: String = this@MapsActivity.getString(R.string.app_name)
                val chooserIntent = Intent.createChooser(intentGoogleNav, title)
                val arr = arrayOfNulls<Intent>(1)
                arr[0] = intentWaze
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arr)
                chooserIntent.resolveActivity(packageManager)?.let {
                    startActivity(chooserIntent)
                }
            }
        }
        btnRoute.setOnTouchListener(infoWindowButtonListenerRoute)
    }

    private fun getPixelsFromDp(context: Context, dp: Float): Int {
        val scale: Float = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private fun setUserLocation() {
        if (!::mMap.isInitialized) return
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            // mMap.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM.toFloat()))
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true

            /*
           * Permission granted, moveCamera to current location and zoom in
     * Get the best and most recent location of the device, which may be null in rare
     * cases when a location is not available - then position to default location.
     */

            val locationResult = fusedLocationProviderClient.lastLocation
            var userLocation: Location?
            locationResult.addOnSuccessListener(this) { location: Location? ->
                userLocation = location

                if (userLocation != null) {
                    mMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(userLocation!!.latitude, userLocation!!.longitude),
                            DEFAULT_ZOOM.toFloat()
                        )
                    )
                } else {
                    Snackbar.make(
                        binding.rootView,
                        getString(R.string.no_current_location_found),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            // Permission to access the location is missing. Show rationale and request permission
            requestPermission(
                this, LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION, false
            )
        }
    }

    // Callback for the result from requesting permissions
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return
        }
        if (isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            // Enable the my location layer if the permission has been granted.
            setUserLocation()
        } else {
            // Permission was denied. Display an error message
            // Display the missing permission error dialog when the fragments resume.
            permissionDenied = true

            // add a marker in Munich and move the camera
            val userMarker =
                mMap.addMarker(MarkerOptions().position(DEFAULT_LOCATION))
            userMarker!!.tag = USER_TAG

            mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    DEFAULT_LOCATION,
                    DEFAULT_ZOOM.toFloat()
                )
            )
        }
    }

    private fun getBetshopsFromApi() {
        viewModel.setLanLonMapView(GERMANY_BOUNDING_BOX)
        viewModel.res.observe(this, { betshopResource ->
            when (betshopResource.status) {

                Status.SUCCESS -> {
                    binding.btnRetry.visibility = View.GONE
                    betshopResource.data?.let {
                        if (it.betshops.isNotEmpty()) {
                            allBetshops = it.betshops
                            // when we have all betshops data -> calling showMarkersBasedOnZoomLevel()
                            showMarkersBasedOnZoomLevel()
                            Snackbar.make(
                                binding.rootView,
                                getString(R.string.betshops_are_set),
                                Snackbar.LENGTH_SHORT
                            ).show()
                        } else {
                            // Status.SUCCESS but empty list
                            Snackbar.make(
                                binding.rootView,
                                getString(R.string.empty_betshop_list),
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                Status.LOADING -> {
                    binding.btnRetry.visibility = View.GONE
                }

                Status.ERROR -> {
                    binding.btnRetry.visibility = View.VISIBLE

                    Snackbar.make(
                        binding.rootView,
                        getString(R.string.cant_provide_data, betshopResource.message),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    /**
     * 1) determine currently zoom level
     * 2) set newList -> all betshops for current boundingBox
     * 3) set markersOnCurrentScreen -> markers which are already on current boundingBox
     * 4) betshopsForShow -> determine how many (and which one) markers we need to show with the help of
     *    setBetshopsForShow() function
     * 5) At the end we call showBetshops() function
     */
    private fun showMarkersBasedOnZoomLevel() {
        var betshopsForShow = ArrayList<BetshopData>()
        val currentZoomLevel = mMap.cameraPosition.zoom

        newList.clear()
        markersOnCurrentScreen.clear()

        val curScreenBB: LatLngBounds = mMap.projection
            .visibleRegion.latLngBounds

        for (bs in allBetshops) {
            if (curScreenBB.contains(bs.getPosition())) {
                newList.add(bs)
            }
        }

        for (bs in shownBetshops) {
            if (curScreenBB.contains(bs.getPosition())) {
                markersOnCurrentScreen.add(bs)
            }
        }

        // numberOfMarkers -> how many markers (fixed number) will be shown based on zoom level
        val numberOfMarkers: Int
        when {
            currentZoomLevel <= 3f -> {
                numberOfMarkers = zoom3AndLess
                betshopsForShow = setBetshopsForShow(numberOfMarkers)
            }
            3f < currentZoomLevel && currentZoomLevel <= 4f -> {
                numberOfMarkers = zoom3_4
                betshopsForShow = setBetshopsForShow(numberOfMarkers)
            }
            4f < currentZoomLevel && currentZoomLevel <= 6f -> {
                numberOfMarkers = zoom4_6
                betshopsForShow = setBetshopsForShow(numberOfMarkers)
            }
            6f < currentZoomLevel && currentZoomLevel <= 7f -> {
                numberOfMarkers = zoom6_7
                betshopsForShow = setBetshopsForShow(numberOfMarkers)
            }
            7f < currentZoomLevel && currentZoomLevel <= 14 -> {
                numberOfMarkers = zoom7_14
                betshopsForShow = setBetshopsForShow(numberOfMarkers)
            }
            currentZoomLevel > 14f -> {
                val (forDelete, _) = removeBetshopsFromNewListThatAreAlreadyOnTheMap(
                    newList,
                    shownBetshops
                )
                newList.removeAll(forDelete)
                betshopsForShow = newList
            }
        }
        showBetshops(betshopsForShow)
    }

    // algorithm for determinig number of betshops to show
    private fun setBetshopsForShow(
        numberOfMarkers: Int
    ): ArrayList<BetshopData> {
        val betshopsForShow: ArrayList<BetshopData> = arrayListOf()
        val betshopsForDelete: ArrayList<BetshopData> = arrayListOf()

        // 1)
        // first entry to activity, put certain amount of markers based on zoom level
        if (markersOnCurrentScreen.size == 0 && newList.size == 0) {
            return betshopsForShow
            // 2)
        } else if (markersOnCurrentScreen.size != 0 && newList.size == 0) {
            deleteMarkers(markersOnCurrentScreen)
            return betshopsForShow
            // 3)
        } else if (markersOnCurrentScreen.size == 0 && newList.size != 0) {
            if (newList.size >= numberOfMarkers) {
                val randomValues = List(numberOfMarkers) { Random.nextInt(0, newList.size - 1) }
                for (rv in randomValues) {
                    betshopsForShow.add(newList[rv])
                }
            } else {
                betshopsForShow.addAll(newList)
            }
            // 4)
        } else if (markersOnCurrentScreen.size != 0 && newList.size != 0) {
            // 4) a)
            if (markersOnCurrentScreen.size == newList.size) {
                return betshopsForShow
                // 4) b)
            } else if (markersOnCurrentScreen.size > newList.size) {
                mMap.clear()
                shownBetshops.clear()
                if (newList.size >= numberOfMarkers) {
                    val randomValues = List(numberOfMarkers) { Random.nextInt(0, newList.size - 1) }
                    for (rv in randomValues) {
                        betshopsForShow.add(newList[rv])
                    }
                } else {
                    betshopsForShow.addAll(newList)
                }
                // 4) c)
            } else if (markersOnCurrentScreen.size < newList.size) {
                // 4) c)*
                if (markersOnCurrentScreen.size == numberOfMarkers) {
                    return betshopsForShow
                    // 4) c)**
                } else if (markersOnCurrentScreen.size < numberOfMarkers) {
                    val (forDelete, fdsize) = removeBetshopsFromNewListThatAreAlreadyOnTheMap(
                        newList,
                        markersOnCurrentScreen
                    )
                    newList.removeAll(forDelete)
                    // 4)** #
                    if (numberOfMarkers - fdsize >= newList.size) {
                        betshopsForShow.addAll(newList)
                        // 4)** ##
                    } else {
                        val randomValues =
                            List(numberOfMarkers - fdsize) { Random.nextInt(0, newList.size - 1) }
                        for (i in randomValues) {
                            betshopsForShow.add(newList[i])
                        }
                    }
                    // 4) c)***
                } else if (markersOnCurrentScreen.size > numberOfMarkers) {
                    val randomValues =
                        List(markersOnCurrentScreen.size - numberOfMarkers) {
                            Random.nextInt(
                                0,
                                markersOnCurrentScreen.size - 1
                            )
                        }
                    for (i in randomValues) {
                        betshopsForDelete.add(markersOnCurrentScreen[i])
                    }
                    deleteMarkers(betshopsForDelete)
                    return betshopsForShow
                }
            }
        }
        return betshopsForShow
    }

    private fun showBetshops(betShopsForShow: List<BetshopData>) {
        betShopsForShow.forEach { betshop ->
            val marker = mMap.addMarker(
                MarkerOptions()
                    .title(betshop.name)
                    .position(LatLng(betshop.location.lat, betshop.location.lng))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_normal))
            )
            markersArray.add(marker!!)
        }
        shownBetshops.addAll(betShopsForShow)
    }

    // delete some markers if there is to many of them (e.g. when zooming-out)
    private fun deleteMarkers(betShopsForDelete: List<BetshopData>) {
        val markerArrayForDelete = ArrayList<Marker>()
        for (betshop in betShopsForDelete) {
            for (marker in markersArray) {
                if (marker.position == betshop.getPosition()) {
                    markerArrayForDelete.add(marker)
                }
            }
            shownBetshops.remove(betshop)
        }
        for (marker in markerArrayForDelete) {
            marker.remove()
        }
    }

    private fun removeBetshopsFromNewListThatAreAlreadyOnTheMap(
        newList: List<BetshopData>,
        shownBetshops: ArrayList<BetshopData>
    ): Pair<ArrayList<BetshopData>, Int> {
        val forDelete = ArrayList<BetshopData>()
        for (item in newList) {
            if (shownBetshops.contains(item)) {
                forDelete.add(item)
            }
        }
        return Pair(forDelete, forDelete.size)
    }

    // repeat whole process
    override fun onCameraIdle() {
        if (allBetshops.size != 0 || allBetshops.isNotEmpty()) {
            showMarkersBasedOnZoomLevel()
        }
    }

    // if closing infoWindow onTouch outside infoWindow
    override fun onInfoWindowClose(marker: Marker) {
        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_normal))
    }

    // to make userLocation marker not clickable, otherwise my custom infoWindow opens
    override fun onMarkerClick(marker: Marker): Boolean {
        return (marker.tag?.toString()).equals(USER_TAG)
    }

}