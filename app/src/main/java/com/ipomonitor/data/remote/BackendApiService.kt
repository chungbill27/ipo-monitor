package com.ipomonitor.data.remote

import com.ipomonitor.data.model.AnalysisAcceptedDto
import com.ipomonitor.data.model.AnalysisRequestDto
import com.ipomonitor.data.model.IPORecordDto
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit service interface for our Python FastAPI backend.
 */
interface BackendApiService {

    /**
     * Submit a new IPO prospectus for AI analysis.
     * Backend will process asynchronously and send FCM notification when done.
     */
    @POST("api/analyze")
    suspend fun requestAnalysis(
        @Body request: AnalysisRequestDto
    ): Response<AnalysisAcceptedDto>

    /**
     * Retry a failed or timed-out analysis.
     */
    @POST("api/retry/{hkexId}")
    suspend fun retryAnalysis(
        @Path("hkexId") hkexId: Int
    ): Response<AnalysisAcceptedDto>

    /**
     * Get full details of a specific IPO record (all 15 fields).
     * Called after receiving FCM notification.
     */
    @GET("api/records/{hkexId}")
    suspend fun getRecord(
        @Path("hkexId") hkexId: Int
    ): Response<IPORecordDto>

    /**
     * List all IPO records with optional filtering.
     */
    @GET("api/records")
    suspend fun listRecords(
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<List<IPORecordDto>>

    /**
     * Register device token for FCM notifications.
     */
    @POST("api/register-device")
    suspend fun registerDevice(
        @Body body: Map<String, String>
    ): Response<Map<String, String>>
}
