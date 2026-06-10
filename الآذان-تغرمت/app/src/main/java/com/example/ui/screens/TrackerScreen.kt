package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdhanViewModel
import com.example.data.PrayerRecord
import com.example.ui.theme.*

@Composable
fun TrackerScreen(viewModel: AdhanViewModel) {
    val todayRecord by viewModel.todayPrayerRecord.collectAsState()

    // Status map
    val prayerStatusMap = listOf(
        PrayerStateConfig("Fajr", "صلاة الفجر", todayRecord.fajrStatus, "🌅"),
        PrayerStateConfig("Dhuhr", "صلاة الظهر", todayRecord.dhuhrStatus, "🕋"),
        PrayerStateConfig("Asr", "صلاة العصر", todayRecord.asrStatus, "🌇"),
        PrayerStateConfig("Maghrib", "صلاة المغرب", todayRecord.maghribStatus, "🌃"),
        PrayerStateConfig("Isha", "صلاة العشاء", todayRecord.ishaStatus, "🌌")
    )

    // Calculations for the card statistics
    val totalTracked = prayerStatusMap.count { it.currentStatus > 0 }
    val totalPrayed = prayerStatusMap.count { it.currentStatus in 1..3 }
    val prayedInJamaah = prayerStatusMap.count { it.currentStatus == 2 }
    val prayedOnTime = prayerStatusMap.count { it.currentStatus == 1 }

    val progressFraction = if (prayerStatusMap.isNotEmpty()) {
        totalPrayed.toFloat() / prayerStatusMap.size
    } else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tracker_screen")
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkStarryBg, Color(0xFF030712))
                )
            ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Progress Stats Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, PrimaryGold.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1.3f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "متابعة صلوات اليوم",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGold
                        )
                        Text(
                            text = "سجل التزامك بصلواتك الخمس لتبني عادة المحافظة عليها في أوقاتها وفي جماعة المسجد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Small row stats
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column {
                                Text("في الجماعة", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                Text("$prayedInJamaah صلوات", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                            }
                            Column {
                                Text("في الوقت", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                Text("$prayedOnTime صلوات", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = AccentGold)
                            }
                        }
                    }

                    // Circular completion progress ring
                    Box(
                        modifier = Modifier
                            .weight(0.7f)
                            .aspectRatio(1f)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier.fillMaxSize(),
                            color = PrimaryGold,
                            strokeWidth = 6.dp,
                            trackColor = MutedText.copy(alpha = 0.1f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(progressFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = LightText,
                                modifier = Modifier.testTag("tracker_progress_text")
                            )
                            Text(
                                text = "$totalPrayed / 5",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedText
                            )
                        }
                    }
                }
            }
        }

        // 2. Section Subtitle
        item {
            Text(
                text = "قائمة الصلوات للتسجيل",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryGold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // 3. Dynamic expandable/clickable list of prayers
        items(prayerStatusMap.size) { index ->
            val prayerConfig = prayerStatusMap[index]
            var choiceExpanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { choiceExpanded = !choiceExpanded }
                    .border(
                        1.dp,
                        if (prayerConfig.currentStatus > 0) PrimaryGold.copy(alpha = 0.2f) else Color.Transparent,
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (prayerConfig.currentStatus > 0) DarkSurfaceCard else DarkSurfaceCard.copy(alpha = 0.6f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Row with prayer name & current active state indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = prayerConfig.emoji, fontSize = 20.sp)
                            Text(
                                text = prayerConfig.arabicLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LightText
                            )
                        }

                        // Rightside indicator of selected status
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(getStatusColor(prayerConfig.currentStatus).copy(alpha = 0.15f))
                                .border(0.5.dp, getStatusColor(prayerConfig.currentStatus), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getStatusLabel(prayerConfig.currentStatus),
                                color = getStatusColor(prayerConfig.currentStatus),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("prayer_status_${prayerConfig.id}")
                            )
                        }
                    }

                    // Expansion pill selectors
                    AnimatedVisibility(
                        visible = choiceExpanded || prayerConfig.currentStatus == 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Divider(color = MutedText.copy(alpha = 0.1f))
                            
                            Text(
                                text = "اختر حالة أداء الصلاة اليوم:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedText,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )

                            // Quick flow grid of states
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 1. Jama'ah status
                                SelectionPill(
                                    label = "في جماعة",
                                    icon = "🕌",
                                    selected = prayerConfig.currentStatus == 2,
                                    activeColor = EmeraldGreen,
                                    onClick = {
                                        viewModel.updatePrayerTracking(prayerConfig.id, 2)
                                        choiceExpanded = false
                                    },
                                    modifier = Modifier.weight(1f).testTag("pill_${prayerConfig.id}_jamaah")
                                )
                                // 2. OnTime status
                                SelectionPill(
                                    label = "في وقتها",
                                    icon = "⏱",
                                    selected = prayerConfig.currentStatus == 1,
                                    activeColor = PrimaryGold,
                                    onClick = {
                                        viewModel.updatePrayerTracking(prayerConfig.id, 1)
                                        choiceExpanded = false
                                    },
                                    modifier = Modifier.weight(1f).testTag("pill_${prayerConfig.id}_ontime")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 3. Late status
                                SelectionPill(
                                    label = "متأخرة",
                                    icon = "⌛",
                                    selected = prayerConfig.currentStatus == 3,
                                    activeColor = AccentGold,
                                    onClick = {
                                        viewModel.updatePrayerTracking(prayerConfig.id, 3)
                                        choiceExpanded = false
                                    },
                                    modifier = Modifier.weight(1f).testTag("pill_${prayerConfig.id}_late")
                                )
                                // 4. Missed status
                                SelectionPill(
                                    label = "فاتتني",
                                    icon = "❌",
                                    selected = prayerConfig.currentStatus == 4,
                                    activeColor = AccentRed,
                                    onClick = {
                                        viewModel.updatePrayerTracking(prayerConfig.id, 4)
                                        choiceExpanded = false
                                    },
                                    modifier = Modifier.weight(1f).testTag("pill_${prayerConfig.id}_missed")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectionPill(
    label: String,
    icon: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) activeColor.copy(alpha = 0.15f) else Color.Transparent
    val borderStrokeColor = if (selected) activeColor else MutedText.copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .border(1.dp, borderStrokeColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = icon, fontSize = 13.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) activeColor else LightText,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helpers for color codes
fun getStatusColor(statusCode: Int): Color {
    return when (statusCode) {
        1 -> PrimaryGold
        2 -> EmeraldGreen
        3 -> AccentGold
        4 -> AccentRed
        else -> MutedText
    }
}

fun getStatusLabel(statusCode: Int): String {
    return when (statusCode) {
        1 -> "في وقتها"
        2 -> "في المسجد جماعة"
        3 -> "متأخرة عن وقتها"
        4 -> "غير مؤداة / فاتت"
        else -> "لم تُسجل بعد"
    }
}

data class PrayerStateConfig(
    val id: String,
    val arabicLabel: String,
    val currentStatus: Int,
    val emoji: String
)
