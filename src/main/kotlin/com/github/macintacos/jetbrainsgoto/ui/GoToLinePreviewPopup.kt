package com.github.macintacos.jetbrainsgoto.ui

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
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
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
            .setTitle("Go to Line")
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
        val helperText = JBLabel("Format: line[:column] — e.g. \"42\" or \"42:10\"").apply {
            foreground = foreground.let { java.awt.Color(it.red, it.green, it.blue, 150) }
            font = font.deriveFont(font.size2D - 1f)
        }

        val inputPanel = JPanel(BorderLayout(0, 3)).apply {
            border = EmptyBorder(0, 0, 5, 0)
            add(lineInput, BorderLayout.NORTH)
            add(helperText, BorderLayout.SOUTH)
        }

        val editorComponent = previewEditor.component.apply {
            preferredSize = Dimension(700, 300)
        }

        return JPanel(BorderLayout(5, 5)).apply {
            border = EmptyBorder(5, 5, 5, 5)
            add(inputPanel, BorderLayout.NORTH)
            add(editorComponent, BorderLayout.CENTER)
            add(statusLabel, BorderLayout.SOUTH)
        }
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
}
