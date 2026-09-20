package com.example.plannerapp.ui.creator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.plannerapp.data.creator.CreatorLedgerEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EscrowLedgerList(
    transactions: List<CreatorLedgerEntity>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Escrow & Settlement Ledger",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "14-day escrow clearing and bank payout history",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = "${transactions.size} Records",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Settlement Records Yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    transactions.forEach { tx ->
                        LedgerTransactionItem(tx = tx)
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerTransactionItem(tx: CreatorLedgerEntity) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
    val formattedDate = dateFormat.format(Date(tx.createdAt))

    val now = System.currentTimeMillis()
    val isPending = tx.status == "PENDING"
    val daysRemaining = if (tx.escrowReleaseDate != null && tx.escrowReleaseDate > now) {
        val diffDays = ((tx.escrowReleaseDate - now) / (24L * 60 * 60 * 1000L)).toInt() + 1
        diffDays.coerceAtLeast(1)
    } else {
        0
    }

    // Status Chip Colors
    val chipBackground: Color
    val chipText: Color
    val chipLabel: String
    when {
        isPending && daysRemaining > 0 -> {
            chipBackground = Color(0xFFFEF3C7)
            chipText = Color(0xFFB45309)
            chipLabel = "Pending (Clears in $daysRemaining d)"
        }
        isPending -> {
            chipBackground = Color(0xFFDCFCE7)
            chipText = Color(0xFF15803D)
            chipLabel = "Available"
        }
        tx.entryType == "BANK_PAYOUT" -> {
            chipBackground = Color(0xFFE0E7FF)
            chipText = Color(0xFF4338CA)
            chipLabel = "Settled to Bank"
        }
        else -> {
            chipBackground = Color(0xFFDBEAFE)
            chipText = Color(0xFF1D4ED8)
            chipLabel = "Settled"
        }
    }

    val isNegative = tx.amountCredits < 0
    val creditSign = if (isNegative) "" else "+"
    val usdSign = if (isNegative) "" else "+"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tx.planTitle ?: if (tx.entryType == "BANK_PAYOUT") "Stripe Connect Direct Payout" else "Escrow Settlement",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = chipBackground
                ) {
                    Text(
                        text = chipLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = chipText
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$creditSign${tx.amountCredits} cr",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isNegative) Color(0xFF475569) else Color(0xFF059669)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "($usdSign$%.2f USD)".format(Locale.US, tx.equivalentUsd),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}
