package com.example.applouaraya_grupogp

import com.example.applouaraya_grupogp.validation.*
import org.junit.Assert.*
import org.junit.Test

class ContenidoValidatorTest {
    private val campania=CampaniaFormulario(titulo="Campaña de primavera",descripcion="Diseños para decorar tu hogar",inicio="17/10/2026",termino="20/10/2026")
    @Test fun fechasValidasInclusivas(){assertTrue(ContenidoValidator.campania(campania).isEmpty());assertTrue(ContenidoValidator.campania(campania.copy(termino=campania.inicio)).isEmpty())}
    @Test fun fechasImposiblesYOrdenIncorrecto(){
        listOf("31/02/2026","2026-10-20","texto","").forEach{assertTrue(ContenidoValidator.campania(campania.copy(inicio=it)).containsKey("inicio"))}
        assertTrue(ContenidoValidator.campania(campania.copy(termino="16/10/2026")).containsKey("termino"))
        assertNotNull(ContenidoValidator.fecha("29/02/2028"));assertNull(ContenidoValidator.fecha("29/02/2027"))
    }
    @Test fun camposObligatoriosCampania(){assertEquals(setOf("titulo","descripcion","inicio","termino"),ContenidoValidator.campania(CampaniaFormulario()).keys)}
    @Test fun coleccionPublicadaRequiereProductos(){
        val f=ColeccionFormulario(nombre="Colección natural",descripcion="Diseños en algodón natural")
        assertTrue(ContenidoValidator.coleccion(f).isEmpty())
        assertTrue(ContenidoValidator.coleccion(f.copy(publicada=true)).containsKey("productos"))
        assertTrue(ContenidoValidator.coleccion(f.copy(publicada=true,productos=setOf(1))).isEmpty())
        assertTrue(ContenidoValidator.coleccion(ColeccionFormulario()).keys.containsAll(listOf("nombre","descripcion")))
    }
}
