package com.ipomonitor.data.local

import androidx.room.*
import com.ipomonitor.data.model.IPOEntity
import com.ipomonitor.data.model.IPOListItem
import kotlinx.coroutines.flow.Flow

@Dao
interface IPODao {

    // ============ Insert / Update ============

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(records: List<IPOEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: IPOEntity)

    @Upsert
    suspend fun upsertRecord(record: IPOEntity)

    @Update
    suspend fun update(record: IPOEntity)

    @Query("UPDATE ipo_records SET status = :status, errorMessage = :error WHERE hkexId = :id")
    suspend fun updateStatus(id: Int, status: String, error: String? = null)

    // ============ Recent IPOs (after cutoff date, with filters) ============
    // HKEX date format is DD/MM/YYYY. We convert to YYYYMMDD for proper comparison.
    // substr(applicationDate,7,4) || substr(applicationDate,4,2) || substr(applicationDate,1,2)
    // converts "31/05/2026" -> "20260531"

    @Query("""
        SELECT hkexId, companyNameZh, companyNameEn, applicationDate, 
               sponsor, industry, status, createdAt 
        FROM ipo_records 
        WHERE (substr(applicationDate,7,4) || substr(applicationDate,4,2) || substr(applicationDate,1,2)) >= :cutoffDateNormalized
        AND (:searchQuery IS NULL OR companyNameZh LIKE '%' || :searchQuery || '%' 
             OR companyNameEn LIKE '%' || :searchQuery || '%'
             OR sponsor LIKE '%' || :searchQuery || '%')
        AND (:month IS NULL OR substr(applicationDate,4,2) || '/' || substr(applicationDate,7,4) = :month)
        AND (:industry IS NULL OR industry = :industry OR industryClassification = :industry)
        ORDER BY (substr(applicationDate,7,4) || substr(applicationDate,4,2) || substr(applicationDate,1,2)) DESC
    """)
    suspend fun getIPOsAfterDate(
        cutoffDateNormalized: String,
        searchQuery: String? = null,
        month: String? = null,
        industry: String? = null
    ): List<IPOListItem>

    // ============ Historical IPOs (before cutoff date, paginated with filters) ============

    @Query("""
        SELECT hkexId, companyNameZh, companyNameEn, applicationDate, 
               sponsor, industry, status, createdAt 
        FROM ipo_records 
        WHERE (substr(applicationDate,7,4) || substr(applicationDate,4,2) || substr(applicationDate,1,2)) < :cutoffDateNormalized
        AND (:searchQuery IS NULL OR companyNameZh LIKE '%' || :searchQuery || '%' 
             OR companyNameEn LIKE '%' || :searchQuery || '%'
             OR sponsor LIKE '%' || :searchQuery || '%')
        AND (:month IS NULL OR substr(applicationDate,4,2) || '/' || substr(applicationDate,7,4) = :month)
        AND (:industry IS NULL OR industry = :industry OR industryClassification = :industry)
        ORDER BY (substr(applicationDate,7,4) || substr(applicationDate,4,2) || substr(applicationDate,1,2)) DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getIPOsBeforeDate(
        cutoffDateNormalized: String,
        limit: Int,
        offset: Int,
        searchQuery: String? = null,
        month: String? = null,
        industry: String? = null
    ): List<IPOListItem>

    // ============ Single Record ============

    @Query("SELECT * FROM ipo_records WHERE hkexId = :id")
    suspend fun getById(id: Int): IPOEntity?

    @Query("SELECT * FROM ipo_records WHERE hkexId = :id")
    fun getByIdFlow(id: Int): Flow<IPOEntity?>

    // ============ All IDs (for sync) ============

    @Query("SELECT hkexId FROM ipo_records")
    suspend fun getAllIds(): List<Int>

    // ============ Metadata Queries (for filter options) ============
    // Industry: from both HKEX raw and AI classification
    @Query("""
        SELECT DISTINCT COALESCE(NULLIF(industryClassification, ''), industry) as ind 
        FROM ipo_records 
        WHERE (industry != '' AND industry IS NOT NULL) 
           OR (industryClassification != '' AND industryClassification IS NOT NULL)
        ORDER BY ind
    """)
    suspend fun getAllIndustries(): List<String>

    // Month: extract MM/YYYY from DD/MM/YYYY format
    @Query("""
        SELECT DISTINCT substr(applicationDate,4,2) || '/' || substr(applicationDate,7,4) as month 
        FROM ipo_records 
        WHERE applicationDate != '' AND applicationDate IS NOT NULL AND length(applicationDate) = 10
        ORDER BY substr(applicationDate,7,4) DESC, substr(applicationDate,4,2) DESC
    """)
    suspend fun getAllMonths(): List<String>

    @Query("SELECT COUNT(*) FROM ipo_records")
    suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM ipo_records WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    // ============ Delete ============

    @Query("DELETE FROM ipo_records WHERE hkexId = :id")
    suspend fun deleteRecord(id: Int)

    @Query("DELETE FROM ipo_records")
    suspend fun deleteAll()
}
