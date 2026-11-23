package com.example.domain

import com.example.data.model.ClefType
import com.example.data.model.InstrumentPart
import com.example.data.model.Song

enum class NoteDuration {
    WHOLE, HALF, QUARTER
}

sealed interface StaffElement {
    data object BarLine : StaffElement
    data object Rest : StaffElement
    data class Note(
        val label: String,
        val diatonicStep: Int, // C4 = 28 (C0=0, D0=1 ... C4=4*7=28)
        val accidental: String, // "", "#", "b"
        val duration: NoteDuration
    ) : StaffElement
}

object MusicTheoryUtils {
    private val SHARP_SCALE = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val FLAT_SCALE = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")

    private val NOTE_TO_SEMITONE = mapOf(
        "C" to 0, "B#" to 0,
        "C#" to 1, "DB" to 1,
        "D" to 2,
        "D#" to 3, "EB" to 3,
        "E" to 4, "FB" to 4,
        "F" to 5, "E#" to 5,
        "F#" to 6, "GB" to 6,
        "G" to 7,
        "G#" to 8, "AB" to 8,
        "A" to 9,
        "A#" to 10, "BB" to 10,
        "B" to 11, "H" to 11, "CB" to 11
    )

    private val LETTER_INDEX = mapOf(
        'C' to 0, 'D' to 1, 'E' to 2, 'F' to 3, 'G' to 4, 'A' to 5, 'B' to 6, 'H' to 6
    )

    private val CHORD_REGEX = Regex("""\[([A-GH][#b]?[^\]\s]*)\]""")
    private val STANDALONE_CHORD_REGEX = Regex("""\b([A-GH][#b]?(?:m|maj|min|dim|aug|sus|add)?[0-9]*(?:/[A-GH][#b]?)?)\b""")
    private val NOTE_TOKEN_REGEX = Regex("""^([A-Ga-gHh])([#b]?)(\d)?(?:/(1|2|4))?$""")

    fun transposeChordSymbol(chord: String, semitones: Int): String {
        if (semitones == 0 || chord.isBlank()) return chord
        val parts = chord.split("/")
        return parts.joinToString("/") { part ->
            transposeSingleChordRoot(part, semitones)
        }
    }

    private fun transposeSingleChordRoot(chordPart: String, semitones: Int): String {
        if (chordPart.isEmpty()) return chordPart
        val first = chordPart[0].uppercaseChar()
        if (first !in 'A'..'H') return chordPart
        val hasAccidental = chordPart.length >= 2 && (chordPart[1] == '#' || chordPart[1] == 'b')
        val root = if (hasAccidental) "${first}${chordPart[1]}" else "$first"
        val suffix = chordPart.substring(if (hasAccidental) 2 else 1)

        val currentIdx = NOTE_TO_SEMITONE[root.uppercase()] ?: return chordPart
        val targetIdx = ((currentIdx + semitones) % 12 + 12) % 12
        val preferFlat = root.endsWith("b") || semitones < 0
        val newRoot = if (preferFlat) FLAT_SCALE[targetIdx] else SHARP_SCALE[targetIdx]
        return newRoot + suffix
    }

    fun transposeLyricsWithBrackets(lyrics: String, semitones: Int): String {
        if (semitones == 0 || lyrics.isBlank()) return lyrics
        return CHORD_REGEX.replace(lyrics) { match ->
            val inner = match.groupValues[1]
            "[${transposeChordSymbol(inner, semitones)}]"
        }
    }

    fun transposeChordProgression(progression: String, semitones: Int): String {
        if (semitones == 0 || progression.isBlank()) return progression
        return STANDALONE_CHORD_REGEX.replace(progression) { match ->
            transposeChordSymbol(match.groupValues[1], semitones)
        }
    }

    fun transposeNoteToken(token: String, semitones: Int): String {
        if (semitones == 0) return token
        val match = NOTE_TOKEN_REGEX.matchEntire(token.trim()) ?: return token
        val letter = match.groupValues[1].uppercase()
        val accidental = match.groupValues[2]
        val octave = match.groupValues[3].toIntOrNull() ?: 4
        val durationSuffix = match.groupValues[4].let { if (it.isNotEmpty()) "/$it" else "" }

        val baseSemitone = NOTE_TO_SEMITONE[(letter + accidental).uppercase()] ?: return token
        val totalSemitones = octave * 12 + baseSemitone + semitones
        val newOctave = (totalSemitones.floorDiv(12)).coerceIn(1, 7)
        val newPc = ((totalSemitones % 12) + 12) % 12
        val useFlat = accidental == "b"
        val noteName = if (useFlat) FLAT_SCALE[newPc] else SHARP_SCALE[newPc]
        return "$noteName$newOctave$durationSuffix"
    }

    fun parseStaffElements(rawNotes: String, semitones: Int = 0): List<StaffElement> {
        if (rawNotes.isBlank()) return emptyList()
        val tokens = rawNotes
            .replace("|", " | ")
            .trim()
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() }

        return tokens.mapNotNull { raw ->
            when (raw) {
                "|" -> StaffElement.BarLine
                "-", "R", "r" -> StaffElement.Rest
                else -> {
                    val transposed = transposeNoteToken(raw, semitones)
                    val match = NOTE_TOKEN_REGEX.matchEntire(transposed) ?: return@mapNotNull null
                    val letterChar = match.groupValues[1].uppercase().first()
                    val accidental = match.groupValues[2]
                    val octave = match.groupValues[3].toIntOrNull() ?: 4
                    val dur = when (match.groupValues[4]) {
                        "1" -> NoteDuration.WHOLE
                        "2" -> NoteDuration.HALF
                        else -> NoteDuration.QUARTER
                    }
                    val letterIdx = LETTER_INDEX[letterChar] ?: return@mapNotNull null
                    val diatonicStep = octave * 7 + letterIdx
                    val displayLabel = "${if (letterChar == 'H') 'B' else letterChar}$accidental"
                    StaffElement.Note(
                        label = displayLabel,
                        diatonicStep = diatonicStep,
                        accidental = accidental,
                        duration = dur
                    )
                }
            }
        }
    }

    /**
     * Bottom line diatonic step for standard 5-line staff:
     * - Treble clef: E4 (4*7 + 2 = 30), top line F5 (38)
     * - Bass clef: G2 (2*7 + 4 = 18), top line A3 (26)
     */
    fun bottomLineDiatonicStep(clef: ClefType): Int = when (clef) {
        ClefType.BASS -> 18
        else -> 30
    }

    fun formatForBandShare(song: Song, parts: List<InstrumentPart>, transpose: Int): String {
        val sb = StringBuilder()
        val effectiveKey = transposeChordSymbol(song.musicalKey, transpose)
        sb.appendLine("🎵 ${song.title} — ${song.artist}")
        sb.appendLine("Tonacja: $effectiveKey | Tempo: ${song.bpm} BPM")
        if (song.bandName.isNotBlank()) {
            sb.appendLine("Zespół: ${song.bandName}")
        }
        sb.appendLine()

        if (parts.isNotEmpty()) {
            sb.appendLine("=== NUTY I PARTIE INSTRUMENTALNE ===")
            parts.forEach { part ->
                sb.appendLine("• ${part.instrument} (${part.resolvedNotationType().labelPl}, ${part.tuning})")
                if (part.chordsProgression.isNotBlank()) {
                    sb.appendLine("  Akordy: ${transposeChordProgression(part.chordsProgression, transpose)}")
                }
                if (part.staffNotes.isNotBlank()) {
                    sb.appendLine("  Nuty: ${part.staffNotes}")
                }
                if (part.tablature.isNotBlank()) {
                    sb.appendLine(part.tablature)
                }
                if (part.performanceNotes.isNotBlank()) {
                    sb.appendLine("  Uwagi: ${part.performanceNotes}")
                }
                sb.appendLine()
            }
        }

        if (song.lyrics.isNotBlank()) {
            sb.appendLine("=== TEKST PIOSENKI ===")
            sb.appendLine(transposeLyricsWithBrackets(song.lyrics, transpose))
        }
        return sb.toString().trim()
    }
}
