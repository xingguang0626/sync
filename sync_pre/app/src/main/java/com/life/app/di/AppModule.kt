package com.life.app.di

import com.life.app.data.mock.MockApi
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

    /**
     * 第一阶段：绑 MockApi（返回假数据）
     * 第二阶段切真后端时，把下面 @Provides 注释掉，改用 KtorHomeApi 即可。
     */
    @Provides
    @Singleton
    @Named("homeApi")
    fun provideHomeApi(): HomeApi = MockApi()

    // ⚠️ 第二阶段启用：把上面这行注释掉，改成下面这段
    // @Provides
    // @Singleton
    // @Named("homeApi")
    // fun provideHomeApi(client: HttpClient): HomeApi = KtorHomeApi(client)

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = KtorClientFactory.create()
}