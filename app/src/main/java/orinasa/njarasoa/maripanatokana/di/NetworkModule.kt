package orinasa.njarasoa.maripanatokana.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import orinasa.njarasoa.maripanatokana.BuildConfig
import orinasa.njarasoa.maripanatokana.data.remote.GdacsApiService
import orinasa.njarasoa.maripanatokana.data.remote.WeatherApiAlertsService
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoApiService
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val okHttpClientBuilder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            okHttpClientBuilder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
        }

        return okHttpClientBuilder.build()
    }

    @Provides
    @Singleton
    fun provideOpenMeteoApiService(okHttpClient: OkHttpClient, json: Json): OpenMeteoApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(OpenMeteoApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideWeatherApiAlertsService(okHttpClient: OkHttpClient, json: Json): WeatherApiAlertsService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.weatherapi.com/v1/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(WeatherApiAlertsService::class.java)
    }

    @Provides
    @Singleton
    fun provideGdacsApiService(okHttpClient: OkHttpClient, json: Json): GdacsApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://www.gdacs.org/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(GdacsApiService::class.java)
    }
}
