package com.scurab.android.appedittext

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.scurab.android.appedittext.drawable.CompoundDrawableBehaviour

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<AppEditText>(R.id.simpleButton).apply {
            setCompoundDrawableBehaviour(
                2,
                CompoundDrawableBehaviour.Button(R.string.app_name) { i, tv ->
                    Toast.makeText(this@MainActivity, "Simple Button Click", Toast.LENGTH_SHORT)
                        .show()
                })
            setCompoundDrawableBehaviour(
                3,
                CompoundDrawableBehaviour.Button(R.string.app_name) { i, tv ->
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.let {
                        it.hideSoftInputFromWindow(tv.windowToken, 0)
                    }
                })
        }

        findViewById<AppEditText>(R.id.password).apply {
            setCompoundDrawableBehaviour(2, CompoundDrawableBehaviour.PasswordButton())
        }

        findViewById<AppEditText>(R.id.clear_text).apply {
            setCompoundDrawableBehaviour(2, CompoundDrawableBehaviour.ClearButton())
            setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_clear, 0)
        }
    }
}