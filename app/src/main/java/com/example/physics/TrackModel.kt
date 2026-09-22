package com.example.physics

import kotlin.math.*

enum class TrackType(
  val id: String,
  val title: String,
  val category: String,
  val description: String,
  val iconName: String
) {
  HILL_CLIMB(
    "hill_climb",
    "Yokuş Tırmanma Testi",
    "Çekiş & Tork",
    "15°'den 60°'ye kadar ekstrem dik yokuşlar. FWD, RWD ve AWD çekiş farkını test edin.",
    "terrain"
  ),
  CLIFF_DROP(
    "cliff_drop",
    "Uçurum Kaza & Düşüş",
    "BeamNG Kaza Testi",
    "Yüksek rampadan uçuruma fırlama, kayalıklara çarpma ve gövde deformasyon simülasyonu.",
    "warning"
  ),
  SUSPENSION_TEST(
    "suspension_test",
    "Süspansiyon & Kasis",
    "Şasi & Konfor",
    "Ardışık dalgalı kasisler, derin çukurlar, artikülasyon ve basamak testi.",
    "tune"
  ),
  DRAG_STRIP(
    "drag_strip",
    "0-100 & Hızlanma Pisti",
    "Maksimum Performans",
    "1.5 km düz asfalt pist. 0-100, 0-200 km/s ve 400 metre çeyrek mil süre ölçümü.",
    "speed"
  )
}

data class TrackPoint(val x: Float, val y: Float, val surfaceGrip: Float = 1.0f)

data class WaterZone(
  val startX: Float,
  val endX: Float,
  val surfaceY: Float,
  val depth: Float,
  val name: String
)

class Track(val type: TrackType) {
  val points: List<TrackPoint> = buildTrackPoints(type)
  val waterZones: List<WaterZone> = buildWaterZones(type)

  val startX: Float = 15f
  val startY: Float = getGroundHeight(15f) + 12f

  fun getWaterZone(x: Float): WaterZone? {
    return waterZones.firstOrNull { x in it.startX..it.endX }
  }

  fun getGroundHeight(x: Float): Float {
    if (points.isEmpty()) return 0f
    if (x <= points.first().x) return points.first().y
    if (x >= points.last().x) return points.last().y

    // Binary search or linear scan for segment
    for (i in 0 until points.size - 1) {
      val p1 = points[i]
      val p2 = points[i + 1]
      if (x >= p1.x && x <= p2.x) {
        val t = (x - p1.x) / (p2.x - p1.x)
        return p1.y + t * (p2.y - p1.y)
      }
    }
    return points.last().y
  }

  fun getGroundNormal(x: Float): Pair<Float, Float> {
    if (points.size < 2) return Pair(0f, 1f)
    for (i in 0 until points.size - 1) {
      val p1 = points[i]
      val p2 = points[i + 1]
      if (x >= p1.x && x <= p2.x) {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val len = sqrt(dx * dx + dy * dy)
        if (len < 0.001f) return Pair(0f, 1f)
        // Normal vector pointing upwards
        val nx = -dy / len
        val ny = dx / len
        return if (ny < 0) Pair(-nx, -ny) else Pair(nx, ny)
      }
    }
    return Pair(0f, 1f)
  }

  fun getSurfaceGrip(x: Float): Float {
    for (i in 0 until points.size - 1) {
      val p1 = points[i]
      val p2 = points[i + 1]
      if (x >= p1.x && x <= p2.x) {
        return p1.surfaceGrip
      }
    }
    return 1.0f
  }

  companion object {
    private fun buildTrackPoints(type: TrackType): List<TrackPoint> {
      val list = mutableListOf<TrackPoint>()
      when (type) {
        TrackType.HILL_CLIMB -> {
          // Flat start
          list.add(TrackPoint(-20f, 0f))
          list.add(TrackPoint(0f, 0f))
          list.add(TrackPoint(40f, 0f))

          // River crossing basin (Water depth ~2.2m)
          list.add(TrackPoint(50f, 2.5f))
          list.add(TrackPoint(55f, 0.5f, surfaceGrip = 0.75f))
          list.add(TrackPoint(75f, 0.5f, surfaceGrip = 0.75f))
          list.add(TrackPoint(80f, 2.5f))

          // 15 deg incline
          list.add(TrackPoint(105f, 16f))
          list.add(TrackPoint(125f, 16f)) // brief rest

          // 30 deg incline
          list.add(TrackPoint(180f, 50f))
          list.add(TrackPoint(200f, 50f)) // brief rest

          // 45 deg steep incline
          list.add(TrackPoint(260f, 110f, surfaceGrip = 0.95f))
          list.add(TrackPoint(280f, 110f))

          // 60 deg extreme wall
          list.add(TrackPoint(340f, 214f, surfaceGrip = 0.85f))

          // Summit plateau & finish line
          list.add(TrackPoint(380f, 215f))
          list.add(TrackPoint(450f, 215f))
          list.add(TrackPoint(500f, 215f))
        }

        TrackType.CLIFF_DROP -> {
          // High cliff edge launchpad
          val cliffHeight = 120f
          list.add(TrackPoint(-20f, cliffHeight))
          list.add(TrackPoint(0f, cliffHeight))
          list.add(TrackPoint(50f, cliffHeight))
          // Launch ramp angled slightly up
          list.add(TrackPoint(75f, cliffHeight + 4f))

          // Vertical sheer cliff drop!
          list.add(TrackPoint(85f, cliffHeight - 15f))
          list.add(TrackPoint(105f, 40f)) // jagged rocky shelf 1
          list.add(TrackPoint(115f, 38f))
          list.add(TrackPoint(135f, -10f)) // rocky shelf 2
          list.add(TrackPoint(145f, -12f))

          // Deep water quarry / lake basin at the base of the cliff
          list.add(TrackPoint(165f, -70f, surfaceGrip = 0.6f))
          list.add(TrackPoint(235f, -70f, surfaceGrip = 0.6f))
          // Shore & barrier
          list.add(TrackPoint(255f, -42f))
          list.add(TrackPoint(270f, -60f))
          list.add(TrackPoint(350f, -60f))
        }

        TrackType.SUSPENSION_TEST -> {
          list.add(TrackPoint(-20f, 0f))
          list.add(TrackPoint(0f, 0f))
          list.add(TrackPoint(30f, 0f))

          // 1. Whoops / wavy speed bumps (6 waves)
          var curX = 30f
          for (i in 0 until 6) {
            list.add(TrackPoint(curX + 3f, 2.5f))
            list.add(TrackPoint(curX + 6f, 0f))
            curX += 6f
          }
          list.add(TrackPoint(curX + 15f, 0f))
          curX += 15f

          // 2. Sharp potholes and high curbs
          list.add(TrackPoint(curX + 2f, -1.8f))
          list.add(TrackPoint(curX + 5f, -1.8f))
          list.add(TrackPoint(curX + 7f, 0f))
          curX += 10f

          list.add(TrackPoint(curX + 2f, 3.5f))
          list.add(TrackPoint(curX + 5f, 3.5f))
          list.add(TrackPoint(curX + 6f, 0f))
          curX += 12f

          // Water & Mud wading basin (Depth ~2m)
          list.add(TrackPoint(curX + 2f, -2.0f, surfaceGrip = 0.65f))
          list.add(TrackPoint(curX + 22f, -2.0f, surfaceGrip = 0.65f))
          list.add(TrackPoint(curX + 25f, 0f))
          curX += 28f

          // 3. Articulation flex ramp (one side ramp)
          list.add(TrackPoint(curX + 10f, 5.5f))
          list.add(TrackPoint(curX + 20f, 8.5f))
          list.add(TrackPoint(curX + 25f, 0f))
          curX += 30f

          // 4. Rocky stair climb
          for (i in 0 until 5) {
            list.add(TrackPoint(curX + 4f, (i + 1) * 2.2f))
            curX += 4f
          }
          list.add(TrackPoint(curX + 50f, 11f))
          list.add(TrackPoint(curX + 100f, 11f))
        }

        TrackType.DRAG_STRIP -> {
          // Flat high speed strip
          list.add(TrackPoint(-20f, 0f))
          list.add(TrackPoint(0f, 0f))
          list.add(TrackPoint(500f, 0f))
          list.add(TrackPoint(1000f, 0f))
          list.add(TrackPoint(1500f, 0f))
        }
      }
      return list
    }

    private fun buildWaterZones(type: TrackType): List<WaterZone> {
      return when (type) {
        TrackType.HILL_CLIMB -> listOf(
          WaterZone(
            startX = 50f,
            endX = 80f,
            surfaceY = 2.5f,
            depth = 2.0f,
            name = "Dağ Nehri Geçişi"
          )
        )
        TrackType.CLIFF_DROP -> listOf(
          WaterZone(
            startX = 160f,
            endX = 255f,
            surfaceY = -48f,
            depth = 22f,
            name = "Uçurum Su Göleti"
          )
        )
        TrackType.SUSPENSION_TEST -> listOf(
          WaterZone(
            startX = 110f,
            endX = 135f,
            surfaceY = 0.0f,
            depth = 2.0f,
            name = "Derin Su & Çamur Test Havuzu"
          )
        )
        TrackType.DRAG_STRIP -> emptyList()
      }
    }
  }
}
