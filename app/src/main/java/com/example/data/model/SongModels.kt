package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class LookupMode(val labelPl: String) {
    SHEET_MUSIC("Nuty"),
    LYRICS("Tekst")
}

enum class NotationType(val labelPl: String) {
    STAFF("Pięciolinia"),
    TAB("Tabulatura"),
    CHORDS("Akordy"),
    PERCUSSION("Perkusja");

    companion object {
        fun fromRaw(raw: String?): NotationType =
            entries.firstOrNull { it.name == raw } ?: STAFF
    }
}

enum class ClefType(val labelPl: String, val symbol: String) {
    TREBLE("Klucz wiolinowy", "𝄞"),
    BASS("Klucz basowy", "𝄢"),
    TAB("Tabulatura", "TAB"),
    PERCUSSION("Klucz perkusyjny", "𝄥");

    companion object {
        fun fromRaw(raw: String?): ClefType =
            entries.firstOrNull { it.name == raw } ?: TREBLE
    }
}

data class InstrumentPreset(
    val name: String,
    val defaultNotation: NotationType,
    val defaultClef: ClefType,
    val defaultTuning: String,
    val defaultStaffSample: String
)

object InstrumentPresets {
    val common: List<InstrumentPreset> = listOf(
        InstrumentPreset(
            name = "Gitara",
            defaultNotation = NotationType.TAB,
            defaultClef = ClefType.TREBLE,
            defaultTuning = "Standard E A D G B E",
            defaultStaffSample = "E4 G4 A4 B4 | C5 B4 A4 G4 |"
        ),
        InstrumentPreset(
            name = "Bas",
            defaultNotation = NotationType.STAFF,
            defaultClef = ClefType.BASS,
            defaultTuning = "Standard E A D G",
            defaultStaffSample = "E2 E2 G2 A2 | C3 B2 A2 G2 |"
        ),
        InstrumentPreset(
            name = "Klawisze",
            defaultNotation = NotationType.STAFF,
            defaultClef = ClefType.TREBLE,
            defaultTuning = "Koncertowe C",
            defaultStaffSample = "C4 E4 G4 B4 | A4 F4 D4 G4 |"
        ),
        InstrumentPreset(
            name = "Perkusja",
            defaultNotation = NotationType.PERCUSSION,
            defaultClef = ClefType.PERCUSSION,
            defaultTuning = "4/4 Groove",
            defaultStaffSample = "C4 F4 C4 F4 | C4 F4 C4 F4 |"
        ),
        InstrumentPreset(
            name = "Saksofon",
            defaultNotation = NotationType.STAFF,
            defaultClef = ClefType.TREBLE,
            defaultTuning = "Eb Alt / Bb Tenor",
            defaultStaffSample = "D4 F4 A4 C5 | Bb4 A4 G4 F4 |"
        ),
        InstrumentPreset(
            name = "Trąbka",
            defaultNotation = NotationType.STAFF,
            defaultClef = ClefType.TREBLE,
            defaultTuning = "Strój Bb",
            defaultStaffSample = "D4 E4 F#4 A4 | G4 F#4 E4 D4 |"
        ),
        InstrumentPreset(
            name = "Skrzypce",
            defaultNotation = NotationType.STAFF,
            defaultClef = ClefType.TREBLE,
            defaultTuning = "G D A E",
            defaultStaffSample = "G4 A4 B4 D5 | C5 B4 A4 G4 |"
        ),
        InstrumentPreset(
            name = "Wokal",
            defaultNotation = NotationType.STAFF,
            defaultClef = ClefType.TREBLE,
            defaultTuning = "Linia melodyczna",
            defaultStaffSample = "C4 D4 E4 G4 | E4 D4 C4 - |"
        )
    )

    fun presetFor(instrumentName: String): InstrumentPreset {
        val normalized = instrumentName.trim().lowercase()
        return common.firstOrNull { normalized.contains(it.name.lowercase()) }
            ?: InstrumentPreset(
                name = instrumentName.trim().ifBlank { "Instrument" },
                defaultNotation = NotationType.STAFF,
                defaultClef = ClefType.TREBLE,
                defaultTuning = "Standard",
                defaultStaffSample = "C4 D4 E4 G4 | A4 G4 E4 - |"
            )
    }
}

@Keep
@IgnoreExtraProperties
data class Song(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val artist: String = "",
    val musicalKey: String = "C",
    val bpm: Long = 120L,
    val lyrics: String = "",
    val primaryInstrument: String = "",
    val bandName: String = "",
    val transposeSemitones: Long = 0L,
    val favorite: Boolean = false,
    val instruments: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class InstrumentPart(
    @DocumentId val id: String = "",
    val userId: String = "",
    val songId: String = "",
    val instrument: String = "",
    val notationType: String = NotationType.STAFF.name,
    val clef: String = ClefType.TREBLE.name,
    val tuning: String = "Standard",
    val capo: Long = 0L,
    val staffNotes: String = "",
    val tablature: String = "",
    val chordsProgression: String = "",
    val performanceNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedNotationType(): NotationType = NotationType.fromRaw(notationType)
    fun resolvedClef(): ClefType = ClefType.fromRaw(clef)
}

@Keep
@IgnoreExtraProperties
data class Setlist(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val venueOrNotes: String = "",
    val songIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class OnlineLyricsResult(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val durationSeconds: Int,
    val plainLyrics: String,
    val syncedLyrics: String?
)

data class OnlineTrackMetadata(
    val trackName: String,
    val artistName: String,
    val collectionName: String,
    val primaryGenreName: String,
    val durationMillis: Long
)
