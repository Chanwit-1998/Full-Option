package com.druk.ai_offline_llm.tools

import android.content.Context
import com.druk.llamacpp.tools.Tool
import com.druk.llamacpp.tools.ToolRegistry
import org.json.JSONObject

fun ToolRegistry.Companion.createDefault(context: Context): ToolRegistry =
    ToolRegistry().apply {
        register(WebSearchTool(webLinkStore))
        register(WebFetchTool(webLinkStore))
        register(JavaScriptTool(context))
        register(OfflineClockAgentTool())
        register(OfflineMathAgentTool())
    }

class OfflineClockAgentTool : Tool {
    override val name = "device_clock"
    override val description = "Get the exact offline device local date/time, calculate relative dates by offsetting days (e.g. +7 or -30 days), or count days between two ISO dates (YYYY-MM-DD)."
    override val parametersSchema = """{"type":"object","properties":{"action":{"type":"string","enum":["now","offset_days","days_between"],"description":"Operation to perform"},"days":{"type":"integer","description":"Number of days to add or subtract when action is offset_days"},"start_date":{"type":"string","description":"Start ISO date YYYY-MM-DD when action is days_between"},"end_date":{"type":"string","description":"End ISO date YYYY-MM-DD when action is days_between"}},"required":["action"]}"""

    override fun execute(arguments: String): String {
        return try {
            val args = JSONObject(arguments)
            when (args.optString("action", "now")) {
                "offset_days" -> {
                    val days = args.optLong("days", 0L)
                    JSONObject().put("result", SystemClockTool.offsetDays(days)).toString()
                }
                "days_between" -> {
                    val start = args.getString("start_date")
                    val end = args.getString("end_date")
                    val res = SystemClockTool.daysBetween(start, end)
                        ?: return """{"error":"Invalid ISO date format. Use YYYY-MM-DD."}"""
                    JSONObject().put("result", res).toString()
                }
                else -> {
                    JSONObject().put("result", SystemClockTool.formatCurrentDateTimeSummary()).toString()
                }
            }
        } catch (e: Exception) {
            val safeMsg = (e.message ?: "Clock tool error").replace("\"", "'")
            """{"error":"$safeMsg"}"""
        }
    }
}

class OfflineMathAgentTool : Tool {
    override val name = "math_calculator"
    override val description = "Evaluate exact high-precision arithmetic expressions (+, -, *, /, ^, %, parentheses) offline."
    override val parametersSchema = """{"type":"object","properties":{"expression":{"type":"string","description":"Mathematical expression to evaluate, e.g. (140 / (1 - 0.30)) * 1.07"}},"required":["expression"]}"""

    override fun execute(arguments: String): String {
        return try {
            val args = JSONObject(arguments)
            val expr = args.getString("expression")
            val result = MathCalculatorTool.evaluateExpression(expr).getOrThrow()
            JSONObject().apply {
                put("expression", expr)
                put("result", result)
            }.toString()
        } catch (e: Exception) {
            val safeMsg = (e.message ?: "Math evaluation error").replace("\"", "'")
            """{"error":"$safeMsg"}"""
        }
    }
}

object BuiltInOfflineTools {
    fun buildEnrichedPrompt(userPrompt: String): String {
        val clockSnippet = SystemClockTool.buildContextSnippetForPrompt(userPrompt)
        val mathSnippet = MathCalculatorTool.tryAutoCalculateFromPrompt(userPrompt)
        return buildString {
            append(clockSnippet)
            if (!mathSnippet.isNullOrBlank()) {
                append(" ")
                append(mathSnippet)
            }
            append("\n\nคำถามของผู้ใช้: ")
            append(userPrompt)
        }
    }
}
