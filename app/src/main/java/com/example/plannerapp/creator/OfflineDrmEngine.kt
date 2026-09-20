package com.example.plannerapp.creator

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Offline DRM engine providing cryptographically signed license tokens and
 * timezone-aware progression locking.
 */
object OfflineDrmEngine {

    // Internal signing secret key used to produce HMAC-SHA256 license signatures
    private const val DRM_SECRET = "PlannerApp_Enterprise_Drm_Secret_Key_2026_Secure"
    private const val ALGORITHM = "HmacSHA256"

    /**
     * Issues an asymmetric/cryptographic verification signature for an acquired plan entitlement.
     * Combines planId, userId, versionId, and grantedAt timestamp into a secure verifiable hash.
     */
    fun generateLicenseSignature(
        planId: String,
        userId: String,
        versionId: String,
        grantedAt: Long
    ): String {
        val payload = "$planId:$userId:$versionId:$grantedAt"
        return try {
            val keySpec = SecretKeySpec(DRM_SECRET.toByteArray(StandardCharsets.UTF_8), ALGORITHM)
            val mac = Mac.getInstance(ALGORITHM)
            mac.init(keySpec)
            val rawBytes = mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8))
            bytesToHex(rawBytes)
        } catch (_: Exception) {
            // Fallback to SHA-256 digest if HMAC instance fails
            val sha256 = MessageDigest.getInstance("SHA-256")
            val hash = sha256.digest(payload.toByteArray(StandardCharsets.UTF_8))
            bytesToHex(hash)
        }
    }

    /**
     * Validates that an entitlement token is authentic and untampered without requiring network access.
     */
    fun verifyLicenseSignature(
        planId: String,
        userId: String,
        versionId: String,
        grantedAt: Long,
        signature: String
    ): Boolean {
        val expected = generateLicenseSignature(planId, userId, versionId, grantedAt)
        return expected.equals(signature, ignoreCase = true)
    }

    /**
     * Calculates whether a specific day in a multi-day plan is unlocked based on the user's
     * local timezone and start date. Prevents bingeing when Strict Midnight Lock is active.
     *
     * @param dayIndex 1-based index of the day (e.g. Day 1, Day 2, etc.)
     * @param planStartDateIso "YYYY-MM-DD" formatted start date
     * @param userTimezoneId Device or user-configured ZoneId string (e.g., "Asia/Kolkata", "UTC")
     * @param freemiumPreviewDays Number of free preview days that are always unlocked
     * @param isStrictLockEnabled Whether Strict Midnight Lock is enforced
     */
    fun isDayUnlocked(
        dayIndex: Int,
        planStartDateIso: String,
        userTimezoneId: String,
        freemiumPreviewDays: Int = 0,
        isStrictLockEnabled: Boolean = false
    ): Boolean {
        // Freemium teaser days are always unlocked
        if (dayIndex <= freemiumPreviewDays) return true

        // If open pacing is allowed, days aren't locked to midnight
        if (!isStrictLockEnabled) return true

        return try {
            val zone = try { ZoneId.of(userTimezoneId) } catch (_: Exception) { ZoneId.systemDefault() }
            val nowInZone = ZonedDateTime.now(zone).toLocalDate()
            val startDate = LocalDate.parse(planStartDateIso)

            // Calculate elapsed calendar days in participant's timezone
            val daysElapsed = java.time.temporal.ChronoUnit.DAYS.between(startDate, nowInZone).toInt()
            val currentAllowedDay = daysElapsed + 1

            dayIndex <= currentAllowedDay
        } catch (_: Exception) {
            // Safe fallback if parsing error occurs
            dayIndex <= 1
        }
    }

    /**
     * Calculates the exact millis until the next midnight in the participant's timezone.
     */
    fun millisUntilNextMidnight(userTimezoneId: String): Long {
        return try {
            val zone = try { ZoneId.of(userTimezoneId) } catch (_: Exception) { ZoneId.systemDefault() }
            val now = ZonedDateTime.now(zone)
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zone)
            java.time.Duration.between(now, nextMidnight).toMillis().coerceAtLeast(0L)
        } catch (_: Exception) {
            86400000L
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789abcdef".toCharArray()
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
