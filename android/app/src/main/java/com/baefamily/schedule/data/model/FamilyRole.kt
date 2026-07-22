package com.baefamily.schedule.data.model

enum class FamilyRole(val id: String, val displayName: String, val emoji: String) {
    DAD("DAD", "아빠", "👨"),
    MOM("MOM", "엄마", "👩"),
    DAUGHTER1("DAUGHTER1", "첫째딸", "👧"),
    DAUGHTER2("DAUGHTER2", "둘째딸", "🧒");

    companion object {
        fun fromId(id: String?): FamilyRole? = entries.find { it.id == id }
    }
}
