package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.RepertoireRepository
import com.example.data.model.InstrumentPresets
import com.example.data.model.LookupMode
import com.example.data.model.OnlineLyricsResult
import com.example.data.model.OnlineTrackMetadata
import com.example.data.model.Setlist
import com.example.data.model.Song
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class RepertoireFilter(val labelPl: String) {
    ALL("Wszystkie"),
    FAVORITES("Ulubione"),
    WITH_LYRICS("Z tekstem"),
    WITH_PARTS("Z nutami")
}

data class LookupUiState(
    val mode: LookupMode = LookupMode.SHEET_MUSIC,
    val songTitle: String = "",
    val artist: String = "",
    val instrument: String = "Gitara",
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val lyricsResults: List<OnlineLyricsResult> = emptyList(),
    val metadataResults: List<OnlineTrackMetadata> = emptyList(),
    val searchError: String? = null,
    val isSaving: Boolean = false,
    val bannerMessage: String? = null
)

class RepertoireViewModel(
    private val repository: RepertoireRepository,
    private val currentUserId: String
) : ViewModel() {

    val songsState: StateFlow<UiState<List<Song>>> = repository.observeSongs(currentUserId)
        .map<List<Song>, UiState<List<Song>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing songs", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Nie udało się pobrać repertuaru."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val setlistsState: StateFlow<UiState<List<Setlist>>> = repository.observeSetlists(currentUserId)
        .map<List<Setlist>, UiState<List<Setlist>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing setlists", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Nie udało się pobrać setlist."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _lookupState = MutableStateFlow(LookupUiState())
    val lookupState: StateFlow<LookupUiState> = _lookupState.asStateFlow()

    private val _repertoireQuery = MutableStateFlow("")
    val repertoireQuery: StateFlow<String> = _repertoireQuery.asStateFlow()

    private val _repertoireFilter = MutableStateFlow(RepertoireFilter.ALL)
    val repertoireFilter: StateFlow<RepertoireFilter> = _repertoireFilter.asStateFlow()

    private val _selectedInstrumentFilter = MutableStateFlow<String?>(null)
    val selectedInstrumentFilter: StateFlow<String?> = _selectedInstrumentFilter.asStateFlow()

    fun setLookupMode(mode: LookupMode) {
        _lookupState.update {
            it.copy(
                mode = mode,
                hasSearched = false,
                searchError = null,
                lyricsResults = emptyList(),
                metadataResults = emptyList()
            )
        }
    }

    fun updateLookupTitle(value: String) {
        _lookupState.update { it.copy(songTitle = value, searchError = null) }
    }

    fun updateLookupArtist(value: String) {
        _lookupState.update { it.copy(artist = value, searchError = null) }
    }

    fun updateLookupInstrument(value: String) {
        _lookupState.update { it.copy(instrument = value, searchError = null) }
    }

    fun clearBannerMessage() {
        _lookupState.update { it.copy(bannerMessage = null) }
    }

    fun setRepertoireQuery(query: String) {
        _repertoireQuery.value = query
    }

    fun setRepertoireFilter(filter: RepertoireFilter) {
        _repertoireFilter.value = filter
    }

    fun setInstrumentFilter(instrument: String?) {
        _selectedInstrumentFilter.value = if (_selectedInstrumentFilter.value == instrument) null else instrument
    }

    fun performLookup() {
        val state = _lookupState.value
        val title = state.songTitle.trim()
        val artist = state.artist.trim()

        if (title.isBlank()) {
            _lookupState.update { it.copy(searchError = "Wpisz tytuł utworu lub piosenki.") }
            return
        }
        if (state.mode == LookupMode.SHEET_MUSIC && state.instrument.trim().isBlank()) {
            _lookupState.update { it.copy(searchError = "Wpisz lub wybierz instrument.") }
            return
        }

        viewModelScope.launch {
            _lookupState.update {
                it.copy(
                    isSearching = true,
                    hasSearched = false,
                    searchError = null,
                    lyricsResults = emptyList(),
                    metadataResults = emptyList()
                )
            }

            try {
                if (state.mode == LookupMode.LYRICS) {
                    val lyrics = repository.searchOnlineLyrics(title, artist)
                    _lookupState.update {
                        it.copy(
                            isSearching = false,
                            hasSearched = true,
                            lyricsResults = lyrics,
                            searchError = if (lyrics.isEmpty()) {
                                "Nie znaleziono tekstu online — możesz zapisać własny tekst do repertuaru."
                            } else null
                        )
                    }
                } else {
                    val tracks = repository.searchTrackMetadata(title, artist)
                    _lookupState.update {
                        it.copy(
                            isSearching = false,
                            hasSearched = true,
                            metadataResults = tracks
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _lookupState.update {
                    it.copy(
                        isSearching = false,
                        hasSearched = true,
                        searchError = "Problem z wyszukiwaniem online. Nadal możesz utworzyć wpis lokalnie w chmurze zespołu."
                    )
                }
            }
        }
    }

    fun saveOnlineLyricsAndOpen(
        result: OnlineLyricsResult,
        onOpenSong: (String, Int) -> Unit
    ) {
        viewModelScope.launch {
            _lookupState.update { it.copy(isSaving = true) }
            val res = repository.createSong(
                title = result.trackName,
                artist = result.artistName.ifBlank { _lookupState.value.artist.ifBlank { "Nieznany wykonawca" } },
                lyrics = result.plainLyrics,
                primaryInstrument = "Wokal"
            )
            _lookupState.update { it.copy(isSaving = false) }
            res.onSuccess { songId ->
                onOpenSong(songId, 0)
            }.onFailure { err ->
                _lookupState.update {
                    it.copy(bannerMessage = err.localizedMessage ?: "Błąd zapisu utworu.")
                }
            }
        }
    }

    fun createSongFromLookup(
        overrideTitle: String? = null,
        overrideArtist: String? = null,
        onOpenSong: (String, Int) -> Unit
    ) {
        val state = _lookupState.value
        val title = (overrideTitle ?: state.songTitle).trim()
        val artist = (overrideArtist ?: state.artist).trim().ifBlank { "Zespół własny" }
        if (title.isBlank()) {
            _lookupState.update { it.copy(searchError = "Podaj tytuł utworu.") }
            return
        }

        viewModelScope.launch {
            _lookupState.update { it.copy(isSaving = true) }
            val result = if (state.mode == LookupMode.SHEET_MUSIC) {
                val instrumentName = state.instrument.trim().ifBlank { "Gitara" }
                val preset = InstrumentPresets.presetFor(instrumentName)
                repository.createSongWithInitialPart(
                    title = title,
                    artist = artist,
                    instrument = instrumentName,
                    notationType = preset.defaultNotation,
                    clef = preset.defaultClef,
                    tuning = preset.defaultTuning,
                    staffNotes = preset.defaultStaffSample,
                    chordsProgression = "| C | G | Am | F |"
                )
            } else {
                repository.createSong(
                    title = title,
                    artist = artist,
                    lyrics = "",
                    primaryInstrument = "Wokal"
                )
            }
            _lookupState.update { it.copy(isSaving = false) }
            result.onSuccess { songId ->
                val initialTab = if (state.mode == LookupMode.SHEET_MUSIC) 1 else 0
                onOpenSong(songId, initialTab)
            }.onFailure { err ->
                _lookupState.update {
                    it.copy(bannerMessage = err.localizedMessage ?: "Nie udało się utworzyć utworu.")
                }
            }
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleSongFavorite(song.id, !song.favorite)
        }
    }

    fun deleteSong(songId: String) {
        viewModelScope.launch {
            repository.deleteSong(songId).onFailure { err ->
                _lookupState.update {
                    it.copy(bannerMessage = err.localizedMessage ?: "Nie udało się usunąć utworu.")
                }
            }
        }
    }

    fun createSetlist(name: String, venueOrNotes: String, songIds: List<String>) {
        viewModelScope.launch {
            repository.createSetlist(name, venueOrNotes, songIds).onFailure { err ->
                _lookupState.update {
                    it.copy(bannerMessage = err.localizedMessage ?: "Nie udało się utworzyć setlisty.")
                }
            }
        }
    }

    fun updateSetlist(setlistId: String, name: String, venueOrNotes: String, songIds: List<String>) {
        viewModelScope.launch {
            repository.updateSetlist(setlistId, name, venueOrNotes, songIds).onFailure { err ->
                _lookupState.update {
                    it.copy(bannerMessage = err.localizedMessage ?: "Nie udało się zapisać setlisty.")
                }
            }
        }
    }

    fun deleteSetlist(setlistId: String) {
        viewModelScope.launch {
            repository.deleteSetlist(setlistId).onFailure { err ->
                _lookupState.update {
                    it.copy(bannerMessage = err.localizedMessage ?: "Nie udało się usunąć setlisty.")
                }
            }
        }
    }

    private companion object {
        const val TAG = "RepertoireVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
