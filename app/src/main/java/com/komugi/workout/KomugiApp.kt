package com.komugi.workout

import android.app.Application
import com.komugi.workout.data.AppDatabase
import com.komugi.workout.data.WorkoutRepository

class KomugiApp : Application() {

    val database by lazy { AppDatabase.get(this) }

    val repository by lazy { WorkoutRepository(database.dao(), this) }
}
