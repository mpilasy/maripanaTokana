package orinasa.njarasoa.maripanatokana.ui.weather

import orinasa.njarasoa.maripanatokana.domain.model.WeatherData

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data object PermissionRequired : WeatherUiState
    data class Success(val data: WeatherData) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}
