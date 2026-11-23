package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.MusicTheoryUtils
import com.example.ui.components.LyricsStudioTab
import com.example.ui.components.SheetMusicStudioTab
import com.example.ui.viewmodel.SongStudioViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongStudioScreen(
    viewModel: SongStudioViewModel,
    initialTab: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val songState by viewModel.songState.collectAsStateWithLifecycle()
    val partsState by viewModel.partsState.collectAsStateWithLifecycle()
    val selectedPartId by viewModel.selectedPartId.collectAsStateWithLifecycle()
    val isMetronomeRunning by viewModel.isMetronomeRunning.collectAsStateWithLifecycle()
    val currentBeat by viewModel.currentBeat.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    var activeTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val song = (songState as? UiState.Success)?.data
    val parts = (partsState as? UiState.Success)?.data.orEmpty()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("studio_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                title = {
                    if (song != null) {
                        Column {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text("Studio utworu")
                    }
                },
                actions = {
                    if (song != null) {
                        IconButton(onClick = { viewModel.toggleFavorite(song) }) {
                            Icon(
                                imageVector = if (song.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = stringResource(R.string.cd_favorite),
                                tint = if (song.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                val textToShare = MusicTheoryUtils.formatForBandShare(
                                    song = song,
                                    parts = parts,
                                    transpose = song.transposeSemitones.toInt()
                                )
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "${song.title} — ${song.artist}")
                                    putExtra(Intent.EXTRA_TEXT, textToShare)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.action_share)))
                            },
                            modifier = Modifier.testTag("share_song_button")
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = stringResource(R.string.action_share)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val sState = songState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(sState.message, color = MaterialTheme.colorScheme.error)
                    }
                }

                is UiState.Success -> {
                    if (song == null) {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Nie znaleziono utworu.")
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 820.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Transposition & Metronome Performance Bar
                            PerformanceControlBar(
                                musicalKey = song.musicalKey,
                                transpose = song.transposeSemitones.toInt(),
                                bpm = song.bpm,
                                isMetronomeRunning = isMetronomeRunning,
                                currentBeat = currentBeat,
                                onTransposeMinus = { viewModel.stepTranspose(song, -1) },
                                onTransposePlus = { viewModel.stepTranspose(song, 1) },
                                onToggleMetronome = { viewModel.toggleMetronome(song.bpm) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            TabRow(selectedTabIndex = activeTab) {
                                Tab(
                                    selected = activeTab == 0,
                                    onClick = { activeTab = 0 },
                                    text = { Text("Tekst piosenki") },
                                    icon = { Icon(Icons.Default.Lyrics, contentDescription = null) },
                                    modifier = Modifier.testTag("studio_tab_lyrics")
                                )
                                Tab(
                                    selected = activeTab == 1,
                                    onClick = { activeTab = 1 },
                                    text = { Text("Nuty (${parts.size})") },
                                    icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                                    modifier = Modifier.testTag("studio_tab_parts")
                                )
                            }

                            if (activeTab == 0) {
                                LyricsStudioTab(
                                    song = song,
                                    onSaveLyrics = { title, artist, key, bpm, band, lyrics ->
                                        viewModel.saveSongDetails(
                                            title = title,
                                            artist = artist,
                                            musicalKey = key,
                                            bpm = bpm,
                                            bandName = band,
                                            lyrics = lyrics,
                                            transposeSemitones = song.transposeSemitones
                                        )
                                    },
                                    onFetchOnlineLyrics = { callback ->
                                        viewModel.fetchLyricsForSong(song, callback)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                SheetMusicStudioTab(
                                    parts = parts,
                                    selectedPartId = selectedPartId,
                                    transposeSemitones = song.transposeSemitones.toInt(),
                                    onSelectPart = viewModel::selectPart,
                                    onQuickAddInstrument = { inst ->
                                        viewModel.quickAddInstrumentPart(inst, song.instruments)
                                    },
                                    onSavePart = { partId, inst, notation, clef, tuning, capo, staff, tab, chords, notes ->
                                        viewModel.saveInstrumentPart(
                                            partId = partId,
                                            instrument = inst,
                                            notationType = notation,
                                            clef = clef,
                                            tuning = tuning,
                                            capo = capo,
                                            staffNotes = staff,
                                            tablature = tab,
                                            chordsProgression = chords,
                                            performanceNotes = notes,
                                            currentSongInstruments = song.instruments
                                        )
                                    },
                                    onDeletePart = { part ->
                                        viewModel.deletePart(part, parts)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceControlBar(
    musicalKey: String,
    transpose: Int,
    bpm: Long,
    isMetronomeRunning: Boolean,
    currentBeat: Int,
    onTransposeMinus: () -> Unit,
    onTransposePlus: () -> Unit,
    onToggleMetronome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveKey = MusicTheoryUtils.transposeChordSymbol(musicalKey, transpose)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Transposition control
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onTransposeMinus,
                    modifier = Modifier.testTag("transpose_minus_button")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Transpozycja w dół")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Tonacja $effectiveKey",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (transpose == 0) "Oryginał" else if (transpose > 0) "+$transpose półt." else "$transpose półt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onTransposePlus,
                    modifier = Modifier.testTag("transpose_plus_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Transpozycja w górę")
                }
            }

            // Metronome control + 4 beat dots
            Surface(
                color = if (isMetronomeRunning) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                shape = RoundedCornerShape(12.dp),
                onClick = onToggleMetronome,
                modifier = Modifier.testTag("metronome_toggle_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isMetronomeRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.cd_metronome),
                        tint = if (isMetronomeRunning) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "$bpm BPM",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (beat in 1..4) {
                            val active = isMetronomeRunning && currentBeat == beat
                            val dotColor by animateColorAsState(
                                targetValue = when {
                                    active && beat == 1 -> MaterialTheme.colorScheme.primary
                                    active -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.outline
                                },
                                label = "beat_dot"
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }
                }
            }
        }
    }
}
