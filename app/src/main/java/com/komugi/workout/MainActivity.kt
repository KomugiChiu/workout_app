package com.komugi.workout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.komugi.workout.ui.AppRoot
import com.komugi.workout.ui.theme.KomugiTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as KomugiApp).repository
        setContent {
            KomugiTheme {
                AppRoot(repository)
            }
        }
    }
}
