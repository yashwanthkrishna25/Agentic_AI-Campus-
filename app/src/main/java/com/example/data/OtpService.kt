package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/** Email/phone format checks and secure-backend OTP calls. OTP generation and storage stay server-side. */
object OtpService {
    private val EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val client by lazy {
        OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
    }

    fun isValidEmail(email: String): Boolean = email.isNotBlank() && EMAIL_PATTERN.matcher(email.trim()).matches()

    fun isValidPhoneNumber(phone: String): Boolean {
        val clean = phone.trim().replace(" ", "").replace("-", "")
        return clean.matches(Regex("^(\\+91)?[6-9]\\d{9}$"))
    }

    suspend fun sendEmailOtp(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.AGENTRA_BACKEND_URL.trim().trimEnd('/')
        if (!baseUrl.startsWith("https://")) return@withContext Result.failure(
            IllegalStateException("Email verification is not configured. Set AGENTRA_BACKEND_URL to your HTTPS backend.")
        )
        try {
            val body = JSONObject().put("email", email.trim().lowercase()).toString()
            val request = Request.Builder().url("$baseUrl/api/otp/send")
                .header("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType())).build()
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(IllegalStateException(JSONObjectOrFallback.message(responseBody, "Could not send OTP. Please try again later.")))
            }
        } catch (_: Exception) {
            Result.failure(IllegalStateException("Could not reach the email verification service. Check your connection."))
        }
    }

    suspend fun verifyEmailOtp(email: String, enteredOtp: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.AGENTRA_BACKEND_URL.trim().trimEnd('/')
        if (!baseUrl.startsWith("https://")) return@withContext Result.failure(
            IllegalStateException("Email verification backend is not configured.")
        )
        if (!enteredOtp.matches(Regex("^\\d{6}$"))) return@withContext Result.success(false)
        try {
            val body = JSONObject().put("email", email.trim().lowercase()).put("otp", enteredOtp.trim()).toString()
            val request = Request.Builder().url("$baseUrl/api/otp/verify")
                .header("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType())).build()
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) Result.success(false)
                else Result.success(JSONObject(responseBody).optBoolean("verified", false))
            }
        } catch (_: Exception) {
            Result.failure(IllegalStateException("Could not reach the email verification service."))
        }
    }

    // OTP verification is server-side; the client cannot assert that an email is verified.
    fun isEmailVerified(email: String): Boolean = false
}

private object JSONObjectOrFallback {
    fun message(body: String, fallback: String): String = try {
        JSONObject(body).optString("error").ifBlank { fallback }
    } catch (_: Exception) { fallback }
}
