package com.taskpulse.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.taskpulse.app.domain.model.Category
import com.taskpulse.app.domain.repository.CategoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore by preferencesDataStore("taskpulse_prefs")

@Singleton
class AppDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val categoryRepository: CategoryRepository,
) {
    private val SEEDED_KEY = booleanPreferencesKey("categories_seeded")
    private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    private val DEFAULT_VIBRATE = booleanPreferencesKey("default_vibrate")
    private val DEFAULT_SHOW_OVERLAY = booleanPreferencesKey("default_show_overlay")
    private val AUTO_RESCHEDULE_MISSED = booleanPreferencesKey("auto_reschedule_missed")

    val geminiApiKeyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[GEMINI_API_KEY].orEmpty()
    }

    val defaultVibrateFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_VIBRATE] ?: true
    }

    val defaultShowOverlayFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_SHOW_OVERLAY] ?: true
    }

    val autoRescheduleMissedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_RESCHEDULE_MISSED] ?: false
    }

    suspend fun getGeminiApiKey(): String = context.dataStore.data.first()[GEMINI_API_KEY].orEmpty()

    suspend fun setGeminiApiKey(value: String) {
        context.dataStore.edit { prefs -> prefs[GEMINI_API_KEY] = value.trim() }
    }

    suspend fun setDefaultVibrate(value: Boolean) {
        context.dataStore.edit { it[DEFAULT_VIBRATE] = value }
    }

    suspend fun setDefaultShowOverlay(value: Boolean) {
        context.dataStore.edit { it[DEFAULT_SHOW_OVERLAY] = value }
    }

    suspend fun setAutoRescheduleMissed(value: Boolean) {
        context.dataStore.edit { it[AUTO_RESCHEDULE_MISSED] = value }
    }

    suspend fun shouldAutoRescheduleMissed(): Boolean = autoRescheduleMissedFlow.first()

    suspend fun seedDefaultCategoriesIfNeeded() {
        val defaults = listOf(
            Category(name = "Health", colorHex = "#00B894", iconName = "favorite"),
            Category(name = "Work", colorHex = "#6C5CE7", iconName = "work"),
            Category(name = "Personal", colorHex = "#FDCB6E", iconName = "person"),
            Category(name = "Fitness", colorHex = "#E17055", iconName = "fitness_center"),
            Category(name = "Finance", colorHex = "#0984E3", iconName = "attach_money"),
            Category(name = "Study", colorHex = "#00CEC9", iconName = "school"),
            Category(name = "Family", colorHex = "#FD79A8", iconName = "family_restroom"),
            Category(name = "Other", colorHex = "#636E72", iconName = "label"),
        )

        val existingNames = categoryRepository.getAllCategories().first()
            .map { it.name.trim().lowercase() }
            .toSet()

        val missing = defaults.filterNot { it.name.trim().lowercase() in existingNames }
        missing.forEach { categoryRepository.insertCategory(it) }

        val alreadySeeded = context.dataStore.data.first()[SEEDED_KEY] == true
        if (!alreadySeeded || missing.isNotEmpty()) {
            context.dataStore.edit { it[SEEDED_KEY] = true }
        }
    }
}
