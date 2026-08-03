package com.ipomonitor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ipomonitor.data.model.IPOEntity

@Database(
    entities = [IPOEntity::class],
    version = 4,
    exportSchema = false
)
abstract class IPODatabase : RoomDatabase() {
    abstract fun ipoDao(): IPODao

    companion object {
        /**
         * Migration from v1 to v2:
         * Added status, pdfValid, errorMessage columns.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ipo_records ADD COLUMN status TEXT NOT NULL DEFAULT 'PENDING'")
                db.execSQL("ALTER TABLE ipo_records ADD COLUMN pdfValid INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE ipo_records ADD COLUMN errorMessage TEXT DEFAULT NULL")
            }
        }

        /**
         * Migration from v2 to v3:
         * Added all AI analysis result columns + metadata columns.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ipo_records ADD COLUMN industry TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE ipo_records ADD COLUMN isRefiled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE ipo_records ADD COLUMN registrationPlace TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE ipo_records ADD COLUMN companyBackground TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE ipo_records ADD COLUMN businessDescription TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE ipo_records ADD COLUMN industryClassification TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE ipo_records ADD COLUMN majorShareholders TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE ipo_records ADD COLUMN listedElsewhere TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE ipo_records ADD COLUMN marketCap TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE ipo_records ADD COLUMN priorFunding TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE ipo_records ADD COLUMN hkSubsidiary TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE ipo_records ADD COLUMN revenueThreeYears TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE ipo_records ADD COLUMN profitThreeYears TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE ipo_records ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE ipo_records ADD COLUMN analyzedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE ipo_records ADD COLUMN analyzedBy TEXT DEFAULT NULL")
            }
        }

        /**
         * Migration from v3 to v4:
         * No schema change. Settings feature uses SharedPreferences only.
         * This migration preserves all existing data.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No schema changes - settings stored in SharedPreferences
            }
        }
    }
}
