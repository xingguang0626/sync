package com.life.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object KtorClientFactory {

    /**
     * BASE_URL 由 build.gradle.kts 通过 buildConfigField 注入：
     *   debug    → http://10.0.2.2:8800 (模拟器访问 host 机器)
     *   release  → https://api.life.example.com
     * 真机调试时把 debug 的 IP 改成电脑局域网 IP 后重 build。
     */
    fun create(): HttpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = false
                encodeDefaults = true
                explicitNulls = false
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        defaultRequest {
            url(BuildConfig.BASE_URL)
            contentType(ContentType.Application.Json)
        }
        expectSuccess = true
    }
}