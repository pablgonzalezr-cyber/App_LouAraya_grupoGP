package com.example.applouaraya_grupogp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.ui.design.LouColors

/** Mismo marco para drawable y fotos importadas: objeto completo, sin estirar ni recortar. */
@Composable
fun FotografiaProducto(painter: Painter, descripcion: String, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(20.dp)).background(LouColors.ArenaClara).padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(painter = painter, contentDescription = descripcion,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}
