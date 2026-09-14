package com.houshmandhesab.app.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.houshmandhesab.app.R
import com.houshmandhesab.app.ai.AiRepository
import com.houshmandhesab.app.data.db.Category
import com.houshmandhesab.app.data.db.TransactionEntity
import com.houshmandhesab.app.data.repo.WalletRepository
import com.houshmandhesab.app.ui.components.CategoryIcon
import com.houshmandhesab.app.ui.components.JalaliDatePickerDialog
import com.houshmandhesab.app.ui.theme.IncomeGreen
import com.houshmandhesab.app.ui.theme.TransferBlue
import com.houshmandhesab.app.util.Format
import com.houshmandhesab.app.util.Jalali
import com.houshmandhesab.app.util.toColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val wallet: WalletRepository,
    private val ai: AiRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val navType: String = savedStateHandle.get<String>("type") ?: "EXPENSE"
    val editId: Long = savedStateHandle.get<Long>("id") ?: -1L

    val accounts = wallet.accounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories = wallet.categories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val persianDigits = wallet.settings.persianDigits
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val currency = wallet.settings.currencySymbol
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _existing = MutableStateFlow<TransactionEntity?>(null)
    val existing = _existing.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved = _saved.asStateFlow()

    val suggesting = MutableStateFlow(false)
    val suggestion = MutableStateFlow<String?>(null)

    init {
        if (editId > 0) {
            viewModelScope.launch { _existing.value = wallet.transaction(editId) }
        }
    }

    fun save(tx: TransactionEntity) {
        viewModelScope.launch {
            wallet.saveTransaction(tx)
            _saved.value = true
        }
    }

    fun suggestCategory(note: String) {
        if (note.isBlank()) return
        viewModelScope.launch {
            suggesting.value = true
            val res = ai.suggestCategory(note, categories.value)
            suggestion.value = res.getOrNull()
            suggesting.value = false
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val persian by viewModel.persianDigits.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val existing by viewModel.existing.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val suggesting by viewModel.suggesting.collectAsStateWithLifecycle()
    val suggestion by viewModel.suggestion.collectAsStateWithLifecycle()

    var type by remember { mutableStateOf(viewModel.navType) }
    var amountDigits by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var toAccountId by remember { mutableStateOf<Long?>(null) }
    var dateMillis by remember { mutableStateOf(Jalali.toMillis(Jalali.today().jy, Jalali.today().jm, Jalali.today().jd)) }
    var note by remember { mutableStateOf("") }
    var attachmentPath by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        existing?.let { tx ->
            type = tx.type
            amountDigits = if (tx.amount > 0) tx.amount.toString() else ""
            categoryId = tx.categoryId
            accountId = tx.accountId
            toAccountId = tx.toAccountId
            dateMillis = tx.date
            note = tx.note
            attachmentPath = tx.attachmentPath
        }
    }
    LaunchedEffect(accounts) {
        if (accountId == null && accounts.isNotEmpty()) accountId = accounts.first().id
    }
    LaunchedEffect(saved) {
        if (saved) onBack()
    }
    LaunchedEffect(suggestion) {
        suggestion?.let { s ->
            val match = categories.firstOrNull { it.name == s }
                ?: categories.firstOrNull { s.contains(it.name) }
            if (match != null) categoryId = match.id
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                attachmentPath = copyReceipt(context, uri)
            }
        }
    }

    val filteredCategories = remember(categories, type) {
        categories.filter { it.type == type }
    }
    val selectedCategory = categories.firstOrNull { it.id == categoryId }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
            Text(
                stringResource(if (viewModel.editId > 0) R.string.edit_transaction else R.string.add_transaction),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(16.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TypeButton("EXPENSE", stringResource(R.string.type_expense), type == "EXPENSE", Modifier.weight(1f)) { type = "EXPENSE"; categoryId = null }
            TypeButton("INCOME", stringResource(R.string.type_income), type == "INCOME", Modifier.weight(1f)) { type = "INCOME"; categoryId = null }
            TypeButton("TRANSFER", stringResource(R.string.type_transfer), type == "TRANSFER", Modifier.weight(1f)) { type = "TRANSFER"; categoryId = null }
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = if (amountDigits.isEmpty()) "" else Format.money(Format.parseAmount(amountDigits), persian),
            onValueChange = { raw ->
                val parsed = Format.parseAmount(raw)
                amountDigits = if (parsed == 0L) "" else parsed.toString()
                amountError = false
            },
            label = { Text(stringResource(R.string.amount)) },
            trailingIcon = { if (currency.isNotBlank()) Text(currency) },
            isError = amountError,
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth()
        )
        if (amountError) {
            Text(
                stringResource(R.string.amount_required),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
        Spacer(Modifier.height(16.dp))

        if (type != "TRANSFER") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.category), style = MaterialTheme.typography.titleSmall)
                TextButtonSmall(
                    text = if (suggesting) stringResource(R.string.ai_suggesting) else stringResource(R.string.suggest_category),
                    enabled = note.isNotBlank() && !suggesting && filteredCategories.isNotEmpty()
                ) { viewModel.suggestCategory(note) }
            }
            if (filteredCategories.isEmpty()) {
                Text(
                    stringResource(R.string.no_categories_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredCategories.forEach { cat ->
                        CategoryPill(
                            cat = cat,
                            selected = cat.id == categoryId
                        ) { categoryId = if (categoryId == cat.id) null else cat.id }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Text(
            stringResource(if (type == "TRANSFER") R.string.from_account else R.string.account),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            accounts.forEach { acc ->
                AccountPill(name = acc.name, color = acc.color, selected = acc.id == accountId) {
                    accountId = acc.id
                }
            }
        }
        if (type == "TRANSFER") {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.to_account), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { acc ->
                    AccountPill(name = acc.name, color = acc.color, selected = acc.id == toAccountId) {
                        toAccountId = acc.id
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(Jalali.format(dateMillis, persian))
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text(stringResource(R.string.note)) },
            placeholder = { Text(stringResource(R.string.note_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        if (attachmentPath != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = File(attachmentPath!!),
                    contentDescription = stringResource(R.string.receipt_attached),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.receipt_attached), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { attachmentPath = null }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.remove_attachment))
                }
            }
        } else {
            OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Image, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.attach_receipt))
            }
        }
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val amount = Format.parseAmount(amountDigits)
                if (amount <= 0) {
                    amountError = true
                    return@Button
                }
                val from = accountId ?: return@Button
                if (type == "TRANSFER" && (toAccountId == null || toAccountId == from)) return@Button
                viewModel.save(
                    TransactionEntity(
                        id = if (viewModel.editId > 0) viewModel.editId else 0,
                        type = type,
                        amount = amount,
                        accountId = from,
                        toAccountId = if (type == "TRANSFER") toAccountId else null,
                        categoryId = if (type == "TRANSFER") null else categoryId,
                        date = dateMillis,
                        note = note.trim(),
                        attachmentPath = attachmentPath
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.save))
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initial = Jalali.fromMillis(dateMillis),
            onDismiss = { showDatePicker = false },
            onSelected = {
                dateMillis = Jalali.toMillis(it.jy, it.jm, it.jd)
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun TypeButton(value: String, label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun CategoryPill(cat: Category, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .background(
                if (selected) toColor(cat.color).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(14.dp)
            )
            .border(
                if (selected) 1.5.dp else 0.dp,
                toColor(cat.color),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
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

@Composable
private fun AccountPill(name: String, color: Long, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .background(
                if (selected) toColor(color).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(14.dp)
            )
            .border(
                if (selected) 1.5.dp else 0.dp,
                toColor(color),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(name, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun TextButtonSmall(text: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

private suspend fun copyReceipt(context: Context, uri: android.net.Uri): String? =
    withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "receipts").apply { mkdirs() }
            val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
