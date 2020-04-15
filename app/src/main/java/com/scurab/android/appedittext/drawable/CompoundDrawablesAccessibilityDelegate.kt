package com.scurab.android.appedittext.drawable

import android.os.Bundle
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import kotlin.math.roundToInt

class CompoundDrawablesAccessibilityDelegate(
    private val controller: ICompoundDrawablesController
) : ExploreByTouchHelper(controller.host) {

    private val virtualViews get() = controller.virtualViews

    override fun getVirtualViewAt(x: Float, y: Float): Int {
        return virtualViews
            .find { it.rect.contains(x.roundToInt(), y.roundToInt()) }?.id
            ?: HOST_ID
    }

    override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
        virtualViews.forEach {
            val id = it.id
            if (controller.getCompoundDrawableClickStrategy(id).isImportantForAccessibility) {
                virtualViewIds.add(id)
            }
        }
    }

    override fun onPerformActionForVirtualView(
        virtualViewId: Int,
        action: Int,
        arguments: Bundle?
    ): Boolean {
        controller
            .getCompoundDrawableClickStrategy(virtualViewId)
            .onAccessibilityAction(action, arguments)
        return true
    }

    override fun onPopulateNodeForVirtualView(
        virtualViewId: Int,
        node: AccessibilityNodeInfoCompat
    ) {
        controller
            .getCompoundDrawableClickStrategy(virtualViewId)
            .onPopulateNode(node)
    }
}