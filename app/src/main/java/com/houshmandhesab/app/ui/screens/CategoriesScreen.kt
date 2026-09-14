package com.houshmandhesab.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.houshmandhesab.app.R
import com.houshmandhesab.app.data.db.Category
import com.houshmandhesab.app.data.repo.WalletRepository
import com.houshmandhesab.app.ui.components.CATEGORY_ICONS
import com.houshmandhesab.app.ui.components.COLOR_PALETTE
import com.houshmandhesab.app.ui.components.CategoryIcon
import com.houshmandhesab.app.ui.components.ConfirmDialog
import com.houshmandhesab.app.ui.components.EmptyState
import com.houshmandhesab.app.ui.components.categoryIcon
import com.houshmandhesab.app.util.toColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val wallet: WalletRepository
) : ViewModel() {
    val categories = wallet.categories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(category: Category) = viewModelScope.launch { wallet.saveCategory(category) }
    fun delete(id: Long) = viewModelScope.launch { wallet.deleteCategory(id) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf("EXPENSE") }
    var editing by remember { mutableStateOf<Category?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var deletingId by remember { mutableStateOf<Long?>(null) }

    val filtered = categories.filter { it.type == tab }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.add_category))
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                }
                Text(
                    stringResource(R.string.categories_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterTab(stringResource(R.string.categories_expense), tab == "EXPENSE") { tab = "EXPENSE" }
                FilterTab(stringResource(R.string.categories_income), tab == "INCOME") { tab = "INCOME" }
            }
            if (filtered.isEmpty()) {
                EmptyState(Icons.Rounded.Category, stringResource(R.string.no_data))
            } else {
                FlowRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filtered.forEach { cat ->
                        Row(
                            Modifier
                                .background(toColor(cat.color).copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                .border(1.dp, toColor(cat.color).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .clickable { editing = cat }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIcon(icon = cat.icon, color = cat.color, size = 26.dp, iconSize = 15.dp)
                            Text(
                                cat.name,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd || editing != null) {
        CategoryDialog(
            category = editing?.let { it.copy(type = tab) } ?: Category(name = "", type = tab),
            isNew = editing == null,
            onDismiss = { showAdd = false; editing = null },
            onSave = { viewModel.save(it); showAdd = false; editing = null },
            onDelete = if (editing != null) {
                { deletingId = editing!!.id; showAdd = false; editing = null }
            } else null
        )
    }
    if (deletingId != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_category_confirm),
            onConfirm = { viewModel.delete(deletingId!!); deletingId = null },
            onDismiss = { deletingId = null }
        )
    }
}

@Composable
private fun FilterTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun CategoryDialog(
    category: Category,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(if (isNew) "" else category.name) }
    var icon by remember { mutableStateOf(if (isNew) "more_horiz" else category.icon) }
    var color by remember { mutableStateOf(if (isNew) COLOR_PALETTE.first() else category.color) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.add_category else R.string.edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(icon = icon, color = color, size = 40.dp, iconSize = 22.dp)
                    Spacer(Modifier.width(10.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.category_name)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.choose_icon), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(CATEGORY_ICONS) { key ->
                        Box(
                            Modifier
                                .size(34.dp)
                                .background(
                                    if (key == icon) toColor(color).copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    CircleShape
                                )
                                .clickable { icon = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                categoryIcon(key),
                                contentDescription = null,
                                tint = if (key == icon) toColor(color) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.choose_color), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    COLOR_PALETTE.forEach { c ->
                        Box(
                            Modifier
                                .size(28.dp)
                                .background(toColor(c), CircleShape)
                                .border(
                                    if (c == color) 3.dp else 0.dp,
                                    MaterialTheme.colorScheme.onSurface,
                                    CircleShape
                                )
                                .clickable { color = c }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { if (name.isNotBlank()) onSave(category.copy(name = name.trim(), icon = icon, color = color)) }) {
                    Text(stringResource(R.string.save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
