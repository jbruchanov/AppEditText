package com.scurab.android.appedittext

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.TextView
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ViewCompat
import com.scurab.android.appedittext.drawable.CompoundDrawableBehaviour
import com.scurab.android.appedittext.drawable.CompoundDrawablesAccessibilityDelegate
import com.scurab.android.appedittext.drawable.CompoundDrawablesController
import com.scurab.android.appedittext.drawable.ICompoundDrawableBehaviour
import com.scurab.android.appedittext.drawable.SuperTextDrawable
import com.scurab.android.appedittext.drawable.bit

open class AppEditText(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    AppCompatEditText(context, attrs, defStyleAttr) {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.editTextStyle
    )
    /*
        pending drawables set via xml, we have to wait until rtl it's resolved,
        otherwise left/right compound drawables are not set and might be lost
     */
    private val pendingDrawable: Array<Drawable?> = arrayOfNulls<Drawable?>(4)

    /**
     * Error state for the view.
     * To reflect the state in drawables use ```app:state_error="true"``` on particular drawable in StateListDrawable
     */
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

    /**
     * Set a behaviour for the drawables.
     * As Behaviours might stateful they are not persisted during [onSaveInstanceState].
     * It's developer's responsibility to handle it manually.
     */
    fun setCompoundDrawableBehaviour(index: Int, behaviour: ICompoundDrawableBehaviour) {
        compoundDrawablesController.setCompoundDrawableClickStrategy(index, behaviour)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        //set drawables from through our delegate
        //needs to be done later, because of rtl resolution
        val viewsDrawables = compoundDrawables
        pendingDrawable.forEachIndexed { i, d ->
            viewsDrawables[i] = d ?: viewsDrawables[i]
            pendingDrawable[i] = null
        }
        super.setCompoundDrawablesRelativeWithIntrinsicBounds(
            viewsDrawables[0],
            viewsDrawables[1],
            viewsDrawables[2],
            viewsDrawables[3]
        )
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
        val onTouchEvent = compoundDrawablesController.dispatchTouchEvent(event)
        return onTouchEvent || super.dispatchTouchEvent(event)
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val state = super.onCreateDrawableState(extraSpace + 1)
        state[state.size - 1] = R.attr.state_error * isInError.sign()
        return state
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        compoundDrawablesController?./*ctor call*/drawableStateChanged()
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
        compoundDrawablesAccessibilityDelegate.onFocusChanged(
            gainFocus,
            direction,
            previouslyFocusedRect
        )
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (false/*debug*/) {
            compoundDrawablesController.debugDraw(canvas)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return SavedState(superState).apply {
            this.isInError = this@AppEditText.isInError
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val superState = (state as? SavedState)?.let {
            this@AppEditText.isInError = it.isInError
            it.superState
        } ?: state
        super.onRestoreInstanceState(superState)
    }

    init {
        attrs?.let { attrs ->
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AppEditText)
            (0 until typedArray.indexCount)
                .forEach {
                    when (val index = typedArray.getIndex(it)) {
                        R.styleable.AppEditText_compoundDrawableLeftTitle -> {
                            //TODO: default styling
                            pendingDrawable[0] = SuperTextDrawable(
                                typedArray.getText(index),
                                context,
                                R.style.labelTextAppearance
                            )
                        }
                        R.styleable.AppEditText_compoundDrawableRightTitle -> {
                            //TODO: default styling
                            pendingDrawable[2] = SuperTextDrawable(
                                typedArray.getText(index),
                                context,
                                R.style.labelTextAppearance
                            )
                        }
                        R.styleable.AppEditText_compoundDrawableRightBehaviour -> {
                            when (typedArray.getInt(index, BEHAVIOUR_NONE)) {
                                BEHAVIOUR_NONE -> setCompoundDrawableBehaviour(2, CompoundDrawableBehaviour.None())
                                BEHAVIOUR_CLEAR_BUTTON -> setCompoundDrawableBehaviour(2, CompoundDrawableBehaviour.ClearButton())
                                BEHAVIOUR_PASSWORD -> setCompoundDrawableBehaviour(2, CompoundDrawableBehaviour.PasswordButton())
                            }
                        }
                    }
                }
            typedArray.recycle()
        }
    }

    companion object {
        //attrs enum
        private const val BEHAVIOUR_NONE = 0
        private const val BEHAVIOUR_CLEAR_BUTTON = 1
        private const val BEHAVIOUR_PASSWORD = 2


        @Keep @Suppress("unused")
        @JvmStatic
        val CREATOR: Parcelable.ClassLoaderCreator<SavedState> =
            object : Parcelable.ClassLoaderCreator<SavedState> {
                @RequiresApi(Build.VERSION_CODES.N)
                override fun createFromParcel(source: Parcel, loader: ClassLoader): SavedState {
                    return SavedState(source, loader)
                }

                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
    }

    class SavedState : BaseSavedState {
        var isInError = false

        constructor(source: Parcel) : super(source) {
            init(source)
        }

        @RequiresApi(Build.VERSION_CODES.N)
        constructor(source: Parcel, loader: ClassLoader?) : super(source, loader) {
            init(source)
        }

        constructor(superState: Parcelable?) : super(superState)

        private fun init(source: Parcel?) {
            isInError = (source?.readInt() ?: 0) == 1
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(isInError.bit())
        }
    }
}