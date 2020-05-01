package com.scurab.android.appedittext

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText


interface IViewBehaviour<T : View>

open class ViewBehaviorMediator(private val view: View) {

    private val behaviours = mutableSetOf<IViewBehaviour<*>>()
    private val eventRegister by lazy { createEventRegister() }
    private val registeredIfaces = mutableSetOf<Any>()

    open fun addBehaviour(behaviour: IViewBehaviour<out View>) {
        behaviours.add(behaviour)

        eventRegister.forEach {
            if (it.iface.isInstance(behaviour)) {
                if (!registeredIfaces.contains(it.iface)) {
                    val added = it.register(view, behaviours)
                    if (added) {
                        registeredIfaces.add(it.iface)
                    }
                }
            }
        }
    }

    open fun removeBehaviour(behaviour: IViewBehaviour<View>) {
        behaviours.remove(behaviour)
        //TODO: unregister?
    }

    protected open fun createEventRegister(): List<ViewBehaviourBinding<*, out View>> {
        //View.OnLongClickListener
        //View.OnTouchListener()
        //View.OnLayoutChangeListener
        //View.OnAttachStateChangeListener
//        return listOf(focusChangeAction, keyListenerAction, textChangedAction, onClickAction)
        return listOf(focusChangeAction, textChangedAction, onClickListener)
    }

    companion object {

        val focusChangeAction = binding<View, View.OnFocusChangeListener> { view, behaviours ->
            view.setOnFocusChangeListener { v, hasFocus ->
                behaviours.forEach { it.onFocusChange(v, hasFocus) }
            }
        }

        val textChangedAction =
            binding<EditText, TextWatcher> { editText, textWatchers ->
                editText.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        textWatchers.forEach { it.afterTextChanged(s) }
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                        textWatchers.forEach { it.beforeTextChanged(s, start, count, after) }
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        textWatchers.forEach { it.onTextChanged(s, start, before, count) }
                    }
                })
            }

        val onClickListener = binding<View, View.OnClickListener> { view, iterable ->
            view.setOnClickListener { v ->
                iterable.forEach { it.onClick(v) }
            }
        }

        inline fun <reified V : View, reified T> binding(
            noinline block: (V, Iterable<T>) -> Unit
        ): ViewBehaviourBinding<T, V> =  binding(V::class.java, T::class.java, block)

        @JvmStatic
        fun <V : View, T> binding(
            viewType: Class<V>,
            iface: Class<T>,
            block: (V, Iterable<T>) -> Unit
        ): ViewBehaviourBinding<T, V> {
            return ViewBehaviourBinding(viewType, iface, block)
        }
    }

    open class ViewBehaviourBinding<T, V : View>(
        val viewType: Class<V>,
        val iface: Class<T>,
        private val registerAction: (V, Iterable<T>) -> Unit
    ) {
        internal fun register(view: View, collection: Collection<IViewBehaviour<*>>): Boolean {
            val isInstance = viewType.isInstance(view)
            if (isInstance) {
                register(view as V, collection as Collection<IViewBehaviour<out V>>)
            }
            return isInstance
        }

        protected open fun register(view: V, collection: Collection<IViewBehaviour<out V>>) {
            registerAction.invoke(view, TypeIterable(iface, collection))
        }
    }

    class TypeIterable<T>(private val type: Class<T>, private val inner: Collection<Any>) :
        Iterable<T> {
        override fun iterator(): Iterator<T> = TypeIterator(type, inner.iterator())
    }

    class TypeIterator<T>(private val type: Class<T>, private val inner: Iterator<Any>) :
        Iterator<T> {
        private var next: T? = null
        private val iterator = inner.iterator()

        override fun hasNext(): Boolean {
            while (inner.hasNext()) {
                val n = inner.next()
                if (type.isInstance(n)) {
                    next = n as T
                    return true
                }
            }
            return false
        }

        @Suppress("UNCHECKED_CAST")
        override fun next(): T {
            val t = next as T
            next = null
            return t
        }
    }
}