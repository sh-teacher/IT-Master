package com.baefamily.schedule.ui.theme

import androidx.compose.ui.graphics.Color
import com.baefamily.schedule.data.model.FamilyRole

val DadColor = Color(0xFF4A80F0)
val MomColor = Color(0xFFE15AA0)
val Daughter1Color = Color(0xFF7B5AE1)
val Daughter2Color = Color(0xFF4CAF6E)

val PrimaryColor = Color(0xFF4A80F0)
val BackgroundLight = Color(0xFFFAFAFC)
val SurfaceLight = Color(0xFFFFFFFF)

fun FamilyRole.toColor(): Color = when (this) {
    FamilyRole.DAD -> DadColor
    FamilyRole.MOM -> MomColor
    FamilyRole.DAUGHTER1 -> Daughter1Color
    FamilyRole.DAUGHTER2 -> Daughter2Color
}
