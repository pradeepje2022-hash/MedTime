package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.DoseStatus
import com.example.model.MedicineLog
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineLogDao {

    @Query("SELECT * FROM medicine_logs WHERE dateString = :dateString")
    fun getLogsForDateFlow(dateString: String): Flow<List<MedicineLog>>

    @Query("SELECT * FROM medicine_logs WHERE dateString = :dateString")
    suspend fun getLogsForDateSync(dateString: String): List<MedicineLog>

    @Query("SELECT * FROM medicine_logs WHERE medicineId = :medicineId AND dateString = :dateString AND scheduledTime = :scheduledTime LIMIT 1")
    suspend fun getLog(medicineId: Long, dateString: String, scheduledTime: String): MedicineLog?

    @Query("SELECT * FROM medicine_logs WHERE medicineId = :medicineId ORDER BY dateString DESC, scheduledTime DESC")
    fun getLogsForMedicineFlow(medicineId: Long): Flow<List<MedicineLog>>

    @Query("SELECT * FROM medicine_logs ORDER BY dateString DESC, scheduledTime DESC LIMIT 200")
    fun getAllRecentLogsFlow(): Flow<List<MedicineLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLog(log: MedicineLog): Long

    @Update
    suspend fun updateLog(log: MedicineLog)

    @Query("DELETE FROM medicine_logs WHERE medicineId = :medicineId")
    suspend fun deleteLogsForMedicine(medicineId: Long)

    @Query("UPDATE medicine_logs SET status = :status, actionTimestamp = :timestamp WHERE id = :logId")
    suspend fun updateStatus(logId: Long, status: DoseStatus, timestamp: Long)

    @Query("SELECT COUNT(*) FROM medicine_logs WHERE status = 'TAKEN'")
    fun getTotalTakenCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM medicine_logs")
    fun getTotalLogsCount(): Flow<Int>

    @Query("DELETE FROM medicine_logs")
    suspend fun deleteAllLogs()
}
