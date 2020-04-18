package com.scurab.android.appedittext.drawable

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.content.res.Resources.Theme
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import androidx.annotation.StyleRes
import androidx.core.content.res.getIntOrThrow
import androidx.core.graphics.drawable.DrawableCompat
import com.scurab.android.appedittext.R
import org.xmlpull.v1.XmlPullParser
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Suppress("unused")
open class SuperTextDrawable : Drawable() {
    private var _alpha: Int = 255
    private val TAG = "SuperTextDrawable"

    var textLayout: Layout? by invalidating(null) { updateBoundsWithIntrinsicSize() }
    var gravity: Int by invalidating(Gravity.CENTER)
    var color: ColorStateList by invalidating(ColorStateList.valueOf(Color.BLACK))
    var text: CharSequence? by invalidating(null) { buildLayout() }
    var textSize: Float
        get() = paint.textSize
        set(value) {
            paint.textSize = value
            invalidateSelf()
        }
    var padding: IntArray by invalidating(IntArray(4)) { array ->
        require(array.size == 4) { "Invalid array size, must be 4, newValue is ${array.size}" }
    }
    var paddingHorizontal: Int
        get() = padding[0] + padding[2]
        set(value) {
            padding[0] = value
            padding[2] = value
            invalidateSelf()
        }

    var paddingVertical: Int
        get() = padding[1] + padding[3]
        set(value) {
            padding[1] = value
            padding[3] = value
            invalidateSelf()
        }

    private val paint = TextPaint().apply {
        isAntiAlias = true
        textSize = Resources.getSystem().displayMetrics.scaledDensity * 12
        color = Color.BLACK
    }
    private val tempRect = Rect()

    override fun inflate(
        r: Resources,
        parser: XmlPullParser,
        attrs: AttributeSet,
        theme: Resources.Theme?
    ) {
        super.inflate(r, parser, attrs, theme)
        val arr = obtainAttributes(r, theme, attrs, R.styleable.SuperTextDrawable)
        (0 until arr.indexCount).forEach {
            when (val index = arr.getIndex(it)) {
                R.styleable.SuperTextDrawable_android_text -> text = arr.getText(index)
                R.styleable.SuperTextDrawable_android_textSize -> paint.textSize = arr.getDimension(index, 0f)
                R.styleable.SuperTextDrawable_android_textColor -> color = arr.getColorStateList(index)!!
                R.styleable.SuperTextDrawable_android_gravity -> gravity = arr.getIntOrThrow(index)
                R.styleable.SuperTextDrawable_android_padding -> {
                    val p = arr.getDimensionPixelSize(index, 0)
                    padding.forEachIndexed { index, _ ->
                        padding[index] = p
                    }
                }
                R.styleable.SuperTextDrawable_android_paddingHorizontal -> {
                    paddingHorizontal = arr.getDimensionPixelSize(index, 0)
                }
                R.styleable.SuperTextDrawable_android_paddingVertical -> {
                    paddingVertical = arr.getDimensionPixelSize(index, 0)
                }
            }
        }
        arr.recycle()
    }

    private fun buildLayout() {
        textLayout = text?.let {
            val measureText = paint.measureText(it, 0, it.length)
            StaticLayout(
                it,
                paint,
                measureText.ceil(),
                Layout.Alignment.ALIGN_CENTER,
                1f,
                0f,
                true
            )
        }
    }

    fun setTextAppearance(context: Context, @StyleRes id: Int) {
        val typedArray = context.obtainStyledAttributes(id, R.styleable.TextAppearanceSubset)
        var fontStyle = Typeface.NORMAL
        var fontFamily: String? = null
        (0 until typedArray.indexCount).forEach { i ->
            when (typedArray.getIndex(i)) {
                R.styleable.TextAppearanceSubset_android_textColor -> {
                    color = typedArray.getColorStateList(i) ?: TODO("null textColor ?")
                }
                R.styleable.TextAppearanceSubset_android_textSize -> {
                    paint.textSize = typedArray.getDimension(i, 0f)
                }
                R.styleable.TextAppearanceSubset_android_font -> {
                    paint.setTypeface(typedArray.getFont(i))
                }
                R.styleable.TextAppearanceSubset_android_fontFamily -> {
                    fontFamily = typedArray.getString(i)
                }
                R.styleable.TextAppearanceSubset_android_textStyle -> {
                    fontStyle = typedArray.getInt(i, Typeface.NORMAL)
                }
                //TODO:more stuff
            }
        }

        fontFamily?.let {
            paint.typeface = Typeface.create(it, fontStyle)
        }
        typedArray.recycle()
        buildLayout()
    }

    fun updateBoundsWithIntrinsicSize() {
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
    }

    override fun getIntrinsicWidth(): Int {
        return padding[0] + padding[2] + (textLayout?.getLineWidth(0)?.ceil() ?: 0)
    }

    override fun getIntrinsicHeight(): Int {
        return padding[1] + padding[3] + (textLayout?.height ?: 0)
    }

    override fun draw(canvas: Canvas) {
        textLayout?.let { textLayout ->
            val c = canvas.save()
            Gravity.apply(gravity, intrinsicWidth, intrinsicHeight, dirtyBounds, tempRect, DrawableCompat.getLayoutDirection(this))
            //translate for gravity
            canvas.translate(tempRect.left.toFloat(), tempRect.top.toFloat())
            //canvas.drawRect(0f, 0f, intrinsicWidth.toFloat(), intrinsicHeight.toFloat(), debugPaint)
            //translate for internal padding
            canvas.translate(padding[0].toFloat(), padding[1].toFloat())
            Log.d(TAG, "draw: alpha=${_alpha} isVisible:${isVisible} text:$text")
            paint.withAlpha(_alpha) {
                textLayout.draw(canvas)
            }

            canvas.restoreToCount(c)
        }
    }

    override fun getAlpha(): Int {
        return _alpha
    }

    override fun setAlpha(alpha: Int) {
        Log.d(TAG, "setAlpha: alpha=${alpha}")
        _alpha = alpha
        invalidateSelf()
    }

    override fun isStateful(): Boolean {
        return color.isStateful
    }

    override fun setState(stateSet: IntArray): Boolean {
        val colorForState = color.getColorForState(stateSet, Color.CYAN)
        paint.color = colorForState
        return super.setState(stateSet)
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getColorFilter(): ColorFilter? {
        return paint.colorFilter
    }

    override fun toString(): String {
        return "${super.toString()}[text=$text]"
    }

    companion object {
        private val debugPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = 0x60000000
            }
        }
    }
}

//TODO: common
private fun Float.ceil() = kotlin.math.ceil(this.toDouble()).toInt()

fun Drawable.obtainAttributes(
    res: Resources,
    theme: Theme?,
    set: AttributeSet, attrs: IntArray
): TypedArray {
    return if (theme == null) {
        res.obtainAttributes(set, attrs)
    } else theme.obtainStyledAttributes(set, attrs, 0, 0)
}

private inline fun Paint.withAlpha(new: Int, block: () -> Unit) {
    val old = this.alpha
    this.alpha = new
    block()
    this.alpha = old
}

private fun <T, R : Drawable> invalidating(initValue: T, block: (R.(T) -> Unit)? = null): DrawablePropertyDelegate<T, R> {
    return DrawablePropertyDelegate(initValue) {v ->
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