package com.pashuaahar.app

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class User(
    @PrimaryKey val phone: String,
    val name: String,
    val password: String
)

@Entity(tableName = "feed_records")
data class FeedRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userPhone: String,
    val date: Long,
    val cowName: String = "",
    val breed: String,
    val age: Double = 0.0,
    val weight: Double,
    val currentYield: Double,
    val targetYield: Double,
    val marketBagPrice: Double = 1200.0,
    val dailySavings: Double
)

@Entity(tableName = "cow_profiles")
data class CowProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userPhone: String,
    val cowName: String,
    val breed: String,
    val age: Double,
    val weight: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_saving_entries")
data class DailySavingEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userPhone: String,
    val dateString: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun register(user: User)

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun login(phone: String): User?
}

@Dao
interface FeedRecordDao {
    @Insert
    suspend fun insertRecord(record: FeedRecord): Long

    @Query("SELECT * FROM feed_records WHERE userPhone = :phone ORDER BY date DESC")
    fun getHistory(phone: String): Flow<List<FeedRecord>>

    @Query("SELECT * FROM feed_records WHERE userPhone = :phone")
    suspend fun getAllSync(phone: String): List<FeedRecord>

    @Query("DELETE FROM feed_records WHERE userPhone = :phone")
    suspend fun deleteAllForUser(phone: String)

    @Delete
    suspend fun deleteRecord(record: FeedRecord)

    @Query("SELECT SUM(dailySavings) FROM feed_records WHERE userPhone = :phone")
    fun getTotalSavings(phone: String): Flow<Double?>
}

@Dao
interface DailySavingEntryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: DailySavingEntry)

    @Query("SELECT * FROM daily_saving_entries WHERE userPhone = :phone ORDER BY timestamp DESC")
    fun getAll(phone: String): Flow<List<DailySavingEntry>>

    @Query("SELECT SUM(amount) FROM daily_saving_entries WHERE userPhone = :phone")
    fun getTotalAccumulated(phone: String): Flow<Double?>

    @Query("SELECT COUNT(*) FROM daily_saving_entries WHERE userPhone = :phone AND dateString = :date")
    suspend fun countForDate(phone: String, date: String): Int

    @Query("DELETE FROM daily_saving_entries WHERE userPhone = :phone")
    suspend fun deleteAllForUser(phone: String)

    @Query("SELECT * FROM daily_saving_entries WHERE userPhone = :phone")
    suspend fun getAllSync(phone: String): List<DailySavingEntry>
}

@Dao
interface CowProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: CowProfile): Long

    @Query("SELECT * FROM cow_profiles WHERE userPhone = :phone ORDER BY createdAt DESC")
    fun getAll(phone: String): Flow<List<CowProfile>>

    @Delete
    suspend fun delete(profile: CowProfile)
}

@Database(entities = [User::class, FeedRecord::class, CowProfile::class, DailySavingEntry::class], version = 6)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun feedRecordDao(): FeedRecordDao
    abstract fun cowProfileDao(): CowProfileDao
    abstract fun dailySavingEntryDao(): DailySavingEntryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE feed_records ADD COLUMN marketBagPrice REAL NOT NULL DEFAULT 1200.0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE feed_records ADD COLUMN age REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE feed_records ADD COLUMN cowName TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS cow_profiles (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userPhone TEXT NOT NULL, cowName TEXT NOT NULL, breed TEXT NOT NULL, age REAL NOT NULL, weight REAL NOT NULL, createdAt INTEGER NOT NULL DEFAULT 0)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS daily_saving_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userPhone TEXT NOT NULL, dateString TEXT NOT NULL, amount REAL NOT NULL, timestamp INTEGER NOT NULL DEFAULT 0)"
                )
            }
        }

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pashu_aahar_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}