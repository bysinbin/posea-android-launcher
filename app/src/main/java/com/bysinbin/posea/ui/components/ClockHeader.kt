package com.bysinbin.posea.ui.components

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClockHeader(
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    onLongClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(Date()) }

    // Saat ve dakikayı dakika başlarında senkronize güncelle
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            currentTime = Date(now)
            val msToNextMinute = 60_000L - (now % 60_000L)
            delay(msToNextMinute.coerceAtLeast(1000L))
        }
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }

    val formattedTime = timeFormatter.format(currentTime)
    val formattedDate = dateFormatter.format(currentTime)

    // Pil durumu
    var batteryInfo by remember { mutableStateOf<Pair<Int?, Boolean>>(Pair(null, false)) }

    LaunchedEffect(currentTime) {
        val bat = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else null
                Pair(pct, isCharging)
            } catch (_: Exception) {
                Pair(null, false)
            }
        }
        batteryInfo = bat
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.Center
    ) {
        // Saat (Tıklanınca Alarm/Saat uygulamasını açar, uzun basınca launcher menüsünü açar)
        Text(
            text = formattedTime,
            fontSize = 58.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            letterSpacing = (-1.5).sp,
            modifier = Modifier.combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { openClockApp(context) },
                onLongClick = onLongClick
            )
        )

        // Tarih (Tıklanınca Takvim uygulamasını açar, uzun basınca launcher menüsünü açar)
        Text(
            text = formattedDate,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = textColor.copy(alpha = 0.8f),
            modifier = Modifier
                .padding(top = 2.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { openCalendarApp(context) },
                    onLongClick = onLongClick
                )
        )

        // Akıllı Bakış (Smart Glance) Çipleri (Pil & Alarm)
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pil Çipi
            batteryInfo.first?.let { pct ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { openBatterySettings(context) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (batteryInfo.second) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                        contentDescription = "Pil",
                        tint = if (batteryInfo.second) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "%$pct",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

private fun openBatterySettings(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

private fun openClockApp(context: Context) {
    try {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback generic
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALENDAR)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { context.startActivity(intent) } catch (_: Exception) {}
    }
}

private fun openCalendarApp(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALENDAR)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
        val intent = Intent(Intent.ACTION_VIEW, builder.build()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { context.startActivity(intent) } catch (_: Exception) {}
    }
}
