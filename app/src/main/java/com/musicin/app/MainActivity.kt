package com.musicin.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicinTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val vm: PlayerViewModel = viewModel()
                    MusicinRoot(vm)
                }
            }
        }
    }
}

@Composable
fun MusicinRoot(vm: PlayerViewModel) {
    val ctx = LocalContext.current
    val audioPerm = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val perms = if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(audioPerm, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(audioPerm)
    }
    fun hasAudio() = ContextCompat.checkSelfPermission(ctx, audioPerm) == PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(hasAudio()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasAudio()
        if (granted) vm.loadSongs()
    }

    LaunchedEffect(Unit) {
        if (granted) vm.loadSongs() else launcher.launch(perms)
    }

    if (granted) {
        MusicinApp(vm)
    } else {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    Icons.Rounded.LibraryMusic, null,
                    Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary,
                )
                Text("Bem-vindo ao Musicin", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Para tocar as músicas que estão no seu telefone, o Musicin precisa de permissão para acessar os arquivos de áudio.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Button(onClick = { launcher.launch(perms) }) { Text("Permitir acesso") }
                TextButton(onClick = {
                    ctx.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")),
                    )
                }) { Text("Abrir configurações do app") }
            }
        }
    }
}

@Composable
fun MusicinApp(vm: PlayerViewModel) {
    val songs by vm.songs.collectAsState()
    val loading by vm.loading.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val current by vm.currentSong.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var openPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var addTarget by remember { mutableStateOf<Song?>(null) }
    val ctx = LocalContext.current

    // atualiza a lista quando o app volta para a tela (músicas novas)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.loadSongs() }

    BackHandler(enabled = showPlayer) { showPlayer = false }
    BackHandler(enabled = !showPlayer && tab == 1 && openPlaylistId != null) { openPlaylistId = null }

    if (showPlayer && current != null) {
        PlayerScreen(vm = vm, onClose = { showPlayer = false })
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Column {
                    current?.let { song ->
                        MiniPlayer(song = song, isPlaying = isPlaying, vm = vm, onOpen = { showPlayer = true })
                    }
                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Rounded.LibraryMusic, null) },
                            label = { Text("Músicas") },
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(Icons.Rounded.QueueMusic, null) },
                            label = { Text("Playlists") },
                        )
                    }
                }
            },
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                if (tab == 0) {
                    SongsTab(
                        vm = vm,
                        songs = songs,
                        loading = loading,
                        currentId = current?.id,
                        onAddToPlaylist = { addTarget = it },
                    )
                } else {
                    val open = playlists.firstOrNull { it.id == openPlaylistId }
                    if (open != null) {
                        PlaylistDetail(
                            vm = vm,
                            playlist = open,
                            songs = songs,
                            currentId = current?.id,
                            onBack = { openPlaylistId = null },
                        )
                    } else {
                        PlaylistsTab(
                            vm = vm,
                            playlists = playlists,
                            songs = songs,
                            onOpen = { openPlaylistId = it },
                        )
                    }
                }
            }
        }
    }

    addTarget?.let { song ->
        AddToPlaylistDialog(
            playlists = playlists,
            onPick = { p ->
                val ok = vm.addToPlaylist(p.id, song.id)
                toast(ctx, if (ok) "Adicionada a \"${p.name}\"" else "Essa música já está em \"${p.name}\"")
                addTarget = null
            },
            onCreate = { name ->
                vm.createPlaylist(name, song)
                toast(ctx, "Playlist \"$name\" criada")
                addTarget = null
            },
            onDismiss = { addTarget = null },
        )
    }
}
