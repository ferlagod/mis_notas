// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.ui.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ferlagod.misnotas.R

/**
 * Pantalla que tapa la app mientras está bloqueada con huella/PIN.
 * Captura todos los toques para que no lleguen al contenido de debajo.
 */
@Composable
fun AppLockScreen(onUnlock: () -> Unit) {
    val activity = LocalActivity.current
    // "Atrás" con la app bloqueada la manda al fondo en lugar de mostrar el contenido.
    BackHandler { activity?.moveTaskToBack(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(140.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.app_locked_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.app_locked_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(onClick = onUnlock, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.unlock))
        }
    }
}
