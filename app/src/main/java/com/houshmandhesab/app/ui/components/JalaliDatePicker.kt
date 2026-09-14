package com.houshmandhesab.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.houshmandhesab.app.R
import com.houshmandhesab.app.util.Format
import com.houshmandhesab.app.util.JDate
import com.houshmandhesab.app.util.Jalali

@Composable
fun JalaliDatePickerDialog(
    initial: JDate,
    onDismiss: () -> Unit,
    onSelected: (JDate) -> Unit
) {
    var jy by remember { mutableIntStateOf(initial.jy) }
    var jm by remember { mutableIntStateOf(initial.jm) }
    var selectedJd by remember { mutableIntStateOf(initial.jd) }

    val daysInMonth = Jalali.monthLength(jy, jm)
    val firstOffset = remember(jy, jm) {
        val g = Jalali.jalaliToLocalDate(jy, jm, 1)
        (g.dayOfWeek.value + 1) % 7
    }
    val today = Jalali.today()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (jm == 1) { jm = 12; jy -= 1 } else jm -= 1
                        if (selectedJd > Jalali.monthLength(jy, jm)) selectedJd = Jalali.monthLength(jy, jm)
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                    }
                    Text(
                        "${Jalali.monthName(jm)} ${Format.toPersianDigits(jy.toString())}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = {
                        if (jm == 12) { jm = 1; jy += 1 } else jm += 1
                        if (selectedJd > Jalali.monthLength(jy, jm)) selectedJd = Jalali.monthLength(jy, jm)
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null)
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach { d ->
                        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                            Text(
                                d,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                val cells: List<Int?> = List(firstOffset) { null } + (1..daysInMonth).toList()
                cells.chunked(7).forEach { week ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        week.forEach { day ->
                            Box(Modifier.size(34.dp)) {
                                if (day != null) {
                                    val isSelected = day == selectedJd
                                    val isToday = day == today.jd && jm == today.jm && jy == today.jy
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .size(34.dp)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                CircleShape
                                            )
                                            .then(
                                                if (isToday && !isSelected) Modifier.border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                    CircleShape
                                                ) else Modifier
                                            )
                                            .clickable { selectedJd = day },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            Format.toPersianDigits(day.toString()),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                        repeat(7 - week.size) { Box(Modifier.size(34.dp)) }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        jy = today.jy; jm = today.jm; selectedJd = today.jd
                    }) { Text(stringResource(R.string.today)) }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = { onSelected(JDate(jy, jm, selectedJd)) }) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}
