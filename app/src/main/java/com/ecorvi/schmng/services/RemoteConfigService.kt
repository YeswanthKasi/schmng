package com.ecorvi.schmng.services

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.tasks.await
import kotlin.time.Duration.Companion.hours

object RemoteConfigService {
    private const val FETCH_INTERVAL = 12 // hours

    // Remote config keys
    object Keys {
        // Feature flags
        const val ENABLE_CHAT_FEATURE = "enable_chat_feature"
        const val ENABLE_ATTENDANCE_FEATURE = "enable_attendance_feature"
        const val ENABLE_LEAVE_MANAGEMENT = "enable_leave_management"
        const val ENABLE_EVENTS_FEATURE = "enable_events_feature"
        const val ENABLE_FEES_FEATURE = "enable_fees_feature"
        
        // App maintenance
        const val MAINTENANCE_MODE = "maintenance_mode"
        const val MAINTENANCE_MESSAGE = "maintenance_message"
        const val MIN_APP_VERSION = "min_app_version"
        const val RECOMMENDED_APP_VERSION = "recommended_app_version"
        
        // School settings
        const val SCHOOL_NAME = "school_name"
        const val SCHOOL_CONTACT = "school_contact"
        const val SCHOOL_EMAIL = "school_email"
        const val SCHOOL_ADDRESS = "school_address"
        
        // Academic settings
        const val ACADEMIC_YEAR = "academic_year"
        const val ATTENDANCE_CUTOFF_TIME = "attendance_cutoff_time"
        const val MAX_LEAVE_DAYS = "max_leave_days"
        const val ENABLE_WEEKEND_ATTENDANCE = "enable_weekend_attendance"
    }

    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = FETCH_INTERVAL.hours.inWholeSeconds
        }
        
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        // Set default values
        remoteConfig.setDefaultsAsync(
            mapOf(
                // Feature flags
                Keys.ENABLE_CHAT_FEATURE to true,
                Keys.ENABLE_ATTENDANCE_FEATURE to true,
                Keys.ENABLE_LEAVE_MANAGEMENT to true,
                Keys.ENABLE_EVENTS_FEATURE to true,
                Keys.ENABLE_FEES_FEATURE to true,
                
                // App maintenance
                Keys.MAINTENANCE_MODE to false,
                Keys.MAINTENANCE_MESSAGE to "We're performing scheduled maintenance. Please try again later.",
                Keys.MIN_APP_VERSION to "1.0.0",
                Keys.RECOMMENDED_APP_VERSION to "1.0.0",
                
                // School settings
                Keys.SCHOOL_NAME to "School Name",
                Keys.SCHOOL_CONTACT to "+1234567890",
                Keys.SCHOOL_EMAIL to "school@example.com",
                Keys.SCHOOL_ADDRESS to "School Address",
                
                // Academic settings
                Keys.ACADEMIC_YEAR to "2024-2025",
                Keys.ATTENDANCE_CUTOFF_TIME to "10:00",
                Keys.MAX_LEAVE_DAYS to 15L,
                Keys.ENABLE_WEEKEND_ATTENDANCE to false
            )
        )
    }

    suspend fun fetchAndActivate(): Boolean {
        return try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            false
        }
    }

    fun getBoolean(key: String): Boolean = remoteConfig.getBoolean(key)
    
    fun getString(key: String): String = remoteConfig.getString(key)
    
    fun getLong(key: String): Long = remoteConfig.getLong(key)
    
    fun getDouble(key: String): Double = remoteConfig.getDouble(key)

    // Helper functions for specific features
    fun isChatFeatureEnabled() = getBoolean(Keys.ENABLE_CHAT_FEATURE)
    fun isAttendanceFeatureEnabled() = getBoolean(Keys.ENABLE_ATTENDANCE_FEATURE)
    fun isLeaveManagementEnabled() = getBoolean(Keys.ENABLE_LEAVE_MANAGEMENT)
    fun isEventsFeatureEnabled() = getBoolean(Keys.ENABLE_EVENTS_FEATURE)
    fun isFeesFeatureEnabled() = getBoolean(Keys.ENABLE_FEES_FEATURE)
    
    fun isInMaintenanceMode() = getBoolean(Keys.MAINTENANCE_MODE)
    fun getMaintenanceMessage() = getString(Keys.MAINTENANCE_MESSAGE)
    fun getMinAppVersion() = getString(Keys.MIN_APP_VERSION)
    fun getRecommendedAppVersion() = getString(Keys.RECOMMENDED_APP_VERSION)
    
    fun getSchoolName() = getString(Keys.SCHOOL_NAME)
    fun getSchoolContact() = getString(Keys.SCHOOL_CONTACT)
    fun getSchoolEmail() = getString(Keys.SCHOOL_EMAIL)
    fun getSchoolAddress() = getString(Keys.SCHOOL_ADDRESS)
    
    fun getAcademicYear() = getString(Keys.ACADEMIC_YEAR)
    fun getAttendanceCutoffTime() = getString(Keys.ATTENDANCE_CUTOFF_TIME)
    fun getMaxLeaveDays() = getLong(Keys.MAX_LEAVE_DAYS)
    fun isWeekendAttendanceEnabled() = getBoolean(Keys.ENABLE_WEEKEND_ATTENDANCE)
} 