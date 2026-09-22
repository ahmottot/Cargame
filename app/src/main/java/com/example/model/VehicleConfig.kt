package com.example.model

import androidx.compose.ui.graphics.Color

enum class DrivetrainType(val label: String, val shortName: String, val desc: String) {
  RWD("Arkadan İtiş (RWD)", "RWD", "Güç arka tekerleklere iletilir. Drift ve pist performansı için ideal."),
  FWD("Önden Çekiş (FWD)", "FWD", "Güç ön tekerleklere iletilir. Kompakt, dengeli ve şehir/ralli uyumlu."),
  AWD("Dört Çeker (4WD / AWD)", "AWD", "Dört tekerlekten çekiş. Dik yokuşlarda ve zorlu arazide maksimum tutunma.")
}

enum class ChassisType(
  val label: String,
  val baseWeightKg: Float,
  val wheelBase: Float,
  val bodyLength: Float,
  val defaultColor: Color,
  val dragCoefficient: Float
) {
  COUPE("GT Coupe Spor", 1320f, 90f, 160f, Color(0xFFE53935), 0.29f),
  HATCHBACK("Hot Hatch Compact", 1140f, 78f, 140f, Color(0xFF00ACC1), 0.32f),
  OFFROAD_SUV("4x4 Offroad SUV", 1920f, 98f, 175f, Color(0xFF43A047), 0.45f),
  HYPERCAR("Karbon Hypercar", 1080f, 92f, 165f, Color(0xFFFFB300), 0.26f),
  MUSCLE("Amerikan Muscle V8", 1680f, 94f, 170f, Color(0xFF8E24AA), 0.38f)
}

enum class EngineType(
  val label: String,
  val baseHp: Int,
  val baseTorqueNm: Int,
  val weightKg: Float,
  val maxRpm: Int,
  val soundType: String
) {
  I4_20("2.0L Turbo Sıralı 4", 210, 300, 130f, 7200, "i4"),
  I6_30("3.0L Twin-Cam Sıralı 6", 340, 480, 185f, 7800, "i6"),
  V8_40("4.0L Çift Turbo V8", 480, 620, 225f, 7500, "v8"),
  V12_60("6.0L Yüksek Devirli V12", 650, 720, 290f, 8500, "v12"),
  ELECTRIC_DUAL("Çift Yüksek Torklu EV Motor", 520, 880, 260f, 12000, "ev")
}

enum class InductionType(val label: String, val powerMultiplier: Float, val allowsBoostTuning: Boolean) {
  NATURALLY_ASPIRATED("Atmosferik (N/A)", 1.0f, false),
  TURBO_SINGLE("Tek Büyük Turboşarj", 1.40f, true),
  TURBO_TWIN("Çift Turbo (Twin-Turbo)", 1.65f, true),
  SUPERCHARGER("Mekanik Kompresör (Supercharger)", 1.50f, true)
}

enum class AirFilterType(val label: String, val powerBonusPercent: Float, val soundBonus: String) {
  STOCK("Fabrika Kağıt Filtre", 0f, "Standart emiş sesi"),
  SPORT_PANEL("Spor Kuru Filtre", 3.5f, "Hafif nefes sesi"),
  COLD_AIR_INTAKE("Soğuk Hava Girişi (CAI)", 7.0f, "Belirgin turbo emiş ıslığı"),
  RACING_OPEN_CONE("Yarış Tipi Açık Koni Filtre", 11.0f, "Agresif emiş ve blow-off")
}

enum class ExhaustType(val label: String, val powerBonusPercent: Float, val backfireChance: Float) {
  STOCK("Standart Fabrika Egzozu", 0f, 0.05f),
  SPORT_VALVED("Valfli Spor Egzoz", 4.0f, 0.25f),
  STRAIGHT_PIPE("Düz Boru (Straight Pipe)", 8.5f, 0.65f),
  TITANIUM_RACE("Titanyum Yarış Sistemi", 12.0f, 0.85f)
}

enum class DifferentialType(val label: String, val lockingFactor: Float) {
  OPEN("Açık Diferansiyel (Open Diff)", 0.2f),
  LSD("Sınırlı Kaydırmalı (LSD)", 0.7f),
  LOCKED("Kilitli Diferansiyel (Locked Spool)", 1.0f)
}

enum class TireType(val label: String, val gripFactor: Float, val offroadFactor: Float) {
  STREET("Standart Sokak / Konfor", 1.0f, 0.7f),
  SEMI_SLICK("Yarı-Slick Pist Lastiği", 1.35f, 0.5f),
  FULL_SLICK("Tam Yarış Sliği", 1.60f, 0.3f),
  OFFROAD_MUD("Dişli Arazi / Çamur Lastiği", 1.15f, 1.6f)
}

data class SuspensionConfig(
  val rideHeightMm: Float = 160f,   // 80 - 360 mm
  val stiffness: Float = 42000f,     // 20000 - 85000 N/m
  val damping: Float = 3200f,        // 1200 - 6500 Ns/m
  val camberDeg: Float = -1.5f       // -4.5 to +0.5
)

data class VehicleConfig(
  val id: String = "veh_custom",
  val name: String = "Özel Araç Taslağı",
  val chassis: ChassisType = ChassisType.COUPE,
  val drivetrain: DrivetrainType = DrivetrainType.RWD,
  val engine: EngineType = EngineType.I6_30,
  val induction: InductionType = InductionType.TURBO_TWIN,
  val boostBar: Float = 1.4f,        // 0.2 to 2.8 bar
  val airFilter: AirFilterType = AirFilterType.COLD_AIR_INTAKE,
  val exhaust: ExhaustType = ExhaustType.SPORT_VALVED,
  val differential: DifferentialType = DifferentialType.LSD,
  val suspension: SuspensionConfig = SuspensionConfig(),
  val tire: TireType = TireType.SEMI_SLICK,
  val tireWidthMm: Int = 265,        // 205 - 345 mm
  val customPaintColor: Color? = null
) {
  val calculatedHp: Int
    get() {
      val base = engine.baseHp.toFloat()
      val inductionFactor = if (induction == InductionType.NATURALLY_ASPIRATED || engine == EngineType.ELECTRIC_DUAL) {
        1.0f
      } else {
        1.0f + (induction.powerMultiplier - 1.0f) * (boostBar / 1.0f)
      }
      val filterBonus = 1.0f + (airFilter.powerBonusPercent / 100f)
      val exhaustBonus = 1.0f + (exhaust.powerBonusPercent / 100f)
      return (base * inductionFactor * filterBonus * exhaustBonus).toInt()
    }

  val calculatedTorqueNm: Int
    get() {
      val base = engine.baseTorqueNm.toFloat()
      val inductionFactor = if (induction == InductionType.NATURALLY_ASPIRATED || engine == EngineType.ELECTRIC_DUAL) {
        1.0f
      } else {
        1.0f + (induction.powerMultiplier - 1.0f) * (boostBar / 1.0f) * 1.15f
      }
      val filterBonus = 1.0f + (airFilter.powerBonusPercent / 120f)
      val exhaustBonus = 1.0f + (exhaust.powerBonusPercent / 120f)
      return (base * inductionFactor * filterBonus * exhaustBonus).toInt()
    }

  val calculatedWeightKg: Int
    get() {
      val tireWeight = (tireWidthMm - 205) * 0.15f * 4
      return (chassis.baseWeightKg + engine.weightKg + tireWeight).toInt()
    }

  val powerToWeightRatio: Float
    get() = calculatedHp / (calculatedWeightKg / 1000f)

  val calculatedZeroToHundredSec: Float
    get() {
      val ptw = powerToWeightRatio
      val gripBonus = tire.gripFactor * (tireWidthMm / 255f)
      val driveBonus = when (drivetrain) {
        DrivetrainType.AWD -> 1.25f
        DrivetrainType.RWD -> 1.05f
        DrivetrainType.FWD -> 0.88f
      }
      val theoretical = 12.0f / (ptw / 100f) / (gripBonus * driveBonus)
      return theoretical.coerceIn(1.8f, 14.0f)
    }

  val calculatedTopSpeedKmh: Int
    get() {
      val hp = calculatedHp.toDouble()
      val speed = 120.0 + kotlin.math.sqrt((hp * 2800.0) / (chassis.dragCoefficient * 10.0))
      return speed.toInt().coerceIn(160, 430)
    }
}

object VehiclePresets {
  val DEFAULT_PRESETS = listOf(
    VehicleConfig(
      id = "preset_rwd_drift",
      name = "Apex Drift GT-R",
      chassis = ChassisType.COUPE,
      drivetrain = DrivetrainType.RWD,
      engine = EngineType.I6_30,
      induction = InductionType.TURBO_TWIN,
      boostBar = 1.6f,
      airFilter = AirFilterType.COLD_AIR_INTAKE,
      exhaust = ExhaustType.STRAIGHT_PIPE,
      differential = DifferentialType.LSD,
      suspension = SuspensionConfig(rideHeightMm = 120f, stiffness = 55000f, damping = 3800f, camberDeg = -2.8f),
      tire = TireType.SEMI_SLICK,
      tireWidthMm = 275,
      customPaintColor = Color(0xFFE53935)
    ),
    VehicleConfig(
      id = "preset_fwd_hatch",
      name = "Rally Sprint FWD",
      chassis = ChassisType.HATCHBACK,
      drivetrain = DrivetrainType.FWD,
      engine = EngineType.I4_20,
      induction = InductionType.TURBO_SINGLE,
      boostBar = 1.3f,
      airFilter = AirFilterType.SPORT_PANEL,
      exhaust = ExhaustType.SPORT_VALVED,
      differential = DifferentialType.LSD,
      suspension = SuspensionConfig(rideHeightMm = 160f, stiffness = 38000f, damping = 3000f, camberDeg = -1.2f),
      tire = TireType.STREET,
      tireWidthMm = 235,
      customPaintColor = Color(0xFF00ACC1)
    ),
    VehicleConfig(
      id = "preset_4wd_crawler",
      name = "MudClimber 4x4 Pro",
      chassis = ChassisType.OFFROAD_SUV,
      drivetrain = DrivetrainType.AWD,
      engine = EngineType.V8_40,
      induction = InductionType.SUPERCHARGER,
      boostBar = 1.1f,
      airFilter = AirFilterType.COLD_AIR_INTAKE,
      exhaust = ExhaustType.SPORT_VALVED,
      differential = DifferentialType.LOCKED,
      suspension = SuspensionConfig(rideHeightMm = 320f, stiffness = 28000f, damping = 2400f, camberDeg = -0.2f),
      tire = TireType.OFFROAD_MUD,
      tireWidthMm = 305,
      customPaintColor = Color(0xFF43A047)
    ),
    VehicleConfig(
      id = "preset_awd_hypercar",
      name = "Aero V12 Hypercar",
      chassis = ChassisType.HYPERCAR,
      drivetrain = DrivetrainType.AWD,
      engine = EngineType.V12_60,
      induction = InductionType.TURBO_TWIN,
      boostBar = 2.4f,
      airFilter = AirFilterType.RACING_OPEN_CONE,
      exhaust = ExhaustType.TITANIUM_RACE,
      differential = DifferentialType.LSD,
      suspension = SuspensionConfig(rideHeightMm = 90f, stiffness = 75000f, damping = 5200f, camberDeg = -3.2f),
      tire = TireType.FULL_SLICK,
      tireWidthMm = 335,
      customPaintColor = Color(0xFFFFB300)
    ),
    VehicleConfig(
      id = "preset_muscle_v8",
      name = "Classic V8 Bruiser",
      chassis = ChassisType.MUSCLE,
      drivetrain = DrivetrainType.RWD,
      engine = EngineType.V8_40,
      induction = InductionType.SUPERCHARGER,
      boostBar = 1.8f,
      airFilter = AirFilterType.RACING_OPEN_CONE,
      exhaust = ExhaustType.STRAIGHT_PIPE,
      differential = DifferentialType.LOCKED,
      suspension = SuspensionConfig(rideHeightMm = 150f, stiffness = 44000f, damping = 3200f, camberDeg = -1.0f),
      tire = TireType.SEMI_SLICK,
      tireWidthMm = 295,
      customPaintColor = Color(0xFF8E24AA)
    )
  )
}
