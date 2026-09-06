package com.example.plannerapp.sharing

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object PlanSharingManager {

    private const val DEEP_LINK_BASE = "plannerapp://share/plan"

    fun generateShareCode(): String = UUID.randomUUID().toString().take(8).uppercase()

    fun buildDeepLinkUri(shareCode: String, authorUserId: Long, planTitle: String): Uri {
        return Uri.parse(DEEP_LINK_BASE).buildUpon()
            .appendQueryParameter("code", shareCode)
            .appendQueryParameter("ref", authorUserId.toString())
            .appendQueryParameter("title", planTitle)
            .build()
    }

    /**
     * Renders a 1080x1920 (9:16) native high-resolution Story Card bitmap.
     * Complies with the Zero Emojis policy and uses clean typography and geometric badge elements.
     */
    fun renderStoryCard(
        context: Context,
        planTitle: String,
        streakDays: Int,
        consistencyPercent: Int,
        totalTasksCompleted: Int
    ): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(Color.parseColor("#121212"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header Category Card
        paint.color = Color.parseColor("#1E1E1E")
        val container = RectF(80f, 180f, 1000f, 1740f)
        canvas.drawRoundRect(container, 48f, 48f, paint)

        // Accent Brand Pill
        paint.color = Color.parseColor("#388E3C")
        canvas.drawRoundRect(RectF(140f, 260f, 440f, 330f), 35f, 35f, paint)

        paint.color = Color.WHITE
        paint.textSize = 32f
        paint.isFakeBoldText = true
        canvas.drawText("DAILY PROTOCOL", 168f, 308f, paint)

        // Plan Heading
        paint.textSize = 68f
        paint.isFakeBoldText = true
        canvas.drawText(planTitle.take(24), 140f, 440f, paint)

        // Streak Block
        paint.color = Color.parseColor("#2A2A2A")
        canvas.drawRoundRect(RectF(140f, 520f, 940f, 820f), 32f, 32f, paint)

        paint.color = Color.parseColor("#4CAF50")
        paint.textSize = 120f
        paint.isFakeBoldText = true
        canvas.drawText("$streakDays", 200f, 690f, paint)

        paint.color = Color.LTGRAY
        paint.textSize = 36f
        paint.isFakeBoldText = false
        canvas.drawText("DAYS ACTIVE STREAK", 200f, 750f, paint)

        // Consistency Metrics Box
        canvas.drawRoundRect(RectF(140f, 860f, 940f, 1160f), 32f, 32f, paint)

        paint.color = Color.WHITE
        paint.textSize = 100f
        paint.isFakeBoldText = true
        canvas.drawText("$consistencyPercent%", 200f, 1030f, paint)

        paint.color = Color.LTGRAY
        paint.textSize = 36f
        paint.isFakeBoldText = false
        canvas.drawText("CONSISTENCY SCORE", 200f, 1090f, paint)

        // Tasks Completed Box
        canvas.drawRoundRect(RectF(140f, 1200f, 940f, 1420f), 32f, 32f, paint)

        paint.color = Color.WHITE
        paint.textSize = 72f
        paint.isFakeBoldText = true
        canvas.drawText("$totalTasksCompleted TASKS", 200f, 1320f, paint)

        paint.color = Color.GRAY
        paint.textSize = 30f
        paint.isFakeBoldText = false
        canvas.drawText("COMPLETED ON SCHEDULE", 200f, 1375f, paint)

        // Footer CTA
        paint.color = Color.LTGRAY
        paint.textSize = 34f
        paint.isFakeBoldText = true
        canvas.drawText("CLONE THIS PLAN ON PLANNER APP", 140f, 1620f, paint)

        return bitmap
    }

    /**
     * Saves the story card to cache and launches the standard Android Sharesheet.
     */
    fun shareStoryCardIntent(context: Context, bitmap: Bitmap, shareDeepLink: Uri): Intent {
        val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
        val imageFile = File(cachePath, "story_card_${System.currentTimeMillis()}.png")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(
                Intent.EXTRA_TEXT,
                "Check out my progress and clone this plan to track your goals:\n$shareDeepLink"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
