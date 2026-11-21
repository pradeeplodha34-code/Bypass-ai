package com.bypass.ide.data

import android.util.Log
import com.bypass.ide.model.ExaminedSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private const val TAG = "WebSearchService"
private const val USER_AGENT = "BypassIDE/1.0 (Android Mobile IDE; Knowledge Grounding)"

class WebSearchService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) {

    suspend fun searchLiveWeb(query: String): List<ExaminedSource> = withContext(Dispatchers.IO) {
        val cleanQuery = query.replace("@web", "", ignoreCase = true).trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        val encoded = URLEncoder.encode(cleanQuery, "UTF-8")

        val collected = withTimeoutOrNull(4_500L) {
            coroutineScope {
                val ddgDeferred = async { fetchDuckDuckGoInstant(cleanQuery, encoded) }
                val wikiDeferred = async { fetchWikipedia(encoded) }
                val merged = (ddgDeferred.await() + wikiDeferred.await()).toMutableList()
                if (merged.size < 2) {
                    merged.addAll(fetchDuckDuckGoLite(cleanQuery, encoded))
                }
                merged.distinctBy { it.url }.take(6)
            }
        }
        collected ?: emptyList()
    }

    private fun fetchDuckDuckGoInstant(cleanQuery: String, encoded: String): List<ExaminedSource> {
        val results = mutableListOf<ExaminedSource>()
        runCatching {
            val req = Request.Builder()
                .url("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1")
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        val abstractText = json.optString("AbstractText")
                        val abstractUrl = json.optString("AbstractURL")
                        val heading = json.optString("Heading")
                        if (abstractText.isNotBlank() && abstractUrl.isNotBlank()) {
                            results.add(
                                ExaminedSource(
                                    title = heading.ifBlank { cleanQuery },
                                    url = abstractUrl,
                                    domain = extractDomain(abstractUrl),
                                    snippet = abstractText.take(280)
                                )
                            )
                        }
                        val related = json.optJSONArray("RelatedTopics")
                        if (related != null) {
                            for (i in 0 until minOf(related.length(), 4)) {
                                val item = related.optJSONObject(i) ?: continue
                                val text = item.optString("Text")
                                val firstUrl = item.optString("FirstURL")
                                if (text.isNotBlank() && firstUrl.isNotBlank()) {
                                    results.add(
                                        ExaminedSource(
                                            title = text.substringBefore(" - ").take(80),
                                            url = firstUrl,
                                            domain = extractDomain(firstUrl),
                                            snippet = text.take(240)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }.onFailure { Log.w(TAG, "DuckDuckGo API search error: ${it.message}") }
        return results
    }

    private fun fetchWikipedia(encoded: String): List<ExaminedSource> {
        val results = mutableListOf<ExaminedSource>()
        runCatching {
            val req = Request.Builder()
                .url("https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&format=json&utf8=1&srlimit=3")
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val searchArr = json.optJSONObject("query")?.optJSONArray("search")
                    if (searchArr != null) {
                        for (i in 0 until searchArr.length()) {
                            val obj = searchArr.optJSONObject(i) ?: continue
                            val title = obj.optString("title")
                            val snippet = obj.optString("snippet")
                                .replace(Regex("<[^>]+>"), "")
                                .replace("&quot;", "\"")
                                .replace("&amp;", "&")
                            if (title.isNotBlank()) {
                                val wikiUrl = "https://en.wikipedia.org/wiki/${URLEncoder.encode(title.replace(" ", "_"), "UTF-8")}"
                                results.add(
                                    ExaminedSource(
                                        title = "$title — Wikipedia",
                                        url = wikiUrl,
                                        domain = "en.wikipedia.org",
                                        snippet = snippet.take(240)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }.onFailure { Log.w(TAG, "Wikipedia search error: ${it.message}") }
        return results
    }

    private fun fetchDuckDuckGoLite(cleanQuery: String, encoded: String): List<ExaminedSource> {
        val results = mutableListOf<ExaminedSource>()
        runCatching {
            val req = Request.Builder()
                .url("https://lite.duckduckgo.com/lite/?q=$encoded")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val html = resp.body?.string().orEmpty()
                    val linkRegex = Regex(
                        """<a[^>]+href=['"](https?://[^'"]+)['"][^>]*class=['"]result-link['"][^>]*>([\s\S]*?)</a>""",
                        RegexOption.IGNORE_CASE
                    )
                    linkRegex.findAll(html).take(4).forEach { match ->
                        val url = match.groupValues[1]
                        val title = match.groupValues[2].replace(Regex("<[^>]+>"), "").trim()
                        if (url.isNotBlank() && title.isNotBlank()) {
                            results.add(
                                ExaminedSource(
                                    title = title.take(90),
                                    url = url,
                                    domain = extractDomain(url),
                                    snippet = "Live web search result for $cleanQuery"
                                )
                            )
                        }
                    }
                }
            }
        }.onFailure { Log.w(TAG, "DuckDuckGo Lite search error: ${it.message}") }
        return results
    }

    suspend fun fetchPageExcerpt(url: String): String = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext "Failed to fetch URL HTTP ${resp.code}"
                val raw = resp.body?.string().orEmpty()
                raw.replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
                    .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
                    .replace(Regex("<[^>]+>"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .take(2000)
            }
        }.getOrElse { "Error fetching URL: ${it.message}" }
    }

    private fun extractDomain(url: String): String {
        return url.removePrefix("https://")
            .removePrefix("http://")
            .substringBefore("/")
            .removePrefix("www.")
            .ifBlank { "web" }
    }
}
