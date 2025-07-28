package com.arny.dnsrewriter.ui.main


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.arny.dnsrewriter.domain.model.DnsRule
import com.arny.dnsrewriter.service.CustomVpnService
import com.arny.dnsrewriter.service.VpnStateLogger
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val vpnServiceIsRunning by CustomVpnService.isRunning.collectAsState() // Подписываемся на реальный статус
    val context = LocalContext.current

    // Launcher для запроса разрешения на VPN
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            context.startVpnService()
        }
    }

    // Launcher для выбора файла
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                    reader.readText()
                }
                if (content != null) {
                    viewModel.onEvent(MainScreenEvent.ImportFile(content))
                }
            } catch (e: Exception) {
                // Обработка ошибки чтения файла
                VpnStateLogger.log("Ошибка чтения файла: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DNS Rewriter") },
                actions = {
                    IconButton(onClick = { filePickerLauncher.launch(arrayOf("text/plain", "*/*")) }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Импортировать hosts.txt")
                    }
                    IconButton(onClick = { viewModel.onEvent(MainScreenEvent.AddRuleClicked) }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить правило")
                    }
                }
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Секция управления VPN
            VpnControlSection(
                isRunning = vpnServiceIsRunning, // Используем реальный статус из сервиса
                onToggle = {
                    if (vpnServiceIsRunning) {
                        context.stopVpnService()
                    } else {
                        val vpnIntent = VpnService.prepare(context)
                        if (vpnIntent != null) {
                            vpnPermissionLauncher.launch(vpnIntent)
                        } else {
                            context.startVpnService()
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Divider()

            // Список правил (занимает 60% экрана)
            RuleListSection(
                rules = state.rules,
                onToggle = { rule -> viewModel.onEvent(MainScreenEvent.ToggleRule(rule)) },
                onDelete = { rule -> viewModel.onEvent(MainScreenEvent.DeleteRule(rule)) },
                modifier = Modifier.weight(0.6f)
            )

            Divider()

            // Секция логов (занимает 40% экрана)
            LogViewSection(
                logs = state.logs,
                modifier = Modifier.weight(0.4f)
            )
        }

        // Диалог добавления правила
        if (state.showAddRuleDialog) {
            AddRuleDialog(
                domain = state.newRuleDomain,
                ip = state.newRuleIp,
                onDomainChange = { viewModel.onEvent(MainScreenEvent.OnDomainChanged(it)) },
                onIpChange = { viewModel.onEvent(MainScreenEvent.OnIpChanged(it)) },
                onConfirm = { viewModel.onEvent(MainScreenEvent.ConfirmAddRule) },
                onDismiss = { viewModel.onEvent(MainScreenEvent.DismissAddRuleDialog) }
            )
        }
    }
}

@Composable
fun VpnControlSection(isRunning: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (isRunning) "Сервис запущен" else "Сервис остановлен",
            style = MaterialTheme.typography.titleMedium,
            color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Button(onClick = onToggle) {
            Text(if (isRunning) "Остановить" else "Запустить")
        }
    }
}


@Composable
fun RuleListSection(
    rules: List<DnsRule>,
    onToggle: (DnsRule) -> Unit,
    onDelete: (DnsRule) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(rules, key = { it.id }) { rule ->
            RuleItem(
                rule = rule,
                onToggle = { onToggle(rule) },
                onDelete = { onDelete(rule) }
            )
            Divider()
        }
    }
}

@Composable
fun RuleItem(rule: DnsRule, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(checked = rule.isEnabled, onCheckedChange = { onToggle() })
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(rule.domain, style = MaterialTheme.typography.bodyLarge)
            Text(rule.ipAddress, style = MaterialTheme.typography.bodySmall, color = LocalContentColor.current.copy(alpha = 0.7f))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Удалить")
        }
    }
}

@Composable
fun LogViewSection(logs: List<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Логи сервиса",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            reverseLayout = true
        ) {
            items(logs) { log ->
                Text(log, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun AddRuleDialog(
    domain: String,
    ip: String,
    onDomainChange: (String) -> Unit,
    onIpChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить правило") },
        text = {
            Column {
                OutlinedTextField(
                    value = domain,
                    onValueChange = onDomainChange,
                    label = { Text("Домен") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = ip,
                    onValueChange = onIpChange,
                    label = { Text("IP адрес") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

// Вспомогательные функции, чтобы не засорять Context
private fun Context.startVpnService() {
    val intent = Intent(this, CustomVpnService::class.java).apply {
        action = CustomVpnService.ACTION_START
    }
    startService(intent)
}

private fun Context.stopVpnService() {
    val intent = Intent(this, CustomVpnService::class.java).apply {
        action = CustomVpnService.ACTION_STOP
    }
    startService(intent)
}