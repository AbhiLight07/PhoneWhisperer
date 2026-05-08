package com.phonewhisperer.ai_engine.llm

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val MODEL_URL = "https://storage.googleapis.com/mediapipe-models/llm_inference/gemma_2b_en_cpu_int8/float16/1/gemma_2b_en_cpu_int8.bin"
    private val FILE_NAME = "gemma_2b_en_cpu_int8.bin"

    // The destination is the app's external files directory so MediaPipe can access it
    private val destinationFile by lazy {
        File(context.getExternalFilesDir(null), FILE_NAME)
    }

    fun isModelDownloaded(): Boolean {
        return destinationFile.exists() && destinationFile.length() > 100_000_000 // roughly check size
    }

    fun getModelPath(): String {
        return destinationFile.absolutePath
    }

    fun startDownload(): Long {
        if (isModelDownloaded()) return -1L

        val request = DownloadManager.Request(Uri.parse(MODEL_URL))
            .setTitle("Downloading Offline AI Model")
            .setDescription("Gemma 2B (MediaPipe)")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, null, FILE_NAME)
            .setAllowedOverMetered(false) // Only download over WiFi since it's 1.5GB
            .setAllowedOverRoaming(false)

        return downloadManager.enqueue(request)
    }

    /**
     * Emits download progress as a percentage [0, 100].
     */
    fun observeDownloadProgress(downloadId: Long): Flow<Int> = flow {
        var downloading = true
        while (downloading) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor: Cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                if (statusIndex != -1 && bytesDownloadedIndex != -1 && bytesTotalIndex != -1) {
                    val status = cursor.getInt(statusIndex)
                    val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                    val bytesTotal = cursor.getLong(bytesTotalIndex)

                    if (bytesTotal > 0) {
                        val progress = ((bytesDownloaded * 100L) / bytesTotal).toInt()
                        emit(progress)
                    }

                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                        downloading = false
                    }
                }
            }
            cursor.close()
            if (downloading) delay(1000)
        }
    }.flowOn(Dispatchers.IO)
}
