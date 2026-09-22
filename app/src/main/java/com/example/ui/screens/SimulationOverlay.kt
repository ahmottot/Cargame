package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VehiclePresetEntity
import com.example.model.DrivetrainType
import com.example.model.VehicleConfig
import com.example.physics.TrackType
import com.example.physics.VehiclePhysics
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SimulationOverlay(
  physics: VehiclePhysics,
  throttle: Float,
  onThrottleChange: (Float) -> Unit,
  brake: Float,
  onBrakeChange: (Float) -> Unit,
  handbrake: Boolean,
  onHandbrakeChange: (Boolean) -> Unit,
  isReverse: Boolean,
  onReverseChange: (Boolean) -> Unit,
  isSlowMo: Boolean,
  onSlowMoToggle: () -> Unit,
  onRepair: () -> Unit,
  onRecoverInPlace: () -> Unit,
  onReset: () -> Unit,
  onTrackSelect: (TrackType) -> Unit,
  onOpenGarage: () -> Unit,
  presetEntities: List<VehiclePresetEntity> = emptyList(),
  onSelectPreset: (VehicleConfig) -> Unit = {},
  modifier: Modifier = Modifier
) {
  var showTrackDialog by remember { mutableStateOf(false) }
  var showPresetDialog by remember { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize().padding(12.dp)) {
    // 1. TOP HEADER: Track Badge, Drivetrain Switcher, Controls
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Track Selector Pill
      Surface(
        onClick = { showTrackDialog = true },
        shape = RoundedCornerShape(24.dp),
        color = CarbonSurface.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, CarbonBorder),
        modifier = Modifier.testTag("track_selector_button")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = RaceOrange,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              text = physics.track.type.title,
              style = MaterialTheme.typography.labelLarge,
              color = TextPrimary,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Parkuru Değiştir",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary
            )
          }
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      // Quick Vehicle Preset Pill (Fast Preset Switcher)
      Surface(
        onClick = { showPresetDialog = true },
        shape = RoundedCornerShape(24.dp),
        color = CarbonSurface.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, CarbonBorder),
        modifier = Modifier.testTag("quick_preset_button")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = null,
            tint = TurboBlue,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              text = physics.config.name,
              style = MaterialTheme.typography.labelLarge,
              color = TextPrimary,
              fontWeight = FontWeight.Bold,
              maxLines = 1
            )
            Text(
              text = "Taslak Değiştir",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary
            )
          }
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      // Live Drivetrain Mode Switcher: FWD / RWD / AWD
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = CarbonDark.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, CarbonBorder)
      ) {
        Row(
          modifier = Modifier.padding(3.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          DrivetrainType.values().forEach { drive ->
            val isSelected = physics.liveDrivetrain == drive
            val bg = if (isSelected) RaceOrange else Color.Transparent
            val textCol = if (isSelected) Color.White else TextSecondary

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .clickable { physics.liveDrivetrain = drive }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("drivetrain_switch_${drive.shortName.lowercase()}"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = drive.shortName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = textCol
              )
            }
          }
        }
      }

      // Action Buttons (SlowMo, Repair, Recover in place, Reset, Garage)
      Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Slow-Motion Toggle
        IconButton(
          onClick = onSlowMoToggle,
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isSlowMo) WarningAmber.copy(alpha = 0.3f) else CarbonSurface.copy(alpha = 0.85f))
            .border(1.dp, if (isSlowMo) WarningAmber else CarbonBorder, CircleShape)
            .testTag("slow_mo_button")
        ) {
          Icon(
            imageVector = Icons.Default.SlowMotionVideo,
            contentDescription = "Ağır Çekim",
            tint = if (isSlowMo) WarningAmber else TextPrimary,
            modifier = Modifier.size(18.dp)
          )
        }

        // YENİLE / TAMİR ET (Quick Repair - Instantly fix damage and restore engine)
        FilledTonalButton(
          onClick = onRepair,
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = NeonGreen.copy(alpha = 0.22f),
            contentColor = NeonGreen
          ),
          border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.7f)),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier
            .height(36.dp)
            .testTag("quick_repair_button")
        ) {
          Icon(Icons.Default.BuildCircle, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("Yenile", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        // YERİNDE DOĞ (Recover In Place / Flip Upright at current spot - BeamNG feature)
        FilledTonalButton(
          onClick = onRecoverInPlace,
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = TurboBlue.copy(alpha = 0.22f),
            contentColor = TurboBlue
          ),
          border = BorderStroke(1.dp, TurboBlue.copy(alpha = 0.7f)),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier
            .height(36.dp)
            .testTag("recover_in_place_button")
        ) {
          Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("Yerinde Doğ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        // Reset to Track Start (Pist Başı)
        IconButton(
          onClick = onReset,
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(CarbonSurface.copy(alpha = 0.85f))
            .border(1.dp, CarbonBorder, CircleShape)
            .testTag("reset_car_button")
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Pist Başı",
            tint = TextPrimary,
            modifier = Modifier.size(18.dp)
          )
        }

        // Garage / Customizer Button
        FilledTonalButton(
          onClick = onOpenGarage,
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = RaceOrange,
            contentColor = Color.White
          ),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier
            .height(36.dp)
            .testTag("open_garage_button")
        ) {
          Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("Garaj", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }

    // 2. TELEMETRY GAUGES (Top Left / Right Floating Card)
    Column(
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(top = 62.dp)
        .widthIn(max = 240.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(CarbonDark.copy(alpha = 0.88f))
        .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
        .padding(10.dp)
    ) {
      // Speed and Gear
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        Row(verticalAlignment = Alignment.Bottom) {
          Text(
            text = "${physics.speedKmh.roundToInt()}",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = Color.White
          )
          Text(
            text = " km/h",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 5.dp)
          )
        }

        // Gear Indicator
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = CarbonCard,
          border = BorderStroke(1.dp, CarbonBorder)
        ) {
          Text(
            text = if (isReverse) "R" else "${physics.gear}",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isReverse) DangerRed else RaceOrange,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // RPM Bar
      val maxRpm = physics.config.engine.maxRpm.toFloat()
      val rpmProgress = (physics.rpm / maxRpm).coerceIn(0f, 1f)
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("DEVİR (RPM)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
          Text("${physics.rpm.roundToInt()} rpm", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
          progress = { rpmProgress },
          modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = if (rpmProgress > 0.88f) DangerRed else if (rpmProgress > 0.7f) WarningAmber else TurboBlue,
          trackColor = CarbonCard
        )
      }

      Spacer(modifier = Modifier.height(5.dp))

      // Turbo Boost
      if (physics.config.boostBar > 0.1f) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("TURBO BASINCI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Text(
              text = String.format("%.2f bar", physics.currentBoostBar),
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              color = TurboBlue,
              fontWeight = FontWeight.Bold
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          val boostProgress = (physics.currentBoostBar / physics.config.boostBar.coerceAtLeast(0.1f)).coerceIn(0f, 1f)
          LinearProgressIndicator(
            progress = { boostProgress },
            modifier = Modifier
              .fillMaxWidth()
              .height(4.dp)
              .clip(RoundedCornerShape(2.dp)),
            color = TurboBlue,
            trackColor = CarbonCard
          )
        }
        Spacer(modifier = Modifier.height(5.dp))
      }

      // Suspension Travel Telemetry
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Front
        Column(modifier = Modifier.weight(1f)) {
          Text("ÖN SÜSP.", fontSize = 9.sp, color = TextMuted)
          LinearProgressIndicator(
            progress = { physics.frontCompression.coerceIn(0f, 1f) },
            modifier = Modifier
              .fillMaxWidth()
              .height(4.dp)
              .clip(RoundedCornerShape(2.dp)),
            color = NeonGreen,
            trackColor = CarbonCard
          )
        }
        // Rear
        Column(modifier = Modifier.weight(1f)) {
          Text("ARKA SÜSP.", fontSize = 9.sp, color = TextMuted)
          LinearProgressIndicator(
            progress = { physics.rearCompression.coerceIn(0f, 1f) },
            modifier = Modifier
              .fillMaxWidth()
              .height(4.dp)
              .clip(RoundedCornerShape(2.dp)),
            color = NeonGreen,
            trackColor = CarbonCard
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Damage / Deform Indicator
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("HASAR / DEFORMASYON", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (physics.damagePercent > 30f) DangerRed else TextMuted)
          Text("${physics.damagePercent.roundToInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (physics.damagePercent > 30f) DangerRed else TextSecondary)
        }
        LinearProgressIndicator(
          progress = { (physics.damagePercent / 100f).coerceIn(0f, 1f) },
          modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp)),
          color = DangerRed,
          trackColor = CarbonCard
        )
      }

      // Track Specific Timing
      when (physics.track.type) {
        TrackType.DRAG_STRIP -> {
          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = CarbonCard,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(6.dp)) {
              val z100 = physics.zeroToHundredTimeSec?.let { String.format("%.2f sn", it) } ?: "Ölçülüyor..."
              val qMile = physics.quarterMileTimeSec?.let { String.format("%.2f sn", it) } ?: "Bekleniyor..."
              Text("0-100 km/s: $z100", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
              Text("400m Çeyrek Mil: $qMile", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            }
          }
        }
        TrackType.CLIFF_DROP -> {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Maksimum G-Kuvveti: ${String.format("%.1f G", physics.peakGForce)}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = WarningAmber
          )
        }
        TrackType.HILL_CLIMB -> {
          Spacer(modifier = Modifier.height(6.dp))
          val angleDeg = Math.toDegrees(abs(physics.angleRad.toDouble())).toFloat()
          Text(
            text = "Mevcut Eğim: ${String.format("%.1f°", angleDeg)}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = RaceOrangeBright
          )
        }
        TrackType.SUSPENSION_TEST -> {}
      }

      // Water Hazard & Engine Hydrolock Telemetry
      if (physics.isInWater) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "🌊 SU İÇİNDE (Derinlik: ${String.format("%.1f m", physics.waterSubmersionDepth)})",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = TurboBlue
        )
      }
      if (physics.isEngineHydrolocked) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "⚠️ MOTOR SU ALDI (BOĞULDU!)",
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
          color = DangerRed
        )
      }
      if (physics.isWindshieldCracked) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "💥 ÖN CAM KIRIK (AĞIR DARBE)",
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          color = WarningAmber
        )
      }
    }

    // 2.5 HYDROLOCK RESCUE BANNER (When engine drowns in water)
    if (physics.isEngineHydrolocked) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = DangerRed.copy(alpha = 0.94f),
        border = BorderStroke(2.dp, Color.White),
        modifier = Modifier
          .align(Alignment.Center)
          .padding(horizontal = 20.dp)
          .testTag("engine_hydrolocked_dialog")
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "MOTOR SU ALDI & BOĞULDU!",
              fontWeight = FontWeight.Black,
              fontSize = 15.sp,
              color = Color.White
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Hava emişine derin su girdiğinden motor durdu. 'Yenile' veya 'Yerinde Doğ' ile aracı kurtarabilirsiniz.",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.95f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
              onClick = onRepair,
              colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DangerRed),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Suyu Boşalt & Yenile", fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
            OutlinedButton(
              onClick = onRecoverInPlace,
              border = BorderStroke(1.5.dp, Color.White),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Yerinde Doğ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
        }
      }
    }

    // 3. BOTTOM CONTROLS (Pedals and Driving Buttons)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .padding(bottom = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom
    ) {
      // LEFT SIDE CONTROLS: Handbrake & Reverse
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.Start
      ) {
        // Reverse Switch (İleri / Geri)
        Surface(
          onClick = { onReverseChange(!isReverse) },
          shape = RoundedCornerShape(14.dp),
          color = if (isReverse) DangerRed else CarbonSurface.copy(alpha = 0.9f),
          border = BorderStroke(1.dp, if (isReverse) DangerRed else CarbonBorder),
          modifier = Modifier.testTag("reverse_gear_toggle")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (isReverse) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
              contentDescription = null,
              tint = if (isReverse) Color.White else TextPrimary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isReverse) "GERİ (R)" else "İLERİ (D)",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = if (isReverse) Color.White else TextPrimary
            )
          }
        }

        // Handbrake Button (El Freni)
        Box(
          modifier = Modifier
            .size(width = 110.dp, height = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (handbrake) DangerRed.copy(alpha = 0.85f) else CarbonSurface.copy(alpha = 0.9f))
            .border(1.5.dp, if (handbrake) DangerRed else CarbonBorder, RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
              detectTapGestures(
                onPress = {
                  onHandbrakeChange(true)
                  tryAwaitRelease()
                  onHandbrakeChange(false)
                }
              )
            }
            .testTag("handbrake_button"),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.PanTool,
              contentDescription = null,
              tint = if (handbrake) Color.White else WarningAmber,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "EL FRENİ",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 11.sp,
              color = if (handbrake) Color.White else TextPrimary
            )
          }
        }
      }

      // RIGHT SIDE CONTROLS: Brake & Throttle Pedals
      Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Bottom
      ) {
        // FOOT BRAKE PEDAL
        Box(
          modifier = Modifier
            .size(width = 80.dp, height = 80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (brake > 0f) DangerRed.copy(alpha = 0.75f) else CarbonSurface.copy(alpha = 0.9f))
            .border(2.dp, if (brake > 0f) DangerRed else CarbonBorder, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
              detectTapGestures(
                onPress = {
                  onBrakeChange(1.0f)
                  tryAwaitRelease()
                  onBrakeChange(0f)
                }
              )
            }
            .testTag("brake_pedal"),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.StopCircle,
              contentDescription = null,
              tint = if (brake > 0f) Color.White else DangerRed,
              modifier = Modifier.size(28.dp)
            )
            Text(
              text = "FREN",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = if (brake > 0f) Color.White else TextPrimary
            )
          }
        }

        // ACCELERATOR (GAS) PEDAL
        Box(
          modifier = Modifier
            .size(width = 95.dp, height = 115.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
              Brush.verticalGradient(
                colors = if (throttle > 0f)
                  listOf(RaceOrange, RaceOrangeBright)
                else
                  listOf(CarbonCard, CarbonSurface)
              )
            )
            .border(2.dp, if (throttle > 0f) RaceOrangeBright else CarbonBorder, RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
              detectTapGestures(
                onPress = {
                  onThrottleChange(1.0f)
                  tryAwaitRelease()
                  onThrottleChange(0f)
                }
              )
            }
            .testTag("gas_pedal"),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.Speed,
              contentDescription = null,
              tint = if (throttle > 0f) Color.White else RaceOrange,
              modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "GAZ",
              fontWeight = FontWeight.Black,
              fontSize = 16.sp,
              color = if (throttle > 0f) Color.White else TextPrimary
            )
          }
        }
      }
    }

    // 4. Track Selection Dialog
    if (showTrackDialog) {
      AlertDialog(
        onDismissRequest = { showTrackDialog = false },
        title = {
          Text(
            text = "Test Haritası Seçin",
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TrackType.values().forEach { trackType ->
              val isCurrent = physics.track.type == trackType
              Surface(
                onClick = {
                  onTrackSelect(trackType)
                  showTrackDialog = false
                },
                shape = RoundedCornerShape(12.dp),
                color = if (isCurrent) RaceOrange.copy(alpha = 0.2f) else CarbonCard,
                border = BorderStroke(1.dp, if (isCurrent) RaceOrange else CarbonBorder),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = when (trackType) {
                      TrackType.HILL_CLIMB -> Icons.Default.Terrain
                      TrackType.CLIFF_DROP -> Icons.Default.Warning
                      TrackType.SUSPENSION_TEST -> Icons.Default.Tune
                      TrackType.DRAG_STRIP -> Icons.Default.Speed
                    },
                    contentDescription = null,
                    tint = if (isCurrent) RaceOrange else TurboBlue,
                    modifier = Modifier.size(24.dp)
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(
                      text = trackType.title,
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp,
                      color = TextPrimary
                    )
                    Text(
                      text = trackType.description,
                      fontSize = 11.sp,
                      color = TextSecondary
                    )
                  }
                }
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { showTrackDialog = false }) {
            Text("Kapat", color = RaceOrange)
          }
        },
        containerColor = CarbonSurface
      )
    }

    // 5. Quick Preset Selection Dialog (Fast Track Loading)
    if (showPresetDialog) {
      AlertDialog(
        onDismissRequest = { showPresetDialog = false },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = TurboBlue, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Hızlı Araç Taslağı Değiştir",
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          }
        },
        text = {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "Pistten çıkmadan kayıtlı motor, turbo ve süspansiyon ayarlarını anında yükleyin:",
              fontSize = 12.sp,
              color = TextSecondary
            )

            val displayList = if (presetEntities.isNotEmpty()) {
              presetEntities
            } else {
              emptyList()
            }

            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 350.dp)
            ) {
              items(displayList, key = { it.id }) { entity ->
                val cfg = entity.toVehicleConfig()
                val isCurrent = physics.config.name == entity.name

                Surface(
                  onClick = {
                    onSelectPreset(cfg)
                    showPresetDialog = false
                  },
                  shape = RoundedCornerShape(12.dp),
                  color = if (isCurrent) RaceOrange.copy(alpha = 0.2f) else CarbonCard,
                  border = BorderStroke(1.dp, if (isCurrent) RaceOrange else CarbonBorder),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                          text = entity.name,
                          fontWeight = FontWeight.Bold,
                          fontSize = 14.sp,
                          color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                          shape = RoundedCornerShape(4.dp),
                          color = if (entity.isUserCustom) NeonGreen.copy(alpha = 0.2f) else TurboBlue.copy(alpha = 0.2f)
                        ) {
                          Text(
                            text = if (entity.isUserCustom) "ÖZEL" else "FABRİKA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (entity.isUserCustom) NeonGreen else TurboBlue,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                          )
                        }
                      }

                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (cfg.drivetrain) {
                          DrivetrainType.RWD -> Color(0xFFD32F2F)
                          DrivetrainType.FWD -> Color(0xFF0288D1)
                          DrivetrainType.AWD -> Color(0xFF388E3C)
                          else -> Color(0xFF388E3C)
                        }
                      ) {
                        Text(
                          text = cfg.drivetrain.shortName,
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold,
                          color = Color.White,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = "⚙️ ${cfg.engine.label} • ${cfg.induction.label} (${String.format("%.1f", cfg.boostBar)} Bar) • ${cfg.calculatedHp} HP",
                      fontSize = 11.sp,
                      color = TurboBlue
                    )
                    Text(
                      text = "📐 Süspansiyon: ${cfg.suspension.rideHeightMm.toInt()}mm • ${(cfg.suspension.stiffness / 1000).toInt()}kN/m",
                      fontSize = 10.sp,
                      color = NeonGreen
                    )
                  }
                }
              }
            }
          }
        },
        confirmButton = {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                showPresetDialog = false
                onOpenGarage()
              },
              border = BorderStroke(1.dp, RaceOrange)
            ) {
              Icon(Icons.Default.Build, contentDescription = null, tint = RaceOrange, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Garajda Düzenle", color = RaceOrange, fontSize = 12.sp)
            }
            TextButton(onClick = { showPresetDialog = false }) {
              Text("Kapat", color = TextSecondary)
            }
          }
        },
        containerColor = CarbonSurface
      )
    }
  }
}
