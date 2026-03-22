package com.checkrate.app.data

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class ExchangeRateApi(private val apiKey: String) {
    data class ApiResponse(
        val timestamp: Long,
        val quotes: Map<String, Double>
    )

    fun fetchQuotes(currencies: Set<String>): ApiResponse {
        if (apiKey.isBlank()) {
            throw IllegalStateException("Missing API key")
        }

        val currencyList = currencies.joinToString(",")
        val endpoint = "http://api.currencylayer.com/live?access_key=$apiKey&currencies=$currencyList"
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseBody = BufferedReader(InputStreamReader(stream)).use { it.readText() }

        val json = JSONObject(responseBody)
        val success = json.optBoolean("success", false)
        if (!success) {
            val message = json.optJSONObject("error")?.optString("info") ?: "Unknown API error"
            throw IllegalStateException(message)
        }

        val timestamp = json.optLong("timestamp", System.currentTimeMillis() / 1000)
        val quotesJson = json.getJSONObject("quotes")
        val quotes = mutableMapOf<String, Double>()
        val keys = quotesJson.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            quotes[key] = quotesJson.optDouble(key)
        }
        return ApiResponse(timestamp = timestamp, quotes = quotes)
    }
}
