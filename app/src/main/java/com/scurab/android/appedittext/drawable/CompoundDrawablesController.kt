package com.scurab.android.appedittext.drawable

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import com.scurab.android.appedittext.toShortString

interface ICompoundDrawablesController {
    val host: View
    val virtualViews: List<VirtualView>

    fun dispatchTouchEvent(event: MotionEvent): Boolean
    fun onLayout()
    fun onAttachedToWindow()
    fun drawableStateChanged()

    fun setCompoundDrawables(l: Drawable?, t: Drawable?, r: Drawable?, b: Drawable?)
    fun getCompoundDrawableClickStrategy(index: Int) : ICompoundDrawableBehaviour
    fun setCompoundDrawableClickStrategy(index: Int, behaviour: ICompoundDrawableBehaviour)
}

open class CompoundDrawablesController(
    override val host: TextView,
    private val viewDrawableSetter: (Drawable?, Drawable?, Drawable?, Drawable?) -> Unit
) : ICompoundDrawablesController {

    override val virtualViews = Array(4) { VirtualView(it, host) }.toList()
    val leftDrawable get() = left.drawable
    val topDrawable get() = top.drawable
    val rightDrawable get() = right.drawable
    val bottomDrawable get() = bottom.drawable

    private val drawableClickStrategies =
        Array<ICompoundDrawableBehaviour>(4) { CompoundDrawableBehaviour.None }

    /* Flag to optimize to mitigate unnecessary setState calls */
    private var isDirty = false

    private val left get() = virtualViews[0]
    private val top get() = virtualViews[1]
    private val right get() = virtualViews[2]
    private val bottom get() = virtualViews[3]

    override fun getCompoundDrawableClickStrategy(index: Int): ICompoundDrawableBehaviour {
        return drawableClickStrategies[index]
    }

    override fun setCompoundDrawableClickStrategy(index: Int, behaviour: ICompoundDrawableBehaviour) {
        val virtualView = virtualViews[index]
        drawableClickStrategies[index].onDetach()
        drawableClickStrategies[index] = behaviour
        behaviour.onAttach(virtualView)
    }

    open fun setCompoundDrawables(drawables: Array<Drawable?>) {
        val (l, t, r, b) = drawables
        setCompoundDrawables(l, t, r, b)
    }

    override fun setCompoundDrawables(l: Drawable?, t: Drawable?, r: Drawable?, b: Drawable?) {
        left.drawable = l?.wrapped()
        top.drawable = t?.wrapped()
        right.drawable = r?.wrapped()
        bottom.drawable = b?.wrapped()
        viewDrawableSetter(leftDrawable, topDrawable, rightDrawable, bottomDrawable)
        onLayout()
    }

    override fun onLayout() {
        DefaultAndroidCompoundDrawableLayout.values().forEach {
            virtualViews[it.index].apply {
                layout(it)
                invalidateDrawableState(true)
            }
        }
    }

    private val debugPaint by lazy {
        Paint().apply {
            color = Color.RED
            strokeWidth = 1f
            textSize = 24f
        }
    }

    final override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val handled = if (host.isEnabled) dispatchTouchEventImpl(event) else false
        isDirty = handled || isDirty
        return handled
    }

    override fun onAttachedToWindow() {
        setCompoundDrawables(host.compoundDrawables)
    }

    protected open fun dispatchTouchEventImpl(event: MotionEvent): Boolean {
        Log.d("VirtualViewMouse", event.toShortString())
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                virtualViews.firstOrNull { it.contains(event) }
                    ?.let { view ->
                        view.isPressed = true
                        view.drawable?.let {
                            view.dispatchDownEvent(event)
                            return true
                        }
                    }
            }
            MotionEvent.ACTION_MOVE -> {
                virtualViews.firstOrNull { it.contains(event) }
                    ?.let {
                        it.setHotspot(event)
                        return true
                    } /*?: resetDrawables()*/
            }
            MotionEvent.ACTION_UP -> {
                virtualViews.firstOrNull { it.contains(event) }
                    ?.let { view ->
                        view.isPressed = false
                        view.drawable?.let {
                            view.dispatchUpEvent(event)
                            dispatchClick(view)
                            return true
                        }
                    }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                virtualViews.forEach {
                    it.isPressed = false
                    it.invalidateDrawableState()
                    it.drawable?.jumpToCurrentState()
                }
            }
        }
        resetDrawables()
        return false
    }

    protected open fun dispatchClick(view: VirtualView) {
        drawableClickStrategies[view.id].onClick()
    }

    override fun drawableStateChanged() {
        resetDrawables(true)
    }

    private fun resetDrawables(force:Boolean = false) {
        if (isDirty || force) {
            virtualViews.forEach { view ->
                view.drawable?.apply {
                    isStateLocked = false
                    view.invalidateDrawableState()
                    isStateLocked = true
                }
            }
            isDirty = false
        }
    }

    private fun Drawable.wrapped() : WrappingDrawable = WrappingDrawable.wrapped(this)

    internal fun debugDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(host.scrollX.toFloat(), host.scrollY.toFloat())

        debugPaint.style = Paint.Style.FILL
        //to see easily where is origin
        canvas.drawCircle(0f, 0f, 5f, debugPaint)
        //just simple counter to seeing refreshes
        canvas.drawText((SystemClock.uptimeMillis() % 10).toString(), 20f, 20f, debugPaint)
        //draw cross
        canvas.drawLine(
            0f,
            host.height / 2f,
            host.width.toFloat(),
            host.height / 2f,
            debugPaint
        )
        canvas.drawLine(
            host.width / 2f,
            0f,
            host.width / 2f,
            host.height.toFloat(),
            debugPaint
        )
        //draw rectangles for virtual views
        debugPaint.style = Paint.Style.STROKE
        virtualViews.forEach {
            canvas.drawRect(it.rect, debugPaint)
        }
        canvas.restore()
    }
}