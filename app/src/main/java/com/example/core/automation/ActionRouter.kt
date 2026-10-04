package com.example.core.automation

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExecutionOutcome(
    val success: Boolean,
    val executionMethod: String, // "INTENT", "ACCESSIBILITY", "SHIZUKU", "BROWSER", "INTERNAL"
    val outputMessage: String,
    val verificationEvidence: String? = null
)

class ActionRouter(
    private val context: Context,
    private val intentEngine: IntentEngine,
    private val shizukuEngine: ShizukuEngine,
    private val actionVerifier: ActionVerifier
) {
    suspend fun executeAction(
        actionType: String,
        target: String,
        parameter: String? = null
    ): ExecutionOutcome = withContext(Dispatchers.Default) {
        when (actionType.lowercase()) {
            "launch_app", "open_app" -> {
                // 1. Try official Intent first
                val launched = intentEngine.launchAppByNameOrPackage(target)
                if (launched) {
                    val verification = actionVerifier.verifyUiAction(
                        expectedState = target,
                        actionType = "open_app",
                        targetIdentifier = target
                    )
                    return@withContext ExecutionOutcome(
                        success = true,
                        executionMethod = "INTENT",
                        outputMessage = "Successfully launched '$target' via Android Intent.",
                        verificationEvidence = verification.evidence
                    )
                }

                // 2. Fallback to Accessibility click if available
                val service = AerisAccessibilityService.instance
                if (service != null && service.clickElementByTextOrId(target)) {
                    val verification = actionVerifier.verifyUiAction(target, "click", target)
                    return@withContext ExecutionOutcome(
                        success = true,
                        executionMethod = "ACCESSIBILITY",
                        outputMessage = "Located and clicked '$target' icon in launcher.",
                        verificationEvidence = verification.evidence
                    )
                }

                ExecutionOutcome(
                    success = false,
                    executionMethod = "INTENT",
                    outputMessage = "Could not find or launch application '$target'.",
                    verificationEvidence = "No matching package or icon identified."
                )
            }

            "open_url" -> {
                val opened = intentEngine.openUrl(target)
                ExecutionOutcome(
                    success = opened,
                    executionMethod = "INTENT",
                    outputMessage = if (opened) "Opened URL '$target' in browser." else "Failed to open URL '$target'.",
                    verificationEvidence = if (opened) "Intent resolved to default web browser." else null
                )
            }

            "open_settings" -> {
                val opened = intentEngine.openSystemSettings(target)
                ExecutionOutcome(
                    success = opened,
                    executionMethod = "INTENT",
                    outputMessage = if (opened) "Opened Android Settings for '$target'." else "Failed to open settings.",
                    verificationEvidence = if (opened) "Settings activity started." else null
                )
            }

            "click_element" -> {
                val service = AerisAccessibilityService.instance
                if (service != null) {
                    val clicked = service.clickElementByTextOrId(target)
                    val verification = actionVerifier.verifyUiAction(
                        expectedState = target,
                        actionType = "click",
                        targetIdentifier = target
                    )
                    ExecutionOutcome(
                        success = clicked,
                        executionMethod = "ACCESSIBILITY",
                        outputMessage = if (clicked) "Clicked UI element '$target'." else "Target element '$target' not clickable.",
                        verificationEvidence = verification.evidence
                    )
                } else {
                    ExecutionOutcome(
                        success = false,
                        executionMethod = "ACCESSIBILITY",
                        outputMessage = "Accessibility Service is not enabled. Please enable Aeris in Settings.",
                        verificationEvidence = "Service offline."
                    )
                }
            }

            "type_text" -> {
                val service = AerisAccessibilityService.instance
                if (service != null) {
                    val textToInput = parameter ?: ""
                    val inputted = service.inputText(textToInput, targetHint = target)
                    val verification = actionVerifier.verifyUiAction(
                        expectedState = textToInput,
                        actionType = "type_text",
                        targetIdentifier = target
                    )
                    ExecutionOutcome(
                        success = inputted,
                        executionMethod = "ACCESSIBILITY",
                        outputMessage = if (inputted) "Typed text into field." else "Could not focus target input field '$target'.",
                        verificationEvidence = verification.evidence
                    )
                } else {
                    ExecutionOutcome(
                        success = false,
                        executionMethod = "ACCESSIBILITY",
                        outputMessage = "Accessibility Service is disabled.",
                        verificationEvidence = null
                    )
                }
            }

            "scroll" -> {
                val service = AerisAccessibilityService.instance
                if (service != null) {
                    val isForward = parameter?.lowercase() != "up" && parameter?.lowercase() != "backward"
                    val scrolled = service.scroll(isForward)
                    ExecutionOutcome(
                        success = scrolled,
                        executionMethod = "ACCESSIBILITY",
                        outputMessage = if (scrolled) "Scrolled ${if (isForward) "down" else "up"}." else "Unable to scroll current window.",
                        verificationEvidence = "Scroll gesture executed."
                    )
                } else {
                    ExecutionOutcome(
                        success = false,
                        executionMethod = "ACCESSIBILITY",
                        outputMessage = "Accessibility service unavailable.",
                        verificationEvidence = null
                    )
                }
            }

            else -> {
                ExecutionOutcome(
                    success = false,
                    executionMethod = "UNKNOWN",
                    outputMessage = "Unsupported action type: $actionType",
                    verificationEvidence = null
                )
            }
        }
    }
}
