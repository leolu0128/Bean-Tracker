package com.leolu.beantracker.ui

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leolu.beantracker.R

private const val DAY_MILLIS = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: AppViewModel, e: EditorState) {
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
    val roasterCovers by vm.roasterCovers.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var pickDate by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = vm::closeEditor) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                title = { Text(stringResource(if (e.isNew) R.string.new_bean else R.string.edit_bean)) },
                actions = {
                    if (!e.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = vm::save,
                    enabled = !e.saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp),
                ) { Text(stringResource(R.string.save)) }
            }
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            val savedCover = roasterCovers[e.roaster.trim().lowercase()]
            CoverPicker(vm, e, savedCover)

            SuggestTextField(
                value = e.roaster,
                onValueChange = { e.roaster = it },
                label = stringResource(R.string.field_roaster),
                suggestions = suggestions.roasters,
                isError = e.showErrors && e.roasterError,
                supportingText = if (e.showErrors && e.roasterError) stringResource(R.string.error_roaster) else null,
            )
            val nameError = e.showErrors && e.nameOrCountryError
            OutlinedTextField(
                value = e.name,
                onValueChange = { e.name = it },
                label = { Text(stringResource(R.string.field_name)) },
                singleLine = true,
                isError = nameError,
                supportingText = {
                    Text(stringResource(if (nameError) R.string.error_name_or_country else R.string.name_or_country_hint))
                },
                modifier = Modifier.fillMaxWidth(),
            )
            ChipTextField(
                values = e.countries,
                draft = e.countryDraft,
                onDraftChange = { e.countryDraft = it },
                label = stringResource(R.string.field_country),
                suggestions = suggestions.countries,
                supportingText = stringResource(R.string.multi_hint),
            )
            SuggestTextField(e.region, { e.region = it }, stringResource(R.string.field_region), suggestions.regions)
            SuggestTextField(e.farm, { e.farm = it }, stringResource(R.string.field_farm), suggestions.farms)
            SuggestTextField(e.variety, { e.variety = it }, stringResource(R.string.field_variety), suggestions.varieties)
            ChipTextField(
                values = e.processes,
                draft = e.processDraft,
                onDraftChange = { e.processDraft = it },
                label = stringResource(R.string.field_process),
                suggestions = suggestions.processes,
                supportingText = stringResource(R.string.multi_hint),
            )
            RoastLevelPicker(e.roastLevel) { e.roastLevel = it }
            RoastDateField(e.roastDate, onPick = { pickDate = true }, onClear = { e.roastDate = null })
            OutlinedTextField(
                value = e.gramsText,
                onValueChange = { text -> e.gramsText = text.filter(Char::isDigit).take(5) },
                label = { Text(stringResource(R.string.field_grams)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = e.roastDate?.times(DAY_MILLIS))
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { e.roastDate = it / DAY_MILLIS }
                    pickDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) { DatePicker(state) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete()
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun CoverPicker(vm: AppViewModel, e: EditorState, savedCover: String?) {
    val context = LocalContext.current
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(vm::importCover)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        if (taken) vm.importCover(Uri.fromFile(vm.cameraFile()))
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        CoverImage(e.pendingCover ?: savedCover, e.roaster, 112.dp, RoundedCornerShape(16.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.cover), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Icon(painterResource(R.drawable.ic_photo), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.cover_gallery))
                }
                OutlinedButton(onClick = {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", vm.cameraFile())
                    try {
                        camera.launch(uri)
                    } catch (ex: ActivityNotFoundException) {
                        e.coverFailed = true
                    }
                }) {
                    Icon(painterResource(R.drawable.ic_camera), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.cover_camera))
                }
            }
            Spacer(Modifier.height(4.dp))
            val hint = when {
                e.coverFailed -> R.string.cover_failed
                e.pendingCover == null && savedCover != null -> R.string.cover_reused_hint
                else -> R.string.cover_shared_hint
            }
            Text(
                stringResource(hint),
                style = MaterialTheme.typography.bodySmall,
                color = if (e.coverFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoastLevelPicker(selected: Int?, onSelect: (Int?) -> Unit) {
    Column {
        Text(
            stringResource(R.string.field_roast_level),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            stringArrayResource(R.array.roast_levels).forEachIndexed { index, label ->
                FilterChip(
                    selected = selected == index,
                    // Tapping the selected level again clears it.
                    onClick = { onSelect(if (selected == index) null else index) },
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
private fun RoastDateField(epochDay: Long?, onPick: () -> Unit, onClear: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = epochDay?.let(::formatDate).orEmpty(),
            onValueChange = {},
            label = { Text(stringResource(R.string.field_roast_date)) },
            placeholder = { Text(stringResource(R.string.pick_date)) },
            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onPick),
        )
        if (epochDay != null) {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
            }
        }
    }
}
