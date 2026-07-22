package com.baefamily.schedule.data.repository

import com.baefamily.schedule.data.firebase.FirestoreRefs
import com.baefamily.schedule.data.model.FamilyRole
import com.baefamily.schedule.data.model.NotificationSettings
import com.baefamily.schedule.data.model.UserProfile
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(private val db: FirebaseFirestore = FirestoreRefs.db()) {

    private val usersRef = db.collection(FirestoreRefs.USERS)

    suspend fun getTakenRoles(): Set<FamilyRole> {
        val snapshot = usersRef.get().await()
        return snapshot.documents.mapNotNull { it.toUserProfile()?.role }.toSet()
    }

    suspend fun createUserProfile(uid: String, name: String, role: FamilyRole): Result<Unit> = runCatching {
        val profile = UserProfile(uid = uid, name = name, role = role)
        usersRef.document(uid).set(profile.toMap()).await()
    }

    suspend fun getUserProfile(uid: String): UserProfile? =
        usersRef.document(uid).get().await().toUserProfile()

    fun observeUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = usersRef.document(uid).addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.toUserProfile())
        }
        awaitClose { registration.remove() }
    }

    fun observeFamilyMembers(): Flow<List<UserProfile>> = callbackFlow {
        val registration = usersRef.addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.documents?.mapNotNull { it.toUserProfile() } ?: emptyList())
        }
        awaitClose { registration.remove() }
    }

    suspend fun updateNotificationSettings(uid: String, settings: NotificationSettings): Result<Unit> = runCatching {
        usersRef.document(uid).update(
            mapOf(
                "notificationSettings" to mapOf(
                    "enabled" to settings.enabled,
                    "vibrate" to settings.vibrate,
                    "defaultLeadMinutes" to settings.defaultLeadMinutes
                )
            )
        ).await()
    }
}

private fun UserProfile.toMap(): Map<String, Any?> = mapOf(
    "uid" to uid,
    "name" to name,
    "role" to role.id,
    "notificationSettings" to mapOf(
        "enabled" to notificationSettings.enabled,
        "vibrate" to notificationSettings.vibrate,
        "defaultLeadMinutes" to notificationSettings.defaultLeadMinutes
    )
)

private fun DocumentSnapshot.toUserProfile(): UserProfile? {
    if (!exists()) return null
    val roleId = getString("role") ?: return null
    val role = FamilyRole.fromId(roleId) ?: return null
    val settingsMap = get("notificationSettings") as? Map<*, *>
    val settings = NotificationSettings(
        enabled = settingsMap?.get("enabled") as? Boolean ?: true,
        vibrate = settingsMap?.get("vibrate") as? Boolean ?: true,
        defaultLeadMinutes = (settingsMap?.get("defaultLeadMinutes") as? Long)?.toInt() ?: 10
    )
    return UserProfile(
        uid = getString("uid") ?: id,
        name = getString("name") ?: "",
        role = role,
        notificationSettings = settings
    )
}
