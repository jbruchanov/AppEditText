package com.scurab.android.appedittext.drawable

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.widget.TextView

interface LayoutStrategy : (Drawable, TextView, Rect) -> Unit

enum class DefaultAndroidCompoundDrawableLayout(val index: Int) : LayoutStrategy {
    Left(0) {
        override fun invoke(drawable: Drawable, holder: TextView, result: Rect) {
            val vOffset = (holder.compoundPaddingTop - holder.compoundPaddingBottom)
            val x = holder.paddingLeft
            val y = ((vOffset + holder.height - drawable.dirtyBounds.height()) / 2)
            result.set(x, y, x + drawable.dirtyBounds.width(), y + drawable.dirtyBounds.height())
        }
    },
    Top(1) {
        override fun invoke(drawable: Drawable, holder: TextView, result: Rect) {
            val hOffset = (holder.compoundPaddingLeft - holder.compoundPaddingRight)
            val x = ((hOffset + holder.width - drawable.dirtyBounds.width()) / 2)
            val y = holder.paddingTop
            result.set(x, y, x + drawable.dirtyBounds.width(), y + drawable.dirtyBounds.height())
        }
    },
    Right(2) {
        override fun invoke(drawable: Drawable, holder: TextView, result: Rect) {
            val vOffset = (holder.compoundPaddingTop - holder.compoundPaddingBottom)
            val x = holder.width - holder.paddingRight - drawable.dirtyBounds.width()
            val y = ((vOffset + holder.height - drawable.dirtyBounds.height()) / 2)
            result.set(x, y, x + drawable.dirtyBounds.width(), y + drawable.dirtyBounds.height())
        }
    },
    Bottom(3) {
        override fun invoke(drawable: Drawable, holder: TextView, result: Rect) {
            val hOffset = (holder.compoundPaddingLeft - holder.compoundPaddingRight)
            val x = ((hOffset + holder.width - drawable.dirtyBounds.width()) / 2)
            val y = holder.height - holder.paddingBottom - drawable.dirtyBounds.height()
            result.set(x, y, x + drawable.dirtyBounds.width(), y + drawable.dirtyBounds.height())
        }
    }
}