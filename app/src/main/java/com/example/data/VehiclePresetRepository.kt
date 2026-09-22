package com.example.data

import com.example.model.VehicleConfig
import com.example.model.VehiclePresets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VehiclePresetRepository(private val presetDao: VehiclePresetDao) {

  val presetEntities: Flow<List<VehiclePresetEntity>> = presetDao.getAllPresets()

  val allPresets: Flow<List<VehicleConfig>> = presetEntities.map { entities ->
    entities.map { it.toVehicleConfig() }
  }

  suspend fun seedDefaultsIfEmpty() {
    val count = presetDao.getCount()
    if (count == 0) {
      val defaultEntities = VehiclePresets.DEFAULT_PRESETS.mapIndexed { index, preset ->
        VehiclePresetEntity.fromVehicleConfig(preset, isUserCustom = false).copy(
          createdAt = System.currentTimeMillis() - (1000L * (VehiclePresets.DEFAULT_PRESETS.size - index))
        )
      }
      presetDao.insertPresets(defaultEntities)
    }
  }

  suspend fun savePreset(config: VehicleConfig) {
    val entity = VehiclePresetEntity.fromVehicleConfig(config, isUserCustom = true)
    presetDao.insertPreset(entity)
  }

  suspend fun deletePreset(id: String) {
    presetDao.deletePresetById(id)
  }
}
