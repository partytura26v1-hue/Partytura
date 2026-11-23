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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ClefType
import com.example.data.model.InstrumentPart
import com.example.data.model.InstrumentPresets
import com.example.data.model.NotationType
import com.example.domain.MusicTheoryUtils
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun SheetMusicStudioTab(
    parts: List<InstrumentPart>,
    selectedPartId: String?,
    transposeSemitones: Int,
    onSelectPart: (String) -> Unit,
    onQuickAddInstrument: (String) -> Unit,
    onSavePart: (
        partId: String?,
        instrument: String,
        notationType: NotationType,
        clef: ClefType,
        tuning: String,
        capo: Long,
        staffNotes: String,
        tablature: String,
        chordsProgression: String,
        performanceNotes: String
    ) -> Unit,
    onDeletePart: (InstrumentPart) -> Unit,
    modifier: Modifier = Modifier
) {
    val activePart = parts.firstOrNull { it.id == selectedPartId } ?: parts.firstOrNull()
    var showAddInstrumentDialog by remember { mutableStateOf(false) }
    var isEditingPart by remember(activePart?.id) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Instrument Selector Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            parts.forEach { part ->
                FilterChip(
                    selected = activePart?.id == part.id,
                    onClick = {
                        isEditingPart = false
                        onSelectPart(part.id)
                    },
                    label = { Text(part.instrument) },
                    leadingIcon = {
                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
            AssistChip(
                onClick = { showAddInstrumentDialog = true },
                label = { Text("+ Instrument") },
                leadingIcon = {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.testTag("add_instrument_part_chip")
            )
        }

        if (activePart == null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Brak zapisanych partii instrumentalnych",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Wybierz instrument (np. Gitara, Bas, Saksofon, Klawisze), aby zapisać pięciolinię, akordy i tabulaturę.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { showAddInstrumentDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Dodaj partię instrumentu")
                    }
                }
            }
        } else if (isEditingPart) {
            PartEditorForm(
                part = activePart,
                onCancel = { isEditingPart = false },
                onSave = { inst, notation, clef, tuning, capo, staff, tab, chords, notes ->
                    onSavePart(activePart.id, inst, notation, clef, tuning, capo, staff, tab, chords, notes)
                    isEditingPart = false
                }
            )
        } else {
            // Read / Performance Mode for the selected InstrumentPart
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(activePart.instrument, style = MaterialTheme.typography.titleLarge)
                            Text(
                                buildString {
                                    append(activePart.resolvedNotationType().labelPl)
                                    if (activePart.tuning.isNotBlank()) append(" • ${activePart.tuning}")
                                    if (activePart.capo > 0L) append(" • Kapodaster ${activePart.capo}")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            IconButton(
                                onClick = { isEditingPart = true },
                                modifier = Modifier.testTag("edit_part_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edytuj nuty")
                            }
                            IconButton(onClick = { onDeletePart(activePart) }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = stringResource(R.string.action_delete)
                                )
                            }
                        }
                    }

                    // 5-line musical staff Canvas
                    StaffNotationCanvas(
                        staffNotes = activePart.staffNotes,
                        clef = activePart.resolvedClef(),
                        transposeSemitones = transposeSemitones
                    )

                    if (activePart.chordsProgression.isNotBlank()) {
                        val transposedChords = remember(activePart.chordsProgression, transposeSemitones) {
                            MusicTheoryUtils.transposeChordProgression(activePart.chordsProgression, transposeSemitones)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Progresja akordów",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = transposedChords,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }

                    if (activePart.tablature.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Tabulatura / Rytm",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = activePart.tablature,
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }

                    if (activePart.performanceNotes.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Uwagi wykonawcze dla zespołu",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    activePart.performanceNotes,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showAddInstrumentDialog) {
        AddInstrumentDialog(
            onDismiss = { showAddInstrumentDialog = false },
            onAdd = { name ->
                showAddInstrumentDialog = false
                onQuickAddInstrument(name)
            }
        )
    }
}

@Composable
private fun PartEditorForm(
    part: InstrumentPart,
    onCancel: () -> Unit,
    onSave: (String, NotationType, ClefType, String, Long, String, String, String, String) -> Unit
) {
    var instrument by remember(part.id) { mutableStateOf(part.instrument) }
    var notationType by remember(part.id) { mutableStateOf(part.resolvedNotationType()) }
    var clef by remember(part.id) { mutableStateOf(part.resolvedClef()) }
    var tuning by remember(part.id) { mutableStateOf(part.tuning) }
    var capo by remember(part.id) { mutableStateOf(part.capo.toString()) }
    var staffNotes by remember(part.id) { mutableStateOf(part.staffNotes) }
    var tablature by remember(part.id) { mutableStateOf(part.tablature) }
    var chordsProgression by remember(part.id) { mutableStateOf(part.chordsProgression) }
    var performanceNotes by remember(part.id) { mutableStateOf(part.performanceNotes) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Edycja partii instrumentalnej", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = instrument,
                onValueChange = { instrument = it },
                label = { Text("Instrument") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClefType.entries.forEach { item ->
                    FilterChip(
                        selected = clef == item,
                        onClick = { clef = item },
                        label = { Text(item.labelPl) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = tuning,
                    onValueChange = { tuning = it },
                    label = { Text("Strój / Transpozycja") },
                    singleLine = true,
                    modifier = Modifier.weight(1.5f)
                )
                OutlinedTextField(
                    value = capo,
                    onValueChange = { capo = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Capo") },
                    singleLine = true,
                    modifier = Modifier.weight(0.7f)
                )
            }

            // Live preview of the 5-line staff while editing!
            StaffNotationCanvas(
                staffNotes = staffNotes,
                clef = clef,
                transposeSemitones = 0
            )

            // Quick note buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("C4", "D4", "E4", "F4", "G4", "A4", "B4", "C5", "|", "-").forEach { token ->
                    AssistChip(
                        onClick = {
                            staffNotes = if (staffNotes.isBlank()) token else "${staffNotes.trimEnd()} $token"
                        },
                        label = { Text(token, fontFamily = JetBrainsMonoFamily) }
                    )
                }
            }

            OutlinedTextField(
                value = staffNotes,
                onValueChange = { staffNotes = it },
                label = { Text("Nuty na pięciolinię (np. C4 D4 E4/2 | G4 A4 G4 -)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = chordsProgression,
                onValueChange = { chordsProgression = it },
                label = { Text("Akordy / Takty (np. | Am7 | Dm7 | G7 | Cmaj7 |)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = tablature,
                onValueChange = { tablature = it },
                label = { Text("Tabulatura / Rytmika") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = performanceNotes,
                onValueChange = { performanceNotes = it },
                label = { Text("Uwagi na próbę / koncert") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        onSave(
                            instrument,
                            notationType,
                            clef,
                            tuning,
                            capo.toLongOrNull()?.coerceIn(0L, 12L) ?: 0L,
                            staffNotes,
                            tablature,
                            chordsProgression,
                            performanceNotes
                        )
                    },
                    enabled = instrument.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_part_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Zapisz nuty")
                }
            }
        }
    }
}

@Composable
private fun AddInstrumentDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var customName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj partię instrumentu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Nazwa instrumentu") },
                    placeholder = { Text("np. Gitara solowa, Saksofon altowy") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InstrumentPresets.common.forEach { preset ->
                        AssistChip(
                            onClick = { customName = preset.name },
                            label = { Text(preset.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(customName.trim()) },
                enabled = customName.isNotBlank()
            ) {
                Text("Dodaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
