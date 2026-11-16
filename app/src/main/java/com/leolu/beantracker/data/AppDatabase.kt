package com.leolu.beantracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Roaster::class, Bean::class, Adjustment::class], version = 4)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun beanDao(): BeanDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "beantracker.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE beans ADD COLUMN roastLevel INTEGER")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE beans ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Variety became a list: split values typed as "SL28 / SL34" into separate entries. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                var value = "variety"
                listOf(" / ", ", ", "/", ",", "、", "，").forEach { sep ->
                    value = "replace($value, '$sep', char(31))"
                }
                db.execSQL("UPDATE beans SET variety = $value")
            }
        }
    }
}
