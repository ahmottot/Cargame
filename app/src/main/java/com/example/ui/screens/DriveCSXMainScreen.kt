package com.example.ui.screens

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.VehicleConfig
import com.example.model.VehiclePresets
import com.example.physics.Track
import com.example.physics.TrackType
import com.example.physics.VehiclePhysics
import com.example.ui.viewmodel.VehiclePresetViewModel

enum class AppScreen {
  SIMULATION,
  GARAGE
}

@Composable
fun DriveCSXMainScreen() {
  val context = LocalContext.current
  val presetViewModel: VehiclePresetViewModel = viewModel(
    factory = VehiclePresetViewModel.Factory(context.applicationContext as Application)
  )
  val presetEntities by presetViewModel.presetsState.collectAsStateWithLifecycle()

  var currentScreen by remember { mutableStateOf(AppScreen.SIMULATION) }

  // Active Car Config
  var currentConfig by remember { mutableStateOf(VehiclePresets.DEFAULT_PRESETS.first()) }

  // Track & Physics State
  var currentTrackType by remember { mutableStateOf(TrackType.HILL_CLIMB) }
  val track = remember(currentTrackType) { Track(currentTrackType) }
  val physics = remember(track) { VehiclePhysics(currentConfig, track) }

  // When vehicle config changes, update physics and live drivetrain
  LaunchedEffect(currentConfig) {
    physics.config = currentConfig
    physics.liveDrivetrain = currentConfig.drivetrain
  }

  // Driver Controls State
  var throttle by remember { mutableFloatStateOf(0f) }
  var brake by remember { mutableFloatStateOf(0f) }
  var handbrake by remember { mutableStateOf(false) }
  var isReverse by remember { mutableStateOf(false) }
  var isSlowMo by remember { mutableStateOf(false) }

  // Simulation Game Loop (60 FPS with Sub-stepping for maximum realism)
  var tickTrigger by remember { mutableLongStateOf(0L) }

  LaunchedEffect(currentScreen) {
    var lastTime = 0L
    while (currentScreen == AppScreen.SIMULATION) {
      withFrameNanos { now ->
        if (lastTime != 0L) {
          val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.002f, 0.04f)
          val timeScale = if (isSlowMo) 0.25f else 1.0f
          val subDt = (dt * timeScale) * 0.5f

          // Two sub-steps for stable spring-mass suspension & collisions
          physics.update(subDt, throttle, brake, handbrake, isReverse)
          physics.update(subDt, throttle, brake, handbrake, isReverse)

          tickTrigger = now
        }
        lastTime = now
      }
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    // 1. SIMULATION SCREEN
    // Simulation Canvas and HUD are kept in the composition
    if (currentScreen == AppScreen.SIMULATION) {
      // Re-trigger Canvas render every physics frame
      key(tickTrigger) {
        SimulationCanvas(
          physics = physics,
          modifier = Modifier.fillMaxSize()
        )
      }

      SimulationOverlay(
        physics = physics,
        throttle = throttle,
        onThrottleChange = { throttle = it },
        brake = brake,
        onBrakeChange = { brake = it },
        handbrake = handbrake,
        onHandbrakeChange = { handbrake = it },
        isReverse = isReverse,
        onReverseChange = { isReverse = it },
        isSlowMo = isSlowMo,
        onSlowMoToggle = { isSlowMo = !isSlowMo },
        onRepair = { physics.repairVehicle() },
        onRecoverInPlace = { physics.recoverInPlace() },
        onReset = { physics.reset() },
        onTrackSelect = { selectedTrack ->
          currentTrackType = selectedTrack
          physics.reset(Track(selectedTrack))
        },
        onOpenGarage = {
          throttle = 0f
          brake = 0f
          currentScreen = AppScreen.GARAGE
        },
        presetEntities = presetEntities,
        onSelectPreset = { newConfig ->
          currentConfig = newConfig
          physics.config = newConfig
          physics.liveDrivetrain = newConfig.drivetrain
        }
      )
    }

    // 2. GARAGE SCREEN (Automation Tuning Lab)
    AnimatedVisibility(
      visible = currentScreen == AppScreen.GARAGE,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      GarageScreen(
        currentConfig = currentConfig,
        onConfigChange = { newConfig ->
          currentConfig = newConfig
          physics.config = newConfig
        },
        presetEntities = presetEntities,
        onSavePreset = { name, config ->
          presetViewModel.saveCustomPreset(name, config)
          currentConfig = config.copy(name = name)
        },
        onUpdatePreset = { updatedConfig ->
          presetViewModel.updatePreset(updatedConfig)
          currentConfig = updatedConfig
        },
        onDeletePreset = { id ->
          presetViewModel.deletePreset(id)
        },
        onStartTestDrive = {
          physics.reset()
          currentScreen = AppScreen.SIMULATION
        }
      )
    }
  }
}

