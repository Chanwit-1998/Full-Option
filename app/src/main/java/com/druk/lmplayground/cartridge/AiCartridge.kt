package com.druk.ai_offline_llm.cartridge

import org.json.JSONArray
import org.json.JSONObject

data class CartridgeKnowledgeEntry(
    val topic: String,
    val content: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("topic", topic)
        put("content", content)
    }

    companion object {
        fun fromAny(raw: Any?): CartridgeKnowledgeEntry? = when (raw) {
            is JSONObject -> {
                val t = raw.optString("topic").ifBlank { raw.optString("title", "ข้อมูลอ้างอิง") }
                val c = raw.optString("content").ifBlank { raw.optString("text", "") }
                if (c.isNotBlank()) CartridgeKnowledgeEntry(t.trim(), c.trim()) else null
            }
            is String -> {
                val s = raw.trim()
                if (s.isEmpty()) null
                else if (s.contains(":")) {
                    val idx = s.indexOf(":")
                    val head = s.substring(0, idx).trim()
                    val body = s.substring(idx + 1).trim()
                    if (head.length in 1..60 && body.isNotEmpty()) CartridgeKnowledgeEntry(head, body)
                    else CartridgeKnowledgeEntry("ข้อมูลอ้างอิง", s)
                } else CartridgeKnowledgeEntry("ข้อมูลอ้างอิง", s)
            }
            else -> null
        }
    }
}

data class AiCartridge(
    val id: String,
    val name: String,
    val version: String = "v1.0",
    val author: String = "Chanwit™",
    val category: String = "คลังความรู้",
    val description: String,
    val knowledge: List<CartridgeKnowledgeEntry> = emptyList(),
    val colorHex: Long = 0xFF4CAF50,
    val isCustom: Boolean = false,
    val bestFor: String = "",
    val usageTips: String = "",
    val samplePrompt: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("version", version)
        put("author", author)
        put("category", category)
        put("description", description)
        put("knowledge", JSONArray().apply { knowledge.forEach { put(it.toJson()) } })
        put("colorHex", colorHex)
        put("isCustom", isCustom)
        put("bestFor", bestFor)
        put("usageTips", usageTips)
        put("samplePrompt", samplePrompt)
    }

    companion object {
        fun parseRawKnowledgeText(rawText: String): List<CartridgeKnowledgeEntry> {
            val blocks = rawText.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
            if (blocks.size > 1) {
                return blocks.mapIndexedNotNull { idx, block ->
                    val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    if (lines.size >= 2 && lines[0].length <= 60) {
                        CartridgeKnowledgeEntry(lines[0].removeSuffix(":"), lines.drop(1).joinToString("\n"))
                    } else {
                        CartridgeKnowledgeEntry.fromAny(block)?.copy(
                            topic = if (block.contains(":")) block.substringBefore(":").trim() else "หัวข้อที่ ${idx + 1}"
                        )
                    }
                }
            }
            return rawText.lines().mapIndexedNotNull { idx, line ->
                CartridgeKnowledgeEntry.fromAny(line)?.let { entry ->
                    if (entry.topic == "ข้อมูลอ้างอิง") entry.copy(topic = "ข้อมูลที่ ${idx + 1}") else entry
                }
            }
        }

        fun fromJson(obj: JSONObject): AiCartridge {
            val entries = mutableListOf<CartridgeKnowledgeEntry>()
            val kArr = obj.optJSONArray("knowledge")
            if (kArr != null) {
                for (i in 0 until kArr.length()) CartridgeKnowledgeEntry.fromAny(kArr.opt(i))?.let { entries.add(it) }
            }
            if (entries.isEmpty()) {
                val fallback = obj.optString("knowledgeText").ifBlank { obj.optString("content") }.ifBlank { obj.optString("systemPrompt") }
                if (fallback.isNotBlank()) entries.addAll(parseRawKnowledgeText(fallback))
            }
            return AiCartridge(
                id = obj.optString("id", "cart_${System.currentTimeMillis()}"),
                name = obj.optString("name", "ตลับความรู้ไม่มีชื่อ"),
                version = obj.optString("version", "v1.0"),
                author = obj.optString("author", "Chanwit™"),
                category = obj.optString("category", "คลังความรู้"),
                description = obj.optString("description", ""),
                knowledge = entries,
                colorHex = obj.optLong("colorHex", 0xFF4CAF50),
                isCustom = obj.optBoolean("isCustom", false),
                bestFor = obj.optString("bestFor", ""),
                usageTips = obj.optString("usageTips", ""),
                samplePrompt = obj.optString("samplePrompt", "")
            )
        }

        fun listToJsonString(list: List<AiCartridge>): String = JSONArray().apply { list.forEach { put(it.toJson()) } }.toString(2)
        fun listFromJsonString(raw: String): List<AiCartridge> = runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }
}
