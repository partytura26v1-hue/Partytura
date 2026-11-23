package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.ClefType
import com.example.data.model.InstrumentPart
import com.example.data.model.NotationType
import com.example.data.model.OnlineLyricsResult
import com.example.data.model.OnlineTrackMetadata
import com.example.data.model.Setlist
import com.example.data.model.Song
import com.example.data.remote.MusicLookupClient
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class RepertoireRepository(
    private val db: FirebaseFirestore,
    private val lookupClient: MusicLookupClient = MusicLookupClient(),
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Użytkownik musi być zalogowany przez Google przed zapisem.")
    }

    fun observeSongs(userId: String): Flow<List<Song>> = flow {
        if (userId.isBlank()) throw IllegalStateException("Brak identyfikatora użytkownika.")
        val path = "users/$userId/songs"
        emitAll(
            db.collection("users").document(userId).collection("songs")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Song::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeSong(userId: String, songId: String): Flow<Song?> = flow {
        if (userId.isBlank() || songId.isBlank()) throw IllegalArgumentException("Brak ID utworu.")
        val path = "users/$userId/songs/$songId"
        emitAll(
            db.collection("users").document(userId).collection("songs").document(songId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(Song::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeParts(userId: String, songId: String): Flow<List<InstrumentPart>> = flow {
        if (userId.isBlank() || songId.isBlank()) throw IllegalArgumentException("Brak ID utworu.")
        val path = "users/$userId/songs/$songId/parts"
        emitAll(
            db.collection("users").document(userId).collection("songs").document(songId)
                .collection("parts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(InstrumentPart::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.instrument.lowercase() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeSetlists(userId: String): Flow<List<Setlist>> = flow {
        if (userId.isBlank()) throw IllegalStateException("Brak identyfikatora użytkownika.")
        val path = "users/$userId/setlists"
        emitAll(
            db.collection("users").document(userId).collection("setlists")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Setlist::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createSong(
        title: String,
        artist: String,
        musicalKey: String = "C",
        bpm: Long = 120L,
        lyrics: String = "",
        primaryInstrument: String = "",
        bandName: String = ""
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauth"}/songs"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(150)
            val cleanArtist = artist.trim().take(150)
            require(cleanTitle.isNotEmpty()) { "Tytuł utworu nie może być pusty." }
            require(cleanArtist.isNotEmpty()) { "Nazwa wykonawcy nie może być pusta." }

            val docRef = db.collection("users").document(uid).collection("songs").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "artist" to cleanArtist,
                "musicalKey" to musicalKey.trim().ifBlank { "C" }.take(20),
                "bpm" to bpm.coerceIn(30L, 300L),
                "lyrics" to lyrics.take(25000),
                "primaryInstrument" to primaryInstrument.trim().take(60),
                "bandName" to bandName.trim().take(100),
                "transposeSemitones" to 0L,
                "favorite" to false,
                "instruments" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createSongWithInitialPart(
        title: String,
        artist: String,
        instrument: String,
        notationType: NotationType,
        clef: ClefType,
        tuning: String,
        staffNotes: String,
        chordsProgression: String,
        musicalKey: String = "C",
        bpm: Long = 120L,
        bandName: String = ""
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauth"}/songs"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(150)
            val cleanArtist = artist.trim().take(150)
            val cleanInstrument = instrument.trim().take(60)
            require(cleanTitle.isNotEmpty()) { "Tytuł utworu nie może być pusty." }
            require(cleanArtist.isNotEmpty()) { "Wykonawca nie może być pusty." }
            require(cleanInstrument.isNotEmpty()) { "Podaj instrument." }

            val songRef = db.collection("users").document(uid).collection("songs").document()
            val partRef = songRef.collection("parts").document()
            docPath = songRef.path

            val songPayload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "artist" to cleanArtist,
                "musicalKey" to musicalKey.trim().ifBlank { "C" }.take(20),
                "bpm" to bpm.coerceIn(30L, 300L),
                "lyrics" to "",
                "primaryInstrument" to cleanInstrument,
                "bandName" to bandName.trim().take(100),
                "transposeSemitones" to 0L,
                "favorite" to false,
                "instruments" to listOf(cleanInstrument),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val partPayload = mapOf(
                "userId" to uid,
                "songId" to songRef.id,
                "instrument" to cleanInstrument,
                "notationType" to notationType.name,
                "clef" to clef.name,
                "tuning" to tuning.trim().take(60),
                "capo" to 0L,
                "staffNotes" to staffNotes.take(15000),
                "tablature" to "",
                "chordsProgression" to chordsProgression.take(5000),
                "performanceNotes" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(songRef, songPayload)
            batch.set(partRef, partPayload)
            batch.commit().await()
            Result.success(songRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSongDetails(
        songId: String,
        title: String,
        artist: String,
        musicalKey: String,
        bpm: Long,
        bandName: String,
        lyrics: String,
        transposeSemitones: Long
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauth"}/songs/$songId"
        return try {
            val uid = requireUserId()
            require(songId.isNotBlank()) { "Brak ID utworu." }
            val cleanTitle = title.trim().take(150)
            val cleanArtist = artist.trim().take(150)
            require(cleanTitle.isNotEmpty() && cleanArtist.isNotEmpty()) {
                "Tytuł i wykonawca są wymagane."
            }
            val docRef = db.collection("users").document(uid).collection("songs").document(songId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "title" to cleanTitle,
                    "artist" to cleanArtist,
                    "musicalKey" to musicalKey.trim().ifBlank { "C" }.take(20),
                    "bpm" to bpm.coerceIn(30L, 300L),
                    "bandName" to bandName.trim().take(100),
                    "lyrics" to lyrics.take(25000),
                    "transposeSemitones" to transposeSemitones.coerceIn(-12L, 12L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleSongFavorite(songId: String, favorite: Boolean): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauth"}/songs/$songId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("songs").document(songId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "favorite" to favorite,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSongTranspose(songId: String, semitones: Long): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauth"}/songs/$songId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("songs").document(songId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "transposeSemitones" to semitones.coerceIn(-12L, 12L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSong(songId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauth"}/songs/$songId"
        return try {
            val uid = requireUserId()
            val songRef = db.collection("users").document(uid).collection("songs").document(songId)
            docPath = songRef.path
            val parts = songRef.collection("parts").get().await()
            val batch = db.batch()
            parts.documents.forEach { batch.delete(it.reference) }
            batch.delete(songRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertInstrumentPart(
        songId: String,
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
        currentSongInstruments: List<String>
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauth"}/songs/$songId/parts"
        return try {
            val uid = requireUserId()
            require(songId.isNotBlank()) { "Brak ID utworu." }
            val cleanInstrument = instrument.trim().take(60)
            require(cleanInstrument.isNotEmpty()) { "Nazwa instrumentu jest wymagana." }

            val songRef = db.collection("users").document(uid).collection("songs").document(songId)
            val partsCol = songRef.collection("parts")
            val partRef = if (partId.isNullOrBlank()) partsCol.document() else partsCol.document(partId)
            docPath = partRef.path

            val updatedInstruments = (currentSongInstruments + cleanInstrument)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .take(15)

            val batch = db.batch()
            if (partId.isNullOrBlank()) {
                batch.set(
                    partRef,
                    mapOf(
                        "userId" to uid,
                        "songId" to songId,
                        "instrument" to cleanInstrument,
                        "notationType" to notationType.name,
                        "clef" to clef.name,
                        "tuning" to tuning.trim().take(60),
                        "capo" to capo.coerceIn(0L, 12L),
                        "staffNotes" to staffNotes.take(15000),
                        "tablature" to tablature.take(15000),
                        "chordsProgression" to chordsProgression.take(5000),
                        "performanceNotes" to performanceNotes.take(3000),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    partRef,
                    mapOf(
                        "instrument" to cleanInstrument,
                        "notationType" to notationType.name,
                        "clef" to clef.name,
                        "tuning" to tuning.trim().take(60),
                        "capo" to capo.coerceIn(0L, 12L),
                        "staffNotes" to staffNotes.take(15000),
                        "tablature" to tablature.take(15000),
                        "chordsProgression" to chordsProgression.take(5000),
                        "performanceNotes" to performanceNotes.take(3000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.update(
                songRef,
                mapOf(
                    "instruments" to updatedInstruments,
                    "primaryInstrument" to updatedInstruments.firstOrNull().orEmpty().take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(partRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteInstrumentPart(
        songId: String,
        partId: String,
        remainingInstruments: List<String>
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauth"}/songs/$songId/parts/$partId"
        return try {
            val uid = requireUserId()
            val songRef = db.collection("users").document(uid).collection("songs").document(songId)
            val partRef = songRef.collection("parts").document(partId)
            docPath = partRef.path
            val batch = db.batch()
            batch.delete(partRef)
            batch.update(
                songRef,
                mapOf(
                    "instruments" to remainingInstruments.take(15),
                    "primaryInstrument" to remainingInstruments.firstOrNull().orEmpty().take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createSetlist(name: String, venueOrNotes: String, songIds: List<String>): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauth"}/setlists"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(120)
            require(cleanName.isNotEmpty()) { "Nazwa setlisty nie może być pusta." }
            val docRef = db.collection("users").document(uid).collection("setlists").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "name" to cleanName,
                    "venueOrNotes" to venueOrNotes.trim().take(300),
                    "songIds" to songIds.take(100),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSetlist(setlistId: String, name: String, venueOrNotes: String, songIds: List<String>): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauth"}/setlists/$setlistId"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(120)
            require(cleanName.isNotEmpty()) { "Nazwa setlisty nie może być pusta." }
            val docRef = db.collection("users").document(uid).collection("setlists").document(setlistId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "name" to cleanName,
                    "venueOrNotes" to venueOrNotes.trim().take(300),
                    "songIds" to songIds.take(100),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSetlist(setlistId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauth"}/setlists/$setlistId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("setlists").document(setlistId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun searchOnlineLyrics(title: String, artist: String): List<OnlineLyricsResult> =
        lookupClient.searchLyrics(title, artist)

    suspend fun searchTrackMetadata(title: String, artist: String): List<OnlineTrackMetadata> =
        lookupClient.searchTrackMetadata(title, artist)
}
