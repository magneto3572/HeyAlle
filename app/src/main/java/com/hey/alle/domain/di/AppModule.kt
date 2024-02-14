package com.hey.alle.domain.di

import com.hey.alle.presentation.MyApplication
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineExceptionHandler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun exceptionHandler() : CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, exception ->
            exception.printStackTrace()
        }
    }

    @Provides
    fun getApplication() : MyApplication {
        return MyApplication()
    }
}