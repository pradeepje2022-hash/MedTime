package com.medtime.reminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Insert
    suspend fun insert(medicine: Medicine): Long

    @Update
    suspend fun update(medicine: Medicine)

    @Delete
    suspend fun delete(medicine: Medicine)

    @Query("SELECT * FROM medicines ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE active = 1")
    suspend fun getAllActive(): List<Medicine>

    @Query("SELECT * FROM medicines WHERE id = :id")
    fun getById(id: Long): Flow<Medicine?>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getByIdOnce(id: Long): Medicine?
}

@Dao
interface ReminderTimeDao {
    @Insert
    suspend fun insert(reminderTime: ReminderTime): Long

    @Insert
    suspend fun insertAll(times: List<ReminderTime>)

    @Delete
    suspend fun delete(reminderTime: ReminderTime)

    @Query("DELETE FROM reminder_times WHERE medicineId = :medicineId")
    suspend fun deleteAllForMedicine(medicineId: Long)

    @Query("SELECT * FROM reminder_times WHERE medicineId = :medicineId ORDER BY hour, minute")
    fun getForMedicine(medicineId: Long): Flow<List<ReminderTime>>

    @Query("SELECT * FROM reminder_times WHERE medicineId = :medicineId ORDER BY hour, minute")
    suspend fun getForMedicineOnce(medicineId: Long): List<ReminderTime>

    @Query("SELECT * FROM reminder_times")
    suspend fun getAllOnce(): List<ReminderTime>

    @Query("SELECT * FROM reminder_times WHERE id = :id")
    suspend fun getByIdOnce(id: Long): ReminderTime?
}

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(entry: HistoryEntry): Long

    @Update
    suspend fun update(entry: HistoryEntry)

    @Query("SELECT * FROM history_entries WHERE dateEpochDay = :epochDay ORDER BY scheduledHour, scheduledMinute")
    fun getForDay(epochDay: Long): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history_entries WHERE dateEpochDay BETWEEN :from AND :to ORDER BY dateEpochDay DESC, scheduledHour DESC")
    fun getForRange(from: Long, to: Long): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history_entries WHERE medicineId = :medicineId AND reminderTimeId = :reminderTimeId AND dateEpochDay = :epochDay LIMIT 1")
    suspend fun findEntry(medicineId: Long, reminderTimeId: Long, epochDay: Long): HistoryEntry?

    @Query("SELECT * FROM history_entries WHERE id = :id")
    suspend fun getByIdOnce(id: Long): HistoryEntry?

    @Query("SELECT * FROM history_entries WHERE status = 'PENDING' AND dateEpochDay = :epochDay AND (scheduledHour * 60 + scheduledMinute) < :nowMinutes")
    suspend fun getOverdueForDay(epochDay: Long, nowMinutes: Int): List<HistoryEntry>
}
