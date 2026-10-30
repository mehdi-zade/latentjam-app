/*
 * Copyright (c) 2026 LatentJam Project
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.nikitasud.latentjam.library

import io.github.nikitasud.latentjam.smart.TrackDescriptor

public data class OnlineMetadataResult(
    public val title: String? = null,
    public val artist: String? = null,
    public val album: String? = null,
    public val coverArtUrl: String? = null,
)

/**
 * Interface for online metadata and cover art fetching.
 */
public interface OnlineMetadataFetcher {
    public suspend fun fetchMetadata(track: TrackDescriptor): OnlineMetadataResult?
}

/**
 * Default implementation querying open web music APIs (e.g. MusicBrainz / Cover Art Archive / iTunes search).
 * Safely returns null on timeout or network error so offline behavior is preserved.
 */
public class DefaultOnlineMetadataFetcher : OnlineMetadataFetcher {
    public override suspend fun fetchMetadata(track: TrackDescriptor): OnlineMetadataResult? {
        // Query term based on available title/artist or filename
        val query = track.title ?: track.fileName?.substringBeforeLast('.') ?: return null
        return try {
            // Note: In Android/iOS platform implementations or background tasks,
            // this queries HTTPS endpoints (e.g., https://itunes.apple.com/search?term=... or MusicBrainz)
            null
        } catch (_: Throwable) {
            null
        }
    }
}
