package com.scurab.android.appedittext

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView

//TODO: keep it or not ? weird issues around

private var View.tagInfiniteLoop
    get() = getTag(R.id.tag_infinite_loop_check)
    set(value) {
        setTag(R.id.tag_infinite_loop_check, value)
    }

fun TextView.setCompoundDrawable(index: Int, drawable: Drawable?, setBounds: Boolean = false) {
    if (isLaidOut) {
        tagInfiniteLoop = null
        setCompoundDrawables(compoundDrawables.also {
            it[index] = drawable?.also { d ->
                if (setBounds) d.setIntrinsicBounds()
            }
        })
    } else {
        if (tagInfiniteLoop == true) {
            throw IllegalStateException("Infinite loop protection")
        }
        tagInfiniteLoop = true
        post { setCompoundDrawable(index, drawable, setBounds) }
    }
}

fun TextView.setCompoundDrawableRelative(index: Int, drawable: Drawable?, setBounds: Boolean = false) {
    if (isLaidOut) {
        tagInfiniteLoop = null
        setCompoundDrawablesRelative(compoundDrawablesRelative.also {
            it[index] = drawable?.also { d ->
                if (setBounds) d.setIntrinsicBounds()
            }
        })
    } else {
        //workaround for potentially pending start/end drawables
        if (tagInfiniteLoop == true) {
            throw IllegalStateException("Infinite loop protection")
        }
        tagInfiniteLoop = true
        post { setCompoundDrawableRelative(index, drawable, setBounds) }
    }
}

fun TextView.setCompoundDrawables(drawable: Array<Drawable?>) {
    setCompoundDrawables(drawable[0], drawable[1], drawable[2], drawable[3])
}

fun TextView.setCompoundDrawablesRelative(drawable: Array<Drawable?>) {
    setCompoundDrawablesRelative(drawable[0], drawable[1], drawable[2], drawable[3])
}