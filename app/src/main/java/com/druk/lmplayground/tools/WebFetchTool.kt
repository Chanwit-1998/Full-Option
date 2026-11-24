package com.druk.ai_offline_llm.tools

import android.util.LruCache
import com.druk.llamacpp.tools.Tool
import com.druk.llamacpp.tools.WebLinkStore
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class WebFetchTool(private val linkStore: WebLinkStore? = null) : Tool {
    override val name = "web_fetch"
    override val description = "Fetch a web page and return its main content as concise markdown."
    override val parametersSchema = """{"type":"object","properties":{"url":{"type":"string","description":"The URL to fetch, or a reference from web_search results such as \"ddg:3\""},"max_length":{"type":"integer","description":"Maximum content length in characters (default 2500)"}},"required":["url"]}"""

    private data class CacheEntry(val title: String, val content: String, val timestampMs: Long)

    private val cache = LruCache<String, CacheEntry>(20)

    private val client = OkHttpClient.Builder()
        .connectTimeout(3500, TimeUnit.MILLISECONDS)
        .readTimeout(3500, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .build()

    override fun cancelInFlight() {
        client.dispatcher.cancelAll()
    }

    override fun execute(arguments: String): String {
        return try {
            val args = JSONObject(arguments)
            val rawUrl = args.getString("url").trim()
            val resolved = linkStore?.resolve(rawUrl)
            if (resolved == null && linkStore?.isReference(rawUrl) == true) {
                return errorJson("Unknown search reference '$rawUrl'. Call web_search first, then fetch a returned ref.")
            }
            val target = resolved ?: rawUrl
            val url = if (!target.startsWith("http")) "https://$target" else target
            val maxLength = args.optInt("max_length", 2500).coerceIn(50, 8000)

            val cached = cache.get(url)
            if (cached != null && System.currentTimeMillis() - cached.timestampMs < CACHE_TTL_MS) {
                val truncated = if (cached.content.length > maxLength) cached.content.substring(0, maxLength) + "..." else cached.content
                return JSONObject().apply {
                    put("url", url)
                    put("title", cached.title)
                    put("content", truncated)
                    put("length", truncated.length)
                }.toString()
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val contentType = response.header("Content-Type") ?: ""
            val body = response.body?.string() ?: return errorJson("Empty response")

            val title: String
            val fullCleanText: String
            if (contentType.contains("text/html") || contentType.contains("application/xhtml")) {
                val doc = Jsoup.parse(body, url)
                title = doc.title()
                fullCleanText = HtmlToMarkdown.convert(doc)
            } else {
                title = ""
                fullCleanText = body
            }

            cache.put(url, CacheEntry(title, fullCleanText.take(8000), System.currentTimeMillis()))

            val truncated = if (fullCleanText.length > maxLength) {
                fullCleanText.substring(0, maxLength) + "..."
            } else {
                fullCleanText
            }

            JSONObject().apply {
                put("url", url)
                put("title", title)
                put("content", truncated)
                put("length", truncated.length)
            }.toString()
        } catch (e: Exception) {
            errorJson(e.message ?: "Fetch failed")
        }
    }

    private fun errorJson(message: String): String {
        return """{"error":"${message.replace("\"", "'")}"}"""
    }

    companion object {
        private const val CACHE_TTL_MS = 10 * 60 * 1000L
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }
}
