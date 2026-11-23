package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.domain.MusicTheoryUtils
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.delay

@Composable
fun LyricsStudioTab(
    song: Song,
    onSaveLyrics: (title: String, artist: String, key: String, bpm: Long, bandName: String, lyrics: String) -> Unit,
    onFetchOnlineLyrics: ((String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember(song.id) { mutableStateOf(song.lyrics.isBlank()) }
    var draftLyrics by remember(song.id, song.lyrics) { mutableStateOf(song.lyrics) }
    var draftTitle by remember(song.id, song.title) { mutableStateOf(song.title) }
    var draftArtist by remember(song.id, song.artist) { mutableStateOf(song.artist) }
    var draftKey by remember(song.id, song.musicalKey) { mutableStateOf(song.musicalKey) }
    var draftBpm by remember(song.id, song.bpm) { mutableStateOf(song.bpm.toString()) }
    var draftBand by remember(song.id, song.bandName) { mutableStateOf(song.bandName) }

    var fontSizeSp by remember { mutableIntStateOf(18) }
    var autoScrollEnabled by remember { mutableStateOf(false) }
    var autoScrollSpeed by remember { mutableFloatStateOf(24f) }
    val scrollState = rememberScrollState()

    LaunchedEffect(autoScrollEnabled, autoScrollSpeed) {
        while (autoScrollEnabled) {
            delay(45L)
            val next = (scrollState.value + (autoScrollSpeed / 12f).toInt().coerceAtLeast(1))
            if (next >= scrollState.maxValue) {
                autoScrollEnabled = false
            } else {
                scrollState.scrollTo(next)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Lyrics Controls Row
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = autoScrollEnabled,
                            onClick = { if (!isEditing) autoScrollEnabled = !autoScrollEnabled },
                            enabled = !isEditing && song.lyrics.isNotBlank(),
                            label = { Text(if (autoScrollEnabled) "Auto-scroll: ON" else "Auto-scroll") },
                            leadingIcon = {
                                Icon(
                                    if (autoScrollEnabled) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { fontSizeSp = (fontSizeSp - 2).coerceAtLeast(14) }
                        ) {
                            Icon(Icons.Default.TextDecrease, contentDescription = "Zmniejsz czcionkę")
                        }
                        IconButton(
                            onClick = { fontSizeSp = (fontSizeSp + 2).coerceAtMost(30) }
                        ) {
                            Icon(Icons.Default.TextIncrease, contentDescription = "Powiększ czcionkę")
                        }
                    }

                    FilterChip(
                        selected = isEditing,
                        onClick = {
                            autoScrollEnabled = false
                            isEditing = !isEditing
                        },
                        label = { Text(if (isEditing) "Podgląd" else "Edytuj") },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("toggle_edit_lyrics")
                    )
                }

                if (autoScrollEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Prędkość:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Slider(
                            value = autoScrollSpeed,
                            onValueChange = { autoScrollSpeed = it },
                            valueRange = 12f..60f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (isEditing) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it },
                        label = { Text("Tytuł") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = draftArtist,
                        onValueChange = { draftArtist = it },
                        label = { Text("Wykonawca") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draftKey,
                        onValueChange = { draftKey = it },
                        label = { Text("Tonacja") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = draftBpm,
                        onValueChange = { input -> draftBpm = input.filter { it.isDigit() }.take(3) },
                        label = { Text("BPM") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = draftBand,
                        onValueChange = { draftBand = it },
                        label = { Text("Zespół") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                }

                // Quick Chord Insertion Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Wstaw akord:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf("C", "G", "Am", "F", "Dm", "Em", "D", "A", "E7").forEach { chord ->
                        AssistChip(
                            onClick = { draftLyrics = "$draftLyrics[$chord]" },
                            label = { Text("[$chord]") }
                        )
                    }
                }

                OutlinedTextField(
                    value = draftLyrics,
                    onValueChange = { draftLyrics = it },
                    label = { Text("Tekst piosenki (akordy w nawiasach np. [Am]Słowa)") },
                    minLines = 9,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lyrics_editor_field")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onFetchOnlineLyrics { fetched ->
                                draftLyrics = fetched
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Pobierz z sieci")
                    }

                    Button(
                        onClick = {
                            val parsedBpm = draftBpm.toLongOrNull()?.coerceIn(30L, 300L) ?: 120L
                            onSaveLyrics(draftTitle, draftArtist, draftKey, parsedBpm, draftBand, draftLyrics)
                            isEditing = false
                        },
                        enabled = draftTitle.isNotBlank() && draftArtist.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_lyrics_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Zapisz tekst")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        } else {
            val transposedLyrics = remember(song.lyrics, song.transposeSemitones) {
                MusicTheoryUtils.transposeLyricsWithBrackets(song.lyrics, song.transposeSemitones.toInt())
            }
            val chordColor = MaterialTheme.colorScheme.primary
            val annotatedLyrics = remember(transposedLyrics, chordColor) {
                buildAnnotatedString {
                    val regex = Regex("""\[([^\]]+)\]""")
                    var lastIdx = 0
                    regex.findAll(transposedLyrics).forEach { match ->
                        if (match.range.first > lastIdx) {
                            append(transposedLyrics.substring(lastIdx, match.range.first))
                        }
                        withStyle(
                            SpanStyle(
                                color = chordColor,
                                fontWeight = FontWeight.Bold,
                                fontFamily = JetBrainsMonoFamily
                            )
                        ) {
                            append(" ${match.groupValues[1]} ")
                        }
                        lastIdx = match.range.last + 1
                    }
                    if (lastIdx < transposedLyrics.length) {
                        append(transposedLyrics.substring(lastIdx))
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (transposedLyrics.isBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Ten utwór nie ma jeszcze zapisanego tekstu.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Dodaj lub pobierz tekst")
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = annotatedLyrics,
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp * 1.55f).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}
