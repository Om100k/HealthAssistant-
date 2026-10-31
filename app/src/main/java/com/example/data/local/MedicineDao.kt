package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Medicine
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY medicineName ASC")
    fun getAllMedicines(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE medicineName LIKE '%' || :query || '%' OR genericName LIKE '%' || :query || '%' OR drugCategory LIKE '%' || :query || '%' ORDER BY medicineName ASC")
    fun searchMedicines(query: String): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE id = :id LIMIT 1")
    suspend fun getMedicineById(id: String): Medicine?

    @Query("SELECT COUNT(*) FROM medicines")
    suspend fun getMedicineCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(medicines: List<Medicine>)
}
