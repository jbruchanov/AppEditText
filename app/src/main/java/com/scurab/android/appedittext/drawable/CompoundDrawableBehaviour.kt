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

    override val isImportantForAccessibility: Boolean get() = _virtualView?.drawable != null

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

    class None : CompoundDrawableBehaviour() {
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

    abstract class HideOnEmptyTextBehaviour(@StringRes contentDescription: Int) : CompoundDrawableBehaviour(contentDescription) {
        private val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //pick the predefined or from view
                //doesn't have to be known in onAttach
                val isEmpty = s?.length == 0
                val wasEmpty = before == 0
                if (isEmpty xor wasEmpty) {
                    //cancels ripple effect, it would appear when shown again
                    virtualView.isVisible = !isEmpty
                }
            }
        }

        override fun onAttach(virtualView: VirtualView) {
            super.onAttach(virtualView)
            virtualView.host.addTextChangedListener(textWatcher)
            textWatcher.onTextChanged(virtualView.host.text, 0, -1, virtualView.host.text?.length ?: 0)
        }

        override fun onDetach() {
            virtualView.host.removeTextChangedListener(textWatcher)
            super.onDetach()
        }
    }

    open class ClearButton : HideOnEmptyTextBehaviour(R.string.a11y_action_clear_text) {
        override fun onClick() {
            virtualView.host.text?.clear()
            //unclear what to do ?
            virtualView.host.post { virtualView.host.requestFocusFromTouch() }
        }
    }

    abstract class ToggleButton(private val clickHandler: (Int, TextView, Boolean) -> Unit) : CompoundDrawableBehaviour() {
        override fun onClick() {
            virtualView.isChecked = !virtualView.isChecked
            virtualView.invalidateDrawableState()
            clickHandler(virtualView.id, virtualView.host, virtualView.isChecked)
        }

        override fun onAttach(virtualView: VirtualView) {
            super.onAttach(virtualView)
        }

        override fun onDetach() {
            virtualView.isChecked = false
            super.onDetach()
        }
    }

    open class PasswordButton() : HideOnEmptyTextBehaviour(R.string.app_name/*TODO:*/) {
        private val TextView.hasPasswordVisible get() = this.transformationMethod is PasswordTransformationMethod
        private val passwordMethod = PasswordTransformationMethod.getInstance()

        override fun onClick() {
            virtualView.apply {
                val newMethod = if (host.hasPasswordVisible) null else passwordMethod
                host.transformationMethod = newMethod
                virtualView.isChecked = newMethod != null
                virtualView.invalidateDrawableState()
                //TODO:
                (host as? EditText)?.let { it.setSelection(it.text.length) }
            }
        }

        override fun onAttach(virtualView: VirtualView) {
            super.onAttach(virtualView)
            virtualView.isChecked = virtualView.host.hasPasswordVisible
        }

        override fun onDetach() {
            virtualView.isChecked = false
            super.onDetach()
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