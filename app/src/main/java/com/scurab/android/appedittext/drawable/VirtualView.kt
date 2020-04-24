package com.scurab.android.appedittext.drawable

import android.graphics.Rect
import android.graphics.drawable.RippleDrawable
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.scurab.android.appedittext.AppEditText
import com.scurab.android.appedittext.R
import com.scurab.android.appedittext.ViewStateBag
import com.scurab.android.appedittext.ViewTag.Companion.requireBagItem
import com.scurab.android.appedittext.sign
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToLong


interface IVirtualView {
    val id: Int
    val host: View
    val rect: Rect
}

open class VirtualView(
        override val id: Int,
        override val host: AppEditText,
        val touchListener: (Int, VirtualView) -> Unit
) : IVirtualView {

    override val rect: Rect by lazy(LazyThreadSafetyMode.NONE) { Rect() }
    var drawable: WrappingDrawable? = null
        set(value) {
            if (value == null) {
                rect.setEmpty()
            }
            field = value
        }

    private var _isVisible = true
    var isVisible: Boolean
        get() = _isVisible
        set(value) {
            _isVisible = value
            drawable?.setVisible(value, false)
            host.invalidate()
        }

    var isPressed: Boolean = false
    var isChecked: Boolean = false
    val isEnabled get() = host.isEnabled
    val isError get() = host.isError
    val isSuccess get() = host.isSuccess
    private val viewStateBag get() = host.requireBagItem(ViewStateBag.ViewStatesTag)

    //TODO: is this check enough ?
    //ignore focused flag for ripple drawables, as they render RippleBackground, looks weird as it's like
    //frozen ripple
    val isFocused get() = drawable?.wrappedDrawable !is RippleDrawable && host.isFocused

    private val touchSlop = ViewConfiguration.get(host.context).scaledTouchSlop.toFloat()

    fun layout(layout: LayoutStrategy) {
        drawable
                ?.let { layout(it, host, rect) }
                ?: rect.setEmpty()
    }

    private fun state() = stateReuseStaticArrays()

    //seems to be only working solution for different cases
    //same "idea" what is used in android's StateSet
    private fun stateReuseStaticArrays(): IntArray {
        //for more states, consider ignoring this global int[][],
        //this might be memory heavy, we could've (2^n + 1) arrays in the worst case
        //in real, most likely states changing are "isFocused, isEnabled, isPressed" + few other edge cases
        val index = booleansToBitMask(isFocused, isEnabled, isPressed, isChecked, isError, isSuccess)
        val result = InternalStates.setIfNull(index) {
            val result = IntArray(StatePromises.size)
            StatePromises.forEachIndexed { i, (attr, isAttrStateActive) ->
                result[i] = attr * isAttrStateActive(this).sign()
            }
            result
        }
        return if (viewStateBag.customStatesCount > 0) {
            //in this case, memory grow can quickly go up with we already have 6 states, another
            //4 will be 2^10 array of 1024 arrays.
            //usage of custom states will be quite rare usecase, so let's keep it simple and potentially optimise later
            viewStateBag.getCustomStates() + result
        } else result
    }

    open fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isVisible || rect.isEmpty) return false

        val contains = rect.containsSlop(event.x, event.y, touchSlop)
        val x = event.x
        val y = event.y
        var handled = false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                //if we touch down and not for our drawable, just ignore it
                if (contains) {
                    dispatchDownEvent(event)
                    handled = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                //handle move action only if we were initially pressed
                //handles weird cases when you touchdown on left and move touching to right
                if (isPressed) {
                    handled = true
                    drawable?.setHotspot(x, y)
                    if (!contains) {
                        //if we started on view, but leaving it
                        //just act like we did touchUp to reset, with no click callbacks
                        dispatchUpEvent(event, contains)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                //just reset drawable state
                dispatchUpEvent(event, contains)
                //notify we clicked on, if the location was related to our view
                handled = contains
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                dispatchUpEvent(event, false)
                //explicit invalidation is necessary for correct ripple finish
                //looks like in scrollview when DOWN -> few MOVEs -> CANCEL
                //ripple doesn't invalidate itself if pressed state changed only in our stateSet
                //so finish ripple nicely like we "left" the view
                host.postInvalidate()
            }
        }
        return handled
    }

    /*
        Force redrawing when we are pressed.
        Ripple sometimes struggles to finish when in scrollview.
     */
    private val invalidateAction = object : Runnable {
        val fps = (1000 / 25f).roundToLong()
        override fun run() {
            if (isPressed) {
                host.invalidate()
                //under a finger thing, 25fps is far enough refreshrate
                host.postDelayed(this, fps)
            }
        }
    }

    protected open fun dispatchDownEvent(event: MotionEvent) {
        isPressed = true
        drawable?.let {
            setHotspot(event)
            invalidateDrawableState()
            invalidateAction.run()
        }
    }

    protected open fun dispatchUpEvent(event: MotionEvent, ownLocation: Boolean) {
        isPressed = false
        drawable?.let {
            setHotspot(event)
            invalidateDrawableState()
            drawable?.let {
                //call only if containedByView is default behaviour
                //however as the drawable is tiny, calling callback if the drawables is pressed
                //but finger outside (to see you've pressed that) does seem to be useful as well
                if (ownLocation) {
                    touchListener(0 /*CLICK, TODO*/, this)
                }
            }
        }
    }


    fun invalidateDrawableState() {
        drawable?.let { it ->
            val state = state()
            val set = it.setStateReal(state)
            val name = host.id
                    .takeIf { it != View.NO_ID }
                    ?.let { host.resources.getResourceName(it) }
                    ?: ""

            Log.d(
                    "VirtualViewState",
                    ("$name[$id] = State[${set.bit()}]:'${dumpState(state)}' {${state}")
            )
            if (set) {
                it.invalidateSelf()
            }
        }
    }

    private fun setHotspot(event: MotionEvent) {
        drawable?.let {
            val x = event.x - rect.left
            val y = event.y - rect.top
            it.setHotspot(x, y)
        }
    }

    private fun dumpState(state: IntArray): String {
        return state.map { v ->
            when (abs(v)) {
                android.R.attr.state_focused -> "focused"
                android.R.attr.state_enabled -> "enabled"
                android.R.attr.state_pressed -> "pressed"
                android.R.attr.state_checkable -> "checkable"
                android.R.attr.state_checked -> "checked"
                R.attr.state_error -> "error"
                R.attr.state_success -> "success"
                else -> v.toString()
            }.let { if (v < 0) it.toLowerCase(Locale.UK) else it.toUpperCase(Locale.UK) }
        }.joinToString(", ")
    }

    companion object {
        //unclear why this is necessary to avoid reusing an array for single drawable
        //when states are changed, but that's exactly what is happening in android StateSet
        //and also view if necessary is creating completely new array everytime ¯\_(ツ)_/¯
        //I'd guess that in drawable hierarchy the array maybe copied or reused
        //hence it's safer to do same thing what google does.
        //If this was broken, UI states weren't changing as expected
        //TODO:  extract this into own place, so anyone could add new state more easily with hiding the complexity about these arrays
        private val StatePromises = arrayOf<Pair<Int, (VirtualView) -> Boolean>>(
                android.R.attr.state_enabled to { v -> v.isEnabled },
                android.R.attr.state_focused to { v -> v.isFocused },
                android.R.attr.state_pressed to { v -> v.isPressed },
                android.R.attr.state_checked to { v -> v.isChecked },
                R.attr.state_error to { v -> v.isError },
                R.attr.state_success to { v -> v.isSuccess }
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
fun Boolean.bit(shift: Int = 0): Int {
    return (if (this) 1 else 0) shl shift
}

fun booleansToBitMask(vararg items: Boolean): Int {
    require(items.size <= 32) { "Maximum size for Int bitmask 32, passed args:${items.size}" }
    return items.foldIndexed(0) { i, acc, v -> acc xor v.bit(i) }
}

private inline fun <T : Any> Array<T?>.setIfNull(index: Int, block: () -> T): T {
    val item = this[index] ?: block()
    this[index] = item
    return item
}