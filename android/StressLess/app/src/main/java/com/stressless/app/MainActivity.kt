package com.stressless.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.stressless.app.navigation.RootNavGraph
import com.stressless.app.ui.theme.StressLessTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StressLessTheme {
                RootNavGraph()
            }
        }
    }
}