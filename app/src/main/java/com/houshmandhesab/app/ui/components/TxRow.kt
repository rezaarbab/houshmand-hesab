package com.houshmandhesab.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.houshmandhesab.app.data.db.TxWithCategory
import com.houshmandhesab.app.ui.theme.ExpenseRed
import com.houshmandhesab.app.ui.theme.IncomeGreen
import com.houshmandhesab.app.ui.theme.TransferBlue
import com.houshmandhesab.app.util.Jalali
import com.houshmandhesab.app.util.toColor

@Composable
fun TxRow(
    tx: TxWithCategory,
    persianDigits: Boolean,
    onClick: () -> Unit
) {
    val isExpense = tx.tx.type == "EXPENSE"
    val isTransfer = tx.tx.type == "TRANSFER"
    val amountColor = when {
        isTransfer -> TransferBlue
        isExpense -> ExpenseRed
        else -> IncomeGreen
    }
    val prefix = when {
        isExpense -> "−"
        isTransfer -> "⇄"
        else -> "+"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(
            icon = tx.category?.icon ?: "more_horiz",
            color = tx.category?.color ?: 0xFF94A3B8
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        ) {
            Text(
                text = tx.tx.note.ifBlank { tx.category?.name ?: "" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tx.category?.name ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = Jalali.format(tx.tx.date, persianDigits),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 10.dp)
        )
        Text(
            text = prefix + " " + com.houshmandhesab.app.util.Format.money(tx.tx.amount, persianDigits),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}
