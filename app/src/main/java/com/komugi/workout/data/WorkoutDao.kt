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

    // ---------- 訓練紀錄 ----------

    @Query(
        "SELECT s.*, (SELECT COUNT(*) FROM entries e WHERE e.sessionId = s.id) AS entryCount " +
            "FROM sessions s ORDER BY s.dateEpochDay DESC, s.id DESC"
    )
    fun observeSummaries(): Flow<List<SessionSummary>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun sessionById(id: Long): SessionEntity?

    @Query("SELECT * FROM entries WHERE sessionId = :sessionId ORDER BY orderIndex ASC, id ASC")
    fun observeEntries(sessionId: Long): Flow<List<EntryEntity>>

    @Query(
        "SELECT sets.* FROM sets INNER JOIN entries ON sets.entryId = entries.id " +
            "WHERE entries.sessionId = :sessionId ORDER BY sets.entryId ASC, sets.setIndex ASC"
    )
    fun observeSets(sessionId: Long): Flow<List<SetEntity>>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun entryById(id: Long): EntryEntity?

    @Query("UPDATE entries SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun updateEntryOrder(id: Long, orderIndex: Int)

    @Query("UPDATE menu_exercises SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun updateMenuExerciseOrder(id: Long, orderIndex: Int)

    @Query(
        "SELECT ss.id AS sessionId, ss.dateEpochDay AS dateEpochDay, ss.menuName AS menuName, " +
            "e.id AS entryId, e.name AS entryName, st.weightKg AS weightKg, st.reps AS reps, " +
            "st.durationSec AS durationSec, st.isBodyweight AS isBodyweight " +
            "FROM sets st INNER JOIN entries e ON st.entryId = e.id " +
            "INNER JOIN sessions ss ON e.sessionId = ss.id " +
            "WHERE (e.exerciseId = :exerciseId OR e.name = :name) " +
            "ORDER BY ss.dateEpochDay ASC, ss.id ASC, st.setIndex ASC"
    )
    fun observeExerciseSets(exerciseId: Long, name: String): Flow<List<ExerciseSetRow>>

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

    // ---------- 菜單（正規化） ----------

    @Query(
        "SELECT m.*, (SELECT COUNT(*) FROM menu_exercises me WHERE me.menuId = m.id) AS exerciseCount " +
            "FROM menus m ORDER BY m.orderIndex ASC"
    )
    fun observeMenuSummaries(): Flow<List<MenuSummary>>

    @Query("SELECT * FROM menus WHERE id = :id")
    fun observeMenu(id: String): Flow<MenuEntity?>

    @Query("SELECT * FROM menus WHERE id = :id")
    suspend fun menuById(id: String): MenuEntity?

    @Query(
        "SELECT me.id AS menuExerciseId, me.menuId AS menuId, me.exerciseId AS exerciseId, " +
            "me.orderIndex AS orderIndex, me.equipment AS equipment, me.targetSets AS targetSets, " +
            "me.targetReps AS targetReps, me.restSec AS restSec, me.supersetKey AS supersetKey, " +
            "me.slotKey AS slotKey, me.role AS role, me.laterality AS laterality, me.pattern AS pattern, " +
            "e.name AS name, e.parts AS parts, " +
            "e.videoUrl AS videoUrl, e.guideId AS guideId " +
            "FROM menu_exercises me INNER JOIN exercises e ON e.id = me.exerciseId " +
            "WHERE me.menuId = :menuId ORDER BY me.orderIndex ASC"
    )
    fun observeMenuExerciseRows(menuId: String): Flow<List<MenuExerciseRow>>

    @Query(
        "SELECT me.id AS menuExerciseId, me.menuId AS menuId, me.exerciseId AS exerciseId, " +
            "me.orderIndex AS orderIndex, me.equipment AS equipment, me.targetSets AS targetSets, " +
            "me.targetReps AS targetReps, me.restSec AS restSec, me.supersetKey AS supersetKey, " +
            "me.slotKey AS slotKey, me.role AS role, me.laterality AS laterality, me.pattern AS pattern, " +
            "e.name AS name, e.parts AS parts, " +
            "e.videoUrl AS videoUrl, e.guideId AS guideId " +
            "FROM menu_exercises me INNER JOIN exercises e ON e.id = me.exerciseId " +
            "WHERE me.menuId = :menuId ORDER BY me.orderIndex ASC"
    )
    suspend fun menuExerciseRows(menuId: String): List<MenuExerciseRow>

    @Query("SELECT COUNT(*) FROM menus")
    suspend fun menuCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMenu(menu: MenuEntity)

    @Query("DELETE FROM menu_exercises")
    suspend fun clearMenuExercises()

    @Query("DELETE FROM menus")
    suspend fun clearMenus()

    @Query("DELETE FROM menu_exercises WHERE menuId NOT IN (SELECT id FROM menus WHERE isCustom = 1)")
    suspend fun clearSeededMenuExercises()

    @Query("DELETE FROM menus WHERE isCustom = 0")
    suspend fun clearSeededMenus()

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM menus")
    suspend fun maxMenuOrder(): Int

    @Delete
    suspend fun deleteMenu(menu: MenuEntity)

    @Query("SELECT value FROM app_meta WHERE `key` = :key LIMIT 1")
    suspend fun metaValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: AppMetaEntity)

    @Insert
    suspend fun insertMenuExercise(item: MenuExerciseEntity): Long

    // ---------- 動作主檔 ----------

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1")
    suspend fun exerciseByName(name: String): ExerciseEntity?

    @Query("SELECT id FROM exercises WHERE name = :name LIMIT 1")
    suspend fun exerciseIdByName(name: String): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    // ---------- 身體數據 ----------

    @Query("SELECT * FROM body_metrics ORDER BY dateEpochDay ASC")
    fun observeBodyMetrics(): Flow<List<BodyMetricEntity>>

    @Query("SELECT * FROM body_metrics WHERE dateEpochDay = :epochDay LIMIT 1")
    suspend fun bodyMetricByDate(epochDay: Long): BodyMetricEntity?

    @Query("SELECT * FROM body_metrics WHERE id = :id LIMIT 1")
    suspend fun bodyMetricById(id: Long): BodyMetricEntity?

    @Query("SELECT COUNT(*) FROM body_metrics")
    suspend fun bodyMetricCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyMetric(metric: BodyMetricEntity): Long

    @Update
    suspend fun updateBodyMetric(metric: BodyMetricEntity)

    @Delete
    suspend fun deleteBodyMetric(metric: BodyMetricEntity)

    // ---------- 備份 / 還原 ----------

    @Query("SELECT * FROM exercises")
    suspend fun allExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM menus")
    suspend fun allMenus(): List<MenuEntity>

    @Query("SELECT * FROM menu_exercises")
    suspend fun allMenuExercises(): List<MenuExerciseEntity>

    @Query("SELECT * FROM sessions")
    suspend fun allSessions(): List<SessionEntity>

    @Query("SELECT * FROM entries")
    suspend fun allEntries(): List<EntryEntity>

    @Query("SELECT * FROM sets")
    suspend fun allSets(): List<SetEntity>

    @Query("SELECT * FROM body_metrics")
    suspend fun allBodyMetrics(): List<BodyMetricEntity>

    @Query("DELETE FROM body_metrics")
    suspend fun clearAllBodyMetrics()

    @Query("DELETE FROM sets")
    suspend fun clearAllSets()

    @Query("DELETE FROM entries")
    suspend fun clearAllEntries()

    @Query("DELETE FROM sessions")
    suspend fun clearAllSessions()

    @Query("DELETE FROM menu_exercises")
    suspend fun clearAllMenuExercises()

    @Query("DELETE FROM menus")
    suspend fun clearAllMenus()

    @Query("DELETE FROM exercises")
    suspend fun clearAllExercises()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(items: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenus(items: List<MenuEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuExercises(items: List<MenuExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(items: List<SessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(items: List<EntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(items: List<SetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyMetrics(items: List<BodyMetricEntity>)
}
