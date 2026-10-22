package com.leolu.beantracker.data

import androidx.room.withTransaction

data class BeanInput(
    val id: Long,
    val roasterName: String,
    val name: String,
    val countries: List<String>,
    val region: String,
    val farm: String,
    val variety: String,
    val processes: List<String>,
    val roastLevel: Int?,
    val note: String,
    val grams: Int,
    val roastDate: Long?,
)

class BeanRepository(private val db: AppDatabase, val covers: CoverStore) {
    private val dao = db.beanDao()

    val items = dao.observeItems()
    val roasters = dao.observeRoasters()

    suspend fun currentItems(): List<BeanItem> = dao.items()

    suspend fun roasterByName(name: String): Roaster? = dao.roasterByName(name.trim())

    suspend fun adjust(beanId: Long, delta: Int) = dao.adjust(beanId, delta, System.currentTimeMillis())

    suspend fun undo(beanId: Long) = dao.undo(beanId, System.currentTimeMillis())

    suspend fun delete(beanId: Long) = dao.deleteBean(beanId)

    suspend fun bean(id: Long): Bean? = dao.bean(id)

    /**
     * Saves the bean, creating its roaster when the name is new. A [newCoverPath]
     * replaces the roaster's cover, which changes it for all of that roaster's beans.
     */
    suspend fun save(input: BeanInput, newCoverPath: String?) {
        val now = System.currentTimeMillis()
        val replacedCover = db.withTransaction {
            var replaced: String? = null
            val existing = dao.roasterByName(input.roasterName)
            val roasterId = if (existing == null) {
                dao.insertRoaster(Roaster(name = input.roasterName, coverPath = newCoverPath))
            } else {
                if (newCoverPath != null && newCoverPath != existing.coverPath) {
                    dao.setRoasterCover(existing.id, newCoverPath)
                    replaced = existing.coverPath
                }
                existing.id
            }

            val previous = if (input.id != 0L) dao.bean(input.id) else null
            val bean = Bean(
                id = previous?.id ?: 0,
                roasterId = roasterId,
                name = input.name,
                countries = input.countries,
                region = input.region,
                farm = input.farm,
                variety = input.variety,
                processes = input.processes,
                roastLevel = input.roastLevel,
                note = input.note,
                grams = input.grams.coerceAtLeast(0),
                roastDate = input.roastDate,
                createdAt = previous?.createdAt ?: now,
                lastUsedAt = now,
            )
            if (previous == null) {
                dao.insertBean(bean)
            } else {
                dao.updateBean(bean)
                // Editing the grams by hand can also be undone from the list.
                if (bean.grams != previous.grams) dao.recordAdjustment(bean.id, bean.grams - previous.grams, now)
            }
            replaced
        }
        covers.delete(replacedCover)
    }
}
