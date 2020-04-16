package com.scurab.android.appedittext

import TextDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.StateSet
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.scurab.android.appedittext.databinding.ActivityMainBinding
import com.scurab.android.appedittext.drawable.CompoundDrawableBehaviour
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        binding.case3.apply {
            //wee need more powerful text drawable
            //-> stateful, gravity, text appereance, base line alignment,
            // correctly working in the stateListDrawable, fill height etc
            val sld = StateListDrawable()
            sld.addState(
                intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked),
                TextDrawable("Hide", this@MainActivity, R.style.labelTextAppearance)
            )

            sld.addState(
                StateSet.WILD_CARD,
                TextDrawable("Show", this@MainActivity, R.style.labelTextAppearance)
            )

            setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, sld, null)
        }

        binding.case11.let { edittext ->
            val isInError = { t: CharSequence? -> (t?.length ?: 0) in (1..5) }
            edittext.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    edittext.isInError = isInError(s)
                }
            })
            edittext.isInError = isInError(edittext.text)
        }

        binding.case12.let { edittext ->
            val isInError = { t: CharSequence? -> t?.all { Character.isDigit(it) } ?: false }
            edittext.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    edittext.isInError = isInError(s)
                }
            })
            edittext.isInError = isInError(edittext.text)
        }

        binding.case13.apply {
            setCompoundDrawableBehaviour(2, CompoundDrawableBehaviour.Button("a11y") { i, v ->
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
    }
}