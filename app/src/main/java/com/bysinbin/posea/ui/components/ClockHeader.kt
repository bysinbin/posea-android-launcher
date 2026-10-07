package com.bysinbin.posea.ui.components

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClockHeader(
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    clockStyle: com.bysinbin.posea.model.ClockStyle = com.bysinbin.posea.model.ClockStyle.DIGITAL,
    onLongClick: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null
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

    val swipeModifier = if (onSwipeDown != null) {
        Modifier.pointerInput(onSwipeDown) {
            var totalDrag = 0f
            detectVerticalDragGestures(
                onDragStart = { totalDrag = 0f },
                onDragEnd = { totalDrag = 0f },
                onDragCancel = { totalDrag = 0f },
                onVerticalDrag = { _, dragAmount: Float ->
                    totalDrag += dragAmount
                    if (totalDrag > 80f) {
                        onSwipeDown()
                        totalDrag = 0f
                    }
                }
            )
        }
    } else Modifier

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(swipeModifier)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.Center
    ) {
        if (clockStyle == com.bysinbin.posea.model.ClockStyle.BCD) {
            // BCD (Binary-Coded Decimal) İkili Matris Saati
            BcdClockMatrix(
                formattedTime = formattedTime,
                textColor = textColor,
                modifier = Modifier.combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { openClockApp(context) },
                    onLongClick = onLongClick
                )
            )
        } else {
            // Klasik Büyük Dijital Saat (Tıklanınca Alarm/Saat uygulamasını açar, uzun basınca launcher menüsünü açar)
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
        }

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

@Composable
private fun BcdClockMatrix(
    formattedTime: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val h1 = formattedTime.getOrNull(0)?.digitToIntOrNull() ?: 0
    val h2 = formattedTime.getOrNull(1)?.digitToIntOrNull() ?: 0
    val m1 = formattedTime.getOrNull(3)?.digitToIntOrNull() ?: 0
    val m2 = formattedTime.getOrNull(4)?.digitToIntOrNull() ?: 0

    val weights = listOf(8, 4, 2, 1)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // Üst Başlık (BCD İkili Saat Etiketi)
        Row(
            modifier = Modifier.padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BCD BINARY CLOCK",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // İkili Matris Izgarası
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sol Ağırlık İndeksi (8, 4, 2, 1)
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (w in weights) {
                    Box(
                        modifier = Modifier.size(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = w.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Saat Onlar Basamağı (H1)
            BcdDigitColumn(digit = h1, weights = weights)

            // Saat Birler Basamağı (H2)
            BcdDigitColumn(digit = h2, weights = weights)

            // Ayırıcı İki Nokta (:)
            Column(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                )
            }

            // Dakika Onlar Basamağı (M1)
            BcdDigitColumn(digit = m1, weights = weights)

            // Dakika Birler Basamağı (M2)
            BcdDigitColumn(digit = m2, weights = weights)
        }

        // Alt Desimal Değerler (1  4  :  3  5)
        Row(
            modifier = Modifier.padding(top = 10.dp, start = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$h1",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.75f),
                modifier = Modifier.width(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "$h2",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.75f),
                modifier = Modifier.width(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = ":",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.5f),
                modifier = Modifier.width(14.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "$m1",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.75f),
                modifier = Modifier.width(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "$m2",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.75f),
                modifier = Modifier.width(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun BcdDigitColumn(
    digit: Int,
    weights: List<Int>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (weight in weights) {
            val isLit = (digit and weight) != 0
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        if (isLit) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    )
            )
        }
    }
}
