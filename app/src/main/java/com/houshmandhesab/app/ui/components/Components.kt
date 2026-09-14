package com.houshmandhesab.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.houshmandhesab.app.ui.theme.AccentViolet
import com.houshmandhesab.app.ui.theme.IncomeGreen
import com.houshmandhesab.app.util.Format
import com.houshmandhesab.app.util.toColor

fun categoryIcon(name: String): ImageVector = when (name) {
    "restaurant" -> Icons.Rounded.Restaurant
    "directions_car" -> Icons.Rounded.DirectionsCar
    "shopping_bag" -> Icons.Rounded.ShoppingBag
    "receipt_long" -> Icons.Rounded.ReceiptLong
    "movie" -> Icons.Rounded.Movie
    "favorite" -> Icons.Rounded.Favorite
    "home" -> Icons.Rounded.Home
    "school" -> Icons.Rounded.School
    "local_cafe" -> Icons.Rounded.LocalCafe
    "card_giftcard" -> Icons.Rounded.CardGiftcard
    "payments" -> Icons.Rounded.Payments
    "work" -> Icons.Rounded.Work
    "trending_up" -> Icons.Rounded.TrendingUp
    "storefront" -> Icons.Rounded.Storefront
    "account_balance" -> Icons.Rounded.AccountBalance
    "flight" -> Icons.Rounded.Flight
    "fitness_center" -> Icons.Rounded.FitnessCenter
    "pets" -> Icons.Rounded.Pets
    "music_note" -> Icons.Rounded.MusicNote
    "phone_iphone" -> Icons.Rounded.PhoneIphone
    else -> Icons.Rounded.MoreHoriz
}

val CATEGORY_ICONS = listOf(
    "restaurant", "local_cafe", "directions_car", "shopping_bag", "receipt_long",
    "movie", "favorite", "home", "school", "card_giftcard",
    "payments", "work", "trending_up", "storefront", "account_balance",
    "flight", "fitness_center", "pets", "music_note", "phone_iphone", "more_horiz"
)

val COLOR_PALETTE = listOf(
    0xFF34D399, 0xFF60A5FA, 0xFFF472B6, 0xFFF59E0B, 0xFFA78BFA,
    0xFFF87171, 0xFF38BDF8, 0xFF94A3B8, 0xFFD97706, 0xFFFB7185
)

@Composable
fun CategoryIcon(icon: String, color: Long, size: Dp = 44.dp, iconSize: Dp = 22.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(toColor(color).copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = categoryIcon(icon),
            contentDescription = null,
            tint = toColor(color),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun MoneyText(
    amount: Long,
    persianDigits: Boolean,
    symbol: String = "",
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Text(
        text = Format.money(amount, persianDigits, symbol),
        style = style,
        color = color,
        modifier = modifier
    )
}

@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradient: List<Color> = listOf(Color(0xFF0EA968), Color(0xFF0D9488)),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(Brush.linearGradient(gradient), RoundedCornerShape(24.dp))
            .padding(20.dp),
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                RoundedCornerShape(20.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (actionText != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionText) }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(56.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text("OK", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("✕") }
        }
    )
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    GlassCard(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

val SampleAccent: Color = AccentViolet
