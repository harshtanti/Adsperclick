package com.adsperclick.media.data.workManager

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.adsperclick.media.views.login.repository.AuthRepository
import javax.inject.Inject

@HiltWorker
class FCMTokenUpdateWorker @Inject constructor(
    private val context: Context,
    private val workerParams: WorkerParameters,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val newToken = inputData.getString("newToken") ?: return Result.failure()

        return try {
            authRepository.onNewToken(newToken)
            Result.success()
        } catch (e: Exception) {
            Log.e("FCM_Worker", "Token update failed: ${e.message}")
            Result.retry() // Retry if the operation fails
        }
    }
}