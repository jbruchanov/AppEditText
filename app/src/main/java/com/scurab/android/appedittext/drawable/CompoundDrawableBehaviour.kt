package com.scurab.android.appedittext.drawable

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.scurab.android.appedittext.AppEditText
import com.scurab.android.appedittext.R
import com.scurab.android.appedittext.setCompoundDrawable

interface ICompoundDrawableBehaviour {
    val isImportantForAccessibility: Boolean
    fun onClick()
    fun onAttach(virtualView: VirtualView)
    fun onDetach()

    fun getContentDescription(): String
    fun onAccessibilityAction(action: Int, arguments: Bundle?)
    fun onPopulateNode(node: AccessibilityNodeInfoCompat)
}

abstract class CompoundDrawableBehaviour private constructor(
    protected val contentDescriptionResId: Int = 0,
    protected val contentDescriptionString: String? = null
) : ICompoundDrawableBehaviour {

    constructor(@StringRes contentDescription: Int) : this(contentDescription, null)
    constructor(contentDescription: String) : this(0, contentDescription)

    private var _virtualView: VirtualView? = null

    protected val virtualView: VirtualView
        get() = _virtualView
            ?: throw NullPointerException("virtualView is null, invalid lifecycle usage")

    override val isImportantForAccessibility: Boolean get() = virtualView.drawable != null

    @CallSuper
    override fun onAttach(virtualView: VirtualView) {
        _virtualView = virtualView
    }

    @CallSuper
    override fun onDetach() {
        _virtualView = null
    }

    @CallSuper
    override fun onPopulateNode(node: AccessibilityNodeInfoCompat) {
        node.setBoundsInParent(virtualView.rect)
        node.contentDescription = getContentDescription()
    }

    override fun getContentDescription(): String {
        return contentDescriptionResId
            .takeIf { it != 0 }
            ?.let { virtualView.host.resources.getString(it) }
            ?: contentDescriptionString
            ?: ""
    }

    override fun onAccessibilityAction(action: Int, arguments: Bundle?) {
        when (action) {
            AccessibilityNodeInfoCompat.ACTION_CLICK -> onClick()
        }
    }

    object None : CompoundDrawableBehaviour() {
        override val isImportantForAccessibility: Boolean get() = false
        override fun onClick() {
            //do nothing
        }
    }

    open class Button(
        @StringRes contentDescriptionResId: Int = 0,
        contentDescriptionString: String? = null,
        private val clickHandler: (Int, TextView) -> Unit
    ) : CompoundDrawableBehaviour(contentDescriptionResId, contentDescriptionString) {

        constructor(
            @StringRes contentDescription: Int,
            clickHandler: (Int, TextView) -> Unit
        ) : this(contentDescription, null, clickHandler)

        constructor(contentDescription: String, clickHandler: (Int, TextView) -> Unit) : this(
            0,
            contentDescription,
            clickHandler
        )

        override fun onClick() {
            clickHandler(virtualView.id, virtualView.host)
        }

        override fun onPopulateNode(node: AccessibilityNodeInfoCompat) {
            super.onPopulateNode(node)
            node.isClickable = true
            node.className = Button::class.java.name
            //node.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK)
            node.addAction(
                AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfoCompat.ACTION_CLICK,
                    "ExtraClick"
                )
            )
        }
    }

    open class ClearButton : CompoundDrawableBehaviour(R.string.a11y_action_clear_text) {
        private var drawable: WrappingDrawable? = null
        private val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //pick the predefined or from view
                //doesn't have to be known in onAttach
                drawable = drawable ?: virtualView.drawable
                val isEmpty = s?.length == 0
                val wasEmpty = before == 0
                if (isEmpty xor wasEmpty) {
                    //cancels ripple effect, it would appear when shown again
                    drawable?.jumpToCurrentState()
                    dispatchUpdateDrawable(if (isEmpty) null else drawable)
                }
            }
        }

        open fun dispatchUpdateDrawable(drawable: Drawable?) {
            virtualView.host.setCompoundDrawable(
                virtualView.id,
                drawable
            )
        }

        override fun onClick() {
            virtualView.host.text = ""
            //unclear what to do ?
            virtualView.host.post { virtualView.host.requestFocusFromTouch() }
        }

        override fun onAttach(virtualView: VirtualView) {
            super.onAttach(virtualView)
            virtualView.host.addTextChangedListener(textWatcher)
        }

        override fun onDetach() {
            virtualView.host.removeTextChangedListener(textWatcher)
            drawable = null
            super.onDetach()
        }
    }

    open class ToggleButton : CompoundDrawableBehaviour() {
        override fun onClick() {
            virtualView.isChecked = !virtualView.isChecked
            virtualView.invalidateDrawableState()
        }

        override fun onAttach(virtualView: VirtualView) {
            super.onAttach(virtualView)
            virtualView.isCheckable = true
        }

        override fun onDetach() {
            virtualView.isCheckable = false
            virtualView.isChecked = false
            super.onDetach()
        }
    }

    open class PasswordButton(private val isCheckedByDefault: Boolean = true) : ToggleButton() {
        private val TextView.hasPasswordVisible get() = this.transformationMethod is PasswordTransformationMethod
        private val passwordMethod = PasswordTransformationMethod.getInstance()
        override fun onClick() {
            super.onClick()
            virtualView.host.apply {
                transformationMethod = if (hasPasswordVisible) null else passwordMethod
                (this as? EditText)?.setSelection(text.length)
            }
        }

        override fun onAttach(virtualView: VirtualView) {
            super.onAttach(virtualView)
            virtualView.isChecked = isCheckedByDefault
        }

        override fun getContentDescription(): String {
            return if (virtualView.host.hasPasswordVisible) {
                virtualView.host.resources.getString(R.string.a11y_action_hide_password)
            } else {
                virtualView.host.resources.getString(R.string.a11y_action_show_password)
            }
        }
    }
}