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
import orinasa.njarasoa.maripanatokana.data.remote.BomApiService
import orinasa.njarasoa.maripanatokana.data.remote.EcccApiService
import orinasa.njarasoa.maripanatokana.data.remote.GdacsApiService
import orinasa.njarasoa.maripanatokana.data.remote.JmaApiService
import orinasa.njarasoa.maripanatokana.data.remote.MeteoAlarmApiService
import orinasa.njarasoa.maripanatokana.data.remote.NominatimApiService
import orinasa.njarasoa.maripanatokana.data.remote.NhcApiService
import orinasa.njarasoa.maripanatokana.data.remote.NwsApiService
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoApiService
import orinasa.njarasoa.maripanatokana.data.remote.PirateWeatherApiService
import orinasa.njarasoa.maripanatokana.data.remote.WmoSwicApiService
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
    fun provideOpenMeteoGeocodingService(okHttpClient: OkHttpClient, json: Json): orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoGeocodingService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoGeocodingService::class.java)
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
    fun provideNwsApiService(okHttpClient: OkHttpClient, json: Json): NwsApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.weather.gov/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(NwsApiService::class.java)
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

@Provides
    @Singleton
    fun providePirateWeatherApiService(okHttpClient: OkHttpClient, json: Json): PirateWeatherApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.pirateweather.net/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(PirateWeatherApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideNominatimApiService(okHttpClient: OkHttpClient, json: Json): NominatimApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(NominatimApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMeteoAlarmApiService(okHttpClient: OkHttpClient): MeteoAlarmApiService =
        Retrofit.Builder()
            .baseUrl("https://feeds.meteoalarm.org/")
            .client(okHttpClient)
            .build()
            .create(MeteoAlarmApiService::class.java)

    @Provides
    @Singleton
    fun provideJmaApiService(okHttpClient: OkHttpClient, json: Json): JmaApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://www.jma.go.jp/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(JmaApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideEcccApiService(okHttpClient: OkHttpClient, json: Json): EcccApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.weather.gc.ca/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(EcccApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideNhcApiService(okHttpClient: OkHttpClient, json: Json): NhcApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://www.nhc.noaa.gov/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(NhcApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideBomApiService(okHttpClient: OkHttpClient, json: Json): BomApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.weather.bom.gov.au/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(BomApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideWmoSwicApiService(okHttpClient: OkHttpClient, json: Json): WmoSwicApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://severe.worldweather.wmo.int/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(WmoSwicApiService::class.java)
    }
}
