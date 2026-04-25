package ca.ilianokokoro.umihi.music.data.datasources.local

import androidx.room.*
import ca.ilianokokoro.umihi.music.models.Playlist
import ca.ilianokokoro.umihi.music.models.PlaylistInfo
import ca.ilianokokoro.umihi.music.models.PlaylistSongCrossRef
import ca.ilianokokoro.umihi.music.models.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalPlaylistDataSource {
    @Transaction
    @Query("SELECT * FROM playlists")
    suspend fun getAll(): List<Playlist>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: String): Playlist?

    @Query("""SELECT DISTINCT songId FROM PlaylistSongCrossRef WHERE songId IN (:songIds)""")
    suspend fun getSongIdsWithPlaylist(songIds: List<String>): List<String>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun observePlaylistById(playlistId: String): Flow<Playlist?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlistInfo: PlaylistInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(refs: List<PlaylistSongCrossRef>)

    @Transaction
    suspend fun insertPlaylistWithSongs(playlist: Playlist) {
        insertPlaylist(playlist.info)
        insertSongs(playlist.songs)
        val refs = playlist.songs.map { PlaylistSongCrossRef(playlist.info.id, it.youtubeId) }
        insertCrossRefs(refs)
    }

    /** Añadir una canción a una playlist existente */
    @Transaction
    suspend fun addSongToPlaylist(playlistId: String, song: Song) {
        insertSongs(listOf(song))
        insertCrossRefs(listOf(PlaylistSongCrossRef(playlistId, song.youtubeId)))
    }

    /** Eliminar una canción de una playlist (solo la referencia) */
    @Query("DELETE FROM PlaylistSongCrossRef WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    /** Obtener todas las canciones de una playlist (solo IDs) */
    @Query("SELECT songId FROM PlaylistSongCrossRef WHERE playlistId = :playlistId")
    suspend fun getSongIdsForPlaylist(playlistId: String): List<String>

    /** Crear una playlist local vacía */
    @Transaction
    suspend fun createPlaylist(title: String): PlaylistInfo {
        val id = "local_${System.currentTimeMillis()}"
        val info = PlaylistInfo(id = id, title = title, coverHref = "")
        insertPlaylist(info)
        return info
    }

    @Query("SELECT * FROM playlists WHERE id LIKE 'local_%'")
    suspend fun getLocalPlaylists(): List<PlaylistInfo>

    @Query("DELETE FROM playlists")
    suspend fun deleteAll()

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: String)

    @Query("DELETE FROM PlaylistSongCrossRef WHERE playlistId = :playlistId")
    suspend fun deleteCrossRefsByPlaylistId(playlistId: String)

    @Transaction
    suspend fun deleteFullPlaylist(playlistId: String) {
        deleteCrossRefsByPlaylistId(playlistId)
        deletePlaylistById(playlistId)
    }
}
