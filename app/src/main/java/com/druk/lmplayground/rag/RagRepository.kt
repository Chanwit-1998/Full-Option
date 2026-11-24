package com.druk.ai_offline_llm.rag

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import com.druk.ai_offline_llm.data.EmbeddingCodec
import com.druk.ai_offline_llm.data.RagChunkEntity
import com.druk.ai_offline_llm.data.RagDao
import com.druk.ai_offline_llm.data.RagDocumentEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.BreakIterator
import java.util.Locale
import java.util.UUID
import kotlin.math.ln

class RagRepository(
    private val appContext: Context,
    private val ragDao: RagDao,
    private val embeddingManager: TextEmbedder,
    private val applicationScope: CoroutineScope,
) {

    data class RetrievedChunk(
        val documentName: String,
        val text: String,
        val score: Float,
    )

    data class IndexingFailure(val displayName: String, val reasonCode: String)

    private val _indexingFailures = MutableSharedFlow<IndexingFailure>(extraBufferCapacity = 4)
    val indexingFailures: SharedFlow<IndexingFailure> = _indexingFailures

    fun observeDocuments(sessionId: String): LiveData<List<RagDocumentEntity>> =
        ragDao.observeDocuments(sessionId)

    fun isEmbeddingModelAvailable(): Boolean = embeddingManager.isModelOnDisk()

    suspend fun hasReadyDocuments(sessionId: String): Boolean =
        ragDao.countReadyDocuments(sessionId) > 0

    suspend fun deleteDocument(documentId: String) =
        ragDao.deleteDocument(documentId)

    fun resetStaleIndexing() {
        applicationScope.launch { ragDao.deleteStaleIndexing() }
    }

    fun attachDocument(
        sessionId: String,
        uri: Uri,
        displayName: String,
        mimeType: String?,
        sizeBytes: Long,
    ): String {
        val documentId = UUID.randomUUID().toString()
        applicationScope.launch {
            ragDao.insertDocument(
                RagDocumentEntity(
                    id = documentId,
                    sessionId = sessionId,
                    displayName = displayName,
                    mimeType = mimeType,
                    sourceUri = uri.toString(),
                    sizeBytes = sizeBytes,
                    status = RagDocumentEntity.STATUS_INDEXING,
                    embeddingModel = embeddingManager.modelInfo.filename,
                    createdAt = System.currentTimeMillis(),
                )
            )
            try {
                indexDocument(documentId, uri, displayName, mimeType, sizeBytes)
            } catch (e: DocumentExtractionException) {
                Log.w(TAG, "Indexing failed for $displayName: ${e.reason}", e)
                dropFailedDocument(documentId, displayName, e.reason.name)
            } catch (t: Throwable) {
                Log.e(TAG, "Indexing failed for $displayName", t)
                dropFailedDocument(documentId, displayName, ERROR_EMBEDDING)
            }
        }
        return documentId
    }

    private suspend fun indexDocument(
        documentId: String,
        uri: Uri,
        displayName: String,
        mimeType: String?,
        sizeBytes: Long,
    ) {
        if (sizeBytes > DocumentTextExtractor.MAX_FILE_BYTES) {
            throw DocumentExtractionException(
                DocumentExtractionException.Reason.TOO_LARGE,
                "File is $sizeBytes bytes",
            )
        }
        val extractor = DocumentTextExtractors.forDocument(mimeType, displayName)
            ?: throw DocumentExtractionException(
                DocumentExtractionException.Reason.UNSUPPORTED,
                "No extractor for $mimeType / $displayName",
            )

        val text = withContext(Dispatchers.IO) { extractor.extract(appContext, uri) }
        val chunks = TextChunker.chunk(text)
        if (chunks.isEmpty()) {
            throw DocumentExtractionException(
                DocumentExtractionException.Reason.NO_TEXT,
                "Document produced no text",
            )
        }
        Log.i(TAG, "Indexing $displayName: ${text.length} chars -> ${chunks.size} chunks")

        var dim = 0
        val rows = ArrayList<RagChunkEntity>(chunks.size)
        for (batch in chunks.chunked(EMBED_BATCH_SIZE)) {
            val vectors = withContext(Dispatchers.Default) {
                embeddingManager.embedDocumentChunks(batch.map { it.text })
            } ?: throw IllegalStateException("Embedding failed")
            dim = vectors.firstOrNull()?.size ?: 0
            for ((chunk, vector) in batch.zip(vectors)) {
                rows.add(
                    RagChunkEntity(
                        documentId = documentId,
                        ordinal = chunk.ordinal,
                        text = chunk.text,
                        embedding = EmbeddingCodec.encode(vector),
                    )
                )
            }
        }

        ragDao.insertChunks(rows)
        ragDao.markDocumentReady(documentId, chunkCount = rows.size, embeddingDim = dim)
        Log.i(TAG, "Indexed $displayName: ${rows.size} chunks, dim=$dim")
    }

    suspend fun retrieve(
        sessionId: String,
        query: String,
        topK: Int = DEFAULT_TOP_K,
        minScore: Float = DEFAULT_MIN_SCORE,
    ): List<RetrievedChunk> {
        val rows = ragDao.getReadyChunksForSession(sessionId)
        if (rows.isEmpty()) return emptyList()
        val queryVector = withContext(Dispatchers.Default) {
            embeddingManager.embedQuery(query)
        }

        val top = withContext(Dispatchers.Default) {
            val queryTerms = tokenizeForKeywordSearch(query)
            val docTermSets = rows.map { tokenizeForKeywordSearch(it.text) }
            val df = mutableMapOf<String, Int>()
            for (terms in docTermSets) {
                for (t in terms.toSet()) {
                    df[t] = (df[t] ?: 0) + 1
                }
            }
            val totalDocs = rows.size.toFloat().coerceAtLeast(1f)
            val avgDocLen = docTermSets.map { it.size }.average().toFloat().coerceAtLeast(1f)

            val vectorScores = rows.map { row ->
                if (queryVector == null) 0f
                else {
                    val vector = EmbeddingCodec.decode(row.embedding)
                    if (vector.size != queryVector.size) 0f else dot(queryVector, vector)
                }
            }

            val bm25Scores = rows.indices.map { idx ->
                if (queryTerms.isEmpty()) 0f
                else computeBm25(queryTerms, docTermSets[idx], rows[idx].text, df, totalDocs, avgDocLen)
            }

            val vectorRanks = rankIndices(vectorScores)
            val bm25Ranks = rankIndices(bm25Scores)
            val hasKeywordHits = bm25Scores.any { it > 0f }

            val combined = rows.indices.mapNotNull { idx ->
                val vScore = vectorScores[idx]
                val bScore = bm25Scores[idx]
                if (queryVector != null && vScore < minScore && bScore <= 0f) {
                    return@mapNotNull null
                }
                val rrfVector = 0.70f / (RRF_K + vectorRanks[idx])
                val rrfKeyword = if (hasKeywordHits && bScore > 0f) 0.30f / (RRF_K + bm25Ranks[idx]) else 0f
                val hybridScore = if (queryVector != null) {
                    (vScore * 0.75f) + (rrfVector + rrfKeyword) * 15f
                } else {
                    bScore
                }
                Triple(rows[idx], hybridScore, vScore)
            }.sortedByDescending { it.second }

            val bestScore = combined.firstOrNull()?.second ?: return@withContext emptyList()
            val gapFloor = bestScore - SCORE_GAP_THRESHOLD
            combined
                .filter { it.second >= gapFloor }
                .take(topK)
                .map { it.first to it.second }
        }
        if (top.isEmpty()) return emptyList()

        val nameById = top.map { (row, _) -> row.documentId }.distinct()
            .associateWith { id -> ragDao.getDocumentName(id) ?: "document" }
        return top.map { (row, score) ->
            RetrievedChunk(
                documentName = nameById.getValue(row.documentId),
                text = row.text,
                score = score,
            )
        }
    }

    private fun rankIndices(scores: List<Float>): IntArray {
        val sorted = scores.indices.sortedByDescending { scores[it] }
        val ranks = IntArray(scores.size)
        for ((rank, idx) in sorted.withIndex()) {
            ranks[idx] = rank + 1
        }
        return ranks
    }

    private fun computeBm25(
        queryTerms: List<String>,
        docTerms: List<String>,
        rawDocText: String,
        df: Map<String, Int>,
        totalDocs: Float,
        avgDocLen: Float,
    ): Float {
        if (docTerms.isEmpty()) return 0f
        val tfMap = mutableMapOf<String, Int>()
        for (term in docTerms) {
            tfMap[term] = (tfMap[term] ?: 0) + 1
        }
        val docLen = docTerms.size.toFloat()
        val lowerDoc = rawDocText.lowercase(THAI_LOCALE)
        val k1 = 1.5f
        val b = 0.75f
        var score = 0f
        for (q in queryTerms.distinct()) {
            val tf = (tfMap[q] ?: 0).toFloat()
            val effectiveTf = if (tf > 0f) tf else if (q.length >= 3 && lowerDoc.contains(q)) 0.6f else 0f
            if (effectiveTf <= 0f) continue
            val docFreq = (df[q] ?: 1).toFloat()
            val idf = ln(((totalDocs - docFreq + 0.5f) / (docFreq + 0.5f)) + 1f).toFloat()
            val norm = effectiveTf * (k1 + 1f) / (effectiveTf + k1 * (1f - b + b * (docLen / avgDocLen)))
            score += idf * norm
        }
        return score
    }

    private fun tokenizeForKeywordSearch(text: String): List<String> {
        val lower = text.lowercase(THAI_LOCALE)
        val iter = BreakIterator.getWordInstance(THAI_LOCALE)
        iter.setText(lower)
        val tokens = mutableListOf<String>()
        var start = iter.first()
        var end = iter.next()
        while (end != BreakIterator.DONE) {
            val word = lower.substring(start, end).trim()
            if (word.length >= 2 && word.any { it.isLetterOrDigit() } && word !in STOP_WORDS) {
                tokens.add(word)
            }
            start = end
            end = iter.next()
        }
        return tokens
    }

    private suspend fun dropFailedDocument(
        documentId: String,
        displayName: String,
        reasonCode: String,
    ) {
        ragDao.deleteDocument(documentId)
        _indexingFailures.tryEmit(IndexingFailure(displayName, reasonCode))
    }

    private fun dot(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) sum += a[i] * b[i]
        return sum
    }

    companion object {
        private const val TAG = "RagRepository"
        const val ERROR_EMBEDDING = "EMBEDDING_FAILED"
        const val DEFAULT_TOP_K = 6
        const val DEFAULT_MIN_SCORE = 0.25f
        private const val SCORE_GAP_THRESHOLD = 0.14f
        private const val RRF_K = 60f
        private const val EMBED_BATCH_SIZE = 16
        private val THAI_LOCALE = Locale("th", "TH")
        private val STOP_WORDS = setOf(
            "the", "is", "are", "was", "were", "in", "on", "at", "to", "for", "of", "and", "or",
            "what", "who", "where", "when", "why", "how", "which", "that", "this",
            "คือ", "อะไร", "ที่", "ใน", "ของ", "และ", "หรือ", "ได้", "มี", "เป็น", "จะ", "ให้", "กับ", "จาก", "โดย", "ว่า", "ไม่"
        )
    }
}
