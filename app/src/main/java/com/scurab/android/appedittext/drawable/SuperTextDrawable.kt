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
import androidx.annotation.Px
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.annotation.StyleableRes
import androidx.core.content.res.getDimensionOrThrow
import androidx.core.content.res.getIntOrThrow
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.graphics.drawable.DrawableCompat
import com.scurab.android.appedittext.R
import com.scurab.android.appedittext.ceilInt
import com.scurab.android.appedittext.invalidating
import com.scurab.android.appedittext.obtainAttributes
import com.scurab.android.appedittext.withMultiplyingAlpha
import org.xmlpull.v1.XmlPullParser
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Suppress("unused", "MemberVisibilityCanBePrivate")
open class SuperTextDrawable() : Drawable() {

    constructor(text: CharSequence?) : this() {
        this.text = text
        updateBoundsWithIntrinsicSize()
    }

    constructor(
        @StringRes textResId: Int,
        context: Context,
        @StyleRes textAppearance: Int = 0
    ) : this(context.getText(textResId), context, textAppearance)

    constructor(text: CharSequence?, context: Context, @StyleRes textAppearance: Int = 0) : this() {
        this.text = text
        if (textAppearance != 0) {
            setTextAppearance(textAppearance, context.resources, context.theme)
        }
        updateBoundsWithIntrinsicSize()
    }

    private var _alpha: Int = 255

    /**
     * Text layout.
     * Set this value for any complex text.
     */
    var textLayout: Layout? by invalidating(null) { updateBoundsWithIntrinsicSize() }
    /**
     * Gravity of the text, useful in case of multiple layers with different size.
     * Use StateListDrawable#attr_android:constantSize = true to respect that.
     * For single drawable itself has no meaning.
     */
    var gravity: Int by invalidating(Gravity.CENTER)

    /**
     * Stateful text color
     */
    var color: ColorStateList by invalidating(ColorStateList.valueOf(Color.BLACK))

    /**
     * CharSequence for easier setup of [textLayout]
     */
    var text: CharSequence? by invalidating(null) { buildLayout() }

    /**
     * Text Alignment in case of multiple lines only.
     * Currently has only naive implementation of textWidth, which doesn't respect the new lines.
     * Calculating text width based on CharSequence with spans might be very tricky, so
     * use only 1 line texts.
     */
    var textAlignment by invalidating(Layout.Alignment.ALIGN_CENTER) {
        buildLayout()
    }

    var minTextWidth : Int by invalidating(0)
    override fun getMinimumWidth(): Int = minTextWidth

    var minTextHeight : Int by invalidating(0)
    override fun getMinimumHeight(): Int = minTextHeight

    /**
     * Text size in Pix
     */
    var textSize: Float
        @Px get() = paint.textSize
        set(@Px value) {
            paint.textSize = value
            updateBoundsWithIntrinsicSize()
            invalidateSelf()
        }

    /**
     * Text padding in pixels
     * Array must have 4 elements following order [left, top, right, bottom]
     * It doesn't respect the Start/End
     */
    var padding: IntArray by invalidating(IntArray(4)) { array ->
        require(array.size == 4) { "Invalid array size, must be 4, newValue is ${array.size}" }
    }

    /**
     * Horizontal text padding
     */
    var paddingHorizontal: Int
        get() = padding[0] + padding[2]
        set(value) {
            padding[0] = value / 2
            padding[2] = value / 2
            invalidateSelf()
        }

    /**
     * Vertical text padding
     */
    var paddingVertical: Int
        get() = padding[1] + padding[3]
        set(value) {
            padding[1] = value / 2
            padding[3] = value / 2
            invalidateSelf()
        }

    protected val paint = TextPaint().apply {
        isAntiAlias = true
        textSize = Resources.getSystem().displayMetrics.scaledDensity * 12
        color = Color.CYAN
        style = Paint.Style.FILL
    }

    private val tempRect = Rect()

    override fun inflate(
            res: Resources,
            parser: XmlPullParser,
            attrs: AttributeSet,
            theme: Resources.Theme?
    ) {
        super.inflate(res, parser, attrs, theme)
        val arr = obtainAttributes(res, theme, attrs, R.styleable.SuperTextDrawable)
        initValueFromAttrs(arr, res, theme ?: res.newTheme(), TEXT_DRAWABLE)
        arr.recycle()
    }

    private fun buildLayout() {
        textLayout = text?.let {
            //doesn't respect new lines.
            //far more complicated as it's CharSequence (having spans)
            //how to measure it properly for all cases
            //TODO: maybe naive implementation for strings only ?
            StaticLayout(
                    it,
                    paint,
                    max(minTextWidth, paint.measureText(it, 0, it.length).ceilInt()),
                    Layout.Alignment.ALIGN_CENTER,
                    1f/*extra line spacing * coef */,
                    0f/*extra line spacing + coef */,
                    false/*include font padding?*/
            )
        }
    }

    /**
     * Update typeface of the textDrawable
     */
    open fun setTypeFace(typeface: Typeface) {
        paint.typeface = typeface
        invalidateSelf()
    }

    /**
     * Set textAppearance of the TextDrawable.
     * Follow [R.attr.CustomTextAppearance] for supported fields
     */
    fun setTextAppearance(@StyleRes resId: Int, res: Resources, theme: Theme) {
        val oldLayout = textLayout
        val array: TypedArray = theme.obtainStyledAttributes(resId, R.styleable.CustomTextAppearance)
        initValueFromAttrs(array, res, theme, TEXT_APPEARANCE)
        array.recycle()
        //if same, rather rebuild whole layout
        if (textLayout == oldLayout) {
            buildLayout()
            updateBoundsWithIntrinsicSize()
            invalidateSelf()
        }
    }

    private fun initValueFromAttrs(array: TypedArray, res: Resources, theme: Theme, q: Int) {
        var textAppearance = 0
        var fontStyle = Typeface.NORMAL
        var fontFamily: String? = null
        var text: CharSequence? = null
        (0 until array.indexCount).forEach {
            when (val index = array.getIndex(it)) {
                StyleAttrs.textAppearance[q] -> textAppearance = array.getResourceIdOrThrow(index)
                StyleAttrs.text[q] -> text = array.getText(index)
                StyleAttrs.textSize[q] -> textSize = array.getDimensionOrThrow(index)
                StyleAttrs.font[q] -> setTypeFace(array.getFont(index)
                        ?: throw NullPointerException("Null typeFace?!"))
                StyleAttrs.textColor[q] -> color = array.getColorStateList(index)!!
                StyleAttrs.gravity[q] -> gravity = array.getIntOrThrow(index)
                StyleAttrs.paddingHorizontal[q] -> paddingHorizontal = array.getDimensionPixelSize(index, 0)
                StyleAttrs.paddingVertical[q] -> paddingVertical = array.getDimensionPixelSize(index, 0)
                StyleAttrs.fontFamily[q] -> fontFamily = array.getString(index)
                StyleAttrs.textStyle[q] -> fontStyle = array.getInt(index, Typeface.NORMAL)
                StyleAttrs.minWidth[q] -> minTextWidth = array.getDimensionPixelSize(index, 0)
                StyleAttrs.minHeight[q] -> minTextHeight = array.getDimensionPixelSize(index, 0)
                StyleAttrs.padding[q] -> {
                    val p = array.getDimensionPixelSize(index, 0)
                    padding.forEachIndexed { index, _ ->
                        padding[index] = p
                    }
                }
            }
        }
        fontFamily?.let { setTypeFace(Typeface.create(it, fontStyle)) }
        if (textAppearance != 0) {
            setTextAppearance(textAppearance, res, theme)
        }
        //set text at the end, so layout is taking all the latest values
        if (q == TEXT_DRAWABLE && text != null) {
            this.text = text
        }
    }

    /**
     * Update bounds based on the [textLayout] and [padding] size
     */
    fun updateBoundsWithIntrinsicSize() {
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
    }

    /**
     * Get [textLayout] width including [paddingHorizontal]
     */
    override fun getIntrinsicWidth(): Int {
        return max(minTextWidth, paddingHorizontal + textWidth)
    }

    /**
     * Get [textLayout] height including [paddingVertical]
     */
    override fun getIntrinsicHeight(): Int {
        return max(minTextHeight, paddingVertical + textHeight)
    }

    /**
     * Get [textLayout] width
     */
    val textWidth get() = (textLayout?.width ?: 0)

    /**
     * Get [textLayout] height
     */
    val textHeight get() = (textLayout?.height ?: 0)

    override fun draw(canvas: Canvas) {
        textLayout?.let { textLayout ->
            val c = canvas.save()
            //translate for gravity
            Gravity.apply(gravity, textWidth + paddingHorizontal, textHeight + paddingVertical, dirtyBounds, tempRect, DrawableCompat.getLayoutDirection(this))
            canvas.translate(tempRect.left.toFloat(), tempRect.top.toFloat())

            if (DEBUG) {
                canvas.drawRect(0f, 0f, intrinsicWidth.toFloat(), intrinsicHeight.toFloat(), debugPaint)
                Log.d(TAG, "draw: alpha=${_alpha} isVisible:${isVisible} text:$text")
            }

            //translate for internal padding
            canvas.translate(padding[0].toFloat(), padding[1].toFloat())
            //changing alpha might be during transition from "parent" drawable
            paint.withMultiplyingAlpha(_alpha) {
                textLayout.draw(canvas)
            }

            canvas.restoreToCount(c)
        }
    }

    override fun getAlpha(): Int {
        return _alpha
    }

    override fun setAlpha(alpha: Int) {
        //don't set the alpha on textColor, this is not driven by UI state change
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
        private const val TEXT_DRAWABLE = 0
        private const val TEXT_APPEARANCE = 1

        private val TAG = "SuperTextDrawable"
        private const val DEBUG = false

        private val debugPaint by lazy {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = 0x10000000
            }
        }

        //simple helper class to handle drawable attrs and textappearance by same code
        //looks like there is no simple way of doing it just by 1 attrSet
        private object StyleAttrs {
            //MIN_VALUE is to ignore textAppearance ref in textAppearance to avoid circular references
            val textAppearance = ResPair(R.styleable.SuperTextDrawable_android_textAppearance, Int.MIN_VALUE)
            val text = ResPair(R.styleable.SuperTextDrawable_android_text, R.styleable.CustomTextAppearance_android_text)
            val textSize = ResPair(R.styleable.SuperTextDrawable_android_textSize, R.styleable.CustomTextAppearance_android_textSize)
            val font = ResPair(R.styleable.SuperTextDrawable_android_font, R.styleable.CustomTextAppearance_android_font)
            val textColor = ResPair(R.styleable.SuperTextDrawable_android_textColor, R.styleable.CustomTextAppearance_android_textColor)
            val gravity = ResPair(R.styleable.SuperTextDrawable_android_gravity, R.styleable.CustomTextAppearance_android_gravity)
            val paddingHorizontal =
                    ResPair(R.styleable.SuperTextDrawable_android_paddingHorizontal, R.styleable.CustomTextAppearance_android_paddingHorizontal)
            val paddingVertical = ResPair(R.styleable.SuperTextDrawable_android_paddingVertical, R.styleable.CustomTextAppearance_android_paddingVertical)
            val fontFamily = ResPair(R.styleable.SuperTextDrawable_android_fontFamily, R.styleable.CustomTextAppearance_android_fontFamily)
            val textStyle = ResPair(R.styleable.SuperTextDrawable_android_textStyle, R.styleable.CustomTextAppearance_android_textStyle)
            val padding = ResPair(R.styleable.SuperTextDrawable_android_padding, R.styleable.CustomTextAppearance_android_padding)
            val minWidth = ResPair(R.styleable.SuperTextDrawable_android_minWidth, R.styleable.CustomTextAppearance_android_minWidth)
            val minHeight = ResPair(R.styleable.SuperTextDrawable_android_minHeight, R.styleable.CustomTextAppearance_android_minHeight)
        }
    }
}

private class ResPair(@StyleableRes private val first: Int, @StyleableRes private val second: Int) {
    operator fun get(i: Int): Int {
        return when (i) {
            0 -> first
            1 -> second
            else -> throw IllegalStateException("Invalid index:$i")
        }
    }
}