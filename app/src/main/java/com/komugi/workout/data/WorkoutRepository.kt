package com.komugi.workout.data

import android.content.Context
import androidx.room.withTransaction
import java.util.UUID
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

data class NewEntryInput(
    val name: String,
    val equipment: String? = null,
    val targetParts: String? = null,
    val exerciseId: Long? = null,
    val targetSets: String? = null,
    val targetReps: String? = null,
    val restSec: String? = null,
    val supersetKey: String? = null
)

data class NewMenuExerciseInput(
    val exerciseId: Long,
    val equipment: String? = null,
    val targetSets: String? = null,
    val targetReps: String? = null,
    val restSec: String? = null,
    val supersetKey: String? = null
)

class WorkoutRepository(
    private val db: AppDatabase,
    private val context: Context
) {
    companion object {
        /** 菜單種子版本：修改 assets/menus.json 後 += 1，已安裝的 App 會自動重建菜單。 */
        private const val SEED_VERSION = 11
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

    private val extraSeed: List<ExtraExercise> by lazy {
        context.assets.open("exercises_extra.json").bufferedReader().use {
            json.decodeFromString<List<ExtraExercise>>(it.readText())
        }
    }

    private val guideSeed: List<ExerciseGuideSeed> by lazy {
        context.assets.open("exercise_guides.json").bufferedReader().use {
            json.decodeFromString<List<ExerciseGuideSeed>>(it.readText())
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
            // 保留自訂菜單；只重建內建菜單與關聯
            dao.clearSeededMenuExercises()
            dao.clearSeededMenus()

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
                                equipment = ex.equipment,
                                videoUrl = ex.video,
                                guideId = ex.guide,
                                isCustom = false,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        // 菜單主檔更新時同步部位／器材／影片／說明（自訂動作若不在菜單中不受影響）
                        if (existing.parts != ex.parts ||
                            existing.equipment != ex.equipment ||
                            existing.videoUrl != ex.video ||
                            existing.guideId != ex.guide ||
                            existing.isCustom
                        ) {
                            dao.updateExercise(
                                existing.copy(
                                    parts = ex.parts ?: existing.parts,
                                    equipment = ex.equipment ?: existing.equipment,
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

            // 動作庫補充動作（不屬於任何菜單）
            extraSeed.forEach { extra ->
                val existing = dao.exerciseByName(extra.name)
                if (existing == null) {
                    dao.insertExercise(
                        ExerciseEntity(
                            name = extra.name,
                            parts = extra.parts,
                            equipment = extra.equipment,
                            location = extra.location,
                            movementType = extra.movementType,
                            difficulty = extra.difficulty,
                            instructions = extra.instructions,
                            warning = extra.warning,
                            isCustom = false,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    dao.updateExercise(
                        existing.copy(
                            parts = if (existing.isCustom) existing.parts else (extra.parts ?: existing.parts),
                            equipment = if (existing.isCustom) existing.equipment else (extra.equipment ?: existing.equipment),
                            location = if (existing.isCustom) existing.location else (extra.location ?: existing.location),
                            movementType = if (existing.isCustom) existing.movementType else (extra.movementType ?: existing.movementType),
                            difficulty = if (existing.isCustom) existing.difficulty else (extra.difficulty ?: existing.difficulty),
                            instructions = if (existing.isCustom) existing.instructions else (extra.instructions ?: existing.instructions),
                            warning = if (existing.isCustom) existing.warning else (extra.warning ?: existing.warning)
                        )
                    )
                }
            }

            // 既有動作的步驟與注意事項（只更新，不新增）
            guideSeed.forEach { guide ->
                val existing = dao.exerciseByName(guide.name) ?: return@forEach
                if (existing.isCustom) return@forEach
                dao.updateExercise(
                    existing.copy(
                        instructions = guide.instructions ?: existing.instructions,
                        warning = guide.warning ?: existing.warning
                    )
                )
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

    suspend fun rememberExercise(
        name: String,
        parts: String? = null,
        equipment: String? = null
    ) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val existing = dao.exerciseByName(clean)
        if (existing == null) {
            dao.insertExercise(
                ExerciseEntity(
                    name = clean,
                    parts = parts,
                    equipment = equipment,
                    isCustom = true,
                    createdAt = System.currentTimeMillis()
                )
            )
        } else {
            val newParts = existing.parts ?: parts
            val newEquipment = existing.equipment ?: equipment
            if (newParts != existing.parts || newEquipment != existing.equipment) {
                dao.updateExercise(
                    existing.copy(parts = newParts, equipment = newEquipment)
                )
            }
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

    suspend fun entryById(id: Long): EntryEntity? = dao.entryById(id)

    /**
     * 某個動作的歷史組數：以 exerciseId 為主，並用名稱補齊舊資料（舊版沒有 exerciseId）。
     */
    fun observeExerciseSets(exerciseId: Long?, name: String): Flow<List<ExerciseSetRow>> =
        dao.observeExerciseSets(exerciseId ?: -1L, name)

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
                    guideAsset = row.guideId,
                    supersetKey = row.supersetKey
                )
            )
        }
        return sessionId
    }

    /**
     * 把目前設定的動作、組次、休息與超級組存成自訂菜單。
     */
    suspend fun createCustomMenu(
        name: String,
        subtitle: String? = null,
        items: List<NewMenuExerciseInput>
    ): String {
        val menuId = "custom-${UUID.randomUUID()}"
        db.withTransaction {
            dao.insertMenu(
                MenuEntity(
                    id = menuId,
                    name = name.trim(),
                    subtitle = subtitle,
                    orderIndex = dao.maxMenuOrder() + 1,
                    isCustom = true
                )
            )
            items.forEachIndexed { index, item ->
                dao.insertMenuExercise(
                    MenuExerciseEntity(
                        menuId = menuId,
                        exerciseId = item.exerciseId,
                        orderIndex = index,
                        equipment = item.equipment,
                        targetSets = item.targetSets,
                        targetReps = item.targetReps,
                        restSec = item.restSec,
                        supersetKey = item.supersetKey
                    )
                )
            }
        }
        return menuId
    }

    suspend fun deleteMenu(menu: MenuEntity) = dao.deleteMenu(menu)

    suspend fun createEmptySession(epochDay: Long): Long =
        dao.insertSession(SessionEntity(dateEpochDay = epochDay))

    /**
     * 把整筆訓練（動作＋組數）複製到指定日期。
     */
    suspend fun copySessionToDate(sessionId: Long, targetEpochDay: Long): Long {
        val source = dao.sessionById(sessionId) ?: return -1L
        val sourceEntries = dao.entriesOf(sessionId)
        return db.withTransaction {
            val newSessionId = dao.insertSession(
                source.copy(id = 0, dateEpochDay = targetEpochDay)
            )
            sourceEntries.forEach { entry ->
                val newEntryId = dao.insertEntry(entry.copy(id = 0, sessionId = newSessionId))
                dao.setsOf(entry.id).forEach { set ->
                    dao.insertSet(set.copy(id = 0, entryId = newEntryId))
                }
            }
            newSessionId
        }
    }

    suspend fun updateDate(session: SessionEntity, epochDay: Long) =
        dao.updateSession(session.copy(dateEpochDay = epochDay))

    suspend fun addEntry(
        sessionId: Long,
        name: String,
        equipment: String? = null,
        targetParts: String? = null,
        exerciseId: Long? = null,
        targetSets: String? = null,
        targetReps: String? = null,
        restSec: String? = null,
        supersetKey: String? = null
    ) {
        val count = dao.entriesOf(sessionId).size
        dao.insertEntry(
            EntryEntity(
                sessionId = sessionId,
                name = name,
                equipment = equipment,
                targetParts = targetParts,
                exerciseId = exerciseId,
                orderIndex = count,
                targetSets = targetSets,
                targetReps = targetReps,
                restSec = restSec,
                supersetKey = supersetKey
            )
        )
    }

    /**
     * 批次加入動作（多選後一次寫入），保留選擇順序。
     */
    suspend fun addEntries(sessionId: Long, items: List<NewEntryInput>) {
        if (items.isEmpty()) return
        db.withTransaction {
            var order = dao.entriesOf(sessionId).size
            items.forEach { item ->
                dao.insertEntry(
                    EntryEntity(
                        sessionId = sessionId,
                        name = item.name,
                        equipment = item.equipment,
                        targetParts = item.targetParts,
                        exerciseId = item.exerciseId,
                        orderIndex = order++,
                        targetSets = item.targetSets,
                        targetReps = item.targetReps,
                        restSec = item.restSec,
                        supersetKey = item.supersetKey
                    )
                )
            }
        }
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
