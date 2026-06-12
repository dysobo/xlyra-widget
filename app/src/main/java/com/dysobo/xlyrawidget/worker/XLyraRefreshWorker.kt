package com.dysobo.xlyrawidget.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dysobo.xlyrawidget.data.MissingConfigException
import com.dysobo.xlyrawidget.data.SettingsStore
import com.dysobo.xlyrawidget.data.XLyraRepository
import com.dysobo.xlyrawidget.widget.XLyraWidget
import java.util.concurrent.TimeUnit

class XLyraRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            XLyraRepository(applicationContext).refreshNow()
            Result.success()
        } catch (error: MissingConfigException) {
            SettingsStore(applicationContext).saveError(error.message ?: "配置不完整")
            XLyraWidget.updateAllWidgets(applicationContext)
            Result.success()
        } catch (error: Exception) {
            SettingsStore(applicationContext).saveError(error.message ?: "刷新失败")
            XLyraWidget.updateAllWidgets(applicationContext)
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK = "xlyra_summary_periodic"
        private const val ONETIME_WORK = "xlyra_summary_once"

        fun schedule(context: Context, refreshMinutes: Int) {
            val minutes = refreshMinutes.coerceAtLeast(15).toLong()
            val request = PeriodicWorkRequestBuilder<XLyraRefreshWorker>(minutes, TimeUnit.MINUTES)
                .setConstraints(networkConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun enqueueOneTime(context: Context) {
            val request = OneTimeWorkRequestBuilder<XLyraRefreshWorker>()
                .setConstraints(networkConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                ONETIME_WORK,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        private fun networkConstraints(): Constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
    }
}
