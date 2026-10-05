package orinasa.njarasoa.maripanatokana.ui.weather

import androidx.annotation.StringRes
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.domain.model.FetchError
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data object PermissionRequired : WeatherUiState
    data class Success(val data: WeatherData) : WeatherUiState
    data class Error(@StringRes val messageResId: Int) : WeatherUiState
}

@get:StringRes
val FetchError.messageResId: Int
    get() = when (this) {
        FetchError.Offline -> R.string.error_offline
        FetchError.Timeout -> R.string.error_timeout
        FetchError.RateLimited -> R.string.error_rate_limited
        FetchError.Server -> R.string.error_server
        FetchError.InvalidApiKey -> R.string.error_invalid_api_key
        FetchError.Generic -> R.string.error_fetch_weather
    }
