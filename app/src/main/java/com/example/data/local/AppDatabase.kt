package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ConsultationEntity
import com.example.data.model.Medicine
import com.example.data.model.Symptom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Symptom::class, Medicine::class, ConsultationEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun symptomDao(): SymptomDao
    abstract fun medicineDao(): MedicineDao
    abstract fun consultationDao(): ConsultationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "symptomed_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate database in background
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).populateInitialData()
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun populateInitialData() {
        if (symptomDao().getSymptomCount() == 0) {
            symptomDao().insertAll(SymptomCatalogueData.getAllSymptoms())
        }
        if (medicineDao().getMedicineCount() == 0) {
            medicineDao().insertAll(MedicineCatalogueData.getAllMedicines())
        }
    }
}
