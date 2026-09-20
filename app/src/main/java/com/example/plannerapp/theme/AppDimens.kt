package com.example.plannerapp.theme

import androidx.compose.ui.unit.dp

/**
 * Central spatial grid and dimension tokens for PlannerApp.
 * Following Clean Minimalism & Material Design principles.
 */
object AppDimens {

    // ── Base spacing scale ─────────────────────────────────────────────────
    val Space2  = 2.dp
    val Space4  = 4.dp
    val Space8  = 8.dp
    val Space12 = 12.dp
    val Space14 = 14.dp
    val Space16 = 16.dp  // Card internal padding
    val Space20 = 20.dp  // Screen horizontal padding
    val Space24 = 24.dp  // Section vertical gap (minimum)
    val Space28 = 28.dp
    val Space32 = 32.dp
    val Space40 = 40.dp
    val Space48 = 48.dp

    // ── Screen & Layout Padding ───────────────────────────────────────────
    val ScreenHorizontalPadding = 20.dp
    val SectionVerticalGap      = 24.dp
    val CardInternalPadding     = 16.dp

    // ── Corner radii ───────────────────────────────────────────────────────
    val CornerCard      = 16.dp  // Standard card corner radius
    val CornerButton    = 14.dp  // Standard button corner radius
    val CornerCompact   = 12.dp  // Input field & chip corner radius
    val CornerPill      = 20.dp  // Pill / container corner
    val CornerMicro     = 8.dp

    // ── Heights ────────────────────────────────────────────────────────────
    val ButtonHeight    = 54.dp  // Standard primary / secondary button height
    val InputHeight     = 54.dp  // Standard text field height
    val ChipHeight      = 48.dp  // Preference chip height
    val ProgressBarHeight = 4.dp // Onboarding progress bar segment height

    // ── Icon sizes ─────────────────────────────────────────────────────────
    val IconSizeXs      = 12.dp
    val IconSizeSm      = 16.dp
    val IconSizeMd      = 20.dp  // Leading icon & checkmark size
    val IconSizeLg      = 24.dp
    val IconSizeXl      = 32.dp
    val IconSizeDisplay = 36.dp  // Notification bell icon

    // ── Icon container boxes ───────────────────────────────────────────────
    val IconBoxSm       = 28.dp  // Profile camera badge
    val IconBoxMd       = 40.dp
    val IconBoxLg       = 48.dp
    val IconBoxXl       = 56.dp
    val IconBoxBell     = 80.dp  // Notification permission icon container

    // ── Avatar sizes ───────────────────────────────────────────────────────
    val AvatarSm        = 32.dp
    val AvatarMd        = 48.dp
    val AvatarLg        = 88.dp  // Profile setup avatar circle

    // ── Elevation / border ─────────────────────────────────────────────────
    val BorderThin      = 1.dp
    val BorderFocus     = 1.5.dp
    val ElevationNone   = 0.dp
    val ElevationCard   = 2.dp   // Soft card elevation
}
