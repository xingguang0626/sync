package com.life.app.di

// 切回 Mock 数据时取消注释下面的 import
// import com.life.app.data.mock.MockApi
import com.life.app.data.remote.HomeApi
import com.life.app.data.remote.KtorClientFactory
import com.life.app.data.remote.KtorHomeApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @Named("homeApi")
    fun provideHomeApi(client: HttpClient): HomeApi = KtorHomeApi(client)

    // 切回 Mock 数据时取消注释下面这行，并注释掉上面 KtorHomeApi 那行
    // @Provides
    // @Singleton
    // @Named("homeApi")
    // fun provideHomeApi(): HomeApi = MockApi()

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = KtorClientFactory.create()
}