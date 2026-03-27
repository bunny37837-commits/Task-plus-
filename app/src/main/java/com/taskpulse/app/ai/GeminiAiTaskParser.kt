package com.taskpulse.app.ai

import com.taskpulse.app.BuildConfig
import com.taskpulse.app.data.datastore.AppDataStore
import com.taskpulse.app.domain.model.Priority
import com.taskpulse.app.domain.model.RecurrenceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiAiTaskParser @Inject constructor(
    private val fallback: HeuristicAiTaskParser,
    private val appDataStore: AppDataStore,
) : AiTaskParser {

    override suspend fun parse(message: String): Result<TaskDraft> {
        val manualKey = appDataStore.getGeminiApiKey()
        val apiKey = if (manualKey.isNotBlank()) manualKey else BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) return fallback.parse(message)

        return runCatching {
            val json = requestGemini(message, apiKey)
            parseDraftJson(json)
        }.recoverCatching {
            fallback.parse(message).getOrThrow()
        }
    }

    private suspend fun requestGemini(message: String, apiKey: String): String = withContext(Dispatchers.IO) {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }

        val prompt = """
            You are a task parser.
            Convert user reminder request into strict JSON with keys:
            title, description, date, time, recurrence, priority.

            Rules:
            - date format: yyyy-MM-dd
            - time format: HH:mm (24-hour)
            - recurrence: NONE, DAILY, WEEKDAYS, WEEKLY, MONTHLY
            - priority: LOW, MEDIUM, HIGH, CRITICAL
            - If missing info, infer sensible defaults near future.
            - Return JSON only, no markdown.

            User message:
            $message
        """.trimIndent()

        val body = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            ))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("responseMimeType", "application/json")
            })
        }

        OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val raw = BufferedReader(stream.reader()).readText()
        if (status !in 200..299) {
            error("Gemini error $status: $raw")
        }

        val root = JSONObject(raw)
        val text = root
            .optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")
            ?.trim()
            ?: error("Gemini returned empty response")

        text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    }

    private fun parseDraftJson(jsonText: String): TaskDraft {
        val json = JSONObject(jsonText)
        val title = json.optString("title").ifBlank { "Reminder" }
        val description = json.optString("description")
        val date = json.optString("date").takeIf { it.isNotBlank() }?.let(LocalDate::parse) ?: LocalDate.now()
        val time = json.optString("time").takeIf { it.isNotBlank() }?.let(LocalTime::parse)
            ?: LocalTime.now().plusMinutes(5).withSecond(0).withNano(0)

        val recurrence = runCatching {
            RecurrenceType.valueOf(json.optString("recurrence", "NONE").uppercase())
        }.getOrDefault(RecurrenceType.NONE)

        val priority = runCatching {
            Priority.valueOf(json.optString("priority", "MEDIUM").uppercase())
        }.getOrDefault(Priority.MEDIUM)

        return TaskDraft(
            title = title,
            description = description,
            date = date,
            time = time,
            recurrence = recurrence,
            priority = priority,
        )
    }
}
