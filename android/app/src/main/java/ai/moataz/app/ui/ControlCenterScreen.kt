package ai.moataz.app.ui

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.moataz.app.CapabilityHealth
import ai.moataz.app.CapabilitySection
import ai.moataz.app.CapabilityStatus
import ai.moataz.app.ControlCenterState
import ai.moataz.app.ControlCenterViewModel
import ai.moataz.app.R
import ai.moataz.app.data.WorkspaceGatewayClient
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun ControlCenterScreen(
    viewModel: ControlCenterViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshAll()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        ControlHeader(state, viewModel, onBack)
        SectionRail(state.section, viewModel::setSection)
        HorizontalDivider()

        when (state.section) {
            CapabilitySection.Overview -> OverviewSection(state, viewModel)
            CapabilitySection.Providers -> ProvidersSection(state, viewModel)
            CapabilitySection.Files -> FilesSection(state, viewModel)
            CapabilitySection.Skills -> SkillsSection(state, viewModel)
            CapabilitySection.Mcp -> McpSection(state, viewModel)
            CapabilitySection.Subagents -> SubagentsSection(state, viewModel)
            CapabilitySection.Memory -> MemorySection(state, viewModel)
            CapabilitySection.ScheduledTasks -> TasksSection(state, viewModel)
            CapabilitySection.Browser -> BrowserSection(state, viewModel)
        }
    }

    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            confirmButton = {
                TextButton(onClick = viewModel::dismissMessage) { Text(stringResource(R.string.dismiss)) }
            },
            title = { Text(stringResource(R.string.action_failed)) },
            text = { Text(message) },
        )
    }
}

@Composable
private fun ControlHeader(
    state: ControlCenterState,
    viewModel: ControlCenterViewModel,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back_to_agent))
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.control_center),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.control_center_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (state.refreshing) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            IconButton(onClick = viewModel::refreshAll) {
                Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.refresh_all))
            }
        }
    }
}

@Composable
private fun SectionRail(selected: CapabilitySection, onSelect: (CapabilitySection) -> Unit) {
    val sections = listOf(
        CapabilitySection.Overview to R.string.cap_overview,
        CapabilitySection.Providers to R.string.cap_providers,
        CapabilitySection.Files to R.string.cap_files,
        CapabilitySection.Skills to R.string.cap_skills,
        CapabilitySection.Mcp to R.string.cap_mcp,
        CapabilitySection.Subagents to R.string.cap_subagents,
        CapabilitySection.Memory to R.string.cap_memory,
        CapabilitySection.ScheduledTasks to R.string.cap_tasks,
        CapabilitySection.Browser to R.string.cap_browser,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sections.forEach { (section, label) ->
            FilterChip(
                selected = selected == section,
                onClick = { onSelect(section) },
                label = { Text(stringResource(label)) },
            )
        }
    }
}

@Composable
private fun OverviewSection(state: ControlCenterState, viewModel: ControlCenterViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                state.origin.ifBlank { stringResource(R.string.status_not_configured) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.source_deerflow),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { CapabilityCard(CapabilitySection.Providers, state.providerStatus, state.providers.size, viewModel) }
        item { CapabilityCard(CapabilitySection.Files, state.filesStatus, state.files.size, viewModel) }
        item { CapabilityCard(CapabilitySection.Skills, state.skillsStatus, state.skills.size, viewModel) }
        item { CapabilityCard(CapabilitySection.Mcp, state.mcpStatus, state.mcpServers.size, viewModel) }
        item { CapabilityCard(CapabilitySection.Subagents, state.subagentsStatus, state.subagents.size, viewModel) }
        item { CapabilityCard(CapabilitySection.Memory, state.memoryStatus, state.memory?.facts?.size ?: 0, viewModel) }
        item { CapabilityCard(CapabilitySection.ScheduledTasks, state.tasksStatus, state.scheduledTasks.size, viewModel) }
        item { CapabilityCard(CapabilitySection.Browser, state.browserStatus, if (state.browser == null) 0 else 1, viewModel) }
        state.notice?.let { notice ->
            item {
                StatusPanel(notice, MaterialTheme.colorScheme.primaryContainer)
            }
        }
    }
}

@Composable
private fun CapabilityCard(
    section: CapabilitySection,
    status: CapabilityStatus,
    count: Int,
    viewModel: ControlCenterViewModel,
) {
    val (label, icon) = sectionPresentation(section)
    Card(
        onClick = { viewModel.setSection(section) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(label), fontWeight = FontWeight.SemiBold)
                Text(
                    status.message ?: statusLabel(status.health),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(count.toString(), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ProvidersSection(state: ControlCenterState, viewModel: ControlCenterViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    SectionList(
        title = stringResource(R.string.cap_providers),
        status = state.providerStatus,
        onRefresh = viewModel::refreshProviders,
        action = {
            Button(onClick = { showAdd = true }) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.add_provider))
            }
        },
    ) {
        if (state.providers.isEmpty()) {
            item { EmptyState(state.providerStatus) }
        }
        items(state.providers, key = { it.id }) { provider ->
            ItemCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(provider.name, fontWeight = FontWeight.SemiBold)
                            if (provider.isDefault) {
                                Spacer(Modifier.width(6.dp))
                                AssistChip(onClick = {}, label = { Text(stringResource(R.string.default_badge)) })
                            }
                        }
                        Text(
                            "${provider.type} · ${provider.modelCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(provider.baseUrl, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { viewModel.deleteProvider(provider.id) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                    }
                }
            }
        }
    }
    if (showAdd) ProviderDialog(viewModel, onDismiss = { showAdd = false })
}

@Composable
private fun ProviderDialog(viewModel: ControlCenterViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("openai_compatible") }
    var baseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var discovered by remember { mutableStateOf<JSONArray?>(null) }
    var resultText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && discovered != null && discovered!!.length() > 0,
                onClick = {
                    val models = discovered ?: JSONArray()
                    viewModel.saveProvider(
                        JSONObject()
                            .put("name", name)
                            .put("provider_type", type)
                            .put("base_url", baseUrl.takeIf { it.isNotBlank() })
                            .put("api_key", apiKey.takeIf { it.isNotBlank() })
                            .put("models", models),
                    )
                    onDismiss()
                },
            ) { Text(stringResource(R.string.save_provider)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        title = { Text(stringResource(R.string.add_provider)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.provider_name)) }, singleLine = true)
                OutlinedTextField(type, { type = it }, label = { Text(stringResource(R.string.provider_type)) }, singleLine = true)
                OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text(stringResource(R.string.base_url)) }, singleLine = true)
                OutlinedTextField(
                    apiKey,
                    { apiKey = it },
                    label = { Text(stringResource(R.string.api_key)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        viewModel.testProvider(type, baseUrl, apiKey) { result ->
                            resultText = result.fold({ if (it.optBoolean("ok")) "OK · ${it.optInt("model_count")}" else it.toString() }, { it.message ?: "Error" })
                        }
                    }) { Text(stringResource(R.string.test_connection)) }
                    OutlinedButton(onClick = {
                        viewModel.discoverProvider(type, baseUrl, apiKey) { result ->
                            result.onSuccess {
                                discovered = it.optJSONArray("models") ?: JSONArray()
                                resultText = "Models: ${discovered?.length() ?: 0}"
                            }.onFailure { resultText = it.message }
                        }
                    }) { Text(stringResource(R.string.discover_models)) }
                }
                resultText?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        },
    )
}

@Composable
private fun FilesSection(state: ControlCenterState, viewModel: ControlCenterViewModel) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val resolver = context.contentResolver
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: "upload.bin"
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val bytes = runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        if (bytes != null) viewModel.uploadFile(name, mime, bytes)
    }

    SectionList(
        title = stringResource(R.string.cap_files),
        status = state.filesStatus,
        onRefresh = viewModel::refreshFiles,
        action = {
            Button(
                enabled = !state.activeThreadId.isNullOrBlank(),
                onClick = { picker.launch(arrayOf("*/*")) },
            ) {
                Icon(Icons.Rounded.UploadFile, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.choose_file))
            }
        },
    ) {
        if (state.activeThreadId.isNullOrBlank()) item { StatusPanel(stringResource(R.string.active_thread_required)) }
        state.uploadLimits?.let { limits ->
            item {
                Text(
                    stringResource(
                        R.string.upload_limits,
                        limits.maxFiles,
                        limits.maxFileSize / 1024 / 1024,
                        limits.maxTotalSize / 1024 / 1024,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.files.isEmpty() && !state.activeThreadId.isNullOrBlank()) item { Text(stringResource(R.string.no_files)) }
        items(state.files, key = { it.virtualPath }) { file ->
            ItemCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(file.filename, fontWeight = FontWeight.SemiBold)
                        Text("${file.size / 1024} KB · ${file.virtualPath}", style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                    IconButton(onClick = { viewModel.deleteFile(file.filename) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillsSection(state: ControlCenterState, viewModel: ControlCenterViewModel) {
    SectionList(
        title = stringResource(R.string.cap_skills),
        status = state.skillsStatus,
        onRefresh = viewModel::refreshSkills,
        action = {
            OutlinedButton(onClick = viewModel::reloadSkills) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.reload))
            }
        },
    ) {
        if (state.skills.isEmpty()) item { EmptyState(state.skillsStatus) }
        items(state.skills, key = { it.name }) { skill ->
            ItemCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(skill.name, fontWeight = FontWeight.SemiBold)
                        Text(skill.description, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                    Switch(skill.enabled, onCheckedChange = { viewModel.setSkillEnabled(skill.name, it) })
                }
            }
        }
    }
}

@Composable
private fun McpSection(state: ControlCenterState, viewModel: ControlCenterViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    SectionList(
        title = stringResource(R.string.cap_mcp),
        status = state.mcpStatus,
        onRefresh = viewModel::refreshMcp,
        action = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::resetMcpCache) { Text(stringResource(R.string.reset_cache)) }
                Button(onClick = { showAdd = true }) { Text(stringResource(R.string.add_mcp_server)) }
            }
        },
    ) {
        if (state.mcpServers.isEmpty()) item { EmptyState(state.mcpStatus) }
        items(state.mcpServers, key = { it.name }) { server ->
            ItemCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.SettingsEthernet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(server.name, fontWeight = FontWeight.SemiBold)
                        Text("${server.transport} · ${server.description}", style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                    Switch(server.enabled, onCheckedChange = { viewModel.setMcpEnabled(server.name, it) })
                    IconButton(onClick = { viewModel.deleteMcpServer(server.name) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                    }
                }
            }
        }
    }
    if (showAdd) McpDialog(viewModel, onDismiss = { showAdd = false })
}

@Composable
private fun McpDialog(viewModel: ControlCenterViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_mcp_server)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.server_name)) }, singleLine = true)
                OutlinedTextField(url, { url = it }, label = { Text(stringResource(R.string.server_url)) }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.description)) })
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && url.isNotBlank(), onClick = {
                val server = JSONObject()
                    .put("enabled", true)
                    .put("url", url)
                    .put("description", description)
                viewModel.saveMcpServer(name, server, replacing = false)
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun SubagentsSection(state: ControlCenterState, viewModel: ControlCenterViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    SectionList(
        title = stringResource(R.string.cap_subagents),
        status = state.subagentsStatus,
        onRefresh = viewModel::refreshSubagents,
        action = { Button(onClick = { showAdd = true }) { Text(stringResource(R.string.add_subagent)) } },
    ) {
        if (state.subagents.isEmpty()) item { EmptyState(state.subagentsStatus) }
        items(state.subagents, key = { "${it.source}:${it.name}" }) { agent ->
            ItemCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(agent.displayName, fontWeight = FontWeight.SemiBold)
                        Text("${agent.source} · ${agent.model}", style = MaterialTheme.typography.bodySmall)
                        Text(agent.description, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                    if (agent.editable) {
                        IconButton(onClick = { viewModel.deleteSubagent(agent.name) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
    }
    if (showAdd) SubagentDialog(viewModel, onDismiss = { showAdd = false })
}

@Composable
private fun SubagentDialog(viewModel: ControlCenterViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("inherit") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_subagent)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.agent_name)) }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.description)) })
                OutlinedTextField(prompt, { prompt = it }, label = { Text(stringResource(R.string.system_prompt)) }, minLines = 3)
                OutlinedTextField(model, { model = it }, label = { Text(stringResource(R.string.model_or_inherit)) }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && description.isNotBlank() && prompt.isNotBlank(), onClick = {
                viewModel.createSubagent(
                    JSONObject()
                        .put("name", name)
                        .put("description", description)
                        .put("system_prompt", prompt)
                        .put("model", model)
                        .put("enabled", true),
                )
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun MemorySection(state: ControlCenterState, viewModel: ControlCenterViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    SectionList(
        title = stringResource(R.string.cap_memory),
        status = state.memoryStatus,
        onRefresh = viewModel::refreshMemory,
        action = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::reloadMemoryData) { Text(stringResource(R.string.reload)) }
                Button(onClick = { showAdd = true }) { Text(stringResource(R.string.add_memory_fact)) }
            }
        },
    ) {
        state.memory?.let { memory ->
            item { SummaryCard(stringResource(R.string.memory_work), memory.workContext) }
            item { SummaryCard(stringResource(R.string.memory_personal), memory.personalContext) }
            item { SummaryCard(stringResource(R.string.memory_top), memory.topOfMind) }
            items(memory.facts, key = { it.id }) { fact ->
                ItemCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(fact.content)
                            Text("${fact.category} · ${"%.2f".format(fact.confidence)}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { viewModel.deleteMemoryFact(fact.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = viewModel::clearMemory, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.clear_memory))
                }
            }
        } ?: item { EmptyState(state.memoryStatus) }
    }
    if (showAdd) MemoryDialog(viewModel, onDismiss = { showAdd = false })
}

@Composable
private fun MemoryDialog(viewModel: ControlCenterViewModel, onDismiss: () -> Unit) {
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("context") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_memory_fact)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(content, { content = it }, label = { Text(stringResource(R.string.fact_content)) }, minLines = 3)
                OutlinedTextField(category, { category = it }, label = { Text(stringResource(R.string.fact_category)) }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(enabled = content.isNotBlank(), onClick = {
                viewModel.addMemoryFact(content, category)
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun TasksSection(state: ControlCenterState, viewModel: ControlCenterViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    SectionList(
        title = stringResource(R.string.cap_tasks),
        status = state.tasksStatus,
        onRefresh = viewModel::refreshScheduledTasks,
        action = { Button(onClick = { showAdd = true }) { Text(stringResource(R.string.create_task)) } },
    ) {
        if (state.scheduledTasks.isEmpty()) item { EmptyState(state.tasksStatus) }
        items(state.scheduledTasks, key = { it.id }) { task ->
            ItemCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(task.title, fontWeight = FontWeight.SemiBold)
                            Text("${task.scheduleType} · ${task.status} · ${task.timezone}", style = MaterialTheme.typography.bodySmall)
                            task.nextRunAt?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                        IconButton(onClick = { viewModel.deleteScheduledTask(task.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = { viewModel.triggerScheduledTask(task.id) }) { Text(stringResource(R.string.run_now)) }
                        if (task.status == "paused") {
                            TextButton(onClick = { viewModel.resumeScheduledTask(task.id) }) { Text(stringResource(R.string.resume)) }
                        } else {
                            TextButton(onClick = { viewModel.pauseScheduledTask(task.id) }) { Text(stringResource(R.string.pause)) }
                        }
                    }
                }
            }
        }
    }
    if (showAdd) TaskDialog(viewModel, state.activeThreadId, onDismiss = { showAdd = false })
}

@Composable
private fun TaskDialog(viewModel: ControlCenterViewModel, activeThreadId: String?, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("cron") }
    var spec by remember { mutableStateOf("0 8 * * *") }
    var timezone by remember { mutableStateOf("UTC") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_task)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.task_title)) }, singleLine = true)
                OutlinedTextField(prompt, { prompt = it }, label = { Text(stringResource(R.string.task_prompt)) }, minLines = 3)
                OutlinedTextField(type, { type = it }, label = { Text(stringResource(R.string.schedule_type)) }, singleLine = true)
                OutlinedTextField(spec, { spec = it }, label = { Text(stringResource(R.string.schedule_value)) }, singleLine = true)
                OutlinedTextField(timezone, { timezone = it }, label = { Text(stringResource(R.string.timezone)) }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank() && prompt.isNotBlank() && spec.isNotBlank(), onClick = {
                val payload = JSONObject()
                    .put("context_mode", if (activeThreadId.isNullOrBlank()) "new_thread" else "reuse_thread")
                    .put("title", title)
                    .put("prompt", prompt)
                    .put("schedule_type", type)
                    .put("schedule_spec", spec)
                    .put("timezone", timezone)
                if (!activeThreadId.isNullOrBlank()) payload.put("thread_id", activeThreadId)
                viewModel.createScheduledTask(payload)
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun BrowserSection(state: ControlCenterState, viewModel: ControlCenterViewModel) {
    var url by remember { mutableStateOf(state.browser?.url.orEmpty()) }
    SectionList(
        title = stringResource(R.string.cap_browser),
        status = state.browserStatus,
        onRefresh = {},
        action = null,
    ) {
        item {
            Text(stringResource(R.string.browser_disabled_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.browser_url)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                enabled = !state.activeThreadId.isNullOrBlank() && url.isNotBlank(),
                onClick = { viewModel.navigateBrowser(url) },
            ) {
                Icon(Icons.Rounded.Language, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.navigate))
            }
        }
        state.browser?.let { result ->
            item {
                ItemCard {
                    Text(result.title.ifBlank { result.url }, fontWeight = FontWeight.SemiBold)
                    Text(result.url, style = MaterialTheme.typography.bodySmall)
                    result.screenshot?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

@Composable
private fun SectionList(
    title: String,
    status: CapabilityStatus,
    onRefresh: () -> Unit,
    action: (@Composable () -> Unit)?,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(status.message ?: statusLabel(status.health), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (status.health == CapabilityHealth.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.refresh)) }
                }
            }
        }
        if (action != null) item { action() }
        content()
    }
}

@Composable
private fun ItemCard(content: @Composable Column.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun SummaryCard(title: String, body: String) {
    if (body.isBlank()) return
    ItemCard {
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyState(status: CapabilityStatus) {
    StatusPanel(status.message ?: statusLabel(status.health))
}

@Composable
private fun StatusPanel(
    text: String,
    container: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(text, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun statusLabel(health: CapabilityHealth): String = when (health) {
    CapabilityHealth.Idle -> stringResource(R.string.not_checked)
    CapabilityHealth.Loading -> stringResource(R.string.cap_loading)
    CapabilityHealth.Ready -> stringResource(R.string.ready)
    CapabilityHealth.Unauthorized -> stringResource(R.string.unauthorized)
    CapabilityHealth.Unavailable -> stringResource(R.string.unavailable)
    CapabilityHealth.Error -> stringResource(R.string.cap_error)
}

@Composable
private fun sectionPresentation(section: CapabilitySection): Pair<Int, ImageVector> = when (section) {
    CapabilitySection.Overview -> R.string.cap_overview to Icons.Rounded.PlayArrow
    CapabilitySection.Providers -> R.string.cap_providers to Icons.Rounded.Cloud
    CapabilitySection.Files -> R.string.cap_files to Icons.Rounded.Folder
    CapabilitySection.Skills -> R.string.cap_skills to Icons.Rounded.Star
    CapabilitySection.Mcp -> R.string.cap_mcp to Icons.Rounded.SettingsEthernet
    CapabilitySection.Subagents -> R.string.cap_subagents to Icons.Rounded.SmartToy
    CapabilitySection.Memory -> R.string.cap_memory to Icons.Rounded.Memory
    CapabilitySection.ScheduledTasks -> R.string.cap_tasks to Icons.Rounded.Schedule
    CapabilitySection.Browser -> R.string.cap_browser to Icons.Rounded.Language
}
