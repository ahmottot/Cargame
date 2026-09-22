package com.example.data

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.*

@Entity(tableName = "vehicle_presets")
data class VehiclePresetEntity(
  @PrimaryKey val id: String,
  val name: String,
  val chassis: String,
  val drivetrain: String,
  val engine: String,
  val induction: String,
  val boostBar: Float,
  val airFilter: String,
  val exhaust: String,
  val differential: String,
  val rideHeightMm: Float,
  val stiffness: Float,
  val damping: Float,
  val camberDeg: Float,
  val tire: String,
  val tireWidthMm: Int,
  val paintColorArgb: Long?,
  val isUserCustom: Boolean = true,
  val createdAt: Long = System.currentTimeMillis()
) {
  fun toVehicleConfig(): VehicleConfig {
    val chassisEnum = runCatching { ChassisType.valueOf(chassis) }.getOrDefault(ChassisType.COUPE)
    val drivetrainEnum = runCatching { DrivetrainType.valueOf(drivetrain) }.getOrDefault(DrivetrainType.RWD)
    val engineEnum = runCatching { EngineType.valueOf(engine) }.getOrDefault(EngineType.I6_30)
    val inductionEnum = runCatching { InductionType.valueOf(induction) }.getOrDefault(InductionType.TURBO_TWIN)
    val airFilterEnum = runCatching { AirFilterType.valueOf(airFilter) }.getOrDefault(AirFilterType.COLD_AIR_INTAKE)
    val exhaustEnum = runCatching { ExhaustType.valueOf(exhaust) }.getOrDefault(ExhaustType.SPORT_VALVED)
    val differentialEnum = runCatching { DifferentialType.valueOf(differential) }.getOrDefault(DifferentialType.LSD)
    val tireEnum = runCatching { TireType.valueOf(tire) }.getOrDefault(TireType.SEMI_SLICK)

    val color = paintColorArgb?.let { Color(it.toULong()) }

    return VehicleConfig(
      id = id,
      name = name,
      chassis = chassisEnum,
      drivetrain = drivetrainEnum,
      engine = engineEnum,
      induction = inductionEnum,
      boostBar = boostBar,
      airFilter = airFilterEnum,
      exhaust = exhaustEnum,
      differential = differentialEnum,
      suspension = SuspensionConfig(
        rideHeightMm = rideHeightMm,
        stiffness = stiffness,
        damping = damping,
        camberDeg = camberDeg
      ),
      tire = tireEnum,
      tireWidthMm = tireWidthMm,
      customPaintColor = color
    )
  }

  companion object {
    fun fromVehicleConfig(config: VehicleConfig, isUserCustom: Boolean = true): VehiclePresetEntity {
      val colorVal = config.customPaintColor?.value?.toLong()
      return VehiclePresetEntity(
        id = config.id,
        name = config.name,
        chassis = config.chassis.name,
        drivetrain = config.drivetrain.name,
        engine = config.engine.name,
        induction = config.induction.name,
        boostBar = config.boostBar,
        airFilter = config.airFilter.name,
        exhaust = config.exhaust.name,
        differential = config.differential.name,
        rideHeightMm = config.suspension.rideHeightMm,
        stiffness = config.suspension.stiffness,
        damping = config.suspension.damping,
        camberDeg = config.suspension.camberDeg,
        tire = config.tire.name,
        tireWidthMm = config.tireWidthMm,
        paintColorArgb = colorVal,
        isUserCustom = isUserCustom
      )
    }
  }
}
