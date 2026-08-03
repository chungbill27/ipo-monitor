package com.ipomonitor.data.model

import androidx.room.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

// ============ Enums ============

/**
 * Analysis status for each IPO record.
 */
enum class AnalysisStatus(val displayName: String) {
    PENDING("待分析"),
    QUEUED("排隊中"),
    ANALYZING("分析中"),
    COMPLETED("已完成"),
    FAILED("失敗")
}

/**
 * AI model provider.
 */
enum class AIProvider(val displayName: String) {
    GEMINI("Google Gemini"),
    OPENAI("OpenAI GPT")
}

/**
 * Gemini model choices.
 */
enum class GeminiModel(val modelId: String, val displayName: String) {
    GEMINI_35_FLASH("gemini-3.5-flash", "Gemini 3.5 Flash"),
    GEMINI_35_FLASH_LITE("gemini-3.5-flash-lite", "Gemini 3.5 Flash Lite"),
    GEMINI_31_FLASH_LITE("gemini-3.1-flash-lite", "Gemini 3.1 Flash Lite"),
    GEMINI_3_FLASH("gemini-3-flash-preview", "Gemini 3 Flash")
}

/**
 * OpenAI model choices.
 */
enum class OpenAIModel(val modelId: String, val displayName: String) {
    GPT_4O("gpt-4o", "GPT-4o"),
    GPT_4O_MINI("gpt-4o-mini", "GPT-4o Mini")
}

// ============ Room Entity ============

@Entity(tableName = "ipo_records")
data class IPOEntity(
    @PrimaryKey
    val hkexId: Int,

    // === Basic info from HKEX JSON (always available, no AI needed) ===
    val companyNameZh: String = "",
    val companyNameEn: String = "",
    val applicationDate: String = "",      // 入表日期 from HKEX
    val sponsor: String = "",              // 保薦人 from HKEX
    val industry: String = "",             // 港交所原始行業分類
    val pdfUrl: String = "",               // 招股書 PDF URL
    val pdfValid: Boolean = true,          // PDF 連結是否有效

    // === Analysis status ===
    val status: String = "PENDING",        // AnalysisStatus name
    val errorMessage: String? = null,

    // === AI Analysis results (populated only after manual trigger) ===
    val isRefiled: Boolean = false,
    val registrationPlace: String? = null,
    val companyBackground: String? = null,
    val businessDescription: String? = null,
    val industryClassification: String? = null,  // AI precise classification
    val majorShareholders: String? = null,
    val listedElsewhere: String? = null,
    val marketCap: String? = null,
    val priorFunding: String? = null,
    val hkSubsidiary: String? = null,
    val revenueThreeYears: String? = null,
    val profitThreeYears: String? = null,

    // === Metadata ===
    val createdAt: Long = System.currentTimeMillis(),
    val analyzedAt: Long? = null,
    val analyzedBy: String? = null               // "gemini" or "openai-gpt-4o"
) {
    fun getAnalysisStatus(): AnalysisStatus =
        try { AnalysisStatus.valueOf(status) } catch (e: Exception) { AnalysisStatus.PENDING }
}

// ============ List Item (lightweight for display) ============

data class IPOListItem(
    val hkexId: Int,
    val companyNameZh: String,
    val companyNameEn: String,
    val applicationDate: String,
    val sponsor: String,
    val industry: String,
    val status: String,
    val createdAt: Long
) {
    fun getAnalysisStatus(): AnalysisStatus =
        try { AnalysisStatus.valueOf(status) } catch (e: Exception) { AnalysisStatus.PENDING }
}

// ============ AI Analysis Result (matches Prompt output) ============

@Serializable
data class IPOAnalysisResult(
    @SerialName("application_date")
    val applicationDate: String? = null,
    @SerialName("is_refiled")
    val isRefiled: Boolean? = false,
    @SerialName("company_name_zh")
    val companyNameZh: String? = null,
    @SerialName("company_name_en")
    val companyNameEn: String? = null,
    @SerialName("place_of_incorporation")
    val registrationPlace: String? = null,
    @SerialName("sponsors")
    val sponsor: String? = null,
    @SerialName("company_background")
    val companyBackground: String? = null,
    @SerialName("business_summary")
    val businessDescription: String? = null,
    @SerialName("industry_classification")
    val industryClassification: String? = null,
    @SerialName("major_shareholders")
    val majorShareholders: String? = null,
    @SerialName("listed_elsewhere")
    val listedElsewhere: String? = null,
    @SerialName("estimated_valuation")
    val marketCap: String? = null,
    @SerialName("pre_ipo_investments")
    val priorFunding: String? = null,
    @SerialName("hk_subsidiaries")
    val hkSubsidiary: String? = null,
    @SerialName("revenue_three_years")
    val revenueThreeYears: String? = null,
    @SerialName("profit_three_years")
    val profitThreeYears: String? = null
)

// ============ HKEX JSON API Models ============

@Serializable
data class HKEXResponse(
    val app: List<HKEXAppEntry> = emptyList()
)

@Serializable
data class HKEXAppEntry(
    val id: Int = 0,
    val a: String = "",    // Company name
    val d: String = "",    // Application date
    val ls: List<HKEXSubmission> = emptyList()
)

@Serializable
data class HKEXSubmission(
    val u1: String? = null,
    val nF: String? = null,
    val nS1: String? = null
)
