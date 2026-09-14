package com.houshmandhesab.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.houshmandhesab.app.data.db.TxWithCategory
import com.houshmandhesab.app.data.repo.WalletRepository
import com.houshmandhesab.app.ui.components.EmptyState
import com.houshmandhesab.app.ui.components.TxRow
import com.houshmandhesab.app.ui.navigation.Routes
import com.houshmandhesab.app.util.Format
import com.houshmandhesab.app.util.JDate
import com.houshmandhesab.app.util.Jalali
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val wallet: WalletRepository
) : ViewModel() {

    private val _month = MutableStateFlow(wallet.currentMonth())
    val month: MutableStateFlow<Pair<Int, Int>> = _month

    val filter = MutableStateFlow("ALL")
    val query = MutableStateFlow("")

    val persianDigits = wallet.settings.persianDigits
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val currency = wallet.settings.currencySymbol
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private fun monthRange(jy: Int, jm: Int): Pair<Long, Long> =
        Jalali.startOfMonthMillis(jy, jm) to Jalali.endOfMonthMillis(jy, jm)

    val monthTotals = _month.map { (jy, jm) -> monthRange(jy, jm) }
        .flatMapLatest { (from, to) -> wallet.monthTotals(from, to) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val items = combine(_month, filter, query) { m, f, q -> Triple(m, f, q) }
        .flatMapLatest { (m, f, q) ->
            val (from, to) = monthRange(m.first, m.second)
            val base = when (f) {
                "INCOME" -> wallet.transactionsRangeByType(from, to, "INCOME")
                "EXPENSE" -> wallet.transactionsRangeByType(from, to, "EXPENSE")
                "TRANSFER" -> wallet.transactionsRangeByType(from, to, "TRANSFER")
                else -> wallet.transactionsRange(from, to)
            }
            base.map { list ->
                if (q.isBlank()) list
                else list.filter {
                    it.tx.note.contains(q, ignoreCase = true) ||
                        (it.category?.name ?: "").contains(q, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun prevMonth() {
        val (jy, jm) = _month.value
        _month.value = if (jm == 1) jy - 1 to 12 else jy to jm - 1
    }

    fun nextMonth() {
        val (jy, jm) = _month.value
        _month.value = if (jm == 12) jy + 1 to 1 else jy to jm + 1
    }
}

data class DayGroup(
    val key: String,
    val label: String,
    val isToday: Boolean,
    val rows: List<TxWithCategory>
)

@Composable
fun TransactionsScreen(
    onNavigate: (String) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val month by viewModel.month.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val persian by viewModel.persianDigits.collectAsStateWithLifecycle()

    val todayKey = remember { Jalali.today().toString() }
    val groups = remember(items, todayKey) {
        val out = mutableListOf<DayGroup>()
        items.forEach { txw ->
            val jd: JDate = Jalali.fromMillis(txw.tx.date)
            val key = jd.toString()
            val last = out.lastOrNull()
            if (last != null && last.key == key) {
                out[out.size - 1] = last.copy(rows = last.rows + txw)
            } else {
                val dayLabel = if (persian) Format.toPersianDigits("${jd.jd} ${Jalali.monthName(jd.jm)}") else "${jd.jd} ${Jalali.monthName(jd.jm)}"
                out += DayGroup(key = key, label = dayLabel, isToday = key == todayKey, rows = listOf(txw))
            }
        }
        out
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.transactions_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::prevMonth) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                }
                Text(
                    if (persian) Format.toPersianDigits("${Jalali.monthName(month.second)} ${month.first}")
                    else "${Jalali.monthName(month.second)} ${month.first}",
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = viewModel::nextMonth) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null)
                }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.query.value = it },
            placeholder = { Text(stringResource(R.string.search_transactions)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "ALL" to R.string.filter_all,
                "INCOME" to R.string.filter_income,
                "EXPENSE" to R.string.filter_expense,
                "TRANSFER" to R.string.filter_transfer
            ).forEach { (key, label) ->
                FilterChip(
                    selected = filter == key,
                    onClick = { viewModel.filter.value = key },
                    label = { Text(stringResource(label)) }
                )
            }
        }
        if (groups.isEmpty()) {
            EmptyState(Icons.Rounded.ReceiptLong, stringResource(R.string.no_transactions))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groups.forEach { group ->
                    item(key = "header_${group.key}") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (group.isToday) stringResource(R.string.today) else group.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "${group.rows.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(group.rows, key = { it.tx.id }) { tx ->
                        TxRow(tx, persian) { onNavigate(Routes.add(id = tx.tx.id)) }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}
