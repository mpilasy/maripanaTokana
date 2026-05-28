package orinasa.njarasoa.maripanatokana.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import orinasa.njarasoa.maripanatokana.data.repository.LocationRepositoryImpl
import orinasa.njarasoa.maripanatokana.domain.repository.LocationRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommonLocationModule

@Module
@InstallIn(SingletonComponent::class)
abstract class CommonLocationBindingModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: LocationRepositoryImpl
    ): LocationRepository
}
