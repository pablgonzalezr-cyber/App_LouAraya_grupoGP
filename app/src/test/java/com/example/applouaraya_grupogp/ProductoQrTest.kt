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
    @Test fun productoInexistente() {
        assertNull(ProductoQr.productoDesde("LOUARAYA:PRODUCTO:999"))
    }
}
