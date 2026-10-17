package com.example.applouaraya_grupogp

import com.example.applouaraya_grupogp.repository.ProductoQr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductoQrTest {
    @Test fun codigoValido() {
        assertEquals(3, ProductoQr.productoDesde("LOUARAYA:PRODUCTO:3"))
    }
    @Test fun codigoAjeno() {
        assertNull(ProductoQr.productoDesde("OTRA:PRODUCTO:3"))
    }
    @Test fun identificadorNuevoSeConsultaEnRoom() { assertEquals(999, ProductoQr.productoDesde("LOUARAYA:PRODUCTO:999")) }
    @Test fun formatoInvalido() { listOf(null,"","LOUARAYA:PRODUCTO:0","LOUARAYA:PRODUCTO:-1","LOUARAYA:PRODUCTO:1:OTRO","LOUARAYA:PRODUCTO:999999999999","https://ejemplo.test").forEach { assertNull(ProductoQr.productoDesde(it)) } }
    @Test fun generacionValidaIdentificador() { org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { ProductoQr.codigoPara(0) }; assertEquals("LOUARAYA:PRODUCTO:42", ProductoQr.codigoPara(42)) }
}
