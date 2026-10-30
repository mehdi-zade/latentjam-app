/*
 * Copyright (c) 2026 LatentJam Project
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.nikitasud.latentjam.library

import io.github.nikitasud.latentjam.smart.TrackId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Storage interface for persistent mood state.
 */
public interface MoodStateStore {
    public suspend fun readLines(): List<String>
    public suspend fun writeLines(lines: List<String>)
}

/**
 * In-memory / persistent manager for song mood allocations and reviews.
 */
public class MoodManager(
    private val store: MoodStateStore? = null,
) {
    private val mutex = Mutex()
    private val states = mutableMapOf<TrackId, SongMoodState>()
    private val mutableStateRevision = MutableStateFlow(0L)
    public val revision: StateFlow<Long> = mutableStateRevision.asStateFlow()
    private var loaded = false

    public suspend fun getState(trackId: TrackId): SongMoodState = mutex.withLock {
        ensureLoaded()
        states[trackId] ?: SongMoodState(trackId)
    }

    public suspend fun allStates(): Map<TrackId, SongMoodState> = mutex.withLock {
        ensureLoaded()
        states.toMap()
    }

    /**
     * Swiping right on a playlist: affirmed that the track belongs to this playlist.
     * Swipe right is done only once per song per playlist.
     */
    public suspend fun affirmMood(trackId: TrackId, playlistId: String): SongMoodState = mutex.withLock {
        ensureLoaded()
        val current = states[trackId] ?: SongMoodState(trackId)
        val updated = current.copy(
            assignedMoodPlaylistIds = current.assignedMoodPlaylistIds + playlistId,
            rejectedMoodPlaylistIds = current.rejectedMoodPlaylistIds - playlistId,
            // If it was undecided and now assigned, user has confirmed a playlist
            isUndecided = false,
        )
        states[trackId] = updated
        persist()
        updated
    }

    /**
     * Swiping left on a playlist: rejects that this track belongs to this playlist.
     */
    public suspend fun rejectMood(trackId: TrackId, playlistId: String): SongMoodState = mutex.withLock {
        ensureLoaded()
        val current = states[trackId] ?: SongMoodState(trackId)
        val updated = current.copy(
            assignedMoodPlaylistIds = current.assignedMoodPlaylistIds - playlistId,
            rejectedMoodPlaylistIds = current.rejectedMoodPlaylistIds + playlistId,
        )
        states[trackId] = updated
        persist()
        updated
    }

    /**
     * Mark as undecided: queues in all other playlists until a proper one is found.
     */
    public suspend fun setUndecided(trackId: TrackId, currentPlaylistId: String): SongMoodState = mutex.withLock {
        ensureLoaded()
        val current = states[trackId] ?: SongMoodState(trackId)
        val updated = current.copy(
            assignedMoodPlaylistIds = current.assignedMoodPlaylistIds - currentPlaylistId,
            rejectedMoodPlaylistIds = current.rejectedMoodPlaylistIds + currentPlaylistId,
            isUndecided = true,
        )
        states[trackId] = updated
        persist()
        updated
    }

    /**
     * Flag or unflag a song that couldn't pass metadata/cover completion.
     */
    public suspend fun setReviewStatus(
        trackId: TrackId,
        needsReview: Boolean,
        reason: String? = null,
    ): SongMoodState = mutex.withLock {
        ensureLoaded()
        val current = states[trackId] ?: SongMoodState(trackId)
        val updated = current.copy(
            needsReview = needsReview,
            reviewReason = if (needsReview) reason else null,
        )
        states[trackId] = updated
        persist()
        updated
    }

    public suspend fun getFlaggedTracks(): List<SongMoodState> = mutex.withLock {
        ensureLoaded()
        states.values.filter { it.needsReview }
    }

    /**
     * Checks if a track was already affirmed for [playlistId] (swipe right done).
     */
    public suspend fun isAffirmed(trackId: TrackId, playlistId: String): Boolean = mutex.withLock {
        ensureLoaded()
        states[trackId]?.assignedMoodPlaylistIds?.contains(playlistId) == true
    }

    /**
     * Indexes a track for metadata completeness and mood classification.
     * Tries online metadata if incomplete, and flags if still incomplete.
     */
    public suspend fun indexTrack(
        track: io.github.nikitasud.latentjam.smart.TrackDescriptor,
        onlineFetcher: OnlineMetadataFetcher? = null,
    ): SongMoodState = mutex.withLock {
        ensureLoaded()
        val current = states[track.id] ?: SongMoodState(track.id)

        // 1. Metadata completeness & online enrichment
        val completeness = TrackMetadataValidator.checkCompleteness(track)
        val needsReview: Boolean
        val reason: String?
        if (!completeness.isComplete) {
            val online = onlineFetcher?.fetchMetadata(track)
            val stillMissing = mutableListOf<String>()
            if (track.title.isNullOrBlank() && online?.title.isNullOrBlank()) stillMissing += "title"
            if (track.artist.isNullOrBlank() && online?.artist.isNullOrBlank()) stillMissing += "artist"
            if (track.artworkUri.isNullOrBlank() && online?.coverArtUrl.isNullOrBlank()) stillMissing += "artwork"

            if (stillMissing.isNotEmpty()) {
                needsReview = true
                reason = "Missing: ${stillMissing.joinToString(", ")}"
            } else {
                needsReview = false
                reason = null
            }
        } else {
            needsReview = false
            reason = null
        }

        // 2. Initial mood playlist allocation
        val assigned = current.assignedMoodPlaylistIds.toMutableSet()
        val genre = track.genre?.lowercase()
        val energy = track.energy

        if (energy != null) {
            if (energy >= 0.7f && StandardMood.ENERGETIC.id !in current.rejectedMoodPlaylistIds) {
                assigned += StandardMood.ENERGETIC.id
            }
            if (energy <= 0.35f && StandardMood.CHILL.id !in current.rejectedMoodPlaylistIds) {
                assigned += StandardMood.CHILL.id
            }
        }
        if (genre != null) {
            if ((genre.contains("ambient") || genre.contains("soundtrack") || genre.contains("classical") || genre.contains("chill")) && StandardMood.FOCUS.id !in current.rejectedMoodPlaylistIds) {
                assigned += StandardMood.FOCUS.id
            }
            if ((genre.contains("dance") || genre.contains("electronic") || genre.contains("party") || genre.contains("pop") || genre.contains("house")) && StandardMood.PARTY.id !in current.rejectedMoodPlaylistIds) {
                assigned += StandardMood.PARTY.id
            }
            if ((genre.contains("happy") || genre.contains("funk") || genre.contains("reggae")) && StandardMood.HAPPY.id !in current.rejectedMoodPlaylistIds) {
                assigned += StandardMood.HAPPY.id
            }
            if ((genre.contains("sad") || genre.contains("blues") || genre.contains("melanchol") || genre.contains("acoustic")) && StandardMood.MELANCHOLIC.id !in current.rejectedMoodPlaylistIds) {
                assigned += StandardMood.MELANCHOLIC.id
            }
        }

        // If no explicit playlist and not rejected anywhere, default to undecided so it auditions everywhere
        val isUndecided = if (assigned.isEmpty() && current.rejectedMoodPlaylistIds.isEmpty()) {
            true
        } else {
            current.isUndecided
        }

        val updated = current.copy(
            assignedMoodPlaylistIds = assigned,
            isUndecided = isUndecided,
            needsReview = needsReview,
            reviewReason = reason,
        )
        states[track.id] = updated
        persist()
        updated
    }

    private suspend fun ensureLoaded() {
        if (loaded || store == null) {
            loaded = true
            return
        }
        val lines = store.readLines()
        for (line in lines) {
            val parts = line.split("\t")
            if (parts.size >= 5) {
                val trackId = TrackId(parts[0])
                val assigned = if (parts[1].isBlank()) emptySet() else parts[1].split(",").toSet()
                val rejected = if (parts[2].isBlank()) emptySet() else parts[2].split(",").toSet()
                val undecided = parts[3].toBoolean()
                val review = parts[4].toBoolean()
                val reason = parts.getOrNull(5)?.takeIf { it.isNotBlank() }
                states[trackId] = SongMoodState(
                    trackId = trackId,
                    assignedMoodPlaylistIds = assigned,
                    rejectedMoodPlaylistIds = rejected,
                    isUndecided = undecided,
                    needsReview = review,
                    reviewReason = reason,
                )
            }
        }
        loaded = true
    }

    private suspend fun persist() {
        mutableStateRevision.value += 1
        val store = store ?: return
        val serialized = states.values.map { state ->
            listOf(
                state.trackId.value,
                state.assignedMoodPlaylistIds.joinToString(","),
                state.rejectedMoodPlaylistIds.joinToString(","),
                state.isUndecided.toString(),
                state.needsReview.toString(),
                state.reviewReason ?: "",
            ).joinToString("\t")
        }
        store.writeLines(serialized)
    }
}
