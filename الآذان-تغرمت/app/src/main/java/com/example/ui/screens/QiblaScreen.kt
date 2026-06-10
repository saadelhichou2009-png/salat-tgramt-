package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdhanViewModel
import com.example.ui.theme.*
import com.example.utils.PrayerTimesCalculator
import java.util.Locale
import kotlin.math.abs

@Composable
fun QiblaScreen(viewModel: AdhanViewModel) {
    val context = LocalContext.current
    val latitude by viewModel.latitude.collectAsState()
    val longitude by viewModel.longitude.collectAsState()
    val deviceHeading by viewModel.deviceHeading.collectAsState()

    // Calculated angle to Mecca from true North
    val qiblaAngle = remember(latitude, longitude) {
        PrayerTimesCalculator.calculateQiblaDirection(latitude, longitude)
    }

    // Toggle simulation slider to play around in AI Studio Emulator
    var useSimulationSlider by remember { mutableStateOf(false) }
    var simulatedHeading by remember { mutableStateOf(0f) }

    // Combine actual sensor with simulated if enabled
    val finalHeading = if (useSimulationSlider) simulatedHeading else deviceHeading

    // Angle of needle relative to top of the screen
    // Needle Angle = QiblaAngle - finalHeading
    val needleAngle = remember(qiblaAngle, finalHeading) {
        val diff = (qiblaAngle - finalHeading + 360) % 360
        diff.toFloat()
    }

    // Aligned condition (needle close to 0)
    val isAligned = abs(needleAngle) < 5f || abs(needleAngle - 360f) < 5f

    // Trigger phone vibration on alignment
    LaunchedEffect(isAligned) {
        if (isAligned) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }
            }
        }
    }

    // Pulsing scale animation for alignment success
    val alignScale by animateFloatAsState(
        targetValue = if (isAligned) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("qibla_screen")
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkStarryBg, Color(0xFF030712))
                )
            )
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Header Information Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "بوصلة اتجاه القبلة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGold
                )
                Text(
                    text = "تم حساب اتجاه الكعبة المشرفة بناءً على موقعك الجغرافي الحالي.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "زاوية القبلة", style = MaterialTheme.typography.labelSmall, color = MutedText)
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f°", qiblaAngle),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "اتجاه الهاتف", style = MaterialTheme.typography.labelSmall, color = MutedText)
                        Text(
                            text = String.format(Locale.getDefault(), "%d°", finalHeading.toInt()),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                    }
                }
            }
        }

        // 2. High-Fidelity Compass Visual Layer
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(alignScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (isAligned) {
                            listOf(PrimaryGold.copy(alpha = 0.2f), Color.Transparent)
                        } else {
                            listOf(DarkSurfaceCard, Color.Transparent)
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Draw compass dial ring and indicators
            Canvas(modifier = Modifier.size(240.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2

                // Circular golden dial border
                drawCircle(
                    color = if (isAligned) PrimaryGold else MutedText.copy(alpha = 0.3f),
                    radius = radius,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Sub-outer indicator ring
                drawCircle(
                    color = PrimaryGold.copy(alpha = 0.1f),
                    radius = radius - 8.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Cardinal Points (N, S, E, W) rotated along with phone rotation
                // North represents 0, East represents 90, etc.
                val rotAngle = -finalHeading
                rotate(rotAngle, center) {
                    // Line ticks across dial for degrees
                    for (i in 0 until 360 step 30) {
                        val tickLen = if (i % 90 == 0) 12.dp.toPx() else 6.dp.toPx()
                        val tickStroke = if (i % 90 == 0) 2.dp.toPx() else 1.dp.toPx()
                        val tickColor = if (i == 0) AccentRed else PrimaryGold.copy(alpha = 0.6f)

                        rotate(i.toFloat(), center) {
                            drawLine(
                                color = tickColor,
                                start = Offset(center.x, 8.dp.toPx()),
                                end = Offset(center.x, 8.dp.toPx() + tickLen),
                                strokeWidth = tickStroke
                            )
                        }
                    }
                }
            }

            // Glowing Indicator Dial (Points to Mecca)
            // It rotates by (Qibla - Phone heading) to lock on Mecca
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .rotate(needleAngle),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    
                    // Main compass arrow pointing Up (towards Mecca)
                    val arrowPath = Path().apply {
                        moveTo(center.x, 24.dp.toPx()) // Tip
                        lineTo(center.x - 14.dp.toPx(), center.y + 12.dp.toPx()) // Bottom left
                        lineTo(center.x, center.y - 10.dp.toPx()) // Inner center notch
                        lineTo(center.x + 14.dp.toPx(), center.y + 12.dp.toPx()) // Bottom right
                        close()
                    }

                    // Shadow/south point of arrow
                    val southPath = Path().apply {
                        moveTo(center.x, size.height - 24.dp.toPx())
                        lineTo(center.x - 10.dp.toPx(), center.y + 24.dp.toPx())
                        lineTo(center.x, center.y + 14.dp.toPx())
                        lineTo(center.x + 10.dp.toPx(), center.y + 24.dp.toPx())
                        close()
                    }

                    // Color of needles
                    val needleColorOuter = if (isAligned) EmeraldGreen else PrimaryGold
                    val needleColorInner = if (isAligned) Color(0xFF34D399) else AccentGold

                    // Draw Mecca indicator arrow (north)
                    drawPath(
                        path = arrowPath,
                        color = needleColorOuter
                    )
                    
                    // Draw opposite subtle grey arrow (south)
                    drawPath(
                        path = southPath,
                        color = MutedText.copy(alpha = 0.4f)
                    )

                    // Kaaba Representation circle at the tip of the arrow
                    drawCircle(
                        color = if (isAligned) EmeraldGreen else PrimaryGold,
                        radius = 8.dp.toPx(),
                        center = Offset(center.x, 24.dp.toPx())
                    )

                    // Central brass cap pivot
                    drawCircle(
                        color = AccentGold,
                        radius = 12.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF78350F), // Deep brass hollow core
                        radius = 4.dp.toPx(),
                        center = center
                    )
                }
            }

            // Miniature Kaaba Icon illustration in center if aligned
            Text(
                text = "🕋",
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 60.dp)
            )
        }

        // 3. Aligned Status Banner
        AnimatedVisibility(
            visible = isAligned,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, EmeraldGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "✓ تم توجيه الهاتف بنجاح نحو اتجاه الكعبة المشرفة",
                    color = Color(0xFF34D399),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp).fillMaxWidth()
                )
            }
        }

        // 4. Interactive Simulation Layer for Virtual Emulator (AI Studio Sidebar Check)
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, MutedText.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp)),
            colors = ListItemDefaults.colors(containerColor = DarkSurfaceCard),
            headlineContent = {
                Text(
                    text = "محاكاة التدوير والبوصلة",
                    color = LightText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            },
            supportingContent = {
                Text(
                    text = "استخدم شريط المحاكاة لتجربة تدوير الهاتف في المتصفح.",
                    color = MutedText,
                    fontSize = 12.sp
                )
            },
            trailingContent = {
                Switch(
                    checked = useSimulationSlider,
                    onCheckedChange = { useSimulationSlider = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryGold,
                        checkedTrackColor = PrimaryGold.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("simulator_switch")
                )
            }
        )

        if (useSimulationSlider) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "محاكاة الزاوية: ${simulatedHeading.toInt()}°",
                            style = MaterialTheme.typography.bodySmall,
                            color = LightText
                        )
                        IconButton(
                            onClick = { simulatedHeading = qiblaAngle.toFloat() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "محاذاة تلقائية",
                                tint = PrimaryGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Slider(
                        value = simulatedHeading,
                        onValueChange = { simulatedHeading = it },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryGold,
                            activeTrackColor = AccentGold,
                            inactiveTrackColor = MutedText.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("simulator_slider")
                    )
                    Text(
                        text = "اسحب شريط الانزلاق حتى تصبح زاوية البوصلة 0° لتتم محاذاة وجهة الكعبة المشرفة بنجاح وبدء الاهتزاز.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Custom extension scalar
private fun Modifier.scale(f: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = f, scaleY = f)
)
