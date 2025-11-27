package com.github.macintacos.jetbrainsgoto.ui

import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.RenderingHints
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.border.EmptyBorder

class ShortcutCaptureDialog(
    private val project: Project,
    private val actionId: String,
) : DialogWrapper(project) {

    private var capturedKeyStroke: KeyStroke? = null
    private val shortcutLabel = JBLabel("Waiting for input...").apply {
        font = font.deriveFont(Font.BOLD, font.size2D + 2f)
        horizontalAlignment = JBLabel.CENTER
    }
    private val hintLabel = JBLabel("Assign a keyboard shortcut for the 'Go to Line (Preview)' command").apply {
        horizontalAlignment = JBLabel.CENTER
    }
    private val conflictLabel = JBLabel().apply {
        horizontalAlignment = JBLabel.CENTER
        foreground = java.awt.Color(200, 150, 50)
        font = font.deriveFont(font.size2D - 1f)
        isVisible = false
    }

    private lateinit var shortcutPanel: JPanel
    private lateinit var contentPanel: JPanel

    init {
        title = "Set Shortcut"
        setOKButtonText("Assign")
        isOKActionEnabled = false
        init()
    }

    override fun createCenterPanel(): JComponent {
        val bg = javax.swing.UIManager.getColor("Panel.background")
            ?: java.awt.Color.GRAY
        val darkerBg = java.awt.Color(
            (bg.red - 10).coerceIn(0, 255),
            (bg.green - 10).coerceIn(0, 255),
            (bg.blue - 10).coerceIn(0, 255),
        )

        // Shortcut display with rounded background
        shortcutPanel = object : JPanel(BorderLayout()) {
            init {
                isOpaque = false
                border = EmptyBorder(20, 30, 20, 30)
                add(shortcutLabel, BorderLayout.CENTER)
            }

            override fun paintComponent(g: Graphics) {
                val g2 = g.create() as Graphics2D
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON,
                )
                g2.color = darkerBg
                g2.fillRoundRect(0, 0, width, height, 12, 12)
                g2.dispose()
                super.paintComponent(g)
            }
        }

        // Content area with GridBagLayout to handle dynamic sizing
        contentPanel = JPanel(GridBagLayout()).apply {
            val gbc = GridBagConstraints().apply {
                gridx = 0
                fill = GridBagConstraints.BOTH
                weightx = 1.0
            }

            // Shortcut panel - expands to fill available space
            gbc.gridy = 0
            gbc.weighty = 1.0
            add(shortcutPanel, gbc)

            // Conflict label - fixed size, hidden when empty
            gbc.gridy = 1
            gbc.weighty = 0.0
            gbc.insets = java.awt.Insets(8, 0, 0, 0)
            add(conflictLabel, gbc)
        }

        val panel = JPanel(BorderLayout(0, 12)).apply {
            border = EmptyBorder(20, 30, 20, 30)
            preferredSize = Dimension(380, 160)

            add(hintLabel, BorderLayout.NORTH)
            add(contentPanel, BorderLayout.CENTER)
        }

        // Capture key events on the panel
        panel.isFocusable = true
        panel.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (isModifierOnly(e)) {
                    return
                }

                capturedKeyStroke = KeyStroke.getKeyStrokeForEvent(e)
                shortcutLabel.text = getKeyStrokeText(capturedKeyStroke!!)
                updateConflictWarning(capturedKeyStroke!!)
                isOKActionEnabled = true
                e.consume()
            }
        })

        // Request focus after dialog is shown
        panel.addHierarchyListener {
            if (panel.isShowing) {
                panel.requestFocusInWindow()
            }
        }

        return panel
    }

    private fun updateConflictWarning(keyStroke: KeyStroke) {
        val keymap = KeymapManager.getInstance().activeKeymap
        val conflictingActions = keymap.getActionIds(keyStroke)
            .filter { it != actionId }

        if (conflictingActions.isEmpty()) {
            conflictLabel.isVisible = false
            conflictLabel.text = ""
        } else {
            conflictLabel.isVisible = true
            conflictLabel.text = when (conflictingActions.size) {
                1 -> "1 other command uses this shortcut"
                else -> "${conflictingActions.size} other commands use this shortcut"
            }
        }
        contentPanel.revalidate()
    }

    override fun doOKAction() {
        capturedKeyStroke?.let { keyStroke ->
            assignShortcut(keyStroke)
        }
        super.doOKAction()
    }

    private fun isModifierOnly(e: KeyEvent): Boolean {
        return e.keyCode == KeyEvent.VK_SHIFT ||
            e.keyCode == KeyEvent.VK_CONTROL ||
            e.keyCode == KeyEvent.VK_ALT ||
            e.keyCode == KeyEvent.VK_META
    }

    private fun getKeyStrokeText(keyStroke: KeyStroke): String {
        val parts = mutableListOf<String>()

        if (keyStroke.modifiers and KeyEvent.CTRL_DOWN_MASK != 0) {
            parts.add("Ctrl")
        }
        if (keyStroke.modifiers and KeyEvent.ALT_DOWN_MASK != 0) {
            parts.add("Alt")
        }
        if (keyStroke.modifiers and KeyEvent.SHIFT_DOWN_MASK != 0) {
            parts.add("Shift")
        }
        if (keyStroke.modifiers and KeyEvent.META_DOWN_MASK != 0) {
            parts.add("Cmd")
        }

        parts.add(KeyEvent.getKeyText(keyStroke.keyCode))

        return parts.joinToString(" + ")
    }

    private fun assignShortcut(keyStroke: KeyStroke) {
        val keymapManager = KeymapManager.getInstance()
        val keymap = keymapManager.activeKeymap

        // Remove existing shortcuts for this action
        keymap.getShortcuts(actionId).forEach { shortcut ->
            keymap.removeShortcut(actionId, shortcut)
        }

        // Add the new shortcut
        val shortcut = com.intellij.openapi.actionSystem.KeyboardShortcut(keyStroke, null)
        keymap.addShortcut(actionId, shortcut)
    }
}
