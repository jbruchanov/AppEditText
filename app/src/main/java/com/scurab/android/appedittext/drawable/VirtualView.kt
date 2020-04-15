package com.scurab.android.appedittext.drawable

import android.graphics.Rect
import android.view.MotionEvent
import android.widget.TextView
import kotlin.math.roundToInt

class VirtualView(val id: Int, val host: TextView) {
    val rect: Rect by lazy(LazyThreadSafetyMode.NONE) { Rect() }
    var drawable: WrappingDrawable? = null
        set(value) {
            initStates()
            field = value
        }

    var isCheckable: Boolean = false
    var isChecked: Boolean = false

    private val statesCount = 3

    //has to be different array, otherwise ripple won't finish the animation
    private lateinit var states: Array<IntArray>
    private lateinit var statePromises: Array<Pair<Int, () -> Boolean>>

    private fun initStates() {
        states = arrayOf(IntArray(statesCount), IntArray(statesCount))
        statePromises = arrayOf(
            android.R.attr.state_enabled to { host.isEnabled },
            android.R.attr.state_checked to { isCheckable && isChecked }
        )
    }

    fun update(layout: LayoutStrategy) {
        drawable
            ?.let { layout(it, host, rect) }
            ?: rect.set(0, 0, -1, -1)
    }

    fun contains(event: MotionEvent): Boolean {
        val x = event.x.roundToInt()
        val y = event.y.roundToInt()
        return rect.contains(x, y)
    }

    private fun state(pressed: Boolean): IntArray {
        val state = states[if (pressed) 1 else 0]
        state[0] = if (pressed) android.R.attr.state_pressed else -android.R.attr.state_pressed
        statePromises.forEachIndexed { index, (attr, isAttrStateActive) ->
            state[index + 1] = attr * isAttrStateActive().sign()
        }
        return state
    }

    fun dispatchDownEvent(event: MotionEvent) {
        drawable?.let {
            it.isStateLocked = false
            setHotspot(event)
            it.state = state(true)
            it.invalidateSelf()
        }
    }

    fun setHotspot(event: MotionEvent) {
        drawable?.let {
            val x = event.x - rect.left
            val y = event.y - rect.top
            it.setHotspot(x, y)
        }
    }

    fun dispatchUpEvent(event: MotionEvent) {
        drawable?.let {
            it.state = state(false)
            it.invalidateSelf()
            it.isStateLocked = true
        }
    }

    private fun Boolean.sign() = if (this) 1 else -1

    fun invalidateDrawableState() {
        drawable?.let {
            if (it.setState(state(false))) {
                it.invalidateSelf()
            }
        }
    }

    companion object {
        private val EmptyDrawable = WrappingDrawable(null)
    }
}
