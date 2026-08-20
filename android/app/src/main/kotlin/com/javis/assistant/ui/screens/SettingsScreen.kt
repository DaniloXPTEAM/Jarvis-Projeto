package com.javis.assistant.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.javis.assistant.data.model.AiProvider
import com.javis.assistant.ui.components.JavisBottomBar
import com.javis.assistant.ui.theme.*
import com.javis.assistant.ui.viewmodel.SettingsUiState
import com.javis.assistant.ui.viewmodel.SettingsViewModel
import com.javis.assistant.voice.ElevenLabsTts

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saved by viewModel.savedMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var localState by remember(state) { mutableStateOf(state) }

    Scaffold(
        bottomBar = { JavisBottomBar(navController) },
        containerColor = DarkBg,
        snackbarHost = {
            saved?.let { msg ->
                Snackbar(
                    action = { TextButton(onClick = viewModel::dismissSaved) { Text("OK", color = CyanAccent) } },
                    containerColor = SurfaceMid
                ) { Text(msg, color = TextPrimary) }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Ajustes",
                style = MaterialTheme.typography.headlineMedium,
                color = CyanAccent,
                modifier = Modifier.padding(16.dp)
            )

            // ── IA ────────────────────────────────────────────────────────
            SettingsSection("Provedor de IA") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiProvider.values().forEach { provider ->
                        FilterChip(
                            selected = localState.aiProvider == provider,
                            onClick = { localState = localState.copy(aiProvider = provider) },
                            label = { Text(provider.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent.copy(0.2f),
                                selectedLabelColor = CyanAccent
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                ApiKeyField(
                    label = "Chave da API Groq",
                    value = localState.groqApiKey,
                    hint = "gsk_…  (grátis em console.groq.com)",
                    onChange = { localState = localState.copy(groqApiKey = it) }
                )
                Spacer(Modifier.height(8.dp))
                ApiKeyField(
                    label = "Chave da API DeepSeek",
                    value = localState.deepSeekApiKey,
                    hint = "sk-…",
                    onChange = { localState = localState.copy(deepSeekApiKey = it) }
                )
                Spacer(Modifier.height(8.dp))
                ApiKeyField(
                    label = "Chave da API Anthropic (Claude)",
                    value = localState.anthropicApiKey,
                    hint = "sk-ant-…  (opcional — para 'falar com o Claude')",
                    onChange = { localState = localState.copy(anthropicApiKey = it) }
                )
            }

            // ── Voz ───────────────────────────────────────────────────────
            SettingsSection("Voz") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Usar voz IA da ElevenLabs", Modifier.weight(1f), color = TextPrimary)
                    Switch(
                        checked = localState.useElevenLabs,
                        onCheckedChange = { localState = localState.copy(useElevenLabs = it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = CyanAccent.copy(0.3f))
                    )
                }
                if (localState.useElevenLabs) {
                    Spacer(Modifier.height(8.dp))
                    ApiKeyField(
                        label = "Chave da API ElevenLabs",
                        value = localState.elevenLabsApiKey,
                        hint = "Plano grátis em elevenlabs.io",
                        onChange = { localState = localState.copy(elevenLabsApiKey = it) }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("ID da voz", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text("Padrão: Roberta — voz feminina pt-BR (conversacional)",
                        color = TextSecondary.copy(0.6f), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = localState.elevenLabsVoiceId,
                        onValueChange = { localState = localState.copy(elevenLabsVoiceId = it) },
                        placeholder = { Text(ElevenLabsTts.DEFAULT_VOICE_ID, color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                    Text("Velocidade: ${"%.1f".format(localState.speechRate)}x", color = TextPrimary)
                    Slider(
                        value = localState.speechRate,
                        onValueChange = { localState = localState.copy(speechRate = it) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Tom: ${"%.2f".format(localState.ttsPitch)}", color = TextPrimary)
                    Slider(
                        value = localState.ttsPitch,
                        onValueChange = { localState = localState.copy(ttsPitch = it) },
                        valueRange = 0.5f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
                    )
                }
            }

            // ── Permissões ────────────────────────────────────────────────
            SettingsSection("Permissões do sistema") {
                PermissionButton(
                    label = "Acesso a notificações",
                    description = "Permitir que a Gabi leia notificações de todos os apps",
                    onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                )
                Spacer(Modifier.height(8.dp))
                PermissionButton(
                    label = "Serviço de acessibilidade",
                    description = "Ativar atalho global + automação de apps",
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                )
            }

            // ── Recursos ──────────────────────────────────────────────────
            SettingsSection("Recursos") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Notificações", color = TextPrimary)
                        Text("Salvar notificações na Gabi", color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = localState.notificationsEnabled,
                        onCheckedChange = { localState = localState.copy(notificationsEnabled = it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = CyanAccent.copy(0.3f))
                    )
                }
            }

            // ── Dados ─────────────────────────────────────────────────────
            SettingsSection("Dados") {
                OutlinedButton(
                    onClick = viewModel::clearMemory,
                    border = BorderStroke(1.dp, ErrorRed.copy(0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteSweep, null, tint = ErrorRed)
                    Spacer(Modifier.width(8.dp))
                    Text("Limpar memória", color = ErrorRed)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = viewModel::clearHistory,
                    border = BorderStroke(1.dp, ErrorRed.copy(0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.History, null, tint = ErrorRed)
                    Spacer(Modifier.width(8.dp))
                    Text("Limpar histórico de conversas", color = ErrorRed)
                }
            }

            // ── Salvar ────────────────────────────────────────────────────
            Button(
                onClick = { viewModel.save(localState) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
                Icon(Icons.Default.Save, null, tint = DarkBg)
                Spacer(Modifier.width(8.dp))
                Text("Salvar ajustes", color = DarkBg, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = CyanAccent.copy(0.8f),
            fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        content()
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
fun ApiKeyField(label: String, value: String, hint: String, onChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(hint, color = TextSecondary.copy(0.5f), style = MaterialTheme.typography.bodySmall) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    null, tint = TextSecondary
                )
            }
        },
        colors = fieldColors(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
fun PermissionButton(label: String, description: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceMid)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Default.OpenInNew, null, tint = CyanAccent.copy(0.6f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanAccent.copy(0.5f),
    unfocusedBorderColor = TextSecondary.copy(0.2f),
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = CyanAccent
)
