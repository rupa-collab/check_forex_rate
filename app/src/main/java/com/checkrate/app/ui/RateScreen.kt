package com.checkrate.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.checkrate.app.R
import com.checkrate.app.util.RateUtils
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class TabItem(val title: String) {
    LIVE("Live Mode"),
    ALERTS("Alerts"),
    THRESHOLDS("Thresholds"),
    SETTINGS("Settings")
}

private fun displayName(code: String): String = when (code) {
    "XAU" -> "Gold"
    "XAG" -> "Silver"
    "XAU22" -> "Gold 22K"
    else -> code
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateScreen() {
    val vm: RateViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(TabItem.LIVE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                scrollBehavior = androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior(
                    rememberTopAppBarState()
                )
            )
        },
        bottomBar = {
            NavigationBar {
                TabItem.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { TabIcon(tab) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (state.apiKeyMissing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("API key missing", style = MaterialTheme.typography.titleMedium)
                            Text("Set EXCHANGE_RATE_API_KEY in local.properties or gradle.properties")
                        }
                    }
                } else if (state.errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Error", style = MaterialTheme.typography.titleMedium)
                            Text(state.errorMessage ?: "")
                        }
                    }
                }
            }

            when (selectedTab) {
                TabItem.LIVE -> liveTab(state, vm)
                TabItem.ALERTS -> alertsTab(state, vm)
                TabItem.THRESHOLDS -> thresholdsTab(state, vm)
                TabItem.SETTINGS -> settingsTab(state, vm)
            }
        }
    }
}

@Composable
private fun TabIcon(tab: TabItem) {
    when (tab) {
        TabItem.LIVE -> androidx.compose.material3.Icon(Icons.Filled.WifiTethering, contentDescription = null)
        TabItem.ALERTS -> androidx.compose.material3.Icon(Icons.Filled.Notifications, contentDescription = null)
        TabItem.THRESHOLDS -> androidx.compose.material3.Icon(Icons.Filled.ShowChart, contentDescription = null)
        TabItem.SETTINGS -> androidx.compose.material3.Icon(Icons.Filled.Settings, contentDescription = null)
    }
}

private fun LazyListScope.liveTab(state: RateUiState, vm: RateViewModel) {
    item {
        if (state.lastRates.fxRates.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("FX data not loaded", style = MaterialTheme.typography.titleMedium)
                    Text("Tap Send Live Update Now to fetch FX once.")
                }
            }
        }
    }

    item {
        RateHeader(
            baseCurrency = state.settings.baseCurrency,
            lastUpdated = state.lastRates.fxUpdatedEpochMillis
        )
    }

    item {
        RatesSection(
            title = "FX Rates",
            titleColor = MaterialTheme.colorScheme.primary,
            baseCurrency = state.settings.baseCurrency,
            rates = state.lastRates.fxRates,
            showMetalAsGram = false,
            containerColor = Color(0xFFF5F2FA)
        )
    }

    item {
        RatesSection(
            title = "Gold & Silver (Pure)",
            titleColor = MaterialTheme.colorScheme.secondary,
            baseCurrency = state.settings.baseCurrency,
            rates = state.lastRates.metalsRates,
            onRefresh = vm::refreshMetalsOnly,
            onlyCodes = setOf("XAU", "XAG"),
            showMetalAsGram = true,
            containerColor = Color(0xFFF3F2F7)
        )
    }

    item {
        RatesSection(
            title = "Gold 22K",
            titleColor = MaterialTheme.colorScheme.secondary,
            baseCurrency = state.settings.baseCurrency,
            rates = state.lastRates.metalsRates,
            onRefresh = vm::refreshMetalsOnly,
            onlyCodes = setOf("XAU22"),
            showMetalAsGram = true,
            containerColor = Color(0xFFF7F2E8)
        )
    }

    item {
        LiveModeSection(
            enabled = state.settings.liveModeEnabled,
            intervalMinutes = state.settings.liveModeIntervalMinutes,
            onToggle = vm::setLiveModeEnabled,
            onIntervalChange = vm::setLiveModeInterval,
            onSendNow = vm::sendLiveUpdateNow,
            sendingNow = state.isSendingLiveUpdate
        )
    }
}
private fun LazyListScope.alertsTab(state: RateUiState, vm: RateViewModel) {
    item {
        AlertsSection(
            alertsEnabled = state.settings.alertsEnabled,
            dailySummaryEnabled = state.settings.dailySummaryEnabled,
            onAlertsChanged = vm::setAlertsEnabled,
            onSummaryChanged = vm::setDailySummaryEnabled,
            onTestNotification = vm::sendTestNotification
        )
    }
}


private fun LazyListScope.thresholdsTab(state: RateUiState, vm: RateViewModel) {
    item {
        ThresholdSection(
            baseCurrency = state.settings.baseCurrency,
            tracked = state.settings.trackedCurrencies + state.settings.trackedMetals + setOf("XAU22"),
            thresholds = state.settings.thresholds,
            onUpdate = vm::setThreshold
        )
    }
}

private fun LazyListScope.settingsTab(state: RateUiState, vm: RateViewModel) {
    item {
        BaseCurrencySection(
            baseCurrency = state.settings.baseCurrency,
            onUpdate = vm::updateBaseCurrency
        )
    }

    item {
        ApiUsageSection(
            count = state.settings.monthlyRequestCount,
            limit = state.settings.monthlyRequestLimit,
            monthKey = state.settings.requestMonthKey
        )
    }

    item {
        TrackedCurrenciesSection(
            tracked = state.settings.trackedCurrencies,
            thresholds = state.settings.thresholds,
            onAdd = vm::addCurrency,
            onRemove = vm::removeCurrency,
            onThresholdUpdate = vm::setThreshold
        )
    }
}

@Composable
private fun RateHeader(baseCurrency: String, lastUpdated: Long, onRefresh: (() -> Unit)? = null,
                       refreshing: Boolean = false) {
    val text = if (lastUpdated > 0L) {
        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        "Updated ${formatter.format(Date(lastUpdated))}"
    } else {
        "No data yet"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Base Currency: $baseCurrency")
                Text(text, style = MaterialTheme.typography.bodySmall)
            }
            if (onRefresh != null) {
                Button(onClick = onRefresh, enabled = !refreshing) {
                    Text(if (refreshing) "Refreshing" else "Refresh")
                }
            }
        }
    }
}

@Composable
private fun ApiUsageSection(count: Int, limit: Int, monthKey: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("API usage")
            Spacer(Modifier.height(8.dp))
            Text("$count of $limit requests used ($monthKey)")
        }
    }
}

@Composable
private fun BaseCurrencySection(baseCurrency: String, onUpdate: (String) -> Unit) {
    var input by remember(baseCurrency) { mutableStateOf(baseCurrency) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Base currency")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = input,
                onValueChange = {
                    val upper = it.uppercase()
                    if (upper.length <= 3) {
                        input = upper
                        if (upper.length == 3) {
                            onUpdate(upper)
                        }
                    }
                },
                label = { Text("ISO code") },
                singleLine = true
            )
        }
    }
}

@Composable
private fun AlertsSection(
    alertsEnabled: Boolean,
    dailySummaryEnabled: Boolean,
    onAlertsChanged: (Boolean) -> Unit,
    onSummaryChanged: (Boolean) -> Unit,
    onTestNotification: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Threshold alerts")
                Switch(checked = alertsEnabled, onCheckedChange = onAlertsChanged)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daily summary")
                Switch(checked = dailySummaryEnabled, onCheckedChange = onSummaryChanged)
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onTestNotification) {
                Text("Test notification")
            }
        }
    }
}

@Composable
private fun LiveModeSection(
    enabled: Boolean,
    intervalMinutes: Int,
    onToggle: (Boolean) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onSendNow: () -> Unit,
    sendingNow: Boolean
) {
    var intervalText by remember(intervalMinutes) { mutableStateOf(intervalMinutes.toString()) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("FX and Gold/Silver Rate", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Foreground checks on a fixed interval", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enabled")
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = intervalText,
                onValueChange = { intervalText = it },
                label = { Text("Interval minutes (>= 15)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val value = intervalText.toIntOrNull()
                        if (value != null && value >= 15) {
                            onIntervalChange(value)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Update Interval", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                Button(
                    onClick = onSendNow,
                    enabled = !sendingNow,
                    modifier = Modifier.weight(1f)
                ) {
                    if (sendingNow) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(16.dp)
                                .width(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (sendingNow) "Sending..." else "Send Live Update Now", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp))
                }
            }
                
        }
    }
}

@Composable
private fun RatesSection(
    title: String,
    titleColor: Color,
    baseCurrency: String,
    rates: Map<String, Double>,
    onlyCodes: Set<String>? = null,
    showMetalAsGram: Boolean,
    containerColor: Color,
    onRefresh: (() -> Unit)? = null
) {
    val format = DecimalFormat("0.####")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = titleColor, style = MaterialTheme.typography.titleMedium)
                if (onRefresh != null) {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pair", fontWeight = FontWeight.SemiBold)
                Text("Rate", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            val display = if (onlyCodes == null) rates else rates.filterKeys { it in onlyCodes }
            if (display.isEmpty()) {
                Text("No rates available")
            } else {
                display.toSortedMap().forEach { (code, value) ->
                    val normalized = if (showMetalAsGram) value else value
                    val label = if (showMetalAsGram) "1 g ${displayName(code)}" else "1 ${displayName(code)}"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label)
                        Text("${format.format(normalized)} $baseCurrency")
                    }
                }
            }
        }
    }
}
@Composable
private fun ThresholdSection(
    baseCurrency: String,
    tracked: Set<String>,
    thresholds: Map<String, Double>,
    onUpdate: (String, Double) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Thresholds")
            Spacer(Modifier.height(8.dp))
            tracked.toSortedSet().forEach { code ->
                val current = thresholds[code] ?: 0.0
                var input by remember(code, current) { mutableStateOf(if (current == 0.0) "" else current.toString()) }
                val labelSuffix = if (RateUtils.isMetal(code)) " (per g)" else ""
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        it.toDoubleOrNull()?.let { value -> onUpdate(code, value) }
                    },
                    label = { Text("${displayName(code)} to $baseCurrency$labelSuffix") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TrackedCurrenciesSection(
    tracked: Set<String>,
    thresholds: Map<String, Double>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onThresholdUpdate: (String, Double) -> Unit
) {
    var newCurrency by remember { mutableStateOf("") }
    var newThreshold by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Tracked currencies")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newCurrency,
                    onValueChange = { newCurrency = it.uppercase() },
                    label = { Text("Add ISO code") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = newThreshold,
                    onValueChange = { newThreshold = it },
                    label = { Text("Threshold") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (newCurrency.length == 3) {
                        onAdd(newCurrency)
                        newThreshold.toDoubleOrNull()?.let { onThresholdUpdate(newCurrency, it) }
                        newCurrency = ""
                        newThreshold = ""
                    }
                }) {
                    Text("Add")
                }
            }
            Spacer(Modifier.height(8.dp))

            if (tracked.isEmpty()) {
                Text("No currencies tracked")
            } else {
                tracked.toSortedSet().forEach { code ->
                    val currentThreshold = thresholds[code] ?: 0.0
                    val thresholdText = if (currentThreshold == 0.0) "Off" else currentThreshold.toString()
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(code)
                            Text("Threshold: $thresholdText")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = true,
                                onCheckedChange = { checked ->
                                    if (!checked) onRemove(code)
                                }
                            )
                            Text("Remove")
                        }
                    }
                }
            }
        }
    }
}