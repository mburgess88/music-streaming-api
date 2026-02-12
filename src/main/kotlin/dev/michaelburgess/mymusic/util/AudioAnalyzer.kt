package dev.michaelburgess.mymusic.util

import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.SampleBuffer
import java.io.FileInputStream
import java.io.File
import kotlin.math.sqrt

class AudioAnalyzer {
    fun analyze(filePath: String, sampleCount: Int = 100): List<Double> {
        val file = File(filePath)
        if (!file.exists() || file.length() < 1000) {
            return generateFallback(sampleCount)
        }

        try {
            val bitstream = Bitstream(FileInputStream(file))
            val decoder = Decoder()
            val allPeaks = mutableListOf<Double>()
            
            var frame = bitstream.readFrame()
            while (frame != null) {
                val output = decoder.decodeFrame(frame, bitstream) as SampleBuffer
                val samples = output.buffer
                
                var sum = 0.0
                for (i in 0 until output.bufferLength) {
                    val s = samples[i].toDouble() / Short.MAX_VALUE
                    sum += s * s
                }
                
                val rms = sqrt(sum / output.bufferLength)
                allPeaks.add(rms)
                
                bitstream.closeFrame()
                frame = bitstream.readFrame()
            }
            
            return processPeaks(allPeaks, sampleCount)
        } catch (e: Exception) {
            println("Error analyzing $filePath: ${e.message}")
            return generateFallback(sampleCount)
        }
    }

    private fun processPeaks(peaks: List<Double>, targetCount: Int): List<Double> {
        if (peaks.isEmpty()) return generateFallback(targetCount)
        
        val result = mutableListOf<Double>()
        val actualCount = peaks.size
        val chunkSize = if (actualCount >= targetCount) actualCount / targetCount else 1
        
        for (i in 0 until targetCount) {
            val start = i * chunkSize
            if (start >= actualCount) {
                result.add(0.1)
                continue
            }
            val end = if (i == targetCount - 1) actualCount else minOf((i + 1) * chunkSize, actualCount)
            val chunk = peaks.subList(start, end)
            if (chunk.isNotEmpty()) {
                result.add(chunk.average())
            } else {
                result.add(0.1)
            }
        }
        
        // Normalize to 0.1 - 1.0
        val max = result.maxOrNull() ?: 1.0
        return result.map { Math.max(0.1, (it / max)) }
    }

    private fun generateFallback(count: Int): List<Double> {
        return (0 until count).map { 
            val h = ((Math.sin(it * 0.4) + Math.cos(it * 0.1) + 2) / 4)
            Math.max(0.1, h * 0.8 + 0.1)
        }
    }
}

fun main() {
    val analyzer = AudioAnalyzer()
    val musicDir = "/Users/michaelburgess/Personal/mymusic/"
    val files = listOf("test.mp3", "summer_vibes.mp3", "night_drive.mp3", "morning_coffee.mp3", "melodic-house-techno-mix.mp3")
    
    files.forEach { fileName ->
        val path = musicDir + fileName
        val peaks = analyzer.analyze(path)
        println("--- WAVEFORM FOR $fileName ---")
        // Output in DynamoDB JSON format for easy copy-pasting into init scripts
        val dynamoJson = peaks.joinToString(separator = ",", prefix = "[", postfix = "]") { 
            "{\"N\":\"${String.format("%.2f", it)}\"}" 
        }
        println(dynamoJson)
        println()
    }
}
