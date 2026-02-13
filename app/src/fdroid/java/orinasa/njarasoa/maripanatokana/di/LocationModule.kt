package orinasa.njarasoa.maripanatokana.di

import android.content.Context
import android.location.Geocoder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import orinasa.njarasoa.maripanatokana.data.location.LocationProvider
import orinasa.njarasoa.maripanatokana.data.location.NativeLocationProvider
import orinasa.njarasoa.maripanatokana.data.repository.LocationRepositoryImpl
import orinasa.njarasoa.maripanatokana.domain.repository.LocationRepository
import java.util.Locale
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Provides
    @Singleton
    fun provideLocationProvider(
        @ApplicationContext context: Context,
    ): LocationProvider {
        return NativeLocationProvider(context)
    }

    @Provides
    @Singleton
    fun provideGeocoder(
        @ApplicationContext context: Context
    ): Geocoder {
        return Geocoder(context, Locale.getDefault())
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationBindingModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: LocationRepositoryImpl
    ): LocationRepository
}
