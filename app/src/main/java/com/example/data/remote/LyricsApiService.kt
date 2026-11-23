package com.example.data.remote

import com.example.data.model.OnlineLyricsResult
import com.example.data.model.OnlineTrackMetadata
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class LrcLibTrackDto(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "trackName") val trackName: String? = null,
    @Json(name = "artistName") val artistName: String? = null,
    @Json(name = "albumName") val albumName: String? = null,
    @Json(name = "duration") val duration: Double? = null,
    @Json(name = "plainLyrics") val plainLyrics: String? = null,
    @Json(name = "syncedLyrics") val syncedLyrics: String? = null
)

@JsonClass(generateAdapter = true)
data class LyricsOvhResponseDto(
    @Json(name = "lyrics") val lyrics: String? = null
)

@JsonClass(generateAdapter = true)
data class ItunesSearchResponseDto(
    @Json(name = "results") val results: List<ItunesTrackDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ItunesTrackDto(
    @Json(name = "trackName") val trackName: String? = null,
    @Json(name = "artistName") val artistName: String? = null,
    @Json(name = "collectionName") val collectionName: String? = null,
    @Json(name = "primaryGenreName") val primaryGenreName: String? = null,
    @Json(name = "trackTimeMillis") val trackTimeMillis: Long? = null
)

interface LrcLibApi {
    @GET("api/search")
    suspend fun searchLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String? = null
    ): List<LrcLibTrackDto>
}

interface LyricsOvhApi {
    @GET("v1/{artist}/{title}")
    suspend fun getLyrics(
        @Path("artist") artist: String,
        @Path("title") title: String
    ): LyricsOvhResponseDto
}

interface ItunesSearchApi {
    @GET("search")
    suspend fun searchTracks(
        @Query("term") term: String,
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 8
    ): ItunesSearchResponseDto
}

class MusicLookupClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "PartyturaAndroid/1.0 (band-repertoire)")
                .build()
            chain.proceed(req)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val lrcLibApi: LrcLibApi = Retrofit.Builder()
        .baseUrl("https://lrclib.net/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(LrcLibApi::class.java)

    private val lyricsOvhApi: LyricsOvhApi = Retrofit.Builder()
        .baseUrl("https://api.lyrics.ovh/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(LyricsOvhApi::class.java)

    private val itunesApi: ItunesSearchApi = Retrofit.Builder()
        .baseUrl("https://itunes.apple.com/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(ItunesSearchApi::class.java)

    suspend fun searchLyrics(title: String, artist: String): List<OnlineLyricsResult> {
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()
        val results = mutableListOf<OnlineLyricsResult>()

        val lrcResults = runCatching {
            lrcLibApi.searchLyrics(
                trackName = cleanTitle,
                artistName = cleanArtist.takeIf { it.isNotBlank() }
            )
        }.getOrElse { emptyList() }

        lrcResults.forEach { dto ->
            val plain = dto.plainLyrics?.trim().orEmpty()
            val syncedClean = dto.syncedLyrics
                ?.replace(Regex("""\[\d{2}:\d{2}\.\d{2,3}\]\s*"""), "")
                ?.trim()
                .orEmpty()
            val finalLyrics = plain.ifBlank { syncedClean }
            if (finalLyrics.isNotBlank()) {
                results.add(
                    OnlineLyricsResult(
                        id = dto.id,
                        trackName = dto.trackName?.takeIf { it.isNotBlank() } ?: cleanTitle,
                        artistName = dto.artistName?.takeIf { it.isNotBlank() } ?: cleanArtist,
                        albumName = dto.albumName.orEmpty(),
                        durationSeconds = dto.duration?.toInt() ?: 0,
                        plainLyrics = finalLyrics,
                        syncedLyrics = dto.syncedLyrics
                    )
                )
            }
        }

        if (results.isEmpty() && cleanArtist.isNotBlank() && cleanTitle.isNotBlank()) {
            val ovh = runCatching { lyricsOvhApi.getLyrics(cleanArtist, cleanTitle) }.getOrNull()
            val text = ovh?.lyrics?.trim().orEmpty()
            if (text.isNotBlank()) {
                results.add(
                    OnlineLyricsResult(
                        id = (cleanTitle + cleanArtist).hashCode().toLong(),
                        trackName = cleanTitle,
                        artistName = cleanArtist,
                        albumName = "",
                        durationSeconds = 0,
                        plainLyrics = text,
                        syncedLyrics = null
                    )
                )
            }
        }

        return results.distinctBy { "${it.trackName.lowercase()}|${it.artistName.lowercase()}|${it.plainLyrics.take(40)}" }
    }

    suspend fun searchTrackMetadata(title: String, artist: String): List<OnlineTrackMetadata> {
        val query = listOf(title.trim(), artist.trim()).filter { it.isNotBlank() }.joinToString(" ")
        if (query.isBlank()) return emptyList()
        val response = runCatching { itunesApi.searchTracks(term = query) }.getOrNull() ?: return emptyList()
        return response.results.mapNotNull { dto ->
            val track = dto.trackName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val art = dto.artistName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            OnlineTrackMetadata(
                trackName = track,
                artistName = art,
                collectionName = dto.collectionName.orEmpty(),
                primaryGenreName = dto.primaryGenreName.orEmpty(),
                durationMillis = dto.trackTimeMillis ?: 0L
            )
        }.distinctBy { "${it.trackName.lowercase()}|${it.artistName.lowercase()}" }
    }
}
