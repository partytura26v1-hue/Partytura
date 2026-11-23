package com.example

import com.example.data.model.ClefType
import com.example.domain.MusicTheoryUtils
import com.example.domain.StaffElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `transpose chord symbols accurately`() {
        assertEquals("Bm", MusicTheoryUtils.transposeChordSymbol("Am", 2))
        assertEquals("G", MusicTheoryUtils.transposeChordSymbol("C", -5))
        assertEquals("F#m7/C#", MusicTheoryUtils.transposeChordSymbol("Em7/B", 2))
    }

    @Test
    fun `transpose lyrics preserves words and transposes bracketed chords`() {
        val input = "[Am]Pierwszy wers [G]drugi wers"
        val result = MusicTheoryUtils.transposeLyricsWithBrackets(input, 2)
        assertEquals("[Bm]Pierwszy wers [A]drugi wers", result)
    }

    @Test
    fun `parse staff elements parses notes, barlines, and transposition`() {
        val elements = MusicTheoryUtils.parseStaffElements("C4 D4 | - E4", semitones = 2)
        assertEquals(5, elements.size)
        val first = elements[0] as StaffElement.Note
        assertEquals("D", first.label)
        assertTrue(elements[2] is StaffElement.BarLine)
        assertTrue(elements[3] is StaffElement.Rest)
    }

    @Test
    fun `bass clef bottom line is G2`() {
        assertEquals(18, MusicTheoryUtils.bottomLineDiatonicStep(ClefType.BASS))
        assertEquals(30, MusicTheoryUtils.bottomLineDiatonicStep(ClefType.TREBLE))
    }
}
