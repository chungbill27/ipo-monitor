package com.ipomonitor.data.remote

import android.util.Base64
import android.util.Log
import com.ipomonitor.data.model.AIProvider
import com.ipomonitor.data.model.IPOAnalysisResult
import com.ipomonitor.data.model.OpenAIModel
import com.ipomonitor.util.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GeminiService"
private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
private const val OPENAI_BASE_URL = "https://api.openai.com/v1"

/**
 * Unified AI service supporting both Gemini and OpenAI for PDF analysis.
 * Gemini: native PDF support (send base64 directly)
 * OpenAI: send PDF as base64 file data to GPT-4o vision
 */
@Singleton
class GeminiService @Inject constructor(
    private val securePrefs: SecurePrefs,
    private val okHttpClient: OkHttpClient
) {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // ============ Validation ============

    /**
     * Validate Gemini API key (uses models list endpoint - no quota consumed).
     */
    suspend fun validateApiKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$GEMINI_BASE_URL/models?key=$apiKey"
            val request = Request.Builder().url(url).get().build()
            val response = okHttpClient.newCall(request).execute()
            val isValid = response.isSuccessful
            response.close()
            Log.i(TAG, "Gemini key validation: $isValid")
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Gemini key validation failed", e)
            false
        }
    }

    /**
     * Validate OpenAI API key.
     */
    suspend fun validateOpenAIKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$OPENAI_BASE_URL/models"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            val response = okHttpClient.newCall(request).execute()
            val isValid = response.isSuccessful
            response.close()
            Log.i(TAG, "OpenAI key validation: $isValid")
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "OpenAI key validation failed", e)
            false
        }
    }

    // ============ Analysis (called from Repository) ============

    /**
     * Analyze PDF with specified provider and model.
     * Returns IPOAnalysisResult on success, null on failure.
     */
    suspend fun analyzePdf(
        pdfUrl: String,
        companyName: String,
        apiKey: String,
        provider: AIProvider,
        model: String
    ): IPOAnalysisResult? {
        return when (provider) {
            AIProvider.GEMINI -> analyzeWithGemini(pdfUrl, companyName, apiKey, model)
            AIProvider.OPENAI -> analyzeWithOpenAI(pdfUrl, companyName, apiKey, model)
        }
    }

    // ============ Gemini Analysis ============

    private suspend fun analyzeWithGemini(
        pdfUrl: String,
        companyName: String,
        apiKey: String,
        model: String
    ): IPOAnalysisResult? = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Downloading PDF for Gemini: $pdfUrl")
            val pdfBytes = downloadPdf(pdfUrl) ?: return@withContext null
            Log.i(TAG, "PDF downloaded: ${pdfBytes.size / 1024}KB")

            val pdfBase64 = Base64.encodeToString(pdfBytes, Base64.NO_WRAP)
            val requestBody = buildGeminiRequest(pdfBase64, companyName)

            val url = "$GEMINI_BASE_URL/models/$model:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody.toString().toRequestBody(jsonMediaType))
                .build()

            Log.i(TAG, "Sending to Gemini ($model)...")
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                response.close()
                Log.e(TAG, "Gemini API error (${response.code}): ${parseErrorMessage(errorBody)}")
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            response.close()

            val extractedText = extractGeminiText(responseBody) ?: return@withContext null
            jsonParser.decodeFromString<IPOAnalysisResult>(cleanJsonText(extractedText))
        } catch (e: Exception) {
            Log.e(TAG, "Gemini analysis failed", e)
            null
        }
    }

    // ============ OpenAI Analysis ============

    private suspend fun analyzeWithOpenAI(
        pdfUrl: String,
        companyName: String,
        apiKey: String,
        model: String
    ): IPOAnalysisResult? = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Downloading PDF for OpenAI: $pdfUrl")
            val pdfBytes = downloadPdf(pdfUrl) ?: return@withContext null
            Log.i(TAG, "PDF downloaded: ${pdfBytes.size / 1024}KB")

            val pdfBase64 = Base64.encodeToString(pdfBytes, Base64.NO_WRAP)
            val requestBody = buildOpenAIRequest(pdfBase64, companyName, model)

            val url = "$OPENAI_BASE_URL/chat/completions"
            val request = Request.Builder()
                .url(url)
                .post(requestBody.toString().toRequestBody(jsonMediaType))
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            Log.i(TAG, "Sending to OpenAI ($model)...")
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                response.close()
                Log.e(TAG, "OpenAI API error (${response.code}): ${parseErrorMessage(errorBody)}")
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            response.close()

            val extractedText = extractOpenAIText(responseBody) ?: return@withContext null
            jsonParser.decodeFromString<IPOAnalysisResult>(cleanJsonText(extractedText))
        } catch (e: Exception) {
            Log.e(TAG, "OpenAI analysis failed", e)
            null
        }
    }

    // ============ Request Builders ============

    private fun buildGeminiRequest(pdfBase64: String, companyName: String): JSONObject {
        return JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "application/pdf")
                                put("data", pdfBase64)
                            })
                        })
                        put(JSONObject().put("text", buildPrompt(companyName)))
                    })
                }
            ))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("topP", 0.95)
                put("maxOutputTokens", 8192)
                put("responseMimeType", "application/json")
            })
        }
    }

    private fun buildOpenAIRequest(pdfBase64: String, companyName: String, model: String): JSONObject {
        return JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "你是一位專業的香港 IPO 招股書分析師。請嚴格按照用戶要求的 JSON 格式輸出結果，不要加任何 markdown 標記。")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "file")
                            put("file", JSONObject().apply {
                                put("filename", "prospectus.pdf")
                                put("file_data", "data:application/pdf;base64,$pdfBase64")
                            })
                        })
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", buildPrompt(companyName))
                        })
                    })
                })
            })
            put("temperature", 0.1)
            put("max_tokens", 8192)
        }
    }

    // ============ Response Parsers ============

    private fun extractGeminiText(responseBody: String): String? {
        return try {
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.getJSONArray("candidates")
            if (candidates.length() > 0) {
                candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract Gemini text", e)
            null
        }
    }

    private fun extractOpenAIText(responseBody: String): String? {
        return try {
            val jsonResponse = JSONObject(responseBody)
            jsonResponse.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract OpenAI text", e)
            null
        }
    }

    // ============ Helpers ============

    private fun cleanJsonText(text: String): String {
        return text
            .replace("```json", "")
            .replace("```", "")
            .trim()
    }

    private fun parseErrorMessage(errorBody: String): String {
        return try {
            val errorJson = JSONObject(errorBody)
            errorJson.getJSONObject("error").getString("message")
        } catch (e: Exception) {
            errorBody.take(200)
        }
    }

    private fun downloadPdf(url: String): ByteArray? {
        return try {
            val fullUrl = if (url.startsWith("http")) url
            else "https://www1.hkexnews.hk$url"
            val request = Request.Builder().url(fullUrl).build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.bytes()
            } else {
                Log.e(TAG, "PDF download failed: ${response.code}")
                response.close()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "PDF download error", e)
            null
        }
    }

    private fun buildPrompt(companyName: String): String = """
你是一位專業的香港 IPO 招股書分析師。請仔細閱讀上方的招股書 PDF 文件（公司名稱參考：$companyName），並精確提取以下 15 個核心欄位。

【輸出要求】
- 必須以 JSON 格式回覆，嚴格遵循下方 schema
- 若某欄位在文件中確實找不到資訊，填寫 "N/A"
- 金額請保留原文幣種和單位（如「人民幣 5.2 億元」）
- 股東持股比例請標註百分比
- 營收和盈利數據請按年份列出

【JSON Schema】
{
  "application_date": "入表日期（格式：YYYY-MM-DD）",
  "is_refiled": false,
  "company_name_zh": "公司中文名稱",
  "company_name_en": "公司英文名稱",
  "place_of_incorporation": "公司註冊地",
  "sponsors": "保薦人（多個以逗號分隔）",
  "company_background": "公司背景簡介（100字內）",
  "business_summary": "公司業務簡介（150字內）",
  "industry_classification": "行業歸類",
  "major_shareholders": "主要股東（名稱及持股比例）",
  "listed_elsewhere": "有沒有在其他地方已上市（交易所名稱或 N/A）",
  "estimated_valuation": "公司市值/估值",
  "pre_ipo_investments": "前期融資（金額及投資人）",
  "hk_subsidiaries": "香港子公司名稱（多個以逗號分隔）",
  "revenue_three_years": "過去3年營收（按年份列出）",
  "profit_three_years": "過去3年稅後盈利（按年份列出）"
}

【注意事項】
1. revenue_three_years 和 profit_three_years 必須包含最近 3 個財政年度的數據
2. major_shareholders 列出持股 5% 以上的股東
3. sponsors 可能有多個，全部列出
4. 若為再次入表，is_refiled 設為 true
5. 只回覆 JSON，不要加任何解釋文字
""".trimIndent()
}
