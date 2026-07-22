package com.baefamily.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.baefamily.schedule.ui.navigation.AppRoot
import com.baefamily.schedule.ui.theme.FamilyScheduleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FamilyScheduleTheme {
                AppRoot()
            }
        }
    }
}
