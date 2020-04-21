package com.scurab.android.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.scurab.android.appedittext.R
import com.scurab.android.appedittext.databinding.FragmentOtherUsecasesBinding
import kotlin.math.abs

class OtherUseCasesFragment : Fragment() {

    private var views: FragmentOtherUsecasesBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentOtherUsecasesBinding.inflate(inflater).let {
            views = it
            it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views?.let { views ->
            views.case1.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    views.case1.isInError = (s?.length ?: 0) in (1..5)
                }
            })

            views.case1toggle.setOnCheckedChangeListener { _, isChecked ->
                views.case1.isEnabled = !isChecked
            }

            views.case2.apply {
                addTextChangedListener(object : TextWatcher {
                    private val states = intArrayOf(
                            R.attr.state_pwd_strength_bad,
                            R.attr.state_pwd_strength_weak,
                            R.attr.state_pwd_strength_good,
                            R.attr.state_pwd_strength_perfect
                    )

                    override fun afterTextChanged(s: Editable?) {}
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        //turn off states
                        states.forEachIndexed { i, v -> states[i] = -abs(v) }
                        val len = s?.length ?: 0
                        val stateIndex = when {
                            len == 0 -> -1
                            len > 8 -> 3
                            len > 6 -> 2
                            len > 3 -> 1
                            else -> 0
                        }
                        if (stateIndex >= 0) {
                            states[stateIndex] = abs(states[stateIndex])
                        }
                        setCustomStates(states)
                    }
                })
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        views = null
    }
}
