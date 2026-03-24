package com.checkrate.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import com.checkrate.app.ui.appOutlinedTextFieldColors
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.checkrate.app.util.RateUtils
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private data class ForexDisplay(val code2: String, val target: String)

private val forexDisplayList = listOf(
    ForexDisplay("IN", "INR"),
    ForexDisplay("GB", "GBP"),
    ForexDisplay("EU", "EUR"),
    ForexDisplay("AU", "AUD"),
    ForexDisplay("AE", "AED")
)

@Composable
internal fun ForexRatesCard(
    baseCurrency: String,
    lastUpdatedMillis: Long,
    rates: Map<String, Double>,
    previousRates: Map<String, Double>,
    onRefresh: () -> Unit,
    currencyOptions: List<String>,
    onBaseCurrencyChange: (String) -> Unit
) {
    val rateFormat = DecimalFormat("0.####")
    val pctFormat = DecimalFormat("0.##")
    val updatedText = formatLastUpdated(lastUpdatedMillis)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Forex Rates",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                    }
                }

                CurrencyDropdown(
                    options = currencyOptions,
                    selected = baseCurrency,
                    onSelected = onBaseCurrencyChange
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                updatedText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            forexDisplayList.forEach { item ->
                val storedRate = rates[item.target]
                val storedPrev = previousRates[item.target]
                val displayRate = storedRate?.let { if (it == 0.0) null else 1.0 / it }
                val displayPrev = storedPrev?.let { if (it == 0.0) null else 1.0 / it }

                val changePct = if (displayRate != null && displayPrev != null && displayPrev != 0.0) {
                    (displayRate - displayPrev) / displayPrev * 100.0
                } else {
                    0.0
                }
                val showChange = displayRate != null && displayPrev != null && displayPrev != 0.0
                val arrow = if (!showChange) "" else if (changePct >= 0) "▲" else "▼"
                val changeColor = if (!showChange) MaterialTheme.colorScheme.onSurfaceVariant
                else if (changePct >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)

                val rateText = displayRate?.let { rateFormat.format(it) } ?: "--"
                val changeText = if (showChange) "${pctFormat.format(abs(changePct))}%" else "--"
                val subText = if (displayRate != null) {
                    "1 ${baseCurrency.uppercase()} = ${rateFormat.format(displayRate)} ${item.target}"
                } else {
                    "Rate unavailable"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDBEAFE)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    item.code2,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "${baseCurrency.uppercase()}/${item.target}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                subText,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                rateText,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "$arrow $changeText",
                                color = changeColor,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
internal fun CurrencyConverterCard(
    baseCurrency: String,
    rates: Map<String, Double>,
    currencyOptions: List<String>
) {
    val format = DecimalFormat("0.##")
    val rateFormat = DecimalFormat("0.####")
    var fromAmount by remember { mutableStateOf("100") }
    var fromCurrency by remember { mutableStateOf("USD") }
    var toCurrency by remember { mutableStateOf("INR") }

    val parsedAmount = fromAmount.toDoubleOrNull() ?: 0.0
    val converted = convertAmount(parsedAmount, fromCurrency, toCurrency, baseCurrency, rates)
    val convertedText = converted?.let { format.format(it) } ?: "--"
    val singleRate = convertAmount(1.0, fromCurrency, toCurrency, baseCurrency, rates)
    val singleRateText = singleRate?.let { rateFormat.format(it) } ?: "--"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Currency Converter",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            Text("From", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = fromAmount,
                    onValueChange = { fromAmount = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = appOutlinedTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                CurrencyDropdown(
                    options = currencyOptions,
                    selected = fromCurrency,
                    onSelected = { fromCurrency = it }
                )
            }

            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        val temp = fromCurrency
                        fromCurrency = toCurrency
                        toCurrency = temp
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(20.dp))
                ) {
                    Icon(Icons.Filled.SwapVert, contentDescription = "Swap", tint = Color.White)
                }
            }
            Spacer(Modifier.height(12.dp))

            Text("To", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = convertedText,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    colors = appOutlinedTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                CurrencyDropdown(
                    options = currencyOptions,
                    selected = toCurrency,
                    onSelected = { toCurrency = it }
                )
            }

            Spacer(Modifier.height(12.dp))
            if (singleRate != null) {
                Text(
                    "1 $fromCurrency = $singleRateText $toCurrency",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    "Rate unavailable",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
internal fun PreciousMetalsCard(
    baseCurrency: String,
    fxRates: Map<String, Double>,
    metalsRates: Map<String, Double>,
    lastUpdatedMillis: Long
) {
    val updatedText = formatLastUpdated(lastUpdatedMillis)
    var showUsd by remember { mutableStateOf(true) }

    val usdPerBase = if (baseCurrency.uppercase() == "USD") 1.0 else fxRates["USD"]
    val goldUsd = metalsRates["XAU"]?.let { basePerGram ->
        if (usdPerBase == null) null else (basePerGram / usdPerBase) * RateUtils.GRAMS_PER_TROY_OUNCE
    }
    val silverUsd = metalsRates["XAG"]?.let { basePerGram ->
        if (usdPerBase == null) null else (basePerGram / usdPerBase) * RateUtils.GRAMS_PER_TROY_OUNCE
    }

    val inrPerUsd = fxRates["USD"]?.let { basePerUsd ->
        val basePerInr = fxRates["INR"]
        if (basePerInr == null || basePerInr == 0.0) null else basePerUsd / basePerInr
    }

    val currencyLabel = if (showUsd) "USD" else "INR"
    val goldDisplay = if (showUsd) goldUsd else goldUsd?.let { value ->
        inrPerUsd?.let { value * it }
    }
    val silverDisplay = if (showUsd) silverUsd else silverUsd?.let { value ->
        inrPerUsd?.let { value * it }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Precious Metals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "USD",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (showUsd) MaterialTheme.colorScheme.secondary else Color(0xFF6B7280),
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = showUsd,
                        onCheckedChange = { showUsd = it },
                        modifier = Modifier.padding(horizontal = 6.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.secondary,
                            checkedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                            uncheckedThumbColor = Color(0xFF6B7280),
                            uncheckedTrackColor = Color(0xFFCBCED4)
                        )
                    )
                    Text(
                        "INR",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (!showUsd) MaterialTheme.colorScheme.secondary else Color(0xFF6B7280),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                updatedText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            MetalRateItem(
                emoji = "🏅",
                name = "Gold",
                symbol = "XAU",
                price = goldDisplay,
                currency = currencyLabel
            )
            Spacer(Modifier.height(12.dp))
            MetalRateItem(
                emoji = "🥈",
                name = "Silver",
                symbol = "XAG",
                price = silverDisplay,
                currency = currencyLabel
            )

            Spacer(Modifier.height(10.dp))
            Text(
                "Prices update every 5 seconds while this screen is open",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFDBEAFE), RoundedCornerShape(10.dp))
                    .padding(vertical = 6.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MetalRateItem(
    emoji: String,
    name: String,
    symbol: String,
    price: Double?,
    currency: String
) {
    val format = DecimalFormat("0.##")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(Color(0xFFFEF3C7), Color(0xFFFEF08A))
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .border(1.dp, Color(0xFFFCD34D), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(symbol, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    price?.let { "${if (currency == "USD") "$" else ""}${format.format(it)}" } ?: "--",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text("${currency}/oz", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
@Composable
private fun CurrencyDropdown(
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
        ) {
            Text(selected)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    }
                )
            }
        }
    }
}

private fun convertAmount(
    amount: Double,
    from: String,
    to: String,
    base: String,
    rates: Map<String, Double>
): Double? {
    val basePerFrom = if (from.uppercase() == base.uppercase()) 1.0 else rates[from.uppercase()]
    val basePerTo = if (to.uppercase() == base.uppercase()) 1.0 else rates[to.uppercase()]
    if (basePerFrom == null || basePerTo == null) return null
    return amount * (basePerFrom / basePerTo)
}

private fun formatLastUpdated(lastUpdatedMillis: Long): String {
    if (lastUpdatedMillis <= 0L) return "Last updated: --"
    val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return "Last updated: ${formatter.format(Date(lastUpdatedMillis))}"
}
