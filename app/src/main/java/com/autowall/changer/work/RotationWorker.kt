package com.autowall.changer.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autowall.changer.data.GalleryStore
import com.autowall.changer.data.SettingsRepository
import com.autowall.changer.wallpaper.WallpaperEngine
import kotlinx.coroutines.flow.first
import kotlin.random.Random

class RotationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repository = SettingsRepository(applicationContext)
        val settings = repository.settings.first()
        if (!settings.enabled) return Result.success()

        val photos = GalleryStore(applicationContext).photos()
        if (photos.isEmpty()) return Result.success()

        val index = nextIndex(settings.lastIndex, photos.size, settings.shuffle)
        return try {
            WallpaperEngine.apply(applicationContext, photos[index], settings)
            repository.markApplied(index)
            if (settings.intervalMinutes < Scheduler.MIN_PERIODIC_MINUTES) {
                Scheduler.scheduleChain(applicationContext, settings.intervalMinutes)
            }
            Result.success()
        } catch (error: Throwable) {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    private fun nextIndex(last: Int, size: Int, shuffle: Boolean): Int {
        if (size <= 1) return 0
        if (shuffle) {
            var candidate = Random.nextInt(size)
            if (candidate == last) candidate = (candidate + 1) % size
            return candidate
        }
        return (last + 1) % size
    }
}
