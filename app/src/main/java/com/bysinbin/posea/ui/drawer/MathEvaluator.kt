package com.bysinbin.posea.ui.drawer

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object MathEvaluator {

    private val df = DecimalFormat("#,##0.####", DecimalFormatSymbols(Locale.US)).apply {
        isGroupingUsed = false
    }

    /**
     * Verilen arama sorgusunun bir matematik ifadesi olup olmadığını denetler ve hesaplar.
     * Geçerli bir işlem ise formatlanmış sonucu döner, aksi halde null döner.
     */
    fun evaluate(query: String): String? {
        val trimmed = query.trim()
        if (trimmed.length < 2) return null

        // İçinde en az bir operatör veya matematiksel fonksiyon olmalı
        val hasOperator = trimmed.any { it in "+-*xX×/÷%^" } ||
                trimmed.contains("sqrt", ignoreCase = true) ||
                trimmed.contains("sin", ignoreCase = true) ||
                trimmed.contains("cos", ignoreCase = true) ||
                trimmed.contains("tan", ignoreCase = true)

        if (!hasOperator) return null

        // Sadece izin verilen karakterler: rakamlar, boşluklar, parantezler, noktalar, virgüller, operatörler ve fonksiyon isimleri
        val sanitized = trimmed
            .replace("×", "*")
            .replace("x", "*")
            .replace("X", "*")
            .replace("÷", "/")
            .replace(",", ".")

        // Fonksiyonlar ve izin verilen harfler dışındaki yabancı harfleri kontrol et
        val stripped = sanitized.lowercase()
            .replace("sqrt", "")
            .replace("sin", "")
            .replace("cos", "")
            .replace("tan", "")
            .replace("abs", "")
            .replace("pi", "")

        if (stripped.any { it.isLetter() }) {
            return null
        }

        return try {
            val parser = Parser(sanitized.lowercase())
            val result = parser.parse()
            if (result.isNaN() || result.isInfinite()) {
                null
            } else {
                formatResult(result)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun formatResult(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            df.format(value)
        }
    }

    private class Parser(private val text: String) {
        private var pos = 0

        fun parse(): Double {
            val res = parseExpression()
            skipWhitespace()
            if (pos < text.length) throw IllegalArgumentException("Unexpected char at $pos")
            return res
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                skipWhitespace()
                when {
                    match('+') -> x += parseTerm()
                    match('-') -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                skipWhitespace()
                when {
                    match('*') -> x *= parseFactor()
                    match('/') -> {
                        val d = parseFactor()
                        if (d == 0.0) throw ArithmeticException("Division by zero")
                        x /= d
                    }
                    match('%') -> {
                        val d = parseFactor()
                        x %= d
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            skipWhitespace()
            if (match('+')) return parseFactor()
            if (match('-')) return -parseFactor()

            var x: Double
            skipWhitespace()
            val startPos = pos

            when {
                match('(') -> {
                    x = parseExpression()
                    skipWhitespace()
                    if (!match(')')) throw IllegalArgumentException("Missing ')'")
                }
                matchFunction("sqrt") -> {
                    skipWhitespace()
                    val arg = if (match('(')) {
                        val v = parseExpression()
                        if (!match(')')) throw IllegalArgumentException("Missing ')'")
                        v
                    } else parseFactor()
                    if (arg < 0) throw ArithmeticException("Negative sqrt")
                    x = sqrt(arg)
                }
                matchFunction("sin") -> {
                    val arg = parseFactor()
                    x = sin(Math.toRadians(arg))
                }
                matchFunction("cos") -> {
                    val arg = parseFactor()
                    x = cos(Math.toRadians(arg))
                }
                matchFunction("tan") -> {
                    val arg = parseFactor()
                    x = tan(Math.toRadians(arg))
                }
                matchFunction("abs") -> {
                    val arg = parseFactor()
                    x = abs(arg)
                }
                matchFunction("pi") -> {
                    x = Math.PI
                }
                else -> {
                    while (pos < text.length && (text[pos].isDigit() || text[pos] == '.')) {
                        pos++
                    }
                    if (pos == startPos) throw IllegalArgumentException("Number expected")
                    x = text.substring(startPos, pos).toDouble()
                }
            }

            skipWhitespace()
            if (match('^')) {
                x = x.pow(parseFactor())
            }

            return x
        }

        private fun match(c: Char): Boolean {
            skipWhitespace()
            if (pos < text.length && text[pos] == c) {
                pos++
                return true
            }
            return false
        }

        private fun matchFunction(fn: String): Boolean {
            skipWhitespace()
            if (text.startsWith(fn, pos)) {
                pos += fn.length
                return true
            }
            return false
        }

        private fun skipWhitespace() {
            while (pos < text.length && text[pos].isWhitespace()) {
                pos++
            }
        }
    }
}
