package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ConsultationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsultationDao {
    @Query("SELECT * FROM consultations ORDER BY timestamp DESC")
    fun getAllConsultations(): Flow<List<ConsultationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsultation(consultation: ConsultationEntity)

    @Query("DELETE FROM consultations WHERE id = :id")
    suspend fun deleteConsultation(id: String)

    @Query("DELETE FROM consultations")
    suspend fun clearAll()
}
