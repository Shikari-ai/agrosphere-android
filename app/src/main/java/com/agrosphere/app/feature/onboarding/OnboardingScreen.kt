package com.agrosphere.app.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrosphere.app.R
import com.agrosphere.app.data.i18n.LocaleManager
import com.agrosphere.app.ui.components.GlassCard
import com.agrosphere.app.ui.theme.AgroBrushes
import com.agrosphere.app.ui.theme.AgroPalette

// ═════════════════════════════════════════════════════════════════════════════
// OnboardingScreen — 4-step post-login setup wizard.
// Step 0: Language (skippable)
// Step 1: App intro + T&C (must accept)
// Step 2: Quick start — add field / scan (skippable)
// Step 3: Setup complete → logo → home
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun OnboardingScreen(
    userName: String = "",
    startStep: Int = 0,
    onApplyLanguage: (tag: String) -> Unit = {},
    onStepChange: (Int) -> Unit = {},
    onModeSelected: (String) -> Unit = {},
    onFinished: (pendingAction: String?) -> Unit,
) {
    var step by remember { mutableIntStateOf(startStep) }
    var selectedLangTag by remember {
        mutableStateOf(
            LocaleManager.currentTag().substringBefore('-').ifEmpty { "en" }
        )
    }
    var tcAccepted by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf("both") }
    var pendingAction by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(step) { onStepChange(step) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AgroBrushes.canvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            StepProgress(step = step, total = 5)

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val fwd = targetState > initialState
                    (slideInHorizontally { if (fwd) it else -it } + fadeIn(tween(260))) togetherWith
                        (slideOutHorizontally { if (fwd) -it else it } + fadeOut(tween(200)))
                },
                modifier = Modifier.weight(1f),
                label = "onb",
            ) { s ->
                when (s) {
                    0 -> LanguageStep(
                        selectedTag = selectedLangTag,
                        onSelect = { selectedLangTag = it },
                        onSkip = { step = 1 },
                        onNext = {
                            onApplyLanguage(selectedLangTag) // saves resume step + applies locale
                            step = 1
                        },
                    )
                    1 -> TermsStep(
                        accepted = tcAccepted,
                        onAcceptChange = { tcAccepted = it },
                        onContinue = { step = 2 },
                    )
                    2 -> ModeStep(
                        selectedMode = selectedMode,
                        onSelect = { selectedMode = it },
                        onNext = {
                            onModeSelected(selectedMode)
                            step = 3
                        },
                    )
                    3 -> QuickStartStep(
                        mode = selectedMode,
                        onSkip = { step = 4 },
                        onContinue = { action ->
                            pendingAction = action
                            step = 4
                        },
                    )
                    else -> CompleteStep(
                        userName = userName,
                        mode = selectedMode,
                        onGo = { onFinished(pendingAction) },
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step progress bar — 4 segments
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepProgress(step: Int, total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            i < step  -> AgroPalette.Primary
                            i == step -> AgroPalette.Primary.copy(alpha = 0.55f)
                            else      -> AgroPalette.SurfaceGlass
                        }
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 0 — Language
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LanguageStep(
    selectedTag: String,
    onSelect: (String) -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    val languages = LocaleManager.supported

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AgroPalette.Amber.copy(alpha = 0.14f))
                    .border(1.dp, AgroPalette.Amber.copy(alpha = 0.30f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Language, null, tint = AgroPalette.Amber, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.onb_lang_title),
                style = MaterialTheme.typography.headlineSmall,
                color = AgroPalette.Ink,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.onb_lang_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = AgroPalette.InkMuted,
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(languages) { lang ->
                val isSelected = lang.tag == selectedTag
                GlassCard(
                    radius = 14.dp,
                    padding = 14.dp,
                    background = if (isSelected)
                        Brush.horizontalGradient(listOf(AgroPalette.Primary.copy(alpha = 0.14f), AgroPalette.Primary.copy(alpha = 0.06f)))
                    else
                        androidx.compose.ui.graphics.SolidColor(AgroPalette.SurfaceGlass),
                    onClick = { onSelect(lang.tag) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(lang.nativeName, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink)
                            if (lang.nativeName != lang.englishName) {
                                Text(lang.englishName, style = MaterialTheme.typography.labelSmall, color = AgroPalette.InkDim)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = AgroPalette.Primary, modifier = Modifier.size(22.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(AgroPalette.SurfaceGlass)
                                    .border(1.dp, AgroPalette.SurfaceGlassBorder, CircleShape),
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        OnboardingBottomBar(
            skipLabel = stringResource(R.string.onb_skip),
            nextLabel = stringResource(R.string.onb_next),
            onSkip = onSkip,
            onNext = onNext,
            nextEnabled = true,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 1 — App intro + Terms & Conditions (not skippable)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TermsStep(
    accepted: Boolean,
    onAcceptChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AgroPalette.Sky.copy(alpha = 0.14f))
                    .border(1.dp, AgroPalette.Sky.copy(alpha = 0.30f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Shield, null, tint = AgroPalette.Sky, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.onb_terms_title),
                style = MaterialTheme.typography.headlineSmall,
                color = AgroPalette.Ink,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.onb_terms_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = AgroPalette.InkMuted,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // What the app does
            GlassCard(background = AgroBrushes.leafCard, radius = 16.dp, padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.onb_terms_what_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = AgroPalette.Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    listOf(
                        stringResource(R.string.onb_terms_feat_1),
                        stringResource(R.string.onb_terms_feat_2),
                        stringResource(R.string.onb_terms_feat_3),
                        stringResource(R.string.onb_terms_feat_4),
                    ).forEach { point ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Rounded.CheckCircle, null,
                                tint = AgroPalette.Primary,
                                modifier = Modifier.size(14.dp).padding(top = 1.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(point, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                        }
                    }
                }
            }

            // T&C summary
            GlassCard(radius = 16.dp, padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.onb_terms_privacy_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = AgroPalette.Ink,
                        fontWeight = FontWeight.SemiBold,
                    )
                    listOf(
                        stringResource(R.string.onb_terms_priv_1),
                        stringResource(R.string.onb_terms_priv_2),
                        stringResource(R.string.onb_terms_priv_3),
                        stringResource(R.string.onb_terms_priv_4),
                        stringResource(R.string.onb_terms_priv_5),
                        stringResource(R.string.onb_terms_priv_6),
                    ).forEach { item ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Rounded.Shield, null,
                                tint = AgroPalette.Sky,
                                modifier = Modifier.size(13.dp).padding(top = 1.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(item, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
                        }
                    }
                }
            }

            // Accept checkbox
            GlassCard(
                radius = 14.dp,
                padding = 14.dp,
                background = if (accepted)
                    Brush.horizontalGradient(listOf(AgroPalette.Primary.copy(alpha = 0.10f), AgroPalette.Primary.copy(alpha = 0.04f)))
                else
                    androidx.compose.ui.graphics.SolidColor(AgroPalette.SurfaceGlass),
                onClick = { onAcceptChange(!accepted) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = accepted,
                        onCheckedChange = onAcceptChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = AgroPalette.Primary,
                            uncheckedColor = AgroPalette.InkDim,
                            checkmarkColor = AgroPalette.BgDeep,
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.onb_terms_agree),
                        style = MaterialTheme.typography.bodySmall,
                        color = AgroPalette.Ink,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
        }

        OnboardingBottomBar(
            skipLabel = null,
            nextLabel = stringResource(R.string.onb_terms_btn),
            onSkip = {},
            onNext = onContinue,
            nextEnabled = accepted,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 2 — Quick start (skippable)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
// ─────────────────────────────────────────────────────────────────────────────
// Step 2 — Mode selection (Farmer / Plant Parent / Both)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ModeStep(
    selectedMode: String,
    onSelect: (String) -> Unit,
    onNext: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AgroPalette.Iris.copy(alpha = 0.14f))
                    .border(1.dp, AgroPalette.Iris.copy(alpha = 0.30f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Grass, null, tint = AgroPalette.Iris, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.onb_mode_title),
                style = MaterialTheme.typography.headlineSmall,
                color = AgroPalette.Ink,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.onb_mode_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = AgroPalette.InkMuted,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModeCard(
                emoji = "🌾",
                title = stringResource(R.string.onb_mode_farmer),
                description = stringResource(R.string.onb_mode_farmer_desc),
                tint = AgroPalette.Amber,
                isSelected = selectedMode == "farmer",
                onClick = { onSelect("farmer") },
            )
            ModeCard(
                emoji = "🪴",
                title = stringResource(R.string.onb_mode_plant),
                description = stringResource(R.string.onb_mode_plant_desc),
                tint = AgroPalette.Primary,
                isSelected = selectedMode == "plant",
                onClick = { onSelect("plant") },
            )
            ModeCard(
                emoji = "🌿",
                title = stringResource(R.string.onb_mode_both),
                description = stringResource(R.string.onb_mode_both_desc),
                tint = AgroPalette.Iris,
                isSelected = selectedMode == "both",
                onClick = { onSelect("both") },
            )
        }

        OnboardingBottomBar(
            skipLabel = null,
            nextLabel = stringResource(R.string.onb_continue),
            onSkip = {},
            onNext = onNext,
            nextEnabled = true,
        )
    }
}

@Composable
private fun ModeCard(
    emoji: String,
    title: String,
    description: String,
    tint: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    GlassCard(
        radius = 16.dp,
        padding = 16.dp,
        background = if (isSelected)
            Brush.linearGradient(listOf(tint.copy(alpha = 0.14f), tint.copy(alpha = 0.05f)))
        else
            androidx.compose.ui.graphics.SolidColor(AgroPalette.SurfaceGlass),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.40f))
        else
            androidx.compose.foundation.BorderStroke(1.dp, AgroPalette.SurfaceGlassBorder),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = if (isSelected) 0.20f else 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = AgroPalette.Ink, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = AgroPalette.InkMuted)
            }
            if (isSelected) {
                Icon(Icons.Rounded.CheckCircle, null, tint = tint, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 3 — Quick start (adapted to mode)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun QuickStartStep(
    mode: String = "both",
    onSkip: () -> Unit,
    onContinue: (action: String?) -> Unit,
) {
    var selectedAction by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AgroPalette.Primary.copy(alpha = 0.14f))
                    .border(1.dp, AgroPalette.Primary.copy(alpha = 0.30f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Grass, null, tint = AgroPalette.Primary, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.onb_quick_title),
                style = MaterialTheme.typography.headlineSmall,
                color = AgroPalette.Ink,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.onb_quick_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = AgroPalette.InkMuted,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Add Field card — shown for farmer + both
            if (mode == "farmer" || mode == "both") {
                QuickStartCard(
                    icon = Icons.Rounded.Grass,
                    title = stringResource(R.string.onb_quick_field_title),
                    description = stringResource(R.string.onb_quick_field_desc),
                    tint = AgroPalette.Amber,
                    isSelected = selectedAction == "fields",
                    onClick = { selectedAction = if (selectedAction == "fields") null else "fields" },
                )
            }

            // Add Plant card — shown for plant + both
            if (mode == "plant" || mode == "both") {
                QuickStartCard(
                    icon = Icons.Rounded.LocalFlorist,
                    title = stringResource(R.string.onb_quick_plant_title),
                    description = stringResource(R.string.onb_quick_plant_desc),
                    tint = AgroPalette.Primary,
                    isSelected = selectedAction == "plants",
                    onClick = { selectedAction = if (selectedAction == "plants") null else "plants" },
                )
            }

            // Scan card — always shown
            QuickStartCard(
                icon = Icons.Rounded.CameraAlt,
                title = stringResource(R.string.onb_quick_scan_title),
                description = stringResource(R.string.onb_quick_scan_desc),
                tint = AgroPalette.Iris,
                isSelected = selectedAction == "scanner",
                onClick = { selectedAction = if (selectedAction == "scanner") null else "scanner" },
            )

            Text(
                stringResource(R.string.onb_quick_hint),
                style = MaterialTheme.typography.labelSmall,
                color = AgroPalette.InkDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }

        OnboardingBottomBar(
            skipLabel = if (selectedAction != null) stringResource(R.string.onb_skip_for_now) else null,
            nextLabel = if (selectedAction != null) stringResource(R.string.onb_continue) else stringResource(R.string.onb_skip_for_now),
            onSkip = onSkip,
            onNext = { onContinue(selectedAction) },
            nextEnabled = true,
        )
    }
}

@Composable
private fun QuickStartCard(
    icon: ImageVector,
    title: String,
    description: String,
    tint: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    GlassCard(
        radius = 16.dp,
        padding = 16.dp,
        background = if (isSelected)
            Brush.linearGradient(listOf(tint.copy(alpha = 0.14f), tint.copy(alpha = 0.05f)))
        else
            androidx.compose.ui.graphics.SolidColor(AgroPalette.SurfaceGlass),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.40f))
        else
            androidx.compose.foundation.BorderStroke(1.dp, AgroPalette.SurfaceGlassBorder),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(tint.copy(alpha = 0.16f))
                    .border(1.dp, tint.copy(alpha = if (isSelected) 0.40f else 0.18f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = AgroPalette.Ink,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroPalette.InkMuted,
                    lineHeight = 18.sp,
                )
            }
            if (isSelected) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.CheckCircle, null, tint = tint, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 3 — Setup complete
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CompleteStep(
    userName: String,
    mode: String = "both",
    onGo: () -> Unit,
) {
    val inf = rememberInfiniteTransition(label = "complete")
    val pulse by inf.animateFloat(
        0.85f, 1.15f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val glow by inf.animateFloat(
        0.25f, 0.70f,
        infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val ringAngle by inf.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(5_000, easing = LinearEasing)),
        label = "ring",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Animated logo
        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Outer glow
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to AgroPalette.Primary.copy(alpha = glow * 0.5f),
                        0.6f to AgroPalette.Primary.copy(alpha = glow * 0.15f),
                        1f to Color.Transparent,
                    ),
                    radius = size.minDimension / 2f * pulse,
                )
                // Rotating ring
                rotate(ringAngle) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(AgroPalette.Primary, AgroPalette.Sky, AgroPalette.Iris, AgroPalette.Amber, AgroPalette.Primary)
                        ),
                        startAngle = 0f, sweepAngle = 270f, useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(8f, 8f),
                        size = androidx.compose.ui.geometry.Size(size.width - 16f, size.height - 16f),
                        style = Stroke(width = 3.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(AgroPalette.Primary.copy(alpha = 0.22f), AgroPalette.Sky.copy(alpha = 0.10f))
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.sweepGradient(listOf(AgroPalette.Primary.copy(alpha = 0.60f + glow * 0.30f), AgroPalette.Sky.copy(alpha = 0.40f), AgroPalette.Primary.copy(alpha = 0.60f + glow * 0.30f))),
                        RoundedCornerShape(26.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Bolt, null,
                    tint = AgroPalette.Primary,
                    modifier = Modifier.size(42.dp),
                )
            }
        }

        Spacer(Modifier.height(30.dp))

        Text(
            "AgroSphere",
            style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 1.sp),
            color = AgroPalette.Primary,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (userName.isNotBlank())
                stringResource(R.string.onb_done_set_name, userName.substringBefore(' '))
            else
                stringResource(R.string.onb_done_set),
            style = MaterialTheme.typography.headlineMedium,
            color = AgroPalette.Ink,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            when (mode) {
                "plant" -> stringResource(R.string.onb_done_body_plant)
                "both"  -> stringResource(R.string.onb_done_body_both)
                else    -> stringResource(R.string.onb_done_body)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = AgroPalette.InkMuted,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )

        Spacer(Modifier.height(44.dp))

        // CTA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(AgroPalette.Primary, AgroPalette.Primary.copy(alpha = 0.75f))
                    )
                )
                .clickable(onClick = onGo)
                .padding(vertical = 17.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Bolt, null, tint = AgroPalette.BgDeep, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.onb_done_btn),
                    style = MaterialTheme.typography.titleMedium,
                    color = AgroPalette.BgDeep,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(R.string.onb_made_in_india),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = AgroPalette.InkDim,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared bottom action bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OnboardingBottomBar(
    skipLabel: String?,
    nextLabel: String,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    nextEnabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (skipLabel != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(AgroPalette.SurfaceGlass)
                    .border(1.dp, AgroPalette.SurfaceGlassBorder, RoundedCornerShape(14.dp))
                    .clickable(onClick = onSkip)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(skipLabel, style = MaterialTheme.typography.labelLarge, color = AgroPalette.InkMuted)
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (nextEnabled) AgroPalette.Primary else AgroPalette.SurfaceGlass
                )
                .clickable(enabled = nextEnabled, onClick = onNext)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                nextLabel,
                style = MaterialTheme.typography.labelLarge,
                color = if (nextEnabled) AgroPalette.BgDeep else AgroPalette.InkDim,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
