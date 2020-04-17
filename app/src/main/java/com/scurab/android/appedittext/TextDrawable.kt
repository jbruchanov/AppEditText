package com.scurab.android.appedittext

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.annotation.StyleRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.AppCompatTextView

//refactor it in UI module
//this is too naive and does work only in case of stateless, totally dumb drawable
class TextDrawable(
    text: CharSequence,
    context: Context,
    @StyleRes textAppearance: Int
) : Drawable() {

    @VisibleForTesting
    internal val textView =
        AppCompatTextView(ContextThemeWrapper(context, textAppearance), null, 0).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

    var text: CharSequence?
        get() = textView.text
        set(value) {
            textView.text = value
            textView.measure(0, 0)
            invalidateSelf()
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }

    init {
        this.text = text
    }

    override fun getIntrinsicWidth() = textView.measuredWidth

    override fun getIntrinsicHeight() = textView.measuredHeight

    override fun onBoundsChange(bounds: Rect?) {
        val bounds = bounds ?: EMPTY_BOUNDS
        textView.layout(bounds.left, bounds.top, bounds.right, bounds.bottom)
        super.onBoundsChange(bounds)
    }

    override fun draw(canvas: Canvas) {
        textView.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        textView.paint.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        textView.paint.colorFilter = cf
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        private val EMPTY_BOUNDS = Rect()
    }
}