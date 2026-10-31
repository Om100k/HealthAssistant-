package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Symptom
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomDao {
    @Query("SELECT * FROM symptoms ORDER BY name ASC")
    fun getAllSymptoms(): Flow<List<Symptom>>

    @Query("SELECT * FROM symptoms WHERE category = :category ORDER BY name ASC")
    fun getSymptomsByCategory(category: String): Flow<List<Symptom>>

    @Query("SELECT * FROM symptoms WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchSymptoms(query: String): Flow<List<Symptom>>

    @Query("SELECT COUNT(*) FROM symptoms")
    suspend fun getSymptomCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(symptoms: List<Symptom>)
}
