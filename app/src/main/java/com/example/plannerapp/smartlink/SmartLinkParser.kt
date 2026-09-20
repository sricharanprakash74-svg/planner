package com.example.plannerapp.smartlink

import android.net.Uri
import java.util.Locale
import java.util.regex.Pattern

/**
 * Parses and classifies URLs from raw text inputs.
 */
object SmartLinkParser {

    private val URL_REGEX: Pattern = Pattern.compile(
        "(?:https?://|plannerapp://)[\\w\\d:#@%/;$()~_?\\+-=\\\\.&]+"
    )

    private val YOUTUBE_HOSTS = setOf(
        "youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be", "music.youtube.com"
    )

    private val IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg"
    )

    /**
     * Extracts all URLs from the given text.
     */
    fun extractUrls(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val matcher = URL_REGEX.matcher(text)
        val urls = mutableListOf<String>()
        while (matcher.find()) {
            var url = matcher.group()
            // Clean trailing punctuation
            while (url.isNotEmpty() && (url.endsWith(".") || url.endsWith(",") || url.endsWith(")") || url.endsWith(";"))) {
                url = url.substring(0, url.length - 1)
            }
            if (url.isNotBlank() && !urls.contains(url)) {
                urls.add(url)
            }
        }
        return urls
    }

    /**
     * Returns the first detected URL, or null if none found.
     */
    fun findFirstUrl(text: String): String? = extractUrls(text).firstOrNull()

    /**
     * Strips detected URLs from the text to yield clean user display title/description.
     */
    fun extractCleanText(text: String): String {
        var clean = text
        for (url in extractUrls(text)) {
            clean = clean.replace(url, "")
        }
        // Normalize whitespace and empty newlines
        return clean.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .trim()
    }

    /**
     * Detects the provider category for a given URL.
     */
    fun detectProvider(rawUrl: String): SmartLinkProvider {
        val uri = try { Uri.parse(rawUrl) } catch (e: Exception) { return SmartLinkProvider.UNKNOWN }
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: ""
        val host = uri.host?.lowercase(Locale.ROOT) ?: ""
        val path = uri.path?.lowercase(Locale.ROOT) ?: ""

        if (scheme == "plannerapp" || host == "plannerapp.com" || host.endsWith(".plannerapp.com")) {
            return SmartLinkProvider.INTERNAL
        }

        if (YOUTUBE_HOSTS.contains(host)) {
            return SmartLinkProvider.YOUTUBE
        }

        val extension = getExtension(path)
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return SmartLinkProvider.IMAGE
        }

        if (extension == "pdf") {
            return SmartLinkProvider.PDF
        }

        if (scheme == "http" || scheme == "https") {
            return SmartLinkProvider.WEBPAGE
        }

        return SmartLinkProvider.UNKNOWN
    }

    /**
     * Extracts YouTube video ID if URL is a recognized YouTube video.
     */
    fun extractYouTubeVideoId(url: String): String? {
        val uri = try { Uri.parse(url) } catch (e: Exception) { return null }
        val host = uri.host?.lowercase(Locale.ROOT) ?: return null
        val path = uri.path ?: ""

        if (host == "youtu.be") {
            return path.removePrefix("/").takeIf { it.isNotBlank() }
        }

        if (host.contains("youtube.com")) {
            // Check query param v
            uri.getQueryParameter("v")?.let { return it }

            // Check /shorts/{id} or /embed/{id}
            val segments = uri.pathSegments
            if (segments.size >= 2 && (segments[0] == "shorts" || segments[0] == "embed" || segments[0] == "v")) {
                return segments[1]
            }
        }
        return null
    }

    /**
     * Extracts a human-friendly domain string (e.g. "youtube.com" or "developer.android.com").
     */
    fun extractDomain(url: String): String {
        return try {
            val uri = Uri.parse(url)
            if (uri.scheme == "plannerapp") return "plannerapp"
            uri.host?.removePrefix("www.") ?: "link"
        } catch (e: Exception) {
            "link"
        }
    }

    /**
     * Extracts file extension from a path string.
     */
    private fun getExtension(path: String): String {
        val lastSlash = path.lastIndexOf('/')
        val filename = if (lastSlash >= 0) path.substring(lastSlash + 1) else path
        val dot = filename.lastIndexOf('.')
        return if (dot >= 0 && dot < filename.length - 1) {
            filename.substring(dot + 1).lowercase(Locale.ROOT)
        } else {
            ""
        }
    }
}
