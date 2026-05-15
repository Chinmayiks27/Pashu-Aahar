package com.pashuaahar.app

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DailySavingWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("PashuAaharPrefs", Context.MODE_PRIVATE)
        val userPhone = prefs.getString("user_phone", null) ?: return Result.success()
        val db = AppDatabase.getDatabase(applicationContext)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val alreadyRecorded = db.dailySavingEntryDao().countForDate(userPhone, today) > 0
        if (alreadyRecorded) return Result.success()
        val allRecords = db.feedRecordDao().getHistory(userPhone).first()
        val totalSaving = allRecords.sumOf { it.dailySavings }
        if (totalSaving <= 0.0) return Result.success()
        val entry = DailySavingEntry(userPhone = userPhone, dateString = today, amount = totalSaving)
        db.dailySavingEntryDao().insert(entry)
        // Sync to Firebase so it survives reinstall
        try { FirebaseManager.syncDailySaving(entry) } catch (_: Exception) {}
        return Result.success()
    }
}

fun scheduleDailyWorker(context: Context) {
    val now = Calendar.getInstance()
    val midnight = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        add(Calendar.DAY_OF_MONTH, 1)
    }
    val delay = midnight.timeInMillis - now.timeInMillis
    val request = PeriodicWorkRequestBuilder<DailySavingWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "daily_saving_worker",
        ExistingPeriodicWorkPolicy.KEEP,
        request
    )
}