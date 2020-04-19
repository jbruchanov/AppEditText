package com.scurab.android.app

import android.view.MotionEvent

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