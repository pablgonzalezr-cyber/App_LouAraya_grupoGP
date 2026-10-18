package com.example.applouaraya_grupogp.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.applouaraya_grupogp.model.PuntoDemostracion
import org.maplibre.android.MapLibre
import org.maplibre.android.RenderingEngine
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

object ProveedorMapa {
    const val ESTILO = "https://tiles.openfreemap.org/styles/liberty"
    const val CAPA_PUNTOS = "lou-puntos-demo"
}

@Composable
fun MapaInteractivo(puntos: List<PuntoDemostracion>, seleccionado: String?, modifier: Modifier = Modifier,
    onListo: (MapLibreMap) -> Unit, onSeleccion: (String) -> Unit, onZoom: (Double) -> Unit, onError: (String) -> Unit) {
    val contexto = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val listoActual by rememberUpdatedState(onListo)
    val seleccionActual by rememberUpdatedState(onSeleccion)
    val zoomActual by rememberUpdatedState(onZoom)
    val errorActual by rememberUpdatedState(onError)
    val resultado = remember(contexto) { runCatching {
        MapLibre.getInstance(contexto, null, WellKnownTileServer.MapLibre, RenderingEngine.Type.OPENGL)
        MapView(contexto).apply { onCreate(null) }
    } }
    val vista = resultado.getOrNull()
    if(vista == null) {
        LaunchedEffect(Unit) { errorActual("Este dispositivo no pudo iniciar el mapa interactivo.") }
        return
    }
    var mapa by remember(vista) { mutableStateOf<MapLibreMap?>(null) }
    var aplicado by remember(vista) { mutableStateOf<String?>(null) }
    AndroidView(factory = { vista }, modifier = modifier, update = {
        if(seleccionado != null && seleccionado != aplicado) puntos.find { it.id == seleccionado }?.let { p ->
            mapa?.let { m ->
                m.animateCamera(CameraUpdateFactory.newLatLng(LatLng(p.latitud, p.longitud)))
                aplicado = seleccionado
            }
        }
    })
    DisposableEffect(vista, lifecycle) {
        var destruido = false
        var iniciado = false
        var reanudado = false
        var click: MapLibreMap.OnMapClickListener? = null
        var idle: MapLibreMap.OnCameraIdleListener? = null
        fun pausar() { if(reanudado) { vista.onPause(); reanudado = false } }
        fun detener() { pausar(); if(iniciado) { vista.onStop(); iniciado = false } }
        val observer = LifecycleEventObserver { _, evento ->
            if(!destruido) when(evento) {
                Lifecycle.Event.ON_START -> if(!iniciado) { vista.onStart(); iniciado = true }
                Lifecycle.Event.ON_RESUME -> if(!reanudado) { vista.onResume(); reanudado = true }
                Lifecycle.Event.ON_PAUSE -> pausar()
                Lifecycle.Event.ON_STOP -> detener()
                else -> Unit
            }
        }
        val fallo = MapView.OnDidFailLoadingMapListener { if(!destruido) errorActual("No se pudo cargar el mapa. Revisa la conexión y reintenta.") }
        vista.addOnDidFailLoadingMapListener(fallo)
        lifecycle.addObserver(observer)
        vista.getMapAsync { m ->
            if(!destruido) {
                mapa = m
                m.uiSettings.isCompassEnabled = true
                m.uiSettings.isAttributionEnabled = true
                m.cameraPosition = CameraPosition.Builder().target(LatLng(-33.445, -70.641)).zoom(12.0).build()
                m.setStyle(Style.Builder().fromUri(ProveedorMapa.ESTILO)) { estilo ->
                    if(!destruido) {
                        val features = puntos.map { p -> Feature.fromGeometry(Point.fromLngLat(p.longitud, p.latitud)).apply { addStringProperty("id", p.id) } }
                        estilo.addSource(GeoJsonSource("lou-puntos-fuente", FeatureCollection.fromFeatures(features)))
                        estilo.addLayer(CircleLayer(ProveedorMapa.CAPA_PUNTOS, "lou-puntos-fuente").withProperties(
                            circleRadius(10f), circleColor("#A65E43"), circleStrokeColor("#FFF9F0"), circleStrokeWidth(3f)))
                        click = MapLibreMap.OnMapClickListener { posicion ->
                            val f = m.queryRenderedFeatures(m.projection.toScreenLocation(posicion), ProveedorMapa.CAPA_PUNTOS).firstOrNull()
                            f?.getStringProperty("id")?.let { seleccionActual(it) }
                            f != null
                        }
                        m.addOnMapClickListener(requireNotNull(click))
                        idle = MapLibreMap.OnCameraIdleListener { if(!destruido) zoomActual(m.cameraPosition.zoom) }
                        m.addOnCameraIdleListener(requireNotNull(idle))
                        zoomActual(m.cameraPosition.zoom)
                        listoActual(m)
                    }
                }
            }
        }
        onDispose {
            destruido = true
            click?.let { mapa?.removeOnMapClickListener(it) }
            idle?.let { mapa?.removeOnCameraIdleListener(it) }
            vista.removeOnDidFailLoadingMapListener(fallo)
            lifecycle.removeObserver(observer)
            detener(); vista.onDestroy()
        }
    }
}
