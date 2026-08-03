package com.ipomonitor.di

import android.content.Context
import androidx.room.Room
import com.ipomonitor.data.local.IPODao
import com.ipomonitor.data.local.IPODatabase
import com.ipomonitor.data.remote.GeminiService
import com.ipomonitor.data.remote.HKEXApiService
import com.ipomonitor.util.SecurePrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)  // PDF analysis can take time
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideHKEXApiService(okHttpClient: OkHttpClient, json: Json): HKEXApiService {
        return Retrofit.Builder()
            .baseUrl("https://www1.hkexnews.hk/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(HKEXApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideGeminiService(securePrefs: SecurePrefs, okHttpClient: OkHttpClient): GeminiService {
        return GeminiService(securePrefs, okHttpClient)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IPODatabase {
        return Room.databaseBuilder(
            context,
            IPODatabase::class.java,
            "ipo_monitor.db"
        )
            .addMigrations(
                IPODatabase.MIGRATION_1_2,
                IPODatabase.MIGRATION_2_3,
                IPODatabase.MIGRATION_3_4
            )
            .fallbackToDestructiveMigration()  // Fallback for edge cases only
            .build()
    }

    @Provides
    @Singleton
    fun provideIPODao(database: IPODatabase): IPODao {
        return database.ipoDao()
    }
}
