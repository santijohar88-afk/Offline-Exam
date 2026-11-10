package com.example.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

data class AppPreferences(
    // Student Identity
    val lastStudentName: String = "",
    val lastStudentId: String = "",
    val lastStudentClass: String = "",

    // Teacher Preferences
    val hapticFeedbackEnabled: Boolean = true,
    val soundAlertsEnabled: Boolean = true,
    val defaultClassFilter: String = "ALL",
    val maxViolationsAllowed: Int = 3,
    val teacherPinHash: String = "",

    // Active Exam Progress Persistence
    val hasActiveExam: Boolean = false,
    val activeExamId: String = "",
    val activeExamAnswersJson: String = "{}",
    val activeExamTimeRemaining: Long = 0L,
    val activeExamEndTime: Long = 0L,
    val activeExamViolations: Int = 0,
    val activeExamBookmarksJson: String = "[]",
    val activeExamSeed: Long = 0L,
    val activeExamStartedAt: String = "",

    // App General Settings
    val darkModePreference: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val lastBackupTimestamp: Long = 0L
)

object PreferenceKeys {
    val KEY_STUDENT_NAME = stringPreferencesKey("pref_student_name")
    val KEY_STUDENT_ID = stringPreferencesKey("pref_student_id")
    val KEY_STUDENT_CLASS = stringPreferencesKey("pref_student_class")

    val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("pref_haptic_feedback")
    val KEY_SOUND_ALERTS = booleanPreferencesKey("pref_sound_alerts")
    val KEY_DEFAULT_CLASS_FILTER = stringPreferencesKey("pref_default_class_filter")
    val KEY_MAX_VIOLATIONS = intPreferencesKey("pref_max_violations")
    val KEY_TEACHER_PIN_HASH = stringPreferencesKey("pref_teacher_pin_hash")

    val KEY_HAS_ACTIVE_EXAM = booleanPreferencesKey("pref_has_active_exam")
    val KEY_ACTIVE_EXAM_ID = stringPreferencesKey("pref_active_exam_id")
    val KEY_ACTIVE_EXAM_ANSWERS = stringPreferencesKey("pref_active_exam_answers")
    val KEY_ACTIVE_EXAM_TIME_REMAINING = longPreferencesKey("pref_active_exam_time_remaining")
    val KEY_ACTIVE_EXAM_END_TIME = longPreferencesKey("pref_active_exam_end_time")
    val KEY_ACTIVE_EXAM_VIOLATIONS = intPreferencesKey("pref_active_exam_violations")
    val KEY_ACTIVE_EXAM_BOOKMARKS = stringPreferencesKey("pref_active_exam_bookmarks")
    val KEY_ACTIVE_EXAM_SEED = longPreferencesKey("pref_active_exam_seed")
    val KEY_ACTIVE_EXAM_STARTED_AT = stringPreferencesKey("pref_active_exam_started_at")

    val KEY_DARK_MODE = stringPreferencesKey("pref_dark_mode")
    val KEY_LAST_BACKUP = longPreferencesKey("pref_last_backup")
}
