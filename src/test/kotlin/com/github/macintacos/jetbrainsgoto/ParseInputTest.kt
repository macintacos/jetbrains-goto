package com.github.macintacos.jetbrainsgoto

import com.github.macintacos.jetbrainsgoto.util.NavigationParser
import com.github.macintacos.jetbrainsgoto.util.NavigationParser.NavigationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for NavigationParser - the input parsing logic used in the Go to Line popup.
 */
class ParseInputTest {

    private fun parseInput(
        text: String,
        currentLine: Int = 50,
        lineCount: Int = 100,
        currentVisualLine: Int = 50,
        maxVisualLine: Int = 100,
    ): NavigationResult? {
        return NavigationParser.parseInput(
            text = text,
            currentLine = currentLine,
            lineCount = lineCount,
            currentVisualLine = currentVisualLine,
            maxVisualLine = maxVisualLine,
        )
    }

    // Empty input tests
    @Test
    fun `empty input returns null`() {
        assertNull(parseInput(""))
    }

    // Absolute line number tests
    @Test
    fun `simple line number parses correctly`() {
        val result = parseInput("42")
        assertEquals(NavigationResult.Absolute(42, 1), result)
    }

    @Test
    fun `line number with column parses correctly`() {
        val result = parseInput("42:10")
        assertEquals(NavigationResult.Absolute(42, 10), result)
    }

    @Test
    fun `line number exceeding file length returns null`() {
        assertNull(parseInput("150", lineCount = 100))
    }

    @Test
    fun `line number zero returns null`() {
        assertNull(parseInput("0"))
    }

    @Test
    fun `negative line number returns null`() {
        assertNull(parseInput("-5"))
    }

    // Relative j (down) tests
    @Test
    fun `10j parses as 10 lines down`() {
        val result = parseInput("10j", currentLine = 50)
        assertEquals(NavigationResult.RelativeLogical(10, 60), result)
    }

    @Test
    fun `1j parses as 1 line down`() {
        val result = parseInput("1j", currentLine = 50)
        assertEquals(NavigationResult.RelativeLogical(1, 51), result)
    }

    @Test
    fun `j that would exceed file length returns null`() {
        assertNull(parseInput("60j", currentLine = 50, lineCount = 100))
    }

    @Test
    fun `0j parses as 0 lines down (stays on same line)`() {
        // 0j is technically valid - it moves 0 lines down
        val result = parseInput("0j", currentLine = 50)
        assertEquals(NavigationResult.RelativeLogical(0, 50), result)
    }

    // Relative k (up) tests
    @Test
    fun `10k parses as 10 lines up`() {
        val result = parseInput("10k", currentLine = 50)
        assertEquals(NavigationResult.RelativeLogical(-10, 40), result)
    }

    @Test
    fun `1k parses as 1 line up`() {
        val result = parseInput("1k", currentLine = 50)
        assertEquals(NavigationResult.RelativeLogical(-1, 49), result)
    }

    @Test
    fun `k that would go before line 1 returns null`() {
        assertNull(parseInput("60k", currentLine = 50))
    }

    // Visual line gj (down) tests
    @Test
    fun `10gj parses as 10 visual lines down`() {
        val result = parseInput("10gj", currentVisualLine = 50)
        assertEquals(NavigationResult.RelativeVisual(10), result)
    }

    @Test
    fun `gj that would exceed max visual line returns null`() {
        assertNull(parseInput("60gj", currentVisualLine = 50, maxVisualLine = 100))
    }

    // Visual line gk (up) tests
    @Test
    fun `10gk parses as 10 visual lines up`() {
        val result = parseInput("10gk", currentVisualLine = 50)
        assertEquals(NavigationResult.RelativeVisual(-10), result)
    }

    @Test
    fun `gk that would go negative returns null`() {
        assertNull(parseInput("60gk", currentVisualLine = 50))
    }

    // Invalid input tests
    @Test
    fun `non-numeric input returns null`() {
        assertNull(parseInput("abc"))
    }

    @Test
    fun `j without number returns null`() {
        assertNull(parseInput("j"))
    }

    @Test
    fun `gj without number returns null`() {
        assertNull(parseInput("gj"))
    }

    @Test
    fun `0gj returns null`() {
        assertNull(parseInput("0gj"))
    }

    // Edge cases
    @Test
    fun `large number parses correctly`() {
        val result = parseInput("99999", lineCount = 100000)
        assertEquals(NavigationResult.Absolute(99999, 1), result)
    }

    @Test
    fun `line 1 is valid`() {
        val result = parseInput("1")
        assertEquals(NavigationResult.Absolute(1, 1), result)
    }

    @Test
    fun `column 1 is default when colon present but no column`() {
        val result = parseInput("42:")
        assertEquals(NavigationResult.Absolute(42, 1), result)
    }

    @Test
    fun `last line is valid`() {
        val result = parseInput("100", lineCount = 100)
        assertEquals(NavigationResult.Absolute(100, 1), result)
    }

    @Test
    fun `j to last line is valid`() {
        val result = parseInput("50j", currentLine = 50, lineCount = 100)
        assertEquals(NavigationResult.RelativeLogical(50, 100), result)
    }

    @Test
    fun `k to first line is valid`() {
        val result = parseInput("49k", currentLine = 50)
        assertEquals(NavigationResult.RelativeLogical(-49, 1), result)
    }
}
