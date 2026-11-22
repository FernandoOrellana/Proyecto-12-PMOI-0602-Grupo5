@file:OptIn(ExperimentalMaterial3Api::class)

package com.uth.proyectofinal.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.flow.Flow

@Composable
fun AppScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    messages: Flow<String>? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val host = remember { SnackbarHostState() }
    LaunchedEffect(messages) {
        messages?.collect { host.showSnackbar(it) }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                actions = actions,
            )
        },
        snackbarHost = { SnackbarHost(host) },
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

@Composable
fun OnResume(action: () -> Unit) {
    LifecycleResumeEffect(Unit) {
        action()
        onPauseOrDispose { }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        if (onRetry != null) {
            TextButton(onClick = onRetry) { Text("Reintentar") }
        }
    }
}

@Composable
fun EmptyBox(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    password: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
) {
    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            isError = error != null,
            singleLine = singleLine,
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard),
        )
        if (error != null) FieldError(error)
    }
}

@Composable
fun FieldError(message: String) {
    Text(
        message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 16.dp, top = 2.dp),
    )
}

@Composable
fun ErrorText(message: String?) {
    if (message != null) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ActionButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    Button(onClick = onClick, enabled = !loading, modifier = modifier) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(text)
        }
    }
}

data class Option(val id: Int, val label: String)

@Composable
fun SelectField(
    label: String,
    options: List<Option>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    error: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = options.firstOrNull { it.id == selected }?.label.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                isError = error != null,
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onSelect(option.id)
                            expanded = false
                        },
                    )
                }
            }
        }
        if (error != null) FieldError(error)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun EstadoBadge(text: String) {
    val (bgColor, textColor) = when (text.uppercase()) {
        "ATENDIDA" -> androidx.compose.ui.graphics.Color(0xFFE8F5E9) to androidx.compose.ui.graphics.Color(0xFF2E7D32)
        "CONFIRMADA" -> androidx.compose.ui.graphics.Color(0xFFE3F2FD) to androidx.compose.ui.graphics.Color(0xFF1565C0)
        "EN_CONSULTA" -> androidx.compose.ui.graphics.Color(0xFFEDE7F6) to androidx.compose.ui.graphics.Color(0xFF4527A0)
        "PENDIENTE" -> androidx.compose.ui.graphics.Color(0xFFFFF8E1) to androidx.compose.ui.graphics.Color(0xFFF57F17)
        "CANCELADA" -> androidx.compose.ui.graphics.Color(0xFFFFEBEE) to androidx.compose.ui.graphics.Color(0xFFC62828)
        "POR COBRAR" -> androidx.compose.ui.graphics.Color(0xFFFFF3E0) to androidx.compose.ui.graphics.Color(0xFFE65100)
        "ATENDIDA · YA PAGADO" -> androidx.compose.ui.graphics.Color(0xFFE8F5E9) to androidx.compose.ui.graphics.Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor,
    ) {
        Text(
            text,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun <T> PagedColumn(
    state: PagedState<T>,
    emptyText: String,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable (T) -> Unit,
) {
    when {
        state.items.isEmpty() && state.loading -> LoadingBox(modifier)
        state.items.isEmpty() && state.error != null -> ErrorBox(state.error, onLoadMore, modifier)
        state.items.isEmpty() -> EmptyBox(emptyText, modifier)
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.items) { row(it) }
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (state.loading) CircularProgressIndicator(Modifier.size(24.dp))
                    ErrorText(state.error)
                    if (!state.loading && state.hasMore) {
                        TextButton(onClick = onLoadMore) { Text("Cargar más") }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleCard(title: String, subtitle: String?, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    val body: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            trailing()
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) { body() }
    } else {
        Card(modifier = Modifier.fillMaxWidth()) { body() }
    }
}

@Composable
fun AddFab(description: String, onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick) {
        Icon(Icons.Default.Add, contentDescription = description)
    }
}

@Composable
fun EditIcon(description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Default.Edit, contentDescription = description)
    }
}

@Composable
fun DeleteIcon(description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Default.Delete, contentDescription = description)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp),
    )
}
