package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VehiclePresetEntity
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
  currentConfig: VehicleConfig,
  onConfigChange: (VehicleConfig) -> Unit,
  presetEntities: List<VehiclePresetEntity>,
  onSavePreset: (name: String, config: VehicleConfig) -> Unit,
  onUpdatePreset: (VehicleConfig) -> Unit,
  onDeletePreset: (String) -> Unit,
  onStartTestDrive: () -> Unit,
  modifier: Modifier = Modifier
) {
  var activeTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Taslaklar", "Motor & Turbo", "Çekiş & Dif", "Süspansiyon", "Gövde & Lastik")

  var showSaveDialog by remember { mutableStateOf(false) }
  var newPresetName by remember { mutableStateOf("") }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Automation Tuning Lab",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Black,
              color = TextPrimary
            )
            Text(
              text = "BeamNG & DriveCSX Araç ve Motor Tasarım Fabrikası",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary
            )
          }
        },
        actions = {
          Button(
            onClick = onStartTestDrive,
            colors = ButtonDefaults.buttonColors(containerColor = RaceOrange),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            modifier = Modifier.padding(end = 8.dp).testTag("start_test_drive_button")
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Piste Çık!", fontWeight = FontWeight.Black)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = CarbonDark)
      )
    },
    containerColor = CarbonDark,
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // 1. LIVE DYNO SPEC SHEET (Automation Style Live Specs)
      DynoSpecCard(
        config = currentConfig,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 6.dp)
      )

      // 2. CATEGORY TABS
      ScrollableTabRow(
        selectedTabIndex = activeTab,
        containerColor = CarbonDark,
        contentColor = RaceOrange,
        edgePadding = 12.dp,
        divider = { HorizontalDivider(color = CarbonBorder) }
      ) {
        tabs.forEachIndexed { index, title ->
          Tab(
            selected = activeTab == index,
            onClick = { activeTab = index },
            text = {
              Text(
                text = title,
                fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal,
                color = if (activeTab == index) RaceOrange else TextSecondary,
                fontSize = 13.sp
              )
            }
          )
        }
      }

      // 3. TAB CONTENT
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(14.dp)
      ) {
        when (activeTab) {
          0 -> PresetsTab(
            currentConfig = currentConfig,
            presetEntities = presetEntities,
            onSelectPreset = onConfigChange,
            onSaveCurrent = {
              newPresetName = "${currentConfig.name} Özel"
              showSaveDialog = true
            },
            onUpdatePreset = onUpdatePreset,
            onDeletePreset = onDeletePreset
          )
          1 -> EngineTurboTab(
            config = currentConfig,
            onConfigChange = onConfigChange
          )
          2 -> DrivetrainTab(
            config = currentConfig,
            onConfigChange = onConfigChange
          )
          3 -> SuspensionTab(
            config = currentConfig,
            onConfigChange = onConfigChange
          )
          4 -> BodyTiresTab(
            config = currentConfig,
            onConfigChange = onConfigChange
          )
        }
      }
    }

    // Save Preset Dialog (Room SQLite Local Storage)
    if (showSaveDialog) {
      AlertDialog(
        onDismissRequest = { showSaveDialog = false },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Save, contentDescription = null, tint = RaceOrange, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Özel Araç Taslağını Kaydet", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
          }
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              "Tüm motor, turbo, diferansiyel ve süspansiyon ayarlarınız yerel veritabanına (Room) kalıcı olarak kaydedilir.",
              fontSize = 12.sp,
              color = TextSecondary
            )
            OutlinedTextField(
              value = newPresetName,
              onValueChange = { newPresetName = it },
              label = { Text("Taslak Adı") },
              placeholder = { Text("Örn: Dağ Canavarı V8, Drag Twin Turbo") },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = RaceOrange,
                unfocusedBorderColor = CarbonBorder
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("preset_name_input")
            )

            // Configuration Quick Summary Card
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = CarbonDark,
              border = BorderStroke(1.dp, CarbonBorder),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Text(
                  text = "KAYDEDİLECEK KONFİGÜRASYON ÖZETİ:",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = RaceOrange
                )
                Text(
                  text = "⚙️ Motor: ${currentConfig.engine.label} (${currentConfig.calculatedHp} HP / ${currentConfig.calculatedTorqueNm} Nm)",
                  fontSize = 11.sp,
                  color = TextPrimary
                )
                Text(
                  text = "🌪️ Aşırı Besleme: ${currentConfig.induction.label} (${currentConfig.boostBar} bar boost)",
                  fontSize = 11.sp,
                  color = TurboBlue
                )
                Text(
                  text = "📐 Süspansiyon: ${currentConfig.suspension.rideHeightMm.toInt()} mm | ${(currentConfig.suspension.stiffness / 1000).toInt()} kN/m | ${currentConfig.suspension.camberDeg}° Kamber",
                  fontSize = 11.sp,
                  color = NeonGreen
                )
                Text(
                  text = "🏁 Aktarma: ${currentConfig.drivetrain.label} • ${currentConfig.differential.label} • ${currentConfig.tire.label}",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
            }
          }
        },
        confirmButton = {
          Button(
            onClick = {
              if (newPresetName.isNotBlank()) {
                onSavePreset(newPresetName.trim(), currentConfig)
              }
              showSaveDialog = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = RaceOrange),
            modifier = Modifier.testTag("confirm_save_preset_button")
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Veritabanına Kaydet", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { showSaveDialog = false }) {
            Text("İptal", color = TextSecondary)
          }
        },
        containerColor = CarbonSurface
      )
    }
  }
}

@Composable
fun DynoSpecCard(config: VehicleConfig, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = CarbonSurface,
    border = BorderStroke(1.dp, CarbonBorder),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = config.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = Color.White
          )
          Text(
            text = "${config.chassis.label} • ${config.drivetrain.shortName} • ${config.engine.label}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
          )
        }
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = when (config.drivetrain) {
            DrivetrainType.RWD -> Color(0xFFD32F2F)
            DrivetrainType.FWD -> Color(0xFF0288D1)
            DrivetrainType.AWD -> Color(0xFF388E3C)
          }
        ) {
          Text(
            text = config.drivetrain.shortName,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 4 Key Stats Grid
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        SpecStat(label = "GÜÇ (HP)", value = "${config.calculatedHp} HP", color = RaceOrange)
        SpecStat(label = "TORK (NM)", value = "${config.calculatedTorqueNm} Nm", color = TurboBlue)
        SpecStat(label = "AĞIRLIK", value = "${config.calculatedWeightKg} kg", color = TextPrimary)
        SpecStat(label = "0-100 TAHMİNİ", value = String.format("%.1f sn", config.calculatedZeroToHundredSec), color = NeonGreen)
      }
    }
  }
}

@Composable
fun SpecStat(label: String, value: String, color: Color) {
  Column {
    Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
    Text(
      text = value,
      fontSize = 14.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
      color = color
    )
  }
}

// 1. PRESETS TAB (Yerel Veritabanı ve Hazır Taslaklar)
@Composable
fun PresetsTab(
  currentConfig: VehicleConfig,
  presetEntities: List<VehiclePresetEntity>,
  onSelectPreset: (VehicleConfig) -> Unit,
  onSaveCurrent: () -> Unit,
  onUpdatePreset: (VehicleConfig) -> Unit,
  onDeletePreset: (String) -> Unit
) {
  var filterIndex by remember { mutableIntStateOf(0) } // 0: Tümü, 1: Özel, 2: Fabrika
  var presetToDelete by remember { mutableStateOf<VehiclePresetEntity?>(null) }
  var presetToOverwrite by remember { mutableStateOf<VehiclePresetEntity?>(null) }

  val filteredEntities = remember(presetEntities, filterIndex) {
    when (filterIndex) {
      1 -> presetEntities.filter { it.isUserCustom }
      2 -> presetEntities.filter { !it.isUserCustom }
      else -> presetEntities
    }
  }

  val customCount = presetEntities.count { it.isUserCustom }
  val factoryCount = presetEntities.count { !it.isUserCustom }

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    // Header & Action Bar
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Araç & Motor Taslakları",
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 15.sp
          )
          Text(
            text = "Yerel hafızada kayıtlı motor, turbo ve süspansiyon ayarları",
            fontSize = 11.sp,
            color = TextSecondary
          )
        }

        Button(
          onClick = onSaveCurrent,
          colors = ButtonDefaults.buttonColors(containerColor = RaceOrange),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("save_current_preset_button")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Yeni Taslak Kaydet", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Filter Chips
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = filterIndex == 0,
          onClick = { filterIndex = 0 },
          label = { Text("Tümü (${presetEntities.size})", fontSize = 12.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = RaceOrange.copy(alpha = 0.2f),
            selectedLabelColor = RaceOrange
          )
        )
        FilterChip(
          selected = filterIndex == 1,
          onClick = { filterIndex = 1 },
          label = { Text("Özel Taslaklarım ($customCount)", fontSize = 12.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = NeonGreen.copy(alpha = 0.2f),
            selectedLabelColor = NeonGreen
          )
        )
        FilterChip(
          selected = filterIndex == 2,
          onClick = { filterIndex = 2 },
          label = { Text("Fabrika Şablonları ($factoryCount)", fontSize = 12.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TurboBlue.copy(alpha = 0.2f),
            selectedLabelColor = TurboBlue
          )
        )
      }
    }

    // Empty state for custom filter
    if (filterIndex == 1 && customCount == 0) {
      item {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = CarbonCard,
          border = BorderStroke(1.dp, CarbonBorder),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Henüz Özel Bir Taslak Kaydetmediniz",
              fontWeight = FontWeight.Bold,
              color = TextPrimary,
              fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Motor, turbo veya süspansiyon sekmesinden ayarlarınızı yapıp 'Yeni Taslak Kaydet' butonuna basarak yerel veritabanına kaydedebilirsiniz.",
              fontSize = 12.sp,
              color = TextSecondary,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      }
    }

    // Presets List
    items(filteredEntities, key = { it.id }) { entity ->
      val presetConfig = remember(entity) { entity.toVehicleConfig() }
      val isSelected = currentConfig.id == presetConfig.id || currentConfig.name == presetConfig.name

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) RaceOrange.copy(alpha = 0.12f) else CarbonCard,
        border = BorderStroke(1.5.dp, if (isSelected) RaceOrange else CarbonBorder),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("preset_${entity.id}")
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          // Card Top Row: Name, Badges & Actions
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Text(
                text = entity.name,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = TextPrimary
              )
              Spacer(modifier = Modifier.width(6.dp))

              // User Custom vs Factory Tag
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (entity.isUserCustom) NeonGreen.copy(alpha = 0.2f) else TurboBlue.copy(alpha = 0.2f)
              ) {
                Text(
                  text = if (entity.isUserCustom) "ÖZEL TASLAK" else "FABRİKA",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (entity.isUserCustom) NeonGreen else TurboBlue,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
              Spacer(modifier = Modifier.width(4.dp))

              // Drivetrain Tag
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = when (presetConfig.drivetrain) {
                  DrivetrainType.RWD -> Color(0xFFD32F2F)
                  DrivetrainType.FWD -> Color(0xFF0288D1)
                  DrivetrainType.AWD -> Color(0xFF388E3C)
                }
              ) {
                Text(
                  text = presetConfig.drivetrain.shortName,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Black,
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
              }
            }

            // Quick Actions (Delete if custom)
            if (entity.isUserCustom) {
              IconButton(
                onClick = { presetToDelete = entity },
                modifier = Modifier
                  .size(32.dp)
                  .testTag("delete_preset_${entity.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = "Taslağı Sil",
                  tint = DangerRed,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Component Breakdown Grid (Motor, Turbo, Süspansiyon, Güç)
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = CarbonDark.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "⚙️ ${presetConfig.engine.label}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                Text(
                  text = "🌪️ ${presetConfig.induction.label} (${String.format("%.1f", presetConfig.boostBar)} Bar)",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = TurboBlue
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "📐 Süspansiyon: ${presetConfig.suspension.rideHeightMm.toInt()}mm • ${(presetConfig.suspension.stiffness / 1000).toInt()}kN/m • ${presetConfig.suspension.camberDeg}°",
                  fontSize = 10.sp,
                  color = NeonGreen
                )
                Text(
                  text = "⚡ ${presetConfig.calculatedHp} HP / ${presetConfig.calculatedTorqueNm} Nm",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Black,
                  color = RaceOrange
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Card Bottom Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (isSelected) {
              Button(
                onClick = {},
                enabled = false,
                colors = ButtonDefaults.buttonColors(
                  disabledContainerColor = RaceOrange.copy(alpha = 0.25f),
                  disabledContentColor = RaceOrange
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Aktif Yüklü", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            } else {
              Button(
                onClick = { onSelectPreset(presetConfig) },
                colors = ButtonDefaults.buttonColors(containerColor = TurboBlue),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("load_preset_${entity.id}")
              ) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Taslağı Yükle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }

            // If it's a custom preset, allow overwriting it with current tweaks
            if (entity.isUserCustom) {
              OutlinedButton(
                onClick = { presetToOverwrite = entity },
                border = BorderStroke(1.dp, CarbonBorder),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
              ) {
                Icon(Icons.Default.Sync, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Üzerine Güncelle", color = TextSecondary, fontSize = 11.sp)
              }
            }
          }
        }
      }
    }
  }

  // Delete Confirmation Dialog
  presetToDelete?.let { entity ->
    AlertDialog(
      onDismissRequest = { presetToDelete = null },
      title = { Text("Taslağı Sil", color = DangerRed, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "'${entity.name}' adlı özel taslağınız yerel veritabanından kalıcı olarak silinecek. Emin misiniz?",
          color = TextPrimary
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeletePreset(entity.id)
            presetToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
        ) {
          Text("Sil", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { presetToDelete = null }) {
          Text("İptal", color = TextSecondary)
        }
      },
      containerColor = CarbonSurface
    )
  }

  // Overwrite Confirmation Dialog
  presetToOverwrite?.let { entity ->
    AlertDialog(
      onDismissRequest = { presetToOverwrite = null },
      title = { Text("Taslağı Güncelle", color = RaceOrange, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "Mevcut motor, turbo ve süspansiyon ayarlarınız '${entity.name}' taslağının üzerine kaydedilecek. Onaylıyor musunuz?",
          color = TextPrimary
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdatePreset(currentConfig.copy(id = entity.id, name = entity.name))
            presetToOverwrite = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = RaceOrange)
        ) {
          Text("Üzerine Kaydet", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { presetToOverwrite = null }) {
          Text("İptal", color = TextSecondary)
        }
      },
      containerColor = CarbonSurface
    )
  }
}

// 2. ENGINE & TURBO TAB (Motor, Turboşarj, Hava Filtresi, Egzoz)
@Composable
fun EngineTurboTab(
  config: VehicleConfig,
  onConfigChange: (VehicleConfig) -> Unit
) {
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    // Engine Type Picker
    item {
      SectionTitle("Motor Bloğu Seçimi")
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EngineType.values().forEach { eng ->
          val isSelected = config.engine == eng
          Surface(
            onClick = { onConfigChange(config.copy(engine = eng)) },
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) RaceOrange.copy(alpha = 0.2f) else CarbonCard,
            border = BorderStroke(1.dp, if (isSelected) RaceOrange else CarbonBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(eng.label, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                Text("Taban: ${eng.baseHp} HP | ${eng.baseTorqueNm} Nm | ${eng.weightKg.toInt()} kg", fontSize = 11.sp, color = TextSecondary)
              }
              if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = RaceOrange)
            }
          }
        }
      }
    }

    // Induction / Turbo Type
    item {
      SectionTitle("Aşırı Besleme (Aspirasyon & Turbo)")
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InductionType.values().forEach { ind ->
          val isSelected = config.induction == ind
          Surface(
            onClick = { onConfigChange(config.copy(induction = ind)) },
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) TurboBlue.copy(alpha = 0.2f) else CarbonCard,
            border = BorderStroke(1.dp, if (isSelected) TurboBlue else CarbonBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(ind.label, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
              if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = TurboBlue)
            }
          }
        }
      }
    }

    // Turbo Boost Pressure Slider (if turbo/supercharger)
    if (config.induction.allowsBoostTuning && config.engine != EngineType.ELECTRIC_DUAL) {
      item {
        SectionTitle("Turbo Basınç Ayarı (Boost Tuning)")
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = CarbonCard,
          border = BorderStroke(1.dp, CarbonBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Basınç Seviyesi", fontWeight = FontWeight.Bold, color = TextPrimary)
              Text(
                text = String.format("%.2f bar (%.1f psi)", config.boostBar, config.boostBar * 14.5038f),
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = TurboBlue
              )
            }
            Slider(
              value = config.boostBar,
              onValueChange = { onConfigChange(config.copy(boostBar = it)) },
              valueRange = 0.2f..2.8f,
              steps = 25,
              colors = SliderDefaults.colors(thumbColor = TurboBlue, activeTrackColor = TurboBlue)
            )
          }
        }
      }
    }

    // Air Filter (Hava Filtresi)
    item {
      SectionTitle("Hava Filtresi / Emiş Kiti (Intake)")
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AirFilterType.values().forEach { filter ->
          val isSelected = config.airFilter == filter
          Surface(
            onClick = { onConfigChange(config.copy(airFilter = filter)) },
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) NeonGreen.copy(alpha = 0.2f) else CarbonCard,
            border = BorderStroke(1.dp, if (isSelected) NeonGreen else CarbonBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(filter.label, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                Text(filter.soundBonus + if (filter.powerBonusPercent > 0) " (+${filter.powerBonusPercent}%)" else "", fontSize = 11.sp, color = TextSecondary)
              }
              if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = NeonGreen)
            }
          }
        }
      }
    }

    // Exhaust System (Egzoz)
    item {
      SectionTitle("Egzoz Sistemi")
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExhaustType.values().forEach { exh ->
          val isSelected = config.exhaust == exh
          Surface(
            onClick = { onConfigChange(config.copy(exhaust = exh)) },
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) RaceOrange.copy(alpha = 0.2f) else CarbonCard,
            border = BorderStroke(1.dp, if (isSelected) RaceOrange else CarbonBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(exh.label, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                Text(if (exh.powerBonusPercent > 0) "+${exh.powerBonusPercent}% Güç Kazanımı" else "Standart Sessiz", fontSize = 11.sp, color = TextSecondary)
              }
              if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = RaceOrange)
            }
          }
        }
      }
    }
  }
}

// 3. DRIVETRAIN TAB (RWD / FWD / AWD & Diferansiyel)
@Composable
fun DrivetrainTab(
  config: VehicleConfig,
  onConfigChange: (VehicleConfig) -> Unit
) {
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      SectionTitle("Çekiş Sistemi (Drivetrain)")
      Text(
        "Kullanıcının seçebileceği arkadan itiş, önden çekiş ve dört çeker çekiş tipleri:",
        fontSize = 12.sp,
        color = TextSecondary,
        modifier = Modifier.padding(bottom = 6.dp)
      )
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DrivetrainType.values().forEach { drive ->
          val isSelected = config.drivetrain == drive
          Surface(
            onClick = { onConfigChange(config.copy(drivetrain = drive)) },
            shape = RoundedCornerShape(14.dp),
            color = if (isSelected) RaceOrange.copy(alpha = 0.2f) else CarbonCard,
            border = BorderStroke(1.5.dp, if (isSelected) RaceOrange else CarbonBorder),
            modifier = Modifier.fillMaxWidth().testTag("select_drive_${drive.shortName.lowercase()}")
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = CircleShape,
                color = if (isSelected) RaceOrange else CarbonBorder,
                modifier = Modifier.size(36.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(drive.shortName, fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White)
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(drive.label, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                Text(drive.desc, fontSize = 11.sp, color = TextSecondary)
              }
              if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RaceOrange)
              }
            }
          }
        }
      }
    }

    item {
      SectionTitle("Diferansiyel Tipi")
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DifferentialType.values().forEach { diff ->
          val isSelected = config.differential == diff
          Surface(
            onClick = { onConfigChange(config.copy(differential = diff)) },
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) TurboBlue.copy(alpha = 0.2f) else CarbonCard,
            border = BorderStroke(1.dp, if (isSelected) TurboBlue else CarbonBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(diff.label, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
              if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = TurboBlue)
            }
          }
        }
      }
    }
  }
}

// 4. SUSPENSION TAB (BeamNG Style Süspansiyon)
@Composable
fun SuspensionTab(
  config: VehicleConfig,
  onConfigChange: (VehicleConfig) -> Unit
) {
  val sus = config.suspension

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    // Ride Height
    item {
      SectionTitle("Yerden Yükseklik (Ride Height)")
      Surface(shape = RoundedCornerShape(14.dp), color = CarbonCard, border = BorderStroke(1.dp, CarbonBorder)) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Şasi Yüksekliği", fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("${sus.rideHeightMm.toInt()} mm", fontWeight = FontWeight.Bold, color = NeonGreen)
          }
          Slider(
            value = sus.rideHeightMm,
            onValueChange = { onConfigChange(config.copy(suspension = sus.copy(rideHeightMm = it))) },
            valueRange = 80f..360f,
            colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen)
          )
        }
      }
    }

    // Spring Stiffness
    item {
      SectionTitle("Yay Sertliği (Spring Rate)")
      Surface(shape = RoundedCornerShape(14.dp), color = CarbonCard, border = BorderStroke(1.dp, CarbonBorder)) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Sertlik", fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("${(sus.stiffness / 1000).roundToInt()} kN/m", fontWeight = FontWeight.Bold, color = WarningAmber)
          }
          Slider(
            value = sus.stiffness,
            onValueChange = { onConfigChange(config.copy(suspension = sus.copy(stiffness = it))) },
            valueRange = 20000f..85000f,
            colors = SliderDefaults.colors(thumbColor = WarningAmber, activeTrackColor = WarningAmber)
          )
        }
      }
    }

    // Shock Damping
    item {
      SectionTitle("Amortisör Sönümleme (Damping)")
      Surface(shape = RoundedCornerShape(14.dp), color = CarbonCard, border = BorderStroke(1.dp, CarbonBorder)) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Darbe Emme Oranı", fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("${sus.damping.roundToInt()} Ns/m", fontWeight = FontWeight.Bold, color = TurboBlue)
          }
          Slider(
            value = sus.damping,
            onValueChange = { onConfigChange(config.copy(suspension = sus.copy(damping = it))) },
            valueRange = 1200f..6500f,
            colors = SliderDefaults.colors(thumbColor = TurboBlue, activeTrackColor = TurboBlue)
          )
        }
      }
    }

    // Wheel Camber
    item {
      SectionTitle("Kamber Açısı (Wheel Camber)")
      Surface(shape = RoundedCornerShape(14.dp), color = CarbonCard, border = BorderStroke(1.dp, CarbonBorder)) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Açı", fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(String.format("%.1f°", sus.camberDeg), fontWeight = FontWeight.Bold, color = RaceOrange)
          }
          Slider(
            value = sus.camberDeg,
            onValueChange = { onConfigChange(config.copy(suspension = sus.copy(camberDeg = it))) },
            valueRange = -4.5f..0.5f,
            colors = SliderDefaults.colors(thumbColor = RaceOrange, activeTrackColor = RaceOrange)
          )
        }
      }
    }
  }
}

// 5. BODY & TIRES TAB
@Composable
fun BodyTiresTab(
  config: VehicleConfig,
  onConfigChange: (VehicleConfig) -> Unit
) {
  val paintColors = listOf(
    Color(0xFFE53935), Color(0xFF00ACC1), Color(0xFF43A047),
    Color(0xFFFFB300), Color(0xFF8E24AA), Color(0xFFFFFFFF),
    Color(0xFF212121), Color(0xFFFF5722)
  )

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    // Chassis Body Style
    item {
      SectionTitle("Gövde / Şasi Tipi")
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChassisType.values().forEach { chassis ->
          val isSelected = config.chassis == chassis
          Surface(
            onClick = { onConfigChange(config.copy(chassis = chassis)) },
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) RaceOrange.copy(alpha = 0.2f) else CarbonCard,
            border = BorderStroke(1.dp, if (isSelected) RaceOrange else CarbonBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(chassis.label, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                Text("Taban Ağırlık: ${chassis.baseWeightKg.toInt()} kg | Hava Direnci: ${chassis.dragCoefficient}", fontSize = 11.sp, color = TextSecondary)
              }
              if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = RaceOrange)
            }
          }
        }
      }
    }

    // Paint Color Picker
    item {
      SectionTitle("Gövde Boyası")
      LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(paintColors) { color ->
          val isSelected = (config.customPaintColor ?: config.chassis.defaultColor) == color
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(color)
              .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color.White else CarbonBorder, CircleShape)
              .clickable { onConfigChange(config.copy(customPaintColor = color)) }
          )
        }
      }
    }

    // Tire Compound
    item {
      SectionTitle("Lastik Hamuru")
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TireType.values().forEach { tire ->
          val isSelected = config.tire == tire
          Surface(
            onClick = { onConfigChange(config.copy(tire = tire)) },
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) NeonGreen.copy(alpha = 0.2f) else CarbonCard,
            border = BorderStroke(1.dp, if (isSelected) NeonGreen else CarbonBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(tire.label, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                Text("Asfalt Tutunma: x${tire.gripFactor} | Arazi Performansı: x${tire.offroadFactor}", fontSize = 11.sp, color = TextSecondary)
              }
              if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = NeonGreen)
            }
          }
        }
      }
    }

    // Tire Width
    item {
      SectionTitle("Lastik Genişliği (Taban mm)")
      Surface(shape = RoundedCornerShape(14.dp), color = CarbonCard, border = BorderStroke(1.dp, CarbonBorder)) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Taban Genişliği", fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("${config.tireWidthMm} mm", fontWeight = FontWeight.Bold, color = TextPrimary)
          }
          Slider(
            value = config.tireWidthMm.toFloat(),
            onValueChange = { onConfigChange(config.copy(tireWidthMm = it.toInt())) },
            valueRange = 205f..345f,
            steps = 14,
            colors = SliderDefaults.colors(thumbColor = RaceOrange, activeTrackColor = RaceOrange)
          )
        }
      }
    }
  }
}

@Composable
fun SectionTitle(text: String) {
  Text(
    text = text,
    fontWeight = FontWeight.Bold,
    fontSize = 13.sp,
    color = TextSecondary,
    modifier = Modifier.padding(bottom = 6.dp)
  )
}
