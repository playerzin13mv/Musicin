package com.musicin.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player

// ---------------------------------------------------------------- Músicas

@Composable
fun SongsTab(
    vm: PlayerViewModel,
    songs: List<Song>,
    loading: Boolean,
    currentId: Long?,
    onAddToPlaylist: (Song) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(songs, query) {
        if (query.isBlank()) songs
        else songs.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Musicin",
            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "${songs.size} músicas no telefone",
            Modifier.padding(start = 16.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            placeholder = { Text("Buscar música, artista ou álbum") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Limpar") }
                }
            },
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = { vm.playAll(filtered, 0) },
                enabled = filtered.isNotEmpty(),
            ) {
                Icon(Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(4.dp))
                Text("Tocar tudo")
            }
            OutlinedButton(
                onClick = { vm.shuffleAll(filtered) },
                enabled = filtered.isNotEmpty(),
            ) {
                Icon(Icons.Rounded.Shuffle, null)
                Spacer(Modifier.width(4.dp))
                Text("Aleatório")
            }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            filtered.isEmpty() -> EmptyMessage(
                if (songs.isEmpty()) "Nenhuma música encontrada no telefone." else "Nada encontrado para \"$query\".",
            )
            else -> LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(filtered, key = { _, s -> s.id }) { index, song ->
                    SongRow(
                        song = song,
                        playing = song.id == currentId,
                        onClick = { vm.playAll(filtered, index) },
                        actions = listOf(
                            "Tocar a seguir" to { vm.playNext(song) },
                            "Adicionar à fila" to { vm.addToQueue(song) },
                            "Adicionar à playlist" to { onAddToPlaylist(song) },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------- Playlists

@Composable
fun PlaylistsTab(
    vm: PlayerViewModel,
    playlists: List<Playlist>,
    songs: List<Song>,
    onOpen: (String) -> Unit,
) {
    val existing = remember(songs) { songs.map { it.id }.toHashSet() }
    val byId = remember(songs) { songs.associateBy { it.id } }
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Playlist?>(null) }
    var deleting by remember { mutableStateOf<Playlist?>(null) }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Playlists",
            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        FilledTonalButton(
            onClick = { creating = true },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Icon(Icons.Rounded.Add, null)
            Spacer(Modifier.width(4.dp))
            Text("Nova playlist")
        }

        if (playlists.isEmpty()) {
            EmptyMessage("Você ainda não tem playlists.\nCrie uma e adicione músicas pelo menu ⋮ de cada música.")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(playlists, key = { _, p -> p.id }) { _, p ->
                    var menuOpen by remember { mutableStateOf(false) }
                    val count = p.songIds.count { it in existing }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(p.id) }
                            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.QueueMusic, null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                p.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                if (count == 1) "1 música" else "$count músicas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, "Mais opções") }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Tocar") },
                                    onClick = {
                                        menuOpen = false
                                        vm.playAll(p.songIds.mapNotNull { byId[it] }, 0)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Renomear") },
                                    onClick = {
                                        menuOpen = false
                                        renaming = p
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Excluir") },
                                    onClick = {
                                        menuOpen = false
                                        deleting = p
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        NameDialog(
            title = "Nova playlist",
            confirmLabel = "Criar",
            onConfirm = {
                vm.createPlaylist(it)
                creating = false
            },
            onDismiss = { creating = false },
        )
    }
    renaming?.let { p ->
        NameDialog(
            title = "Renomear playlist",
            initial = p.name,
            onConfirm = {
                vm.renamePlaylist(p.id, it)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { p ->
        ConfirmDialog(
            title = "Excluir playlist?",
            message = "\"${p.name}\" será excluída. As músicas continuam no telefone.",
            confirmLabel = "Excluir",
            onConfirm = {
                vm.deletePlaylist(p.id)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
fun PlaylistDetail(
    vm: PlayerViewModel,
    playlist: Playlist,
    songs: List<Song>,
    currentId: Long?,
    onBack: () -> Unit,
) {
    val byId = remember(songs) { songs.associateBy { it.id } }
    val items = remember(playlist, byId) { playlist.songIds.mapNotNull { byId[it] } }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar") }
            Column(Modifier.weight(1f)) {
                Text(
                    playlist.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (items.size == 1) "1 música" else "${items.size} músicas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(onClick = { vm.playAll(items, 0) }, enabled = items.isNotEmpty()) {
                Icon(Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(4.dp))
                Text("Tocar")
            }
            OutlinedButton(onClick = { vm.shuffleAll(items) }, enabled = items.isNotEmpty()) {
                Icon(Icons.Rounded.Shuffle, null)
                Spacer(Modifier.width(4.dp))
                Text("Aleatório")
            }
        }
        if (items.isEmpty()) {
            EmptyMessage("Essa playlist está vazia.\nNa aba Músicas, use o menu ⋮ e escolha \"Adicionar à playlist\".")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(items, key = { _, s -> s.id }) { index, song ->
                    SongRow(
                        song = song,
                        playing = song.id == currentId,
                        onClick = { vm.playAll(items, index) },
                        actions = listOf(
                            "Tocar a seguir" to { vm.playNext(song) },
                            "Adicionar à fila" to { vm.addToQueue(song) },
                            "Remover da playlist" to { vm.removeFromPlaylist(playlist.id, song.id) },
                        ),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Mini player

@Composable
fun MiniPlayer(song: Song, isPlaying: Boolean, vm: PlayerViewModel, onOpen: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    ) {
        Column {
            MiniProgress(vm)
            Row(
                Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        song.artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                IconButton(onClick = { vm.togglePlay() }) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (isPlaying) "Pausar" else "Tocar",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                IconButton(onClick = { vm.next() }) {
                    Icon(Icons.Rounded.SkipNext, "Próxima", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
}

@Composable
private fun MiniProgress(vm: PlayerViewModel) {
    val position by vm.position.collectAsState()
    val duration by vm.duration.collectAsState()
    LinearProgressIndicator(
        progress = { if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f },
        modifier = Modifier.fillMaxWidth().height(2.dp),
    )
}

// ---------------------------------------------------------------- Player completo

@Composable
fun PlayerScreen(vm: PlayerViewModel, onClose: () -> Unit) {
    val song by vm.currentSong.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val position by vm.position.collectAsState()
    val duration by vm.duration.collectAsState()
    val shuffle by vm.shuffle.collectAsState()
    val repeat by vm.repeat.collectAsState()
    val s = song ?: return

    val art = rememberArtwork(s)
    var dragging by remember { mutableStateOf<Float?>(null) }
    val max = duration.coerceAtLeast(1L).toFloat()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, "Fechar", Modifier.size(32.dp)) }
            Text(
                "Tocando agora",
                Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.width(48.dp))
        }

        Spacer(Modifier.weight(1f))

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (art != null) {
                Image(
                    bitmap = art,
                    contentDescription = "Capa do álbum",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Rounded.MusicNote, null,
                    Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            s.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "${s.artist} • ${s.album}",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
        Slider(
            value = (dragging ?: position.toFloat()).coerceIn(0f, max),
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { vm.seekTo(it.toLong()) }
                dragging = null
            },
            valueRange = 0f..max,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                formatTime((dragging ?: position.toFloat()).toLong()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                formatTime(duration),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.toggleShuffle() }) {
                Icon(
                    Icons.Rounded.Shuffle, "Aleatório",
                    tint = if (shuffle) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { vm.previous() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.SkipPrevious, "Anterior", Modifier.size(36.dp))
            }
            FilledIconButton(onClick = { vm.togglePlay() }, modifier = Modifier.size(76.dp)) {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (isPlaying) "Pausar" else "Tocar",
                    Modifier.size(44.dp),
                )
            }
            IconButton(onClick = { vm.next() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.SkipNext, "Próxima", Modifier.size(36.dp))
            }
            IconButton(onClick = { vm.cycleRepeat() }) {
                Icon(
                    if (repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    "Repetir",
                    tint = if (repeat != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
