package com.example.core.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class UiNodeData(
    val text: String?,
    val viewId: String?,
    val className: String?,
    val contentDescription: String?,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isChecked: Boolean,
    val bounds: Rect
)

class AerisAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AerisAccessibilityService? = null
            private set

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _lastCapturedHierarchy = MutableStateFlow<String?>(null)
        val lastCapturedHierarchy: StateFlow<String?> = _lastCapturedHierarchy.asStateFlow()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Active event logging or window change listener
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isConnected.value = false
    }

    fun captureUiHierarchy(): List<UiNodeData> {
        val root = rootInActiveWindow ?: return emptyList()
        val nodes = mutableListOf<UiNodeData>()
        traverseNode(root, nodes)
        _lastCapturedHierarchy.value = formatNodesJson(nodes)
        return nodes
    }

    private fun traverseNode(node: AccessibilityNodeInfo, list: MutableList<UiNodeData>) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        val viewId = node.viewIdResourceName
        val className = node.className?.toString()

        if (!text.isNullOrBlank() || !desc.isNullOrBlank() || node.isClickable || node.isEditable) {
            list.add(
                UiNodeData(
                    text = text,
                    viewId = viewId,
                    className = className,
                    contentDescription = desc,
                    isClickable = node.isClickable,
                    isEditable = node.isEditable,
                    isChecked = node.isChecked,
                    bounds = bounds
                )
            )
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseNode(child, list)
        }
    }

    fun clickElementByTextOrId(query: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val foundNodes = root.findAccessibilityNodeInfosByText(query)
        if (foundNodes.isNotEmpty()) {
            for (node in foundNodes) {
                if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        return true
                    }
                    parent = parent.parent
                }
            }
        }

        if (root.viewIdResourceName?.contains(query, ignoreCase = true) == true) {
            if (root.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        }

        return false
    }

    fun inputText(text: String, targetHint: String? = null): Boolean {
        val root = rootInActiveWindow ?: return false
        val targetNode: AccessibilityNodeInfo? = if (targetHint != null) {
            val nodes = root.findAccessibilityNodeInfosByText(targetHint)
            nodes.firstOrNull { it.isEditable } ?: root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        } else {
            root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        }

        if (targetNode != null && targetNode.isEditable) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
        return false
    }

    fun scroll(forward: Boolean = true): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return root.performAction(action)
    }

    fun clickAtCoordinates(x: Float, y: Float, callback: (Boolean) -> Unit = {}) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0, 50)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    callback(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    callback(false)
                }
            }, null)
        } else {
            callback(false)
        }
    }

    private fun formatNodesJson(nodes: List<UiNodeData>): String {
        val array = JSONArray()
        for (n in nodes) {
            array.put(JSONObject().apply {
                put("text", n.text.orEmpty())
                put("viewId", n.viewId.orEmpty())
                put("class", n.className.orEmpty())
                put("desc", n.contentDescription.orEmpty())
                put("clickable", n.isClickable)
                put("editable", n.isEditable)
                put("checked", n.isChecked)
            })
        }
        return array.toString()
    }
}
