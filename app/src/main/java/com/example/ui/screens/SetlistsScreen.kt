package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Setlist
import com.example.data.model.Song
import com.example.ui.viewmodel.RepertoireViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun SetlistsScreen(
    viewModel: RepertoireViewModel,
    onOpenSong: (songId: String, initialTab: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val setlistsState by viewModel.setlistsState.collectAsStateWithLifecycle()
    val songsState by viewModel.songsState.collectAsStateWithLifecycle()
    val songsById = ((songsState as? UiState.Success)?.data.orEmpty()).associateBy { it.id }
    val allSongs = (songsState as? UiState.Success)?.data.orEmpty()

    var editingSetlist by remember { mutableStateOf<Setlist?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_setlist")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nowa setlista")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val state = setlistsState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }

                is UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Setlisty na próby i koncerty",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }

                        if (state.data.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.QueueMusic,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Text(
                                            "Brak setlist zespołu",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            "Ułóż kolejność utworów na koncert lub próbę, aby szybko przełączać teksty i nuty na scenie.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Button(onClick = { showCreateDialog = true }) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Utwórz setlistę")
                                        }
                                    }
                                }
                            }
                        } else {
                            items(state.data, key = { it.id }) { setlist ->
                                SetlistCard(
                                    setlist = setlist,
                                    songsById = songsById,
                                    onOpenSong = onOpenSong,
                                    onEdit = { editingSetlist = setlist },
                                    onDelete = { viewModel.deleteSetlist(setlist.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingSetlist != null) {
        val current = editingSetlist
        SetlistEditorDialog(
            initialName = current?.name.orEmpty(),
            initialNotes = current?.venueOrNotes.orEmpty(),
            initialSongIds = current?.songIds.orEmpty(),
            availableSongs = allSongs,
            onDismiss = {
                showCreateDialog = false
                editingSetlist = null
            },
            onConfirm = { name, notes, selectedIds ->
                if (current == null) {
                    viewModel.createSetlist(name, notes, selectedIds)
                } else {
                    viewModel.updateSetlist(current.id, name, notes, selectedIds)
                }
                showCreateDialog = false
                editingSetlist = null
            }
        )
    }
}

@Composable
private fun SetlistCard(
    setlist: Setlist,
    songsById: Map<String, Song>,
    onOpenSong: (String, Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val resolvedSongs = setlist.songIds.mapNotNull { songsById[it] }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(18.dp),
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
                    Text(setlist.name, style = MaterialTheme.typography.titleLarge)
                    if (setlist.venueOrNotes.isNotBlank()) {
                        Text(
                            setlist.venueOrNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edytuj setlistę")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.action_delete)
                        )
                    }
                }
            }

            if (resolvedSongs.isEmpty()) {
                Text(
                    "Brak przypisanych utworów — kliknij ikonę edycji, aby dodać piosenki z repertuaru.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                resolvedSongs.forEachIndexed { idx, song ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSong(song.id, 0) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}.",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    song.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${song.artist} • ${song.musicalKey} • ${song.bpm} BPM",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun SetlistEditorDialog(
    initialName: String,
    initialNotes: String,
    initialSongIds: List<String>,
    availableSongs: List<Song>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var notes by remember { mutableStateOf(initialNotes) }
    val selectedIds = remember { mutableStateListOf<String>().apply { addAll(initialSongIds) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialName.isBlank()) "Nowa setlista" else "Edytuj setlistę") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa setlisty") },
                    placeholder = { Text("np. Koncert Klubowy / Próba") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Miejsce lub uwagi") },
                    placeholder = { Text("np. Piątek 20:00, zestaw akustyczny") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text("Wybierz utwory z repertuaru:", style = MaterialTheme.typography.labelLarge)
                if (availableSongs.isEmpty()) {
                    Text(
                        "Dodaj najpierw utwory w zakładce Szukaj.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    availableSongs.forEach { song ->
                        val checked = selectedIds.contains(song.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) selectedIds.remove(song.id) else selectedIds.add(song.id)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (!selectedIds.contains(song.id)) selectedIds.add(song.id)
                                    } else {
                                        selectedIds.remove(song.id)
                                    }
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    song.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, notes, selectedIds.toList()) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
