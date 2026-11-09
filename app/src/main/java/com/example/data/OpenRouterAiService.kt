package com.example.data

import android.util.Log
import com.example.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AI Completion Service adapter.
 * Directly routes AI requests through the high-speed Groq Cloud LLM engine
 * with clean ChatGPT-style plain text formatting (no asterisks).
 */
object OpenRouterAiService {
    private const val TAG = "OpenRouterAiService"

    fun buildSystemPrompt(userRole: UserRole, userContext: String? = null): String {
        return GroqAiService.buildSystemPrompt(userRole, userContext)
    }

    suspend fun getChatCompletion(
        userMessage: String,
        userRole: UserRole,
        history: List<Pair<String, String>> = emptyList(),
        userContext: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        // Direct routing to Groq Cloud API
        val groqResult = GroqAiService.getChatCompletion(
            userMessage = userMessage,
            userRole = userRole,
            history = history,
            userContext = userContext
        )

        if (groqResult.isSuccess) {
            val reply = groqResult.getOrNull().orEmpty()
            if (reply.isNotBlank()) {
                return@withContext Result.success(GroqAiService.cleanNormalChatGptResponse(reply))
            }
        }

        Log.w(TAG, "Groq AI query fallback: ${groqResult.exceptionOrNull()?.message}")
        groqResult
    }
}
