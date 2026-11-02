package com.komugi.workout.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.Json

data class EntryUi(
    val entry: EntryEntity,
    val sets: List<SetEntity>
)

data class SessionUi(
    val session: SessionEntity,
    val entries: List<EntryUi>
)

class WorkoutRepository(
    private val db: AppDatabase,
    private val context: Context
) {
    companion object {
        /** 菜單種子版本：修改 assets/menus.json 後 += 1，已安裝的 App 會自動重建菜單。 */
        private const val SEED_VERSION = 2
    }

    private val dao = db.dao()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 首次啟動（或升級後）把 assets/menus.json 的菜單種子資料寫進資料庫。
     * 菜單 → 動作主檔 → 菜單動作關聯。
     */
    private val menuSeed: List<Menu> by lazy {
        context.assets.open("menus.json").bufferedReader().use {
            json.decodeFromString<List<Menu>>(it.readText())
        }
    }

    val guides: List<GuideRef> by lazy {
        context.assets.open("guides.json").bufferedReader().use {
            json.decodeFromString<GuideIndex>(it.readText()).guides
        }
    }

    fun guideById(id: String?): GuideRef? = guides.firstOrNull { it.id == id }

    fun readGuide(assetPath: String): String =
        context.assets.open(assetPath).bufferedReader().use { it.readText() }

    suspend fun ensureSeeded() {
        val currentVersion = dao.metaValue("seed_version")?.toIntOrNull() ?: 0
        if (currentVersion >= SEED_VERSION) return

        db.withTransaction {
            // 保留 exercises（含自訂動作）與訓練紀錄；只重建菜單與關聯
            dao.clearMenuExercises()
            dao.clearMenus()

            menuSeed.forEachIndexed { menuIndex, menu ->
                dao.insertMenu(
                    MenuEntity(
                        id = menu.id,
                        name = menu.name,
                        subtitle = menu.subtitle,
                        note = menu.note,
                        orderIndex = menuIndex
                    )
                )
                menu.exercises.forEachIndexed { exIndex, ex ->
                    val existing = dao.exerciseByName(ex.name)
                    val exerciseId = if (existing == null) {
                        dao.insertExercise(
                            ExerciseEntity(
                                name = ex.name,
                                parts = ex.parts,
                                videoUrl = ex.video,
                                guideId = ex.guide,
                                isCustom = false,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        if (existing.parts == null &&
                            (ex.parts != null || ex.video != null || ex.guide != null)
                        ) {
                            dao.updateExercise(
                                existing.copy(
                                    parts = ex.parts ?: existing.parts,
                                    videoUrl = ex.video ?: existing.videoUrl,
                                    guideId = ex.guide ?: existing.guideId,
                                    isCustom = false
                                )
                            )
                        }
                        existing.id
                    }
                    dao.insertMenuExercise(
                        MenuExerciseEntity(
                            menuId = menu.id,
                            exerciseId = exerciseId,
                            orderIndex = exIndex,
                            equipment = ex.equipment,
                            targetSets = ex.sets,
                            targetReps = ex.reps,
                            restSec = ex.rest
                        )
                    )
                }
            }

            dao.upsertMeta(AppMetaEntity("seed_version", SEED_VERSION.toString()))
        }
    }

    // ---------- 菜單 ----------

    fun observeMenuSummaries(): Flow<List<MenuSummary>> = dao.observeMenuSummaries()

    fun observeMenu(id: String): Flow<MenuEntity?> = dao.observeMenu(id)

    fun observeMenuExercises(id: String): Flow<List<MenuExerciseRow>> =
        dao.observeMenuExerciseRows(id)

    // ---------- 動作主檔 ----------

    fun observeExercises(): Flow<List<ExerciseEntity>> = dao.observeExercises()

    suspend fun exerciseByName(name: String): ExerciseEntity? =
        dao.exerciseByName(name.trim())

    suspend fun rememberExercise(name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty() && dao.exerciseIdByName(clean) == null) {
            dao.insertExercise(
                ExerciseEntity(
                    name = clean,
                    isCustom = true,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    // ---------- 訓練紀錄 ----------

    fun observeSummaries(): Flow<List<SessionSummary>> = dao.observeSummaries()

    fun observeSessionUi(id: Long): Flow<SessionUi?> =
        combine(
            dao.observeSession(id),
            dao.observeEntries(id),
            dao.observeSets(id)
        ) { session, entries, sets ->
            session?.let {
                SessionUi(
                    session = it,
                    entries = entries.map { entry ->
                        EntryUi(
                            entry = entry,
                            sets = sets.filter { set -> set.entryId == entry.id }
                                .sortedBy { set -> set.setIndex }
                        )
                    }
                )
            }
        }

    /**
     * 從菜單建立訓練：把當下的菜單內容「快照」進紀錄，
     * 之後修改菜單不會影響已建立的歷史。
     */
    suspend fun createSessionFromMenu(menuId: String, epochDay: Long): Long {
        val menu = dao.menuById(menuId) ?: return -1L
        val rows = dao.menuExerciseRows(menuId)
        val sessionId = dao.insertSession(
            SessionEntity(
                dateEpochDay = epochDay,
                menuId = menu.id,
                menuName = menu.name
            )
        )
        rows.forEach { row ->
            dao.insertEntry(
                EntryEntity(
                    sessionId = sessionId,
                    name = row.name,
                    equipment = row.equipment,
                    targetParts = row.parts,
                    exerciseId = row.exerciseId,
                    orderIndex = row.orderIndex,
                    targetSets = row.targetSets,
                    targetReps = row.targetReps,
                    restSec = row.restSec,
                    videoUrl = row.videoUrl,
                    guideAsset = row.guideId
                )
            )
        }
        return sessionId
    }

    suspend fun createEmptySession(epochDay: Long): Long =
        dao.insertSession(SessionEntity(dateEpochDay = epochDay))

    suspend fun updateDate(session: SessionEntity, epochDay: Long) =
        dao.updateSession(session.copy(dateEpochDay = epochDay))

    suspend fun addEntry(
        sessionId: Long,
        name: String,
        equipment: String? = null,
        targetParts: String? = null,
        exerciseId: Long? = null
    ) {
        val count = dao.entriesOf(sessionId).size
        dao.insertEntry(
            EntryEntity(
                sessionId = sessionId,
                name = name,
                equipment = equipment,
                targetParts = targetParts,
                exerciseId = exerciseId,
                orderIndex = count
            )
        )
    }

    suspend fun updateEntry(entry: EntryEntity) = dao.updateEntry(entry)

    suspend fun deleteEntry(entry: EntryEntity) = dao.deleteEntry(entry)

    suspend fun deleteSession(session: SessionEntity) = dao.deleteSession(session)

    suspend fun addSet(
        entryId: Long,
        weight: Double? = null,
        reps: Int? = null,
        duration: Int? = null,
        bodyweight: Boolean = false
    ) {
        val existing = dao.setsOf(entryId)
        val nextIndex = (existing.maxOfOrNull { it.setIndex } ?: 0) + 1
        dao.insertSet(
            SetEntity(
                entryId = entryId,
                setIndex = nextIndex,
                weightKg = weight,
                reps = reps,
                durationSec = duration,
                isBodyweight = bodyweight
            )
        )
    }

    suspend fun updateLastSet(
        entryId: Long,
        weight: Double? = null,
        reps: Int? = null,
        duration: Int? = null
    ) {
        val sets = dao.setsOf(entryId)
        if (sets.isEmpty()) {
            addSet(entryId, weight, reps, duration)
            return
        }
        val last = sets.last()
        dao.updateSet(
            last.copy(
                weightKg = weight ?: last.weightKg,
                reps = reps ?: last.reps,
                durationSec = duration ?: last.durationSec
            )
        )
    }

    suspend fun setTotalSets(entryId: Long, target: Int) {
        val sets = dao.setsOf(entryId)
        when {
            target > sets.size -> {
                var index = sets.maxOfOrNull { it.setIndex } ?: 0
                repeat(target - sets.size) {
                    dao.insertSet(SetEntity(entryId = entryId, setIndex = ++index))
                }
            }
            target < sets.size -> {
                sets.sortedByDescending { it.setIndex }
                    .take(sets.size - target)
                    .forEach { dao.deleteSet(it) }
            }
        }
    }

    suspend fun duplicateLastSet(entryId: Long) {
        val sets = dao.setsOf(entryId)
        val last = sets.lastOrNull()
        dao.insertSet(
            SetEntity(
                entryId = entryId,
                setIndex = (last?.setIndex ?: 0) + 1,
                weightKg = last?.weightKg,
                reps = last?.reps,
                durationSec = last?.durationSec,
                isBodyweight = last?.isBodyweight ?: false
            )
        )
    }

    suspend fun updateSet(
        set: SetEntity,
        weight: Double?,
        reps: Int?,
        duration: Int?,
        bodyweight: Boolean
    ) {
        dao.updateSet(
            set.copy(
                weightKg = weight,
                reps = reps,
                durationSec = duration,
                isBodyweight = bodyweight
            )
        )
    }

    suspend fun deleteSet(set: SetEntity) = dao.deleteSet(set)
}
