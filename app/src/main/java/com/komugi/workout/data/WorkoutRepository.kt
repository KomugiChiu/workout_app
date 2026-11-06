package com.komugi.workout.data

import android.content.Context
import androidx.room.withTransaction
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
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
    val supersetKey: String? = null,
    val slotKey: String? = null,
    val role: String? = null,
    val laterality: String? = null,
    val pattern: String? = null
)

@Serializable
data class MenuExerciseLastUsed(
    val menuExerciseId: Long,
    val exerciseId: Long,
    val slotKey: String? = null
)

/**
 * 把記住的上次動作套用到目前的課表列。
 *
 * 優先以 slotKey 配對（種子重建後 menuExerciseId 會變，slotKey 不變）；
 * 沒有 slotKey 的舊資料才用 menuExerciseId 配對，對不上的直接忽略。
 */
fun resolveExerciseOverrides(
    rows: List<MenuExerciseRow>,
    overrides: List<MenuExerciseLastUsed>,
    exerciseById: (Long) -> ExerciseEntity?
): List<MenuExerciseRow> {
    if (overrides.isEmpty()) return rows
    val bySlot = overrides
        .filter { !it.slotKey.isNullOrBlank() }
        .associate { it.slotKey as String to it.exerciseId }
    val byRowId = overrides
        .filter { it.slotKey.isNullOrBlank() }
        .associate { it.menuExerciseId to it.exerciseId }
    return rows.map { row ->
        val exerciseId = if (row.slotKey != null) {
            bySlot[row.slotKey] ?: byRowId[row.menuExerciseId]
        } else {
            byRowId[row.menuExerciseId]
        } ?: return@map row
        if (exerciseId == row.exerciseId) return@map row
        val exercise = exerciseById(exerciseId) ?: return@map row
        row.copy(
            exerciseId = exercise.id,
            name = exercise.name,
            equipment = exercise.equipment,
            parts = exercise.parts,
            videoUrl = exercise.videoUrl,
            guideId = exercise.guideId
        )
    }
}

/**
 * 計算目前列與模板的差異，只記住有更換的槽位。
 * 配對以 slotKey 為主，避免種子重建後記憶失效。
 */
fun diffExerciseOverrides(
    rows: List<MenuExerciseRow>,
    templateRows: List<MenuExerciseRow>
): List<MenuExerciseLastUsed> {
    val templateBySlot = templateRows
        .filter { it.slotKey != null }
        .associate { (it.slotKey as String) to it.exerciseId }
    val templateByRowId = templateRows.associate { it.menuExerciseId to it.exerciseId }
    return rows.mapNotNull { row ->
        val templateExerciseId = if (row.slotKey != null) {
            templateBySlot[row.slotKey] ?: templateByRowId[row.menuExerciseId]
        } else {
            templateByRowId[row.menuExerciseId]
        }
        if (templateExerciseId == null || templateExerciseId == row.exerciseId) {
            null
        } else {
            MenuExerciseLastUsed(
                menuExerciseId = row.menuExerciseId,
                exerciseId = row.exerciseId,
                slotKey = row.slotKey
            )
        }
    }
}

class WorkoutRepository(
    private val db: AppDatabase,
    private val context: Context
) {
    companion object {
        /** 菜單種子版本：修改 assets/menus.json 後 += 1，已安裝的 App 會自動重建菜單。 */
        private const val SEED_VERSION = 21
    }

    private val dao = db.dao()
    private val json = Json { ignoreUnknownKeys = true }
    private val backupJson = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

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
                        orderIndex = menuIndex,
                        isTemplate = menu.template,
                        isReference = menu.reference
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
                            restSec = ex.rest,
                            supersetKey = ex.superset,
                            slotKey = ex.slot,
                            role = ex.role,
                            laterality = ex.laterality,
                            pattern = ex.pattern
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

    // ---------- 備份 / 還原 ----------

    suspend fun exportBackupJson(): String {
        val backup = BackupFile(
            exportedAt = System.currentTimeMillis(),
            exercises = dao.allExercises().map { it.toBackup() },
            menus = dao.allMenus().map { it.toBackup() },
            menuExercises = dao.allMenuExercises().map { it.toBackup() },
            sessions = dao.allSessions().map { it.toBackup() },
            entries = dao.allEntries().map { it.toBackup() },
            sets = dao.allSets().map { it.toBackup() },
            bodyMetrics = dao.allBodyMetrics().map { it.toBackup() }
        )
        return backupJson.encodeToString(backup)
    }

    /**
     * 以備份內容完整取代目前資料；失敗時交易會 rollback。
     */
    suspend fun importBackupJson(text: String) {
        val backup = backupJson.decodeFromString<BackupFile>(text)
        backup.requireImportable()
        db.withTransaction {
            dao.clearAllSets()
            dao.clearAllEntries()
            dao.clearAllSessions()
            dao.clearAllMenuExercises()
            dao.clearAllMenus()
            dao.clearAllExercises()
            dao.clearAllBodyMetrics()

            if (backup.exercises.isNotEmpty()) {
                dao.insertExercises(backup.exercises.map { it.toEntity() })
            }
            if (backup.menus.isNotEmpty()) {
                // 內建書本課表的參考旗標以種子為準，避免舊備份把它們寫回菜單頁
                val seedReference = menuSeed.associate { it.id to it.reference }
                dao.insertMenus(
                    backup.menus.map { menu ->
                        menu.toEntity().copy(
                            isReference = seedReference[menu.id] ?: menu.isReference
                        )
                    }
                )
            }
            if (backup.menuExercises.isNotEmpty()) {
                dao.insertMenuExercises(backup.menuExercises.map { it.toEntity() })
            }
            if (backup.sessions.isNotEmpty()) {
                dao.insertSessions(backup.sessions.map { it.toEntity() })
            }
            if (backup.entries.isNotEmpty()) {
                dao.insertEntries(backup.entries.map { it.toEntity() })
            }
            if (backup.sets.isNotEmpty()) {
                dao.insertSets(backup.sets.map { it.toEntity() })
            }
            if (backup.bodyMetrics.isNotEmpty()) {
                dao.insertBodyMetrics(backup.bodyMetrics.map { it.toEntity() })
            }

            // 已還原完整資料，避免下次啟動又被內建種子覆蓋
            dao.upsertMeta(AppMetaEntity("seed_version", SEED_VERSION.toString()))
            dao.upsertMeta(AppMetaEntity("body_seed_v1", "1"))
        }
    }

    // ---------- 身體數據 ----------

    fun observeBodyMetrics(): Flow<List<BodyMetricEntity>> = dao.observeBodyMetrics()

    suspend fun saveBodyMetric(
        id: Long,
        dateEpochDay: Long,
        weightKg: Double?,
        bodyFatPercent: Double?
    ) {
        val existing = dao.bodyMetricByDate(dateEpochDay)
        if (existing != null && existing.id != id) {
            dao.updateBodyMetric(
                existing.copy(
                    weightKg = weightKg,
                    bodyFatPercent = bodyFatPercent
                )
            )
            if (id != 0L) {
                dao.bodyMetricById(id)?.let { dao.deleteBodyMetric(it) }
            }
        } else {
            dao.insertBodyMetric(
                BodyMetricEntity(
                    id = id,
                    dateEpochDay = dateEpochDay,
                    weightKg = weightKg,
                    bodyFatPercent = bodyFatPercent
                )
            )
        }
    }

    suspend fun deleteBodyMetric(metric: BodyMetricEntity) = dao.deleteBodyMetric(metric)

    /**
     * 第一次加入身體數據功能時，建立使用者的第一筆資料。
     */
    suspend fun ensureBodyMetricSeed() {
        if (dao.metaValue("body_seed_v1") == "1") return
        if (dao.bodyMetricCount() == 0) {
            dao.insertBodyMetric(
                BodyMetricEntity(
                    dateEpochDay = LocalDate.now().toEpochDay(),
                    weightKg = 70.1,
                    bodyFatPercent = 21.3
                )
            )
        }
        dao.upsertMeta(AppMetaEntity("body_seed_v1", "1"))
    }

    // ---------- 菜單 ----------

    fun observeMenuSummaries(): Flow<List<MenuSummary>> = dao.observeMenuSummaries()

    fun observeMenu(id: String): Flow<MenuEntity?> = dao.observeMenu(id)

    fun observeMenuExercises(id: String): Flow<List<MenuExerciseRow>> =
        dao.observeMenuExerciseRows(id)

    /**
     * 記住上次使用的器材分類，讓下次進訓練計畫時自動帶入。
     */
    suspend fun equipmentProfile(): Set<String> =
        dao.metaValue("equipment_profile")
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            ?: emptySet()

    suspend fun saveEquipmentProfile(categories: Set<String>) {
        dao.upsertMeta(
            AppMetaEntity(
                key = "equipment_profile",
                value = categories.sorted().joinToString(",")
            )
        )
    }

    /**
     * 每個訓練計畫記住上次使用的動作（以 slotKey 配對，種子重建也不會失效）。
     * 只儲存差異；回到原模板時會寫入空清單。
     */
    suspend fun lastUsedMenuExercises(
        menuId: String,
        namespace: String? = null
    ): List<MenuExerciseLastUsed> {
        val key = "menu_last_used_$menuId" + (namespace?.let { "-$it" } ?: "")
        val raw = dao.metaValue(key) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<MenuExerciseLastUsed>>(raw)
        }.getOrNull() ?: emptyList()
    }

    suspend fun saveLastUsedMenuExercises(
        menuId: String,
        rows: List<MenuExerciseRow>,
        templateRows: List<MenuExerciseRow>,
        namespace: String? = null
    ) {
        val key = "menu_last_used_$menuId" + (namespace?.let { "-$it" } ?: "")
        dao.upsertMeta(
            AppMetaEntity(
                key = key,
                value = json.encodeToString(diffExerciseOverrides(rows, templateRows))
            )
        )
    }

    /**
     * 可調三天循環上次選的 A/B 天（全域記住，重開維持）。
     */
    suspend fun cycleFlexDay(): String =
        dao.metaValue("cycle_flex_day") ?: "A"

    suspend fun saveCycleFlexDay(day: String) {
        dao.upsertMeta(
            AppMetaEntity(
                key = "cycle_flex_day",
                value = day
            )
        )
    }

    /**
     * 可調重訓計畫上次選的 A/B 天（全域記住，重開維持）。
     */
    suspend fun planFlexDay(): String =
        dao.metaValue("plan_flex_day") ?: "A"

    suspend fun savePlanFlexDay(day: String) {
        dao.upsertMeta(
            AppMetaEntity(
                key = "plan_flex_day",
                value = day
            )
        )
    }

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
     * 從「已調整的課表草稿」建立訓練：不讀取原始菜單，
     * 直接把畫面上的動作、組次、休息與配對快照進紀錄。
     */
    suspend fun createSessionFromAdjustedMenu(
        menuId: String,
        epochDay: Long,
        rows: List<MenuExerciseRow>,
        equipmentNote: String? = null,
        adjusted: Boolean = false
    ): Long {
        val menu = dao.menuById(menuId) ?: return -1L
        val sessionId = dao.insertSession(
            SessionEntity(
                dateEpochDay = epochDay,
                menuId = menu.id,
                menuName = if (adjusted) "${menu.name}（調整）" else menu.name,
                note = equipmentNote
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { "今日器材：$it" }
            )
        )
        rows.forEachIndexed { index, row ->
            dao.insertEntry(
                EntryEntity(
                    sessionId = sessionId,
                    name = row.name,
                    equipment = row.equipment,
                    targetParts = row.parts,
                    exerciseId = row.exerciseId,
                    orderIndex = index,
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
                        supersetKey = item.supersetKey,
                        slotKey = item.slotKey,
                        role = item.role,
                        laterality = item.laterality,
                        pattern = item.pattern
                    )
                )
            }
        }
        return menuId
    }

    suspend fun deleteMenu(menu: MenuEntity) = dao.deleteMenu(menu)

    /**
     * 把整筆訓練紀錄（動作、組次、休息、超級組）存成自訂菜單。
     */
    suspend fun createMenuFromSession(sessionId: Long, name: String): String? {
        val session = dao.sessionById(sessionId) ?: return null
        val entries = dao.entriesOf(sessionId)
        if (entries.isEmpty()) return null
        val items = mutableListOf<NewMenuExerciseInput>()
        for (entry in entries) {
            val exerciseId = entry.exerciseId ?: run {
                rememberExercise(entry.name, entry.targetParts, entry.equipment)
                dao.exerciseByName(entry.name)?.id
            } ?: continue
            items.add(
                NewMenuExerciseInput(
                    exerciseId = exerciseId,
                    equipment = entry.equipment,
                    targetSets = entry.targetSets,
                    targetReps = entry.targetReps,
                    restSec = entry.restSec,
                    supersetKey = entry.supersetKey
                )
            )
        }
        if (items.isEmpty()) return null
        return createCustomMenu(name, session.note, items)
    }

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
                source.copy(
                    id = 0,
                    dateEpochDay = targetEpochDay,
                    startedAt = null,
                    finishedAt = null
                )
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

    suspend fun reorderEntries(sessionId: Long, orderedEntryIds: List<Long>) {
        db.withTransaction {
            orderedEntryIds.forEachIndexed { index, id ->
                dao.updateEntryOrder(id, index)
            }
        }
    }

    suspend fun reorderMenuExercises(menuId: String, orderedRowIds: List<Long>) {
        db.withTransaction {
            orderedRowIds.forEachIndexed { index, id ->
                dao.updateMenuExerciseOrder(id, index)
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
