package com.pashuaahar.app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object FirebaseManager {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val currentUser: FirebaseUser? get() = auth.currentUser
    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun register(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun syncCowProfile(profile: CowProfile): Result<Unit> {
        val uid = currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val data = mapOf(
                "cowName"   to profile.cowName,
                "breed"     to profile.breed,
                "age"       to profile.age,
                "weight"    to profile.weight,
                "createdAt" to profile.createdAt
            )
            db.collection("users")
                .document(uid)
                .collection("cow_profiles")
                .document(profile.cowName.ifBlank { "unnamed_${profile.id}" })
                .set(data, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncFeedRecord(record: FeedRecord): Result<Unit> {
        val uid = currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val data = mapOf(
                "cowName"        to record.cowName,
                "breed"          to record.breed,
                "age"            to record.age,
                "weight"         to record.weight,
                "currentYield"   to record.currentYield,
                "targetYield"    to record.targetYield,
                "marketBagPrice" to record.marketBagPrice,
                "dailySavings"   to record.dailySavings,
                "date"           to record.date
            )
            // Use id_date as document key to guarantee uniqueness
            val docId = "${record.id}_${record.date}"
            db.collection("users")
                .document(uid)
                .collection("feed_records")
                .document(docId)
                .set(data, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveUserProfile(name: String, phone: String): Result<Unit> {
        val uid = currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            db.collection("users").document(uid)
                .set(mapOf("name" to name, "phone" to phone, "createdAt" to System.currentTimeMillis()), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Restore all feed records from Firebase back to local Room DB after login
    // Uses date as unique key to prevent duplicates
    suspend fun restoreFeedRecords(localDb: com.pashuaahar.app.AppDatabase, userPhone: String): Result<Unit> {
        val uid = currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val snapshot = db.collection("users")
                .document(uid)
                .collection("feed_records")
                .get()
                .await()
            // Get all existing dates+cownames already in local DB to avoid duplicates
            val existing = localDb.feedRecordDao().getAllSync(userPhone)
            val existingKeys = existing.map { "${it.date}_${it.cowName}_${it.breed}" }.toSet()
            for (doc in snapshot.documents) {
                try {
                    val date = doc.getLong("date") ?: System.currentTimeMillis()
                    val cowName = doc.getString("cowName") ?: ""
                    val breed = doc.getString("breed") ?: ""
                    val key = "${date}_${cowName}_${breed}"
                    // Only insert if not already in local DB
                    if (key !in existingKeys) {
                        val record = com.pashuaahar.app.FeedRecord(
                            id = 0, // autoGenerate
                            userPhone = userPhone,
                            cowName = cowName,
                            breed = breed,
                            age = doc.getDouble("age") ?: 0.0,
                            weight = doc.getDouble("weight") ?: 0.0,
                            currentYield = doc.getDouble("currentYield") ?: 0.0,
                            targetYield = doc.getDouble("targetYield") ?: 0.0,
                            marketBagPrice = doc.getDouble("marketBagPrice") ?: 0.0,
                            dailySavings = doc.getDouble("dailySavings") ?: 0.0,
                            date = date
                        )
                        localDb.feedRecordDao().insertRecord(record)
                    }
                } catch (_: Exception) {}
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete a feed record from Firebase (called when user deletes from History screen)
    // We query by `date` field because after reinstall+restore, the local Room id
    // is regenerated and no longer matches the original Firebase docId (originalId_date).
    suspend fun deleteFeedRecord(record: FeedRecord): Result<Unit> {
        val uid = currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val snapshot = db.collection("users")
                .document(uid)
                .collection("feed_records")
                .whereEqualTo("date", record.date)
                .get()
                .await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sync a daily saving entry to Firebase
    suspend fun syncDailySaving(entry: DailySavingEntry): Result<Unit> {
        val uid = currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            db.collection("users")
                .document(uid)
                .collection("daily_savings")
                .document(entry.dateString) // dateString = "2026-05-03", unique per day
                .set(mapOf(
                    "dateString" to entry.dateString,
                    "amount" to entry.amount,
                    "timestamp" to entry.timestamp
                ), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Restore daily savings from Firebase back to Room DB after login
    suspend fun restoreDailySavings(localDb: AppDatabase, userPhone: String): Result<Unit> {
        val uid = currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val snapshot = db.collection("users")
                .document(uid)
                .collection("daily_savings")
                .get()
                .await()
            // Get existing dates to avoid duplicates
            val existingDates = localDb.dailySavingEntryDao()
                .getAllSync(userPhone).map { it.dateString }.toSet()
            for (doc in snapshot.documents) {
                try {
                    val dateString = doc.getString("dateString") ?: continue
                    if (dateString !in existingDates) {
                        localDb.dailySavingEntryDao().insert(
                            DailySavingEntry(
                                userPhone = userPhone,
                                dateString = dateString,
                                amount = doc.getDouble("amount") ?: 0.0,
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}