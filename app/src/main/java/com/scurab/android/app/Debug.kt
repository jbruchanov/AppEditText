package com.scurab.android.app

import android.view.MotionEvent
import com.scurab.android.appedittext.R
import java.util.Locale
import kotlin.math.abs

fun MotionEvent.toShortString(): String {
    val action = when (actionMasked) {
        MotionEvent.ACTION_DOWN -> "ACTION_DOWN"
        MotionEvent.ACTION_UP -> "ACTION_UP"
        MotionEvent.ACTION_MOVE -> "ACTION_MOVE"
        MotionEvent.ACTION_CANCEL -> "ACTION_CANCEL"
        else -> "ELSE"
    }
    return "$action ${this.x}, ${this.y}"
}

fun dumpState(state: IntArray): String {
    return state.map { v ->
        when (abs(v)) {
            android.R.attr.state_focused -> "focused"
            android.R.attr.state_window_focused -> "window_focused"
            android.R.attr.state_enabled -> "enabled"
            android.R.attr.state_checkable -> "checkable"
            android.R.attr.state_checked -> "checked"
            android.R.attr.state_selected -> "selected"
            android.R.attr.state_pressed -> "pressed"
            android.R.attr.state_activated -> "activated"
            android.R.attr.state_active -> "active"
            android.R.attr.state_single -> "single"
            android.R.attr.state_first -> "first"
            android.R.attr.state_middle -> "middle"
            android.R.attr.state_last -> "last"
            android.R.attr.state_accelerated -> "accelerated"
            android.R.attr.state_hovered -> "hovered"
            android.R.attr.state_drag_can_accept -> "drag_can_accept"
            android.R.attr.state_drag_hovered -> "drag_hovered"
            R.attr.state_error -> "error"
            R.attr.state_success -> "success"
            else -> v.toString()
        }.let { if (v < 0) it.toLowerCase(Locale.UK) else it.toUpperCase(Locale.UK) }
    }.joinToString(", ")
}