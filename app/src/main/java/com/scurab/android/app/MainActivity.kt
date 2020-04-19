package com.scurab.android.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.scurab.android.appedittext.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this).apply {
            id = R.id.container
        }
        setContentView(root)

        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.container, MenuFragment())
                    .commit()
        }
    }
}


class MenuFragment : Fragment() {

    private val list = listOf(
            "Usecases" to DefinedUseCasesFragment::class.java,
            "Other usecases" to OtherUseCasesFragment::class.java
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = inflater.context
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL

            list.forEach { (name, fragment) ->
                addView(Button(context).apply {
                    text = name
                    setOnClickListener { openFragment(fragment) }
                })
            }
        }
    }

    private fun openFragment(fragment: Class<out Fragment>) {
        requireFragmentManager()
                .beginTransaction()
                .addToBackStack(null)
                .replace(R.id.container, fragment.newInstance())
                .commit()
    }
}
