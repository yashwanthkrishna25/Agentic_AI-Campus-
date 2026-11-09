//package com.example.data
//
//import android.util.Log
//import com.example.model.ChatMessage
//import com.example.model.UserRole
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import okhttp3.MediaType.Companion.toMediaType
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import okhttp3.RequestBody.Companion.toRequestBody
//import org.json.JSONArray
//import org.json.JSONObject
//import java.util.concurrent.TimeUnit
//
//object GeminiService {
//    private const val TAG = "GeminiService"
//    const val MODEL_FLASH = "gemini-3.8-flash"
//    const val MODEL_FLASH_LITE = "gemini-3.1-flash-lite-preview"
//    const val MODEL_ARGON = "gemini-4-argon"
//
//    private val client by lazy {
//        OkHttpClient.Builder()
//            .connectTimeout(15, TimeUnit.SECONDS)
//            .readTimeout(25, TimeUnit.SECONDS)
//            .writeTimeout(15, TimeUnit.SECONDS)
//            .build()
//    }
//
//    /**
//     * Executes a multi-turn chat completion using Google Gemini models.
//     * Maintains full conversation history, applies system instructions for specific user roles,
//     * and gracefully falls back if API key is not yet set in Secrets panel.
//     */
//    suspend fun sendMultiTurnChat(
//        history: List<ChatMessage>,
//        userMessage: String,
//        role: UserRole,
//        modelName: String = MODEL_FLASH,
//        apiKey: String = ""
//    ): String = withContext(Dispatchers.IO) {
//        if (apiKey.isNotBlank()) {
//            try {
//                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
//                val systemInstruction = buildSystemInstruction(role)
//
//                val contentsArray = JSONArray()
//                // Add conversation history
//                for (item in history.takeLast(10)) {
//                    val partRole = if (item.isFromAi) "model" else "user"
//                    contentsArray.put(JSONObject().apply {
//                        put("role", partRole)
//                        put("parts", JSONArray().put(JSONObject().put("text", item.message)))
//                    })
//                }
//                // Add current message
//                contentsArray.put(JSONObject().apply {
//                    put("role", "user")
//                    put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
//                })
//
//                val requestBody = JSONObject().apply {
//                    put("contents", contentsArray)
//                    put("systemInstruction", JSONObject().apply {
//                        put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
//                    })
//                    put("generationConfig", JSONObject().apply {
//                        put("temperature", 0.7)
//                        put("maxOutputTokens", 800)
//                    })
//                }
//
//                val request = Request.Builder()
//                    .url(url)
//                    .addHeader("Content-Type", "application/json")
//                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
//                    .build()
//
//                val response = client.newCall(request).execute()
//                val responseBody = response.body?.string()
//
//                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
//                    val root = JSONObject(responseBody)
//                    val candidates = root.optJSONArray("candidates")
//                    if (candidates != null && candidates.length() > 0) {
//                        val candidate = candidates.getJSONObject(0)
//                        val content = candidate.optJSONObject("content")
//                        val parts = content?.optJSONArray("parts")
//                        if (parts != null && parts.length() > 0) {
//                            val text = parts.getJSONObject(0).optString("text")
//                            if (!text.isNullOrBlank()) {
//                                return@withContext text.trim()
//                            }
//                        }
//                    }
//                }
//                Log.w(TAG, "Gemini endpoint returned HTTP ${response.code}: $responseBody")
//            } catch (e: Exception) {
//                Log.e(TAG, "Error connecting to Gemini API", e)
//            }
//        }
//
//        // Secondary fallback to AgentRouter AI service
//        val agentRouterResponse = AgentRouterService.sendChatMessage(userMessage, role)
//        if (agentRouterResponse.isNotBlank()) {
//            return@withContext agentRouterResponse
//        }
//
//        return@withContext ""
//    }
//
//    fun buildSystemInstruction(role: UserRole): String {
//        return when (role) {
//            UserRole.STUDENT -> """
//                You are AGENTRA AI Student Academic & Attendance Advisor for KSRM College of Engineering (Autonomous), Kadapa.
//                Tone: Empathetic, clear, encouraging, precise with numbers.
//                Guidelines:
//                - Mandatory Attendance Bar: 75.0%.
//                - If attendance < 75%, advise practical catch-up class hours to reach 75% or 80%.
//                - Assist with engineering course questions (Deep Learning, Cloud Computing, Cyber Security, Java/Kotlin).
//                - Explain Condonation criteria (65% to 75% allowed with valid medical certificate approved by Dean/Principal).
//            """.trimIndent()
//
//            UserRole.FACULTY -> """
//                You are AGENTRA AI Faculty Co-Pilot for KSRMCE Kadapa.
//                Tone: Professional, academic, efficient.
//                Guidelines:
//                - Assist professors with attendance ledger compliance and biometric KNN validation.
//                - Help formulate continuous assessment quizzes, internal exam rubrics, and syllabus pacing.
//                - Guide on identifying students at academic risk early in the semester.
//            """.trimIndent()
//
//            UserRole.HOD -> """
//                You are AGENTRA AI Departmental HOD Co-Pilot for the Head of Department.
//                Tone: Authoritative, strategic, data-driven.
//                Guidelines:
//                - Provide analytics on branch sections, attendance distributions, and faculty workload.
//                - Facilitate sending low attendance alerts to parents and advisors for students below 75%.
//                - Guide on curriculum regulations, NBA accreditation, and department review meetings.
//            """.trimIndent()
//
//            UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> """
//                You are AGENTRA AI Executive Governance Advisor to the Principal and Dean.
//                Tone: Executive, formal, compliance-oriented.
//                Guidelines:
//                - Provide college-wide institutional governance, NAAC/NIRF metrics, and biometrics audit.
//                - Advise on detention enforcement policy, condonation approvals, and campus notifications.
//            """.trimIndent()
//
//            UserRole.PARENT -> """
//                You are AGENTRA AI Parent Companion for guardians of enrolled students.
//                Tone: Respectful, reassuring, transparent.
//                Guidelines:
//                - Explain the child's daily class period attendance and upcoming exam schedule.
//                - Clarify attendance rules (75% mandatory) and process for submitting medical leave.
//            """.trimIndent()
//
//            UserRole.ADMIN -> """
//                You are AGENTRA Central Infrastructure AI Advisor.
//                Tone: Technical, diagnostic, reliable.
//                Guidelines:
//                - Assist with multi-college database synchronizations, App Check, and system monitoring.
//            """.trimIndent()
//        }
//    }
//}




package com.example.data

import android.util.Log
import com.example.model.ChatMessage
import com.example.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {

    private const val TAG = "GeminiService"

    const val MODEL_FLASH = "gemini-3.8-flash"
    const val MODEL_FLASH_LITE = "gemini-3.1-flash-lite-preview"
    const val MODEL_ARGON = "gemini-4-argon"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Sends a multi-turn conversation to Gemini.
     * Falls back to AgentRouter if Gemini is unavailable.
     */
    suspend fun sendMultiTurnChat(
        history: List<ChatMessage>,
        userMessage: String,
        role: UserRole,
        modelName: String = MODEL_FLASH,
        apiKey: String = ""
    ): String = withContext(Dispatchers.IO) {

        if (apiKey.isNotBlank()) {
            try {
                val url =
                    "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

                val systemInstruction = buildSystemInstruction(role)
                val contentsArray = JSONArray()

                // Add the most recent conversation history.
                for (item in history.takeLast(10)) {
                    val partRole =
                        if (item.isFromAi) "model" else "user"

                    contentsArray.put(
                        JSONObject().apply {
                            put("role", partRole)
                            put(
                                "parts",
                                JSONArray().put(
                                    JSONObject().put("text", item.message)
                                )
                            )
                        }
                    )
                }

                // Add the current user message.
                contentsArray.put(
                    JSONObject().apply {
                        put("role", "user")
                        put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put("text", userMessage)
                            )
                        )
                    }
                )

                val requestBody = JSONObject().apply {
                    put("contents", contentsArray)

                    put(
                        "systemInstruction",
                        JSONObject().apply {
                            put(
                                "parts",
                                JSONArray().put(
                                    JSONObject().put(
                                        "text",
                                        systemInstruction
                                    )
                                )
                            )
                        }
                    )

                    put(
                        "generationConfig",
                        JSONObject().apply {
                            put("temperature", 0.7)
                            put("maxOutputTokens", 800)
                        }
                    )
                }

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(
                        requestBody.toString().toRequestBody(
                            "application/json".toMediaType()
                        )
                    )
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    if (response.isSuccessful &&
                        !responseBody.isNullOrBlank()
                    ) {
                        val root = JSONObject(responseBody)
                        val candidates = root.optJSONArray("candidates")

                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")

                            if (parts != null && parts.length() > 0) {
                                val text = parts.getJSONObject(0)
                                    .optString("text", "")

                                if (text.isNotBlank()) {
                                    return@withContext text.trim()
                                }
                            }
                        }
                    }

                    Log.w(
                        TAG,
                        "Gemini endpoint returned HTTP ${response.code}: $responseBody"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to Gemini API", e)
            }
        }

        // Secondary fallback to AgentRouter.
        try {
            val agentRouterResponse =
                AgentRouterService.sendChatMessage(userMessage, role)

            if (agentRouterResponse.isNotBlank()) {
                return@withContext agentRouterResponse
            }
        } catch (e: Exception) {
            Log.e(TAG, "AgentRouter fallback failed", e)
        }

        return@withContext ""
    }

    /**
     * Returns system instructions for every supported user role.
     */
    private fun buildSystemInstruction(role: UserRole): String {
        return when (role) {

            UserRole.STUDENT -> """
                You are AGENTRA AI Student Academic and Attendance Advisor
                for KSRM College of Engineering, Kadapa.

                Tone: Empathetic, clear, encouraging, and precise.

                Guidelines:
                - The mandatory attendance threshold is 75%.
                - Help students calculate attendance and plan catch-up classes.
                - Explain engineering subjects, including Deep Learning,
                  Cloud Computing, Cyber Security, Java, and Kotlin.
                - Explain attendance condonation procedures without
                  claiming that an application has been approved.
                - Never invent student records, attendance figures, or results.
            """.trimIndent()

            UserRole.FACULTY -> """
                You are AGENTRA AI Faculty Co-Pilot for KSRMCE Kadapa.

                Tone: Professional, academic, and efficient.

                Guidelines:
                - Assist with attendance records and academic administration.
                - Help prepare quizzes, internal assessments, and rubrics.
                - Help identify students who may need academic support.
                - Do not invent attendance records, marks, or student details.
            """.trimIndent()

            UserRole.HOD -> """
                You are AGENTRA AI Department Head Co-Pilot.

                Tone: Strategic, professional, and data-driven.

                Guidelines:
                - Assist with department attendance and academic analytics.
                - Help review faculty workload and academic progress.
                - Support department planning and review meetings.
                - Help prepare low-attendance reports and communications.
                - Use verified records when discussing actual students.
            """.trimIndent()

            UserRole.PRINCIPAL,
            UserRole.PRINCIPAL_ADMIN -> """
                You are AGENTRA AI Executive Governance Advisor
                for the Principal and institutional leadership.

                Tone: Executive, formal, and compliance-oriented.

                Guidelines:
                - Assist with institution-wide academic administration.
                - Help interpret verified attendance and academic reports.
                - Support governance, compliance, and institutional planning.
                - Explain approval workflows and audit requirements.
                - Never claim an approval or administrative action succeeded
                  unless the system confirms it.
            """.trimIndent()

            UserRole.PARENT -> """
                You are AGENTRA AI Parent Companion.

                Tone: Respectful, reassuring, and transparent.

                Guidelines:
                - Explain attendance policies and academic schedules.
                - The usual mandatory attendance threshold is 75%.
                - Help parents understand verified academic progress.
                - Do not disclose another student's private information.
                - Only discuss a child's records when access is authorized.
            """.trimIndent()

            UserRole.ADMIN -> """
                You are AGENTRA AI College Administration Advisor.

                Tone: Technical, reliable, and professional.

                Guidelines:
                - Assist with authorized college administration.
                - Help troubleshoot institutional configuration and records.
                - Support account provisioning and approved workflows.
                - Respect role-based permissions and audit requirements.
                - Never bypass access controls or invent system results.
            """.trimIndent()

            UserRole.SUPER_ADMIN -> """
                You are AGENTRA AI Super Administrator Advisor.

                Tone: Professional, security-focused, and precise.

                Guidelines:
                - Assist with authorized platform-wide administration.
                - Help monitor system health and troubleshoot integrations.
                - Explain role-based access control and audit logs.
                - Recommend least-privilege access and secure configuration.
                - Protect institution boundaries and confidential information.
                - Never claim an administrative action succeeded unless
                  the system confirms that it actually succeeded.
            """.trimIndent()
        }
    }
}

