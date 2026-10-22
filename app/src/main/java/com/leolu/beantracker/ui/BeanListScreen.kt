package com.leolu.beantracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leolu.beantracker.R
import com.leolu.beantracker.data.BeanItem
import com.leolu.beantracker.data.ThemeMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeanListScreen(vm: AppViewModel) {
    val state by vm.listState.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    var searching by rememberSaveable { mutableStateOf(false) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    fun closeSearch() {
        searching = false
        vm.setQuery("")
    }
    BackHandler(enabled = searching) { closeSearch() }

    Scaffold(
        topBar = {
            Column {
                if (searching) {
                    SearchBar(query, vm::setQuery, onClose = ::closeSearch) {
                        FilterButton(filters.count) { showFilters = true }
                    }
                } else {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name)) },
                        actions = {
                            IconButton(onClick = { searching = true }) {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                            }
                            FilterButton(filters.count) { showFilters = true }
                            ThemeMenu(vm.themeMode, vm::setTheme)
                        },
                    )
                }
                PrimaryTabRow(selectedTabIndex = vm.tab.ordinal) {
                    Tab(
                        selected = vm.tab == ListTab.Active,
                        onClick = { vm.selectTab(ListTab.Active) },
                        text = { Text(stringResource(R.string.tab_active, state.active.size)) },
                    )
                    Tab(
                        selected = vm.tab == ListTab.Finished,
                        onClick = { vm.selectTab(ListTab.Finished) },
                        text = { Text(stringResource(R.string.tab_finished, state.finished.size)) },
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::openNew) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_bean))
            }
        },
    ) { padding ->
        val list = if (vm.tab == ListTab.Active) state.active else state.finished
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                // Draw nothing until the first query returns, so the empty state never flashes.
                !state.loaded -> Unit
                list.isEmpty() -> EmptyMessage(
                    when {
                        query.isNotBlank() || filters.count > 0 -> R.string.empty_search
                        vm.tab == ListTab.Active -> R.string.empty_active
                        else -> R.string.empty_finished
                    },
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(list, key = { it.bean.id }) { item ->
                        BeanCard(
                            item = item,
                            onClick = { vm.openEdit(item) },
                            onAdjust = { vm.adjust(item.bean.id, it) },
                            onUndo = { vm.undo(item.bean.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    if (showFilters) {
        val suggestions by vm.suggestions.collectAsStateWithLifecycle()
        FilterSheet(filters, suggestions, vm::setFilters, onDismiss = { showFilters = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    actions: @Composable () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )
        },
        actions = { actions() },
    )
}

@Composable
private fun FilterButton(count: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        BadgedBox(badge = { if (count > 0) Badge { Text(count.toString()) } }) {
            Icon(painterResource(R.drawable.ic_filter), contentDescription = stringResource(R.string.filter))
        }
    }
}

@Composable
private fun ThemeMenu(mode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Text(
                stringResource(R.string.theme),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            listOf(
                ThemeMode.System to R.string.theme_system,
                ThemeMode.Light to R.string.theme_light,
                ThemeMode.Dark to R.string.theme_dark,
            ).forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    onClick = {
                        onSelect(value)
                        open = false
                    },
                    trailingIcon = { if (value == mode) Icon(Icons.Default.Check, contentDescription = null) },
                )
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: Int) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(text),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

fun formatDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dateFormatter)

@Composable
private fun BeanCard(
    item: BeanItem,
    onClick: () -> Unit,
    onAdjust: (Int) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bean = item.bean
    val finished = bean.grams == 0
    val haptics = LocalHapticFeedback.current
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(if (finished) 0.55f else 1f),
            ) {
                CoverImage(item.coverPath, item.roasterName, 72.dp, RoundedCornerShape(12.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.roasterName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val countries = bean.countries.joinToString(" / ")
                    Text(
                        bean.name.ifBlank { countries },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    DetailLine(listOf(if (bean.name.isNotBlank()) countries else "", bean.region, bean.farm))
                    val roastLevel = bean.roastLevel?.let { stringArrayResource(R.array.roast_levels).getOrNull(it) }
                    DetailLine(listOf(bean.variety, bean.processes.joinToString(" / "), roastLevel.orEmpty()))
                    bean.roastDate?.let { day ->
                        val age = LocalDate.now().toEpochDay() - day
                        DetailLine(
                            listOf(
                                if (age >= 0) stringResource(R.string.roast_date_days, formatDate(day), age.toInt())
                                else stringResource(R.string.roast_date_future, formatDate(day)),
                            ),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.grams_value, bean.grams),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (finished) {
                        Text(
                            stringResource(R.string.finished_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val tap = { delta: Int ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAdjust(delta)
                }
                val buttonPadding = PaddingValues(horizontal = 0.dp)
                FilledTonalButton(onClick = { tap(-20) }, enabled = !finished, contentPadding = buttonPadding, modifier = Modifier.weight(1f)) { Text("-20") }
                FilledTonalButton(onClick = { tap(-15) }, enabled = !finished, contentPadding = buttonPadding, modifier = Modifier.weight(1f)) { Text("-15") }
                OutlinedButton(onClick = onUndo, enabled = item.canUndo, contentPadding = buttonPadding, modifier = Modifier.weight(1f)) {
                    Icon(painterResource(R.drawable.ic_undo), contentDescription = stringResource(R.string.undo))
                }
                FilledTonalButton(onClick = { tap(15) }, contentPadding = buttonPadding, modifier = Modifier.weight(1f)) { Text("+15") }
                FilledTonalButton(onClick = { tap(20) }, contentPadding = buttonPadding, modifier = Modifier.weight(1f)) { Text("+20") }
            }
        }
    }
}

@Composable
private fun DetailLine(parts: List<String>) {
    val text = parts.filter { it.isNotBlank() }.joinToString(" · ")
    if (text.isEmpty()) return
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    filters: Filters,
    suggestions: Suggestions,
    onChange: (Filters) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.filter), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { onChange(Filters()) }, enabled = filters.count > 0) {
                    Text(stringResource(R.string.filter_clear_all))
                }
            }
            @Composable
            fun Section(title: Int, options: List<String>, selected: Set<String>, update: (Set<String>) -> Filters) {
                if (options.isEmpty()) return
                Text(
                    stringResource(title).removeSuffix(" *"),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { option ->
                        val on = option in selected
                        FilterChip(
                            selected = on,
                            onClick = { onChange(update(if (on) selected - option else selected + option)) },
                            label = { Text(option) },
                        )
                    }
                }
            }
            Section(R.string.field_roaster, suggestions.roasters, filters.roasters) { filters.copy(roasters = it) }
            Section(R.string.field_country, suggestions.countries, filters.countries) { filters.copy(countries = it) }
            Section(R.string.field_process, suggestions.processes, filters.processes) { filters.copy(processes = it) }
            val levels = stringArrayResource(R.array.roast_levels)
            Section(
                R.string.field_roast_level,
                levels.toList(),
                filters.roastLevels.mapNotNull { levels.getOrNull(it) }.toSet(),
            ) { picked -> filters.copy(roastLevels = picked.map { levels.indexOf(it) }.toSet()) }
            Spacer(Modifier.height(24.dp))
        }
    }
}
