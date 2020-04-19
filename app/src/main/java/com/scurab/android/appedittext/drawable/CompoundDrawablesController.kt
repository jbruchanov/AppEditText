package com.scurab.android.appedittext.drawable

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import com.scurab.android.app.toShortString
import com.scurab.android.appedittext.RtlDrawableIndexes

interface ICompoundDrawablesController {
    val host: View
    val virtualViews: List<VirtualView>

    //endregion to call from view
    fun dispatchTouchEvent(event: MotionEvent): Boolean
    fun onLayout()
    fun drawableStateChanged()
    //endregion

    fun getCompoundDrawableClickStrategy(index: Int): ICompoundDrawableBehaviour
    fun setCompoundDrawableClickStrategyRelative(index: Int, behaviour: ICompoundDrawableBehaviour)
}

open class CompoundDrawablesController(
        override val host: TextView
) : ICompoundDrawablesController {

    private val drawablesHelper = RtlDrawableIndexes(host.context)
    private val touchListener = { id: Int, view: VirtualView -> dispatchClick(view) }
    override val virtualViews = Array(4) { VirtualView(it, host, touchListener) }.toList()
    val leftDrawable get() = left.drawable
    val topDrawable get() = top.drawable
    val rightDrawable get() = right.drawable
    val bottomDrawable get() = bottom.drawable

    private val drawableClickStrategies =
            Array<ICompoundDrawableBehaviour>(4) { i ->
                CompoundDrawableBehaviour.None().also { it.onAttach(virtualViews[i]) }
            }

    /* Flag to optimize unnecessary setState calls */
    private var isDirty = false

    //absolute positioning, RTL handled in setCompoundDrawableClickStrategyRelative
    private val left get() = virtualViews[0]
    private val top get() = virtualViews[1]
    private val right get() = virtualViews[2]
    private val bottom get() = virtualViews[3]

    override fun getCompoundDrawableClickStrategy(index: Int): ICompoundDrawableBehaviour {
        return drawableClickStrategies[index]
    }

    override fun setCompoundDrawableClickStrategyRelative(
            index: Int,
            behaviour: ICompoundDrawableBehaviour
    ) {
        val rtlIndex = drawablesHelper.transform(index)
        val virtualView = virtualViews[rtlIndex]
        drawableClickStrategies[rtlIndex].onDetach()
        drawableClickStrategies[rtlIndex] = behaviour
        behaviour.onAttach(virtualView)
    }

    override fun onLayout() {
        val compoundDrawables = host.compoundDrawables
        DefaultAndroidCompoundDrawableLayout.values().forEach {
            virtualViews[it.index].apply {
                val d = compoundDrawables[it.index]
                if (d != null && d !is WrappingDrawable) {
                    throw IllegalStateException("Drawable for index:${it.index} should be already " +
                            "wrapped in ${WrappingDrawable::class.java}, but it's ${d.javaClass.name}." +
                            "All drawables has to be wrapped before layout")
                }
                drawable = d as WrappingDrawable?
                layout(it)
                invalidateDrawableState()
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
        Log.d("VirtualViewMouse", event.toShortString())
        if (!host.isEnabled) {
            return false
        }
        val handled = virtualViews.firstOrNull { it.onTouchEvent(event) } != null
        isDirty = handled || isDirty
        return handled
    }

    protected open fun dispatchClick(view: VirtualView) {
        drawableClickStrategies[view.id].onClick()
    }

    override fun drawableStateChanged() {
        virtualViews.forEach { view ->
            view.invalidateDrawableState()
        }
    }

    fun Drawable.wrapped(): WrappingDrawable = WrappingDrawable.wrapped(this)

    private var debugDrawCounter = 0
    internal fun debugDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(host.scrollX.toFloat(), host.scrollY.toFloat())

        debugPaint.style = Paint.Style.FILL
        //to see easily where is origin
        canvas.drawCircle(0f, 0f, 5f, debugPaint)
        //just simple counter to seeing refreshes
        canvas.drawText((debugDrawCounter++ % 10).toString(), 20f, 30f, debugPaint)
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