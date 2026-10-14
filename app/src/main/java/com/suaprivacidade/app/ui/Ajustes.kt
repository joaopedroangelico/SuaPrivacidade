package com.suaprivacidade.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.suaprivacidade.app.BuildConfig
import com.suaprivacidade.app.R
import com.suaprivacidade.app.data.Apps
import com.suaprivacidade.app.data.OPCOES_DIAS_HISTORICO
import com.suaprivacidade.app.data.Preferencias
import com.suaprivacidade.app.data.Tema
import com.suaprivacidade.app.ui.theme.semente
import com.suaprivacidade.app.ui.theme.sucesso

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FolhaAjustes(
    preferencias: Preferencias,
    onFechar: () -> Unit,
    onTema: (Tema) -> Unit,
    onDiasHistorico: (Int) -> Unit,
    onTirarOculto: (String) -> Unit,
    onEscolherOcultos: () -> Unit,
    onApagarHistorico: () -> Unit,
    /** Espaço ocupado (precisa do ViewModel, por isso vem pronta de fora). */
    secaoArmazenamento: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val cores = MaterialTheme.colorScheme
    val permissoes = lembrarPermissoes()
    var confirmarApagar by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = cores.surfaceContainerLow,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("Ajustes", style = MaterialTheme.typography.headlineSmall)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Tema", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Todos os temas acompanham o modo claro/escuro do celular.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant,
                )
            }
            Tema.entries.chunked(3).forEach { linha ->
                Row(Modifier.fillMaxWidth()) {
                    linha.forEach { tema ->
                        AmostraTema(
                            tema = tema,
                            selecionado = preferencias.tema == tema,
                            onClick = { onTema(tema) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = cores.outlineVariant)
            TituloSecao(Icons.Rounded.History, "Histórico")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Guardar por", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OPCOES_DIAS_HISTORICO.forEach { dias ->
                        FilterChip(
                            selected = preferencias.diasHistorico == dias,
                            onClick = { onDiasHistorico(dias) },
                            label = { Text("$dias dias") },
                            shape = CircleShape,
                        )
                    }
                }
                Text(
                    "Vale para microfone, câmera e notificações. O que for mais antigo é apagado sozinho.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant,
                )
            }
            TextButton(onClick = { confirmarApagar = true }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = cores.error, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Apagar todo o histórico agora", color = cores.error)
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = cores.outlineVariant)
            TituloSecao(Icons.Rounded.VisibilityOff, "Texto oculto nas notificações")
            Text(
                "Destes apps, o histórico guarda só o app e o horário, sem título nem texto. Bom para banco, " +
                    "códigos de verificação e conversas que você prefere não deixar salvas.",
                style = MaterialTheme.typography.bodySmall,
                color = cores.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                preferencias.textoOculto.map { it to Apps.nome(context, it) }.sortedBy { it.second.lowercase() }.forEach { (pacote, nome) ->
                    InputChip(
                        selected = false,
                        onClick = { onTirarOculto(pacote) },
                        label = { Text(nome) },
                        leadingIcon = { IconeApp(pacote, tamanho = 20.dp) },
                        trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = "Tirar $nome", modifier = Modifier.size(16.dp)) },
                        shape = CircleShape,
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = onEscolherOcultos,
                    label = { Text("Escolher apps") },
                    leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = CircleShape,
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = cores.outlineVariant)
            Text("Permissões", style = MaterialTheme.typography.titleMedium)
            ListaPermissoes(permissoes)

            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = cores.outlineVariant)
            secaoArmazenamento()

            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = cores.outlineVariant)
            SecaoComoFunciona()

            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = cores.outlineVariant)
            SecaoPrivacidade()

            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = cores.outlineVariant)
            SecaoSobre()
        }
    }

    if (confirmarApagar) {
        AlertDialog(
            onDismissRequest = { confirmarApagar = false },
            icon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null) },
            title = { Text("Apagar o histórico?") },
            text = {
                Text(
                    "Somem todos os registros de microfone, câmera, notificações e mudanças de permissão. " +
                        "Os apps supervisionados e os ajustes continuam. Não dá para desfazer.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarApagar = false
                    onApagarHistorico()
                }) { Text("Apagar", color = cores.error) }
            },
            dismissButton = { TextButton(onClick = { confirmarApagar = false }) { Text("Cancelar") } },
        )
    }
}

/** O que o app consegue e o que não consegue fazer. */
@Composable
private fun SecaoComoFunciona() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TituloSecao(Icons.Rounded.Info, "Como funciona")
        Text(TEXTO_COMO_FUNCIONA, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

const val TEXTO_COMO_FUNCIONA =
    "O Sua Privacidade vigia e avisa: ele percebe quando o microfone ou a câmera ligam e guarda o histórico. " +
        "Ele não consegue bloquear outros apps, porque o Android não dá esse poder a nenhum app comum. " +
        "Quem bloqueia é você, nas permissões de cada app (o app leva direto até lá). O Android também não " +
        "diz qual app ligou o sensor, então o nome mostrado é um palpite, baseado no app que estava aberto " +
        "ou rodando em segundo plano. Desde o Android 9, apps em segundo plano sem notificação fixa recebem " +
        "silêncio do microfone."

/**
 * Resumo da política de privacidade (texto completo em PRIVACIDADE.md no repositório).
 * Cada frase aqui precisa continuar verdadeira: o app não tem permissão de internet, os dados
 * ficam fora do backup na nuvem (res/xml/regras_*.xml) e não existe exportação de dados.
 */
@Composable
private fun SecaoPrivacidade() {
    val cores = MaterialTheme.colorScheme
    var completa by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TituloSecao(Icons.Rounded.Lock, "Privacidade", cores.sucesso)
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(cores.sucesso.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            ItemPrivacidade("O histórico e as notificações guardadas ficam apenas neste celular.")
            ItemPrivacidade("Nada é enviado ou compartilhado com ninguém, nem com o desenvolvedor.")
            ItemPrivacidade("Os dados não vão para o backup na nuvem.")
            ItemPrivacidade("O app não tem acesso à internet, anúncios ou rastreamento.")
            ItemPrivacidade("O app nunca liga o microfone nem a câmera: só percebe quando outro app liga.")
        }
        TextButton(onClick = { completa = true }, contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text("Ler política de privacidade completa")
        }
    }
    if (completa) {
        AlertDialog(
            onDismissRequest = { completa = false },
            icon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            title = { Text("Política de privacidade") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    TopicoPolitica(
                        "O que fica guardado",
                        "Os momentos em que o microfone ou a câmera foram ligados (com o palpite de qual app foi), " +
                            "as notificações recebidas (app, horário, título e texto, menos dos apps marcados como " +
                            "“texto oculto”), os apps que você supervisiona, as mudanças de permissão deles e suas " +
                            "preferências. Tudo fica armazenado localmente, neste aparelho, pelo prazo escolhido em Ajustes.",
                    )
                    TopicoPolitica(
                        "O que o app lê, sem guardar",
                        "Lista de apps instalados e as permissões de cada um, tempo de uso e internet gasta por app " +
                            "(estatísticas do próprio Android). Isso é lido na hora, para mostrar na tela.",
                    )
                    TopicoPolitica(
                        "Nada sai do celular",
                        "O app não tem permissão de acesso à internet, então o próprio Android impede o envio de " +
                            "qualquer informação. Não há anúncios, estatísticas de uso nem serviços de terceiros. " +
                            "O desenvolvedor não tem acesso aos seus dados.",
                    )
                    TopicoPolitica(
                        "Permissões",
                        "Acesso a notificações, para perceber microfone e câmera e guardar as notificações; acesso " +
                            "ao uso, para saber qual app estava aberto, tempo de uso e internet por app; ver todos os " +
                            "apps instalados; notificações, para os avisos; e o pedido para não sofrer economia de bateria.",
                    )
                    TopicoPolitica(
                        "Apagar seus dados",
                        "Em Ajustes → Histórico dá para apagar tudo de uma vez; ou desinstale/limpe os dados do app nas " +
                            "configurações do Android. Não existe cópia fora do aparelho: dados apagados não podem ser recuperados.",
                    )
                    TopicoPolitica(
                        "LGPD",
                        "Notificações podem conter dados pessoais de terceiros (mensagens, nomes). Como o app não coleta " +
                            "nem transmite nada, não há tratamento de dados pelo desenvolvedor (Lei nº 13.709/2018).",
                    )
                    Text(
                        "Última atualização: 14 de outubro de 2026",
                        style = MaterialTheme.typography.labelSmall,
                        color = cores.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { completa = false }) { Text("Entendi") } },
        )
    }
}

@Composable
private fun ItemPrivacidade(texto: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Rounded.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.sucesso,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp),
        )
        Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun TopicoPolitica(titulo: String, texto: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall)
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Autoria e direitos do app, visíveis para quem instala o APK. */
@Composable
private fun SecaoSobre() {
    val cores = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Sobre", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            // O ícone do launcher é adaptativo (não suportado pelo painterResource): monta fundo + desenho,
            // com o escudo ocupando a mesma proporção do círculo que ocupa no ícone do celular.
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colorResource(R.color.icone_fundo)),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_escudo),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                )
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleSmall)
                Text(
                    "Versão ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant,
                )
            }
        }
        Text(
            "© 2026 João Pedro Angélico. Todos os direitos reservados.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            "Uso pessoal permitido. É proibido copiar, modificar, redistribuir ou vender este aplicativo, " +
                "no todo ou em parte, sem autorização do autor.",
            style = MaterialTheme.typography.bodySmall,
            color = cores.onSurfaceVariant,
        )
    }
}

@Composable
private fun AmostraTema(tema: Tema, selecionado: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cores = MaterialTheme.colorScheme
    val pincel = if (tema == Tema.SISTEMA) {
        // Metade clara, metade escura: indica que segue o modo do celular.
        Brush.linearGradient(
            0f to Color(0xFFF1F1F4),
            0.5f to Color(0xFFF1F1F4),
            0.5f to Color(0xFF2A2C33),
            1f to Color(0xFF2A2C33),
        )
    } else {
        Brush.linearGradient(listOf(lerp(tema.semente, Color.White, 0.45f), tema.semente))
    }
    val borda by animateDpAsState(if (selecionado) 3.dp else 0.dp, label = "borda")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(4.dp)
            .selectable(selected = selecionado, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 8.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(56.dp)
                    .border(borda, cores.onSurface, CircleShape)
                    .padding(if (selecionado) 5.dp else 0.dp)
                    .background(pincel, CircleShape),
            )
            if (selecionado) {
                Box(
                    Modifier
                        .size(22.dp)
                        .background(cores.surface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = cores.onSurface, modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            tema.rotulo,
            style = MaterialTheme.typography.labelLarge,
            color = if (selecionado) cores.onSurface else cores.onSurfaceVariant,
        )
    }
}
