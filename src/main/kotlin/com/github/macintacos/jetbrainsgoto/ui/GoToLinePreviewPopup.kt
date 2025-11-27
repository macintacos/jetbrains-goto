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
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
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
import javax.swing.border.EmptyBorder
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
    private val statusLabel = JBLabel("Enter line[:column] (1-$lineCount)")
    private val defaultStatusColor = statusLabel.foreground

    fun show() {
        previewEditor = createPreviewEditor()
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
            border = EmptyBorder(0, 2, 0, 0)
        }

        val headerPanel = JPanel(BorderLayout()).apply {
            border = EmptyBorder(0, 0, 8, 0)
            add(titleLabel, BorderLayout.WEST)
            add(settingsButton, BorderLayout.EAST)
        }

        val helperText = JBLabel("Format: line[:column] — e.g. \"42\" or \"42:10\"").apply {
            foreground = foreground.let { java.awt.Color(it.red, it.green, it.blue, 150) }
            font = font.deriveFont(font.size2D - 1f)
        }

        val inputPanel = JPanel(BorderLayout(0, 3)).apply {
            border = EmptyBorder(0, 0, 5, 0)
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
            border = EmptyBorder(8, 8, 8, 8)
            add(topPanel, BorderLayout.NORTH)
            add(editorComponent, BorderLayout.CENTER)
            add(bottomPanel, BorderLayout.SOUTH)
        }
    }

    private fun createSettingsButton(): JPanel {
        var isHovered = false
        var isMenuOpen = false

        val bg = javax.swing.UIManager.getColor("Panel.background")
            ?: java.awt.Color.GRAY
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
                border = EmptyBorder(4, 6, 4, 6)
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
            add(object : AnAction("Customize Shortcut...", "Configure keyboard shortcut", AllIcons.General.Settings) {
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
                val isAllowed = e.keyChar.isDigit() ||
                    e.keyChar == ':' ||
                    e.keyChar == KeyEvent.VK_BACK_SPACE.toChar()
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

    private fun parseInput(): Pair<Int, Int>? {
        val text = lineInput.text
        if (text.isEmpty()) return null

        val parts = text.split(":")
        val lineNumber = parts[0].toIntOrNull() ?: return null
        if (lineNumber < 1 || lineNumber > lineCount) return null

        val column = if (parts.size > 1 && parts[1].isNotEmpty()) {
            parts[1].toIntOrNull() ?: return null
        } else {
            1
        }

        return Pair(lineNumber, column)
    }

    private fun updatePreview() {
        statusLabel.foreground = defaultStatusColor

        val text = lineInput.text
        if (text.isEmpty()) {
            statusLabel.text = "Enter line[:column] (1-$lineCount)"
            return
        }

        val (lineNumber, column) = parseInput() ?: run {
            statusLabel.text = "Invalid position — file has $lineCount lines"
            return
        }

        val lineStartOffset = document.getLineStartOffset(lineNumber - 1)
        val lineEndOffset = document.getLineEndOffset(lineNumber - 1)
        val lineLength = lineEndOffset - lineStartOffset

        val statusText = if (column > 1) {
            "Line $lineNumber:$column of $lineCount (line length: $lineLength)"
        } else {
            "Line $lineNumber of $lineCount"
        }
        statusLabel.text = statusText

        // Calculate offset with column
        val columnOffset = (column - 1).coerceIn(0, lineLength)
        val offset = lineStartOffset + columnOffset

        previewEditor.caretModel.moveToOffset(offset)
        previewEditor.scrollingModel.scrollToCaret(ScrollType.CENTER)
    }

    private fun tryNavigateToLine(): Boolean {
        val text = lineInput.text
        if (text.isEmpty()) {
            showError("Please enter a line number")
            return false
        }

        val parsed = parseInput()
        if (parsed == null) {
            showError("Invalid position — file has $lineCount lines")
            return false
        }

        val (lineNumber, column) = parsed
        val lineStartOffset = document.getLineStartOffset(lineNumber - 1)
        val lineEndOffset = document.getLineEndOffset(lineNumber - 1)
        val lineLength = lineEndOffset - lineStartOffset

        val columnOffset = (column - 1).coerceIn(0, lineLength)
        val offset = lineStartOffset + columnOffset

        editor.caretModel.moveToOffset(offset)
        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        return true
    }

    private fun showError(message: String) {
        statusLabel.text = message
        statusLabel.foreground = java.awt.Color.RED
    }

    private fun createDismissHint(): JPanel {
        val bg = javax.swing.UIManager.getColor("Panel.background")
            ?: javax.swing.UIManager.getColor("control")
        val fg = javax.swing.UIManager.getColor("Label.foreground")
            ?: java.awt.Color.GRAY
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
                border = EmptyBorder(2, 6, 2, 6)
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
