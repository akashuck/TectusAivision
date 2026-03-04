package com.example.iris

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

class IrisAccessibilityService : AccessibilityService() {

    private var tts: TextToSpeech? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        serviceInfo = info

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    fun performIrisAction(action: String, target: String, value: String?, response: String) {
        if (isSecurityBlocked(target, value)) {
            speakWarning("Action blocked for security reasons.")
            return
        }

        val rootNode = rootInActiveWindow ?: return
        
        when (action.lowercase()) {
            "click" -> {
                val nodes = findNodesByText(rootNode, target)
                if (nodes.isNotEmpty()) {
                    nodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
            "scroll" -> {
                if (target.lowercase().contains("forward")) {
                    rootNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                } else if (target.lowercase().contains("backward")) {
                    rootNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                }
            }
            "type" -> {
                val nodes = findNodesByText(rootNode, target)
                if (nodes.isNotEmpty() && value != null) {
                    val arguments = Bundle()
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
                    nodes[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                }
            }
        }
        
        speakResponse(response)
    }

    private fun findNodesByText(rootNode: AccessibilityNodeInfo, text: String): List<AccessibilityNodeInfo> {
        val foundNodes = mutableListOf<AccessibilityNodeInfo>()
        searchNodes(rootNode, text, foundNodes)
        return foundNodes
    }

    private fun searchNodes(node: AccessibilityNodeInfo?, text: String, foundNodes: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        
        val nodeText = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        
        if (nodeText.contains(text, ignoreCase = true) || contentDesc.contains(text, ignoreCase = true)) {
            foundNodes.add(node)
        }
        
        for (i in 0 until node.childCount) {
            searchNodes(node.getChild(i), text, foundNodes)
        }
    }

    private fun isSecurityBlocked(target: String, value: String?): Boolean {
        val blockedKeywords = listOf("Pay", "Transfer", "Send Money", "Confirm Payment", "UPI")
        val blockedPackages = listOf("bank", "pay", "wallet")

        val currentPackage = rootInActiveWindow?.packageName?.toString() ?: ""
        if (blockedPackages.any { currentPackage.contains(it, ignoreCase = true) }) return true

        if (blockedKeywords.any { target.contains(it, ignoreCase = true) }) return true
        if (value != null && blockedKeywords.any { value.contains(it, ignoreCase = true) }) return true

        return false
    }

    private fun speakWarning(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "warning")
    }

    private fun speakResponse(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "response")
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
    }
}
