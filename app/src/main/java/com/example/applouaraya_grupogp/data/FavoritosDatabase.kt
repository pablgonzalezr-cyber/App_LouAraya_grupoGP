package com.example.applouaraya_grupogp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// BD independiente para que esta rama NO modifique AppDatabase.kt
// (usuarios) ni se cruce con el trabajo de cotizaciones.
@Database(entities = [FavoritoEntity::class], version = 1, exportSchema = false)
abstract class FavoritosDatabase : RoomDatabase() {
    abstract fun favoritoDao(): FavoritoDao

    companion object {
        @Volatile private var INSTANCIA: FavoritosDatabase? = null

        fun obtener(context: Context): FavoritosDatabase =
            INSTANCIA ?: synchronized(this) {
                INSTANCIA ?: Room.databaseBuilder(
                    context.applicationContext,
                    FavoritosDatabase::class.java,
                    "lou_araya_favoritos.db"
                ).build().also { INSTANCIA = it }
            }
    }
}
