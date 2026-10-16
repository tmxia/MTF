package com.aazhou.mtforum

import com.aazhou.mtforum.data.model.BACKGROUND_TABS
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit








class NoticePollWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as? MtfApp)?.container ?: return Result.failure()
        if (!container.httpClient.isLoggedIn()) return Result.success()
        val fresh = try {
            container.noticeChecker.check(BACKGROUND_TABS)
        } catch (e: Exception) {
            
            container.logger.warn("Notice", "后台消息轮询失败，交给系统退避重试", e)
            return Result.retry()
        }
        if (fresh.isNotEmpty()) NoticeNotifier.show(applicationContext, fresh)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "notice-poll"

        
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<NoticePollWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
