package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdhanViewModel
import com.example.ui.theme.*
import com.example.utils.PrayerTimes
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * 2026 Luxury Dynamic Themes for Adhan app.
 * Automatically alters text readability, accentuation & shapes based on active sky.
 */
enum class PrayerBgState(
    val bgColors: List<Color>,
    val isLight: Boolean,
    val primaryText: Color,
    val accentColor: Color,
    val mutedText: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val tagBg: Color
) {
    FAJR(
        bgColors = listOf(Color(0xFF0F172A), Color(0xFF2E1065), Color(0xFF3B0764)), // Deep slate to royal lilac
        isLight = false,
        primaryText = Color(0xFFF8FAFC),
        accentColor = Color(0xFFD8B4FE), // Dreamy lavender
        mutedText = Color(0xFFC084FC),
        cardBg = Color(0x261E1B4B), // Translucent deep violet
        cardBorder = Color(0x33A78BFA),
        tagBg = Color(0x33A78BFA)
    ),
    DHUHR(
        bgColors = listOf(Color(0xFFFCFBF6), Color(0xFFF5EFE0), Color(0xFFECE3D1)), // Elegant warm ivory-beige day
        isLight = true,
        primaryText = Color(0xFF2C1D11), // High-contrast charcoal brown
        accentColor = Color(0xFFC2410C),  // Solar deep amber
        mutedText = Color(0xFF7C2D12),
        cardBg = Color(0x99FFFDFB), // Translucent clear light glass
        cardBorder = Color(0x40BE123C),
        tagBg = Color(0x1FBE123C)
    ),
    MAGHRIB(
        bgColors = listOf(Color(0xFF180A0A), Color(0xFF450A0A), Color(0xFF7A2006)), // Crimson solar eclipse / sunset
        isLight = false,
        primaryText = Color(0xFFFEF2F2),
        accentColor = Color(0xFFFB923C), // Sunset electric orange
        mutedText = Color(0xFFFCA5A5),
        cardBg = Color(0x26450A0A),
        cardBorder = Color(0x33FB923C),
        tagBg = Color(0x33FB923C)
    ),
    ISHA(
        bgColors = listOf(Color(0xFF030712), Color(0xFF070B19), Color(0xFF0F172A)), // Ultimate deep cosmic sky
        isLight = false,
        primaryText = Color(0xFFF8FAFC),
        accentColor = Color(0xFFF59E0B),  // Celestial star gold
        mutedText = Color(0xFF94A3B8),
        cardBg = Color(0x261E293B),
        cardBorder = Color(0x33F59E0B),
        tagBg = Color(0x1AF59E0B)
    )
}

@Composable
fun PrayerTimesScreen(viewModel: AdhanViewModel) {
    val prayerTimes by viewModel.prayerTimes.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    val latitude by viewModel.latitude.collectAsState()
    val longitude by viewModel.longitude.collectAsState()
    val locationName by viewModel.locationName.collectAsState()

    // 1. Determine active background state based on current active prayer
    val prayersList = getPrayersWithTimes(prayerTimes, currentTime)
    val activePrayer = remember(prayersList) { prayersList.find { it.isActive } }
    val bgState = remember(activePrayer) {
        when (activePrayer?.id) {
            "fajr", "sunrise" -> PrayerBgState.FAJR
            "dhuhr", "asr" -> PrayerBgState.DHUHR
            "maghrib" -> PrayerBgState.MAGHRIB
            else -> PrayerBgState.ISHA
        }
    }

    // 2. Animate color shifts smoothly when theme/sky state changes
    val color1 by animateColorAsState(targetValue = bgState.bgColors[0], animationSpec = tween(1200), label = "c1")
    val color2 by animateColorAsState(targetValue = bgState.bgColors[1], animationSpec = tween(1200), label = "c2")
    val color3 by animateColorAsState(targetValue = bgState.bgColors[2], animationSpec = tween(1200), label = "c3")

    val primaryTextColor by animateColorAsState(targetValue = bgState.primaryText, animationSpec = tween(1000), label = "pt")
    val mutedTextColor by animateColorAsState(targetValue = bgState.mutedText, animationSpec = tween(1000), label = "mt")
    val accentColor by animateColorAsState(targetValue = bgState.accentColor, animationSpec = tween(1000), label = "ac")
    val cardBgColor by animateColorAsState(targetValue = bgState.cardBg, animationSpec = tween(1000), label = "cb")
    val cardBorderColor by animateColorAsState(targetValue = bgState.cardBorder, animationSpec = tween(1000), label = "crdB")
    val tagBgColor by animateColorAsState(targetValue = bgState.tagBg, animationSpec = tween(1000), label = "tagB")

    // 3. Setup infinite slow background animations
    val infiniteTransition = rememberInfiniteTransition(label = "BackgroundParticles")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(80000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BgRotation"
    )
    val starPulse by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StarPulse"
    )

    // Calculate countdown parameters
    val (nextPrayerName, remainingTimeStr) = remember(currentTime, prayerTimes) {
        calculateRemainingTime(currentTime, prayerTimes)
    }
    val progress = remember(currentTime, prayerTimes) {
        calculatePrayerProgress(currentTime, prayerTimes)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("prayer_times_screen")
    ) {
        // --- 1. Dynamic Animated/Interactive Atmospheric Background Canvas ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw smooth background gradient
            drawRect(
                brush = Brush.verticalGradient(colors = listOf(color1, color2, color3))
            )

            // Render custom micro-interactions corresponding to currently selected times
            when (bgState) {
                PrayerBgState.FAJR -> {
                    // Fajr: Sunbeams rising slowly
                    val cx = size.width / 2f
                    val cy = size.height * 1.1f
                    val beamCount = 8
                    val path = Path()
                    val length = size.height * 0.9f
                    for (i in 0 until beamCount) {
                        val angleRad = Math.toRadians((22.5 * i + (rotationAngle * 0.05f)).toDouble())
                        path.reset()
                        path.moveTo(cx, cy)
                        val x1 = cx + length * cos(angleRad - 0.04f).toFloat()
                        val y1 = cy - length * sin(angleRad - 0.04f).toFloat()
                        val x2 = cx + length * cos(angleRad + 0.04f).toFloat()
                        val y2 = cy - length * sin(angleRad + 0.04f).toFloat()
                        path.lineTo(x1, y1)
                        path.lineTo(x2, y2)
                        path.close()

                        drawPath(
                            path = path,
                            color = accentColor.copy(alpha = 0.06f)
                        )
                    }
                }
                PrayerBgState.DHUHR -> {
                    // Dhuhr: Giant warm core sun halo reflecting midday brilliance
                    val cx = size.width / 2f
                    val cy = size.height * 0.25f
                    val maxRadius = size.width * 0.7f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentColor.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = maxRadius * (1f + sin(rotationAngle * 0.02f) * 0.08f)
                        ),
                        radius = maxRadius * (1f + sin(rotationAngle * 0.02f) * 0.08f),
                        center = Offset(cx, cy)
                    )
                }
                PrayerBgState.MAGHRIB -> {
                    // Maghrib: Golden-orange horizon shimmer waves
                    val yLevel = size.height * 0.65f
                    val count = 4
                    for (i in 0 until count) {
                        val offset = yLevel + (i * 35.dp.toPx()) + sin((rotationAngle * 0.04f) + i).toFloat() * 12f
                        drawLine(
                            color = accentColor.copy(alpha = 0.05f),
                            start = Offset(0f, offset),
                            end = Offset(size.width, offset),
                            strokeWidth = 8.dp.toPx()
                        )
                    }
                }
                PrayerBgState.ISHA -> {
                    // Isha: Quiet deep sky constellations (gentle twinkling dots)
                    val starPositions = listOf(
                        Offset(0.12f, 0.10f), Offset(0.32f, 0.07f), Offset(0.78f, 0.12f),
                        Offset(0.88f, 0.04f), Offset(0.52f, 0.19f), Offset(0.22f, 0.30f),
                        Offset(0.06f, 0.40f), Offset(0.64f, 0.44f), Offset(0.95f, 0.26f),
                        Offset(0.44f, 0.52f), Offset(0.20f, 0.62f), Offset(0.80f, 0.67f)
                    )
                    starPositions.forEach { pos ->
                        val px = pos.x * size.width
                        val py = pos.y * size.height
                        val pulse = (starPulse * 0.5f) + 0.5f * sin((rotationAngle * 0.08f) + (px * py)).toFloat()
                        drawCircle(
                            color = Color.White.copy(alpha = 0.35f * pulse.coerceIn(0f, 1f)),
                            radius = 1.8.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }

        // --- 2. Spacious Contents Content ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant header block with Location info
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "الموقع",
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = locationName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "عُرض: %.4f | طُول: %.4f", latitude, longitude),
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedTextColor.copy(alpha = 0.8f)
                        )
                    }

                    // Minimalist moon crescent vector
                    Text(
                        text = if (bgState.isLight) "☀️" else "🌙",
                        fontSize = 26.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // Spacious Gregorian / Hijri Date Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, cardBorderColor.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = viewModel.hijriDateStr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = viewModel.currentDateStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = mutedTextColor
                            )
                        }
                        Text(
                            text = "🕌 تغرامت",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // LUXURIOUS CENTRAL COUNTDOWN CIRCLE (Whitespace-friendly)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ClockCountdownCircular(
                        progress = progress,
                        nextPrayerName = nextPrayerName,
                        remainingTimeStr = remainingTimeStr,
                        accentColor = accentColor,
                        primaryTextColor = primaryTextColor,
                        mutedTextColor = mutedTextColor,
                        starColor = accentColor
                    )
                }
            }

            // 2026 Premium Adhan Audio Player Controller (Hassan II Mosque Voice / مسجد الحسن الثاني)
            item {
                val isPlayingAdhan by viewModel.isPlayingAdhan.collectAsState()
                val adhanErrorMessage by viewModel.adhanErrorMessage.collectAsState()
                val activeAdhanTitle by viewModel.activeAdhanTitle.collectAsState()
                val isAdhanAutoEnabled by viewModel.isAdhanAutoEnabled.collectAsState()

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("adhan_audio_controller_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, cardBorderColor.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "أذان مسجد الحسن الثاني مباشر",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                                Text(
                                    text = if (isPlayingAdhan) "جاري البث والتشغيل الآن..." else "بث حي وتنبيه تلقائي عند حدوث وقت الأذان",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = mutedTextColor
                                )
                            }
                            
                            // Switch to enable/disable automated adhan alarms
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = if (isAdhanAutoEnabled) "تلقائي" else "صامت",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAdhanAutoEnabled) accentColor else mutedTextColor
                                )
                                Switch(
                                    checked = isAdhanAutoEnabled,
                                    onCheckedChange = { viewModel.toggleAdhanAuto(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = accentColor,
                                        checkedTrackColor = accentColor.copy(alpha = 0.3f),
                                        uncheckedThumbColor = mutedTextColor,
                                        uncheckedTrackColor = mutedTextColor.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.testTag("adhan_auto_switch")
                                )
                            }
                        }

                        // Play/Stop Action block
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(primaryTextColor.copy(alpha = 0.05f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                                    .clickable {
                                        if (isPlayingAdhan) {
                                            viewModel.stopAdhan()
                                        } else {
                                            viewModel.playAdhan("مسجد الحسن الثاني")
                                        }
                                    }
                                    .testTag("play_adhan_test_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isPlayingAdhan) "⏸" else "▶",
                                    color = if (bgState.isLight) Color.White else Color.Black,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeAdhanTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryTextColor
                                )
                                Text(
                                    text = "الأذان بصوت المقرئ الشهير لمؤذني المغرب من الدار البيضاء",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = mutedTextColor
                                )
                            }
                        }

                        adhanErrorMessage?.let { err ->
                            Text(
                                text = "⚠️ $err",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp).testTag("adhan_error_message")
                            )
                        }
                    }
                }
            }

            // Expandable/Scrollable Prayers Header Label
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مواقيت صلاة اليوم",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = "اسحب للأعلى",
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedTextColor.copy(alpha = 0.6f)
                    )
                }
            }

            // Interactive Glassmorphic list layout
            items(prayersList) { prayer ->
                PrayerItemRow(
                    prayer = prayer,
                    primaryTextColor = primaryTextColor,
                    mutedTextColor = mutedTextColor,
                    accentColor = accentColor,
                    cardBgColor = cardBgColor,
                    cardBorderColor = cardBorderColor,
                    tagBgColor = tagBgColor
                )
            }
        }
    }
}

@Composable
fun ClockCountdownCircular(
    progress: Float,
    nextPrayerName: String,
    remainingTimeStr: String,
    accentColor: Color,
    primaryTextColor: Color,
    mutedTextColor: Color,
    starColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(230.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        val pulseTransition = rememberInfiniteTransition(label = "StarBreathing")
        val breathAlpha by pulseTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = EaseInOutCirc),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathingAlpha"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2.0f, height / 2.0f)
            val radius = (width - 12.dp.toPx()) / 2.0f

            // 1. Draw elegant 8-pointed star (Islamic Geometric Art)
            val starPath = Path()
            val points = 8
            val outerRadius = radius * 0.94f
            val innerRadius = radius * 0.70f
            for (i in 0 until (points * 2)) {
                val r = if (i % 2 == 0) outerRadius else innerRadius
                val angle = i * Math.PI / points
                val x = center.x + r * cos(angle).toFloat()
                val y = center.y + r * sin(angle).toFloat()
                if (i == 0) {
                    starPath.moveTo(x, y)
                } else {
                    starPath.lineTo(x, y)
                }
            }
            starPath.close()

            drawPath(
                path = starPath,
                color = starColor.copy(alpha = 0.08f * breathAlpha),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // 2. Draw circular shadow tracks
            drawCircle(
                color = primaryTextColor.copy(alpha = 0.04f),
                radius = radius,
                style = Stroke(width = 2.dp.toPx())
            )

            // 3. Draw active countdown swept progress line
            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(
                    width = 4.5.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                ),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )

            // 4. Draw cursor node at progress lip
            val tipAngle = Math.toRadians(((-90f + 360f * progress)).toDouble())
            val tx = center.x + radius * cos(tipAngle).toFloat()
            val ty = center.y + radius * sin(tipAngle).toFloat()
            drawCircle(
                color = accentColor,
                radius = 5.5.dp.toPx(),
                center = Offset(tx, ty)
            )
            drawCircle(
                color = primaryTextColor,
                radius = 2.0.dp.toPx(),
                center = Offset(tx, ty)
            )
        }

        // Inside layout text metrics
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "المتبقي لصلاة",
                style = MaterialTheme.typography.labelSmall,
                color = mutedTextColor,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = nextPrayerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = remainingTimeStr,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 24.sp,
                    letterSpacing = 1.5.sp
                ),
                fontWeight = FontWeight.Black,
                color = primaryTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("remaining_time_text")
            )
        }
    }
}

@Composable
fun PrayerItemRow(
    prayer: PrayerItem,
    primaryTextColor: Color,
    mutedTextColor: Color,
    accentColor: Color,
    cardBgColor: Color,
    cardBorderColor: Color,
    tagBgColor: Color
) {
    // Elegant glow/border for the active row
    val borderStroke = if (prayer.isActive) {
        BorderStroke(1.5.dp, accentColor)
    } else {
        BorderStroke(1.dp, cardBorderColor.copy(alpha = 0.2f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(18.dp),
        border = borderStroke,
        colors = CardDefaults.cardColors(
            containerColor = if (prayer.isActive) cardBgColor.copy(alpha = 0.85f) else cardBgColor.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Right Section: Icon and Localized Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Round glass badge representing the prayer icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (prayer.isActive) accentColor.copy(alpha = 0.2f) else tagBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = prayer.emoji,
                        fontSize = 18.sp
                    )
                }

                Column {
                    Text(
                        text = prayer.arabicName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (prayer.isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (prayer.isActive) accentColor else primaryTextColor
                    )
                    Text(
                        text = prayer.englishName,
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedTextColor
                    )
                }
            }

            // Left Section: Formatted Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = convertTo12HourFormat(prayer.time),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (prayer.isActive) accentColor else primaryTextColor,
                    modifier = Modifier.testTag("prayer_time_${prayer.id}")
                )

                if (prayer.isActive) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "الآن",
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// Data structures and helper models
data class PrayerItem(
    val id: String,
    val arabicName: String,
    val englishName: String,
    val time: String,
    val emoji: String,
    val epochMs: Long,
    val isActive: Boolean
)

private fun getPrayersWithTimes(times: PrayerTimes, currentTime: Long): List<PrayerItem> {
    val list = listOf(
        PrayerItem("fajr", "الفجر", "Fajr", times.fajr, "🌅", times.fajrTimeMs, false),
        PrayerItem("sunrise", "الشروق", "Sunrise", times.sunrise, "☀️", times.sunriseTimeMs, false),
        PrayerItem("dhuhr", "الظهر", "Dhuhr", times.dhuhr, "🕋", times.dhuhrTimeMs, false),
        PrayerItem("asr", "العصر", "Asr", times.asr, "🌇", times.asrTimeMs, false),
        PrayerItem("maghrib", "المغرب", "Maghrib", times.maghrib, "🌃", times.maghribTimeMs, false),
        PrayerItem("isha", "العشاء", "Isha", times.isha, "🌌", times.ishaTimeMs, false)
    )

    var activeIndex = -1
    for (i in list.indices) {
        if (list[i].epochMs <= currentTime) {
            activeIndex = i
        }
    }

    if (activeIndex == -1) {
        activeIndex = 5 // Set last prayer as active (Isha)
    }

    return list.mapIndexed { idx, item ->
        item.copy(isActive = idx == activeIndex)
    }
}

private fun calculateRemainingTime(currentTime: Long, times: PrayerTimes): Pair<String, String> {
    val list = listOf(
        "الفجر" to times.fajrTimeMs,
        "الشروق" to times.sunriseTimeMs,
        "الظهر" to times.dhuhrTimeMs,
        "العصر" to times.asrTimeMs,
        "المغرب" to times.maghribTimeMs,
        "العشاء" to times.ishaTimeMs
    )

    val next = list.find { it.second > currentTime }

    return if (next != null) {
        val diff = next.second - currentTime
        val hours = diff / 3600000
        val minutes = (diff % 3600000) / 60000
        val seconds = (diff % 60000) / 1000
        val timeStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        next.first to timeStr
    } else {
        val diff = (times.fajrTimeMs + 86400000) - currentTime
        val hours = diff / 3600000
        val minutes = (diff % 3600000) / 60000
        val seconds = (diff % 60000) / 1000
        val timeStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        "الفجر (غداً)" to timeStr
    }
}

private fun calculatePrayerProgress(currentTime: Long, times: PrayerTimes): Float {
    val list = listOf(
        times.fajrTimeMs,
        times.sunriseTimeMs,
        times.dhuhrTimeMs,
        times.asrTimeMs,
        times.maghribTimeMs,
        times.ishaTimeMs
    )

    var prevTime = 0L
    var nextTime = 0L

    for (time in list) {
        if (time <= currentTime) {
            prevTime = time
        }
    }

    val next = list.find { it > currentTime }
    if (next != null) {
        nextTime = next
    } else {
        prevTime = list.last()
        nextTime = list.first() + 86400000L
    }

    if (prevTime == 0L) {
        prevTime = list.last() - 86400000L
        nextTime = list.first()
    }

    val totalDiff = nextTime - prevTime
    if (totalDiff <= 0L) return 0f

    val elapsed = currentTime - prevTime
    return (elapsed.toFloat() / totalDiff).coerceIn(0f, 1f)
}

fun convertTo12HourFormat(time24: String): String {
    return try {
        val sdf24 = SimpleDateFormat("HH:mm", Locale.US)
        val dateObj = sdf24.parse(time24)
        val sdf12 = SimpleDateFormat("h:mm a", Locale("ar"))
        sdf12.format(dateObj ?: Date())
    } catch (e: Exception) {
        time24
    }
}
