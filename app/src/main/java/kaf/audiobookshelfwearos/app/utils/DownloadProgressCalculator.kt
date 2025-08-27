package kaf.audiobookshelfwearos.app.utils

object DownloadProgressCalculator {
    private val speedHistory = mutableMapOf<String, MutableList<Pair<Long, Long>>>()
    private val smoothedSpeeds = mutableMapOf<String, Long>()
    
    fun calculateDownloadSpeed(trackId: String, bytesDownloaded: Long): Long {
        val currentTime = System.currentTimeMillis()
        val history = speedHistory.getOrPut(trackId) { mutableListOf() }
        
        // Only add if we have actual progress
        if (bytesDownloaded > 0) {
            history.add(Pair(currentTime, bytesDownloaded))
        }
        
        // Keep only last 30 seconds of data for better smoothing
        history.removeAll { (time, _) -> currentTime - time > 30000 }
        
        if (history.size < 2) return smoothedSpeeds[trackId] ?: 0L
        
        // Calculate speed over the entire history period for stability
        val (oldTime, oldBytes) = history.first()
        val (newTime, newBytes) = history.last()
        
        val timeDiff = (newTime - oldTime) / 1000.0
        val bytesDiff = newBytes - oldBytes
        
        val instantSpeed = if (timeDiff > 0 && bytesDiff >= 0) {
            (bytesDiff / timeDiff).toLong().coerceAtLeast(0L)
        } else {
            0L
        }
        
        // Apply exponential smoothing for more stable estimates
        val previousSmoothedSpeed = smoothedSpeeds[trackId] ?: instantSpeed
        val smoothingFactor = 0.3 // Lower value = more smoothing
        val newSmoothedSpeed = if (instantSpeed > 0) {
            ((1 - smoothingFactor) * previousSmoothedSpeed + smoothingFactor * instantSpeed).toLong()
        } else {
            previousSmoothedSpeed
        }
        
        smoothedSpeeds[trackId] = newSmoothedSpeed
        return newSmoothedSpeed
    }
    
    fun calculateEstimatedTime(remainingBytes: Long, downloadSpeed: Long): Long {
        return if (downloadSpeed > 1000) { // Only estimate if speed is reasonable (>1KB/s)
            val estimatedSeconds = remainingBytes / downloadSpeed
            // Cap at 24 hours and ensure minimum of 1 second
            estimatedSeconds.coerceIn(1L, 86400L)
        } else {
            Long.MAX_VALUE
        }
    }
    
    fun formatTime(seconds: Long): String {
        return when {
            seconds == Long.MAX_VALUE -> "Unknown"
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> {
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60
                if (remainingSeconds == 0L) "${minutes}m" else "${minutes}m ${remainingSeconds}s"
            }
            else -> {
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                if (minutes == 0L) "${hours}h" else "${hours}h ${minutes}m"
            }
        }
    }
    
    fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return "%.1f %s".format(size, units[unitIndex])
    }
    
    fun clearSpeedHistory(trackId: String) {
        speedHistory.remove(trackId)
        smoothedSpeeds.remove(trackId)
    }
}
