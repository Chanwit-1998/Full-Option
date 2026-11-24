package com.druk.ai_offline_llm.tools

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object SystemClockTool {

    private val thaiLocale = Locale("th", "TH")

    private val thaiDayOfWeek = mapOf(
        1 to "วันจันทร์",
        2 to "วันอังคาร",
        3 to "วันพุธ",
        4 to "วันพฤหัสบดี",
        5 to "วันศุกร์",
        6 to "วันเสาร์",
        7 to "วันอาทิตย์"
    )

    private val thaiMonth = mapOf(
        1 to "มกราคม",
        2 to "กุมภาพันธ์",
        3 to "มีนาคม",
        4 to "เมษายน",
        5 to "พฤษภาคม",
        6 to "มิถุนายน",
        7 to "กรกฎาคม",
        8 to "สิงหาคม",
        9 to "กันยายน",
        10 to "ตุลาคม",
        11 to "พฤศจิกายน",
        12 to "ธันวาคม"
    )

    fun now(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
        return Instant.now().atZone(zoneId).toLocalDateTime()
    }

    fun formatThaiDate(date: LocalDate): String {
        val dayName = thaiDayOfWeek[date.dayOfWeek.value] ?: ""
        val monthName = thaiMonth[date.monthValue] ?: ""
        val buddhistYear = date.year + 543
        return dayName + "ที่ " + date.dayOfMonth + " " + monthName + " พ.ศ. " + buddhistYear + " (ค.ศ. " + date.year + ")"
    }

    fun formatCurrentDateTimeSummary(zoneId: ZoneId = ZoneId.systemDefault()): String {
        val nowDt = now(zoneId)
        val timeStr = nowDt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US))
        val isoDate = nowDt.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return buildString {
            appendLine("เวลาปัจจุบันจากระบบเครื่อง (100% Offline):")
            appendLine("- วันที่: ${formatThaiDate(nowDt.toLocalDate())} [$isoDate]")
            appendLine("- เดือนนี้: ${thaiMonth[nowDt.monthValue]} (เดือนที่ ${nowDt.monthValue})")
            appendLine("- เวลา: $timeStr น.")
            appendLine("- เขตเวลา: ${zoneId.id}")
        }.trim()
    }

    fun offsetDays(days: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val base = now(zoneId).toLocalDate()
        val target = base.plusDays(days)
        val relation = when {
            days == 0L -> "วันนี้"
            days == 1L -> "พรุ่งนี้ (+1 วัน)"
            days == -1L -> "เมื่อวาน (-1 วัน)"
            days > 0L -> "อีก $days วันข้างหน้า"
            else -> "ย้อนหลัง ${kotlin.math.abs(days)} วันก่อน"
        }
        return "$relation (${base.format(DateTimeFormatter.ISO_LOCAL_DATE)}) -> ${formatThaiDate(target)} [${target.format(DateTimeFormatter.ISO_LOCAL_DATE)}]"
    }

    fun daysBetween(startIsoDate: String, endIsoDate: String): String? {
        return runCatching {
            val start = LocalDate.parse(startIsoDate.trim())
            val end = LocalDate.parse(endIsoDate.trim())
            val diff = ChronoUnit.DAYS.between(start, end)
            "จำนวนวันระหว่าง $startIsoDate ถึง $endIsoDate คือ $diff วัน"
        }.getOrNull()
    }

    fun buildContextSnippetForPrompt(userPrompt: String = ""): String {
        val zone = ZoneId.systemDefault()
        val nowDt = now(zone)
        val today = nowDt.toLocalDate()
        val timeStr = nowDt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))
        val yesterday = today.minusDays(1)
        val tomorrow = today.plusDays(1)

        val sb = StringBuilder()
        sb.append("[SYSTEM CLOCK - DEVICE LOCAL TIME]")
        sb.append(" วันนี้: ${formatThaiDate(today)} (${today})")
        sb.append(" | เดือนนี้: ${thaiMonth[today.monthValue]}")
        sb.append(" | เวลา: $timeStr น. (${zone.id})")

        val lower = userPrompt.lowercase(thaiLocale)
        if (lower.contains("พรุ่งนี้") || lower.contains("tomorrow")) {
            sb.append(" | พรุ่งนี้: ${formatThaiDate(tomorrow)} (${tomorrow})")
        }
        if (lower.contains("เมื่อวาน") || lower.contains("yesterday")) {
            sb.append(" | เมื่อวาน: ${formatThaiDate(yesterday)} (${yesterday})")
        }
        sb.append(" ใช้ข้อมูลเวลาจากระบบเครื่องเป็นหลักเสมอ ห้ามเดาวันที่หรือเดือนเอง")
        return sb.toString()
    }
}
