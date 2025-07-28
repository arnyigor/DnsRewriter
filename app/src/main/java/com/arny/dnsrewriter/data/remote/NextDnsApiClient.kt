package com.arny.dnsrewriter.data.remote

import android.util.Log
import com.arny.dnsrewriter.data.remote.dto.CreateProfileResponse
import com.arny.dnsrewriter.domain.model.NextDnsProfile
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException


class NextDnsApiClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(JavaNetCookieJar(java.net.CookieManager()))
        .followRedirects(false)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    fun createAnonymousProfile(): NextDnsProfile {
        // --- ШАГ 1: "Прогрев" - заходим на страницу регистрации, чтобы получить начальные cookie ---
        Log.d("NextDnsApiClient", "--- ШАГ 1: GET /signup для инициализации сессии ---")
        val signupRequest = Request.Builder()
            .url("https://my.nextdns.io/signup")
            .headers(getBrowserHeaders())
            .build()

        client.newCall(signupRequest).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Шаг 1: Не удалось получить страницу signup. Код: ${response.code}")
            // CookieJar автоматически сохранил все, что прислал сервер.
            Log.d("NextDnsApiClient", "Шаг 1: Сессия инициализирована, cookie сохранены в CookieJar.")
        }

        // --- ШАГ 2: Создание профиля ---
        Log.d("NextDnsApiClient", "--- ШАГ 2: POST-запрос на /accounts для создания профиля ---")
        val requestBody = "".toRequestBody("application/json".toMediaType())
        val createProfileRequest = Request.Builder()
            .url("https://api.nextdns.io/accounts")
            .post(requestBody)
            .headers(getBrowserHeaders(isApi = true)) // Используем API-заголовки
            .build()

        client.newCall(createProfileRequest).execute().use { response ->
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val profileResponse = json.decodeFromString<CreateProfileResponse>(responseBody)
                    val configId = profileResponse.data.id

                    val cookies = client.cookieJar.loadForRequest("https://api.nextdns.io/".toHttpUrl())
                    val cookiesString = cookies.joinToString(separator = "; ") { it.toString() }

                    Log.d("NextDnsApiClient", "УСПЕХ! Создан профиль: ID=$configId")
                    return NextDnsProfile(configId, cookiesString)
                }
            }

            val errorBody = response.body?.string() ?: "Тело ответа пустое"
            throw IOException("Шаг 2: Не удалось создать профиль. Код: ${response.code}. Тело: $errorBody")
        }
    }

    // Вспомогательная функция для генерации заголовков
    private fun getBrowserHeaders(isApi: Boolean = false): Headers {
        val builder = Headers.Builder()
            .add(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
            )
            .add("Accept-Language", "en-US,en;q=0.9,ru;q=0.8")
            .add(
                "Sec-Ch-Ua",
                "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\""
            )
            .add("Sec-Ch-Ua-Mobile", "?0")
            .add("Sec-Ch-Ua-Platform", "\"Windows\"")

        if (isApi) {
            builder.add("Accept", "application/json, text/plain, */*")
            builder.add("Origin", "https://my.nextdns.io")
            builder.add("Referer", "https://my.nextdns.io/")
            builder.add("Sec-Fetch-Dest", "empty")
            builder.add("Sec-Fetch-Mode", "cors")
            builder.add("Sec-Fetch-Site", "same-site")
        } else {
            builder.add(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
            )
            builder.add("Sec-Fetch-Dest", "document")
            builder.add("Sec-Fetch-Mode", "navigate")
            builder.add("Sec-Fetch-Site", "none")
            builder.add("Sec-Fetch-User", "?1")
            builder.add("Upgrade-Insecure-Requests", "1")
        }
        return builder.build()
    }
}