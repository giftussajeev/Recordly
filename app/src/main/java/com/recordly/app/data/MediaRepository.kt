package com.recordly.app.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

data class Recording(
    val id: Long,
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAdded: Long,
    val resolution: String
)

class MediaRepository(private val context: Context) {
    fun getRecordings(saveLocationUri: String): Flow<List<Recording>> = flow {
        val recordings = mutableListOf<Recording>()
        
        if (saveLocationUri.isNotEmpty()) {
            try {
                val treeUri = Uri.parse(saveLocationUri)
                val docTree = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
                if (docTree != null && docTree.isDirectory) {
                    docTree.listFiles().forEach { docFile: androidx.documentfile.provider.DocumentFile ->
                        if (docFile.isFile && docFile.name?.endsWith(".mp4") == true) {
                            val retriever = MediaMetadataRetriever()
                            try {
                                retriever.setDataSource(context, docFile.uri)
                                val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) ?: ""
                                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) ?: ""
                                val res = if (width.isNotEmpty() && height.isNotEmpty()) "${width}x${height}" else "Unknown"
                                recordings.add(
                                    Recording(
                                        id = docFile.uri.hashCode().toLong(),
                                        uri = docFile.uri,
                                        name = docFile.name ?: "Unknown",
                                        durationMs = dur,
                                        sizeBytes = docFile.length(),
                                        dateAdded = docFile.lastModified(),
                                        resolution = res
                                    )
                                )
                            } catch (e: Exception) {
                                // ignore
                            } finally {
                                retriever.release()
                            }
                        }
                    }
                    recordings.sortByDescending { it.dateAdded }
                    emit(recordings)
                    return@flow
                }
            } catch (e: Exception) {
                // fallback to default
            }
        }

        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.RESOLUTION
        )
        val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%Movies/Recordly%")
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection, projection, selection, selectionArgs, sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val resColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Unknown"
                val duration = cursor.getLong(durationColumn)
                val size = cursor.getLong(sizeColumn)
                val dateAdded = cursor.getLong(dateColumn)
                val resolution = cursor.getString(resColumn) ?: "Unknown"
                val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                recordings.add(
                    Recording(
                        id = id,
                        uri = contentUri,
                        name = name,
                        durationMs = duration,
                        sizeBytes = size,
                        dateAdded = dateAdded,
                        resolution = resolution
                    )
                )
            }
        }
        emit(recordings)
    }.flowOn(Dispatchers.IO)
    
    fun deleteRecording(uri: Uri) {
        try {
            if (uri.scheme == "content" && uri.authority != MediaStore.AUTHORITY) {
                // Likely a DocumentFile URI
                androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)?.delete()
            } else {
                context.contentResolver.delete(uri, null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
