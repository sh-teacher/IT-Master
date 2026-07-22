package com.baefamily.schedule.data.firebase

import com.google.firebase.firestore.FirebaseFirestore

object FirestoreRefs {
    const val USERS = "users"
    const val SCHEDULES = "schedules"

    fun db(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
