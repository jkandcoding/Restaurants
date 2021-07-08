package com.jkandcoding.android.myapplication.ui

import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.google.android.gms.maps.model.Marker


abstract class OnInfoWindowElemTouchListener(val view: View, val bgDrawableNormal: Drawable?, val bgDrawablePressed: Drawable?) : View.OnTouchListener {

    private val handler: Handler = Handler()

    private var marker: Marker? = null
    private var pressed = false

    fun setMarker(marker: Marker) {
        this.marker = marker
        Log.d("infoWindow", "OnInfoWindowElemTouchListener - setMarker, Marker is null = " + (marker == null).toString())
    }

    override fun onTouch(vv: View?, event: MotionEvent?): Boolean {
        if (0 <= event!!.x && event.x <= view.getWidth() && 0 <= event.y && event.y <= view.getHeight()) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> startPress()
                MotionEvent.ACTION_UP -> handler.postDelayed(confirmClickRunnable, 150)
                MotionEvent.ACTION_CANCEL -> endPress()
                else -> {
                }
            }
        } else {
            // If the touch goes outside of the view's area
            // (like when moving finger out of the pressed button)
            // just release the press
            endPress()
        }
        return false
    }

    private fun startPress() {
        if (!pressed) {
            pressed = true
            handler.removeCallbacks(confirmClickRunnable)
            view.background = bgDrawablePressed
            if (marker != null) marker!!.showInfoWindow()
        }
    }

    private fun endPress(): Boolean {
        return if (pressed) {
            pressed = false
            handler.removeCallbacks(confirmClickRunnable)
            view.background = bgDrawableNormal
            if (marker != null) marker!!.showInfoWindow()
            true
        } else false
    }

    private val confirmClickRunnable = Runnable {
        if (endPress()) {
            onClickConfirmed(view, marker)
            Log.d("infoWindow", "OnInfoWindowElemTouchListener - confirmClickRunnable, Marker is null = " + (marker == null).toString())
        }
    }

    /**
     * This is called after a successful click
     */
    protected abstract fun onClickConfirmed(v: View?, marker: Marker?)

}