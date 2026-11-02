package com.komugi.workout.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    private val dao: WorkoutDao,
    private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    val menus: List<Menu> by lazy {
        context.assets.open("menus.json").bufferedReader().use {
            json.decodeFromString<List<Menu>>(it.readText())
        }
    }

    val guides: List<GuideRef> by lazy {
        context.assets.open("guides.json").bufferedReader().use {
            json.decodeFromString<GuideIndex>(it.readText()).guides
        }
    }

    private val menuNames: List<String> by lazy {
        menus.flatMap { it.exercises }.map { it.name }.distinct()
    }

    fun menuById(id: String?): Menu? = menus.firstOrNull { it.id == id }

    fun guideById(id: String?): GuideRef? = guides.firstOrNull { it.id == id }

    fun readGuide(assetPath: String): String =
        context.assets.open(assetPath).bufferedReader().use { it.readText() }

    fun menuExerciseNames(): List<String> = menuNames.sorted()

    fun exercisePartsMap(): Map<String, String> =
        menus.flatMap { it.exercises }
            .mapNotNull { ex -> ex.parts?.let { ex.name to it } }
            .toMap()

    fun exerciseLibrary(): Flow<List<String>> =
        dao.observeCustomExercises().map { custom ->
            (menuNames + custom.map { it.name }).distinct().sorted()
        }

    suspend fun rememberExercise(name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty()) {
            dao.insertCustomExercise(
                CustomExerciseEntity(name = clean, addedAt = System.currentTimeMillis())
            )
        }
    }

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

    suspend fun createSessionFromMenu(menu: Menu, epochDay: Long): Long {
        val sessionId = dao.insertSession(
            SessionEntity(dateEpochDay = epochDay, menuId = menu.id, menuName = menu.name)
        )
        menu.exercises.forEachIndexed { index, ex ->
            dao.insertEntry(
                EntryEntity(
                    sessionId = sessionId,
                    name = ex.name,
                    equipment = ex.equipment,
                    targetParts = ex.parts,
                    orderIndex = index,
                    targetSets = ex.sets,
                    targetReps = ex.reps,
                    restSec = ex.rest,
                    videoUrl = ex.video,
                    guideAsset = ex.guide
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
        targetParts: String? = null
    ) {
        val count = dao.entriesOf(sessionId).size
        dao.insertEntry(
            EntryEntity(
                sessionId = sessionId,
                name = name,
                equipment = equipment,
                targetParts = targetParts,
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
