package com.github.macintacos.goto.util

/**
 * Validates input characters for the Go to Line popup.
 */
object InputValidator {

    /**
     * Determines if a character should be allowed at the given caret position.
     *
     * @param currentText The current text in the input field
     * @param caretPos The current caret position
     * @param char The character being typed
     * @return true if the character should be allowed, false otherwise
     */
    fun isInputAllowed(
        currentText: String,
        caretPos: Int,
        char: Char,
    ): Boolean {
        val hasDirectionKey = currentText.contains('j') || currentText.contains('k')
        val hasG = currentText.contains('g')

        // Find where the suffix starts (g, j, or k)
        val suffixStart = currentText.indexOfFirst { it == 'g' || it == 'j' || it == 'k' }

        return when {
            char == '\b' -> true
            // If direction key exists, only allow digits before the suffix
            hasDirectionKey && char.isDigit() -> caretPos <= suffixStart
            // Allow 'g' to be inserted directly before 'j' or 'k' (to convert "10j" to "10gj")
            hasDirectionKey && char == 'g' && !hasG ->
                caretPos == suffixStart && suffixStart > 0 && currentText[suffixStart - 1].isDigit()
            // If 'g' exists but no direction key yet, allow digits before 'g' or j/k at end
            hasG && char.isDigit() -> caretPos <= suffixStart
            hasG && (char == 'j' || char == 'k') ->
                caretPos == currentText.length && currentText.last() == 'g'
            // Block any other input if direction key exists
            hasDirectionKey -> false
            // 'j' or 'k' can be typed at end after a number or after 'g'
            char == 'j' || char == 'k' -> currentText.isNotEmpty() &&
                caretPos == currentText.length &&
                (currentText.last().isDigit() || currentText.last() == 'g')
            // 'g' can only be typed at end after a number
            char == 'g' -> currentText.isNotEmpty() &&
                caretPos == currentText.length &&
                currentText.last().isDigit() &&
                !hasG
            char.isDigit() -> !hasG
            char == ':' -> !hasG && !hasDirectionKey
            else -> false
        }
    }
}
