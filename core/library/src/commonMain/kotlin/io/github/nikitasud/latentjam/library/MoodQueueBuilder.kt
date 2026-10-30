/*
 * Copyright (c) 2026 LatentJam Project
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.nikitasud.latentjam.library

import io.github.nikitasud.latentjam.smart.TrackDescriptor
import io.github.nikitasud.latentjam.smart.TrackId

/**
 * Builds and ranks a playback queue for a selected playlist/mood,
 * prioritizing least-heard songs.
 */
public object MoodQueueBuilder {

    /**
     * Constructs a queue for [playlistId].
     * @param allTracks Full track catalog.
     * @param playlistTracks Tracks directly belonging to the playlist.
     * @param moodManager Mood manager state to check if other songs are eligible (e.g. undecided).
     * @param playCounts Map of TrackId to number of times played.
     */
    public fun buildQueue(
        playlistId: String,
        allTracks: List<TrackDescriptor>,
        playlistTracks: List<TrackDescriptor>,
        moodManager: MoodManager? = null,
        playCounts: Map<TrackId, Int> = emptyMap(),
        moodStates: Map<TrackId, SongMoodState> = emptyMap(),
    ): List<TrackDescriptor> {
        val directIds = playlistTracks.map { it.id }.toSet()
        val candidateTracks = mutableListOf<TrackDescriptor>()
        candidateTracks.addAll(playlistTracks)

        // Include any tracks marked as "undecided" (queued in all other playlists until a proper one is found)
        for (track in allTracks) {
            if (track.id !in directIds) {
                val state = moodStates[track.id]
                if (state != null && state.isEligibleForMood(playlistId)) {
                    candidateTracks.add(track)
                }
            }
        }

        // Prioritize the least heard songs more (ascending play count),
        // with secondary shuffling for variety among tracks with the same play count.
        return candidateTracks.sortedWith(
            compareBy<TrackDescriptor> { playCounts[it.id] ?: 0 }
                .thenBy { it.id.value.hashCode() xor 0x5bd1e995 }
        )
    }
}
