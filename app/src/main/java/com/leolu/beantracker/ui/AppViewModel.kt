package com.leolu.beantracker.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.leolu.beantracker.BeanTrackerApp
import com.leolu.beantracker.R
import com.leolu.beantracker.data.BeanItem
import com.leolu.beantracker.data.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ListTab { Active, Finished }

data class Filters(
    val roasters: Set<String> = emptySet(),
    val countries: Set<String> = emptySet(),
    val processes: Set<String> = emptySet(),
    val roastLevels: Set<Int> = emptySet(),
) {
    val count get() = roasters.size + countries.size + processes.size + roastLevels.size
}

data class ListState(
    val loaded: Boolean = false,
    val active: List<BeanItem> = emptyList(),
    val finished: List<BeanItem> = emptyList(),
)

/** Previously entered values, most used first, for autocomplete and filters. */
data class Suggestions(
    val roasters: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val regions: List<String> = emptyList(),
    val farms: List<String> = emptyList(),
    val varieties: List<String> = emptyList(),
    val processes: List<String> = emptyList(),
)

/**
 * The list order (and which tab a bean sits in) is a snapshot taken at launch,
 * on tab switch and after editing, so cards never jump away under the finger
 * while the quick buttons are being tapped.
 */
private data class Order(val active: List<Long>, val finished: List<Long>)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as BeanTrackerApp).repository
    private val prefs = (app as BeanTrackerApp).prefs

    var themeMode by mutableStateOf(prefs.themeMode)
        private set

    var tab by mutableStateOf(ListTab.Active)
        private set

    var editor by mutableStateOf<EditorState?>(null)
        private set

    val query = MutableStateFlow("")
    val filters = MutableStateFlow(Filters())
    private val order = MutableStateFlow<Order?>(null)

    private val items = repo.items.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val roasterCovers: StateFlow<Map<String, String?>> = repo.roasters
        .map { list -> list.associate { it.name.lowercase() to it.coverPath } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val suggestions: StateFlow<Suggestions> = combine(items, repo.roasters) { items, roasters ->
        val beans = items.orEmpty()
        fun rank(values: List<String>) = values.filter { it.isNotBlank() }
            .groupingBy { it }.eachCount().entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
        Suggestions(
            roasters = rank(beans.map { it.roasterName } + roasters.map { it.name }),
            countries = rank(beans.flatMap { it.bean.countries }),
            regions = rank(beans.map { it.bean.region }),
            farms = rank(beans.map { it.bean.farm }),
            varieties = rank(beans.map { it.bean.variety }),
            processes = rank(beans.flatMap { it.bean.processes }),
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, Suggestions())

    val listState: StateFlow<ListState> = combine(items, order, query, filters) { items, order, q, f ->
        if (items == null || order == null) return@combine ListState()
        val byId = items.associateBy { it.bean.id }
        val known = order.active.toHashSet().apply { addAll(order.finished) }
        // Beans added since the snapshot go on top of the tab matching their grams.
        val fresh = items.filter { it.bean.id !in known }
        val active = fresh.filter { it.bean.grams > 0 } + order.active.mapNotNull(byId::get)
        val finished = fresh.filter { it.bean.grams == 0 } + order.finished.mapNotNull(byId::get)
        val terms = q.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val levels = getApplication<Application>().resources.getStringArray(R.array.roast_levels)
        ListState(
            loaded = true,
            active = active.filter { it.matches(terms, f, levels) },
            finished = finished.filter { it.matches(terms, f, levels) },
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, ListState())

    init {
        refreshOrder()
    }

    fun refreshOrder() {
        viewModelScope.launch {
            val current = repo.currentItems()
            order.value = Order(
                active = current.filter { it.bean.grams > 0 }.map { it.bean.id },
                finished = current.filter { it.bean.grams == 0 }.map { it.bean.id },
            )
        }
    }

    fun selectTab(value: ListTab) {
        if (tab == value) return
        tab = value
        refreshOrder()
    }

    fun setTheme(mode: ThemeMode) {
        prefs.themeMode = mode
        themeMode = mode
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilters(value: Filters) {
        filters.value = value
    }

    fun adjust(beanId: Long, delta: Int) {
        viewModelScope.launch { repo.adjust(beanId, delta) }
    }

    fun undo(beanId: Long) {
        viewModelScope.launch { repo.undo(beanId) }
    }

    fun openNew() {
        editor = EditorState(0)
    }

    fun openEdit(item: BeanItem) {
        editor = EditorState.from(item)
    }

    fun closeEditor() {
        editor?.pendingCover?.let(repo.covers::delete)
        editor = null
    }

    fun cameraFile() = repo.covers.cameraFile()

    fun importCover(uri: Uri) {
        val target = editor ?: return
        viewModelScope.launch {
            val path = repo.covers.import(uri)
            if (path == null) {
                target.coverFailed = true
                return@launch
            }
            target.coverFailed = false
            target.pendingCover?.let(repo.covers::delete)
            target.pendingCover = path
        }
    }

    fun save() {
        val e = editor ?: return
        if (e.saving) return
        e.commitDrafts()
        if (e.roasterError || e.nameOrCountryError) {
            e.showErrors = true
            return
        }
        e.saving = true
        viewModelScope.launch {
            repo.save(e.toInput(), e.pendingCover)
            if (editor === e) editor = null
            refreshOrder()
        }
    }

    fun delete() {
        val e = editor ?: return
        viewModelScope.launch {
            repo.delete(e.id)
            e.pendingCover?.let(repo.covers::delete)
            if (editor === e) editor = null
            refreshOrder()
        }
    }
}

private fun BeanItem.matches(terms: List<String>, f: Filters, levels: Array<String>): Boolean {
    if (f.roasters.isNotEmpty() && roasterName !in f.roasters) return false
    if (f.countries.isNotEmpty() && bean.countries.none { it in f.countries }) return false
    if (f.processes.isNotEmpty() && bean.processes.none { it in f.processes }) return false
    if (f.roastLevels.isNotEmpty() && bean.roastLevel?.let { it in f.roastLevels } != true) return false
    if (terms.isEmpty()) return true
    val haystack = buildString {
        append(roasterName).append(' ').append(bean.name).append(' ')
        bean.countries.forEach { append(it).append(' ') }
        append(bean.region).append(' ').append(bean.farm).append(' ').append(bean.variety).append(' ')
        bean.processes.forEach { append(it).append(' ') }
        bean.roastLevel?.let { levels.getOrNull(it) }?.let { append(it) }
    }.lowercase()
    return terms.all { it in haystack }
}
