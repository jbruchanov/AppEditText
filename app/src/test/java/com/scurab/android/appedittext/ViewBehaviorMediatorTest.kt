package com.scurab.android.appedittext

import android.app.Instrumentation
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class ViewBehaviorMediatorTest {

    class MyBehaviour : IViewBehaviour<TextView>,
        View.OnFocusChangeListener,
        View.OnClickListener,
        TextWatcher {
        override fun onFocusChange(v: View, hasFocus: Boolean) {
            println("onFocusChange:" + this.toString())
        }

        override fun onClick(v: View?) {
            println("onClick:" + this.toString())
        }

        override fun afterTextChanged(s: Editable?) {
            println("afterTextChanged:" + s)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            println("beforeTextChanged:" + s)
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            println("onTextChanged:" + s)
        }
    }

    @Test
    fun testX() {
        val view = View(RuntimeEnvironment.systemContext)
        val mediator = ViewBehaviorMediator(view)

        val myBehaviour1 = MyBehaviour()
        val myBehaviour2 = MyBehaviour()

        mediator.addBehaviour(myBehaviour1)
        mediator.addBehaviour(myBehaviour2)

        shadowOf(view).setViewFocus(true)
        view.performClick()

//        view.append("Hovno")
//        view.append("Prdel")
    }
}