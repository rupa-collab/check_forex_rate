package com.checkrate.app.data

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class AuthApi(private val baseUrl: String) {
    fun requestOtp(email: String): String {
        val response = requestJson("/auth/request-otp", JSONObject(mapOf("email" to email)))
        val json = JSONObject(response)
        return json.optString("otp", "")
    }

    fun verifyOtp(email: String, password: String, otp: String): String {
        val response = requestJson(
            "/auth/verify-otp",
            JSONObject(mapOf("email" to email, "password" to password, "otp" to otp))
        )
        val json = JSONObject(response)
        val token = json.optString("access_token", "")
        if (token.isBlank()) {
            throw IllegalStateException("Missing access token")
        }
        return token
    }

    fun login(email: String, password: String): String {
        val response = requestJson("/auth/login", JSONObject(mapOf("email" to email, "password" to password)))
        val json = JSONObject(response)
        val token = json.optString("access_token", "")
        if (token.isBlank()) {
            throw IllegalStateException("Missing access token")
        }
        return token
    }

    private fun requestJson(path: String, body: JSONObject): String {
        if (baseUrl.isBlank()) {
            throw IllegalStateException("AUTH_API_BASE_URL is missing")
        }
        val url = URL(baseUrl.trimEnd('/') + path)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        connection.outputStream.use { os ->
            os.write(body.toString().toByteArray())
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseBody = BufferedReader(InputStreamReader(stream)).use { it.readText() }

        if (responseCode !in 200..299) {
            val message = try {
                JSONObject(responseBody).optString("detail", responseBody)
            } catch (_: Exception) {
                responseBody
            }
            throw IllegalStateException(message)
        }

        return responseBody
    }
}
