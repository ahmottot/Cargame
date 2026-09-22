package com.example.physics

import com.example.model.ChassisType
import com.example.model.DrivetrainType
import com.example.model.EngineType
import com.example.model.InductionType
import com.example.model.VehicleConfig
import kotlin.math.*

enum class ParticleType {
  SPARK,
  SMOKE,
  WATER_SPLASH,
  STEAM
}

data class Particle(
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  var life: Float, // 1.0 down to 0.0
  val type: ParticleType = ParticleType.SMOKE
)

class VehiclePhysics(
  var config: VehicleConfig,
  var track: Track
) {
  // World position in meters
  var posX: Float = track.startX
  var posY: Float = track.startY
  var velX: Float = 0f
  var velY: Float = 0f

  var angleRad: Float = 0f
  var angularVel: Float = 0f

  // Chassis geometry
  val halfLength: Float = 2.1f // ~4.2m car
  val halfHeight: Float = 0.55f
  val wheelRadius: Float = 0.36f
  val halfWheelbase: Float = 1.35f

  // Suspension states
  var frontCompression: Float = 0.15f
  var rearCompression: Float = 0.15f
  var frontContact: Boolean = false
  var rearContact: Boolean = false

  // Dynamic Drivetrain & Engine state
  var liveDrivetrain: DrivetrainType = config.drivetrain
  var rpm: Float = 900f
  var gear: Int = 1
  var currentBoostBar: Float = 0f
  var speedKmh: Float = 0f

  // Damage & Deformation (BeamNG style)
  var damagePercent: Float = 0f
  var frontDeform: Float = 0f
  var rearDeform: Float = 0f
  var roofDeform: Float = 0f
  var peakGForce: Float = 0f
  var isWindshieldCracked: Boolean = false
  var wheelAlignmentDamage: Float = 0f

  // Water & Engine Hydrolock Physics
  var isInWater: Boolean = false
  var waterSubmersionDepth: Float = 0f
  var isEngineHydrolocked: Boolean = false
  var waterIngestionTimer: Float = 0f

  // Drag / 0-100 Test Timer
  var isTimingZeroToHundred: Boolean = false
  var zeroToHundredTimeSec: Float? = null
  var quarterMileTimeSec: Float? = null
  var dragRunStarted: Boolean = false
  var dragTimer: Float = 0f
  var startDistanceX: Float = track.startX

  // Visual effects
  val particles = mutableListOf<Particle>()
  var lastBackfireTime: Long = 0L

  val gearRatios = floatArrayOf(3.6f, 2.3f, 1.6f, 1.2f, 0.95f, 0.78f)
  val finalDrive = 3.7f

  // 1. TAMIR ET / YENILE (Quick Repair - Restore car without resetting position)
  fun repairVehicle() {
    damagePercent = 0f
    frontDeform = 0f
    rearDeform = 0f
    roofDeform = 0f
    isWindshieldCracked = false
    wheelAlignmentDamage = 0f
    isEngineHydrolocked = false
    waterIngestionTimer = 0f
    rpm = 900f
  }

  // 2. BULUNDUĞU YERDE YENİDEN DOĞMA (Recover In Place / Flip Upright - BeamNG feature)
  fun recoverInPlace() {
    // Keep exact posX
    angleRad = 0f
    angularVel = 0f
    velX = 0f
    velY = 0f

    val groundY = track.getGroundHeight(posX)
    val waterZone = track.getWaterZone(posX)
    val safeBaseY = if (waterZone != null) max(groundY, waterZone.surfaceY) else groundY
    val restSpringLen = (config.suspension.rideHeightMm / 1000f) + 0.15f
    posY = safeBaseY + restSpringLen + wheelRadius + 0.4f

    // Drain water & restart engine
    isEngineHydrolocked = false
    waterIngestionTimer = 0f
    rpm = 900f
    gear = 1
  }

  // 3. PİST BAŞINA SIFIRLA (Full Reset to track starting point)
  fun reset(newTrack: Track? = null) {
    if (newTrack != null) {
      track = newTrack
    }
    posX = track.startX
    posY = track.startY
    velX = 0f
    velY = 0f
    angleRad = 0f
    angularVel = 0f
    frontCompression = 0.15f
    rearCompression = 0.15f
    frontContact = false
    rearContact = false
    rpm = 900f
    gear = 1
    currentBoostBar = 0f
    speedKmh = 0f
    damagePercent = 0f
    frontDeform = 0f
    rearDeform = 0f
    roofDeform = 0f
    peakGForce = 0f
    isWindshieldCracked = false
    wheelAlignmentDamage = 0f
    isInWater = false
    waterSubmersionDepth = 0f
    isEngineHydrolocked = false
    waterIngestionTimer = 0f
    isTimingZeroToHundred = false
    zeroToHundredTimeSec = null
    quarterMileTimeSec = null
    dragRunStarted = false
    dragTimer = 0f
    startDistanceX = track.startX
    particles.clear()
    liveDrivetrain = config.drivetrain
  }

  fun update(
    dt: Float,
    throttle: Float,  // 0.0 to 1.0
    brake: Float,     // 0.0 to 1.0
    handbrake: Boolean,
    reverse: Boolean
  ) {
    val massKg = config.calculatedWeightKg.toFloat()
    val gravity = -9.81f

    val cosA = cos(angleRad)
    val sinA = sin(angleRad)

    // -------------------------------------------------------------
    // 1. WATER HAZARD & ENGINE HYDROLOCK (Suya Girince Bozulma Fiziği)
    // -------------------------------------------------------------
    val waterZone = track.getWaterZone(posX)
    var waterForceY = 0f
    var waterForceX = 0f

    if (waterZone != null && posY < waterZone.surfaceY + halfHeight + 0.3f) {
      isInWater = true
      waterSubmersionDepth = (waterZone.surfaceY - (posY - halfHeight)).coerceAtLeast(0f)

      // Engine Air Intake Position (front hood area)
      val intakeX = posX + halfLength * 0.65f * cosA
      val intakeY = posY + halfLength * 0.65f * sinA + (config.suspension.rideHeightMm / 1000f) * 0.2f

      // Check if engine air intake is submerged under water
      if (intakeY < waterZone.surfaceY) {
        waterIngestionTimer += dt
        // After 0.4 seconds under water, engine hydro-locks!
        if (waterIngestionTimer > 0.4f) {
          isEngineHydrolocked = true
          rpm = 0f

          // Emit white steam particles from submerged engine bay
          if (Math.random() < 0.5) {
            particles.add(
              Particle(
                x = intakeX + (Math.random().toFloat() - 0.5f) * 0.6f,
                y = waterZone.surfaceY,
                vx = (Math.random().toFloat() - 0.5f) * 1.5f,
                vy = Math.random().toFloat() * 3.5f + 1.2f,
                life = 1.0f,
                type = ParticleType.STEAM
              )
            )
          }
        }
      } else if (!isEngineHydrolocked) {
        // Intake above water, clear ingestion timer
        waterIngestionTimer = (waterIngestionTimer - dt * 0.5f).coerceAtLeast(0f)
      }

      // Water Buoyancy Force (Archimedes principle)
      val displacedVolume = (waterSubmersionDepth / (halfHeight * 2f)).coerceIn(0f, 1.8f)
      val buoyantForce = displacedVolume * massKg * 11.5f
      waterForceY += buoyantForce

      // Water Hydrodynamic Drag
      val waterDragCoeff = 3.5f * (waterSubmersionDepth / 1.0f).coerceIn(0.2f, 2.0f)
      waterForceX -= velX * massKg * waterDragCoeff
      waterForceY -= velY * massKg * waterDragCoeff

      // Water splash particles if entering or moving in water
      val speedInWater = sqrt(velX * velX + velY * velY)
      if (speedInWater > 2.5f && Math.random() < 0.6) {
        for (w in 0 until 3) {
          particles.add(
            Particle(
              x = posX + (Math.random().toFloat() - 0.5f) * (halfLength * 2f),
              y = waterZone.surfaceY + 0.1f,
              vx = -velX * 0.3f + (Math.random().toFloat() - 0.5f) * 6f,
              vy = Math.random().toFloat() * 6.5f + 2f,
              life = 0.8f,
              type = ParticleType.WATER_SPLASH
            )
          )
        }
      }
    } else {
      isInWater = false
      waterSubmersionDepth = 0f
    }

    // -------------------------------------------------------------
    // 2. ENGINE & TURBO DYNAMICS (Disabled if hydrolocked!)
    // -------------------------------------------------------------
    val effectiveThrottle = if (isEngineHydrolocked) 0f else throttle

    val targetBoost = if (config.induction == InductionType.NATURALLY_ASPIRATED || config.engine == EngineType.ELECTRIC_DUAL || isEngineHydrolocked) {
      0f
    } else {
      config.boostBar * effectiveThrottle
    }
    // Turbo spool up / blow off
    val spoolSpeed = if (targetBoost > currentBoostBar) 3.5f else 6.0f
    currentBoostBar += (targetBoost - currentBoostBar) * (spoolSpeed * dt).coerceIn(0f, 1f)

    // Speed calculation
    val forwardSpeed = velX * cosA + velY * sinA
    speedKmh = abs(forwardSpeed) * 3.6f

    // Drag Strip Timers
    if (effectiveThrottle > 0.05f && !dragRunStarted) {
      dragRunStarted = true
      isTimingZeroToHundred = true
      dragTimer = 0f
      startDistanceX = posX
    }
    if (dragRunStarted) {
      dragTimer += dt
      if (speedKmh >= 100f && isTimingZeroToHundred && zeroToHundredTimeSec == null) {
        zeroToHundredTimeSec = dragTimer
        isTimingZeroToHundred = false
      }
      val dist = posX - startDistanceX
      if (dist >= 402.3f && quarterMileTimeSec == null) {
        quarterMileTimeSec = dragTimer
      }
    }

    // Gearbox logic
    val wheelAngularSpeed = abs(forwardSpeed) / wheelRadius
    val wheelRpm = (wheelAngularSpeed * 60f / (2 * Math.PI.toFloat())) * finalDrive
    val maxRpm = config.engine.maxRpm.toFloat()
    val idleRpm = 850f

    if (isEngineHydrolocked) {
      rpm = 0f
    } else if (reverse) {
      gear = -1
      rpm = (idleRpm + effectiveThrottle * 3200f).coerceIn(idleRpm, 4500f)
    } else {
      val ratio = gearRatios[gear - 1]
      val calculatedRpm = wheelRpm * ratio
      rpm = max(idleRpm, calculatedRpm + (effectiveThrottle * 1200f * (1f - (frontContact.toInt() + rearContact.toInt()) * 0.4f)))
      if (rpm > maxRpm * 0.88f && gear < 6) {
        gear++
      } else if (rpm < maxRpm * 0.45f && gear > 1) {
        gear--
      }
      rpm = rpm.coerceIn(idleRpm, maxRpm)
    }

    // Engine Drive Force (Zero if engine drowned!)
    val totalTorque = if (isEngineHydrolocked) 0f else {
      config.calculatedTorqueNm.toFloat() * (1f + currentBoostBar * 0.35f) * (rpm / maxRpm).coerceIn(0.2f, 1.1f)
    }
    val driveForce = if (isEngineHydrolocked) {
      0f
    } else if (reverse) {
      -totalTorque * 3.5f * effectiveThrottle * (1f - damagePercent * 0.007f)
    } else {
      totalTorque * gearRatios[(gear - 1).coerceIn(0, 5)] * finalDrive * effectiveThrottle * (1f - damagePercent * 0.007f) / wheelRadius
    }

    // -------------------------------------------------------------
    // 3. SUSPENSION & WHEEL DYNAMICS
    // -------------------------------------------------------------
    val frontPivotX = posX + halfWheelbase * cosA - (halfHeight - 0.2f) * -sinA
    val frontPivotY = posY + halfWheelbase * sinA + (halfHeight - 0.2f) * cosA

    val rearPivotX = posX - halfWheelbase * cosA - (halfHeight - 0.2f) * -sinA
    val rearPivotY = posY - halfWheelbase * sinA + (halfHeight - 0.2f) * cosA

    val restSpringLen = (config.suspension.rideHeightMm / 1000f) + 0.15f
    val springK = config.suspension.stiffness
    val damperC = config.suspension.damping

    // Ground contact for front wheel
    val frontGroundY = track.getGroundHeight(frontPivotX)
    val frontDistToGround = frontPivotY - frontGroundY
    frontContact = frontDistToGround <= restSpringLen + wheelRadius
    val frontTravel = if (frontContact) (restSpringLen + wheelRadius - frontDistToGround).coerceIn(0f, 0.45f) else 0f
    frontCompression = frontTravel / 0.45f

    // Ground contact for rear wheel
    val rearGroundY = track.getGroundHeight(rearPivotX)
    val rearDistToGround = rearPivotY - rearGroundY
    rearContact = rearDistToGround <= restSpringLen + wheelRadius
    val rearTravel = if (rearContact) (restSpringLen + wheelRadius - rearDistToGround).coerceIn(0f, 0.45f) else 0f
    rearCompression = rearTravel / 0.45f

    // Forces accumulation
    var totalForceX = waterForceX
    var totalForceY = massKg * gravity + waterForceY
    var totalTorqueChassis = 0f

    var frontNormal = 0f
    if (frontContact) {
      val springF = springK * frontTravel
      val damperF = damperC * (-velY - angularVel * halfWheelbase * cosA)
      frontNormal = max(0f, springF + damperF)
      totalForceY += frontNormal * cosA
      totalForceX += -frontNormal * sinA
      totalTorqueChassis += frontNormal * (halfWheelbase * cosA)
    }

    var rearNormal = 0f
    if (rearContact) {
      val springF = springK * rearTravel
      val damperF = damperC * (-velY + angularVel * halfWheelbase * cosA)
      rearNormal = max(0f, springF + damperF)
      totalForceY += rearNormal * cosA
      totalForceX += -rearNormal * sinA
      totalTorqueChassis -= rearNormal * (halfWheelbase * cosA)
    }

    // Traction based on live drivetrain (FWD / RWD / AWD)
    val baseGrip = config.tire.gripFactor * (config.tireWidthMm / 245f)
    val trackGrip = track.getSurfaceGrip(posX)
    val effectiveGrip = baseGrip * trackGrip

    val maxFrontTraction = frontNormal * effectiveGrip
    val maxRearTraction = rearNormal * effectiveGrip

    var frontDriveF = 0f
    var rearDriveF = 0f

    when (liveDrivetrain) {
      DrivetrainType.FWD -> {
        frontDriveF = driveForce.coerceIn(-maxFrontTraction, maxFrontTraction)
      }
      DrivetrainType.RWD -> {
        rearDriveF = driveForce.coerceIn(-maxRearTraction, maxRearTraction)
      }
      DrivetrainType.AWD -> {
        val halfDrive = driveForce * 0.5f
        frontDriveF = halfDrive.coerceIn(-maxFrontTraction, maxFrontTraction)
        rearDriveF = halfDrive.coerceIn(-maxRearTraction, maxRearTraction)
      }
    }

    // Braking
    if (brake > 0f) {
      val brakeForceTotal = massKg * 9.81f * 1.2f * brake
      val fBrakeFront = (brakeForceTotal * 0.65f).coerceAtMost(maxFrontTraction)
      val fBrakeRear = (brakeForceTotal * 0.35f).coerceAtMost(maxRearTraction)
      val sign = if (forwardSpeed > 0.05f) -1f else if (forwardSpeed < -0.05f) 1f else 0f
      frontDriveF += fBrakeFront * sign
      rearDriveF += fBrakeRear * sign
    }
    if (handbrake) {
      val hBrake = maxRearTraction * 1.5f
      val sign = if (forwardSpeed > 0.05f) -1f else if (forwardSpeed < -0.05f) 1f else 0f
      rearDriveF += hBrake * sign
    }

    val totalTractionX = (frontDriveF + rearDriveF) * cosA
    val totalTractionY = (frontDriveF + rearDriveF) * sinA

    totalForceX += totalTractionX
    totalForceY += totalTractionY

    // Air Drag
    val airDrag = 0.5f * 1.225f * config.chassis.dragCoefficient * 2.2f * (velX * abs(velX))
    totalForceX -= airDrag
    val rollingResist = 0.015f * massKg * 9.81f * (if (velX > 0) 1f else -1f)
    if (abs(velX) > 0.2f && (frontContact || rearContact)) {
      totalForceX -= rollingResist
    }

    // -------------------------------------------------------------
    // 4. CHASSIS COLLISION & BEAMNG DEFORMATION (Gelişmiş Çarpışma)
    // -------------------------------------------------------------
    val corners = listOf(
      Pair(posX + halfLength * cosA - (halfHeight - frontDeform) * -sinA, posY + halfLength * sinA + (halfHeight - frontDeform) * cosA),
      Pair(posX - halfLength * cosA - (halfHeight - rearDeform) * -sinA, posY - halfLength * sinA + (halfHeight - rearDeform) * cosA),
      Pair(posX + (halfLength - 0.5f) * cosA - (halfHeight + 0.6f - roofDeform) * sinA, posY + (halfLength - 0.5f) * sinA + (halfHeight + 0.6f - roofDeform) * cosA),
      Pair(posX - (halfLength - 0.5f) * cosA - (halfHeight + 0.6f - roofDeform) * sinA, posY - (halfLength - 0.5f) * sinA + (halfHeight + 0.6f - roofDeform) * cosA)
    )

    corners.forEachIndexed { index, corner ->
      val groundY = track.getGroundHeight(corner.first)
      if (corner.second < groundY) {
        val penetration = groundY - corner.second
        val normal = track.getGroundNormal(corner.first)
        val impactSpeed = -(velX * normal.first + velY * normal.second)

        val restoreForce = penetration * massKg * 380f
        totalForceX += normal.first * restoreForce
        totalForceY += normal.second * restoreForce

        val rx = corner.first - posX
        val ry = corner.second - posY
        totalTorqueChassis += (rx * normal.second - ry * normal.first) * restoreForce * 0.045f

        velX *= 0.94f
        velY *= 0.84f
        angularVel *= 0.86f

        if (impactSpeed > 4.2f) {
          val damage = (impactSpeed - 3.8f) * 2.2f
          damagePercent = (damagePercent + damage).coerceIn(0f, 100f)
          val gForce = impactSpeed / (dt * 9.81f)
          if (gForce > peakGForce) peakGForce = gForce

          if (impactSpeed > 7f) {
            isWindshieldCracked = true
            wheelAlignmentDamage = (wheelAlignmentDamage + 0.08f).coerceAtMost(0.4f)
          }

          when (index) {
            0 -> frontDeform = (frontDeform + impactSpeed * 0.035f).coerceAtMost(0.55f)
            1 -> rearDeform = (rearDeform + impactSpeed * 0.035f).coerceAtMost(0.55f)
            2, 3 -> roofDeform = (roofDeform + impactSpeed * 0.045f).coerceAtMost(0.60f)
          }

          // Spawn crash sparks and smoke
          for (p in 0 until 5) {
            particles.add(
              Particle(
                x = corner.first,
                y = corner.second,
                vx = -normal.first * 5f + (Math.random().toFloat() - 0.5f) * 8f,
                vy = normal.second * 6f + Math.random().toFloat() * 6f,
                life = 1.0f,
                type = if (p % 2 == 0) ParticleType.SMOKE else ParticleType.SPARK
              )
            )
          }
        }
      }
    }

    // Exhaust Backfire & Pops
    if (!isEngineHydrolocked && effectiveThrottle < 0.1f && currentBoostBar > 0.4f && Math.random().toFloat() < config.exhaust.backfireChance * 0.2f) {
      val exhaustX = posX - (halfLength + 0.1f) * cosA
      val exhaustY = posY - (halfLength + 0.1f) * sinA
      for (k in 0 until 3) {
        particles.add(
          Particle(
            x = exhaustX,
            y = exhaustY,
            vx = -cosA * 4f + (Math.random().toFloat() - 0.5f) * 2f,
            vy = -sinA * 4f + (Math.random().toFloat() - 0.5f) * 2f,
            life = 0.8f,
            type = ParticleType.SPARK
          )
        )
      }
    }

    // -------------------------------------------------------------
    // 5. NUMERICAL INTEGRATION
    // -------------------------------------------------------------
    val accelX = totalForceX / massKg
    val accelY = totalForceY / massKg
    val inertia = (1f / 12f) * massKg * (halfLength * 2 * halfLength * 2 + halfHeight * 2 * halfHeight * 2)
    val angularAccel = totalTorqueChassis / inertia

    velX += accelX * dt
    velY += accelY * dt
    angularVel += angularAccel * dt

    if (frontContact || rearContact) {
      val lateralSpeed = -velX * sinA + velY * cosA
      val lateralFriction = lateralSpeed * massKg * 14f * effectiveGrip
      velX += (lateralFriction * sinA / massKg) * dt
      velY -= (lateralFriction * cosA / massKg) * dt
      angularVel *= (1f - 0.05f * effectiveGrip)
    }

    posX += velX * dt
    posY += velY * dt
    angleRad += angularVel * dt

    // Update particles
    val it = particles.iterator()
    while (it.hasNext()) {
      val p = it.next()
      p.x += p.vx * dt
      p.y += p.vy * dt
      when (p.type) {
        ParticleType.WATER_SPLASH -> {
          p.vy += -9.81f * 1.2f * dt
          p.life -= dt * 1.8f
        }
        ParticleType.STEAM -> {
          p.vy += 1.2f * dt
          p.life -= dt * 1.2f
        }
        ParticleType.SMOKE -> {
          p.vy += 0.8f * dt
          p.life -= dt * 2.2f
        }
        ParticleType.SPARK -> {
          p.vy += -9.81f * 0.6f * dt
          p.life -= dt * 2.8f
        }
      }
      if (p.life <= 0f) {
        it.remove()
      }
    }
  }

  private fun Boolean.toInt() = if (this) 1 else 0
}
