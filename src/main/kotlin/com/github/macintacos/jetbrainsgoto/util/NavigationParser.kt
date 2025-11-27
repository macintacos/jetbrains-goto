package com.github.macintacos.jetbrainsgoto.util

/**
 * Parses navigation input text into navigation results.
 */
object NavigationParser {

    sealed class NavigationResult {
        data class Absolute(val line: Int, val column: Int) : NavigationResult()
        data class RelativeLogical(val count: Int, val targetLine: Int) : NavigationResult()
        data class RelativeVisual(val count: Int) : NavigationResult()
    }

    /**
     * Parses input text into a navigation result.
     *
     * @param text The input text to parse
     * @param currentLine The current logical line number (1-indexed)
     * @param lineCount The total number of lines in the document
     * @param currentVisualLine The current visual line number (0-indexed)
     * @param maxVisualLine The maximum visual line number (0-indexed)
     * @return A NavigationResult if the input is valid, null otherwise
     */
    fun parseInput(
        text: String,
        currentLine: Int,
        lineCount: Int,
        currentVisualLine: Int,
        maxVisualLine: Int,
    ): NavigationResult? {
        if (text.isEmpty()) return null

        // Handle visual line navigation with "gj" suffix (e.g., "10gj" = 10 visual lines down)
        if (text.endsWith("gj")) {
            val count = text.dropLast(2).toIntOrNull() ?: return null
            if (count < 1) return null
            // Check if target visual line would be valid
            if (currentVisualLine + count > maxVisualLine) return null
            return NavigationResult.RelativeVisual(count)
        }

        // Handle visual line navigation with "gk" suffix (e.g., "10gk" = 10 visual lines up)
        if (text.endsWith("gk")) {
            val count = text.dropLast(2).toIntOrNull() ?: return null
            if (count < 1) return null
            // Check if target visual line would be valid
            if (currentVisualLine - count < 0) return null
            return NavigationResult.RelativeVisual(-count)
        }

        // Handle logical line navigation with "j" suffix (e.g., "10j" = 10 lines down)
        if (text.endsWith("j")) {
            val count = text.dropLast(1).toIntOrNull() ?: return null
            val targetLine = currentLine + count
            if (targetLine < 1 || targetLine > lineCount) return null
            return NavigationResult.RelativeLogical(count, targetLine)
        }

        // Handle logical line navigation with "k" suffix (e.g., "10k" = 10 lines up)
        if (text.endsWith("k")) {
            val count = text.dropLast(1).toIntOrNull() ?: return null
            val targetLine = currentLine - count
            if (targetLine < 1 || targetLine > lineCount) return null
            return NavigationResult.RelativeLogical(-count, targetLine)
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
}
