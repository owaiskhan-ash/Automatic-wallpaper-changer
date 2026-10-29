package com.autowall.changer.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autowall.changer.data.GalleryStore
import com.autowall.changer.data.SettingsRepository
import com.autowall.changer.model.AutoWallSettings
import com.autowall.changer.model.FitMode
import com.autowall.changer.wallpaper.WallpaperEngine
import com.autowall.changer.work.Scheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)
    private val gallery = GalleryStore(application)

    val settings: StateFlow<AutoWallSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AutoWallSettings(),
    )

    private val _photos = MutableStateFlow(gallery.photos())
    val photos: StateFlow<List<File>> = _photos.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            val current = repository.settings.first()
            if (current.enabled) Scheduler.sync(getApplication(), current)
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun importPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val added = gallery.import(uris)
            _photos.value = gallery.photos()
            _message.value = when {
                added == 0 -> "Couldn't read those photos"
                added == 1 -> "Photo added"
                else -> "$added photos added"
            }
        }
    }

    fun removePhoto(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            gallery.delete(file)
            _photos.value = gallery.photos()
            _message.value = "Photo removed"
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && _photos.value.isEmpty()) {
                _message.value = "Add a photo first"
                return@launch
            }
            val next = settings.value.copy(enabled = enabled)
            repository.update { it.copy(enabled = enabled) }
            Scheduler.sync(getApplication(), next)
            if (enabled) Scheduler.changeNow(getApplication())
            _message.value = if (enabled) "Auto rotation on" else "Auto rotation paused"
        }
    }

    fun setInterval(minutes: Int) {
        viewModelScope.launch {
            repository.update { it.copy(intervalMinutes = minutes) }
            Scheduler.sync(getApplication(), settings.value.copy(intervalMinutes = minutes))
        }
    }

    fun setFitMode(mode: FitMode) = viewModelScope.launch {
        repository.update { it.copy(fitMode = mode) }
    }

    fun setCornerRadius(dp: Int) = viewModelScope.launch {
        repository.update { it.copy(cornerRadiusDp = dp) }
    }

    fun setTone(index: Int) = viewModelScope.launch {
        repository.update { it.copy(toneIndex = index) }
    }

    fun setToneStrength(strength: Float) = viewModelScope.launch {
        repository.update { it.copy(toneStrength = (strength * 100).toInt() / 100f) }
    }

    fun setShuffle(shuffle: Boolean) = viewModelScope.launch {
        repository.update { it.copy(shuffle = shuffle) }
    }

    fun setLockScreen(enabled: Boolean) = viewModelScope.launch {
        repository.update { it.copy(applyToLockScreen = enabled) }
    }

    /** Applies the next photo in the queue, or [file] when the user taps a thumbnail. */
    fun applyNow(file: File? = null) {
        if (_busy.value) return
        val all = _photos.value
        if (all.isEmpty()) {
            _message.value = "Add a photo first"
            return
        }
        val current = settings.value
        val target = file ?: all[((current.lastIndex + 1).coerceAtLeast(0)) % all.size]
        val index = all.indexOf(target).coerceAtLeast(0)
        viewModelScope.launch(Dispatchers.IO) {
            _busy.value = true
            runCatching { WallpaperEngine.apply(getApplication(), target, current) }
                .onSuccess {
                    repository.markApplied(index)
                    _message.value = "Wallpaper updated"
                }
                .onFailure { error ->
                    _message.value = error.message ?: "Couldn't set the wallpaper"
                }
            _busy.value = false
        }
    }

    fun reapplyCurrentLook() {
        val index = settings.value.lastIndex
        val all = _photos.value
        if (index in all.indices) applyNow(all[index]) else applyNow()
    }
}
