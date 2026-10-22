package com.leolu.beanlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BeanDao {
    @Query(
        """
        SELECT beans.*, r.name AS roasterName, r.coverPath AS coverPath,
            EXISTS(SELECT 1 FROM adjustments a WHERE a.beanId = beans.id) AS canUndo
        FROM beans JOIN roasters r ON r.id = beans.roasterId
        ORDER BY beans.lastUsedAt DESC
        """,
    )
    fun observeItems(): Flow<List<BeanItem>>

    @Query(
        """
        SELECT beans.*, r.name AS roasterName, r.coverPath AS coverPath,
            EXISTS(SELECT 1 FROM adjustments a WHERE a.beanId = beans.id) AS canUndo
        FROM beans JOIN roasters r ON r.id = beans.roasterId
        ORDER BY beans.lastUsedAt DESC
        """,
    )
    suspend fun items(): List<BeanItem>

    @Query("SELECT * FROM roasters ORDER BY name")
    fun observeRoasters(): Flow<List<Roaster>>

    @Query("SELECT * FROM roasters WHERE name = :name LIMIT 1")
    suspend fun roasterByName(name: String): Roaster?

    @Insert
    suspend fun insertRoaster(roaster: Roaster): Long

    @Query("UPDATE roasters SET coverPath = :coverPath WHERE id = :id")
    suspend fun setRoasterCover(id: Long, coverPath: String?)

    @Query("SELECT * FROM beans WHERE id = :id")
    suspend fun bean(id: Long): Bean?

    @Insert
    suspend fun insertBean(bean: Bean): Long

    @Update
    suspend fun updateBean(bean: Bean)

    @Query("DELETE FROM adjustments WHERE beanId = :id")
    suspend fun deleteAdjustmentsFor(id: Long)

    @Query("DELETE FROM beans WHERE id = :id")
    suspend fun deleteBeanRow(id: Long)

    @Query("SELECT grams FROM beans WHERE id = :id")
    suspend fun grams(id: Long): Int?

    @Query("UPDATE beans SET grams = :grams, lastUsedAt = :now WHERE id = :id")
    suspend fun setGrams(id: Long, grams: Int, now: Long)

    @Insert
    suspend fun insertAdjustment(adjustment: Adjustment)

    @Query("SELECT * FROM adjustments WHERE beanId = :beanId ORDER BY id DESC LIMIT 1")
    suspend fun lastAdjustment(beanId: Long): Adjustment?

    @Query("DELETE FROM adjustments WHERE id = :id")
    suspend fun deleteAdjustment(id: Long)

    @Query(
        """
        DELETE FROM adjustments WHERE beanId = :beanId AND id NOT IN
            (SELECT id FROM adjustments WHERE beanId = :beanId ORDER BY id DESC LIMIT :keep)
        """,
    )
    suspend fun pruneAdjustments(beanId: Long, keep: Int)

    /** Changes the remaining grams, never going below zero, and records it for undo. */
    @Transaction
    suspend fun adjust(beanId: Long, delta: Int, now: Long) {
        val current = grams(beanId) ?: return
        val updated = (current + delta).coerceAtLeast(0)
        if (updated == current) return
        setGrams(beanId, updated, now)
        recordAdjustment(beanId, updated - current, now)
    }

    @Transaction
    suspend fun undo(beanId: Long, now: Long) {
        val last = lastAdjustment(beanId) ?: return
        val current = grams(beanId) ?: return
        setGrams(beanId, (current - last.delta).coerceAtLeast(0), now)
        deleteAdjustment(last.id)
    }

    suspend fun recordAdjustment(beanId: Long, delta: Int, now: Long) {
        insertAdjustment(Adjustment(beanId = beanId, delta = delta, createdAt = now))
        pruneAdjustments(beanId, UNDO_DEPTH)
    }

    @Transaction
    suspend fun deleteBean(id: Long) {
        deleteAdjustmentsFor(id)
        deleteBeanRow(id)
    }

    companion object {
        const val UNDO_DEPTH = 20
    }
}
