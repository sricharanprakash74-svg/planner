package com.example.plannerapp.theme

import androidx.compose.ui.unit.dp

/**
 * Central spatial grid for PlannerApp.
 *
 * All margins, paddings, gutters, and icon boxes are locked to multiples of 4.
 * This enforces a strict 4pt grid, with the primary rhythm on 8pt increments.
 */
object AppDimens {

    // ── Base spacing scale (4pt grid) ──────────────────────────────────────
    val Space2  = 2.dp   // micro separator
    val Space4  = 4.dp   // nano gap
    val Space8  = 8.dp   // compact gap
    val Space12 = 12.dp  // small gap
    val Space16 = 16.dp  // base unit (primary screen margin)
    val Space20 = 20.dp  // medium gap
    val Space24 = 24.dp  // large gap / section spacing
    val Space32 = 32.dp  // extra-large
    val Space40 = 40.dp  // jumbo
    val Space48 = 48.dp  // display

    // ── Corner radii ───────────────────────────────────────────────────────
    /** Default card corner radius. */
    val CornerCard      = 16.dp
    /** Compact corner for chips, small surfaces. */
    val CornerCompact   = 12.dp
    /** Pill / badge corner. */
    val CornerPill      = 20.dp
    /** Micro corner for small chips. */
    val CornerMicro     = 8.dp

    // ── Icon sizes ─────────────────────────────────────────────────────────
    val IconSizeXs      = 12.dp
    val IconSizeSm      = 16.dp
    val IconSizeMd      = 20.dp
    val IconSizeLg      = 24.dp
    val IconSizeXl      = 32.dp

    // ── Icon container boxes ───────────────────────────────────────────────
    /** Small icon box (e.g., badge, mini avatar). */
    val IconBoxSm       = 32.dp
    /** Medium icon box (e.g., brand mark in drawer). */
    val IconBoxMd       = 40.dp
    /** Large icon box (e.g., empty-state illustration well). */
    val IconBoxLg       = 48.dp
    /** XL icon box (e.g., auth screen brand icon). */
    val IconBoxXl       = 56.dp

    // ── Avatar sizes ───────────────────────────────────────────────────────
    val AvatarSm        = 32.dp
    val AvatarMd        = 48.dp
    val AvatarLg        = 80.dp

    // ── Elevation / border ─────────────────────────────────────────────────
    /** Ambient card border — preferred over harsh drop shadows. */
    val BorderThin      = 1.dp
    val ElevationNone   = 0.dp
    val ElevationCard   = 0.dp  // Cards use border strokes, not elevation
}
