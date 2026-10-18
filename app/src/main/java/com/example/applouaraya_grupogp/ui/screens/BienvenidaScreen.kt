
package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.example.applouaraya_grupogp.ui.adaptive.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Colores iniciales de Lou Araya Macramé
private val Crema = Color(0xFFF7F1E8)
private val Cafe = Color(0xFF51362B)
private val Terracota = Color(0xFFA65E43)

@Composable
fun BienvenidaScreen(
    onComenzar: () -> Unit = {},
    onRegistrarse: () -> Unit = {}
) {
    AdaptiveColumn(padding = 28.dp, centered = true) {

        Text(
            text = "Lou Araya Macramé",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Cafe,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Arte tejido a mano, creado para transformar tus espacios.",
            style = MaterialTheme.typography.bodyLarge,
            color = Cafe,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onComenzar,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Terracota
            )
        ) {
            Text("Comenzar")
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = onRegistrarse,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Terracota
            )
        ) {
            Text("Crear una cuenta")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BienvenidaScreenPreview() {
    MaterialTheme {
        BienvenidaScreen()
    }
}
