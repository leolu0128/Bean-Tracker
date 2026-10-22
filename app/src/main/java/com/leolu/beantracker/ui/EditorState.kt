package com.leolu.beantracker.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.leolu.beantracker.data.BeanInput
import com.leolu.beantracker.data.BeanItem

/** Form state for adding or editing one bag of beans. */
class EditorState(val id: Long) {
    var roaster by mutableStateOf("")
    var name by mutableStateOf("")
    val countries = mutableStateListOf<String>()
    var region by mutableStateOf("")
    var farm by mutableStateOf("")
    var variety by mutableStateOf("")
    val processes = mutableStateListOf<String>()
    var gramsText by mutableStateOf("")
    var roastDate by mutableStateOf<Long?>(null)

    // Text typed into the multi-value fields but not yet turned into a chip.
    var countryDraft by mutableStateOf("")
    var processDraft by mutableStateOf("")

    /** A newly picked image, not yet saved to the roaster. */
    var pendingCover by mutableStateOf<String?>(null)
    var coverFailed by mutableStateOf(false)
    var showErrors by mutableStateOf(false)
    var saving by mutableStateOf(false)

    val isNew get() = id == 0L

    val roasterError get() = roaster.isBlank()
    val nameOrCountryError get() = name.isBlank() && countries.isEmpty() && countryDraft.isBlank()

    /** Commits any half-typed chip text so it is not lost on save. */
    fun commitDrafts() {
        addTo(countries, countryDraft)
        countryDraft = ""
        addTo(processes, processDraft)
        processDraft = ""
    }

    fun toInput() = BeanInput(
        id = id,
        roasterName = roaster.trim(),
        name = name.trim(),
        countries = countries.toList(),
        region = region.trim(),
        farm = farm.trim(),
        variety = variety.trim(),
        processes = processes.toList(),
        grams = gramsText.toIntOrNull() ?: 0,
        roastDate = roastDate,
    )

    companion object {
        fun addTo(list: MutableList<String>, value: String) {
            val v = value.trim()
            if (v.isNotEmpty() && list.none { it.equals(v, ignoreCase = true) }) list.add(v)
        }

        fun from(item: BeanItem) = EditorState(item.bean.id).apply {
            val b = item.bean
            roaster = item.roasterName
            name = b.name
            countries.addAll(b.countries)
            region = b.region
            farm = b.farm
            variety = b.variety
            processes.addAll(b.processes)
            gramsText = b.grams.toString()
            roastDate = b.roastDate
        }
    }
}
