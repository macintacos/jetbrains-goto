package com.github.macintacos.jetbrainsgoto

import com.github.macintacos.jetbrainsgoto.util.InputValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for InputValidator - the input validation logic used in the Go to Line popup.
 */
class InputValidationTest {

    // Basic digit input tests
    @Test
    fun `digits allowed in empty input`() {
        assertTrue(InputValidator.isInputAllowed("", 0, '1'))
        assertTrue(InputValidator.isInputAllowed("", 0, '9'))
        assertTrue(InputValidator.isInputAllowed("", 0, '0'))
    }

    @Test
    fun `digits allowed after other digits`() {
        assertTrue(InputValidator.isInputAllowed("1", 1, '2'))
        assertTrue(InputValidator.isInputAllowed("12", 2, '3'))
        assertTrue(InputValidator.isInputAllowed("123", 3, '4'))
    }

    // Colon input tests
    @Test
    fun `colon allowed after digits`() {
        assertTrue(InputValidator.isInputAllowed("42", 2, ':'))
    }

    @Test
    fun `colon allowed at start`() {
        // Colon is allowed in empty input (though not useful, it's not blocked)
        assertTrue(InputValidator.isInputAllowed("", 0, ':'))
    }

    @Test
    fun `digits allowed after colon`() {
        assertTrue(InputValidator.isInputAllowed("42:", 3, '1'))
    }

    // j/k direction key tests
    @Test
    fun `j allowed after digit`() {
        assertTrue(InputValidator.isInputAllowed("10", 2, 'j'))
    }

    @Test
    fun `k allowed after digit`() {
        assertTrue(InputValidator.isInputAllowed("10", 2, 'k'))
    }

    @Test
    fun `j not allowed at start`() {
        assertFalse(InputValidator.isInputAllowed("", 0, 'j'))
    }

    @Test
    fun `k not allowed at start`() {
        assertFalse(InputValidator.isInputAllowed("", 0, 'k'))
    }

    @Test
    fun `j not allowed in middle of number`() {
        assertFalse(InputValidator.isInputAllowed("10", 1, 'j'))
    }

    // g prefix tests
    @Test
    fun `g allowed after digit`() {
        assertTrue(InputValidator.isInputAllowed("10", 2, 'g'))
    }

    @Test
    fun `g not allowed at start`() {
        assertFalse(InputValidator.isInputAllowed("", 0, 'g'))
    }

    @Test
    fun `j allowed after g`() {
        assertTrue(InputValidator.isInputAllowed("10g", 3, 'j'))
    }

    @Test
    fun `k allowed after g`() {
        assertTrue(InputValidator.isInputAllowed("10g", 3, 'k'))
    }

    @Test
    fun `second g not allowed`() {
        assertFalse(InputValidator.isInputAllowed("10g", 3, 'g'))
    }

    // Editing after direction key tests
    @Test
    fun `digits allowed before j when editing`() {
        // "10j" with cursor at position 1 (between 1 and 0)
        assertTrue(InputValidator.isInputAllowed("10j", 1, '5'))
        // "10j" with cursor at position 0 (before 1)
        assertTrue(InputValidator.isInputAllowed("10j", 0, '5'))
        // "10j" with cursor at position 2 (between 0 and j)
        assertTrue(InputValidator.isInputAllowed("10j", 2, '5'))
    }

    @Test
    fun `digits not allowed after j`() {
        // "10j" with cursor at position 3 (after j)
        assertFalse(InputValidator.isInputAllowed("10j", 3, '5'))
    }

    @Test
    fun `g can be inserted before j to make gj`() {
        // "10j" with cursor at position 2 (between 0 and j)
        assertTrue(InputValidator.isInputAllowed("10j", 2, 'g'))
    }

    @Test
    fun `g cannot be inserted after j`() {
        assertFalse(InputValidator.isInputAllowed("10j", 3, 'g'))
    }

    @Test
    fun `g cannot be inserted before digits in Nj`() {
        // "10j" with cursor at position 0 or 1
        assertFalse(InputValidator.isInputAllowed("10j", 0, 'g'))
        assertFalse(InputValidator.isInputAllowed("10j", 1, 'g'))
    }

    // Complex editing scenarios
    @Test
    fun `digits allowed before g in Ngj`() {
        assertTrue(InputValidator.isInputAllowed("10gj", 0, '5'))
        assertTrue(InputValidator.isInputAllowed("10gj", 1, '5'))
        assertTrue(InputValidator.isInputAllowed("10gj", 2, '5'))
    }

    @Test
    fun `digits not allowed after g in Ngj`() {
        assertFalse(InputValidator.isInputAllowed("10gj", 3, '5'))
        assertFalse(InputValidator.isInputAllowed("10gj", 4, '5'))
    }

    @Test
    fun `colon not allowed when g present`() {
        assertFalse(InputValidator.isInputAllowed("10g", 2, ':'))
    }

    @Test
    fun `colon not allowed when direction key present`() {
        assertFalse(InputValidator.isInputAllowed("10j", 0, ':'))
    }

    // Invalid character tests
    @Test
    fun `letters other than g j k not allowed`() {
        assertFalse(InputValidator.isInputAllowed("10", 2, 'a'))
        assertFalse(InputValidator.isInputAllowed("10", 2, 'x'))
        assertFalse(InputValidator.isInputAllowed("", 0, 'a'))
    }

    // Backspace tests
    @Test
    fun `backspace always allowed`() {
        assertTrue(InputValidator.isInputAllowed("", 0, '\b'))
        assertTrue(InputValidator.isInputAllowed("10", 2, '\b'))
        assertTrue(InputValidator.isInputAllowed("10j", 3, '\b'))
        assertTrue(InputValidator.isInputAllowed("10gj", 4, '\b'))
    }
}
