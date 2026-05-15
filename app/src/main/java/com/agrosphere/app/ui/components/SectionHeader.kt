package com.agrosphere.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agrosphere.app.ui.theme.AgroPalette

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = AgroPalette.Ink)
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelMedium,
                color = AgroPalette.Primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun ScreenTitle(eyebrow: String, title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, color = AgroPalette.Primary)
        Text(title, style = MaterialTheme.typography.displayMedium, color = AgroPalette.Ink)
    }
}
