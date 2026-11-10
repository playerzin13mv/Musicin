package com.musicin.app

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
) {
    val uri: Uri
        get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
}

data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<Long>,
)

object MusicRepository {
    /** Lê todas as músicas guardadas no telefone (ignora toques, alarmes e áudios muito curtos). */
    fun load(context: Context): List<Song> {
        val out = mutableListOf<Song>()
        val collection: Uri = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
        )
        val selection = "${MediaStore.Audio.Media.DURATION} >= 20000 AND " +
            "${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND " +
            "${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND " +
            "${MediaStore.Audio.Media.IS_ALARM} = 0"
        val order = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(collection, projection, selection, null, order)?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (c.moveToNext()) {
                    val artist = c.getString(iArtist)?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: "Artista desconhecido"
                    val album = c.getString(iAlbum)?.takeIf { it.isNotBlank() } ?: "Álbum desconhecido"
                    out += Song(
                        id = c.getLong(iId),
                        title = c.getString(iTitle)?.takeIf { it.isNotBlank() } ?: "Sem título",
                        artist = artist,
                        album = album,
                        albumId = c.getLong(iAlbumId),
                        durationMs = c.getLong(iDur),
                    )
                }
            }
        } catch (e: Exception) {
            // sem permissão ou erro de leitura: devolve o que tiver
        }
        return out
    }
}

class PlaylistStore(context: Context) {
    private val prefs = context.getSharedPreferences("musicin_playlists", Context.MODE_PRIVATE)

    fun load(): List<Playlist> = try {
        val arr = JSONArray(prefs.getString("data", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val ids = o.getJSONArray("songs")
            Playlist(
                id = o.getString("id"),
                name = o.getString("name"),
                songIds = (0 until ids.length()).map { ids.getLong(it) },
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun save(list: List<Playlist>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("songs", JSONArray(p.songIds)),
            )
        }
        prefs.edit().putString("data", arr.toString()).apply()
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
