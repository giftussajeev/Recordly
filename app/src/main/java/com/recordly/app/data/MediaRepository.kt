package com.recordly.app.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.media.MediaMetadataRetriever
import android.util.Log
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

    companion object {
        private const val TAG = "MediaRepository"
    }

    fun getRecordings(saveLocationUri: String): Flow<List<Recording>> = flow {
        val recordings = mutableListOf<Recording>()

        // 1. If user set a custom SAF location, try that first
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
                                        dateAdded = docFile.lastModified() / 1000, // Convert to epoch seconds
                                        resolution = res
                                    )
                                )
                            } catch (e: Exception) {
                                Log.w(TAG, "Could not read metadata for ${docFile.name}: ${e.message}")
                            } finally {
                                retriever.release()
                            }
                        }
                    }
                    recordings.sortByDescending { it.dateAdded }
                    Log.d(TAG, "SAF recordings found: ${recordings.size}")
                    emit(recordings)
                    return@flow
                }
            } catch (e: Exception) {
                Log.w(TAG, "SAF query failed, falling back to MediaStore: ${e.message}")
            }
        }

        // 2. Default: Query MediaStore for videos in Movies/Recordly
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        // Build a robust projection — RESOLUTION might not exist on all devices
        val baseProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED
        )

        // RELATIVE_PATH is only available on Android 10+
        val selection: String?
        val selectionArgs: Array<String>?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Android 10+: filter by RELATIVE_PATH and exclude .tmp files
            selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ? AND ${MediaStore.Video.Media.DISPLAY_NAME} LIKE ? AND ${MediaStore.Video.Media.DISPLAY_NAME} NOT LIKE ?"
            selectionArgs = arrayOf("Movies/Recordly%", "Recordly_%", "%.tmp")
        } else {
            // On Android 8-9: filter by DISPLAY_NAME prefix (no RELATIVE_PATH)
            selection = "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ? AND ${MediaStore.Video.Media.DISPLAY_NAME} NOT LIKE ?"
            selectionArgs = arrayOf("Recordly_%", "%.tmp")
        }

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection, baseProjection, selection, selectionArgs, sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateColumn)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    // Get resolution via MediaMetadataRetriever for reliability
                    val resolution = try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(context, contentUri)
                        val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) ?: ""
                        val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) ?: ""
                        retriever.release()
                        if (w.isNotEmpty() && h.isNotEmpty()) "${w}x${h}" else "Unknown"
                    } catch (_: Exception) {
                        "Unknown"
                    }

                    // Skip 0-byte files
                    if (size > 0) {
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore query failed: ${e.message}", e)
        }

        Log.d(TAG, "MediaStore recordings found: ${recordings.size}")
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
            Log.e(TAG, "Delete failed: ${e.message}", e)
        }
    }

    fun renameRecording(uri: Uri, newName: String) {
        try {
            if (uri.scheme == "content" && uri.authority != MediaStore.AUTHORITY) {
                // DocumentFile URI
                androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)?.renameTo(newName)
            } else {
                // MediaStore URI
                val values = android.content.ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, newName)
                }
                context.contentResolver.update(uri, values, null, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Rename failed: ${e.message}", e)
        }
    }
}
