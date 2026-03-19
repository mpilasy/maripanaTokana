package orinasa.njarasoa.maripanatokana.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import orinasa.njarasoa.maripanatokana.data.location.LocationProvider
import orinasa.njarasoa.maripanatokana.data.location.NativeLocationProvider
import orinasa.njarasoa.maripanatokana.ui.permission.PermissionHandler
import orinasa.njarasoa.maripanatokana.ui.permission.FDroidPermissionHandler
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
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationBindingModule {

    @Binds
    @Singleton
    abstract fun bindPermissionHandler(
        impl: FDroidPermissionHandler
    ): PermissionHandler
}
