package com.thinh.snaplet.di

import com.thinh.snaplet.platform.permission.PermissionManager
import com.thinh.snaplet.platform.permission.PermissionManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PermissionModule {

    @Binds
    abstract fun bindPermissionManager(
        impl: PermissionManagerImpl
    ): PermissionManager
}
