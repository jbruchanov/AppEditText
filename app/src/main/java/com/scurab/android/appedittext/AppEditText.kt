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
import android.view.View
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ViewCompat
import androidx.customview.view.AbsSavedState
import com.scurab.android.appedittext.drawable.CompoundDrawableBehaviour
import com.scurab.android.appedittext.drawable.CompoundDrawablesAccessibilityDelegate
import com.scurab.android.appedittext.drawable.CompoundDrawablesController
import com.scurab.android.appedittext.drawable.ICompoundDrawableBehaviour
import com.scurab.android.appedittext.drawable.SuperTextDrawable
import com.scurab.android.appedittext.drawable.bit

@Suppress("MemberVisibilityCanBePrivate")
open class AppEditText(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
        AppCompatEditText(context, attrs, defStyleAttr),
        IViewStateBag by ViewStateBag() {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(
            context,
            attrs,
            R.attr.editTextStyle
    )

    /**
     * Error state for the view.
     * To reflect the state in drawables use ```app:state_error="true"``` on particular drawable in StateListDrawable
     * Mutual exclusive with [isSuccess]
     */
    open var isError: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                if (isError) {
                    isSuccess = false
                }
                refreshDrawableState()
            }
        }

    /**
     * Susccess state for the view.
     * To reflect the state in drawables use ```app:state_success="true"``` on particular drawable in StateListDrawable
     * Mutual exclusive with [isError]
     */
    open var isSuccess: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                if (isSuccess) {
                    isError = false
                }
                refreshDrawableState()
            }
        }

    //intentionally nullable, as we can't guarantee nonnull value because of overridden
    //function calls through ctor
    @Suppress("LeakingThis")
    private val compoundDrawablesController =
            CompoundDrawablesController(this).also {
                ViewCompat.setAccessibilityDelegate(this, CompoundDrawablesAccessibilityDelegate(it))
            }

    private var compoundDrawablesAccessibilityDelegate: CompoundDrawablesAccessibilityDelegate? = null

    private fun initAccessibilityDelegate() {
        compoundDrawablesAccessibilityDelegate = (compoundDrawablesAccessibilityDelegate
                ?: CompoundDrawablesAccessibilityDelegate(compoundDrawablesController).also {
                    ViewCompat.setAccessibilityDelegate(this, it)
                })
    }

    /**
     * Set a behaviour for the drawables.
     * As Behaviours might stateful with complex un-serializable data (click listener, etc), hence they are not persisted during [onSaveInstanceState].
     * It's developer's responsibility to handle it manually if necessary
     */
    open fun setCompoundDrawableBehaviourRelative(index: Int, behaviour: ICompoundDrawableBehaviour) {
        compoundDrawablesController.setCompoundDrawableClickStrategyRelative(index, behaviour)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        //TODO: if a11y enabled use initAccessibilityDelegate()
        //it works for keyboard and has side effects
    }

    override fun setCompoundDrawables(
            left: Drawable?, top: Drawable?, right: Drawable?, bottom: Drawable?
    ) {
        if (!isSuperInitialized) {
            return
        }
        @Suppress("IfThenToSafeAccess", "SENSELESS_COMPARISON")
        //potential ctor call
        if (!isInEditMode && compoundDrawablesController != null) {
            compoundDrawablesController.apply {
                super.setCompoundDrawables(
                        left?.wrapped(),
                        top?.wrapped(),
                        right?.wrapped(),
                        bottom?.wrapped()
                )
            }
            compoundDrawablesAccessibilityDelegate?.invalidateRoot()
        } else {
            super.setCompoundDrawables(left, top, right, bottom)
        }
    }

    override fun setCompoundDrawablesRelative(start: Drawable?, top: Drawable?, end: Drawable?, bottom: Drawable?) {
        if (!isSuperInitialized) {
            return
        }
        @Suppress("SENSELESS_COMPARISON")
        //potential ctor call
        if (!isInEditMode && compoundDrawablesController != null) {
            compoundDrawablesController.apply {
                super.setCompoundDrawablesRelative(
                        start?.wrapped(),
                        top?.wrapped(),
                        end?.wrapped(),
                        bottom?.wrapped()
                )
                compoundDrawablesAccessibilityDelegate?.invalidateRoot()
            }
        } else {
            super.setCompoundDrawablesRelative(start, top, end, bottom)
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

    private val internalStates = listOf(
            { R.attr.state_success * isSuccess.sign() },
            { R.attr.state_error * isError.sign() }
    )

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        if (!isSuperInitialized) return super.onCreateDrawableState(extraSpace)

        val states = super.onCreateDrawableState(extraSpace + internalStates.size + customStatesCount)
        internalStates.forEachIndexed { i, statePromise ->
            states[states.size - i - 1] = statePromise()
        }
        //merge our isError/isSuccess state with any other custom
        if (customStatesCount > 0) {
            val customStates = getCustomStates()
            System.arraycopy(customStates, 0, states, states.size - internalStates.size - customStatesCount, customStates.size)
        }
        return states
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        compoundDrawablesController?./*ctor call*/drawableStateChanged()
    }

    //region a11y
    override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        return (compoundDrawablesAccessibilityDelegate?.dispatchHoverEvent(event) ?: false ||
                super.dispatchHoverEvent(event))
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        //TODO: keyboard enter behaves like a event click
        //we shouldn't allow that
        return (compoundDrawablesAccessibilityDelegate?.dispatchKeyEvent(event) ?: false ||
                super.dispatchKeyEvent(event))
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        compoundDrawablesAccessibilityDelegate?.onFocusChanged(
                gainFocus,
                direction,
                previouslyFocusedRect
        )
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (true/*debug*/) {
            compoundDrawablesController.debugDraw(canvas)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        return SavedState(super.onSaveInstanceState()!!).apply {
            this.isError = this@AppEditText.isError
            this.isSuccess = this@AppEditText.isSuccess
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val state = state as? SavedState
        if (state != null) {
            super.onRestoreInstanceState(state.superState)
            this@AppEditText.isError = state.isError
            this@AppEditText.isSuccess = state.isSuccess
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    //keep track when we have returned from parent ctor,
    //some overridden methods are called from super and could crash because we don't have initialised fields yet
    private var isSuperInitialized = false

    init {
        isSuperInitialized = true
        @Suppress("LeakingThis")
        initStateBag()
		val pendingDrawables: Array<Drawable?> = arrayOfNulls<Drawable?>(4)
        attrs?.let { attrs ->
            val rtl = RtlDrawableIndexes(context)
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AppEditText)
            (0 until typedArray.indexCount)
                    .forEach {
                        when (val index = typedArray.getIndex(it)) {
                            R.styleable.AppEditText_android_drawableLeft -> pendingDrawables[rtl.left] = typedArray.getDrawable(index)
                            R.styleable.AppEditText_android_drawableTop -> pendingDrawables[rtl.top] = typedArray.getDrawable(index)
                            R.styleable.AppEditText_android_drawableRight -> pendingDrawables[rtl.right] = typedArray.getDrawable(index)
                            R.styleable.AppEditText_android_drawableBottom -> pendingDrawables[rtl.bottom] = typedArray.getDrawable(index)
                            //intentionally left/right as following will call setCompoundDrawablesRelative
                            R.styleable.AppEditText_android_drawableStart -> pendingDrawables[rtl.left] = typedArray.getDrawable(index)
                            R.styleable.AppEditText_android_drawableEnd -> pendingDrawables[rtl.right] = typedArray.getDrawable(index)
                            R.styleable.AppEditText_compoundDrawableStartTitle -> {
                                //TODO: default styling
                                pendingDrawables[rtl.left] = SuperTextDrawable(
                                        typedArray.getText(index),
                                        context,
                                        R.style.labelTextAppearance
                                )
                            }
                            R.styleable.AppEditText_compoundDrawableEndTitle -> {
                                //TODO: default styling
                                pendingDrawables[rtl.right] = SuperTextDrawable(
                                        typedArray.getText(index),
                                        context,
                                        R.style.labelTextAppearance
                                )
                            }
                            R.styleable.AppEditText_compoundDrawableEndBehaviour -> {
                                when (typedArray.getInt(index, BEHAVIOUR_NONE)) {
                                    BEHAVIOUR_NONE -> setCompoundDrawableBehaviourRelative(rtl.right, CompoundDrawableBehaviour.None())
                                    BEHAVIOUR_CLEAR_BUTTON -> setCompoundDrawableBehaviourRelative(rtl.right, CompoundDrawableBehaviour.ClearButton())
                                    BEHAVIOUR_PASSWORD -> setCompoundDrawableBehaviourRelative(rtl.right, CompoundDrawableBehaviour.PasswordButton())
                                }
                            }
                        }
                    }
            typedArray.recycle()
        }
        setCompoundDrawablesRelativeWithIntrinsicBounds(pendingDrawables[0], pendingDrawables[1], pendingDrawables[2], pendingDrawables[3])
        pendingDrawables.fill(null)
    }

    companion object {
        //attrs enum
        private const val BEHAVIOUR_NONE = 0
        private const val BEHAVIOUR_CLEAR_BUTTON = 1
        private const val BEHAVIOUR_PASSWORD = 2
    }

	class SavedState : BaseSavedState {
		var isError = false
		var isSuccess = false

		constructor(source: Parcel) : super(source) {
			init(source)
		}

		@RequiresApi(Build.VERSION_CODES.N)
		constructor(source: Parcel, loader: ClassLoader?) : super(source, loader) {
			init(source)
		}

		constructor(superState: Parcelable?) : super(superState)

		private fun init(source: Parcel?) {
			isError = (source?.readInt() ?: 0) == 1
			isSuccess = (source?.readInt() ?: 0) == 1
		}

		override fun writeToParcel(out: Parcel, flags: Int) {
			super.writeToParcel(out, flags)
			out.writeInt(isError.bit())
			out.writeInt(isSuccess.bit())
		}

		companion object {
			@Keep
			@Suppress("unused")
			@JvmField
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
	}
}