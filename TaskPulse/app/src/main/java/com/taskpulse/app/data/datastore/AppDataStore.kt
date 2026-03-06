package com.taskpulse.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.taskpulse.app.domain.model.Category
import com.taskpulse.app.domain.repository.CategoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore by preferencesDataStore("taskpulse_prefs")

@Singleton
class AppDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val categoryRepository: CategoryRepository,
) {
    private val SEEDED_KEY = booleanPreferencesKey("categories_seeded")

    suspend fun seedDefaultCategoriesIfNeeded() {
        val prefs = context.dataStore.data.first()
        if (prefs[SEEDED_KEY] == true) return

        val defaults = listOf(
            Category(name = "Health",   colorHex = "#00B894", iconName = "favorite"),
            Category(name = "Work",     colorHex = "#6C5CE7", iconName = "work"),
            Category(name = "Personal", colorHex = "#FDCB6E", iconName = "person"),
            Category(name = "Fitness",  colorHex = "#E17055", iconName = "fitness_center"),
            Category(name = "Finance",  colorHex = "#0984E3", iconName = "attach_money"),
            Category(name = "Study",    colorHex = "#00CEC9", iconName = "school"),
            Category(name = "Family",   colorHex = "#FD79A8", iconName = "family_restroom"),
            Category(name = "Other",    colorHex = "#636E72", iconName = "label"),
        )
        defaults.forEach { categoryRepository.insertCategory(it) }

        context.dataStore.edit { it[SEEDED_KEY] = true }
    }
}
