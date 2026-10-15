package com.cloudy.ai

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.ByteArrayOutputStream

interface CloudApi {
    @Multipart
    @POST
    suspend fun translateImage(
        @Url url: String,
        @Part image: MultipartBody.Part,
        @Part("target") target: RequestBody,
        @Header("Authorization") apiKey: String
    ): TranslateResponse
}

data class TranslateResponse(val translatedText: String)

class CloudTranslationService(private val baseUrl: String, private val apiKey: String) {
    private val api: CloudApi

    init {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BASIC
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(CloudApi::class.java)
    }

    suspend fun translateBitmap(bitmap: Bitmap, targetLang: String = "ar"): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.JPEG, 80, baos)
        val bytes = baos.toByteArray()

        val reqFile = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", "capture.jpg", reqFile)
        val targetBody = targetLang.toRequestBody("text/plain".toMediaTypeOrNull())

        val response = api.translateImage(baseUrl, body, targetBody, "Bearer $apiKey")
        return response.translatedText
    }
}
