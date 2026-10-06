package com.bysinbin.posea

import com.bysinbin.posea.ui.drawer.MathEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeatureUnitTest {

    @Test
    fun testMathEvaluator_arithmetic() {
        assertEquals("100", MathEvaluator.evaluate("25 * 4"))
        assertEquals("100", MathEvaluator.evaluate("25 x 4"))
        assertEquals("100", MathEvaluator.evaluate("25 × 4"))
        assertEquals("50", MathEvaluator.evaluate("(120 + 80) / 4"))
        assertEquals("16", MathEvaluator.evaluate("2^4"))
        assertEquals("12", MathEvaluator.evaluate("sqrt(144)"))
        assertEquals("12.5", MathEvaluator.evaluate("25 / 2"))
    }

    @Test
    fun testMathEvaluator_nonMathQueries_returnNull() {
        assertNull(MathEvaluator.evaluate("whatsapp"))
        assertNull(MathEvaluator.evaluate("chrome"))
        assertNull(MathEvaluator.evaluate("yt music"))
        assertNull(MathEvaluator.evaluate("1234"))
        assertNull(MathEvaluator.evaluate(""))
    }
}
