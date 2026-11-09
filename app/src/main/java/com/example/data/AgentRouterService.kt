package com.example.data

import com.example.model.UserRole

/** Compatibility adapter: all AI requests go through the authenticated campus backend. */
object AgentRouterService {
    @Deprecated("Provider keys are server-side only; apiKey is intentionally ignored.")
    suspend fun sendChatMessage(userPrompt: String, role: UserRole, apiKey: String = ""): String {
        return OpenRouterAiService.getChatCompletion(userPrompt, role).getOrElse { "" }
    }
}
