package com.leolu.beanlog.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A roaster owns the cover image, so every bean from the same roaster shares it. */
@Entity(tableName = "roasters", indices = [Index(value = ["name"], unique = true)])
data class Roaster(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val coverPath: String?,
)

@Entity(
    tableName = "beans",
    foreignKeys = [ForeignKey(entity = Roaster::class, parentColumns = ["id"], childColumns = ["roasterId"])],
    indices = [Index("roasterId")],
)
data class Bean(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roasterId: Long,
    val name: String,
    /** Several entries for blends. */
    val countries: List<String>,
    val region: String,
    val farm: String,
    val variety: String,
    /** Several entries for blends. */
    val processes: List<String>,
    val grams: Int,
    /** LocalDate epoch day, or null when unknown. */
    val roastDate: Long?,
    val createdAt: Long,
    val lastUsedAt: Long,
)

/** One gram change, kept so the undo button can revert it. */
@Entity(
    tableName = "adjustments",
    foreignKeys = [
        ForeignKey(
            entity = Bean::class,
            parentColumns = ["id"],
            childColumns = ["beanId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("beanId")],
)
data class Adjustment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val beanId: Long,
    /** The change actually applied after clamping at zero. */
    val delta: Int,
    val createdAt: Long,
)

data class BeanItem(
    @Embedded val bean: Bean,
    val roasterName: String,
    val coverPath: String?,
    val canUndo: Boolean,
)
