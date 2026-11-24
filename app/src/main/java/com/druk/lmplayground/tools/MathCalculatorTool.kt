package com.druk.ai_offline_llm.tools

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat

object MathCalculatorTool {

    private val mc = MathContext(18, RoundingMode.HALF_UP)

    fun evaluateExpression(rawExpression: String): Result<String> {
        val cleaned = normalizeExpression(rawExpression)
        if (cleaned.isBlank()) {
            return Result.failure(IllegalArgumentException("นิพจน์คณิตศาสตร์ว่างเปล่า"))
        }
        return runCatching {
            val tokens = tokenize(cleaned)
            val parser = Parser(tokens)
            val value = parser.parseExpression()
            if (parser.hasRemaining()) {
                throw IllegalArgumentException("รูปแบบนิพจน์ไม่ถูกต้องใกล้ตำแหน่ง '${parser.peek()}'")
            }
            formatNumber(value)
        }
    }

    fun tryAutoCalculateFromPrompt(prompt: String): String? {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return null

        val candidate = extractMathCandidate(trimmed) ?: return null
        val evalResult = evaluateExpression(candidate)
        return evalResult.getOrNull()?.let { resultStr ->
            "[MATH CALCULATOR - EXACT RESULT] นิพจน์: $candidate = $resultStr (ใช้ผลลัพธ์ตัวเลขที่คำนวณได้นี้เสมอ ห้ามคำนวณใหม่เอง)"
        }
    }

    private fun extractMathCandidate(text: String): String? {
        val cleanedText = text
            .replace("เท่ากับเท่าไร", "")
            .replace("เท่าไหร่", "")
            .replace("คือเท่าไร", "")
            .replace("คำนวณ", "")
            .replace("ช่วยคิด", "")
            .replace("?", "")
            .replace("？", "")
            .trim()

        val match = Regex("""[0-9\s\+\-\*\/xX×÷\^\(\)\.,%]+""").find(cleanedText) ?: return null
        val expr = match.value.trim()
        val hasOperator = Regex("""[\+\-\*\/xX×÷\^%]""").containsMatchIn(expr)
        val hasDigit = Regex("""\d""").containsMatchIn(expr)
        if (!hasOperator || !hasDigit) return null
        return expr
    }

    private fun normalizeExpression(input: String): String {
        return input
            .replace(",", "")
            .replace("×", "*")
            .replace("x", "*", ignoreCase = true)
            .replace("÷", "/")
            .replace(Regex("""(\d+(?:\.\d+)?)\s*%""")) { m ->
                "(${m.groupValues[1]} / 100)"
            }
            .trim()
    }

    private fun formatNumber(value: BigDecimal): String {
        val stripped = value.stripTrailingZeros()
        val normalized = if (stripped.scale() < 0) {
            BigDecimal(stripped.toPlainString())
        } else {
            stripped
        }
        val plain = normalized.toPlainString()
        val parts = plain.split(".")
        val intPart = runCatching {
            val bdInt = BigDecimal(parts[0])
            DecimalFormat("#,##0").format(bdInt)
        }.getOrDefault(parts[0])

        return if (parts.size == 2 && parts[1].isNotEmpty()) {
            val fracTrimmed = parts[1].take(10).trimEnd('0')
            if (fracTrimmed.isEmpty()) intPart else "$intPart.$fracTrimmed"
        } else {
            intPart
        }
    }

    private sealed interface Token {
        data class Number(val value: BigDecimal) : Token
        data class Op(val symbol: Char) : Token
        data object LParen : Token
        data object RParen : Token
    }

    private fun tokenize(expr: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val n = expr.length
        while (i < n) {
            val ch = expr[i]
            when {
                ch.isWhitespace() -> i++
                ch.isDigit() || ch == '.' -> {
                    val start = i
                    var dotCount = 0
                    while (i < n && (expr[i].isDigit() || expr[i] == '.')) {
                        if (expr[i] == '.') {
                            dotCount++
                            if (dotCount > 1) {
                                throw IllegalArgumentException("จุดทศนิยมซ้ำในตัวเลข")
                            }
                        }
                        i++
                    }
                    val numStr = expr.substring(start, i)
                    if (numStr == ".") {
                        throw IllegalArgumentException("ตัวเลขไม่สมบูรณ์")
                    }
                    tokens.add(Token.Number(BigDecimal(numStr)))
                }
                ch == '(' -> {
                    if (tokens.isNotEmpty() && (tokens.last() is Token.Number || tokens.last() is Token.RParen)) {
                        tokens.add(Token.Op('*'))
                    }
                    tokens.add(Token.LParen)
                    i++
                }
                ch == ')' -> {
                    tokens.add(Token.RParen)
                    i++
                }
                ch in listOf('+', '-', '*', '/', '^') -> {
                    tokens.add(Token.Op(ch))
                    i++
                }
                else -> throw IllegalArgumentException("ไม่รองรับเครื่องหมาย '$ch'")
            }
        }
        return tokens
    }

    private class Parser(private val tokens: List<Token>) {
        private var pos = 0

        fun hasRemaining(): Boolean = pos < tokens.size
        fun peek(): Token? = tokens.getOrNull(pos)
        private fun consume(): Token = tokens[pos++]

        fun parseExpression(): BigDecimal {
            var left = parseTerm()
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.symbol == '+' || t.symbol == '-')) {
                    consume()
                    val right = parseTerm()
                    left = if (t.symbol == '+') left.add(right, mc) else left.subtract(right, mc)
                } else {
                    break
                }
            }
            return left
        }

        private fun parseTerm(): BigDecimal {
            var left = parsePower()
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.symbol == '*' || t.symbol == '/')) {
                    consume()
                    val right = parsePower()
                    if (t.symbol == '/' && right.compareTo(BigDecimal.ZERO) == 0) {
                        throw ArithmeticException("ไม่สามารถหารด้วยศูนย์ได้")
                    }
                    left = if (t.symbol == '*') left.multiply(right, mc) else left.divide(right, mc)
                } else {
                    break
                }
            }
            return left
        }

        private fun parsePower(): BigDecimal {
            val base = parseUnary()
            val t = peek()
            if (t is Token.Op && t.symbol == '^') {
                consume()
                val exp = parsePower()
                val expInt = runCatching { exp.intValueExact() }.getOrNull()
                return if (expInt != null) {
                    base.pow(expInt, mc)
                } else {
                    val resDouble = Math.pow(base.toDouble(), exp.toDouble())
                    if (resDouble.isNaN() || resDouble.isInfinite()) {
                        throw ArithmeticException("ผลลัพธ์ยกกำลังอยู่นอกช่วงที่คำนวณได้")
                    }
                    BigDecimal.valueOf(resDouble)
                }
            }
            return base
        }

        private fun parseUnary(): BigDecimal {
            val t = peek()
            if (t is Token.Op && (t.symbol == '+' || t.symbol == '-')) {
                consume()
                val inner = parseUnary()
                return if (t.symbol == '-') inner.negate(mc) else inner
            }
            return parsePrimary()
        }

        private fun parsePrimary(): BigDecimal {
            val token = consume() ?: throw IllegalArgumentException("นิพจน์จบไม่สมบูรณ์")
            return when (token) {
                is Token.Number -> {
                    val value = token.value
                    if (peek() is Token.LParen) {
                        val inside = parseExpression()
                        val close = consume()
                        if (close !is Token.RParen) {
                            throw IllegalArgumentException("ขาดวงเล็บปิด ')'")
                        }
                        value.multiply(inside, mc)
                    } else {
                        value
                    }
                }
                is Token.LParen -> {
                    val inside = parseExpression()
                    val close = consume()
                    if (close !is Token.RParen) {
                        throw IllegalArgumentException("ขาดวงเล็บปิด ')'")
                    }
                    inside
                }
                else -> throw IllegalArgumentException("รูปแบบนิพจน์ไม่ถูกต้อง")
            }
        }
    }
}
