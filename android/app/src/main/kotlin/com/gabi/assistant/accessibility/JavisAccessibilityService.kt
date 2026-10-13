package com.gabi.assistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * JAVIS Accessibility Service
 *
 * Provides the backbone for:
 * - Global shortcut button (Android Accessibility Shortcut / Floating Button)
 * - WhatsApp message typing and sending via UI automation
 * - Reading any app's on-screen text
 * - Detecting the active foreground app
 * - Executing back/home/recents gestures
 */
class JavisAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: JavisAccessibilityService? = null

        fun getInstance(): JavisAccessibilityService? = instance

        private val _isEnabled = MutableStateFlow(false)
        val isEnabled: StateFlow<Boolean> = _isEnabled

        private val _activePackage = MutableStateFlow("")
        val activePackage: StateFlow<String> = _activePackage

        // â”€â”€ WhatsApp helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        const val WA_PACKAGE = "com.whatsapp"
        const val WA_PACKAGE_BUSINESS = "com.whatsapp.w4b"

        fun isWhatsAppActive() = _activePackage.value == WA_PACKAGE || _activePackage.value == WA_PACKAGE_BUSINESS
    }

    /**
     * Views IDs (ex.: "com.whatsapp:id/entry") são sempre prefixados pelo package do
     * app dono da tela — quem usa WhatsApp Business em vez do normal tem essas mesmas
     * views só que sob "com.whatsapp.w4b:id/...", então um WA_PACKAGE fixo nunca
     * encontrava nada pra quem só tem o Business instalado. Resolve pelo app que está
     * de fato em primeiro plano agora; cai pro normal se nenhum dos dois estiver ativo.
     */
    private fun currentWaPackage(): String =
        _activePackage.value.takeIf { it == WA_PACKAGE || it == WA_PACKAGE_BUSINESS } ?: WA_PACKAGE

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isEnabled.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.toString()?.let { pkg ->
                _activePackage.value = pkg
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        _isEnabled.value = false
        super.onDestroy()
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Public API called by the ViewModel / Repository
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Trigger the global back action */
    fun goBack() = performGlobalAction(GLOBAL_ACTION_BACK)

    /** Trigger the home button */
    fun goHome() = performGlobalAction(GLOBAL_ACTION_HOME)

    /** Open recents */
    fun openRecents() = performGlobalAction(GLOBAL_ACTION_RECENTS)

    /**
     * Type text into the currently-focused input field.
     * Works in any app that uses standard Android inputs.
     */
    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = findFocusedEditText(root) ?: findFirstEditText(root) ?: return false
        val args = Bundle().apply {
            putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        focused.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    /** Append text to the current input field (preserves existing content) */
    fun appendText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = findFocusedEditText(root) ?: findFirstEditText(root) ?: return false
        val existing = focused.text?.toString() ?: ""
        val args = Bundle().apply {
            putString(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                if (existing.isEmpty()) text else "$existing $text"
            )
        }
        focused.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    /**
     * Click the first node whose content-description or text matches [label].
     * Used to press WhatsApp's Send button.
     */
    fun clickNodeWithLabel(label: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return findNodeByText(root, label)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            ?: findNodeByContentDesc(root, label)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            ?: false
    }

    /** Read all visible text on screen (flat string) */
    fun readScreenText(): String {
        val root = rootInActiveWindow ?: return ""
        return buildString { collectText(root, this) }.trim()
    }

    /** Read WhatsApp conversation list (recent chats) */
    fun readWhatsAppChats(): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        val result = mutableListOf<String>()
        findAllNodesByViewId(root, "${currentWaPackage()}:id/conversations_row_contact_name")
            .forEach { result.add(it.text?.toString() ?: "") }
        return result.filter { it.isNotBlank() }
    }

    /**
     * Tap a WhatsApp chat row by contact name. If [name] isn't in the
     * recent-chats list currently on screen (the old behavior only ever
     * checked that), falls back to WhatsApp's own search: opens it, types
     * [name], waits for results to filter, and retries the tap.
     */
    fun openWhatsAppChatByName(name: String): Boolean {
        if (tapChatRowByName(name)) return true

        if (!openWhatsAppSearch()) return false
        Thread.sleep(400)
        if (!typeIntoWhatsAppSearch(name)) return false
        Thread.sleep(600) // dá tempo da lista de resultados filtrar

        return tapChatRowByName(name)
    }

    /**
     * CONFIRMADO AO VIVO em 26/08/2026 via `uiautomator dump` real (dispositivo do
     * usuário conectado por ADB) qual era a causa raiz de verdade — o fallback
     * genérico de texto que existia aqui antes (removido nesta correção) buscava
     * "Luciane" na árvore INTEIRA da tela, e batia num node
     * `com.whatsapp:id/msg_from_tv` — o rótulo de "quem mandou a última mensagem"
     * dentro da PRÉVIA de um GRUPO qualquer onde a Luciane comentou por último (ex.:
     * um grupo mostrando "Luciane: oi pessoal" como preview). O código tocava o PAI
     * desse node e abria esse grupo errado — ou, dependendo da lista no momento, uma
     * Atualização de Status dela (que abre o visualizador de foto/perfil, não um
     * chat) — confirmado reproduzindo exatamente esse toque e vendo
     * `com.whatsapp.Conversation` abrir um grupo aleatório ("GGDARK CONTAS"), não a
     * conversa da Luciane. Testado também que a via seguinte (achar via
     * `conversations_row_contact_name`, tanto na lista principal quanto nos
     * resultados de busca) SEMPRE abre a conversa certa. Removido o fallback
     * genérico por completo — arriscado demais (casa com qualquer menção do nome em
     * qualquer prévia de mensagem/grupo). Sem achar por essa view ID específica,
     * devolve false e deixa quem chamou (`openWhatsAppChatByName`) cair pra busca,
     * que já usa a mesma view ID seguro e funciona de verdade.
     */
    private fun tapChatRowByName(name: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val target = normalizeForMatch(name)

        val chatRow = findAllNodesByViewId(root, "${currentWaPackage()}:id/conversations_row_contact_name")
            .firstOrNull { normalizeForMatch(it.text?.toString().orEmpty()).contains(target) }
            ?: return false

        val parent = chatRow.parent ?: chatRow
        if (parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        return chatRow.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /** Só letras/números/espaço, minúsculo — pra comparar "Luciane" com "Luciane ❤️" como iguais. */
    private fun normalizeForMatch(s: String): String =
        s.lowercase().filter { it.isLetterOrDigit() || it.isWhitespace() }.trim()

    /**
     * Tap WhatsApp's own search bar. `menuitem_search` e o contentDesc "Pesquisar"/
     * "Search" eram de uma versão antiga do WhatsApp — confirmado ao vivo em
     * 26/08/2026 (`uiautomator dump` real) que a versão atual nem tem mais um ícone
     * de busca separado: a barra inteira do topo é um único elemento clicável
     * (`search_bar_inner_layout`, dentro de `my_search_bar`) com o texto "Pergunte à
     * Meta AI ou pesquise" (integração com a Meta AI). Sem esses IDs, isso sempre
     * falhava silenciosamente.
     */
    private fun openWhatsAppSearch(): Boolean {
        val root = rootInActiveWindow ?: return false
        val searchIcon = findNodeByViewId(root, "${currentWaPackage()}:id/search_bar_inner_layout")
            ?: findNodeByViewId(root, "${currentWaPackage()}:id/my_search_bar")
            ?: findNodeByViewId(root, "${currentWaPackage()}:id/menuitem_search")
            ?: findNodeByContentDesc(root, "Pesquisar")
            ?: findNodeByContentDesc(root, "Search")
            ?: return false
        return searchIcon.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /**
     * Type into WhatsApp's search field once it's open. Usava só `ACTION_SET_TEXT` sem
     * conferir — o mesmo problema do campo de mensagem (ver `setTextAndVerify`): se o
     * campo de busca também ignorar a ação silenciosamente, a busca nunca roda de
     * verdade, a lista fica vazia, e "não achei o contato" sai mesmo quando o contato
     * existe — foi exatamente o que aconteceu com um contato salvo como "Luciane ❤️"
     * (reportado em 24/08/2026, com print confirmando que o contato existe). Também
     * confirmado ao vivo em 26/08/2026 que a view ID mudou de `search_src_text` pra
     * `search_input` na versão atual do WhatsApp.
     */
    private fun typeIntoWhatsAppSearch(query: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val field = findNodeByViewId(root, "${currentWaPackage()}:id/search_input")
            ?: findNodeByViewId(root, "${currentWaPackage()}:id/search_src_text")
            ?: findFirstEditText(root)
            ?: return false
        field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        return setTextAndVerify(field, query)
    }

    /**
     * Full WhatsApp send flow: focar o campo, escrever, conferir que o texto REALMENTE
     * apareceu, clicar em enviar, e conferir que o campo REALMENTE esvaziou depois.
     *
     * Antes disso, `performAction(ACTION_SET_TEXT)`/`ACTION_CLICK` retornando `true`
     * era tratado como "deu certo" — mas esse retorno só confirma que a AÇÃO foi
     * despachada, não que o composer de texto customizado do WhatsApp de fato aceitou
     * o texto (é um `EditText` customizado, e alguns desses ignoram `ACTION_SET_TEXT`
     * silenciosamente). Resultado: a Gabi dizia "mandei a mensagem" sem ter escrito
     * nada — reportado pelo usuário em 24/08/2026 ("abre o WhatsApp e o contato... mas
     * não chega a enviar"). Agora cada etapa é conferida lendo o estado real da tela
     * de novo antes de seguir, e há um fallback por clipboard/paste pra quando
     * ACTION_SET_TEXT sozinho não pega.
     */
    fun whatsAppSendMessage(message: String): Boolean {
        var root = rootInActiveWindow ?: return false

        var entryBox = findNodeByViewId(root, "${currentWaPackage()}:id/entry")
            ?: findFirstEditText(root)

        // Sem campo de digitar na tela: provavelmente caímos no PERFIL do contato, não
        // no chat (a busca do WhatsApp mostra as duas coisas). O perfil tem um botão
        // "Mensagem"/"Message" que abre o chat de verdade — tenta esse resgate antes
        // de desistir.
        if (entryBox == null) {
            val messageButton = findNodeByContentDesc(root, "Mensagem")
                ?: findNodeByContentDesc(root, "Message")
                ?: return false
            if (!messageButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return false
            Thread.sleep(500)
            root = rootInActiveWindow ?: return false
            entryBox = findNodeByViewId(root, "${currentWaPackage()}:id/entry") ?: findFirstEditText(root) ?: return false
        }

        entryBox.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        entryBox.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

        if (!setTextAndVerify(entryBox, message)) return false

        Thread.sleep(300)

        // Refaz a busca do zero — o node antigo pode estar obsoleto depois da UI
        // reagir ao texto novo, e usar ele "velho" pode devolver ações que reportam
        // sucesso sem afetar a tela atual.
        val postTypeRoot = rootInActiveWindow ?: return false
        val sendButton = findNodeByViewId(postTypeRoot, "${currentWaPackage()}:id/send")
            ?: findNodeByContentDesc(postTypeRoot, "Enviar")
            ?: findNodeByContentDesc(postTypeRoot, "Send")
            ?: findNodeByContentDesc(postTypeRoot, "send")
            ?: return false
        if (!sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return false

        // Depois de um envio de verdade, o WhatsApp sempre limpa o campo de digitar —
        // se o texto ainda está lá, o clique não teve efeito nenhum na tela real.
        //
        // A checagem original comparava com `isNullOrBlank()`, achando que um campo
        // "vazio" devolve texto nulo/em branco — ERRADO, confirmado ao vivo em
        // 26/08/2026 via `uiautomator dump` direto no campo vazio de verdade: o
        // WhatsApp devolve o HINT ("Mensagem") como `text` da árvore de acessibilidade
        // quando o campo está vazio, não null/branco. Isso fazia TODA verificação
        // reportar falha mesmo com a mensagem entregue de verdade (visto na tela real
        // do WhatsApp, com tique de entregue) — a Gabi dizia "não consegui enviar"
        // pra envios que, na real, tinham funcionado. Corrigido: em vez de checar
        // "ficou em branco", checa se o campo NÃO contém mais o texto que a gente
        // mandou (funciona tanto se ele virar branco de verdade quanto se mostrar só
        // o hint) — muito mais confiável que adivinhar qual string de hint usar.
        repeat(5) {
            Thread.sleep(300)
            val postSendRoot = rootInActiveWindow ?: return true
            val postSendEntry = findNodeByViewId(postSendRoot, "${currentWaPackage()}:id/entry")
                ?: findFirstEditText(postSendRoot)
            val stillThere = postSendEntry?.text?.toString()?.contains(message) == true
            if (!stillThere) return true
        }
        return false
    }

    /** Escreve [text] em [node] e confere lendo de volta; cai pra colar via clipboard se precisar. */
    /**
     * `isNotBlank()` tinha o mesmo problema do check de envio (ver `whatsAppSendMessage`):
     * um campo vazio no WhatsApp devolve o HINT ("Mensagem") como `text`, que também
     * não é branco — então esse check sempre dava `true` de cara, MESMO quando
     * `ACTION_SET_TEXT` não tinha escrito nada de verdade (o fallback de clipboard
     * nunca era exercitado). Trocado por conferir se o texto DIGITADO realmente
     * aparece no campo.
     */
    private fun setTextAndVerify(node: AccessibilityNodeInfo, text: String): Boolean {
        val args = Bundle().apply {
            putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        Thread.sleep(150)
        // refresh() é necessário — sem ele, node.text pode devolver o valor "congelado"
        // de quando o node foi consultado, não o estado atual real da tela.
        node.refresh()
        if (node.text?.toString()?.contains(text) == true) return true

        val clipboard = getSystemService(ClipboardManager::class.java) ?: return false
        clipboard.setPrimaryClip(ClipData.newPlainText("gabi_whatsapp", text))
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        Thread.sleep(150)
        node.refresh()
        return node.text?.toString()?.contains(text) == true
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Private helpers
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun findFocusedEditText(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isFocused && root.className?.contains("EditText") == true) return root
        for (i in 0 until root.childCount) {
            findFocusedEditText(root.getChild(i) ?: continue)?.let { return it }
        }
        return null
    }

    private fun findFirstEditText(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.className?.contains("EditText") == true) return root
        for (i in 0 until root.childCount) {
            findFirstEditText(root.getChild(i) ?: continue)?.let { return it }
        }
        return null
    }

    private fun findNodeByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return nodes?.firstOrNull()
    }

    private fun findNodeByContentDesc(root: AccessibilityNodeInfo, desc: String): AccessibilityNodeInfo? {
        if (root.contentDescription?.toString()?.contains(desc, ignoreCase = true) == true) return root
        for (i in 0 until root.childCount) {
            findNodeByContentDesc(root.getChild(i) ?: continue, desc)?.let { return it }
        }
        return null
    }

    private fun findNodeByViewId(root: AccessibilityNodeInfo, viewId: String): AccessibilityNodeInfo? {
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        return nodes?.firstOrNull()
    }

    private fun findAllNodesByViewId(root: AccessibilityNodeInfo, viewId: String): List<AccessibilityNodeInfo> {
        return root.findAccessibilityNodeInfosByViewId(viewId) ?: emptyList()
    }

    private fun collectText(node: AccessibilityNodeInfo, sb: StringBuilder) {
        node.text?.let { sb.append(it).append(" ") }
        for (i in 0 until node.childCount) {
            collectText(node.getChild(i) ?: continue, sb)
        }
    }
}
