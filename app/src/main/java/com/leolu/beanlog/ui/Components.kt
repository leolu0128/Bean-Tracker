package com.leolu.beanlog.ui

import androidx.compose.foundation.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.leolu.beanlog.R
import java.io.File

@Composable
fun CoverImage(path: String?, fallbackText: String, size: Dp, shape: Shape, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (path != null) {
            AsyncImage(
                model = File(path),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            Text(
                text = fallbackText.trim().take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/** Values matching what is typed, shown as tappable chips while the field is focused. */
private fun matching(suggestions: List<String>, typed: String, exclude: Collection<String>): List<String> {
    val t = typed.trim()
    return suggestions.asSequence()
        .filter { s -> exclude.none { it.equals(s, ignoreCase = true) } }
        .filter { t.isEmpty() || (it.contains(t, ignoreCase = true) && !it.equals(t, ignoreCase = true)) }
        .take(12)
        .toList()
}

@Composable
private fun SuggestionRow(values: List<String>, onPick: (String) -> Unit) {
    if (values.isEmpty()) return
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(values) { value ->
            SuggestionChip(onClick = { onPick(value) }, label = { Text(value) })
        }
    }
}

@Composable
fun SuggestTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            isError = isError,
            supportingText = supportingText?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
        )
        if (focused) SuggestionRow(matching(suggestions, value, emptyList()), onValueChange)
    }
}

/** A field that collects several values as chips, e.g. the countries of a blend. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipTextField(
    values: MutableList<String>,
    draft: String,
    onDraftChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    var focused by remember { mutableStateOf(false) }
    fun commit(text: String) {
        EditorState.addTo(values, text)
        onDraftChange("")
    }
    Column(modifier) {
        if (values.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                values.toList().forEach { value ->
                    val remove = stringResource(R.string.remove_value, value)
                    InputChip(
                        selected = false,
                        onClick = { values.remove(value) },
                        label = { Text(value) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = remove,
                                modifier = Modifier.size(InputChipDefaults.IconSize),
                            )
                        },
                    )
                }
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { text ->
                // Typing a separator turns the text before it into a chip.
                val parts = text.split(',', '，', '、', '/')
                if (parts.size > 1) {
                    parts.dropLast(1).forEach { EditorState.addTo(values, it) }
                    onDraftChange(parts.last())
                } else {
                    onDraftChange(text)
                }
            },
            label = { Text(label) },
            singleLine = true,
            supportingText = supportingText?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit(draft) }),
            trailingIcon = {
                if (draft.isNotBlank()) {
                    IconButton(onClick = { commit(draft) }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_value))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
        )
        if (focused) SuggestionRow(matching(suggestions, draft, values), ::commit)
    }
}
