package com.baefamily.schedule.data.model

data class NotificationSettings(
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
    val defaultLeadMinutes: Int = 10
)

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val role: FamilyRole = FamilyRole.DAD,
    val notificationSettings: NotificationSettings = NotificationSettings()
)
