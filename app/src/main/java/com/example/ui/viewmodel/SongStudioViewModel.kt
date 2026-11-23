package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.RepertoireRepository
import com.example.data.model.ClefType
import com.example.data.model.InstrumentPart
import com.example.data.model.InstrumentPresets
import com.example.data.model.NotationType
import com.example.data.model.Song
import com.example.domain.MetronomeEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SongStudioViewModel(
    private val repository: RepertoireRepository,
    private val currentUserId: String,
    private val songId: String
) : ViewModel() {

    private val metronomeEngine = MetronomeEngine()
    private var metronomeJob: Job? = null

    val songState: StateFlow<UiState<Song?>> = repository.observeSong(currentUserId, songId)
        .map<Song?, UiState<Song?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing song $songId", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Błąd odczytu utworu."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val partsState: StateFlow<UiState<List<InstrumentPart>>> = repository.observeParts(currentUserId, songId)
        .map<List<InstrumentPart>, UiState<List<InstrumentPart>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing parts for $songId", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Błąd odczytu nut."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _selectedPartId = MutableStateFlow<String?>(null)
    val selectedPartId: StateFlow<String?> = _selectedPartId.asStateFlow()

    private val _isMetronomeRunning = MutableStateFlow(false)
    val isMetronomeRunning: StateFlow<Boolean> = _isMetronomeRunning.asStateFlow()

    private val _currentBeat = MutableStateFlow(0)
    val currentBeat: StateFlow<Int> = _currentBeat.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun selectPart(partId: String) {
        _selectedPartId.value = partId
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleSongFavorite(song.id, !song.favorite)
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun stepTranspose(song: Song, delta: Int) {
        val next = (song.transposeSemitones + delta).coerceIn(-12L, 12L)
        viewModelScope.launch {
            repository.updateSongTranspose(song.id, next)
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun saveSongDetails(
        title: String,
        artist: String,
        musicalKey: String,
        bpm: Long,
        bandName: String,
        lyrics: String,
        transposeSemitones: Long,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateSongDetails(
                songId = songId,
                title = title,
                artist = artist,
                musicalKey = musicalKey,
                bpm = bpm,
                bandName = bandName,
                lyrics = lyrics,
                transposeSemitones = transposeSemitones
            ).onSuccess {
                _statusMessage.value = "Zapisano zmiany utworu."
                onSaved()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Nie udało się zapisać zmian."
            }
        }
    }

    fun fetchLyricsForSong(song: Song, onFetched: (String) -> Unit) {
        viewModelScope.launch {
            val results = runCatching {
                repository.searchOnlineLyrics(song.title, song.artist)
            }.getOrElse { emptyList() }
            val best = results.firstOrNull()
            if (best != null) {
                onFetched(best.plainLyrics)
                _statusMessage.value = "Pobrano tekst piosenki z bazy online."
            } else {
                _statusMessage.value = "Nie znaleziono tekstu online dla tego utworu."
            }
        }
    }

    fun saveInstrumentPart(
        partId: String?,
        instrument: String,
        notationType: NotationType,
        clef: ClefType,
        tuning: String,
        capo: Long,
        staffNotes: String,
        tablature: String,
        chordsProgression: String,
        performanceNotes: String,
        currentSongInstruments: List<String>,
        onSaved: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.upsertInstrumentPart(
                songId = songId,
                partId = partId,
                instrument = instrument,
                notationType = notationType,
                clef = clef,
                tuning = tuning,
                capo = capo,
                staffNotes = staffNotes,
                tablature = tablature,
                chordsProgression = chordsProgression,
                performanceNotes = performanceNotes,
                currentSongInstruments = currentSongInstruments
            ).onSuccess { savedId ->
                _selectedPartId.value = savedId
                _statusMessage.value = "Zapisano partię: $instrument."
                onSaved(savedId)
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Nie udało się zapisać partii."
            }
        }
    }

    fun quickAddInstrumentPart(
        instrumentName: String,
        currentSongInstruments: List<String>
    ) {
        val preset = InstrumentPresets.presetFor(instrumentName)
        saveInstrumentPart(
            partId = null,
            instrument = instrumentName,
            notationType = preset.defaultNotation,
            clef = preset.defaultClef,
            tuning = preset.defaultTuning,
            capo = 0L,
            staffNotes = preset.defaultStaffSample,
            tablature = "",
            chordsProgression = "| C | G | Am | F |",
            performanceNotes = "",
            currentSongInstruments = currentSongInstruments
        )
    }

    fun deletePart(part: InstrumentPart, allParts: List<InstrumentPart>) {
        viewModelScope.launch {
            val remaining = allParts
                .filter { it.id != part.id }
                .map { it.instrument }
                .distinctBy { it.lowercase() }
            repository.deleteInstrumentPart(songId, part.id, remaining)
                .onSuccess {
                    if (_selectedPartId.value == part.id) {
                        _selectedPartId.value = allParts.firstOrNull { it.id != part.id }?.id
                    }
                    _statusMessage.value = "Usunięto partię: ${part.instrument}."
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Nie udało się usunąć partii."
                }
        }
    }

    fun toggleMetronome(bpm: Long, audioEnabled: Boolean = true) {
        if (_isMetronomeRunning.value) {
            stopMetronome()
        } else {
            startMetronome(bpm, audioEnabled)
        }
    }

    private fun startMetronome(bpm: Long, audioEnabled: Boolean) {
        metronomeJob?.cancel()
        _isMetronomeRunning.value = true
        val safeBpm = bpm.coerceIn(30L, 300L)
        val intervalMs = (60_000L / safeBpm).coerceAtLeast(180L)
        metronomeJob = viewModelScope.launch(Dispatchers.Default) {
            var beat = 0
            while (isActive) {
                val beatInMeasure = (beat % 4) + 1
                _currentBeat.value = beatInMeasure
                if (audioEnabled) {
                    metronomeEngine.playBeatClick(beatInMeasure == 1)
                }
                beat++
                delay(intervalMs)
            }
        }
    }

    fun stopMetronome() {
        metronomeJob?.cancel()
        metronomeJob = null
        _isMetronomeRunning.value = false
        _currentBeat.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        stopMetronome()
    }

    private companion object {
        const val TAG = "SongStudioVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
