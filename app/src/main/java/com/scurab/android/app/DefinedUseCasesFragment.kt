package com.scurab.android.app

import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.StateListDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.StateSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.scurab.android.appedittext.R
import com.scurab.android.appedittext.databinding.FragmentUsecasesBinding
import com.scurab.android.appedittext.drawable.CompoundDrawableBehaviour
import com.scurab.android.appedittext.drawable.SuperTextDrawable
import com.scurab.android.appedittext.setCompoundDrawableRelative
import java.util.Locale

class DefinedUseCasesFragment : Fragment() {

    private var views: FragmentUsecasesBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentUsecasesBinding.inflate(inflater).let {
            views = it
            it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views?.let { views ->
            views.case3.apply {
                val sld = object : StateListDrawable() {
                    override fun setConstantState(state: DrawableContainerState) {
                        //necessary for different text sizes and correct "center" alignment
                        state.isConstantSize = true
                        super.setConstantState(state)
                    }
                }
                sld.addState(
                        intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked),
                        SuperTextDrawable(R.string.action_show, requireContext(), R.style.labelTextAppearance)
                )

                sld.addState(
                        StateSet.WILD_CARD,
                        SuperTextDrawable("Hide", requireContext(), R.style.labelTextAppearance)
                )
                setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, ripple(sld), null)
            }

            views.case5.apply {
                val d = SuperTextDrawable(R.string.action_clear, requireContext(), R.style.labelTextAppearance)
                setCompoundDrawableRelative(0, ripple(d), true)
            }

            views.case11.let { edittext ->
                val isInError = { t: CharSequence? -> (t?.length ?: 0) in (1..5) }
                edittext.onTextChanged { edittext.isError = isInError(it) }
                edittext.isError = isInError(edittext.text)
            }

            views.case12.let { editText ->
                val isInError = { t: CharSequence? -> t?.all { Character.isDigit(it) } ?: false }
                editText.onTextChanged { editText.isError = isInError(it) }
                editText.isError = isInError(editText.text)
            }

            views.case13.apply {
                setCompoundDrawableBehaviourRelative(2, CompoundDrawableBehaviour.Button("a11y") { i, v ->
                    var txt = this.text?.toString() ?: ""
                    if (txt.isNotEmpty()) {
                        txt = if (Character.isUpperCase(txt[0])) {
                            txt.toLowerCase(Locale.UK)
                        } else {
                            txt.toUpperCase(Locale.UK)
                        }
                        setText(txt)
                    }
                })
            }

            views.case14.apply {
                onTextChanged {
                    val len = it?.length ?: 0
                    when {
                        len in 1..3 -> isError = true
                        len > 6 -> isSuccess = true
                        else -> {
                            isError = false
                            isSuccess = false
                        }
                    }
                }
            }
        }
    }

    private fun ripple(drawable: Drawable): RippleDrawable {
        val color = ContextCompat.getColorStateList(requireContext(), R.color.colorAccent)!!
        return RippleDrawable(color, drawable, ShapeDrawable(OvalShape()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        views = null
    }
}

fun TextView.onTextChanged(onTextChanged: (s: CharSequence?) -> Unit) : TextWatcher {
    return textWatcher(onTextChanged).also { addTextChangedListener(it) }
}

fun textWatcher(onTextChanged: (s: CharSequence?) -> Unit): TextWatcher {
    return object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onTextChanged(s)
        }
    }
}