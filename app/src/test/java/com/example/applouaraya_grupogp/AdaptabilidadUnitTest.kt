package com.example.applouaraya_grupogp

import androidx.compose.material3.windowsizeclass.*
import androidx.compose.ui.unit.*
import com.example.applouaraya_grupogp.ui.adaptive.columnasPara
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class AdaptabilidadUnitTest {
    private fun verificar(width: Int, expected: Int) {
        val size = WindowSizeClass.calculateFromSize(DpSize(width.dp, 800.dp))
        assertEquals(expected, columnasPara(size.widthSizeClass))
    }
    @Test fun celular() = verificar(360, 1)
    @Test fun limiteCompacto() = verificar(599, 1)
    @Test fun inicioMediano() = verificar(600, 2)
    @Test fun limiteMediano() = verificar(839, 2)
    @Test fun inicioExpandido() = verificar(840, 3)
    @Test fun tablet() = verificar(1100, 3)
}
