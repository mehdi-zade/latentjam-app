/*
 * Copyright (c) 2026 LatentJam Project
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.nikitasud.latentjam.library

import io.github.nikitasud.latentjam.smart.SemanticLabel
import io.github.nikitasud.latentjam.smart.TrackDescriptor
import io.github.nikitasud.latentjam.smart.TrackId
import io.github.nikitasud.latentjam.smart.TrackSemantics

/**
 * Standard mood categories for mood-based playlists.
 */
public enum class StandardMood(
    public val id: String,
    public val displayName: String,
    public val primarySemanticLabel: SemanticLabel? = null,
) {
    ENERGETIC("mood_energetic", "Energetic", SemanticLabel.ENERGY_HIGH),
    CHILL("mood_chill", "Chill & Relax", SemanticLabel.ENERGY_LOW),
    HAPPY("mood_happy", "Happy & Uplifting", SemanticLabel.MOOD_HAPPY),
    MELANCHOLIC("mood_melancholic", "Melancholic & Sad", SemanticLabel.MOOD_SAD),
    FOCUS("mood_focus", "Focus & Ambient", SemanticLabel.GENRE_AMBIENT_SOUNDTRACK),
    PARTY("mood_party", "Party & Dance", SemanticLabel.GENRE_ELECTRONIC);

    public companion object {
        public fun fromId(id: String): StandardMood? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Tracks mood allocation state for a song.
 */
public data class SongMoodState(
    public val trackId: TrackId,
    /** Playlists/moods that the user or index has explicitly affirmed this track belongs to. */
    public val assignedMoodPlaylistIds: Set<String> = emptySet(),
    /** Playlists/moods the user swiped left (rejected) on. */
    public val rejectedMoodPlaylistIds: Set<String> = emptySet(),
    /** If true, the user marked this track as "undecided", so it queues in all other mood playlists. */
    public val isUndecided: Boolean = false,
    /** Whether the song needs manual review due to missing metadata or cover art. */
    public val needsReview: Boolean = false,
    public val reviewReason: String? = null,
) {
    public fun isEligibleForMood(playlistId: String): Boolean {
        if (playlistId in rejectedMoodPlaylistIds) return false
        if (playlistId in assignedMoodPlaylistIds) return true
        return isUndecided
    }
}
