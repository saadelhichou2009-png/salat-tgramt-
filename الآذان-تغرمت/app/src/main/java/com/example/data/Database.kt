package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Entities ---

@Entity(tableName = "prayer_records")
data class PrayerRecord(
    @PrimaryKey val date: String, // Format: "YYYY-MM-DD"
    val fajrStatus: Int = 0,      // 0: Unchecked, 1: Prayed On Time, 2: Prayed in Jama'ah, 3: Late, 4: Missed
    val dhuhrStatus: Int = 0,
    val asrStatus: Int = 0,
    val maghribStatus: Int = 0,
    val ishaStatus: Int = 0
)

@Entity(tableName = "dhikr_counts")
data class DhikrCount(
    @PrimaryKey val id: String, // Key id, e.g. "subhanallah", "alhamdulillah"
    val arabicText: String,
    val count: Int = 0
)

// --- DAO (Data Access Object) ---

@Dao
interface AppDao {
    // Prayer Log Operations
    @Query("SELECT * FROM prayer_records WHERE date = :date")
    fun getPrayerRecordForDate(date: String): Flow<PrayerRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePrayerRecord(record: PrayerRecord)

    @Query("SELECT * FROM prayer_records ORDER BY date DESC LIMIT 30")
    fun getRecentPrayerRecords(): Flow<List<PrayerRecord>>

    // Dhikr Counter Operations
    @Query("SELECT * FROM dhikr_counts")
    fun getAllDhikrs(): Flow<List<DhikrCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDhikr(dhikr: DhikrCount)

    @Query("UPDATE dhikr_counts SET count = :count WHERE id = :id")
    suspend fun updateDhikrCountOnly(id: String, count: Int)
}

// --- Room Database ---

@Database(entities = [PrayerRecord::class, DhikrCount::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "adhan_taghramt_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- Repository ---

class AppRepository(private val appDao: AppDao) {
    fun getPrayerRecord(date: String): Flow<PrayerRecord?> = appDao.getPrayerRecordForDate(date)
    val recentPrayerRecords: Flow<List<PrayerRecord>> = appDao.getRecentPrayerRecords()
    val allDhikrs: Flow<List<DhikrCount>> = appDao.getAllDhikrs()

    suspend fun savePrayerRecord(record: PrayerRecord) {
        appDao.insertOrUpdatePrayerRecord(record)
    }

    suspend fun saveDhikr(dhikr: DhikrCount) {
        appDao.insertOrUpdateDhikr(dhikr)
    }

    suspend fun updateDhikrCount(id: String, count: Int) {
        appDao.updateDhikrCountOnly(id, count)
    }
}
