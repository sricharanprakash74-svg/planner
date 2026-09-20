package com.example.plannerapp.theme

import androidx.compose.ui.graphics.Color

// ── The 4 Core Brand Colors (Dark to Light) ──────────────────────────────────
val BrandColor1 = Color(0xFFD4720E) // Deep burnt orange (Primary brand color)
val BrandColor2 = Color(0xFFE89600) // Amber orange (Secondary / active states)
val BrandColor3 = Color(0xFFF0BC00) // Golden amber (Accent / highlights / checkmarks)
val BrandColor4 = Color(0xFFF5D400) // Bright yellow (Surface tint base only)

// ── Accessible Semantic Variants (WCAG AA >= 4.5:1 Compliant) ─────────────────
// Primary button surface with white text (#FFFFFF): Contrast 4.55:1 (passes WCAG AA)
val AppPrimaryButton            = Color(0xFFB85E08)
// Pressed state of primary button: darkens to provide tactile visual feedback
val AppPrimaryButtonPressed      = Color(0xFF9E4F04)
// Accessible text on yellow-tinted surfaces (rgba(#F5D400, 0.15) over white): Contrast 5.06:1
val AppPrimaryTextOnLightTint   = Color(0xFFA85400)

// ── Semantic Role Mapping ───────────────────────────────────────────────────
val AppPrimary                  = BrandColor1 // #D4720E: icons, borders, progress, indicators, focus
val AppSecondary                = BrandColor2 // #E89600: secondary icon tints, partial step indicators
val AppAccent                   = BrandColor3 // #F0BC00: streak badges, paywall checkmarks, unread indicators

// Surface Tints (NEVER used at full opacity for text-bearing backgrounds)
val AppSurfaceTintLight         = BrandColor4.copy(alpha = 0.12f)
val AppSurfaceTintSelectedChip  = BrandColor4.copy(alpha = 0.15f)
val AppSurfaceTintDark          = BrandColor4.copy(alpha = 0.10f)

// ── Neutrals (Preserved Exactly) ─────────────────────────────────────────────
val AppBackgroundLight          = Color(0xFFF7F8FA) // Light warm off-white
val AppSurfaceLight             = Color(0xFFFFFFFF) // Pure white cards & modals
val AppBackgroundDark           = Color(0xFF0F1117) // Deep slate black
val AppSurfaceDark              = Color(0xFF1C1F26) // Elevated charcoal surface
val AppTextPrimaryLight         = Color(0xFF1A1D23) // Near-black high contrast text
val AppTextSecondaryLight       = Color(0xFF6B7280) // Medium gray for subtext, hints, labels
val AppTextPrimaryDark          = Color(0xFFF3F4F6) // High contrast light text in dark mode
val AppTextSecondaryDark        = Color(0xFF9CA3AF) // Muted gray secondary text in dark mode
val AppDisabledBgLight          = Color(0xFFE2E5EA) // Disabled button & segment background
val AppDisabledText             = Color(0xFF9CA3AF) // Disabled text & icon placeholder
val AppFieldBgLight             = Color(0xFFF0F2F5) // Soft neutral input background
val AppFieldBgDark              = Color(0xFF262A34) // Dark input field background
val AppBorderLight              = Color(0xFFE2E5EA) // Subtle 1dp borders
val AppBorderDark               = Color(0xFF2D323E) // Subtle dark border
val AppDisabledBgDark           = Color(0xFF2D323E) // Disabled button background in dark mode

// Functional Colors
val AppSuccess                  = BrandColor3       // #F0BC00 (golden accent on paywall / streak)
val AppError                    = Color(0xFFEF4444) // Error alerts & validation rings

// Dark Mode Adaptations
val AppPrimaryDark              = BrandColor1       // #D4720E (accessible on dark surfaces)
val AppFocusBorderDark          = BrandColor2       // #E89600 (stepped up one shade for dark visibility)
val AppAccentDark               = BrandColor3       // #F0BC00

// Backward-compatible aliases
val Purple80                    = AppPrimaryDark
val PurpleGrey80                = AppTextSecondaryDark
val Pink80                      = AppAccent
val Purple40                    = AppPrimary
val PurpleGrey40                = AppTextSecondaryLight
val Pink40                      = AppAccent
val AppPrimarySubtleLight       = AppSurfaceTintSelectedChip
val AppPrimarySubtleDark        = AppSurfaceTintDark
