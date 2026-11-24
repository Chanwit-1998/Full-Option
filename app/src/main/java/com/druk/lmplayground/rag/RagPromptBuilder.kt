package com.druk.ai_offline_llm.rag

object RagPromptBuilder {

    fun budgetChars(contextSize: Int): Int = minOf(contextSize, 8192)

    fun build(
        userText: String,
        chunks: List<RagRepository.RetrievedChunk>,
        maxContextChars: Int,
    ): String {
        if (chunks.isEmpty() || maxContextChars <= 0) return userText

        val deduplicated = deduplicateChunks(chunks)
        val excerpts = StringBuilder()
        var included = 0
        for (chunk in deduplicated) {
            val idx = included + 1
            val header = "[$idx] Source: ${chunk.documentName}\n"
            val addition = header.length + chunk.text.length + 2
            if (excerpts.length + addition > maxContextChars) break
            excerpts.append(header).append(chunk.text.trim()).append("\n\n")
            included++
        }
        if (included == 0) return userText

        return buildString {
            append(
                "Answer strictly using ONLY the verified document excerpts inside <context> below. " +
                    "Do NOT guess, assume, or add outside knowledge. " +
                    "Cite source numbers like [1], [2] at the end of supported facts. " +
                    "If the answer is not explicitly stated in <context>, clearly state that the attached document does not contain that information.\n\n"
            )
            append("<context>\n")
            append(excerpts)
            append("</context>\n\n")
            append(userText)
        }
    }

    private fun deduplicateChunks(
        chunks: List<RagRepository.RetrievedChunk>,
    ): List<RagRepository.RetrievedChunk> {
        val accepted = mutableListOf<RagRepository.RetrievedChunk>()
        for (candidate in chunks) {
            val normCandidate = candidate.text.trim().lowercase()
            val isDuplicate = accepted.any { existing ->
                val normExisting = existing.text.trim().lowercase()
                normExisting.contains(normCandidate) ||
                    normCandidate.contains(normExisting) ||
                    jaccardSimilarity(normExisting, normCandidate) >= 0.80f
            }
            if (!isDuplicate) {
                accepted.add(candidate)
            }
        }
        return accepted
    }

    private fun jaccardSimilarity(a: String, b: String): Float {
        val setA = a.split(Regex("\\s+")).filter { it.length >= 2 }.toSet()
        val setB = b.split(Regex("\\s+")).filter { it.length >= 2 }.toSet()
        if (setA.isEmpty() || setB.isEmpty()) return 0f
        val intersection = setA.intersect(setB).size.toFloat()
        val union = setA.union(setB).size.toFloat()
        return if (union == 0f) 0f else intersection / union
    }
}
