package com.example.applouaraya_grupogp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.applouaraya_grupogp.model.UsuarioEntity
import com.example.applouaraya_grupogp.model.CotizacionEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UsuarioEntity::class, CotizacionEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun cotizacionDao(): CotizacionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `cotizaciones` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `usuarioId` INTEGER NOT NULL, `tokenEnvio` TEXT NOT NULL,
                    `descripcion` TEXT NOT NULL, `categoria` TEXT NOT NULL, `material` TEXT NOT NULL,
                    `anchoCm` REAL NOT NULL, `largoCm` REAL NOT NULL, `cantidad` INTEGER NOT NULL,
                    `fechaRequerida` TEXT, `productoReferenciaId` INTEGER, `productoReferenciaNombre` TEXT,
                    `creadaEn` INTEGER NOT NULL, `estado` TEXT NOT NULL,
                    FOREIGN KEY(`usuarioId`) REFERENCES `usuarios`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                )""".trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cotizaciones_usuarioId` ON `cotizaciones` (`usuarioId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cotizaciones_tokenEnvio` ON `cotizaciones` (`tokenEnvio`)")
            }
        }
        @Volatile
        private var INSTANCIA: AppDatabase? = null

        fun obtener(context: Context): AppDatabase {
            return INSTANCIA ?: synchronized(this) {
                INSTANCIA ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lou_araya.db"
                ).addMigrations(MIGRATION_1_2).build().also {
                    INSTANCIA = it
                }
            }
        }
    }
}

