/*
 * Copyright (c) 2026 LatentJam Project
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.nikitasud.latentjam.library

import io.github.nikitasud.latentjam.smart.TrackDescriptor

/**
 * Result of metadata completeness check.
 */
public data class MetadataCompleteness(
    public val isComplete: Boolean,
    public val missingFields: List<String>,
)

public object TrackMetadataValidator {
    /**
     * Checks if a song has complete metadata and cover art.
     * Required: Title, Artist, and Cover Art (artworkUri).
     */
    public fun checkCompleteness(track: TrackDescriptor): MetadataCompleteness {
        val missing = mutableListOf<String>()
        if (track.title.isNullOrBlank()) missing += "title"
        if (track.artist.isNullOrBlank()) missing += "artist"
        if (track.artworkUri.isNullOrBlank()) missing += "artwork"
        return MetadataCompleteness(
            isComplete = missing.isEmpty(),
            missingFields = missing,
        )
    }
}
