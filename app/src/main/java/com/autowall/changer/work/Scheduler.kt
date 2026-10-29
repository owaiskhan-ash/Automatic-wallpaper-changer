package com.autowall.changer.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.workDataOf
import androidx.work.WorkManager
import com.autowall.changer.model.AutoWallSettings
import java.util.concurrent.TimeUnit

object Scheduler {

    const val MIN_PERIODIC_MINUTES = 15

    private const val PERIODIC_WORK = "autowall.rotation.periodic"
    private const val CHAIN_WORK = "autowall.rotation.chain"
    private const val NOW_WORK = "autowall.rotation.now"
    const val INPUT_FORCE = "force_manual_change"

    fun sync(context: Context, settings: AutoWallSettings) {
        val workManager = WorkManager.getInstance(context)
        if (!settings.enabled) {
            workManager.cancelUniqueWork(PERIODIC_WORK)
            workManager.cancelUniqueWork(CHAIN_WORK)
            return
        }
        if (settings.intervalMinutes < MIN_PERIODIC_MINUTES) {
            workManager.cancelUniqueWork(PERIODIC_WORK)
            scheduleChain(context, settings.intervalMinutes)
        } else {
            workManager.cancelUniqueWork(CHAIN_WORK)
            val request = PeriodicWorkRequestBuilder<RotationWorker>(
                settings.intervalMinutes.toLong(),
                TimeUnit.MINUTES,
            )
                .setConstraints(Constraints.NONE)
                .build()
            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }

    /** WorkManager cannot repeat faster than 15 minutes, so short intervals self-chain. */
    fun scheduleChain(context: Context, minutes: Int) {
        val request = OneTimeWorkRequestBuilder<RotationWorker>()
            .setInitialDelay(minutes.toLong().coerceAtLeast(1L), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            CHAIN_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun changeNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<RotationWorker>()
            .setInputData(workDataOf(INPUT_FORCE to true))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            NOW_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
