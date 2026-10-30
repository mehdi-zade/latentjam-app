/*
 * Copyright (c) 2026 LatentJam Project
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.nikitasud.latentjam.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.nikitasud.latentjam.library.Playlist
import io.github.nikitasud.latentjam.smart.TrackDescriptor

/**
 * Reassignment popup when swiping on cover image ("assign playlists").
 * Allows multi-selecting custom playlists, removing subtitle text, full list view,
 * and places the "Undecided" option at the very bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistAllocationDialog(
    track: TrackDescriptor,
    currentPlaylistId: String?,
    currentPlaylistName: String?,
    customPlaylists: List<Playlist>,
    onAssignPlaylists: (selectedPlaylistIds: List<String>) -> Unit,
    onSelectUndecided: () -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedPlaylists = remember {
        mutableStateMapOf<String, Boolean>().apply {
            customPlaylists.forEach { pl ->
                if (pl.trackIds.contains(track.id.value)) {
                    this[pl.id] = true
                }
            }
        }
    }

    AppModalSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Assign Playlists",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${track.title ?: track.fileName ?: "Track"} • ${track.artist ?: "Unknown artist"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Full list of custom playlists (without cramped scroll or "Assign to this playlist" subtitle)
                items(customPlaylists, key = { it.id }) { playlist ->
                    val isChecked = selectedPlaylists[playlist.id] == true
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedPlaylists[playlist.id] = !isChecked
                            },
                        color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedPlaylists[playlist.id] = checked
                                },
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${playlist.trackIds.size} tracks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // "Undecided" placed at the very END/bottom of the list
                item {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectUndecided() },
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f),
                                        CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shuffle,
                                    contentDescription = "Undecided",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Undecided",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Text(
                                    text = "Keep in Undecided (not in any playlist)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val chosenIds = selectedPlaylists.filter { it.value }.keys.toList()
                        onAssignPlaylists(chosenIds)
                    },
                ) {
                    Text("Assign")
                }
            }
        }
    }
}

/**
 * Bottom sheet displaying songs flagged for missing metadata / artwork needing user review.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FlaggedTracksSheet(
    flaggedTracks: List<Pair<TrackDescriptor, String>>,
    onEditTrack: (TrackDescriptor) -> Unit,
    onDismiss: () -> Unit,
) {
    AppModalSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Needs Review (${flaggedTracks.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Songs with incomplete metadata or missing cover art",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (flaggedTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "All songs have complete metadata and cover art!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(flaggedTracks, key = { it.first.id.value }) { (track, reason) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onEditTrack(track) },
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.HelpOutline,
                                        contentDescription = "Review",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title ?: track.fileName ?: "Unknown title",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${track.artist ?: "Unknown artist"} • $reason",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                IconButton(onClick = { onEditTrack(track) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit metadata",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
