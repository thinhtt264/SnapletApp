package com.thinh.snaplet.di

import com.thinh.snaplet.platform.share.ShareManager
import com.thinh.snaplet.platform.share.ShareManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ShareModule {

    @Binds
    abstract fun bindShareManager(
        impl: ShareManagerImpl
    ): ShareManager
}
