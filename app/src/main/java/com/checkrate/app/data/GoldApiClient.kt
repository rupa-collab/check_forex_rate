package com.checkrate.app.data

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class GoldApiClient {
    data class MetalPrice(
        val metal: String,
        val priceUsdPerOunce: Double,
        val timestamp: Long
    )

    fun fetchPrice(metal: String): MetalPrice {
        val endpoint = "https://api.gold-api.com/price/$metal"
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseBody = BufferedReader(InputStreamReader(stream)).use { it.readText() }

        val json = JSONObject(responseBody)
        val price = json.optDouble("price", Double.NaN)
        if (price.isNaN()) {
            val message = json.optString("message", "Gold API error")
            throw IllegalStateException(message)
        }
        val ts = json.optLong("timestamp", System.currentTimeMillis() / 1000)
        return MetalPrice(metal = metal, priceUsdPerOunce = price, timestamp = ts)
    }
}
