package com.scurab.android.appedittext.drawable

import android.graphics.Rect
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.TextView
import com.scurab.android.appedittext.AppEditText
import com.scurab.android.appedittext.R
import com.scurab.android.appedittext.sign
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class VirtualView(val id: Int, val host: TextView, val touchListener: (Int, VirtualView) -> Unit) {
    val rect: Rect by lazy(LazyThreadSafetyMode.NONE) { Rect() }
    var drawable: WrappingDrawable? = null
        set(value) {
            if (value == null) {
                rect.setEmpty()
            }
            field = value
        }

    var isPressed: Boolean = false
    var isCheckable: Boolean = false
    var isChecked: Boolean = false
    val isEnabled get() = host.isEnabled
    val isInError get() = (host as? AppEditText)?.isInError ?: false
    private val touchSlop = ViewConfiguration.get(host.context).scaledTouchSlop.toFloat()
    private val isFocused get() = host.isFocused

    fun layout(layout: LayoutStrategy) {
        drawable
            ?.let { layout(it, host, rect) }
            ?: rect.setEmpty()
    }

    fun contains(event: MotionEvent): Boolean {
        val x = event.x.roundToInt()
        val y = event.y.roundToInt()
        return rect.contains(x, y)
    }

    private fun state() = stateReuseStaticArrays()

    //seems to be only working solution for different cases
    private fun stateReuseStaticArrays(): IntArray {
        val index = isEnabled.bit(3) or isPressed.bit(2) or isChecked.bit(1) or isInError.bit(0)
        val result = InternalStates.setIfNull(index) {
            val result = IntArray(StatePromises.size)
            StatePromises.forEachIndexed { i, (attr, isAttrStateActive) ->
                result[i] = attr * isAttrStateActive(this).sign()
            }
            result
        }
        return result
    }

    fun setHotspot(event: MotionEvent) {
        drawable?.let {
            val x = event.x - rect.left
            val y = event.y - rect.top
            it.setHotspot(x, y)
        }
    }

    protected open fun dispatchDownEvent(event: MotionEvent) {
        drawable?.let {
            it.isStateLocked = false
            setHotspot(event)
            invalidateDrawableState()
        }
    }

    protected open fun dispatchUpEvent(event: MotionEvent) {
        drawable?.let {
            setHotspot(event)
            invalidateDrawableState()
            it.isStateLocked = true
        }
    }

    fun invalidateDrawableState(jumpToCurrentState: Boolean = false) {
        drawable?.let {
            val state = state()
            val set = it.setStateReal(state)
            val name = host.resources.getResourceName(host.id)
            Log.d(
                "VirtualViewState",
                ("$name[$id] = State[${set.bit()}]:'${dumpState(state)}' {${state}")
            )
            if (set) {
                if (jumpToCurrentState) {
                    it.jumpToCurrentState()
                }
                it.invalidateSelf()
            }
        }
    }

    private fun dumpState(state: IntArray): String {
        return state.map { v ->
            when (abs(v)) {
                android.R.attr.state_enabled -> "enabled"
                android.R.attr.state_pressed -> "pressed"
                android.R.attr.state_checkable -> "checkable"
                android.R.attr.state_checked -> "checked"
                R.attr.state_error -> "error"
                else -> v.toString()
            }.let { if (v < 0) it.toLowerCase(Locale.UK) else it.toUpperCase(Locale.UK) }
        }.joinToString(", ")
    }

    open fun onTouchEvent(event: MotionEvent): Boolean {
        if (!rect.containsSlop(event.x, event.y, touchSlop)) {
            return false
        }

        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                dispatchDownEvent(event)
            }
            MotionEvent.ACTION_MOVE -> {
                drawable?.setHotspot(x, y)
            }
            MotionEvent.ACTION_UP -> {
                isPressed = false
                dispatchUpEvent(event)
                drawable?.let {
                    touchListener(0 /*CLICK, TODO*/, this)
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                isPressed = false
                invalidateDrawableState()
                drawable?.jumpToCurrentState()
                return false
            }
        }
        return true
    }

    companion object {
        private val EmptyDrawable = WrappingDrawable(null)
        //unclear why this is necessary to avoid reusing an array for single drawable
        //when states are changed, but that's exactly what is happening in android StateSet
        //and also view if necessary is creating completely new array everytime ¯\_(ツ)_/¯
        //in our case, currently I need only 2 arrays based on error/pressed state.
        //I'd guess that in drawable hierarchy the array maybe copied or reused
        //hence it's safer to do same thing what google does.
        //If this was broken, UI states weren't changing as expected

        private val StatePromises = arrayOf<Pair<Int, (VirtualView) -> Boolean>>(
            //isFocused is weird for Ripple, as it renders android.graphics.drawable.RippleBackground
            //which is unexpected and doesn't seem to be way to turning it off
            //android.R.attr.state_focused to { isFocused },
            android.R.attr.state_enabled to { v -> v.isEnabled },
            android.R.attr.state_pressed to { v -> v.isPressed },
            android.R.attr.state_checkable to { v -> v.isCheckable },
            android.R.attr.state_checked to { v -> v.isCheckable && v.isChecked },
            R.attr.state_error to { v -> v.isInError }
        )
        private val InternalStates = arrayOfNulls<IntArray>(1 shl StatePromises.size)
    }
}

private fun Rect.containsSlop(x: Float, y: Float, slop: Float): Boolean {
    return left < right && top < bottom /*check for empty first*/ &&
            x >= (left - slop) &&
            x < (right + slop) &&
            y >= (top - slop) &&
            y < (bottom + slop)
}


//common utils
private fun Boolean.bit(shift: Int = 0): Int {
    return (if (this) 1 else 0) shl shift
}

private inline fun <T : Any> Array<T?>.setIfNull(index: Int, block: () -> T): T {
    val item = this[index] ?: block()
    this[index] = item
    return item
}
