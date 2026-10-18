package com.example.applouaraya_grupogp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.applouaraya_grupogp.model.UsuarioEntity
import com.example.applouaraya_grupogp.model.CotizacionEntity
import com.example.applouaraya_grupogp.model.Producto
import com.example.applouaraya_grupogp.model.ColeccionEntity
import com.example.applouaraya_grupogp.model.ColeccionProducto
import com.example.applouaraya_grupogp.model.CampaniaEntity
import com.example.applouaraya_grupogp.repository.ProductosDemo
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.applouaraya_grupogp.model.AvisoEntity

@Database(
    entities = [UsuarioEntity::class, CotizacionEntity::class, Producto::class, ColeccionEntity::class, ColeccionProducto::class, CampaniaEntity::class, AvisoEntity::class],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun cotizacionDao(): CotizacionDao
    abstract fun productoDao(): ProductoDao
    abstract fun contenidoDao(): ContenidoDao
    abstract fun avisoDao(): AvisoDao

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS avisos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, usuarioId INTEGER NOT NULL, tipo TEXT NOT NULL, titulo TEXT NOT NULL, mensaje TEXT NOT NULL, referenciaId INTEGER NOT NULL, claveEvento TEXT NOT NULL, creadaEn INTEGER NOT NULL, leida INTEGER NOT NULL, FOREIGN KEY(usuarioId) REFERENCES usuarios(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_avisos_usuarioId ON avisos(usuarioId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_avisos_usuarioId_claveEvento ON avisos(usuarioId,claveEvento)")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS colecciones (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nombre TEXT NOT NULL, descripcion TEXT NOT NULL, activa INTEGER NOT NULL, publicada INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS campanias (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, titulo TEXT NOT NULL, descripcion TEXT NOT NULL, inicio TEXT NOT NULL, termino TEXT NOT NULL, publicada INTEGER NOT NULL, activa INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS coleccion_productos (coleccionId INTEGER NOT NULL, productoId INTEGER NOT NULL, PRIMARY KEY(coleccionId,productoId), FOREIGN KEY(coleccionId) REFERENCES colecciones(id) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(productoId) REFERENCES productos(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_coleccion_productos_productoId ON coleccion_productos(productoId)")
            }
        }
        private fun insertarProductosIniciales(db: SupportSQLiteDatabase) {
            ProductosDemo.productos.forEach { p ->
                db.execSQL("INSERT OR IGNORE INTO productos (id,nombre,categoria,material,precio,descripcion,anchoCm,largoCm,disponible,imagenNombre,activo,imagenArchivo) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    arrayOf<Any?>(p.id,p.nombre,p.categoria,p.material,p.precio,p.descripcion,p.anchoCm,p.largoCm,if(p.disponible) 1 else 0,p.imagenNombre,1,""))
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS productos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nombre TEXT NOT NULL, categoria TEXT NOT NULL, material TEXT NOT NULL, precio INTEGER NOT NULL, descripcion TEXT NOT NULL, anchoCm INTEGER NOT NULL, largoCm INTEGER NOT NULL, disponible INTEGER NOT NULL, imagenNombre TEXT NOT NULL, activo INTEGER NOT NULL, imagenArchivo TEXT NOT NULL)")
                insertarProductosIniciales(db)
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `usuarios` ADD COLUMN `rol` TEXT NOT NULL DEFAULT 'CLIENTE'")
            }
        }
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
        val MIGRACIONES: Array<Migration> get() = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)

        fun obtener(context: Context): AppDatabase {
            return INSTANCIA ?: synchronized(this) {
                INSTANCIA ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lou_araya.db"
                ).addMigrations(*MIGRACIONES)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) { insertarProductosIniciales(db) }
                    }).build().also {
                    INSTANCIA = it
                }
            }
        }
    }
}

