package com.scurab.android.appedittext

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ViewCompat
import com.scurab.android.appedittext.drawable.CompoundDrawablesAccessibilityDelegate
import com.scurab.android.appedittext.drawable.CompoundDrawablesController
import com.scurab.android.appedittext.drawable.ICompoundDrawableBehaviour

open class AppEditText(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    AppCompatEditText(context, attrs, defStyleAttr) {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.editTextStyle
    )

    var isInError: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                refreshDrawableState()
            }
        }

    //intentionally nullable, as we can't guarantee nonnull value because of overridden
    //function calls through ctor
    @Suppress("LeakingThis")
    private val compoundDrawablesController =
        CompoundDrawablesController(this) { l: Drawable?, t: Drawable?, r: Drawable?, b: Drawable? ->
            //explicit reference for super.setCompoundDrawables, we can't call it from
            //CompoundDrawableController directly
            super.setCompoundDrawables(l, t, r, b)
        }.also {
            ViewCompat.setAccessibilityDelegate(this, CompoundDrawablesAccessibilityDelegate(it))
        }

    private val compoundDrawablesAccessibilityDelegate =
        CompoundDrawablesAccessibilityDelegate(compoundDrawablesController).also {
            ViewCompat.setAccessibilityDelegate(this, it)
        }

    fun setCompoundDrawableBehaviour(index: Int, behaviour: ICompoundDrawableBehaviour) {
        compoundDrawablesController.setCompoundDrawableClickStrategy(index, behaviour)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        //set drawables from through our delegate
        //needs to be done later, because of rtl resolution
        compoundDrawablesController.onAttachedToWindow()
    }

    override fun setCompoundDrawables(
        left: Drawable?, top: Drawable?, right: Drawable?, bottom: Drawable?
    ) {
        @Suppress("IfThenToSafeAccess", "SENSELESS_COMPARISON")
        //called from super.ctor so if we don't have ref yet, call just super
        //will be init in onAttachedToWindow()
        if (!isInEditMode && compoundDrawablesController != null) {
            compoundDrawablesController.setCompoundDrawables(left, top, right, bottom)
            compoundDrawablesAccessibilityDelegate.invalidateRoot()
        } else {
            super.setCompoundDrawables(left, top, right, bottom)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        compoundDrawablesController.onLayout()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val onTouchEvent = compoundDrawablesController.dispatchTouchEvent(event) ?: false
        return onTouchEvent || super.dispatchTouchEvent(event)
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val state = super.onCreateDrawableState(extraSpace + 1)
        state[state.size - 1] = R.attr.state_error * isInError.sign()
        return state
    }

    //region a11y
    override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        return (compoundDrawablesAccessibilityDelegate.dispatchHoverEvent(event)
                || super.dispatchHoverEvent(event))
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return (compoundDrawablesAccessibilityDelegate.dispatchKeyEvent(event)
                || super.dispatchKeyEvent(event))
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        compoundDrawablesAccessibilityDelegate.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    }
    //endregion a11y
}