package com.example.securemate.wifi_scanner

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * Network speed test implementation to measure download and upload speeds
 */
class NetworkSpeedTester(private val context: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private val _speedTestState = MutableLiveData<SpeedTestState>(SpeedTestState.IDLE)
    val speedTestState: LiveData<SpeedTestState> = _speedTestState

    // Default URLs for speed testing - these should be replaced with actual speed test endpoints
    private val downloadTestUrl = "https://speed.cloudflare.com/__down?bytes=10000000"
    private val uploadTestUrl = "https://speed.cloudflare.com/__up"

    // Size of data to upload in bytes (5MB)
    private val uploadDataSize = 5 * 1024 * 1024

    // Test history storage
    private val speedTestHistoryPrefs = context.getSharedPreferences("speed_test_history", Context.MODE_PRIVATE)

    /**
     * Run a full speed test (download and upload)
     */
    fun runSpeedTest() {
        executor.execute {
            try {
                _speedTestState.postValue(SpeedTestState.TESTING(0, TestType.DOWNLOAD))
                val downloadSpeed = measureDownloadSpeed()

                _speedTestState.postValue(SpeedTestState.TESTING(0, TestType.UPLOAD))
                val uploadSpeed = measureUploadSpeed()

                val result = SpeedTestResult(
                    downloadSpeedMbps = downloadSpeed,
                    uploadSpeedMbps = uploadSpeed,
                    timestamp = System.currentTimeMillis()
                )

                saveTestResult(result)
                _speedTestState.postValue(SpeedTestState.COMPLETE(result))
            } catch (e: Exception) {
                _speedTestState.postValue(SpeedTestState.ERROR(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Measure download speed
     */
    private fun measureDownloadSpeed(): Double {
        var connection: HttpURLConnection? = null
        try {
            val startTime = System.currentTimeMillis()
            var totalBytesRead = 0L

            val url = URL(downloadTestUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000 // 10 seconds
            connection.readTimeout = 20000 // 20 seconds
            connection.connect()

            val inputStream = connection.inputStream
            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                totalBytesRead += bytesRead
                val currentTime = System.currentTimeMillis()
                val elapsedSeconds = (currentTime - startTime) / 1000.0
                if (elapsedSeconds > 0) {
                    val currentSpeedMbps = (totalBytesRead * 8.0) / (elapsedSeconds * 1000000.0)
                    val progressPercent = minOf((elapsedSeconds / 10.0 * 100).roundToInt(), 100)
                    _speedTestState.postValue(SpeedTestState.TESTING(progressPercent, TestType.DOWNLOAD))
                }
                // Stop after 10 seconds or 100MB
                if (System.currentTimeMillis() - startTime >= 10000 || totalBytesRead >= 100 * 1024 * 1024) {
                    break
                }
            }

            inputStream.close()

            val endTime = System.currentTimeMillis()
            val totalTimeSeconds = (endTime - startTime) / 1000.0
            val speedMbps = (totalBytesRead * 8.0) / (totalTimeSeconds * 1000000.0)

            return speedMbps.roundToDecimals(2)
        } catch (e: IOException) {
            throw IOException("Download test failed: ${e.message}")
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Measure upload speed (simulated for demonstration)
     */
    private fun measureUploadSpeed(): Double {
        var connection: HttpURLConnection? = null
        try {
            val startTime = System.currentTimeMillis()

            // Create random data for upload
            val data = ByteArray(uploadDataSize)
            for (i in data.indices) {
                data[i] = (i % 256).toByte()
            }

            val url = URL(uploadTestUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 10000 // 10 seconds
            connection.readTimeout = 20000 // 20 seconds
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.connect()

            val outputStream = connection.outputStream

            // Upload data in chunks to show progress
            val chunkSize = 64 * 1024 // 64KB chunks
            var bytesSent = 0L

            while (bytesSent < data.size) {
                val bytesToSend = minOf(chunkSize.toLong(), data.size - bytesSent).toInt()
                outputStream.write(data, bytesSent.toInt(), bytesToSend)
                outputStream.flush()

                bytesSent += bytesToSend

                val currentTime = System.currentTimeMillis()
                val elapsedSeconds = (currentTime - startTime) / 1000.0
                if (elapsedSeconds > 0) {
                    val currentSpeedMbps = (bytesSent * 8.0) / (elapsedSeconds * 1000000.0)
                    val progressPercent = minOf((bytesSent.toDouble() / data.size * 100).roundToInt(), 100)
                    _speedTestState.postValue(SpeedTestState.TESTING(progressPercent, TestType.UPLOAD))
                }

                // Simulate network latency
                Thread.sleep(50)
            }

            outputStream.close()

            // Read the response (important for HTTP compliance)
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Unexpected response: $responseCode")
            }

            val endTime = System.currentTimeMillis()
            val totalTimeSeconds = (endTime - startTime) / 1000.0
            val speedMbps = (bytesSent * 8.0) / (totalTimeSeconds * 1000000.0)

            return speedMbps.roundToDecimals(2)
        } catch (e: IOException) {
            throw IOException("Upload test failed: ${e.message}")
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Save test result to history
     */
    private fun saveTestResult(result: SpeedTestResult) {
        val history = getSpeedTestHistory().toMutableList()
        history.add(0, result) // Add at beginning

        // Keep only the last 10 results
        while (history.size > 10) {
            history.removeAt(history.size - 1)
        }

        // Save to SharedPreferences
        speedTestHistoryPrefs.edit {
            clear()
            for (i in history.indices) {
                val test = history[i]
                putLong("timestamp_$i", test.timestamp)
                putFloat("download_$i", test.downloadSpeedMbps.toFloat())
                putFloat("upload_$i", test.uploadSpeedMbps.toFloat())
            }
            putInt("count", history.size)
        }
    }

    /**
     * Get speed test history
     */
    fun getSpeedTestHistory(): List<SpeedTestResult> {
        val count = speedTestHistoryPrefs.getInt("count", 0)
        val history = mutableListOf<SpeedTestResult>()

        for (i in 0 until count) {
            val timestamp = speedTestHistoryPrefs.getLong("timestamp_$i", 0)
            val download = speedTestHistoryPrefs.getFloat("download_$i", 0f).toDouble()
            val upload = speedTestHistoryPrefs.getFloat("upload_$i", 0f).toDouble()

            history.add(
                SpeedTestResult(
                    downloadSpeedMbps = download,
                    uploadSpeedMbps = upload,
                    timestamp = timestamp
                )
            )
        }

        return history
    }

    /**
     * Get average speeds from history
     */
    fun getAverageSpeedFromHistory(): Pair<Double, Double> {
        val history = getSpeedTestHistory()
        if (history.isEmpty()) {
            return Pair(0.0, 0.0)
        }

        val totalDownload = history.sumOf { it.downloadSpeedMbps }
        val totalUpload = history.sumOf { it.uploadSpeedMbps }

        return Pair(
            (totalDownload / history.size).roundToDecimals(2),
            (totalUpload / history.size).roundToDecimals(2)
        )
    }
}

/**
 * Round a double to a specified number of decimal places
 */
fun Double.roundToDecimals(decimals: Int): Double {
    val factor = Math.pow(10.0, decimals.toDouble())
    return (this * factor).roundToInt() / factor
}

/**
 * Speed test result data class
 */
data class SpeedTestResult(
    val downloadSpeedMbps: Double,
    val uploadSpeedMbps: Double,
    val timestamp: Long
)

/**
 * Test type enum
 */
enum class TestType {
    DOWNLOAD,
    UPLOAD
}

/**
 * Speed test state sealed class
 */
sealed class SpeedTestState {
    data object IDLE : SpeedTestState()
    data class TESTING(val progressPercent: Int, val type: TestType) : SpeedTestState()
    data class COMPLETE(val result: SpeedTestResult) : SpeedTestState()
    data class ERROR(val message: String) : SpeedTestState()
}