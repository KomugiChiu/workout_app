package com.komugi.workout.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SessionEntity::class,
        EntryEntity::class,
        SetEntity::class,
        ExerciseEntity::class,
        MenuEntity::class,
        MenuExerciseEntity::class,
        AppMetaEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sets ADD COLUMN isBodyweight INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS custom_exercises (" +
                        "name TEXT NOT NULL PRIMARY KEY, " +
                        "addedAt INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entries ADD COLUMN targetParts TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `exercises` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`parts` TEXT, " +
                        "`videoUrl` TEXT, " +
                        "`guideId` TEXT, " +
                        "`isCustom` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_exercises_name` " +
                        "ON `exercises` (`name`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `menus` (" +
                        "`id` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`subtitle` TEXT, " +
                        "`note` TEXT, " +
                        "`orderIndex` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `menu_exercises` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`menuId` TEXT NOT NULL, " +
                        "`exerciseId` INTEGER NOT NULL, " +
                        "`orderIndex` INTEGER NOT NULL, " +
                        "`equipment` TEXT, " +
                        "`targetSets` TEXT, " +
                        "`targetReps` TEXT, " +
                        "`restSec` TEXT, " +
                        "FOREIGN KEY(`menuId`) REFERENCES `menus`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_menu_exercises_menuId` " +
                        "ON `menu_exercises` (`menuId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_menu_exercises_exerciseId` " +
                        "ON `menu_exercises` (`exerciseId`)"
                )
                db.execSQL("ALTER TABLE `entries` ADD COLUMN `exerciseId` INTEGER")
                db.execSQL(
                    "INSERT OR IGNORE INTO `exercises` (`name`, `isCustom`, `createdAt`) " +
                        "SELECT `name`, 1, `addedAt` FROM `custom_exercises`"
                )
                db.execSQL("DROP TABLE IF EXISTS `custom_exercises`")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_meta` (" +
                        "`key` TEXT NOT NULL, " +
                        "`value` TEXT NOT NULL, " +
                        "PRIMARY KEY(`key`))"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `entries` ADD COLUMN `supersetKey` TEXT")
                db.execSQL("ALTER TABLE `exercises` ADD COLUMN `equipment` TEXT")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `menus` ADD COLUMN `isCustom` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `menu_exercises` ADD COLUMN `supersetKey` TEXT"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "komugi-workout.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
