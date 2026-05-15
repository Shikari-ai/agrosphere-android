package com.agrosphere.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.agrosphere.app.ui.components.GhostButton
import com.agrosphere.app.ui.components.PrimaryButton
import com.agrosphere.app.ui.theme.AgroPalette

@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        // Logo mark
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(AgroPalette.PrimaryDim),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Spa, contentDescription = null, tint = AgroPalette.Primary, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("AgroSphere", style = MaterialTheme.typography.displayLarge, color = AgroPalette.Ink)
        Text(
            if (isSignUp) "Create your farm's digital twin." else "Welcome back to your fields.",
            style = MaterialTheme.typography.bodyLarge,
            color = AgroPalette.InkMuted,
        )

        Spacer(Modifier.height(36.dp))

        val colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AgroPalette.Primary,
            unfocusedBorderColor = AgroPalette.SurfaceGlassBorder,
            focusedTextColor = AgroPalette.Ink,
            unfocusedTextColor = AgroPalette.Ink,
            cursorColor = AgroPalette.Primary,
            focusedLabelColor = AgroPalette.Primary,
            unfocusedLabelColor = AgroPalette.InkMuted,
            focusedContainerColor = AgroPalette.SurfaceGlass,
            unfocusedContainerColor = AgroPalette.SurfaceGlass,
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = colors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = colors,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = if (isSignUp) "Create account" else "Sign in",
            icon = Icons.Rounded.ArrowForward,
            enabled = email.isNotBlank() && password.length >= 4,
            onClick = onAuthenticated,
        )
        Spacer(Modifier.height(12.dp))
        GhostButton(
            text = if (isSignUp) "I already have an account" else "Create a new account",
            onClick = { isSignUp = !isSignUp },
        )

        Spacer(Modifier.height(24.dp))
        Text(
            "Continue as guest demo",
            style = MaterialTheme.typography.labelMedium,
            color = AgroPalette.Primary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .padding(6.dp),
        )
        Text(
            "Tap above to skip auth and see the app",
            style = MaterialTheme.typography.labelSmall,
            color = AgroPalette.InkDim,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        // Real "guest" affordance — full-width click target
        Spacer(Modifier.height(8.dp))
        GhostButton(text = "Continue without signing in", onClick = onAuthenticated)
    }
}
