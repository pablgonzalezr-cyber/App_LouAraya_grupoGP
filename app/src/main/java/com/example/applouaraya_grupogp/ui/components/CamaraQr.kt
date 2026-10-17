package com.example.applouaraya_grupogp.ui.components

import android.util.Size
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.applouaraya_grupogp.repository.QrImagen
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private class AnalizadorQr(private val habilitado: () -> Boolean, private val codigo: (String?) -> Unit,
    private val error: (String) -> Unit, private val primerFrame: () -> Unit): ImageAnalysis.Analyzer {
    private val detector = QrImagen.detector()
    private val activo = AtomicBoolean(true)
    private val ocupado = AtomicBoolean(false)
    private val primerFrameEnviado = AtomicBoolean(false)

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(proxy: ImageProxy) {
        if(!activo.get() || !habilitado() || !ocupado.compareAndSet(false, true)) { proxy.close(); return }
        val imagen = proxy.image
        if(imagen == null) { ocupado.set(false); proxy.close(); return }
        try {
            detector.process(InputImage.fromMediaImage(imagen, proxy.imageInfo.rotationDegrees))
                .addOnSuccessListener { encontrados ->
                    if(activo.get()) {
                        if(primerFrameEnviado.compareAndSet(false, true)) primerFrame()
                        if(habilitado()) encontrados.firstOrNull()?.let { codigo(it.rawValue) }
                    }
                }
                .addOnFailureListener { if(activo.get()) error("No se pudo analizar la imagen de la cámara. Reintenta.") }
                .addOnCompleteListener { ocupado.set(false); proxy.close() }
        } catch(_: Exception) { ocupado.set(false); proxy.close(); if(activo.get()) error("No se pudo procesar la cámara.") }
    }
    fun cerrar() { activo.set(false); detector.close() }
}

@Composable
fun CamaraQr(modifier: Modifier = Modifier, habilitado: Boolean, onCodigo: (String?) -> Unit,
    onError: (String) -> Unit, onPrimerFrame: () -> Unit) {
    val contexto = LocalContext.current
    val propietario = LocalLifecycleOwner.current
    val permisoActual by rememberUpdatedState(habilitado)
    val codigoActual by rememberUpdatedState(onCodigo)
    val errorActual by rememberUpdatedState(onError)
    val frameActual by rememberUpdatedState(onPrimerFrame)
    val vista = remember(contexto) { PreviewView(contexto).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }
    AndroidView(factory = { vista }, modifier = modifier)
    DisposableEffect(propietario, vista) {
        val ejecutor = Executors.newSingleThreadExecutor()
        val main = ContextCompat.getMainExecutor(contexto)
        val analizador = AnalizadorQr({ permisoActual }, { codigoActual(it) }, { mensaje -> main.execute { errorActual(mensaje) } }, { frameActual() })
        val preview = Preview.Builder().build().apply { setSurfaceProvider(vista.surfaceProvider) }
        val analisis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(ResolutionStrategy(Size(1280,720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)).build())
            .build().apply { setAnalyzer(ejecutor, analizador) }
        val futuro = ProcessCameraProvider.getInstance(contexto)
        var proveedor: ProcessCameraProvider? = null
        var camara: Camera? = null
        var observer: Observer<CameraState>? = null
        val terminado = AtomicBoolean(false)
        futuro.addListener({
            if(!terminado.get()) {
                try {
                    val p = futuro.get(); proveedor = p
                    val selector = when {
                        p.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                        p.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                        else -> throw IllegalStateException("No hay cámara disponible")
                    }
                    camara = p.bindToLifecycle(propietario, selector, preview, analisis)
                    observer = Observer { estado -> if(!terminado.get() && estado.error != null) errorActual("La cámara no está disponible o está siendo usada por otra aplicación.") }
                    camara?.cameraInfo?.cameraState?.observe(propietario, requireNotNull(observer))
                } catch(_: SecurityException) { errorActual("El permiso de cámara no está disponible. Revísalo en Ajustes.")
                } catch(_: Exception) { errorActual("No se pudo abrir una cámara. Puede no estar disponible en este dispositivo.") }
            }
        }, main)
        onDispose {
            terminado.set(true)
            observer?.let { camara?.cameraInfo?.cameraState?.removeObserver(it) }
            analisis.clearAnalyzer(); proveedor?.unbind(preview, analisis)
            analizador.cerrar(); ejecutor.shutdown()
        }
    }
}
