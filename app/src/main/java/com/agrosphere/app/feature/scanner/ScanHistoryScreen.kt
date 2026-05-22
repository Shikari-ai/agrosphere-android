package com.agrosphere.app.feature.scanner

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrosphere.app.R
import com.agrosphere.app.data.repo.LocalScanStore
import com.agrosphere.app.ui.components.ScreenTitle
import com.agrosphere.app.ui.theme.AgroPalette

@Composable
fun ScanHistoryScreen(padding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val scans = remember { LocalScanStore.load(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = padding.calculateBottomPadding()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = AgroPalette.Ink)
            }
            Spacer(Modifier.width(4.dp))
            ScreenTitle(eyebrow = stringResource(R.string.scanner_eyebrow), title = stringResource(R.string.scanner_recent_scans))
        }

        if (scans.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.History, null, tint = AgroPalette.InkMuted, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.scanner_recent_empty), style = MaterialTheme.typography.bodyMedium, color = AgroPalette.InkMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(scans) { s ->
                    Column {
                        val time = if (s.createdAtMillis > 0)
                            DateUtils.getRelativeTimeSpanString(
                                s.createdAtMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                            ).toString() else ""
                        if (time.isNotBlank()) {
                            Text(
                                time.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                                color = AgroPalette.InkMuted,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        DiagnosisCard(s.diagnosis)
                    }
                }
            }
        }
    }
}
