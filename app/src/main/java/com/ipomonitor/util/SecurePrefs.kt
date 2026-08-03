package com.ipomonitor.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ipomonitor.data.model.AIProvider
import com.ipomonitor.data.model.GeminiModel
import com.ipomonitor.data.model.OpenAIModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Check frequency options for WorkManager scheduling.
 */
enum class CheckFrequency(val minutes: Long, val displayName: String) {
    MINUTES_15(15, "每 15 分鐘"),
    MINUTES_30(30, "每 30 分鐘"),
    HOURS_1(60, "每 1 小時"),
    HOURS_3(180, "每 3 小時"),
    MANUAL(0, "手動檢查");

    companion object {
        fun fromMinutes(minutes: Long): CheckFrequency {
            return entries.find { it.minutes == minutes } ?: HOURS_1
        }
    }
}

/**
 * Secure storage for sensitive data (API keys, preferences) using EncryptedSharedPreferences.
 * Supports multiple AI providers: Gemini and OpenAI.
 */
@Singleton
class SecurePrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "ipo_monitor_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_OPENAI_API_KEY = "openai_api_key"
        private const val KEY_AI_PROVIDER = "ai_provider"
        private const val KEY_GEMINI_MODEL = "gemini_model"
        private const val KEY_OPENAI_MODEL = "openai_model"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_LAST_CHECK_TIME = "last_check_time"
        private const val KEY_ANALYSIS_COUNT = "analysis_count"
        private const val KEY_CHECK_FREQUENCY = "check_frequency"
        private const val KEY_WORK_HOURS_ONLY = "work_hours_only"
    }

    // ============ AI Provider ============

    fun setProvider(provider: AIProvider) {
        prefs.edit().putString(KEY_AI_PROVIDER, provider.name).apply()
    }

    fun getProvider(): AIProvider {
        val name = prefs.getString(KEY_AI_PROVIDER, AIProvider.GEMINI.name)
        return try { AIProvider.valueOf(name!!) } catch (e: Exception) { AIProvider.GEMINI }
    }

    // ============ Gemini ============

    fun setGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, key).apply()
    }

    fun getGeminiApiKey(): String? {
        return prefs.getString(KEY_GEMINI_API_KEY, null)
    }

    fun setGeminiModel(model: GeminiModel) {
        prefs.edit().putString(KEY_GEMINI_MODEL, model.name).apply()
    }

    fun getGeminiModel(): GeminiModel {
        val name = prefs.getString(KEY_GEMINI_MODEL, null) ?: return GeminiModel.GEMINI_35_FLASH
        return try { GeminiModel.valueOf(name) } catch (e: Exception) { GeminiModel.GEMINI_35_FLASH }
    }

    // ============ OpenAI ============

    fun setOpenAIApiKey(key: String) {
        prefs.edit().putString(KEY_OPENAI_API_KEY, key).apply()
    }

    fun getOpenAIApiKey(): String? {
        return prefs.getString(KEY_OPENAI_API_KEY, null)
    }

    fun setOpenAIModel(model: OpenAIModel) {
        prefs.edit().putString(KEY_OPENAI_MODEL, model.name).apply()
    }

    fun getOpenAIModel(): OpenAIModel? {
        val name = prefs.getString(KEY_OPENAI_MODEL, null) ?: return null
        return try { OpenAIModel.valueOf(name) } catch (e: Exception) { OpenAIModel.GPT_4O }
    }

    // ============ Setup State ============

    fun hasApiKey(): Boolean {
        return when (getProvider()) {
            AIProvider.GEMINI -> !getGeminiApiKey().isNullOrBlank()
            AIProvider.OPENAI -> !getOpenAIApiKey().isNullOrBlank()
        }
    }

    fun setSetupComplete(complete: Boolean) {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, complete).apply()
    }

    fun isSetupComplete(): Boolean {
        return prefs.getBoolean(KEY_SETUP_COMPLETE, false)
    }

    // ============ Monitoring Settings ============

    fun setCheckFrequency(frequency: CheckFrequency) {
        prefs.edit().putLong(KEY_CHECK_FREQUENCY, frequency.minutes).apply()
    }

    fun getCheckFrequency(): CheckFrequency {
        val minutes = prefs.getLong(KEY_CHECK_FREQUENCY, CheckFrequency.HOURS_1.minutes)
        return CheckFrequency.fromMinutes(minutes)
    }

    fun setWorkHoursOnly(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WORK_HOURS_ONLY, enabled).apply()
    }

    fun isWorkHoursOnly(): Boolean {
        return prefs.getBoolean(KEY_WORK_HOURS_ONLY, false)
    }

    fun setLastCheckTime(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_CHECK_TIME, timestamp).apply()
    }

    fun getLastCheckTime(): Long {
        return prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
    }

    // ============ Usage Tracking ============

    fun incrementAnalysisCount() {
        val current = prefs.getInt(KEY_ANALYSIS_COUNT, 0)
        prefs.edit().putInt(KEY_ANALYSIS_COUNT, current + 1).apply()
    }

    fun getAnalysisCount(): Int {
        return prefs.getInt(KEY_ANALYSIS_COUNT, 0)
    }

    fun resetAnalysisCount() {
        prefs.edit().putInt(KEY_ANALYSIS_COUNT, 0).apply()
    }

    // ============ Reset ============

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
