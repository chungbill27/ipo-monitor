package com.ipomonitor.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============ HKEX JSON API Response Models ============

/**
 * Root response from HKEX JSON API.
 * Endpoint: https://www1.hkexnews.hk/ncms/json/eds/appactive_app_sehk_c.json
 */
@Serializable
data class HKEXResponse(
    val genDate: Long,
    val uDate: String,
    val app: List<HKEXAppEntry>
)

/**
 * Single application entry from HKEX.
 */
@Serializable
data class HKEXAppEntry(
    val id: Int,
    val d: String,           // Date (DD/MM/YYYY)
    val a: String,           // Applicant name
    val s: String = "",      // Status
    val w: String = "",      // Warning PDF path
    val sD: Int = 0,
    val sA: Int = 0,
    val ls: List<HKEXSubmission> = emptyList(),  // Submissions list
    val ps: List<HKEXSubmission> = emptyList(),  // Previous submissions
    val hasPhip: Boolean = false,
    val postingDate: String = ""
)

/**
 * A single submission document entry.
 */
@Serializable
data class HKEXSubmission(
    val d: String = "",           // Date
    val nF: String? = null,       // Document type name (e.g., "申請版本（第一次呈交）")
    val nS1: String? = null,      // Sub-document name 1 (e.g., "全文檔案")
    val nS2: String? = null,      // Sub-document name 2 (e.g., "多檔案")
    val u1: String? = null,       // URL path 1 (PDF)
    val u2: String? = null        // URL path 2 (HTML)
)

// ============ Backend API Response Models ============

/**
 * Full IPO record response from our backend.
 */
@Serializable
data class IPORecordDto(
    val id: Int,
    @SerialName("hkex_id") val hkexId: Int,
    @SerialName("pdf_url") val pdfUrl: String,
    val status: String,
    @SerialName("error_message") val errorMessage: String? = null,

    // 15 Core Fields
    @SerialName("application_date") val applicationDate: String? = null,
    @SerialName("is_refiled") val isRefiled: Boolean = false,
    @SerialName("company_name_zh") val companyNameZh: String? = null,
    @SerialName("company_name_en") val companyNameEn: String? = null,
    @SerialName("place_of_incorporation") val placeOfIncorporation: String? = null,
    val sponsors: List<String> = emptyList(),
    @SerialName("company_background") val companyBackground: String? = null,
    @SerialName("business_summary") val businessSummary: String? = null,
    @SerialName("industry_classification") val industryClassification: String? = null,
    @SerialName("major_shareholders") val majorShareholders: List<ShareholderInfoDto> = emptyList(),
    @SerialName("listed_elsewhere") val listedElsewhere: String? = null,
    @SerialName("estimated_valuation") val estimatedValuation: String? = null,
    @SerialName("pre_ipo_investments") val preIpoInvestments: String? = null,
    @SerialName("hk_subsidiaries") val hkSubsidiaries: List<String> = emptyList(),
    val financials: List<FinancialYearDto> = emptyList()
)

@Serializable
data class ShareholderInfoDto(
    val name: String,
    val percentage: String
)

@Serializable
data class FinancialYearDto(
    val year: String,
    val revenue: String,
    @SerialName("net_profit") val netProfit: String
)

/**
 * Request to submit PDF for analysis.
 */
@Serializable
data class AnalysisRequestDto(
    @SerialName("pdf_url") val pdfUrl: String,
    @SerialName("company_name") val companyName: String,
    @SerialName("hkex_id") val hkexId: Int
)

/**
 * Response when analysis is accepted.
 */
@Serializable
data class AnalysisAcceptedDto(
    val status: String,
    @SerialName("hkex_id") val hkexId: Int,
    val message: String = ""
)

// ============ Mapping Extensions ============

/**
 * Convert backend DTO to local Room entity.
 */
fun IPORecordDto.toEntity(): IPOEntity = IPOEntity(
    hkexId = hkexId,
    pdfUrl = pdfUrl,
    status = status,
    applicationDate = applicationDate,
    isRefiled = isRefiled,
    companyNameZh = companyNameZh,
    companyNameEn = companyNameEn,
    placeOfIncorporation = placeOfIncorporation,
    sponsors = sponsors,
    companyBackground = companyBackground,
    businessSummary = businessSummary,
    industryClassification = industryClassification,
    majorShareholders = majorShareholders.map { ShareholderInfo(it.name, it.percentage) },
    listedElsewhere = listedElsewhere,
    estimatedValuation = estimatedValuation,
    preIpoInvestments = preIpoInvestments,
    hkSubsidiaries = hkSubsidiaries,
    financials = financials.map { FinancialYear(it.year, it.revenue, it.netProfit) },
    errorMessage = errorMessage,
    updatedAt = System.currentTimeMillis()
)
