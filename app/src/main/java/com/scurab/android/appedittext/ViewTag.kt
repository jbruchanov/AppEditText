package com.scurab.android.appedittext

import android.view.View
import androidx.annotation.AttrRes
import com.scurab.android.appedittext.ViewTag.Companion.setBagItem
import java.util.TreeMap
import kotlin.math.abs

interface IBagItem

interface IViewTag<T : IBagItem> {
    val tagId: Int
}

@Suppress("UNCHECKED_CAST")
open class ViewTag<T : IBagItem>(override val tagId: Int) : IViewTag<T> {

    companion object {
        fun <T : IBagItem> View.setBagItem(tag: IViewTag<T>, item: T) = setTag(tag.tagId, item)
        fun <T : IBagItem> View.getBagItem(tag: IViewTag<T>): T? = getTag(tag.tagId) as T?
        fun <T : IBagItem> View.requireBagItem(tag: IViewTag<T>): T = getTag(tag.tagId) as T
    }
}

interface IViewStateBag : IBagItem {
    //Type private intentionally
    fun View.initStateBag()
    fun setCustomState(@AttrRes stateId: Int, enabled: Boolean)
    fun setCustomStates(states: IntArray)
    fun getCustomStates(): IntArray
    fun registerCustomStates(states: IntArray)
    val statesCount: Int
}

open class ViewStateBag : IViewStateBag, IBagItem {
    private var view: View? = null
    private val map = TreeMap<Int, Boolean>()
    private var data: IntArray? = null
    override val statesCount get() = map.size

    override fun View.initStateBag() {
        check(this@ViewStateBag.view == null) { "ViewStateBag has been already initialised with ${this@ViewStateBag.view}" }
        this@ViewStateBag.view = this
        setBagItem(ViewStatesTag, this@ViewStateBag)
    }

    override fun setCustomState(stateId: Int, enabled: Boolean) {
        check(map.contains(stateId))
        map[stateId] = enabled
        view?.refreshDrawableState()
    }

    override fun setCustomStates(states: IntArray) {
        states.forEach { it ->
            val stateId = abs(it)
            check(map.contains(stateId))
            map[stateId] = it > 0
        }
        view?.refreshDrawableState()
    }

    override fun getCustomStates(): IntArray {
        return data?.let { data ->
            map.keys.forEachIndexed { index, i ->
                data[index] = if (map[i] == true) i else -i
            }
            data
        } ?: Empty
    }

    override fun registerCustomStates(states: IntArray) {
        check(map.isEmpty()) { "View has already registered ${map.size} custom states" }
        if (states.isNotEmpty()) {
            states.forEach { map[it] = false }
            data = IntArray(states.size)
        }
    }

    companion object {
        val ViewStatesTag = ViewTag<ViewStateBag>(R.id.tag_viewbag)
        val Empty = IntArray(0)
    }
}