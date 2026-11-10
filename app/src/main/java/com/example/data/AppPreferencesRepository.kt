package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.IOException

val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "ekiii_app_preferences")

class AppPreferencesRepository(private val context: Context) {

    private val dataStore = context.appPreferencesDataStore
    private val scope = CoroutineScope(Dispatchers.IO)

    val preferencesFlow: Flow<AppPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppPreferences(
                lastStudentName = preferences[PreferenceKeys.KEY_STUDENT_NAME] ?: "",
                lastStudentId = preferences[PreferenceKeys.KEY_STUDENT_ID] ?: "",
                lastStudentClass = preferences[PreferenceKeys.KEY_STUDENT_CLASS] ?: "",

                hapticFeedbackEnabled = preferences[PreferenceKeys.KEY_HAPTIC_FEEDBACK] ?: true,
                soundAlertsEnabled = preferences[PreferenceKeys.KEY_SOUND_ALERTS] ?: true,
                defaultClassFilter = preferences[PreferenceKeys.KEY_DEFAULT_CLASS_FILTER] ?: "ALL",
                maxViolationsAllowed = preferences[PreferenceKeys.KEY_MAX_VIOLATIONS] ?: 3,
                teacherPinHash = preferences[PreferenceKeys.KEY_TEACHER_PIN_HASH] ?: "",

                hasActiveExam = preferences[PreferenceKeys.KEY_HAS_ACTIVE_EXAM] ?: false,
                activeExamId = preferences[PreferenceKeys.KEY_ACTIVE_EXAM_ID] ?: "",
                activeExamAnswersJson = preferences[PreferenceKeys.KEY_ACTIVE_EXAM_ANSWERS] ?: "{}",
                activeExamTimeRemaining = preferences[PreferenceKeys.KEY_ACTIVE_EXAM_TIME_REMAINING] ?: 0L,
                activeExamEndTime = preferences[PreferenceKeys.KEY_ACTIVE_EXAM_END_TIME] ?: 0L,
                activeExamViolations = preferences[PreferenceKeys.KEY_ACTIVE_EXAM_VIOLATIONS] ?: 0,
                activeExamBookmarksJson = preferences[PreferenceKeys.KEY_ACTIVE_EXAM_BOOKMARKS] ?: "[]",
                activeExamSeed = preferences[PreferenceKeys.KEY_ACTIVE_EXAM_SEED] ?: 0L,
                activeExamStartedAt = preferences[PreferenceKeys.KEY_ACTIVE_EXAM_STARTED_AT] ?: "",

                darkModePreference = preferences[PreferenceKeys.KEY_DARK_MODE] ?: "SYSTEM",
                lastBackupTimestamp = preferences[PreferenceKeys.KEY_LAST_BACKUP] ?: 0L
            )
        }

    suspend fun updateStudentProfile(name: String, id: String, className: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.KEY_STUDENT_NAME] = name
            preferences[PreferenceKeys.KEY_STUDENT_ID] = id
            preferences[PreferenceKeys.KEY_STUDENT_CLASS] = className
        }
    }

    suspend fun updateTeacherPreferences(
        hapticFeedback: Boolean,
        defaultClassFilter: String,
        maxViolations: Int
    ) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.KEY_HAPTIC_FEEDBACK] = hapticFeedback
            preferences[PreferenceKeys.KEY_DEFAULT_CLASS_FILTER] = defaultClassFilter
            preferences[PreferenceKeys.KEY_MAX_VIOLATIONS] = maxViolations
        }
    }

    suspend fun updateDarkModePreference(mode: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.KEY_DARK_MODE] = mode
        }
    }

    suspend fun saveActiveExamProgress(
        examId: String,
        studentName: String,
        studentId: String,
        studentClass: String,
        answersJson: String,
        timeRemaining: Long,
        endTime: Long,
        violations: Int,
        bookmarksJson: String,
        seed: Long,
        startedAt: String
    ) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.KEY_HAS_ACTIVE_EXAM] = true
            preferences[PreferenceKeys.KEY_ACTIVE_EXAM_ID] = examId
            preferences[PreferenceKeys.KEY_STUDENT_NAME] = studentName
            preferences[PreferenceKeys.KEY_STUDENT_ID] = studentId
            preferences[PreferenceKeys.KEY_STUDENT_CLASS] = studentClass
            preferences[PreferenceKeys.KEY_ACTIVE_EXAM_ANSWERS] = answersJson
            preferences[PreferenceKeys.KEY_ACTIVE_EXAM_TIME_REMAINING] = timeRemaining
            preferences[PreferenceKeys.KEY_ACTIVE_EXAM_END_TIME] = endTime
            preferences[PreferenceKeys.KEY_ACTIVE_EXAM_VIOLATIONS] = violations
            preferences[PreferenceKeys.KEY_ACTIVE_EXAM_BOOKMARKS] = bookmarksJson
            preferences[PreferenceKeys.KEY_ACTIVE_EXAM_SEED] = seed
            preferences[PreferenceKeys.KEY_ACTIVE_EXAM_STARTED_AT] = startedAt
        }
    }

    suspend fun clearActiveExamProgress() {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.KEY_HAS_ACTIVE_EXAM] = false
            preferences[PreferenceKeys.KEY_ACTIVE_EXAM_ID] = ""
            preferences[PreferenceKeys.KEY_ACTIVE_EXAM_ANSWERS] = "{}"
            preferences[PreferenceKeys.KEY_ACTIVE_EXAM_TIME_REMAINING] = 0L
            preferences[PreferenceKeys.KEY_ACTIVE_EXAM_END_TIME] = 0L
            preferences[PreferenceKeys.KEY_ACTIVE_EXAM_VIOLATIONS] = 0
            preferences[PreferenceKeys.KEY_ACTIVE_EXAM_BOOKMARKS] = "[]"
            preferences[PreferenceKeys.KEY_ACTIVE_EXAM_SEED] = 0L
            preferences[PreferenceKeys.KEY_ACTIVE_EXAM_STARTED_AT] = ""
        }
    }

    suspend fun saveTeacherPinHash(pinHash: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.KEY_TEACHER_PIN_HASH] = pinHash
        }
    }

    suspend fun recordBackupTimestamp(timestamp: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.KEY_LAST_BACKUP] = timestamp
        }
    }

    // Synchronous accessors for JavaScript Interface bridging
    fun saveExamProgressAsync(
        examId: String,
        studentName: String,
        studentId: String,
        studentClass: String,
        answersJson: String,
        timeRemaining: Long,
        endTime: Long,
        violations: Int,
        bookmarksJson: String,
        seed: Long,
        startedAt: String
    ) {
        scope.launch {
            saveActiveExamProgress(
                examId = examId,
                studentName = studentName,
                studentId = studentId,
                studentClass = studentClass,
                answersJson = answersJson,
                timeRemaining = timeRemaining,
                endTime = endTime,
                violations = violations,
                bookmarksJson = bookmarksJson,
                seed = seed,
                startedAt = startedAt
            )
        }
    }

    fun clearExamProgressAsync() {
        scope.launch {
            clearActiveExamProgress()
        }
    }

    fun getActiveExamProgressJson(): String {
        return try {
            runBlocking(Dispatchers.IO) {
                val current = preferencesFlow.first()
                if (!current.hasActiveExam || current.activeExamId.isBlank()) {
                    "{}"
                } else {
                    JSONObject().apply {
                        put("hasActiveExam", current.hasActiveExam)
                        put("examId", current.activeExamId)
                        put("studentName", current.lastStudentName)
                        put("studentId", current.lastStudentId)
                        put("studentClass", current.lastStudentClass)
                        put("answersJson", current.activeExamAnswersJson)
                        put("timeRemaining", current.activeExamTimeRemaining)
                        put("endTime", current.activeExamEndTime)
                        put("violations", current.activeExamViolations)
                        put("bookmarksJson", current.activeExamBookmarksJson)
                        put("seed", current.activeExamSeed)
                        put("startedAt", current.activeExamStartedAt)
                    }.toString()
                }
            }
        } catch (_: Exception) {
            "{}"
        }
    }

    fun getStudentProfileJson(): String {
        return try {
            runBlocking(Dispatchers.IO) {
                val current = preferencesFlow.first()
                JSONObject().apply {
                    put("name", current.lastStudentName)
                    put("id", current.lastStudentId)
                    put("className", current.lastStudentClass)
                }.toString()
            }
        } catch (_: Exception) {
            "{}"
        }
    }
}
