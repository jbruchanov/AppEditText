package com.scurab.android.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.scurab.android.appedittext.databinding.FragmentOtherUsecasesBinding

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
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        views = null
    }
}
