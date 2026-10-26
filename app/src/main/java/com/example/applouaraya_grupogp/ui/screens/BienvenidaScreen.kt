package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.R
import com.example.applouaraya_grupogp.ui.adaptive.AdaptiveColumn
import com.example.applouaraya_grupogp.ui.components.FotografiaProducto
import com.example.applouaraya_grupogp.ui.design.*

@Composable
fun BienvenidaScreen(onComenzar: () -> Unit = {}, onRegistrarse: () -> Unit = {}) {
    AdaptiveColumn(padding = 22.dp, centered = false, spacing = 16.dp) {
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
            LouBrandMark()
        }
        Text("Lou Araya\nMacramé", modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
            color = LouColors.Cafe, textAlign = TextAlign.Center, lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified)
        Text("Arte en fibras que decora tus historias", Modifier.fillMaxWidth(),
            color = LouColors.TextoSecundario, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge)
        FotografiaProducto(painter = painterResource(R.drawable.macrame_tapiz),
            descripcion = "Tapiz artesanal de Lou Araya Macramé",
            modifier = Modifier.fillMaxWidth().aspectRatio(1f))
        LouPrimaryButton("Comenzar", onComenzar, Modifier.fillMaxWidth())
        OutlinedButton(onClick = onRegistrarse, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(15.dp)) { Text("Crear una cuenta") }
        LouPanel(Modifier.fillMaxWidth()) {
            Text("Hecho con dedicación", color = LouColors.Cafe,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Diseños artesanales únicos · Para hogares y celebraciones · Piezas personalizadas",
                color = LouColors.TextoSecundario)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun BienvenidaScreenPreview() { AppLouPreviewContent() }

@Composable
private fun AppLouPreviewContent() {
    com.example.applouaraya_grupogp.ui.theme.AppLouAraya_grupoGPTheme {
        BienvenidaScreen()
    }
}
