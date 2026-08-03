package com.ipomonitor.data.remote

import com.ipomonitor.data.model.HKEXResponse
import retrofit2.Response
import retrofit2.http.GET

/**
 * Retrofit service for HKEX public JSON API.
 * No authentication required.
 */
interface HKEXApiService {

    @GET("ncms/json/eds/appactive_app_sehk_c.json")
    suspend fun getActiveMainBoardApps(): Response<HKEXResponse>

    @GET("ncms/json/eds/appactive_app_gem_c.json")
    suspend fun getActiveGEMApps(): Response<HKEXResponse>
}
