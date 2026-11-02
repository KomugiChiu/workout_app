package com.komugi.workout

import android.app.Application
import com.komugi.workout.data.AppDatabase
import com.komugi.workout.data.WorkoutRepository
import com.komugi.workout.ui.theme.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KomugiApp : Application() {

    val database by lazy { AppDatabase.get(this) }

    val repository by lazy { WorkoutRepository(database, this) }

    val themeManager by lazy { ThemeManager(this) }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            repository.ensureSeeded()
        }
    }
}
