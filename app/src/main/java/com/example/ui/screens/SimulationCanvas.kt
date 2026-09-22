package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.example.model.ChassisType
import com.example.physics.ParticleType
import com.example.physics.Track
import com.example.physics.TrackType
import com.example.physics.VehiclePhysics
import kotlin.math.*

@Composable
fun SimulationCanvas(
  physics: VehiclePhysics,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier.fillMaxSize()) {
    val canvasW = size.width
    val canvasH = size.height

    // Scale: meters to pixels (e.g. 1 meter = 24 pixels on screen)
    val scale = (canvasW / 36f).coerceIn(18f, 32f)

    // Camera target: smooth centered slightly ahead of vehicle
    val camTargetX = physics.posX + physics.velX * 0.15f
    val camTargetY = physics.posY + 1.2f

    val camOriginX = canvasW * 0.38f
    val camOriginY = canvasH * 0.62f

    fun worldToScreen(wx: Float, wy: Float): Offset {
      val sx = camOriginX + (wx - camTargetX) * scale
      val sy = camOriginY - (wy - camTargetY) * scale
      return Offset(sx, sy)
    }

    // 1. Sky Background
    drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(
          Color(0xFF0D1B2A),
          Color(0xFF1B263B),
          Color(0xFF24282F)
        ),
        startY = 0f,
        endY = canvasH
      )
    )

    // Distant mountain silhouettes
    val mountainPath = Path()
    mountainPath.moveTo(0f, canvasH * 0.55f)
    var mx = 0f
    while (mx < canvasW + 60f) {
      val h = canvasH * 0.55f - (sin(mx * 0.005f + camTargetX * 0.02f) * 60f + cos(mx * 0.012f) * 35f)
      mountainPath.lineTo(mx, h)
      mx += 40f
    }
    mountainPath.lineTo(canvasW, canvasH)
    mountainPath.lineTo(0f, canvasH)
    mountainPath.close()
    drawPath(mountainPath, color = Color(0xFF161A22))

    // 2. Track Terrain Surface & Fill
    val track = physics.track
    val terrainPath = Path()
    val roadTopPath = Path()

    val pFirst = worldToScreen(track.points.first().x, track.points.first().y)
    terrainPath.moveTo(pFirst.x, pFirst.y)
    roadTopPath.moveTo(pFirst.x, pFirst.y)

    track.points.forEach { pt ->
      val sPt = worldToScreen(pt.x, pt.y)
      terrainPath.lineTo(sPt.x, sPt.y)
      roadTopPath.lineTo(sPt.x, sPt.y)
    }

    val pLast = worldToScreen(track.points.last().x, track.points.last().y)
    terrainPath.lineTo(pLast.x, canvasH + 200f)
    terrainPath.lineTo(pFirst.x, canvasH + 200f)
    terrainPath.close()

    // Fill ground with rich layered gradient
    drawPath(
      path = terrainPath,
      brush = Brush.verticalGradient(
        colors = listOf(
          Color(0xFF2A2E37),
          Color(0xFF1A1D24),
          Color(0xFF0F1115)
        ),
        startY = camOriginY,
        endY = canvasH + 200f
      )
    )

    // Road top stroke / surface line
    drawPath(
      path = roadTopPath,
      color = when (track.type) {
        TrackType.HILL_CLIMB -> Color(0xFFFF7043)
        TrackType.CLIFF_DROP -> Color(0xFFFF1744)
        TrackType.SUSPENSION_TEST -> Color(0xFF00E5FF)
        TrackType.DRAG_STRIP -> Color(0xFFFFFFFF)
      },
      style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Draw Water Bodies (Translucent Water Volumes with Surface Ripple)
    track.waterZones.forEach { wZone ->
      val sLeft = worldToScreen(wZone.startX, wZone.surfaceY)
      val sRight = worldToScreen(wZone.endX, wZone.surfaceY)
      val sBottomLeft = worldToScreen(wZone.startX, wZone.surfaceY - wZone.depth)
      val sBottomRight = worldToScreen(wZone.endX, wZone.surfaceY - wZone.depth)

      val waterPath = Path()
      waterPath.moveTo(sLeft.x, sLeft.y)
      val waveSegments = 16
      val stepX = (sRight.x - sLeft.x) / waveSegments
      val animTime = (System.currentTimeMillis() % 2000) * 0.005f
      for (ws in 1..waveSegments) {
        val wx = sLeft.x + stepX * ws
        val waveH = sin(ws * 0.7f + animTime) * 4f
        waterPath.lineTo(wx, sLeft.y + waveH)
      }
      waterPath.lineTo(sBottomRight.x, sBottomRight.y)
      waterPath.lineTo(sBottomLeft.x, sBottomLeft.y)
      waterPath.close()

      // Fill translucent water body
      drawPath(
        path = waterPath,
        brush = Brush.verticalGradient(
          colors = listOf(
            Color(0x8800BCD4),
            Color(0xB00288D1),
            Color(0xD501579B)
          ),
          startY = sLeft.y,
          endY = sBottomLeft.y
        )
      )

      // Highlight water surface rim
      drawLine(
        color = Color(0xDD80DEEA),
        start = sLeft,
        end = sRight,
        strokeWidth = 3.5f
      )
    }

    // Track Distance & Incline Markers
    when (track.type) {
      TrackType.HILL_CLIMB -> {
        val markers = listOf(
          Pair(40f, "15° Başlangıç"),
          Pair(120f, "30° Eğim"),
          Pair(200f, "45° Dik Yokuş"),
          Pair(280f, "60° Duvar Zirve!"),
          Pair(380f, "🏁 ZİRVE / BİTİŞ")
        )
        markers.forEach { (dist, label) ->
          val groundY = track.getGroundHeight(dist)
          val sPos = worldToScreen(dist, groundY)
          drawLine(
            color = Color(0x66FFFFFF),
            start = sPos,
            end = Offset(sPos.x, sPos.y - 45f),
            strokeWidth = 3f
          )
          drawCircle(
            color = Color(0xFFFF5722),
            radius = 6f,
            center = Offset(sPos.x, sPos.y - 45f)
          )
        }
      }
      TrackType.CLIFF_DROP -> {
        val cliffDropPos = worldToScreen(75f, 124f)
        drawLine(
          color = Color(0xFFFF1744),
          start = cliffDropPos,
          end = Offset(cliffDropPos.x, cliffDropPos.y - 60f),
          strokeWidth = 4f
        )
        drawCircle(
          color = Color(0xFFFF1744),
          radius = 8f,
          center = Offset(cliffDropPos.x, cliffDropPos.y - 60f)
        )
      }
      TrackType.DRAG_STRIP -> {
        val dragMarkers = listOf(
          Pair(0f, "START"),
          Pair(100f, "100m"),
          Pair(200f, "200m"),
          Pair(402.3f, "400m (1/4 Mil) 🏁"),
          Pair(1000f, "1000m")
        )
        dragMarkers.forEach { (dist, _) ->
          val sPos = worldToScreen(dist, 0f)
          drawLine(
            color = Color(0x88FFFFFF),
            start = sPos,
            end = Offset(sPos.x, sPos.y - 30f),
            strokeWidth = if (dist == 402.3f) 6f else 3f
          )
        }
      }
      TrackType.SUSPENSION_TEST -> {
        val susMarkers = listOf(
          Pair(30f, "Whoops Kasisleri"),
          Pair(90f, "Derin Çukurlar"),
          Pair(130f, "Artikülasyon Rampası"),
          Pair(180f, "Merdiven Tırmanışı")
        )
        susMarkers.forEach { (dist, _) ->
          val sPos = worldToScreen(dist, track.getGroundHeight(dist))
          drawCircle(color = Color(0xFF00E5FF), radius = 5f, center = Offset(sPos.x, sPos.y - 25f))
        }
      }
    }

    // 3. Render Particles (Smoke, sparks, water splash, steam)
    physics.particles.forEach { p ->
      val pScreen = worldToScreen(p.x, p.y)
      when (p.type) {
        ParticleType.WATER_SPLASH -> {
          drawCircle(
            color = Color(0xCCB2EBF2).copy(alpha = (p.life * 0.8f).coerceIn(0f, 1f)),
            radius = (1.0f - p.life) * 8f + 3f,
            center = pScreen
          )
        }
        ParticleType.STEAM -> {
          drawCircle(
            color = Color(0xCCECEFF1).copy(alpha = (p.life * 0.7f).coerceIn(0f, 1f)),
            radius = (1.0f - p.life) * 22f + 6f,
            center = pScreen
          )
        }
        ParticleType.SMOKE -> {
          drawCircle(
            color = Color(0x77AAAAAA).copy(alpha = (p.life * 0.6f).coerceIn(0f, 1f)),
            radius = (1.0f - p.life) * 16f + 4f,
            center = pScreen
          )
        }
        ParticleType.SPARK -> {
          drawCircle(
            color = Color(0xFFFFAB40).copy(alpha = p.life.coerceIn(0f, 1f)),
            radius = 5f * p.life,
            center = pScreen
          )
        }
      }
    }

    // 4. Vehicle Rendering
    val carCenter = worldToScreen(physics.posX, physics.posY)
    val carAngleDeg = Math.toDegrees(physics.angleRad.toDouble()).toFloat()

    val cosA = cos(physics.angleRad)
    val sinA = sin(physics.angleRad)

    // Calculate Wheel Positions in screen space
    val restSpringLen = (physics.config.suspension.rideHeightMm / 1000f) + 0.15f
    val frontTravel = physics.frontCompression * 0.45f
    val rearTravel = physics.rearCompression * 0.45f

    val frontPivotWorldX = physics.posX + physics.halfWheelbase * cosA
    val frontPivotWorldY = physics.posY + physics.halfWheelbase * sinA
    val frontWheelWorldY = frontPivotWorldY - (restSpringLen - frontTravel)
    val frontWheelScreen = worldToScreen(frontPivotWorldX, frontWheelWorldY)
    val frontPivotScreen = worldToScreen(frontPivotWorldX, frontPivotWorldY)

    val rearPivotWorldX = physics.posX - physics.halfWheelbase * cosA
    val rearPivotWorldY = physics.posY - physics.halfWheelbase * sinA
    val rearWheelWorldY = rearPivotWorldY - (restSpringLen - rearTravel)
    val rearWheelScreen = worldToScreen(rearPivotWorldX, rearWheelWorldY)
    val rearPivotScreen = worldToScreen(rearPivotWorldX, rearPivotWorldY)

    // Draw Suspension Coil Springs (Connecting Chassis to Wheels)
    fun drawSpring(top: Offset, bottom: Offset) {
      val segments = 7
      var prev = top
      val dx = (bottom.x - top.x) / segments
      val dy = (bottom.y - top.y) / segments
      val perpX = -dy * 0.4f
      val perpY = dx * 0.4f

      for (i in 1..segments) {
        val factor = if (i % 2 == 1) 1f else -1f
        val cur = if (i == segments) bottom else Offset(top.x + dx * i + perpX * factor, top.y + dy * i + perpY * factor)
        drawLine(
          color = Color(0xFFFFAB00),
          start = prev,
          end = cur,
          strokeWidth = 3f,
          cap = StrokeCap.Round
        )
        prev = cur
      }
    }

    drawSpring(frontPivotScreen, frontWheelScreen)
    drawSpring(rearPivotScreen, rearWheelScreen)

    // Draw Wheels (Tire & Rims)
    val wheelRadiusPx = physics.wheelRadius * scale
    fun drawWheel(center: Offset, isFront: Boolean) {
      // Tire outer rubber
      drawCircle(
        color = Color(0xFF1E2024),
        radius = wheelRadiusPx,
        center = center
      )
      // Tire inner rim ring
      drawCircle(
        color = Color(0xFF424752),
        radius = wheelRadiusPx * 0.72f,
        center = center,
        style = Stroke(width = 3f)
      )
      // Center hub
      drawCircle(
        color = Color(0xFFCCCCCC),
        radius = wheelRadiusPx * 0.35f,
        center = center
      )
      // Rotating spoke indicators
      val rot = (physics.posX / physics.wheelRadius)
      val spokeLen = wheelRadiusPx * 0.65f
      for (spoke in 0 until 5) {
        val sAngle = rot + spoke * (2 * Math.PI.toFloat() / 5f)
        val end = Offset(
          center.x + cos(sAngle) * spokeLen,
          center.y + sin(sAngle) * spokeLen
        )
        drawLine(
          color = Color(0xFF888888),
          start = center,
          end = end,
          strokeWidth = 2.5f
        )
      }
      // Brake Disc glow if braking
      drawCircle(
        color = Color(0xFF9E9E9E),
        radius = wheelRadiusPx * 0.48f,
        center = center,
        style = Stroke(width = 2f)
      )
    }

    drawWheel(rearWheelScreen, false)
    drawWheel(frontWheelScreen, true)

    // 5. Draw Chassis Body with Rotation and Deformation
    val carPaintColor = physics.config.customPaintColor ?: physics.config.chassis.defaultColor

    rotate(degrees = -carAngleDeg, pivot = carCenter) {
      val halfLenPx = physics.halfLength * scale
      val halfHeightPx = physics.halfHeight * scale

      val fDeformPx = physics.frontDeform * scale * 1.4f
      val rDeformPx = physics.rearDeform * scale * 1.4f
      val roofDeformPx = physics.roofDeform * scale * 1.2f

      val bodyPath = Path()

      when (physics.config.chassis) {
        ChassisType.COUPE, ChassisType.HYPERCAR -> {
          // Sleek aerodynamic body silhouette
          bodyPath.moveTo(-halfLenPx + rDeformPx, halfHeightPx)
          bodyPath.lineTo(-halfLenPx + rDeformPx, -halfHeightPx * 0.3f)
          bodyPath.lineTo(-halfLenPx * 0.55f, -halfHeightPx * 0.7f)
          bodyPath.lineTo(-halfLenPx * 0.2f, -halfHeightPx * 1.7f + roofDeformPx)
          bodyPath.lineTo(halfLenPx * 0.35f, -halfHeightPx * 1.7f + roofDeformPx)
          bodyPath.lineTo(halfLenPx * 0.75f - fDeformPx, -halfHeightPx * 0.5f)
          bodyPath.lineTo(halfLenPx - fDeformPx, -halfHeightPx * 0.2f)
          bodyPath.lineTo(halfLenPx - fDeformPx, halfHeightPx)
          bodyPath.close()
        }
        ChassisType.HATCHBACK -> {
          // Compact hatchback silhouette
          bodyPath.moveTo(-halfLenPx + rDeformPx, halfHeightPx)
          bodyPath.lineTo(-halfLenPx + rDeformPx, -halfHeightPx * 1.5f + roofDeformPx)
          bodyPath.lineTo(-halfLenPx * 0.3f, -halfHeightPx * 1.6f + roofDeformPx)
          bodyPath.lineTo(halfLenPx * 0.35f, -halfHeightPx * 1.6f + roofDeformPx)
          bodyPath.lineTo(halfLenPx * 0.75f - fDeformPx, -halfHeightPx * 0.4f)
          bodyPath.lineTo(halfLenPx - fDeformPx, halfHeightPx)
          bodyPath.close()
        }
        ChassisType.OFFROAD_SUV -> {
          // Rugged tall SUV silhouette
          bodyPath.moveTo(-halfLenPx + rDeformPx, halfHeightPx)
          bodyPath.lineTo(-halfLenPx + rDeformPx, -halfHeightPx * 1.8f + roofDeformPx)
          bodyPath.lineTo(halfLenPx * 0.3f, -halfHeightPx * 1.8f + roofDeformPx)
          bodyPath.lineTo(halfLenPx * 0.6f - fDeformPx, -halfHeightPx * 0.8f)
          bodyPath.lineTo(halfLenPx - fDeformPx, -halfHeightPx * 0.4f)
          bodyPath.lineTo(halfLenPx - fDeformPx, halfHeightPx)
          bodyPath.close()
        }
        ChassisType.MUSCLE -> {
          // Long hood aggressive muscle silhouette
          bodyPath.moveTo(-halfLenPx + rDeformPx, halfHeightPx)
          bodyPath.lineTo(-halfLenPx + rDeformPx, -halfHeightPx * 0.7f)
          bodyPath.lineTo(-halfLenPx * 0.4f, -halfHeightPx * 1.6f + roofDeformPx)
          bodyPath.lineTo(halfLenPx * 0.15f, -halfHeightPx * 1.6f + roofDeformPx)
          bodyPath.lineTo(halfLenPx * 0.5f - fDeformPx, -halfHeightPx * 0.6f)
          bodyPath.lineTo(halfLenPx - fDeformPx, -halfHeightPx * 0.3f)
          bodyPath.lineTo(halfLenPx - fDeformPx, halfHeightPx)
          bodyPath.close()
        }
      }

      // Draw Main Painted Body with metallic gradient
      drawPath(
        path = bodyPath,
        brush = Brush.verticalGradient(
          colors = listOf(
            carPaintColor.copy(alpha = 0.95f),
            carPaintColor,
            carPaintColor.copy(red = carPaintColor.red * 0.6f, green = carPaintColor.green * 0.6f, blue = carPaintColor.blue * 0.6f)
          ),
          startY = carCenter.y - halfHeightPx * 2f,
          endY = carCenter.y + halfHeightPx
        )
      )

      // Body outline stroke
      drawPath(
        path = bodyPath,
        color = Color(0x55000000),
        style = Stroke(width = 3f)
      )

      // Cabin Glass / Windows
      val windowPath = Path()
      windowPath.moveTo(carCenter.x - halfLenPx * 0.15f, carCenter.y - halfHeightPx * 1.55f + roofDeformPx)
      windowPath.lineTo(carCenter.x + halfLenPx * 0.28f, carCenter.y - halfHeightPx * 1.55f + roofDeformPx)
      windowPath.lineTo(carCenter.x + halfLenPx * 0.55f - fDeformPx, carCenter.y - halfHeightPx * 0.6f)
      windowPath.lineTo(carCenter.x - halfLenPx * 0.35f, carCenter.y - halfHeightPx * 0.6f)
      windowPath.close()

      drawPath(
        path = windowPath,
        color = Color(0xDD1E3A5F)
      )
      drawPath(
        path = windowPath,
        color = Color(0x66FFFFFF),
        style = Stroke(width = 2f)
      )

      // Headlight (front right)
      val headlightPos = Offset(carCenter.x + halfLenPx - fDeformPx - 6f, carCenter.y - halfHeightPx * 0.15f)
      drawCircle(
        color = Color(0xFFFFFFFF),
        radius = 5f,
        center = headlightPos
      )
      // Headlight Beam forward
      drawArc(
        color = Color(0x22FFFFFF),
        startAngle = -20f,
        sweepAngle = 40f,
        useCenter = true,
        topLeft = Offset(headlightPos.x, headlightPos.y - 40f),
        size = Size(180f, 80f)
      )

      // Taillight (rear left)
      val taillightPos = Offset(carCenter.x - halfLenPx + rDeformPx + 4f, carCenter.y - halfHeightPx * 0.25f)
      drawCircle(
        color = Color(0xFFFF1744),
        radius = 5f,
        center = taillightPos
      )

      // Rear Spoiler / Wing (Coupe & Hypercar)
      if (physics.config.chassis == ChassisType.COUPE || physics.config.chassis == ChassisType.HYPERCAR) {
        val wingY = carCenter.y - halfHeightPx * 1.1f
        drawLine(
          color = Color(0xFF222222),
          start = Offset(carCenter.x - halfLenPx + rDeformPx + 8f, carCenter.y - halfHeightPx * 0.3f),
          end = Offset(carCenter.x - halfLenPx + rDeformPx + 6f, wingY),
          strokeWidth = 3f
        )
        drawLine(
          color = Color(0xFF111111),
          start = Offset(carCenter.x - halfLenPx + rDeformPx - 4f, wingY),
          end = Offset(carCenter.x - halfLenPx + rDeformPx + 18f, wingY),
          strokeWidth = 5f,
          cap = StrokeCap.Round
        )
      }

      // Damage Cracks Overlay if car has taken impacts
      if (physics.damagePercent > 15f) {
        val crackColor = Color(0x88000000)
        drawLine(
          color = crackColor,
          start = Offset(carCenter.x + halfLenPx * 0.4f, carCenter.y - halfHeightPx * 0.5f),
          end = Offset(carCenter.x + halfLenPx * 0.7f - fDeformPx, carCenter.y - halfHeightPx * 0.2f),
          strokeWidth = 2f
        )
      }

      // Windshield Spiderweb Cracks (BeamNG crash visual)
      if (physics.isWindshieldCracked) {
        val wCenter = Offset(carCenter.x + halfLenPx * 0.15f, carCenter.y - halfHeightPx * 1.0f)
        val glassCrackColor = Color(0xDDFFFFFF)
        for (i in 0 until 6) {
          val gAngle = i * (Math.PI.toFloat() / 3f)
          drawLine(
            color = glassCrackColor,
            start = wCenter,
            end = Offset(wCenter.x + cos(gAngle) * 16f, wCenter.y + sin(gAngle) * 12f),
            strokeWidth = 1.5f
          )
        }
      }
    }

    // Water Ingestion / Engine Hydrolock Flooded Indicator above car
    if (physics.isEngineHydrolocked) {
      val warnPos = Offset(carCenter.x, carCenter.y - 70f)
      drawCircle(
        color = Color(0xDD000000),
        radius = 24f,
        center = warnPos
      )
      drawCircle(
        color = Color(0xFFFF1744),
        radius = 24f,
        center = warnPos,
        style = Stroke(width = 3f)
      )
      // Water droplet / danger indicator icon
      drawLine(
        color = Color(0xFF00E5FF),
        start = Offset(warnPos.x, warnPos.y - 12f),
        end = Offset(warnPos.x, warnPos.y + 4f),
        strokeWidth = 4f,
        cap = StrokeCap.Round
      )
      drawCircle(
        color = Color(0xFFFF5252),
        radius = 3f,
        center = Offset(warnPos.x, warnPos.y + 11f)
      )
    }
  }
}
