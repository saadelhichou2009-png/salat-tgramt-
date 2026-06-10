package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.AdhanViewModel
import com.example.ui.theme.*
import com.example.utils.CalculationMethod
import com.example.utils.JuristicMethod
import java.util.*

@Composable
fun SettingsScreen(viewModel: AdhanViewModel) {
    val context = LocalContext.current
    val latitude by viewModel.latitude.collectAsState()
    val longitude by viewModel.longitude.collectAsState()
    val calcMethod by viewModel.calcMethod.collectAsState()
    val juristicMethod by viewModel.juristicMethod.collectAsState()
    val locationName by viewModel.locationName.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.requestLocationUpdates()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen")
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkStarryBg, Color(0xFF030712))
                )
            )
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Settings Title Header
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
                    text = "الضبط الإداري والحساب الجغرافي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGold
                )
                Text(
                    text = "اضبط هيئة ووقت الحساب الشرعي لمواقيت الصلاة، أو حدث إحداثيات موقعك الجغرافي لتناسب مدينتك الحالية.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 2. Selectable Presets
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "اختر مدينة سريعة:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGold
                )

                // Grid of fast buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PresetLocationButton(
                            name = "تغرامت (المغرب)",
                            isSelected = locationName == "تغرامت (المغرب)",
                            onClick = { viewModel.selectPresetCity("تغرامت (المغرب)", 35.7971, -5.4053) },
                            modifier = Modifier.weight(1f).testTag("preset_taghramt_morocco")
                        )
                        PresetLocationButton(
                            name = "تغرمت (بجاية)",
                            isSelected = locationName == "تغرمت (بجاية)",
                            onClick = { viewModel.selectPresetCity("تغرمت (بجاية)", 36.75, 5.08) },
                            modifier = Modifier.weight(1f).testTag("preset_taghramt_algeria")
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PresetLocationButton(
                            name = "الدار البيضاء",
                            isSelected = locationName == "الدار البيضاء",
                            onClick = { viewModel.selectPresetCity("الدار البيضاء", 33.57, -7.58) },
                            modifier = Modifier.weight(1f)
                        )
                        PresetLocationButton(
                            name = "الجزائر العاصمة",
                            isSelected = locationName == "الجزائر العاصمة",
                            onClick = { viewModel.selectPresetCity("الجزائر العاصمة", 36.75, 3.06) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PresetLocationButton(
                            name = "مكة المكرمة",
                            isSelected = locationName == "مكة المكرمة",
                            onClick = { viewModel.selectPresetCity("مكة المكرمة", 21.42, 39.82) },
                            modifier = Modifier.weight(1f)
                        )
                        PresetLocationButton(
                            name = "القاهرة",
                            isSelected = locationName == "القاهرة",
                            onClick = { viewModel.selectPresetCity("القاهرة", 30.04, 31.23) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // GPS sync button
                Button(
                    onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            viewModel.requestLocationUpdates()
                        } else {
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                    modifier = Modifier.fillMaxWidth().testTag("gps_sync_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = DarkStarryBg)
                        Text("استيراد الموقع التلقائي من الـ (GPS)", color = DarkStarryBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // 3. Coordinate Manual Adjusters
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "تعديل الإحداثيات الجغرافية يدوياً:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGold
                )

                // Latitude Slider
                Column {
                    Text(
                        text = String.format(Locale.US, "خط العرض (Latitude): %.4f°", latitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = LightText
                    )
                    Slider(
                        value = latitude.toFloat(),
                        onValueChange = { viewModel.updateCoordinatesManual(it.toDouble(), longitude) },
                        valueRange = -90f..90f,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryGold,
                            activeTrackColor = AccentGold,
                            inactiveTrackColor = MutedText.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("lat_slider")
                    )
                }

                // Longitude Slider
                Column {
                    Text(
                        text = String.format(Locale.US, "خط الطول (Longitude): %.4f°", longitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = LightText
                    )
                    Slider(
                        value = longitude.toFloat(),
                        onValueChange = { viewModel.updateCoordinatesManual(latitude, it.toDouble()) },
                        valueRange = -180f..180f,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryGold,
                            activeTrackColor = AccentGold,
                            inactiveTrackColor = MutedText.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("lon_slider")
                    )
                }
            }
        }

        // 4. Calculation Formulas Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "طريقة وجداول الحساب الشرعي:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGold
                )

                // Tabular Method selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("طريقة الاحتساب والزاوية:", fontSize = 12.sp, color = MutedText)
                    CalculationMethod.values().forEach { method ->
                        val isSelected = method == calcMethod
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PrimaryGold.copy(alpha = 0.1f) else Color.Transparent)
                                .border(
                                    0.5.dp,
                                    if (isSelected) PrimaryGold else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.updateCalculationMethod(method) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = method.displayName, color = if (isSelected) PrimaryGold else LightText, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.updateCalculationMethod(method) },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryGold, unselectedColor = MutedText)
                            )
                        }
                    }
                }

                Divider(color = MutedText.copy(alpha = 0.1f))

                // Asr school
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("المذهب الفقهي لحساب صلاة العصر:", fontSize = 12.sp, color = MutedText)
                    JuristicMethod.values().forEach { juristic ->
                        val isSelected = juristic == juristicMethod
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PrimaryGold.copy(alpha = 0.1f) else Color.Transparent)
                                .border(
                                    0.5.dp,
                                    if (isSelected) PrimaryGold else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.updateJuristicMethod(juristic) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = juristic.name, color = if (isSelected) PrimaryGold else LightText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(text = juristic.displayName, color = MutedText, fontSize = 11.sp)
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.updateJuristicMethod(juristic) },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryGold, unselectedColor = MutedText)
                            )
                        }
                    }
                }
            }
        }

        // 5. Developer & Creator Info Card (Saad El Heicho)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("developer_info_card"),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "💻 مطور التطبيق",
                    style = MaterialTheme.typography.titleSmall,
                    color = PrimaryGold,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "سعد الهيشو",
                    style = MaterialTheme.typography.titleLarge,
                    color = LightText,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "تم تطوير هذا العمل المبارك بفضل الله وتوفيقه للإسهام في تيسير متابعة العبادات اليومية ومواقيت الصلاة والقبلة الكريمة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "الآذان تغرمت v1.0.0 © ٢٠٢٦",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PresetLocationButton(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) PrimaryGold.copy(alpha = 0.15f) else Color.Transparent
    val strokeColor = if (isSelected) PrimaryGold else MutedText.copy(alpha = 0.2f)
    
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, strokeColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) PrimaryGold else LightText,
            textAlign = TextAlign.Center
        )
    }
}
