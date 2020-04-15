package com.scurab.android.appedittext

import android.graphics.drawable.Drawable
import android.widget.TextView

fun TextView.setCompoundDrawable(index: Int, drawable: Drawable?) {
    setCompoundDrawables(compoundDrawables.also {
        it[index] = drawable
    })
}

fun TextView.setCompoundDrawables(drawable: Array<Drawable?>) {
    setCompoundDrawables(drawable[0], drawable[1], drawable[2], drawable[3])
}

fun Boolean.sign() = if (this) 1 else -1