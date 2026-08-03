package com.ipomonitor.data.repository

import android.util.Log
import com.ipomonitor.data.local.IPODao
import com.ipomonitor.data.model.*
import com.ipomonitor.data.remote.GeminiService
import com.ipomonitor.data.remote.HKEXApiService
import com.ipomonitor.util.SecurePrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "IPORepository"
private const val HKEX_PDF_BASE_URL = "https://www1.hkexnews.hk/app/"

@Singleton
class IPORepository @Inject constructor(
    private val ipoDao: IPODao,
    private val hkexApi: HKEXApiService,
    private val geminiService: GeminiService,
    private val securePrefs: SecurePrefs
) {
    // Limit concurrent analysis to 2 tasks max
    private val analysisSemaphore = Semaphore(2)

    // ============ API Key Validation ============

    suspend fun validateGeminiKey(key: String): Boolean {
        return geminiService.validateApiKey(key)
    }

    suspend fun validateOpenAIKey(key: String): Boolean {
        return geminiService.validateOpenAIKey(key)
    }

    // ============ HKEX Data Sync ============

    /**
     * Sync with HKEX - fetch all listings and store basic info.
     * Does NOT trigger analysis. Returns count of new entries.
     */
    suspend fun syncFromHKEX(): Int {
        val response = hkexApi.getActiveMainBoardApps()
        if (!response.isSuccessful) {
            throw Exception("HKEX API error: ${response.code()}")
        }

        val hkexData = response.body()
            ?: throw Exception("Empty HKEX response")

        val existingIds = ipoDao.getAllIds().toSet()
        val newRecords = hkexData.app
            .filter { it.id !in existingIds }
            .mapNotNull { entry -> parseHKEXEntry(entry) }

        if (newRecords.isNotEmpty()) {
            ipoDao.insertAll(newRecords)
        }

        Log.i(TAG, "HKEX sync: ${hkexData.app.size} total, ${newRecords.size} new")
        return newRecords.size
    }

    private fun parseHKEXEntry(entry: HKEXAppEntry): IPOEntity? {
        val submission = entry.ls.firstOrNull { sub ->
            sub.nF?.contains("申請版本") == true || sub.nS1 == "全文檔案"
        } ?: entry.ls.firstOrNull()

        val pdfRelativePath = submission?.u1 ?: return null
        val pdfUrl = "$HKEX_PDF_BASE_URL$pdfRelativePath"

        val nameZh = entry.a.split("/").firstOrNull()?.trim() ?: entry.a
        val nameEn = entry.a.split("/").getOrNull(1)?.trim() ?: ""

        return IPOEntity(
            hkexId = entry.id,
            companyNameZh = nameZh,
            companyNameEn = nameEn,
            applicationDate = entry.d,
            sponsor = "",
            industry = "",
            pdfUrl = pdfUrl,
            status = AnalysisStatus.PENDING.name
        )
    }

    // ============ Paginated Queries ============

    /** Get IPOs after a given date (recent), with optional filters */
    suspend fun getIPOsAfterDate(
        cutoffDate: String,
        searchQuery: String = "",
        month: String? = null,
        industry: String? = null
    ): List<IPOListItem> {
        return ipoDao.getIPOsAfterDate(cutoffDate, searchQuery.ifBlank { null }, month, industry)
    }

    /** Get IPOs before a given date (historical), with pagination and filters */
    suspend fun getIPOsBeforeDate(
        cutoffDate: String,
        offset: Int,
        limit: Int,
        searchQuery: String = "",
        month: String? = null,
        industry: String? = null
    ): List<IPOListItem> {
        return ipoDao.getIPOsBeforeDate(cutoffDate, limit, offset, searchQuery.ifBlank { null }, month, industry)
    }

    // ============ Filter Options ============

    suspend fun getAvailableMonths(): List<String> = ipoDao.getAllMonths()
    suspend fun getAvailableIndustries(): List<String> = ipoDao.getAllIndustries()

    // ============ Single Record ============

    fun observeRecord(hkexId: Int): Flow<IPOEntity?> = ipoDao.getByIdFlow(hkexId)

    // ============ Status Update ============

    suspend fun updateStatus(hkexId: Int, status: String) {
        ipoDao.updateStatus(hkexId, status)
    }

    // ============ Manual Analysis ============

    /**
     * Analyze a specific IPO. Uses semaphore to limit to 2 concurrent analyses.
     * Returns true on success, false on failure.
     */
    suspend fun analyzeIPO(hkexId: Int): Boolean {
        val existing = ipoDao.getById(hkexId) ?: return false

        return analysisSemaphore.withPermit {
            try {
                val provider = securePrefs.getProvider()
                val apiKey = when (provider) {
                    AIProvider.GEMINI -> securePrefs.getGeminiApiKey()
                    AIProvider.OPENAI -> securePrefs.getOpenAIApiKey()
                } ?: return@withPermit false

                val model = when (provider) {
                    AIProvider.OPENAI -> securePrefs.getOpenAIModel()?.modelId ?: "gpt-4o"
                    AIProvider.GEMINI -> securePrefs.getGeminiModel().modelId
                }

                val result = geminiService.analyzePdf(
                    pdfUrl = existing.pdfUrl,
                    companyName = existing.companyNameZh,
                    apiKey = apiKey,
                    provider = provider,
                    model = model
                )

                if (result != null) {
                    // Update entity with analysis results
                    val updated = existing.copy(
                        sponsor = result.sponsor ?: existing.sponsor,
                        industry = result.industryClassification ?: existing.industry,
                        companyBackground = result.companyBackground,
                        businessDescription = result.businessDescription,
                        registrationPlace = result.registrationPlace,
                        majorShareholders = result.majorShareholders,
                        listedElsewhere = result.listedElsewhere,
                        marketCap = result.marketCap,
                        priorFunding = result.priorFunding,
                        hkSubsidiary = result.hkSubsidiary,
                        revenueThreeYears = result.revenueThreeYears,
                        profitThreeYears = result.profitThreeYears,
                        isRefiled = result.isRefiled ?: false,
                        status = AnalysisStatus.COMPLETED.name,
                        analyzedAt = System.currentTimeMillis()
                    )
                    ipoDao.upsertRecord(updated)
                    securePrefs.incrementAnalysisCount()
                    Log.i(TAG, "Analysis success: ${existing.companyNameZh}")
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Analysis failed: ${e.message}")
                false
            }
        }
    }

    // ============ For WorkManager ============

    suspend fun checkForNewListings(): kotlin.Result<List<HKEXAppEntry>> {
        return try {
            val response = hkexApi.getActiveMainBoardApps()
            if (!response.isSuccessful) {
                return kotlin.Result.failure(Exception("HKEX API error: ${response.code()}"))
            }
            val hkexData = response.body()
                ?: return kotlin.Result.failure(Exception("Empty response"))
            val existingIds = ipoDao.getAllIds().toSet()
            val newEntries = hkexData.app.filter { it.id !in existingIds }
            kotlin.Result.success(newEntries)
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
}
