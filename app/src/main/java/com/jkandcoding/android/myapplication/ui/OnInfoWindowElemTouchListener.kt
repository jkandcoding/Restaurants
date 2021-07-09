package com.jkandcoding.android.myapplication.ui

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import com.google.android.gms.maps.model.Marker

abstract class OnInfoWindowElemTouchListener(
    private val view: View,
    private val bgDrawableNormal: Drawable?,
    private val bgDrawablePressed: Drawable?
) : View.OnTouchListener {

    private val handler: Handler = Handler(Looper.getMainLooper())

    private var marker: Marker? = null
    private var pressed = false

    fun setMarker(marker: Marker) {
        this.marker = marker
    }

    override fun onTouch(vv: View?, event: MotionEvent?): Boolean {
        if (0 <= event!!.x && event.x <= view.width && 0 <= event.y && event.y <= view.height) {
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
        }
    }

    /**
     * This is called after a successful click
     */
    protected abstract fun onClickConfirmed(v: View?, marker: Marker?)

}