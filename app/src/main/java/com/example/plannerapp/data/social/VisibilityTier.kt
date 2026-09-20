package com.example.plannerapp.data.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 3-tier visibility model for granular privacy across plans and posts.
 */
enum class VisibilityTier(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    PUBLIC(
        id = "public",
        title = "Public / Community",
        description = "Indexed in Community & Explore. Visible to everyone.",
        icon = Icons.Outlined.Public
    ),
    PRIVATE(
        id = "private",
        title = "Private",
        description = "Visible only to you and accepted network members.",
        icon = Icons.Outlined.Lock
    ),
    VAULT(
        id = "vault",
        title = "Vault (Only Me)",
        description = "Strictly confidential. Excluded from all public feeds.",
        icon = Icons.Outlined.Shield
    );

    companion object {
        fun fromId(id: String?): VisibilityTier = when (id?.lowercase()) {
            "private" -> PRIVATE
            "vault" -> VAULT
            else -> PUBLIC
        }
    }
}

/**
 * Reusable 3-way segmented card selector for choosing visibility during plan and post creation.
 */
@Composable
fun VisibilitySelector(
    selectedTier: VisibilityTier,
    onTierSelected: (VisibilityTier) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Content Visibility & Privacy",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        VisibilityTier.entries.forEach { tier ->
            val isSelected = tier == selectedTier
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTierSelected(tier) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tier.icon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tier.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = tier.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(
                        selected = isSelected,
                        onClick = { onTierSelected(tier) },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

/**
 * Micro-indicator chip displayed on cards to give user clear awareness of content exposure.
 */
@Composable
fun VisibilityMicroChip(
    tier: VisibilityTier,
    modifier: Modifier = Modifier
) {
    val chipBg = when (tier) {
        VisibilityTier.PUBLIC -> Color(0xFFE0E7FF)
        VisibilityTier.PRIVATE -> Color(0xFFFEF3C7)
        VisibilityTier.VAULT -> Color(0xFFF1F5F9)
    }
    val chipFg = when (tier) {
        VisibilityTier.PUBLIC -> Color(0xFF3730A3)
        VisibilityTier.PRIVATE -> Color(0xFF92400E)
        VisibilityTier.VAULT -> Color(0xFF475569)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = chipBg,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = tier.icon,
                contentDescription = null,
                tint = chipFg,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when (tier) {
                    VisibilityTier.PUBLIC -> "Public"
                    VisibilityTier.PRIVATE -> "Private"
                    VisibilityTier.VAULT -> "Vault"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = chipFg,
                fontSize = 10.sp
            )
        }
    }
}
