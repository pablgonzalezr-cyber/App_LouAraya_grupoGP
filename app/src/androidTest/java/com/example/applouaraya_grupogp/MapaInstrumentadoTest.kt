package com.example.applouaraya_grupogp

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.applouaraya_grupogp.repository.PuntosDemostracion
import com.example.applouaraya_grupogp.ui.screens.MapaScreen
import com.example.applouaraya_grupogp.viewmodel.MapaViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapaInstrumentadoTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun mapaNativoCargaCartografiaPermiteZoomYSeleccionarPuntos() {
        val vm=MapaViewModel()
        compose.setContent { MaterialTheme { MapaScreen(vm,onVolver={}) } }
        compose.waitUntil(40000) { compose.onAllNodesWithText("Mapa cargado").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("mapa-real").assertIsDisplayed()
        compose.onNodeWithText("Zoom +").performClick()
        compose.waitUntil(10000) { vm.estado.value.zoom > 12.5 }
        compose.onNodeWithText("Zoom −").performClick()
        compose.waitUntil(10000) { vm.estado.value.zoom < 12.5 }
        val punto=PuntosDemostracion.puntos.first()
        compose.onNodeWithText(punto.nombre).performClick()
        compose.onNodeWithText(punto.descripcion).assertExists()
        compose.onNodeWithText("Dirección: ${punto.direccion}").assertExists()
        compose.onNodeWithText("Indicaciones a punto ficticio").assertExists()
    }
}
