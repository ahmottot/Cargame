package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VehiclePresetDao {
  @Query("SELECT * FROM vehicle_presets ORDER BY isUserCustom ASC, createdAt DESC")
  fun getAllPresets(): Flow<List<VehiclePresetEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPreset(preset: VehiclePresetEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertPresets(presets: List<VehiclePresetEntity>)

  @Query("DELETE FROM vehicle_presets WHERE id = :id")
  suspend fun deletePresetById(id: String)

  @Query("SELECT COUNT(*) FROM vehicle_presets")
  suspend fun getCount(): Int
}
