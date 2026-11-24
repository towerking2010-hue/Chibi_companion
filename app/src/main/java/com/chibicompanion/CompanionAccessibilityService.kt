package com.chibicompanion

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Optional, user-enabled screen context. Captures only accessible UI labels in memory;
 * it does not save screenshots, passwords, or screen history.
 */
class CompanionAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        instance = this
        super.onServiceConnected()
        configureServiceInfo()
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun navigate(action: String): Boolean = when (action) {
        "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
        "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
        "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        "quick_settings" -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        "scroll_down" -> scrollActiveWindow(true)
        "scroll_up" -> scrollActiveWindow(false)
        else -> false
    }

    private fun scrollActiveWindow(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val target = findScrollable(root) ?: return false
        return target.performAction(if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    }

    private fun findScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val found = findScrollable(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    companion object {
        @Volatile private var instance: CompanionAccessibilityService? = null
        fun navigateCurrent(action: String): Boolean = instance?.navigate(action) ?: false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !AssistantPreferences.screenAwarenessEnabled(this)) return
        if (event.eventType !in setOf(
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                AccessibilityEvent.TYPE_VIEW_SCROLLED
            )) return
        val root = rootInActiveWindow ?: return
        val lines = LinkedHashSet<String>()
        collect(root, lines, 0)
        val packageName = event.packageName?.toString().orEmpty()
        val text = lines.take(100).joinToString("\n").take(5000)
        if (text.isNotBlank()) ScreenContext.update(packageName, text)
    }

    private fun collect(node: AccessibilityNodeInfo?, out: MutableSet<String>, depth: Int) {
        if (node == null || depth > 18 || out.size >= 120) return
        val label = (node.text ?: node.contentDescription)?.toString()?.trim().orEmpty()
        // Do not collect editable fields: they may contain passwords or private messages.
        if (label.isNotBlank() && !node.isPassword && !node.isEditable) out.add(label)
        for (i in 0 until node.childCount) collect(node.getChild(i), out, depth + 1)
    }

    override fun onInterrupt() { ScreenContext.clear() }
    private fun configureServiceInfo() {
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 350
        }
    }
}

object ScreenContext {
    @Volatile private var packageName: String = ""
    @Volatile private var visibleText: String = ""

    fun update(pkg: String, text: String) { packageName = pkg; visibleText = text }
    fun clear() { packageName = ""; visibleText = "" }
    fun describe(): String {
        val text = visibleText
        if (text.isBlank()) return "No accessible screen content is available. The user may need to enable Screen Awareness or open a screen with accessible text."
        return "Foreground app package: $packageName\nVisible accessible UI text (editable/password fields excluded):\n$text"
    }
}
