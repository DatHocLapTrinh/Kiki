package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.sqlite.room.ResearchTelemetryEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tiện ích trích xuất và xuất khẩu dữ liệu thực nghiệm (Research Telemetry Exporter)
 * Phục vụ nghiên cứu khoa học, luận văn và phân tích dữ liệu trên Python/R/SPSS.
 */
object ResearchTelemetryExporter {

    private const val CSV_HEADER =
        "session_id,user_id,timestamp,slo_topic,challenge_type,item_identifier,response_latency_ms,is_correct,used_hint,is_distracted,objective_quality,easiness_factor,interval_days,retention_probability\n"

    /**
     * Chuyển đổi danh sách thực thể telemetry sang chuỗi CSV chuẩn RFC 4180
     */
    fun exportToCsv(telemetryList: List<ResearchTelemetryEntity>): String {
        val sb = StringBuilder()
        sb.append(CSV_HEADER)

        for (item in telemetryList) {
            sb.append(escapeCsv(item.sessionId)).append(",")
            sb.append(item.userId).append(",")
            sb.append(escapeCsv(item.timestamp)).append(",")
            sb.append(escapeCsv(item.sloTopic)).append(",")
            sb.append(escapeCsv(item.challengeType)).append(",")
            sb.append(escapeCsv(item.itemIdentifier)).append(",")
            sb.append(item.responseLatencyMs).append(",")
            sb.append(if (item.isCorrect) 1 else 0).append(",")
            sb.append(if (item.usedHint) 1 else 0).append(",")
            sb.append(if (item.isDistracted) 1 else 0).append(",")
            sb.append(item.objectiveQuality).append(",")
            sb.append(String.format(Locale.US, "%.2f", item.easinessFactor)).append(",")
            sb.append(item.intervalDays).append(",")
            sb.append(String.format(Locale.US, "%.4f", item.retentionProbability))
            sb.append("\n")
        }

        return sb.toString()
    }

    /**
     * Lưu file CSV vào bộ nhớ cache của ứng dụng
     */
    fun saveCsvToFile(context: Context, telemetryList: List<ResearchTelemetryEntity>): File {
        val fileName = "kiki_research_telemetry_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv"
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val file = File(exportDir, fileName)
        val csvContent = exportToCsv(telemetryList)
        FileOutputStream(file).use { out ->
            out.write(csvContent.toByteArray(Charsets.UTF_8))
        }
        return file
    }

    /**
     * Tạo Intent chia sẻ file CSV qua các ứng dụng ngoài (Email, Drive, Chat)
     */
    fun createShareIntent(context: Context, csvFile: File): Intent {
        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )
        } catch (_: Exception) {
            android.net.Uri.fromFile(csvFile)
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Kiki Microlearning Research Telemetry Dataset")
            putExtra(
                Intent.EXTRA_TEXT,
                "Dữ liệu thực nghiệm Kiki (Microlearning & Objective SM-2 Telemetry Dataset) phục vụ nghiên cứu khoa học."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\n", " ").replace("\r", " ").trim()
        return if (clean.contains(",") || clean.contains("\"") || clean.contains(";")) {
            "\"${clean.replace("\"", "\"\"")}\""
        } else {
            clean
        }
    }
}
