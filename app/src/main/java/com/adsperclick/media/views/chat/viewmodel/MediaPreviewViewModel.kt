package com.adsperclick.media.views.chat.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class MediaPreviewViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _downloadState = MutableLiveData<DownloadState>()
    val downloadState: LiveData<DownloadState> = _downloadState

    private val context = getApplication<Application>()
//    private val downloadDirectory: File = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
//        ?: File(context.filesDir, "downloads").also { it.mkdirs() }

    val downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//    val file = File(downloadDirectory, "myfile.jpg")

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Int) : DownloadState()
        data class Success(val file: File?) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    fun downloadMedia(url: String, fileName: String): LiveData<File?> {
        val result = MutableLiveData<File?>()

        // Check if file already exists
        val file = getDownloadedFile(url, fileName)
        if (file != null && file.exists() && file.length() > 0) {
            _downloadState.postValue(DownloadState.Success(file))
            result.postValue(file)
            return result
        }

        _downloadState.value = DownloadState.Downloading(0)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Ensure filename has no invalid characters
                val sanitizedFileName = sanitizeFileName(fileName)

                // Create a unique file name to avoid conflicts
                val targetFile = File(downloadDirectory, sanitizedFileName)

                // Start download
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    withContext(Dispatchers.Main) {
                        _downloadState.value = DownloadState.Error("Server returned ${connection.responseCode}")
                        result.value = null
                    }
                    return@launch
                }

                val fileLength = connection.contentLength

                // Download the file
                connection.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(4 * 1024) // 4K buffer size
                        var bytesRead: Int
                        var totalBytesRead: Long = 0

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)

                            totalBytesRead += bytesRead
                            if (fileLength > 0) {
                                val progress = (totalBytesRead * 100 / fileLength).toInt()
                                withContext(Dispatchers.Main) {
                                    _downloadState.value = DownloadState.Downloading(progress)
                                }
                            }
                        }

                        output.flush()
                    }
                }

                withContext(Dispatchers.Main) {
                    _downloadState.value = DownloadState.Success(targetFile)
                    result.value = targetFile
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _downloadState.value = DownloadState.Error(e.message ?: "Unknown error occurred")
                    result.value = null
                }
            }
        }

        return result
    }

    fun getDownloadedFile(url: String?, fileName: String): File? {
        if (url == null) return null

        val sanitizedFileName = sanitizeFileName(fileName)
        val file = File(downloadDirectory, sanitizedFileName)

        return if (file.exists() && file.length() > 0) file else null
    }

    private fun sanitizeFileName(fileName: String): String {
        // Remove invalid file characters
        var sanitized = fileName.replace("[\\\\/:*?\"<>|]".toRegex(), "_")

        // Add extension if missing based on known patterns
        if (!sanitized.contains(".")) {
            if (sanitized.startsWith("image", ignoreCase = true)) {
                sanitized += ".jpg"
            } else if (sanitized.startsWith("video", ignoreCase = true)) {
                sanitized += ".mp4"
            } else if (sanitized.startsWith("document", ignoreCase = true)) {
                sanitized += ".pdf"
            }
        }

        return sanitized
    }
}