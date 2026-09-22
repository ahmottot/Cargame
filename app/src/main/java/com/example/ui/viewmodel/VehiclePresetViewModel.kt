package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.VehiclePresetEntity
import com.example.data.VehiclePresetRepository
import com.example.model.VehicleConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VehiclePresetViewModel(
  application: Application,
  private val repository: VehiclePresetRepository = VehiclePresetRepository(
    AppDatabase.getInstance(application).vehiclePresetDao()
  )
) : AndroidViewModel(application) {

  val presetsState: StateFlow<List<VehiclePresetEntity>> = repository.presetEntities
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  init {
    viewModelScope.launch {
      repository.seedDefaultsIfEmpty()
    }
  }

  fun saveCustomPreset(name: String, config: VehicleConfig) {
    viewModelScope.launch {
      val customId = "preset_user_${System.currentTimeMillis()}"
      val presetToSave = config.copy(id = customId, name = name.trim())
      repository.savePreset(presetToSave)
    }
  }

  fun updatePreset(config: VehicleConfig) {
    viewModelScope.launch {
      repository.savePreset(config)
    }
  }

  fun deletePreset(id: String) {
    viewModelScope.launch {
      repository.deletePreset(id)
    }
  }

  class Factory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return VehiclePresetViewModel(application) as T
    }
  }
}
