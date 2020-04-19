package com.scurab.android.appedittext

import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import kotlin.math.roundToInt
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun Boolean.sign() = if (this) 1 else -1

fun Float.ceilInt() = kotlin.math.ceil(this.toDouble()).toInt()

fun Drawable.obtainAttributes(
        res: Resources,
        theme: Resources.Theme?,
        set: AttributeSet,
        attrs: IntArray
): TypedArray {
    return if (theme == null) {
        res.obtainAttributes(set, attrs)
    } else theme.obtainStyledAttributes(set, attrs, 0, 0)
}

inline fun Paint.withMultiplyingAlpha(new: Int, block: () -> Unit) {
    val old = this.alpha
    //rather multiple instead of overwrite
    //if paint had 0.5 and we needed 0.75, it would be higher then color from state
    // will be => 0.5*0.75 = 0.375
    this.alpha = (old * (new / 255f)).roundToInt().coerceIn(0, 255)
    block()
    this.alpha = old
}

//some abstraction for views and other types
fun <T, R : Drawable> invalidating(initValue: T, block: (R.(T) -> Unit)? = null): DrawablePropertyDelegate<T, R> {
    return DrawablePropertyDelegate(initValue) { v ->
        block?.invoke(this, v)
        invalidateSelf()
    }
}

class DrawablePropertyDelegate<T, R : Drawable>(initValue: T, private val onSetAction: R.(T) -> Unit) : ReadWriteProperty<R, T> {
    private var value: T? = initValue
    override fun getValue(thisRef: R, property: KProperty<*>): T {
        return value as T
    }

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        this.value = value
        onSetAction(thisRef, value)
    }
}