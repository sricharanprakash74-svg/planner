package com.example.plannerapp.smartlink

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Asynchronous metadata resolver for Smart Links.
 * Complies with Google Play safety requirements: never executes JavaScript,
 * parses only OpenGraph headers up to 64KB, and respects timeouts.
 */
object SmartLinkResolver {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val gson = Gson()

    private val OG_TITLE_REGEX = Pattern.compile(
        """<meta[^>]+(?:property|name)=["'](?:og:title|twitter:title)["'][^>]+content=["']([^"']+)["']""",
        Pattern.CASE_INSENSITIVE
    )
    private val OG_TITLE_REVERSE_REGEX = Pattern.compile(
        """<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name)=["'](?:og:title|twitter:title)["']""",
        Pattern.CASE_INSENSITIVE
    )
    private val TITLE_TAG_REGEX = Pattern.compile(
        """<title[^>]*>([^<]+)</title>""",
        Pattern.CASE_INSENSITIVE
    )

    private val OG_DESC_REGEX = Pattern.compile(
        """<meta[^>]+(?:property|name)=["'](?:og:description|description|twitter:description)["'][^>]+content=["']([^"']+)["']""",
        Pattern.CASE_INSENSITIVE
    )
    private val OG_DESC_REVERSE_REGEX = Pattern.compile(
        """<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name)=["'](?:og:description|description|twitter:description)["']""",
        Pattern.CASE_INSENSITIVE
    )

    private val OG_IMAGE_REGEX = Pattern.compile(
        """<meta[^>]+(?:property|name)=["'](?:og:image|twitter:image|twitter:image:src)["'][^>]+content=["']([^"']+)["']""",
        Pattern.CASE_INSENSITIVE
    )
    private val OG_IMAGE_REVERSE_REGEX = Pattern.compile(
        """<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name)=["'](?:og:image|twitter:image|twitter:image:src)["']""",
        Pattern.CASE_INSENSITIVE
    )

    private val OG_SITE_REGEX = Pattern.compile(
        """<meta[^>]+(?:property|name)=["'](?:og:site_name)["'][^>]+content=["']([^"']+)["']""",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Resolves metadata for a URL asynchronously.
     * Guaranteed never to throw and always returns valid metadata (falling back gracefully).
     */
    suspend fun resolve(url: String): SmartLinkMetadata = withContext(Dispatchers.IO) {
        // 1. Check cache
        SmartLinkCache.get(url)?.let { return@withContext it }

        val provider = SmartLinkParser.detectProvider(url)
        val domain = SmartLinkParser.extractDomain(url)

        val metadata = when (provider) {
            SmartLinkProvider.YOUTUBE -> resolveYouTube(url, domain)
            SmartLinkProvider.IMAGE -> resolveImage(url, domain)
            SmartLinkProvider.PDF -> resolvePdf(url, domain)
            SmartLinkProvider.INTERNAL -> resolveInternal(url, domain)
            SmartLinkProvider.WEBPAGE -> resolveWebpage(url, domain)
            SmartLinkProvider.UNKNOWN -> createFallback(url, domain)
        }

        // Cache result
        SmartLinkCache.put(metadata)
        metadata
    }

    private suspend fun resolveYouTube(url: String, domain: String): SmartLinkMetadata {
        val videoId = SmartLinkParser.extractYouTubeVideoId(url)
        val defaultThumb = if (videoId != null) "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else null

        if (videoId == null) {
            return SmartLinkMetadata(
                url = url,
                provider = SmartLinkProvider.YOUTUBE,
                title = "YouTube Video",
                domain = domain,
                type = SmartLinkType.VIDEO,
                thumbnailUrl = defaultThumb
            )
        }

        // Fetch official YouTube oEmbed JSON (no API key required)
        val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
        try {
            val request = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "PlannerApp-Android/1.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = gson.fromJson(body, JsonObject::class.java)
                        val title = json.get("title")?.asString ?: "YouTube Video"
                        val author = json.get("author_name")?.asString ?: "YouTube"
                        val thumb = json.get("thumbnail_url")?.asString ?: defaultThumb

                        return SmartLinkMetadata(
                            url = url,
                            provider = SmartLinkProvider.YOUTUBE,
                            title = title,
                            author = author,
                            thumbnailUrl = thumb,
                            domain = "youtube.com",
                            type = SmartLinkType.VIDEO,
                            internalTargetId = videoId
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Fall through to default
        }

        return SmartLinkMetadata(
            url = url,
            provider = SmartLinkProvider.YOUTUBE,
            title = "YouTube Video",
            author = "YouTube",
            thumbnailUrl = defaultThumb,
            domain = "youtube.com",
            type = SmartLinkType.VIDEO,
            internalTargetId = videoId
        )
    }

    private fun resolveImage(url: String, domain: String): SmartLinkMetadata {
        val uri = Uri.parse(url)
        val filename = uri.lastPathSegment ?: "Image"
        val title = filename.substringBeforeLast('.')
            .replace('-', ' ')
            .replace('_', ' ')
            .trim()
            .ifBlank { "Embedded Image" }

        return SmartLinkMetadata(
            url = url,
            provider = SmartLinkProvider.IMAGE,
            title = title,
            thumbnailUrl = url,
            domain = domain,
            type = SmartLinkType.IMAGE
        )
    }

    private fun resolvePdf(url: String, domain: String): SmartLinkMetadata {
        val uri = Uri.parse(url)
        val filename = uri.lastPathSegment ?: "Document.pdf"
        val title = filename.substringBeforeLast('.')
            .replace('-', ' ')
            .replace('_', ' ')
            .trim()
            .ifBlank { "PDF Document" }

        return SmartLinkMetadata(
            url = url,
            provider = SmartLinkProvider.PDF,
            title = title,
            description = "PDF Document",
            domain = domain,
            type = SmartLinkType.DOCUMENT
        )
    }

    private fun resolveInternal(url: String, domain: String): SmartLinkMetadata {
        val uri = Uri.parse(url)
        val code = uri.getQueryParameter("code") ?: uri.lastPathSegment ?: ""
        val title = uri.getQueryParameter("title") ?: "Shared Plan Protocol"

        return SmartLinkMetadata(
            url = url,
            provider = SmartLinkProvider.INTERNAL,
            title = title,
            description = "Native PlannerApp Protocol",
            domain = "plannerapp",
            type = SmartLinkType.PLAN,
            internalTargetId = code
        )
    }

    private suspend fun resolveWebpage(url: String, domain: String): SmartLinkMetadata {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val stream = response.body?.byteStream()
                    if (stream != null) {
                        val reader = BufferedReader(InputStreamReader(stream))
                        val htmlBuilder = StringBuilder()
                        var line: String?
                        var bytesRead = 0
                        val maxBytes = 65536 // 64KB max reading

                        while (reader.readLine().also { line = it } != null) {
                            htmlBuilder.append(line).append("\n")
                            bytesRead += (line?.length ?: 0)
                            if (bytesRead >= maxBytes || htmlBuilder.contains("</head>", ignoreCase = true)) {
                                break
                            }
                        }

                        val headHtml = htmlBuilder.toString()

                        var title = extractMatch(OG_TITLE_REGEX, headHtml)
                            ?: extractMatch(OG_TITLE_REVERSE_REGEX, headHtml)
                            ?: extractMatch(TITLE_TAG_REGEX, headHtml)

                        var desc = extractMatch(OG_DESC_REGEX, headHtml)
                            ?: extractMatch(OG_DESC_REVERSE_REGEX, headHtml)

                        var image = extractMatch(OG_IMAGE_REGEX, headHtml)
                            ?: extractMatch(OG_IMAGE_REVERSE_REGEX, headHtml)

                        val siteName = extractMatch(OG_SITE_REGEX, headHtml)

                        // Sanitize and clean HTML entities
                        title = cleanHtmlEntities(title)?.take(120)
                        desc = cleanHtmlEntities(desc)?.take(200)

                        // If image URL is relative, resolve against base URL
                        if (image != null && !image.startsWith("http://") && !image.startsWith("https://")) {
                            image = try {
                                val baseUri = Uri.parse(url)
                                baseUri.buildUpon().path(image).build().toString()
                            } catch (e: Exception) {
                                null
                            }
                        }

                        return SmartLinkMetadata(
                            url = url,
                            provider = SmartLinkProvider.WEBPAGE,
                            title = title ?: domain,
                            description = desc,
                            thumbnailUrl = image,
                            domain = siteName ?: domain,
                            type = SmartLinkType.ARTICLE
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Network failure fallback
        }

        return createFallback(url, domain)
    }

    private fun createFallback(url: String, domain: String): SmartLinkMetadata {
        return SmartLinkMetadata(
            url = url,
            provider = SmartLinkProvider.WEBPAGE,
            title = domain,
            description = url,
            domain = domain,
            type = SmartLinkType.LINK
        )
    }

    private fun extractMatch(pattern: Pattern, text: String): String? {
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group(1)?.trim() else null
    }

    private fun cleanHtmlEntities(text: String?): String? {
        if (text == null) return null
        return text.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&#x27;", "'")
            .replace("&#x2F;", "/")
            .replace("&nbsp;", " ")
            .trim()
    }
}
