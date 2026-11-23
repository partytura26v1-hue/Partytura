package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.InstrumentPresets
import com.example.data.model.LookupMode
import com.example.data.model.OnlineLyricsResult
import com.example.data.model.OnlineTrackMetadata
import com.example.data.model.Song
import com.example.ui.viewmodel.RepertoireViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun LookupScreen(
    viewModel: RepertoireViewModel,
    userEmail: String?,
    onSignOut: () -> Unit,
    onOpenSong: (songId: String, initialTab: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lookupState by viewModel.lookupState.collectAsStateWithLifecycle()
    val songsState by viewModel.songsState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    val savedSongs = (songsState as? UiState.Success)?.data.orEmpty()
    val matchingSavedSongs = savedSongs.filter { song ->
        val titleQ = lookupState.songTitle.trim().lowercase()
        val artistQ = lookupState.artist.trim().lowercase()
        val instQ = lookupState.instrument.trim().lowercase()

        val titleOk = titleQ.isNotBlank() && song.title.lowercase().contains(titleQ)
        val artistOk = artistQ.isBlank() || song.artist.lowercase().contains(artistQ)
        val instOk = lookupState.mode == LookupMode.LYRICS ||
            instQ.isBlank() ||
            song.instruments.any { it.lowercase().contains(instQ) } ||
            song.primaryInstrument.lowercase().contains(instQ)
        titleOk && artistOk && instOk
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .clip(RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_stage_banner),
                        contentDescription = stringResource(R.string.cd_stage_banner),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xEB141210), Color(0x66141210))
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color(0xFFFAF5EB)
                            )
                            Text(
                                text = if (!userEmail.isNullOrBlank()) userEmail else stringResource(R.string.tagline),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFDE68A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = stringResource(R.string.sign_out),
                                tint = Color(0xFFFAF5EB)
                            )
                        }
                    }
                }
            }

            // Main Lookup / Entry Window requested by user
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Okno wyszukiwania i tworzenia",
                            style = MaterialTheme.typography.titleLarge
                        )

                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LookupMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = lookupState.mode == mode,
                                    onClick = { viewModel.setLookupMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = LookupMode.entries.size
                                    ),
                                    icon = {
                                        Icon(
                                            imageVector = if (mode == LookupMode.SHEET_MUSIC) {
                                                Icons.Default.MusicNote
                                            } else {
                                                Icons.Default.Lyrics
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("mode_${mode.name.lowercase()}")
                                ) {
                                    Text(
                                        text = if (mode == LookupMode.SHEET_MUSIC) {
                                            stringResource(R.string.mode_sheet_music)
                                        } else {
                                            stringResource(R.string.mode_lyrics)
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = lookupState.songTitle,
                            onValueChange = viewModel::updateLookupTitle,
                            label = {
                                Text(
                                    if (lookupState.mode == LookupMode.SHEET_MUSIC) "Utwór (tytuł)"
                                    else "Tytuł piosenki"
                                )
                            },
                            placeholder = { Text("np. Autobiografia, Długość dźwięku samotności") },
                            leadingIcon = {
                                Icon(Icons.Default.LibraryMusic, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lookup_title_input")
                        )

                        OutlinedTextField(
                            value = lookupState.artist,
                            onValueChange = viewModel::updateLookupArtist,
                            label = { Text(stringResource(R.string.label_artist)) },
                            placeholder = { Text("np. Perfect, Myslovitz") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = if (lookupState.mode == LookupMode.LYRICS) ImeAction.Search else ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    viewModel.performLookup()
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lookup_artist_input")
                        )

                        AnimatedVisibility(visible = lookupState.mode == LookupMode.SHEET_MUSIC) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = lookupState.instrument,
                                    onValueChange = viewModel::updateLookupInstrument,
                                    label = { Text(stringResource(R.string.label_instrument)) },
                                    placeholder = { Text("np. Gitara, Saksofon, Klawisze, Bas") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Piano, contentDescription = null)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            focusManager.clearFocus()
                                            viewModel.performLookup()
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("lookup_instrument_input")
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    InstrumentPresets.common.forEach { preset ->
                                        val selected = lookupState.instrument.equals(preset.name, ignoreCase = true)
                                        FilterChip(
                                            selected = selected,
                                            onClick = { viewModel.updateLookupInstrument(preset.name) },
                                            label = { Text(preset.name) },
                                            leadingIcon = if (selected) {
                                                {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }

                        if (!lookupState.searchError.isNullOrBlank()) {
                            Text(
                                text = lookupState.searchError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.performLookup()
                                },
                                enabled = !lookupState.isSearching && !lookupState.isSaving,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("lookup_search_button")
                            ) {
                                if (lookupState.isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (lookupState.mode == LookupMode.SHEET_MUSIC) "Szukaj"
                                        else stringResource(R.string.action_search_lyrics)
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.createSongFromLookup(onOpenSong = onOpenSong)
                                },
                                enabled = !lookupState.isSaving && lookupState.songTitle.isNotBlank(),
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("lookup_create_direct_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (lookupState.mode == LookupMode.SHEET_MUSIC) "Nowe nuty"
                                    else "Własny tekst"
                                )
                            }
                        }
                    }
                }
            }

            // Matching saved songs in user's band repertoire
            if (matchingSavedSongs.isNotEmpty()) {
                item {
                    Text(
                        text = "W Twoim repertuarze (${matchingSavedSongs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(matchingSavedSongs, key = { "saved_${it.id}" }) { song ->
                    SavedMatchCard(
                        song = song,
                        requestedInstrument = if (lookupState.mode == LookupMode.SHEET_MUSIC) {
                            lookupState.instrument
                        } else "",
                        onClick = {
                            val tab = if (lookupState.mode == LookupMode.SHEET_MUSIC) 1 else 0
                            onOpenSong(song.id, tab)
                        }
                    )
                }
            }

            // Online lyrics results (LRCLIB / lyrics.ovh)
            if (lookupState.mode == LookupMode.LYRICS && lookupState.lyricsResults.isNotEmpty()) {
                item {
                    Text(
                        text = "Znalezione teksty piosenek (${lookupState.lyricsResults.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                items(lookupState.lyricsResults, key = { "lrc_${it.id}_${it.trackName}" }) { item ->
                    OnlineLyricsResultCard(
                        result = item,
                        isSaving = lookupState.isSaving,
                        onSaveAndOpen = {
                            viewModel.saveOnlineLyricsAndOpen(item, onOpenSong)
                        }
                    )
                }
            }

            // Online track metadata results for Sheet Music mode
            if (lookupState.mode == LookupMode.SHEET_MUSIC && lookupState.metadataResults.isNotEmpty()) {
                item {
                    Text(
                        text = "Rozpoznane utwory — otwórz partię: ${lookupState.instrument}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                items(lookupState.metadataResults, key = { "meta_${it.trackName}_${it.artistName}" }) { track ->
                    OnlineTrackMetadataCard(
                        track = track,
                        instrument = lookupState.instrument,
                        isSaving = lookupState.isSaving,
                        onCreatePart = {
                            viewModel.createSongFromLookup(
                                overrideTitle = track.trackName,
                                overrideArtist = track.artistName,
                                onOpenSong = onOpenSong
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedMatchCard(
    song: Song,
    requestedInstrument: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${song.artist} • Tonacja ${song.musicalKey} • ${song.bpm} BPM",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (song.instruments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Instrumenty: ${song.instruments.joinToString(", ")}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            AssistChip(
                onClick = onClick,
                label = {
                    Text(if (requestedInstrument.isNotBlank()) requestedInstrument else "Otwórz")
                }
            )
        }
    }
}

@Composable
private fun OnlineLyricsResultCard(
    result: OnlineLyricsResult,
    isSaving: Boolean,
    onSaveAndOpen: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(result.trackName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        buildString {
                            append(result.artistName)
                            if (result.albumName.isNotBlank()) append(" • ${result.albumName}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onSaveAndOpen,
                    enabled = !isSaving
                ) {
                    Text("Zapisz i otwórz")
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = result.plainLyrics.lines().take(5).joinToString("\n"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun OnlineTrackMetadataCard(
    track: OnlineTrackMetadata,
    instrument: String,
    isSaving: Boolean,
    onCreatePart: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(track.trackName, style = MaterialTheme.typography.titleMedium)
                Text(
                    buildString {
                        append(track.artistName)
                        if (track.primaryGenreName.isNotBlank()) append(" • ${track.primaryGenreName}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onCreatePart,
                enabled = !isSaving
            ) {
                Text("Nuty: $instrument")
            }
        }
    }
}
