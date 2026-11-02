package com.komugi.workout.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Query(
        "SELECT s.*, (SELECT COUNT(*) FROM entries e WHERE e.sessionId = s.id) AS entryCount " +
            "FROM sessions s ORDER BY s.dateEpochDay DESC, s.id DESC"
    )
    fun observeSummaries(): Flow<List<SessionSummary>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM entries WHERE sessionId = :sessionId ORDER BY orderIndex ASC, id ASC")
    fun observeEntries(sessionId: Long): Flow<List<EntryEntity>>

    @Query(
        "SELECT sets.* FROM sets INNER JOIN entries ON sets.entryId = entries.id " +
            "WHERE entries.sessionId = :sessionId ORDER BY sets.entryId ASC, sets.setIndex ASC"
    )
    fun observeSets(sessionId: Long): Flow<List<SetEntity>>

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Insert
    suspend fun insertEntry(entry: EntryEntity): Long

    @Update
    suspend fun updateEntry(entry: EntryEntity)

    @Delete
    suspend fun deleteEntry(entry: EntryEntity)

    @Insert
    suspend fun insertSet(set: SetEntity): Long

    @Update
    suspend fun updateSet(set: SetEntity)

    @Delete
    suspend fun deleteSet(set: SetEntity)

    @Query("SELECT * FROM sets WHERE entryId = :entryId ORDER BY setIndex ASC")
    suspend fun setsOf(entryId: Long): List<SetEntity>

    @Query("SELECT * FROM entries WHERE sessionId = :sessionId ORDER BY orderIndex ASC, id ASC")
    suspend fun entriesOf(sessionId: Long): List<EntryEntity>

    @Query("SELECT * FROM custom_exercises ORDER BY addedAt DESC")
    fun observeCustomExercises(): Flow<List<CustomExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCustomExercise(item: CustomExerciseEntity)
}
