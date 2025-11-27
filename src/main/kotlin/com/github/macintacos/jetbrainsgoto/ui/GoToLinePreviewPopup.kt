package com.github.macintacos.jetbrainsgoto.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

class GoToLinePreviewPopup(
    private val project: Project,
    private val editor: Editor,
) {
    private val document = editor.document
    private val lineCount = document.lineCount

    private lateinit var popup: JBPopup
    private lateinit var previewEditor: EditorEx
    private var isEditorReleased = false
    private val lineInput = JBTextField(10)
    private val statusLabel = JBLabel("")
    private val defaultStatusColor = statusLabel.foreground

    // Store the selection anchor if there was a selection when the popup was opened
    private val selectionAnchor: Int? = if (editor.selectionModel.hasSelection()) {
        editor.selectionModel.selectionStart
    } else {
        null
    }

    // Store the current line number (1-indexed) for relative navigation
    private val currentLine = document.getLineNumber(editor.caretModel.offset) + 1

    fun show() {
        previewEditor = createPreviewEditor()

        // Set initial position before showing (will scroll properly after popup is shown)
        val currentOffset = editor.caretModel.offset
        previewEditor.caretModel.moveToOffset(currentOffset)

        val panel = createPanel()
        setupListeners()

        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, lineInput)
            .setFocusable(true)
            .setRequestFocus(true)
            .setMovable(true)
            .setResizable(true)
            .setCancelCallback {
                if (!isEditorReleased) {
                    isEditorReleased = true
                    EditorFactory.getInstance().releaseEditor(previewEditor)
                }
                true
            }
            .createPopup()

        popup.showCenteredInCurrentWindow(project)

        // Scroll to current position after popup is shown and sized
        javax.swing.SwingUtilities.invokeLater {
            previewEditor.scrollingModel.disableAnimation()
            previewEditor.scrollingModel.scrollToCaret(ScrollType.CENTER)
            previewEditor.scrollingModel.enableAnimation()
        }
    }

    private fun createPreviewEditor(): EditorEx {
        val editorFactory = EditorFactory.getInstance()
        val previewEditor = editorFactory.createViewer(document, project) as EditorEx

        // Apply syntax highlighting from the original file
        val virtualFile = FileDocumentManager.getInstance().getFile(document)
        if (virtualFile != null) {
            val highlighter = EditorHighlighterFactory.getInstance()
                .createEditorHighlighter(project, virtualFile)
            previewEditor.highlighter = highlighter
        }

        // Configure the preview editor appearance
        previewEditor.settings.apply {
            isLineNumbersShown = true
            isFoldingOutlineShown = false
            isLineMarkerAreaShown = false
            isIndentGuidesShown = true
            additionalLinesCount = 0
            additionalColumnsCount = 0
            isCaretRowShown = true
        }

        // Match the color scheme of the main editor
        previewEditor.colorsScheme = editor.colorsScheme

        return previewEditor
    }

    private fun createPanel(): JPanel {
        val settingsButton = createSettingsButton()

        val titleLabel = JBLabel("Go to Line").apply {
            font = font.deriveFont(Font.BOLD, font.size2D + 4f)
            border = JBUI.Borders.emptyLeft(2)
        }

        val headerPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyBottom(8)
            add(titleLabel, BorderLayout.WEST)
            add(settingsButton, BorderLayout.EAST)
        }

        val helperText = createHelperText()

        val inputPanel = JPanel(BorderLayout(0, 3)).apply {
            border = JBUI.Borders.emptyBottom(5)
            add(lineInput, BorderLayout.NORTH)
            add(helperText, BorderLayout.SOUTH)
        }

        val topPanel = JPanel(BorderLayout()).apply {
            add(headerPanel, BorderLayout.NORTH)
            add(inputPanel, BorderLayout.SOUTH)
        }

        val editorComponent = previewEditor.component.apply {
            preferredSize = Dimension(700, 300)
        }

        val dismissHint = createDismissHint()

        val bottomPanel = JPanel(BorderLayout()).apply {
            add(statusLabel, BorderLayout.WEST)
            add(dismissHint, BorderLayout.EAST)
        }

        return JPanel(BorderLayout(5, 5)).apply {
            border = JBUI.Borders.empty(8)
            add(topPanel, BorderLayout.NORTH)
            add(editorComponent, BorderLayout.CENTER)
            add(bottomPanel, BorderLayout.SOUTH)
        }
    }

    private fun createSettingsButton(): JPanel {
        var isHovered = false
        var isMenuOpen = false

        val bg = javax.swing.UIManager.getColor("Panel.background")
            ?: JBColor.GRAY
        val isDarkTheme = (bg.red + bg.green + bg.blue) / 3 < 128
        val hoverShift = if (isDarkTheme) 30 else -30
        val hoverBg = java.awt.Color(
            (bg.red + hoverShift).coerceIn(0, 255),
            (bg.green + hoverShift).coerceIn(0, 255),
            (bg.blue + hoverShift).coerceIn(0, 255),
        )

        val button = object : JPanel(BorderLayout()) {
            init {
                isOpaque = false
                border = JBUI.Borders.empty(4, 6)
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                toolTipText = "Settings"
                add(JBLabel(AllIcons.Actions.More), BorderLayout.CENTER)
            }

            override fun paintComponent(g: Graphics) {
                if (isHovered || isMenuOpen) {
                    val g2 = g.create() as Graphics2D
                    g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON,
                    )
                    g2.color = hoverBg
                    g2.fillRoundRect(0, 0, width, height, 6, 6)
                    g2.dispose()
                }
                super.paintComponent(g)
            }
        }

        button.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                isHovered = true
                button.repaint()
            }

            override fun mouseExited(e: MouseEvent) {
                isHovered = false
                button.repaint()
            }

            override fun mouseClicked(e: MouseEvent) {
                isMenuOpen = true
                button.repaint()
                showSettingsMenu(e, button) {
                    isMenuOpen = false
                    button.repaint()
                }
            }
        })

        return button
    }

    private fun showSettingsMenu(e: MouseEvent, component: JPanel, onClose: () -> Unit) {
        val actionGroup = DefaultActionGroup().apply {
            add(object : AnAction(
                "Customize Shortcut...",
                "Configure keyboard shortcut",
                AllIcons.General.Settings,
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    openShortcutDialog()
                }

                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
            })
        }

        val popupMenu = JBPopupFactory.getInstance()
            .createActionGroupPopup(
                null,
                actionGroup,
                com.intellij.openapi.actionSystem.impl.SimpleDataContext.getProjectContext(project),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false,
            )

        popupMenu.addListener(object : com.intellij.openapi.ui.popup.JBPopupListener {
            override fun onClosed(event: com.intellij.openapi.ui.popup.LightweightWindowEvent) {
                onClose()
            }
        })

        popupMenu.show(RelativePoint(component, Point(0, component.height)))
    }

    private fun openShortcutDialog() {
        popup.cancel()
        ShortcutCaptureDialog(project, "GoToLinePreview").show()
    }

    private fun setupListeners() {
        // Filter to only allow numbers and colon
        lineInput.addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                val currentText = lineInput.text
                val isAllowed = when {
                    e.keyChar == KeyEvent.VK_BACK_SPACE.toChar() -> true
                    // If 'j' is already in the input, only allow backspace
                    currentText.contains('j') -> false
                    // 'j' can be typed after a number or after 'g' (for "Ngj" visual line syntax)
                    e.keyChar == 'j' -> currentText.isNotEmpty() &&
                        (currentText.last().isDigit() || currentText.last() == 'g')
                    // 'g' can only be typed after a number (for "Ngj" syntax)
                    e.keyChar == 'g' -> currentText.isNotEmpty() &&
                        currentText.last().isDigit() &&
                        !currentText.contains('g')
                    e.keyChar.isDigit() -> !currentText.contains('g')
                    e.keyChar == ':' -> !currentText.contains('g')
                    else -> false
                }
                if (!isAllowed) {
                    e.consume()
                }
            }

            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_ENTER -> {
                        if (tryNavigateToLine()) {
                            popup.cancel()
                        }
                    }

                    KeyEvent.VK_ESCAPE -> popup.cancel()
                }
            }
        })

        // Live preview update
        lineInput.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                updatePreview()
            }
        })
    }

    private sealed class NavigationResult {
        data class Absolute(val line: Int, val column: Int) : NavigationResult()
        data class RelativeLogical(val count: Int, val targetLine: Int) : NavigationResult()
        data class RelativeVisual(val count: Int) : NavigationResult()
    }

    private fun parseInput(): NavigationResult? {
        val text = lineInput.text
        if (text.isEmpty()) return null

        // Handle visual line navigation with "gj" suffix (e.g., "10gj" = 10 visual lines down)
        if (text.endsWith("gj")) {
            val count = text.dropLast(2).toIntOrNull() ?: return null
            if (count < 1) return null
            return NavigationResult.RelativeVisual(count)
        }

        // Handle logical line navigation with "j" suffix (e.g., "10j" = 10 lines down)
        if (text.endsWith("j")) {
            val count = text.dropLast(1).toIntOrNull() ?: return null
            val targetLine = currentLine + count
            if (targetLine < 1 || targetLine > lineCount) return null
            return NavigationResult.RelativeLogical(count, targetLine)
        }

        val parts = text.split(":")
        val lineNumber = parts[0].toIntOrNull() ?: return null
        if (lineNumber < 1 || lineNumber > lineCount) return null

        val column = if (parts.size > 1 && parts[1].isNotEmpty()) {
            parts[1].toIntOrNull() ?: return null
        } else {
            1
        }

        return NavigationResult.Absolute(lineNumber, column)
    }

    private fun calculateOffset(result: NavigationResult): Int {
        return when (result) {
            is NavigationResult.Absolute -> {
                val lineStartOffset = document.getLineStartOffset(result.line - 1)
                val lineEndOffset = document.getLineEndOffset(result.line - 1)
                val lineLength = lineEndOffset - lineStartOffset
                val columnOffset = (result.column - 1).coerceIn(0, lineLength)
                lineStartOffset + columnOffset
            }
            is NavigationResult.RelativeLogical -> {
                document.getLineStartOffset(result.targetLine - 1)
            }
            is NavigationResult.RelativeVisual -> {
                // Get current visual position and move down by visual lines
                val currentOffset = editor.caretModel.offset
                val currentVisualPos = editor.offsetToVisualPosition(currentOffset)
                val targetVisualLine = currentVisualPos.line + result.count
                val targetVisualPos = com.intellij.openapi.editor.VisualPosition(targetVisualLine, 0)
                editor.visualPositionToOffset(targetVisualPos)
            }
        }
    }

    private fun updatePreview() {
        statusLabel.foreground = defaultStatusColor
        statusLabel.text = ""

        val text = lineInput.text
        if (text.isEmpty()) {
            return
        }

        val result = parseInput() ?: run {
            showError("Invalid position — file has $lineCount lines")
            return
        }

        // Show navigation hint
        statusLabel.text = when (result) {
            is NavigationResult.Absolute -> {
                val hasColumn = text.contains(":") && text.substringAfter(":").isNotEmpty()
                if (hasColumn) {
                    "Navigate to line ${result.line}, column ${result.column}"
                } else {
                    "Navigate to line ${result.line}"
                }
            }
            is NavigationResult.RelativeLogical -> {
                "Navigate ${result.count} lines down to line ${result.targetLine}"
            }
            is NavigationResult.RelativeVisual -> {
                "Navigate ${result.count} visual lines down"
            }
        }

        val offset = calculateOffset(result)
        previewEditor.caretModel.moveToOffset(offset)
        previewEditor.scrollingModel.scrollToCaret(ScrollType.CENTER)
    }

    private fun tryNavigateToLine(): Boolean {
        val text = lineInput.text
        if (text.isEmpty()) {
            showError("Please enter a line number")
            return false
        }

        val result = parseInput()
        if (result == null) {
            showError("Invalid position — file has $lineCount lines")
            return false
        }

        val offset = calculateOffset(result)

        // If there was a selection when the popup was opened, extend it to the new position
        if (selectionAnchor != null) {
            editor.selectionModel.setSelection(selectionAnchor, offset)
        }
        editor.caretModel.moveToOffset(offset)
        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        return true
    }

    private fun showError(message: String) {
        statusLabel.text = message
        statusLabel.foreground = JBColor.RED
    }

    private fun createHelperText(): JPanel {
        val bg = javax.swing.UIManager.getColor("Panel.background")
            ?: JBColor.GRAY
        val fg = javax.swing.UIManager.getColor("Label.foreground")
            ?: JBColor.GRAY
        val isDarkTheme = (bg.red + bg.green + bg.blue) / 3 < 128

        // Subdued foreground color
        val subduedFg = java.awt.Color(
            (fg.red + bg.red) / 2,
            (fg.green + bg.green) / 2,
            (fg.blue + bg.blue) / 2,
        )

        // Badge background
        val shift = if (isDarkTheme) 25 else -25
        val badgeBg = java.awt.Color(
            (bg.red + shift).coerceIn(0, 255),
            (bg.green + shift).coerceIn(0, 255),
            (bg.blue + shift).coerceIn(0, 255),
        )

        fun createBadge(text: String): JBLabel = object : JBLabel(text) {
            init {
                font = Font(Font.MONOSPACED, Font.PLAIN, font.size - 1)
                foreground = subduedFg
                border = JBUI.Borders.empty(1, 4)
                isOpaque = false
            }

            override fun paintComponent(g: Graphics) {
                val g2 = g.create() as Graphics2D
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON,
                )
                g2.color = badgeBg
                g2.fillRoundRect(0, 0, width, height, 6, 6)
                g2.dispose()
                super.paintComponent(g)
            }
        }

        fun createText(text: String): JBLabel = JBLabel(text).apply {
            foreground = subduedFg
            font = font.deriveFont(font.size2D - 1f)
        }

        return JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            isOpaque = false
            add(createText("Format: "))
            add(createBadge("line[:column]"))
            add(createText(", "))
            add(createBadge("#[j,k]"))
            add(createText(" (lines) — e.g. "))
            add(createBadge("42"))
            add(createText(", "))
            add(createBadge("42:10"))
            add(createText(", or "))
            add(createBadge("10j"))
        }
    }

    private fun createDismissHint(): JPanel {
        val bg = javax.swing.UIManager.getColor("Panel.background")
            ?: javax.swing.UIManager.getColor("control")
        val fg = javax.swing.UIManager.getColor("Label.foreground")
            ?: JBColor.GRAY
        val isDarkTheme = (bg.red + bg.green + bg.blue) / 3 < 128

        // Subdued foreground color (50% opacity effect)
        val subduedFg = java.awt.Color(
            (fg.red + bg.red) / 2,
            (fg.green + bg.green) / 2,
            (fg.blue + bg.blue) / 2,
        )

        // Key badge background - shift from panel background
        val shift = if (isDarkTheme) 25 else -25
        val keyBg = java.awt.Color(
            (bg.red + shift).coerceIn(0, 255),
            (bg.green + shift).coerceIn(0, 255),
            (bg.blue + shift).coerceIn(0, 255),
        )

        // Custom key badge with rounded corners
        val keyBadge = object : JBLabel("Esc") {
            init {
                font = Font(Font.MONOSPACED, Font.PLAIN, font.size - 1)
                foreground = subduedFg
                border = JBUI.Borders.empty(2, 6)
                isOpaque = false
            }

            override fun paintComponent(g: Graphics) {
                val g2 = g.create() as Graphics2D
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON,
                )
                g2.color = keyBg
                g2.fillRoundRect(0, 0, width, height, 6, 6)
                g2.dispose()
                super.paintComponent(g)
            }
        }

        val toLabel = JBLabel(" to dismiss").apply {
            foreground = subduedFg
            font = font.deriveFont(font.size2D - 1f)
        }

        return JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0)).apply {
            isOpaque = false
            add(keyBadge)
            add(toLabel)
        }
    }
}
