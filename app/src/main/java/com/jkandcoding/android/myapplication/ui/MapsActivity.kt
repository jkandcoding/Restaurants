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
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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
import com.jkandcoding.android.myapplication.R
import com.jkandcoding.android.myapplication.databinding.ActivityMapsBinding
import com.jkandcoding.android.myapplication.map.PermissionUtils.PermissionUtils.PermissionDeniedDialog.Companion.newInstance
import com.jkandcoding.android.myapplication.map.PermissionUtils.PermissionUtils.isPermissionGranted
import com.jkandcoding.android.myapplication.map.PermissionUtils.PermissionUtils.requestPermission
import com.jkandcoding.android.myapplication.network.BetshopData
import com.jkandcoding.android.myapplication.other.Status
import dagger.hilt.android.AndroidEntryPoint
import java.time.*
import kotlin.random.Random


@AndroidEntryPoint
class MapsActivity : AppCompatActivity(), OnRequestPermissionsResultCallback, OnMapReadyCallback,
    GoogleMap.OnCameraIdleListener, GoogleMap.OnInfoWindowCloseListener, GoogleMap.OnMarkerClickListener {

    private val viewModel: BetshopsViewModel by viewModels()
    private lateinit var binding: ActivityMapsBinding

    private var permissionDenied = false
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient


    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
//    private var lastKnownLocation: Location? =
//        null // can't be lateinit because of saveInstanceState
    private lateinit var lastKnownLocation: Location

    // betshops
    private var allBetshops: ArrayList<BetshopData> = arrayListOf()
    private val shownBetshops = ArrayList<BetshopData>()
    private var newList: ArrayList<BetshopData> = arrayListOf()

    // markers
    private var markersOnCurrentScreen: ArrayList<BetshopData> = arrayListOf()
    private var markersArray: ArrayList<Marker> = arrayListOf()

    // infoWindow
    private var infoWindow: ViewGroup? = null
    private var infoWindowButtonListener: OnInfoWindowElemTouchListener? = null
    private var infoWindowButtonListenerClose: OnInfoWindowElemTouchListener? = null

    // private var infoWindowButtonListenerClose: OnInfoWindowElemTouchListener? = null
    private lateinit var btn_route: ImageButton
    private lateinit var btn_close: ImageButton
    private lateinit var tv_name: TextView
    private lateinit var tv_adress: TextView
    private lateinit var tv_city: TextView
    private lateinit var tv_county: TextView
    private lateinit var tv_phone: TextView
    private lateinit var tv_hours: TextView


    companion object {
        /**
         * Request code for location permission request.
         * @see .onRequestPermissionsResult
         */
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

        //        private const val READ_EX_STORAGE_PERMISSION_REQUEST_CODE = 2
//        private const val WRITE_EX_STORAGE_PERMISSION_REQUEST_CODE = 3
        private const val DEFAULT_ZOOM = 15
        private const val GERMANY_BOUNDING_BOX = "55.099161,15.0419319,47.2701114,5.8663153"

        // Munich:
        private val DEFAULT_LOCATION = LatLng(48.137154, 11.576124)
        private const val USER_TAG = "userMarker"

        // numbers of markers showed on screen based on zoom level
        private const val zoom3AndLess = 2
        private const val zoom3_4 = 4
        private const val zoom4_6 = 7
        private const val zoom6_7 = 10
        private const val zoom7_14 = 15

        // time
        @RequiresApi(Build.VERSION_CODES.O)
        private val germanOffset = ZoneOffset.ofHours(2)

        @RequiresApi(Build.VERSION_CODES.O)
        private val START_TIME: OffsetTime = LocalTime.of(8, 0).atOffset(germanOffset)

        @RequiresApi(Build.VERSION_CODES.O)
        private val END_TIME: OffsetTime = LocalTime.of(16, 0).atOffset(germanOffset)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        if (savedInstanceState != null) {
//            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
//            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
//        }
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Construct a FusedLocationProviderClient - for getting user's location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment

        mapFragment.getMapAsync(this)

        getBetshopsFromApi()

    }


//    override fun onSaveInstanceState(outState: Bundle) {
//        mMap.let { mMap ->
//            outState.putParcelable(KEY_CAMERA_POSITION, mMap.cameraPosition)
//            outState.putParcelable(KEY_LOCATION, lastKnownLocation)
//        }
//        super.onSaveInstanceState(outState)
//    }

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
        // mMap.setOnMarkerClickListener(mClusterManager)

        // MapWrapperLayout initialization
        // 39 - default marker height
        // 20 - offset between the default InfoWindow bottom edge and it's content bottom edge
        binding.mapFrameLayout.init(mMap, getPixelsFromDp(this, (39 + 20).toFloat()))


        infoWindow = layoutInflater.inflate(R.layout.info_window_layout, null) as ViewGroup?
        btn_route = infoWindow?.findViewById(R.id.btn_route) as ImageButton
        btn_close = infoWindow?.findViewById(R.id.btn_close) as ImageButton
        tv_name = infoWindow?.findViewById(R.id.tv_name)!!
        tv_adress = infoWindow?.findViewById(R.id.tv_adress)!!
        tv_city = infoWindow?.findViewById(R.id.tv_city)!!
        tv_county = infoWindow?.findViewById(R.id.tv_county)!!
        tv_phone = infoWindow?.findViewById(R.id.tv_phone)!!
        tv_hours = infoWindow?.findViewById(R.id.tv_hours)!!

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setOnCameraIdleListener(this)
        mMap.setOnMarkerClickListener(this)
        // mMap.setInfoWindowAdapter(MyInfoWindowAdapter(this, shownBetshops))
        mMap.setOnInfoWindowCloseListener(this)

        setUserLocation()




        this.infoWindowButtonListener = object : OnInfoWindowElemTouchListener(
            btn_route,
            ContextCompat.getDrawable(this, R.drawable.route_dark),
            ContextCompat.getDrawable(this, R.drawable.route_pressed_light)
        ) {
            override fun onClickConfirmed(v: View?, marker: Marker?) {

                //todo ovo sredi, marker.podaci su null!!!!!!!!!!!

                Log.d(
                    "infoWindow",
                    "onClickConfirmed, Marker is null = " + (marker == null).toString()
                )
                Log.d("infoWindow", "onClickConfirmed, navigate to: ${marker?.position}")
                Log.d("infoWindow", "onClickConfirmed, navigate to: " + marker?.position)
                // Here we can perform some action triggered after clicking the button
                val lat = marker?.position?.latitude
                val lon = marker?.position?.longitude
                val gmmIntentUri = Uri.parse("google.navigation:q=${lat},${lon}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.resolveActivity(packageManager)?.let {
                    startActivity(mapIntent)
                }
                Toast.makeText(this@MapsActivity, "click on button 1", Toast.LENGTH_SHORT).show()
            }

        }
        btn_route.setOnTouchListener(infoWindowButtonListener)


        infoWindowButtonListenerClose = object : OnInfoWindowElemTouchListener(
            btn_close,
            ContextCompat.getDrawable(this, R.drawable.close_black),
            ContextCompat.getDrawable(this, R.drawable.close_pressed_gray)
        ) {
            override fun onClickConfirmed(v: View?, marker: Marker?) {
                Log.d(
                    "infoWindow",
                    "onClickConfirmed, btn CLOSE, v==null " + (v == null) + " marker==null " + (marker?.position)
                )
                marker?.hideInfoWindow()
                marker?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_normal))
                Toast.makeText(this@MapsActivity, "click on button 2", Toast.LENGTH_SHORT).show()
            }
        }
        btn_close.setOnTouchListener(infoWindowButtonListenerClose)


        mMap.setInfoWindowAdapter(object : InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? {
                return null
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun getInfoContents(marker: Marker): View {
                // Setting up the infoWindow with current's marker info

                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_active))

                (infoWindowButtonListener as OnInfoWindowElemTouchListener).setMarker(marker)
                (infoWindowButtonListenerClose as OnInfoWindowElemTouchListener).setMarker(marker)
                Log.d(
                    "infoWindow",
                    "getInfoContents, Marker is null = " + (marker == null).toString()
                )


                val nowTimeWithoutOffset = OffsetTime.now(ZoneOffset.UTC)
                val germanNowTime = nowTimeWithoutOffset.withOffsetSameInstant(germanOffset)

                var betshopInfo: BetshopData? = null
                for (bs in allBetshops) {
                    if (marker.position.equals(bs.getPosition())) {
                        betshopInfo = bs
                    }
                }

                if (betshopInfo != null) {
                    tv_name.text = betshopInfo.name.trim()
                    tv_adress.text = betshopInfo.address
                    tv_city.text = betshopInfo.city
                    tv_county.text = betshopInfo.county

                    if (germanNowTime.isAfter(START_TIME) && germanNowTime.isBefore(END_TIME)) {
                        tv_hours.text = "Open now until " + END_TIME.hour + "h"
                        tv_hours.setTextColor(resources.getColor(R.color.teal_200))
                    } else {
                        tv_hours.text = "Opens tomorrow at " + START_TIME.hour + "h"
                    }
                    Log.d("hghgh", "TIME time now GE/HR: " + germanNowTime)
                    Log.d("hghgh", "TIME time now without offset: " + nowTimeWithoutOffset)
                    Log.d(
                        "hghgh",
                        "TIME until closing left hours: " + Duration.between(
                            germanNowTime,
                            END_TIME
                        )
                    )

                }

                // We must call this to set the current marker and infoWindow references
                // to the MapWrapperLayout
                Log.d("hghgh", " USER MARKER tag = " + marker.tag?.toString())
                Log.d("hghgh", " USER MARKER tag = " + USER_TAG)
                Log.d("hghgh", "NIJE USER MARKER = " + (!(marker.tag?.toString()).equals(USER_TAG)))

                binding.mapFrameLayout.setMarkerWithInfoWindow(marker, infoWindow)

                return infoWindow!!
            }

        })


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
// TODO NE ZOOMIRA - POGLEDAJ JOS MALO
            val locationResult = fusedLocationProviderClient.lastLocation

            locationResult.addOnCompleteListener(this) { task ->
                if (task.isSuccessful && task.result != null) {
                    // Set the map's camera position to the current location of the device.
                    lastKnownLocation = task.result

                    mMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude),
                            DEFAULT_ZOOM.toFloat()
                        )
                    )

                    Log.d(
                        "lokacija",
                        "userova lokacija1 - permitted, should be zoomed " + lastKnownLocation
                    )

//                    currentMapScreen = mMap.projection.visibleRegion.latLngBounds
//                    getBetshopsFromApi(getBoundingBox(currentMapScreen))

                } else {
                    //  Toast.makeText(this, "No current location found", Toast.LENGTH_LONG).show()
                    Log.d(
                        "lokacija",
                        "permitted user location, but didn't get it, - ne bi se trebalo desit"
                    )
                }
            }
        } else {
            // Permission to access the location is missing. Show rationale and request permission
            requestPermission(
                this, LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION, false
            )

            Log.d("lokacija", "setUserLocation() neda permitt ")
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

            // todo Add a marker in Munich and move the camera - set blue dot, not marker
            val userMarker =
                mMap.addMarker(MarkerOptions().position(DEFAULT_LOCATION).title("Marker in Munich"))
            userMarker!!.tag = USER_TAG
            mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    DEFAULT_LOCATION,
                    DEFAULT_ZOOM.toFloat()
                    //  7f
                )
            )
            Log.d("lokacija", "Munich lokacija")
//            currentMapScreen = mMap.projection.visibleRegion.latLngBounds
//            getBetshopsFromApi(getBoundingBox(currentMapScreen))
            // todo vidi jel treba otkomentirati ovo nize, mislim da ne
            // permissionDenied = false
        }
    }

    //todo ovo mi ili ne treba ili staviti negdje drugdje
//    override fun onResumeFragments() {
//        super.onResumeFragments()
//        if (permissionDenied) {
//            // Permission was not granted, display error dialog.
//            showMissingPermissionError()
//            permissionDenied = false
//        }
//    }

    private fun showMissingPermissionError() {
        newInstance(false).show(supportFragmentManager, "dialog")
    }

    private fun getBetshopsFromApi() {
        Log.d("hghgh", "Activity boundingBox: ")
        viewModel.setLanLonMapView(GERMANY_BOUNDING_BOX)
        //todo check internet connection
        viewModel.res.observe(this, { betshopResource ->
            when (betshopResource.status) {
                Status.SUCCESS -> {
                    Log.d("hghgh", "activity success")
                    Log.d(
                        "hghgh",
                        "activity success -> betshopResource.count = " + (betshopResource.data?.betshops?.size     // 11611
                                )
                    )    // 11611
                    //todo set visibilities
//                    if (betshopResource.data != null) {
//                        allBetshops = betshopResource.data.betshopsData
//                    }

                    betshopResource.data?.let {
                        if (it.betshops.isNotEmpty()) {
                            Log.d("hghgh", "activity success -> tu sam")
//                            Log.d(
//                                "hghgh",
//                                "activity success -> it.betshopsData.size" + (it.betshopsData.size)    // 0
//                            )
                            allBetshops.addAll(it.betshops)
                            showMarkersBasedOnZoomLevel()
//
                        } else {
                            Log.d("hghgh", "activity success - lista prazna ")
                        }

                    }
//                   if (betshopResource.data!!.betshopsData.isNotEmpty()) {
//                       allBetshops.addAll(betshopResource.data.betshopsData)
//                       showMarkersBasedOnZoomLevel()
//                       Log.d("hghgh", "activity success -> allBetshops.size " + allBetshops.size)
//                   } else {
//                            Log.d("hghgh", "activity success - lista prazna ")
//                        }
                    //-------------------------------------------------------
//                    betshopResource.data?.let { res ->
//                        if (res.betshopsData.isNotEmpty()) {
//                            allBetshops.addAll(res.betshopsData)
//                            // val noMarkersAtCurrentScreen = ArrayList<BetshopData>()
//                            // showBetshops(allBetshops)
//                            showMarkersBasedOnZoomLevel()
//
//                            Log.d("hghgh", "activity success: " + allBetshops.size)
//                        } else {
//                            Log.d("hghgh", "activity success - lista prazna ")
//                        }
//                    }
                }
                Status.LOADING -> {
                    // nothing here
                    Log.d("hghgh", "activity loading")
                }
                Status.ERROR -> {
                    //todo set visibilities
                    Toast.makeText(
                        this,
                        "Can't provide data - " + betshopResource.message,
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("hghgh", "activity error; allBetshops are hardcoded")
                    Log.d("hghgh", "activity error -> " + betshopResource.message)
                }
            }
        })
        // return betShopsForShow
    }

    private fun showBetshops(betShopsForShow: List<BetshopData>) {

//       val img: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_pin_normal)
//        val bitmapDescriptor: BitmapDescriptor = BitmapDescriptorFactory.fromBitmap(img)

        Log.d("lklklk", "showBetshops here")
        betShopsForShow.forEach { betshop ->

            val marker = mMap.addMarker(
                MarkerOptions()
                    .title(betshop.name)
                    .position(LatLng(betshop.location.lat, betshop.location.lng))
                    // .icon(bitmapDescriptor)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_normal))

            )

            markersArray.add(marker!!)
        }
        shownBetshops.addAll(betShopsForShow)
        Log.d("lklklk", "shownBetshops.size = " + shownBetshops.size)
        Log.d("lklklk", "showBetshops OVER")
    }

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

    private fun showMarkersBasedOnZoomLevel() {
        var betshopsForShow = ArrayList<BetshopData>()
        val currentZoomLevel = mMap.cameraPosition.zoom

        Log.d("lklklk", "showMarkersBasedOnZoomLevel")

        //------------------------------------------------------------------

        //************************* IZ oNcAMERAiDLE()*************
        newList.clear()
        markersOnCurrentScreen.clear()

        val curScreenBB: LatLngBounds = mMap.getProjection()
            .getVisibleRegion().latLngBounds
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

//--------------------------------------------------------------------------
        val numberOfMarkers: Int
        when {
            currentZoomLevel <= 3f -> {
                numberOfMarkers = zoom3AndLess
                betshopsForShow = setBetshopsForShow(numberOfMarkers)
                Log.d(
                    "lklklk",
                    "show---------------currentZoomLevel: " + currentZoomLevel + ", betshopsForShow: " + betshopsForShow.size
                )
            }
            3f < currentZoomLevel && currentZoomLevel <= 4f -> {
                numberOfMarkers = zoom3_4
                betshopsForShow = setBetshopsForShow(numberOfMarkers)
                Log.d(
                    "lklklk",
                    "show---------------currentZoomLevel: " + currentZoomLevel + ", betshopsForShow: " + betshopsForShow.size
                )
            }
            4f < currentZoomLevel && currentZoomLevel <= 6f -> {
                numberOfMarkers = zoom4_6
                betshopsForShow = setBetshopsForShow(numberOfMarkers)
                Log.d(
                    "lklklk",
                    "show---------------currentZoomLevel: " + currentZoomLevel + ", betshopsForShow: " + betshopsForShow.size
                )
            }
            6f < currentZoomLevel && currentZoomLevel <= 7f -> {
                numberOfMarkers = zoom6_7
                betshopsForShow = setBetshopsForShow(numberOfMarkers)
                Log.d(
                    "lklklk",
                    "show---------------currentZoomLevel: " + currentZoomLevel + ", betshopsForShow: " + betshopsForShow.size
                )
            }
            7f < currentZoomLevel && currentZoomLevel <= 14 -> {
                numberOfMarkers = zoom7_14
                betshopsForShow = setBetshopsForShow(numberOfMarkers)
                Log.d(
                    "lklklk",
                    "show---------------currentZoomLevel: " + currentZoomLevel + ", betshopsForShow: " + betshopsForShow.size
                )
            }
            currentZoomLevel > 14f -> {
                val (forDelete, fdsize) = removeBetshopsFromNewListThatAreAlreadyOnTheMap(
                    newList,
                    shownBetshops
                )
                newList.removeAll(forDelete)
                betshopsForShow = newList
                Log.d(
                    "lklklk",
                    "show---------------currentZoomLevel: " + currentZoomLevel + ", betshopsForShow: " + betshopsForShow.size
                )
            }
        }
        showBetshops(betshopsForShow)

    }

    private fun setBetshopsForShow(
        numberOfMarkers: Int
    ): ArrayList<BetshopData> {
        val betshopsForShow: ArrayList<BetshopData> = arrayListOf()
        val betshopsForDelete: ArrayList<BetshopData> = arrayListOf()
        // first entry to activity, put certain amount of markers based on zoom level
        Log.d(
            "lklklk",
            "setBetshopsForShow  - markersOnCurrentScreen = " + markersOnCurrentScreen.size     //1
        )

        Log.d(
            "lklklk",
            "setBetshopsForShow - newList = " + newList.size        //2
        )

        // 1)
        if (markersOnCurrentScreen.size == 0 && newList.size == 0) {
            Log.d(
                "lklklk",
                "setBetshopsForShow - 1) "
            )
            return betshopsForShow
            // 2)
        } else if (markersOnCurrentScreen.size != 0 && newList.size == 0) {
            deleteMarkers(markersOnCurrentScreen)
            Log.d(
                "lklklk",
                "setBetshopsForShow - 2) "
            )
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
            Log.d(
                "lklklk",
                "setBetshopsForShow - 3) "
            )
            // 4)
        } else if (markersOnCurrentScreen.size != 0 && newList.size != 0) {
            // 4) a)
            if (markersOnCurrentScreen.size == newList.size) {
                Log.d(
                    "lklklk",
                    "setBetshopsForShow - 4) a) "
                )
                return betshopsForShow
                // 4) b)
            } else if (markersOnCurrentScreen.size > newList.size) {
                Log.d(
                    "lklklk",
                    "setBetshopsForShow - 4) b) -> NEVER SHOULD HAPPEN"
                )
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
                    Log.d(
                        "lklklk",
                        "setBetshopsForShow - 4) c)* "
                    )
                    return betshopsForShow
                    // 4) c)**
                } else if (markersOnCurrentScreen.size < numberOfMarkers) {
                    Log.d(
                        "lklklk",
                        "setBetshopsForShow - 4) c)** "
                    )
                    val (forDelete, fdsize) = removeBetshopsFromNewListThatAreAlreadyOnTheMap(
                        newList,
                        markersOnCurrentScreen
                    )
                    newList.removeAll(forDelete)
                    // 4)** #
                    if (numberOfMarkers - fdsize >= newList.size) {
                        Log.d(
                            "lklklk",
                            "setBetshopsForShow - 4) c)** #"
                        )
                        betshopsForShow.addAll(newList)
                        // 4)** ##
                    } else {
                        Log.d(
                            "lklklk",
                            "setBetshopsForShow - 4) c)** ##"
                        )
                        val randomValues =
                            List(numberOfMarkers - fdsize) { Random.nextInt(0, newList.size - 1) }
                        for (i in randomValues) {
                            betshopsForShow.add(newList[i])
                        }
                    }

                    // 4) c)***
                } else if (markersOnCurrentScreen.size > numberOfMarkers) {
                    Log.d(
                        "lklklk",
                        "setBetshopsForShow - 4) c)*** "
                    )
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

    override fun onCameraIdle() {
        Log.d("lklklk", "onCameraIdle here")

        if (allBetshops.size != 0 || allBetshops.isNotEmpty()) {
            showMarkersBasedOnZoomLevel()
        }
    }


    override fun onInfoWindowClose(marker: Marker) {
        Log.d("lklklk", "onInfoWindowClose")
        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_normal))
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        return (marker.tag?.toString()).equals(USER_TAG)
    }


}