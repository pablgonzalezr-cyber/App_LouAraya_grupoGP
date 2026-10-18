package com.example.applouaraya_grupogp.repository

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

object QrImagen {
    fun detector() = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    fun generar(codigo: String, lado: Int = 768): Bitmap {
        require(ProductoQr.productoDesde(codigo) != null)
        require(lado in 128..2048)
        val matriz = QRCodeWriter().encode(codigo, BarcodeFormat.QR_CODE, lado, lado, mapOf(EncodeHintType.MARGIN to 4))
        val pixeles = IntArray(lado * lado) { indice -> if(matriz[indice % lado, indice / lado]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
        return Bitmap.createBitmap(pixeles, lado, lado, Bitmap.Config.ARGB_8888)
    }
}
