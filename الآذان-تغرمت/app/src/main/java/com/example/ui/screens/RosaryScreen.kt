package com.example.ui.screens

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdhanViewModel
import com.example.data.DhikrCount
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun RosaryScreen(viewModel: AdhanViewModel) {
    val context = LocalContext.current
    val dhikrList by viewModel.dbDhikrList.collectAsState()
    val selectedDhikrId by viewModel.selectedDhikrId.collectAsState()

    val activeDhikr = remember(dhikrList, selectedDhikrId) {
        dhikrList.find { it.id == selectedDhikrId } ?: DhikrCount(selectedDhikrId, "سبحان الله", 0)
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var customDhikrText by remember { mutableStateOf("") }

    // Haptics and sound generators
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_SYSTEM, 60)
        } catch (e: Exception) {
            null
        }
    }

    // Interactive scale bounce animation when tapped
    var clickTriggerState by remember { mutableStateOf(false) }
    val clickScale by animateFloatAsState(
        targetValue = if (clickTriggerState) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        finishedListener = { clickTriggerState = false }
    )

    fun handleCounterIncrement() {
        // Increment in VM Database
        viewModel.incrementDhikrCount()
        clickTriggerState = true

        // Play feedback sound & vibration
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(55, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(55)
                }
            }
        } catch (e: Exception) {
            // Ignore feedback failures safely
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("rosary_screen")
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkStarryBg, Color(0xFF030712))
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Core Header Block
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "المسبحة الإلكترونية الكونية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGold
                )
                Text(
                    text = "انقر فوق قرص السبحة للتسبيح والاستغفار واستمع إلى نقرة التأكيد واهتزاز الهاتف للتركيز الكامل.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 2. Selectable Horizontal List of Dhikrs
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "اختر صيغة الذكر:",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryGold,
                    fontWeight = FontWeight.Bold
                )
                
                // Add Custom Dhikr link button
                Row(
                    modifier = Modifier.clickable { showAddDialog = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة ذكر",
                        tint = AccentGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "إضافة ذكر مخصص",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold
                    )
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(dhikrList) { dhikr ->
                    val isSelected = dhikr.id == selectedDhikrId
                    val borderGold = if (isSelected) BorderStroke(1.dp, PrimaryGold) else null
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) PrimaryGold.copy(alpha = 0.15f) else DarkSurfaceCard.copy(alpha = 0.5f))
                            .then(if (isSelected) Modifier.border(1.dp, PrimaryGold, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable { viewModel.setSelectedDhikr(dhikr.id) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dhikr.arabicText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) PrimaryGold else LightText,
                            modifier = Modifier.testTag("dhikr_pill_${dhikr.id}")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Luxurious Giant Touch Counter Dial (Rosary Dome Bead)
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(clickScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(DarkSurfaceCard, Color(0xFF090D1A))
                    )
                )
                .border(3.dp, PrimaryGold, CircleShape)
                .border(8.dp, DarkSurfaceCard, CircleShape)
                .clickable { handleCounterIncrement() }
                .testTag("rosary_bead_clicker"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Glow text of the phrase
                Text(
                    text = activeDhikr.arabicText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                // Giant numerical indicator
                Text(
                    text = String.format(Locale.getDefault(), "%d", activeDhikr.count),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = LightText,
                    letterSpacing = (-1).sp,
                    modifier = Modifier.testTag("rosary_count_text")
                )

                Text(
                    text = "انقر للتسبيح",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText
                )
            }
            
            // Touch trigger feedback halo outline
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .border(0.5.dp, PrimaryGold.copy(alpha = 0.2f), CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Reset Action Box
        Row(
            modifier = Modifier.fillMaxWidth(0.6f),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { viewModel.resetDhikrCount() },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceCard),
                border = BorderStroke(0.5.dp, AccentRed.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_dhikr_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "إعادة تصفير العداد",
                        tint = AccentRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "تصفير عداد الذكر الحالي",
                        color = AccentRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Custom dialog to add dhikr
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "إضافة ذكر مخصص",
                    color = PrimaryGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "اكتب الذكر أو الاستغفار وسوف يُحفظ محلياً في مسبحتك:",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightText,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = customDhikrText,
                        onValueChange = { customDhikrText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_dhikr_input"),
                        placeholder = {
                            Text(
                                "مثال: لا حول ولا قوة إلا بالله",
                                color = MutedText,
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkStarryBg,
                            unfocusedContainerColor = DarkStarryBg,
                            focusedTextColor = LightText,
                            unfocusedTextColor = LightText,
                            focusedIndicatorColor = PrimaryGold,
                            unfocusedIndicatorColor = MutedText.copy(alpha = 0.3f)
                        ),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customDhikrText.isNotBlank()) {
                            viewModel.addNewCustomDhikr(customDhikrText)
                            customDhikrText = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                    modifier = Modifier.testTag("dialog_add_dhikr_confirm")
                ) {
                    Text("إضافة الذكر", color = DarkStarryBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MutedText)
                ) {
                    Text("إلغاء")
                }
            },
            containerColor = DarkSurfaceCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
