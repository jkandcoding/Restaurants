package com.jkandcoding.android.myapplication.ui

//import android.app.Activity
//import android.content.Context
//import android.util.Log
//import android.view.View
//import android.widget.TextView
//import androidx.core.view.isVisible
//import com.google.android.gms.maps.GoogleMap
//import com.google.android.gms.maps.model.BitmapDescriptorFactory
//import com.google.android.gms.maps.model.Marker
//import com.jkandcoding.android.myapplication.R
//import com.jkandcoding.android.myapplication.network.BetshopData
//
//class MyInfoWindowAdapter(context: Context, betshops: ArrayList<BetshopData>) : GoogleMap.InfoWindowAdapter {
//
//    val mBetshops = betshops
//    var betshopInfo: BetshopData? = null
//    var mContext = context
//    var mWindow = (context as Activity).layoutInflater.inflate(R.layout.info_window_layout, null)
//
//    private fun rendowInfoWindow(marker: Marker, view: View) {
//        Log.d("lklklk", "MyInfoWindowAdapter - rendowInfoWindow")
//        val tv_name = view.findViewById<TextView>(R.id.tv_name)
//        val tv_adress = view.findViewById<TextView>(R.id.tv_adress)
//        val tv_city = view.findViewById<TextView>(R.id.tv_city)
//        val tv_county = view.findViewById<TextView>(R.id.tv_county)
//        val tv_phone = view.findViewById<TextView>(R.id.tv_phone)
//        val tv_hours = view.findViewById<TextView>(R.id.tv_hours)
//        val btn_route = view.findViewById<TextView>(R.id.btn_route)
//
//        for (bs in mBetshops) {
//            if (marker.position.equals(bs.getPosition())) {
//                betshopInfo = bs
//            }
//        }
//
//        if (betshopInfo != null) {
//            tv_name.text = betshopInfo!!.name
//            tv_adress.text = betshopInfo!!.address
//            tv_city.text = betshopInfo!!.city
//            tv_county.text = betshopInfo!!.county
//            tv_hours.text = "Open now"
//            btn_route.text = "ROUTE"
//        }
//
//    }
//
//    override fun getInfoWindow(marker: Marker): View? {
//        rendowInfoWindow(marker, mWindow)
//        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_active))
//        Log.d("lklklk", "MyInfoWindowAdapter - getInfoWindow")
//        return mWindow
//    }
//
//    override fun getInfoContents(marker: Marker): View? {
//        rendowInfoWindow(marker, mWindow)
//        Log.d("lklklk", "MyInfoWindowAdapter - getInfoContents")
//        return mWindow
//    }
//
//
//}
