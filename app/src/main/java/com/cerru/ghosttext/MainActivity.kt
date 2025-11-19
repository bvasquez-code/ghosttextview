package com.cerru.ghosttext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cerru.ghosttext.data.LocalStorage
import com.cerru.ghosttext.service.GhostTextApi
import com.cerru.ghosttext.service.GhostTextService
import com.cerru.ghosttext.ui.navigation.GhostTextApp
import com.cerru.ghosttext.ui.theme.GhostTextViewTheme
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://localhost:7070/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        val api = retrofit.create(GhostTextApi::class.java)
        val service = GhostTextService(api)
        val storage = LocalStorage(this)

        setContent {
            GhostTextViewTheme {
                GhostTextApp(storage = storage, service = service)
            }
        }
    }
}
