package com.druk.ai_offline_llm.rag

import java.text.BreakIterator
import java.util.Locale

object TextChunker {

    data class Chunk(val text: String, val ordinal: Int)

    private val PARAGRAPH_SPLIT = Regex("\\n{2,}")
    private val THAI_LOCALE = Locale("th", "TH")

    fun chunk(
        text: String,
        targetChars: Int = 1000,
        overlapChars: Int = 150,
        maxChars: Int = 1600,
    ): List<Chunk> {
        require(targetChars > 0 && maxChars >= targetChars && overlapChars < targetChars)

        val pieces = text.split(PARAGRAPH_SPLIT)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .flatMap { paragraph -> splitOversized(paragraph, maxChars) }
        if (pieces.isEmpty()) return emptyList()

        val chunks = mutableListOf<String>()
        val current = StringBuilder()
        for (piece in pieces) {
            if (current.isNotEmpty() && current.length + piece.length + 1 > targetChars) {
                chunks.add(current.toString())
                val tail = overlapTail(current.toString(), overlapChars)
                current.setLength(0)
                if (tail.isNotEmpty()) current.append(tail)
            }
            if (current.isNotEmpty()) current.append('\n')
            current.append(piece)
        }
        if (current.isNotBlank()) chunks.add(current.toString())

        return chunks.mapIndexed { index, chunkText -> Chunk(chunkText, index) }
    }

    private fun splitOversized(paragraph: String, maxChars: Int): List<String> {
        if (paragraph.length <= maxChars) return listOf(paragraph)

        val sentences = mutableListOf<String>()
        val iterator = BreakIterator.getSentenceInstance(THAI_LOCALE)
        iterator.setText(paragraph)
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val sentence = paragraph.substring(start, end).trim()
            if (sentence.isNotEmpty()) {
                if (sentence.length > maxChars) {
                    sentences += splitByWordBoundary(sentence, maxChars)
                } else {
                    sentences.add(sentence)
                }
            }
            start = end
            end = iterator.next()
        }

        val pieces = mutableListOf<String>()
        val current = StringBuilder()
        for (sentence in sentences) {
            if (current.isNotEmpty() && current.length + sentence.length + 1 > maxChars) {
                pieces.add(current.toString())
                current.setLength(0)
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(sentence)
        }
        if (current.isNotEmpty()) pieces.add(current.toString())
        return pieces
    }

    private fun splitByWordBoundary(text: String, maxChars: Int): List<String> {
        if (text.length <= maxChars) return listOf(text)
        val wordIter = BreakIterator.getWordInstance(THAI_LOCALE)
        wordIter.setText(text)
        val result = mutableListOf<String>()
        var chunkStart = 0
        var lastValidEnd = 0
        var boundary = wordIter.next()
        while (boundary != BreakIterator.DONE) {
            if (boundary - chunkStart > maxChars) {
                if (lastValidEnd > chunkStart) {
                    val part = text.substring(chunkStart, lastValidEnd).trim()
                    if (part.isNotEmpty()) result.add(part)
                    chunkStart = lastValidEnd
                } else {
                    val hardEnd = (chunkStart + maxChars).coerceAtMost(text.length)
                    val part = text.substring(chunkStart, hardEnd).trim()
                    if (part.isNotEmpty()) result.add(part)
                    chunkStart = hardEnd
                }
            }
            lastValidEnd = boundary
            boundary = wordIter.next()
        }
        if (chunkStart < text.length) {
            val tail = text.substring(chunkStart).trim()
            if (tail.isNotEmpty()) result.add(tail)
        }
        return result
    }

    private fun overlapTail(chunk: String, overlapChars: Int): String {
        if (overlapChars <= 0 || chunk.length <= overlapChars) return ""
        val startOffset = chunk.length - overlapChars
        val wordIter = BreakIterator.getWordInstance(THAI_LOCALE)
        wordIter.setText(chunk)
        val snappedStart = wordIter.following(startOffset)
        return if (snappedStart != BreakIterator.DONE && snappedStart < chunk.length) {
            chunk.substring(snappedStart).trim()
        } else {
            val raw = chunk.substring(startOffset)
            val firstSpace = raw.indexOfFirst { it.isWhitespace() }
            if (firstSpace < 0) raw.trim() else raw.substring(firstSpace).trim()
        }
    }
}
